import boofcv.abst.tracker.PointTrack
import org.openrndr.color.ColorHSLa
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.loadImage
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.color.presets.ORANGE
import org.openrndr.extra.color.spaces.ColorOKHSVa
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.noise.uniformRing
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.bounds

class TrackPoint(val pos: Vector2, val id: Int): PointTrack()

class Droplet {

    var start = System.currentTimeMillis()
    var timeAlive = 0L
    var counter = 0
    var isTracked = true

    var imageLoaded = false
    var cb = colorBuffer(100, 100)

    var file = ""


    var points = setOf<TrackPoint>()
        set(value) {
            bounds = value.map { it.pos }.bounds
            currentIds = value.map { it.id }
            field = value
        }

    var currentIds = listOf<Int>()
        set(value) {
            val condition = value.intersect(field).isNotEmpty()
            isTracked = condition
            field = value
        }

    var bounds = Rectangle.EMPTY

    var previousPoints = setOf<TrackPoint>()

    var contour = ShapeContour.EMPTY

    private fun checkStatic() {
        if(counter == 120) {
            if(previousPoints == points) {
                isTracked = false
            } else {
                counter = 0
            }
        }
        counter++
    }

    val c = ColorRGBa.RED

    fun update() {
        timeAlive = System.currentTimeMillis() - start

        if(timeAlive > 4000L && !imageLoaded) {
            println(file)
            //cb = loadImage(file) // TODO
            //getLabel()
            imageLoaded = true
        }
    }


    fun draw(drawer: Drawer) {
        update()

        if(timeAlive > 2000L && points.isNotEmpty()) {
           // checkStatic()


            drawer.fill = ColorRGBa.WHITE

            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                                  vec2 texCoord = c_boundsPosition.xy;
                                  texCoord.y = 1.0 - texCoord.y;
                                  vec2 size = textureSize(p_image, 0);
                                  texCoord.x /= size.x/size.y;
                                  texCoord.x += 0.18;
                                  x_fill = texture(p_image, texCoord);
                              """
                parameter("image", cb)
            }

            drawer.stroke = ColorRGBa.WHITE
            drawer.shape(contour.shape)
            drawer.shadeStyle = null

            // debug

            drawer.fill = if(isTracked) ColorRGBa.GREEN else ColorRGBa.ORANGE
            drawer.text(isTracked.toString(), bounds.corner + Vector2(10.0, 24.0))
            drawer.text(counter.toString(), bounds.corner + Vector2(10.0, 48.0))
            drawer.text(timeAlive.toString().dropLast(3), bounds.corner + Vector2(10.0, 64.0))
        }
    }


}