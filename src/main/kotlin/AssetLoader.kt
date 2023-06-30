import org.openrndr.animatable.Animatable
import org.openrndr.animatable.PropertyAnimationKey
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.events.Event
import org.openrndr.ffmpeg.PlayMode
import org.openrndr.ffmpeg.VideoPlayerConfiguration
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.math.Vector2
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import java.io.File
import kotlin.random.Random

class AssetLoader: Animatable() {

    val path = "C:\\Users\\31640\\Desktop\\Archive_0.1\\Archive_segmented\\Videos_backup"
    val thumbnails = loadThumbnails().take(300)
    val videos = loadVideos()

    val thumbnailsToClips = thumbnails.associate { t -> t to videos.filter { it.src.parent == t.src.parent} }

    var waitBeforeNextVideo = 0.0
    var wait = false
        set(value) {
            field = value
            ::waitBeforeNextVideo.animate(1.0, 15000).completed.listen {
                field = false
                println("go")
            }
        }

    var lastThumbnail : Thumbnail? = null
    var recentlyPlayed = mutableListOf<Video>()
    var activeVideo: Video? = null
        set(value) {
            if(field == null && value != null) {
                field = value
                println(value.src)
                println(value.video)
                value.load()
                value.video?.ended?.listen {
                    println(recentlyPlayed.size)
                    lastThumbnail?.isPlaying = false
                    lastThumbnail = null
                    if (recentlyPlayed.size > 10) recentlyPlayed.drop(1)
                    recentlyPlayed.add(value)
                    wait = true
                    field = null
                }
            }
        }

    init {
        thumbnails.map { thumbnail ->
            thumbnail.playVideoEvent.listen {
                if (!wait && activeVideo == null) {
                    val clips = thumbnailsToClips[thumbnail]
                    if(!clips.isNullOrEmpty()) {
                        val clip = clips.random(Random(1))
                        activeVideo = if(recentlyPlayed.contains(clip)) clips.random(Random(2)) else clip
                        thumbnail.isPlaying = true
                        lastThumbnail = thumbnail
                    }
                }
            }
        }
    }


    fun loadThumbnails(): List<Thumbnail> {
        val files = File(path).walk().filter { it.isFile && ( it.extension == "jpg" || it.extension == "png")}.toMutableList()

        return files.map { Thumbnail(it) }
    }

    fun loadVideos(): List<Video> {
        val files = File(path).walk().filter { it.isFile && it.extension == "mp4" && it.nameWithoutExtension.contains("mp4") }.toMutableList()

        return files.mapIndexed { i, it ->
            println("loading $i/${files.size} - ${it.name}")
            Video(it)
        }
    }

    fun update(drawer: Drawer) {
        updateAnimation()
        activeVideo?.video?.draw(drawer, blind = true)
        thumbnails.forEach {
            it.updateAnimation()
        }
    }

//    fun update(drawer: Drawer) {
//        this.videos.forEach {
//            if(it.isActive && it.video != null) it.update(drawer)
//        }
//    }


}

class Info(val lines: List<String>): Animatable() {

    val types = listOf("title", "type", "author", "year", "description")

    var fader = 0.0

    fun fadeIn() {
        ::fader.animate(1.0, 3000, Easing.SineInOut, 3000)
    }

    fun fadeOut() {
        ::fader.animate(0.0, 3000, Easing.SineInOut)
    }

    val typeFm = loadFont("data/fonts/AzeretMono-BoldItalic.ttf", 13.0)
    val textFm = loadFont("data/fonts/AzeretMono-Medium.ttf", 18.0)

    fun draw(drawer: Drawer) {
        updateAnimation()

        val bounds = Rectangle(drawer.bounds.center + Vector2(-180.0, - 190.0), 280.0, 440.0)

        drawer.isolated {
            drawer.translate(drawer.bounds.center)
            drawer.scale(-1.0, 1.0)
            drawer.translate(-drawer.bounds.center)
            drawer.writer {
                box = bounds
                for((line, type) in lines zip types) {
                    drawer.fill = ColorRGBa.WHITE.opacify(0.7)
                    drawer.fontMap = typeFm
                    newLine()
                    gaplessNewLine()
                    text(type.take((type.length * fader).toInt()))
                    newLine()
                    drawer.fill = ColorRGBa.WHITE
                    drawer.fontMap = textFm
                    text(line.take((line.length * fader).toInt()))
                }
            }

        }
    }
}

class Thumbnail(val src: File): Animatable() {

    val playVideoEvent = Event<Unit>()

    val info = Info(File(src.parent + "/info.txt").readLines())

    var sizeFader = 0.0
    var imageFader = 0.0

    fun grow(): PropertyAnimationKey<Double> {
        return ::sizeFader.animate(1.0, 3000, Easing.SineInOut)
    }

    fun fadeIn() {
        ::imageFader.animate(1.0, 3000, Easing.SineInOut)
    }

    fun shrink() {
        ::sizeFader.animate(0.0, 3000, Easing.SineInOut)
    }

    fun fadeOut(): PropertyAnimationKey<Double>  {
        return ::imageFader.animate(1.0, 3000, Easing.SineInOut)
    }

    var isPlaying = false
        set(value) {
            if(!field && value) {
                grow().completed.listen {
                    fadeIn()
                    info.fadeIn()
                }
            } else {
                info.fadeOut()
                fadeOut().completed.listen {
                    shrink()
                }
            }
            field = value
        }

    var isActive = false
        set(value) {
            if(!field && value) {
                playVideoEvent.trigger(Unit)
            }

            field = value
        }

    var timer = 0.0
        set(value) {
            isActive = value > 200.0
            field = value.coerceAtLeast(0.0)
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

    fun load() {

        if(cb == null) {
            val vc = VideoPlayerConfiguration().apply {
                useHardwareDecoding = true
                synchronizeToClock = true
            }
            video = VideoPlayerFFMPEG.fromFile(src.path, configuration =  vc).apply {
                play()
            }

            cb = colorBuffer(video!!.width, video!!.height)
            video!!.newFrame.listen {
                it.frame.copyTo(cb!!)
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

fun main() = application {
    program {
        val al = AssetLoader()
        println(al.thumbnailsToClips.map { it.key.src.name to it.value.map { it.src.name }.size })
    }
}