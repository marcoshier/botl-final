import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2

fun main() = application {
    configure {
        width = 1920
        height = 1080
        position = IntVector2(1520 + ((1920 - 1080) / 2), 0)
        windowAlwaysOnTop = true
    }
    program {
        val cb = colorBuffer(width, height)
        val video = loadWebcam()

        video.newFrame.listen {
            it.frame.copyTo(cb,
                sourceRectangle = it.frame.bounds.flippedVertically().toInt(),
                targetRectangle = drawer.bounds.toInt()
            )
        }

        val rabbit = RabbitControlServer()
        val s = object {
            @DoubleParameter("x", 0.0, 1080.0)
            var x = 319.0

            @DoubleParameter("y", 0.0, 1080.0)
            var y = -106.0

            @DoubleParameter("scaleX", 0.0, 1.0)
            var scaleX = 0.91

            @DoubleParameter("scaleY", 0.0, 1.0)
            var scaleY = 0.68

        }

        rabbit.add(s)
        extend(rabbit)
        extend {

            video.draw(drawer, blind = true)



            drawer.stroke = ColorRGBa.GREEN
            drawer.fill = null
            drawer.rectangle(drawer.bounds)

            drawer.fill = ColorRGBa.RED
            drawer.strokeWeight = 30.0



            drawer.translate(drawer.bounds.center.x + s.x, drawer.bounds.center.y + s.y)
            drawer.scale(s.scaleX, s.scaleY)
            drawer.circle(Vector2.ZERO, 540.0)


            drawer.image(cb)
        }
    }
}