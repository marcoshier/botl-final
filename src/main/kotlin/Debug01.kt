import org.openrndr.Program
import org.openrndr.application
import org.openrndr.draw.ColorBuffer
import org.openrndr.extra.shapes.grid
import org.openrndr.extra.viewbox.viewBox
import kotlin.math.sqrt

fun main() = application {
    configure {
        width = 960
        height = 960
       //position = IntVector2(1920, 0)
    }
    program {

        val grid = drawer.bounds.grid(2, 2)

        //val dry = viewBox(grid[0][0]).apply { loadVideoSource( false, dry = true, true)  }
        val masked = viewBox(grid[0][1]).apply { loadVideoSource( true, dry = false, false)  }
        val treated = viewBox(grid[1][0]).apply { wet(true) }
        //val videos = viewBox(grid[1][1]).apply { videosGrid() }

        val treat: (img: ColorBuffer) -> Unit by treated.userProperties

        extend {

            //dry.draw()

            masked.draw()

            treat(masked.result)
            treated.draw()

            //videos.draw()
        }
    }
}

fun Program.videosGrid() {

    val assetLoader = AssetLoader()
    val rc = sqrt(assetLoader.thumbnails.size * 1.0).toInt()
    val grid = drawer.bounds.grid(rc, rc).flatten()

    var active = 0

    keyboard.keyUp.listen {
        if(it.name == "k") {
            active++
            assetLoader.thumbnails[active].isActive = true
        }
    }

    extend {
        assetLoader.thumbnails.filter { it.isActive }.take(grid.size).mapIndexed { i, it ->
            if(it.cb != null) drawer.image(it.cb!!, it.cb!!.bounds, grid[i])
        }


    }


}