import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.invert
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.ffmpeg.PlayMode
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.ffmpeg.loadVideoDevice
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle

fun Program.loadVideoSource(debug: Boolean = false, dry: Boolean = false, gui: Boolean = false) {
    val cb = colorBuffer(width, height)
    val video = if(debug) loadDefaultVideo() else loadWebcam()

    video.newFrame.listen {
        it.frame.copyTo(cb,
            sourceRectangle = it.frame.bounds.flippedVertically().toInt(),
            targetRectangle = Rectangle(-(it.frame.bounds.width - drawer.bounds.width) / 2.0 + (it.frame.bounds.width / 10.0), 0.0, it.frame.bounds.width, it.frame.bounds.height).toInt()
        )
    }

    val g = GUI()
    val params = object {
        @DoubleParameter("mask size", 200.0, 500.0)
        var maskSize = 239.13

        @DoubleParameter("image x offset", 0.0, 1.0)
        var xOffset = 0.099

        @DoubleParameter("image y offset", -1.0, 1.0)
        var yOffset = 0.143

        @DoubleParameter("circle x offset", -1.0, 1.0)
        var cxOffset = 0.5

        @DoubleParameter("circle y offset", 0.0, 1.0)
        var cyOffset = 0.5

        @DoubleParameter("scaleX ", 0.0, 2.0)
        var scaleX = 1.168

        @DoubleParameter("scaleY ", 0.0, 2.0)
        var scaleY = 1.280
    }.addTo(g)

    extend(g)
    extend {

        g.visible = !dry && gui

        drawer.clear(ColorRGBa.WHITE)
        video.draw(drawer, blind = true)

        if(dry) {
            drawer.image(cb)
        } else {

            drawer.drawStyle.colorMatrix = invert
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                        vec2 texCoord = c_boundsPosition.xy;
                        texCoord.y = 1.0 - texCoord.y;
                        vec2 size = textureSize(p_image, 0);
                        texCoord.x /= (size.x * p_scaleX) / size.y;
                        texCoord.y /= p_scaleY;
                        texCoord.x += p_xOffset;
                        texCoord.y += p_yOffset;
                        x_fill = texture(p_image, texCoord);
                    """
                parameter("image", cb)
                parameter("xOffset", params.xOffset)
                parameter("yOffset", params.yOffset)
                parameter("scaleX", params.scaleX)
                parameter("scaleY", params.scaleY)
            }

            drawer.stroke = null
            val shape = Circle(width * params.cxOffset, height * params.cyOffset, params.maskSize).shape
            drawer.shape(shape)
        }


    }
}

fun loadDefaultVideo(): VideoPlayerFFMPEG {
    return VideoPlayerFFMPEG.fromFile("offline-data/plate.mp4", PlayMode.VIDEO).apply {
        seek(180.0)
        play()
        ended.listen {
            restart()
        }
    }
}

fun loadWebcam(): VideoPlayerFFMPEG {
    val devices = VideoPlayerFFMPEG.listDeviceNames()
    val webcam = devices.firstOrNull { it.contains("OBS") }

    println(devices)

    return if(webcam != null) {
        loadVideoDevice(webcam, PlayMode.VIDEO).apply {
            play()
        }
    } else error("target webcam not found")
}