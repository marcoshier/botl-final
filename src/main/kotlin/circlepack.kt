import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import kotlin.math.*

fun main() {
    application {
        configure {
            width = 800
            height = 800
        }
        program {
            val smallerCircleSizes = listOf(50.0, 30.0, 40.0, 20.0) // Fixed sizes of smaller circles

            val boundingCircleRadius = width / 2.0 // Radius of the bounding circle
            val centerPoint = Vector2(width / 2.0, height / 2.0)

            val packedCircles = packCircles(smallerCircleSizes, boundingCircleRadius)

            extend {
                drawer.clear(ColorRGBa.BLACK)

                // Draw the bounding circle
                drawer.fill = null
                drawer.stroke = ColorRGBa.WHITE
                drawer.circle(centerPoint.x, centerPoint.y, boundingCircleRadius)

                // Draw the packed circles
                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = null
                packedCircles.forEach { circle ->
                    drawer.circle(circle.center.x, circle.center.y, circle.radius)
                }
            }

        }
    }
}

data class PackedCircle(var center: Vector2, val radius: Double)

fun packCircles(sizes: List<Double>, boundingRadius: Double): List<PackedCircle> {
    val packedCircles = mutableListOf<PackedCircle>()

    val totalArea = sizes.sumByDouble { it * it * PI }
    val scaleFactor = sqrt(totalArea / (PI * boundingRadius * boundingRadius))

    val sortedSizes = sizes.sortedDescending()

    sortedSizes.forEachIndexed { index, size ->
        val scaledRadius = size * scaleFactor
        val circle = PackedCircle(Vector2.ZERO, scaledRadius)

        var attempts = 0
        while (true) {
            val angle = random(0.0, 2 * PI)
            val distance = random(scaledRadius, boundingRadius)

            val position = Vector2(
                boundingRadius + cos(angle) * distance,
                boundingRadius + sin(angle) * distance
            )

            circle.center = position

            if (packedCircles.none { it.center.distanceTo(circle.center) < it.radius + circle.radius }) {
                packedCircles.add(circle)
                break
            }

            attempts++
            if (attempts >= 1000) {
                break
            }
        }
    }

    return packedCircles
}

fun random(min: Double, max: Double): Double {
    return min + (max - min) * Math.random()
}