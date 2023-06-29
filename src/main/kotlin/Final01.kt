import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.isolated
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

fun main() = application {
    val debug = true
    configure {
        if(!debug) {
            hideWindowDecorations = true
            hideCursor = true
            windowAlwaysOnTop = true
            //position = IntVector2(1520 + ((1920 - 1080) / 2), 0)
        }
        width = 1920
        height = 1080
        //position = IntVector2(1920, 0)
    }
    program {


        val dry = viewBox(Rectangle(0.0, 0.0, 480.0, 480.0)).apply { loadVideoSource(debug, gui = false)  }
        val video = viewBox(Rectangle(0.0, 0.0, 480.0, 480.0)).apply { wet(false) }
        val treat: (img: ColorBuffer) -> Unit by video.userProperties

        val plate = Plate(drawer.bounds)

        plate.loader.thumbnails.forEach {
            it.playVideoEvent.listen {
                println("now play $it")
            }
        }

        extend {

            dry.update()
            treat(dry.result)
            video.update()

            plate.update(video.result)
            plate.draw(drawer)

            drawer.fill = null
            drawer.stroke = ColorRGBa.BLACK
            drawer.strokeWeight = 750.0
            drawer.translate(drawer.bounds.center.x + 328, drawer.bounds.center.y + -120)
            drawer.scale(0.93, 0.7)
            drawer.circle(Vector2.ZERO, 1281.0)



             drawer.isolated {
                  drawer.defaults()
                  video.draw()
             }
        }
    }
}