import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.*
import org.openrndr.extra.color.presets.ORANGE
import org.openrndr.extra.fx.Post
import org.openrndr.extra.fx.color.ColorCorrection
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.shape.IntRectangle
import java.io.File

fun Program.wet(gui: Boolean = false) {

    val cb = colorBuffer(width, height)

    var treat: (img: ColorBuffer)->Unit by this.userProperties
    treat = { img ->
        img.copyTo(cb, sourceRectangle = IntRectangle(0, 0, 480, 480),
                        targetRectangle = IntRectangle(0, 0, width, height))
    }

    val paramsPath = File("gui-parameters/wet-latest.json")
    val g = GUI()
    val params = object {

        @BooleanParameter("threshold")
        var th = true

        @BooleanParameter("cc")
        var cc = true

    }.addTo(g)

    val threshold = ColorMoreThan().addTo(g).apply {
        background = ColorRGBa.GRAY
        foreground = rgb(0.118, 0.529, 0.827, a = 1.0)
    }
    val colorcorr = ColorCorrection().addTo(g).apply {
        brightness = 0.677
        contrast = -0.031
        saturation = 1.0
        gamma = 1.522
        hueShift = 1.118
    }

    this.ended.listen {
        g.saveParameters(paramsPath)
    }

    extend(g)
    extend(Post()) {
        post { input, output ->
            val int = intermediate[0]
            if(params.cc) {
                colorcorr.apply(input, int)
                if(params.th) {
                    threshold.apply(int, output)
                } else {
                    int.copyTo(output)
                }
            } else {
                input.copyTo(output)
            }
        }
    }
    extend {

        g.visible = gui

        drawer.image(cb)
    }
}


val colorRangeShader = """
       
          in vec2 v_texCoord0;
          uniform sampler2D tex0;
          uniform vec4 background;
          uniform vec4 foreground;
          out vec4 o_color;
        
          void main() {
              vec3 c = vec3(0.4);
              vec3 fill = texture(tex0, v_texCoord0).xyz;
              
              if(any(lessThan(fill, foreground.xyz))) {
                    c = fill.xyz;
              } else {
                    c = background.xyz;
              }
              
              o_color = vec4(c, 1.0);
          }
        
""".trimIndent()

@Description("ColorMoreThan")
class ColorMoreThan: Filter1to1(filterShaderFromCode(colorRangeShader, "color-range-shader")) {

    @ColorParameter("foreground")
    var foreground: ColorRGBa by parameters
    var background: ColorRGBa by parameters

    init {
        foreground = ColorRGBa.ORANGE
        background = ColorRGBa.BLACK
    }

}