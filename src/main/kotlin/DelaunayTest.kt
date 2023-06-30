import org.openrndr.application

fun main() = application {
    configure { }
    program {
        extend {
            drawer.circle(drawer.bounds.center, 100.0)
        }
    }
}