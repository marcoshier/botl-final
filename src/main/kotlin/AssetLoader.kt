import org.openrndr.animatable.Animatable
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.loadImage
import org.openrndr.events.Event
import org.openrndr.ffmpeg.PlayMode
import org.openrndr.ffmpeg.VideoPlayerConfiguration
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import java.io.File

class AssetLoader {

    val path = "C:\\Users\\31640\\Desktop\\Archive_0.1\\Archive_segmented\\Videos"
    val thumbnails = loadThumbnails().take(300)
    //val videos = loadVideos()



    fun loadThumbnails(): List<Thumbnail> {
        val files = File(path).walk().filter { it.isFile && ( it.extension == "jpg" || it.extension == "png" )}.toMutableList()

        return files.map { Thumbnail(it) }
    }

    fun loadVideos(): List<Video> {
        val files = File(path).walk().filter { it.isFile && it.extension == "mp4" }.toMutableList()

        return files.mapIndexed { i, it ->
            println("loading $i/${files.size} - ${it.name}")
            Video(it)
        }
    }

//    fun update(drawer: Drawer) {
//        this.videos.forEach {
//            if(it.isActive && it.video != null) it.update(drawer)
//        }
//    }


}

class Thumbnail(val src: File): Animatable() {


    val playVideoEvent = Event<String>()

    var isPlaying = false
    var isActive = false
        set(value) {
            field = value
            if(value && !isPlaying) {
                startVideoTimer()
            } else {
                stopVideoTimer()
            }
        }
    var timer = 0.0
        set(value) {
            isActive = value > 200.0
            field = value.coerceIn(0.0, 200.0)
        }

    var videoTimer = 0.0

    fun startVideoTimer() {
        ::videoTimer.animate(1.0, 10000).completed.listen {
            isPlaying = true
        }
    }

    fun stopVideoTimer() {
        ::videoTimer.cancel()
    }

    var cb = loadImage(src)

}

class Video(val src: File) {

    var isActive = false
        set(value) {
            if(!field && value) load()
            else if (field && !value) unload()
            field = value
        }

    var cb: ColorBuffer? = null
    var video: VideoPlayerFFMPEG? = null

    var counter = 0

    fun load() {
        println("loading ${src.name}")
        if(cb == null) {
            if(src.extension == "mp4") {
                val vc = VideoPlayerConfiguration().apply {
                    useHardwareDecoding = true
                    synchronizeToClock = true
                }
                video = VideoPlayerFFMPEG.fromFile(src.path, PlayMode.VIDEO, vc).apply {
                    play()
                    ended.listen { restart() }
                }

                cb = colorBuffer(video!!.width, video!!.height)
                video!!.newFrame.listen { it.frame.copyTo(cb!!) }

            } else if (src.extension == "jpg" || src.extension == "png"){
                cb = loadImage(src)
            }
        }

    }

    fun unload() {
        video?.pause()
        video?.dispose()
    }

    fun update(drawer: Drawer) {
        if(isActive) {
            video?.draw(drawer, true)
        }
    }
}