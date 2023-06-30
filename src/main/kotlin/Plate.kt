import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.color.presets.TURQUOISE
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.triangulation.delaunayTriangulation
import org.openrndr.math.CatmullRomChain2
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.shape.*
import tools.computeContours
import tools.range
import kotlin.random.Random

class Plate(frame: Rectangle) {

    val loader = AssetLoader().also { println(it.thumbnails.size) }

    val circle = Circle(Vector2.ZERO,frame.height / 2.8).contour.transform(
        transform {
            translate(frame.center.x + 328.0, frame.center.y -120)
            scale(1.274, 0.95)
        }
    )
    val points = loader.thumbnails.take(150).mapIndexed { i, it ->
        val positions = circle.contour.equidistantPositions(30)
        if(i < positions.size) {
            positions[i]
        } else circle.shape.uniform(10.0, Random(i)) }
    val del = points.delaunayTriangulation()

    val delaunayContours = del.triangles().map { CatmullRomChain2(it.contour.equidistantPositions(15), loop = true).toContour() }
    val trianglesToThumbnails = (delaunayContours zip loader.thumbnails).toMap()

    var contours = listOf<ShapeContour>()
    var image = colorBuffer(480, 480)

    var otn = mutableListOf<Pair<ShapeContour, ShapeContour>>()

    fun update(cb: ColorBuffer) {
        cb.copyTo(image)

        val oldToNew = mutableListOf<Pair<ShapeContour, ShapeContour>>()

        val newContours = computeContours(image)
            .filter { it.bounds.width in range && it.bounds.height in range }
            .map { it.transform(transform {
                translate(800.0, 60.0)
                scale(2.03, 1.54)
            }) }.toMutableList()
        if(newContours.isNotEmpty()) {
            contours = newContours.map {
                CatmullRomChain2(it.equidistantPositions(10), loop = true).toContour()
            }
        }

        otn = oldToNew
    }


    fun draw(drawer: Drawer) {
        drawer.clear(ColorRGBa.RED)

        fun drawTile(triangle: ShapeContour, thumbnail: Thumbnail) {

            val points = (triangle.contour.equidistantPositions(20) zip circle.contour.equidistantPositions(20)).map {
                it.first.mix(it.second, thumbnail.sizeFader)
            }
            val triangleContour = CatmullRomChain2(points, loop = true).toContour()

            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE.opacify(0.3)
            drawer.strokeWeight = 0.2
            drawer.contour(triangleContour)


            val contoursInTriangle = contours.filter { it.bounds.center.distanceTo(triangle.bounds.center) < 40.0 }

            if(contoursInTriangle.isNotEmpty()) {
                thumbnail.timer += 1.0
            } else {
                thumbnail.timer -= 1.0
            }

            val normalized = (thumbnail.timer / 200.0).coerceIn(0.0, 1.0)
            val videoCb = loader.activeVideo?.video?.colorBuffer

            drawer.stroke = null
            drawer.fill = ColorRGBa.WHITE
            ssImageToShape.parameter("opacity", normalized)
            ssImageToShape.parameter("image", thumbnail.cb)
            ssImageToShape.parameter("image2", videoCb ?: thumbnail.cb)
            drawer.shadeStyle = ssImageToShape
            drawer.shape(triangleContour.shape)
            drawer.shadeStyle = null


            drawer.stroke = if (thumbnail.isPlaying) ColorRGBa.BLUE else ColorRGBa.WHITE
            drawer.strokeWeight = 0.75
            drawer.contour(triangleContour.sub(0.0, normalized))

        }

        val list = trianglesToThumbnails.entries.toMutableList()
        val active = list.firstOrNull { it.value.isPlaying }
        if(active != null) {
            list.remove(active)
            list.add(active)
        }

        list.forEach { (triangle, thumbnail) ->
            //drawer.text(it.second.src.name, it.first)
                drawTile(triangle, thumbnail)
//            drawer.fill = ColorRGBa.WHITE
//            drawer.text(thumbnail.timer.toString(), triangle.bounds.center)
        }

        if(active != null && active.value.isPlaying) {
                active.value.info.updateAnimation()
                active.value.info.draw(drawer)
        }

        drawer.strokeWeight = 1.2
        drawer.stroke = ColorRGBa.YELLOW
        drawer.fill = null

        for((index, contour) in contours.withIndex()) {
            drawer.contour(contour)
        }

        loader.thumbnails.forEach {
            it.updateAnimation()
        }

        drawer.stroke = ColorRGBa.TURQUOISE
        drawer.lineSegments((otn.map { it.first.bounds.center } zip otn.map { it.second.bounds.center }).map { LineSegment(it.first, it.second) })
//
//        drawer.strokeWeight = 10.0
//        drawer.stroke = ColorRGBa.BLUE
//        drawer.fill = ColorRGBa.BLUE.opacify(0.2)
//        drawer.contour(circle)
//
//        drawer.fill = null
//        drawer.strokeWeight = 1.0
//        drawer.stroke = ColorRGBa.PINK
//        drawer.contours(delaunayContours)

    }

}


val ssImageToShape = shadeStyle {
    fragmentTransform = """
                        vec2 texCoord = c_boundsPosition.xy;
                        texCoord.y = 1.0 - texCoord.y;
                        vec2 size = textureSize(p_image, 0);
                        texCoord.x /= size.x / size.y;
                        vec4 t1 = texture(p_image, texCoord);
                        
                  
                        vec2 texCoord2 = c_boundsPosition.xy;
                        texCoord2.y = texCoord2.y;
                        vec2 size2 = textureSize(p_image2, 0);
                        texCoord2.x /= size2.x / size2.y;
                        vec4 t2 = texture(p_image2, texCoord2);
                        
                        x_fill = mix(t1, t2, t2.a);
                        x_fill.a = p_opacity;
                    """
}