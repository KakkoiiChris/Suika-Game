package kakkoiichris.suika
import kakkoiichris.hypergame.input.Input
import kakkoiichris.hypergame.media.Colors
import kakkoiichris.hypergame.media.Renderable
import kakkoiichris.hypergame.media.Renderer
import kakkoiichris.hypergame.state.StateManager
import kakkoiichris.hypergame.util.Time
import kakkoiichris.hypergame.util.math.Box
import kakkoiichris.hypergame.view.Sketch
import kakkoiichris.hypergame.view.View
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints

fun main() {
    val game = Game()

    game.open()
}

class Game : Sketch(1280, 720, "スイカゲーム") {
    private lateinit var jar: Box

    private val fruits = mutableListOf<Fruit>()

    override fun swapTo(view: View) {
        view.renderer.setRenderingHints(
            mapOf(
                RenderingHints.KEY_ANTIALIASING to RenderingHints.VALUE_ANTIALIAS_ON,
                RenderingHints.KEY_TEXT_ANTIALIASING to RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            )
        )

        view.renderer.stroke = BasicStroke(8F)

        jar = Box(view.width / 3.0, view.height / 4.0, view.width / 3.0, 2.75 * (view.height / 4.0))

        var n = 5.0

        for (type in Fruit.Type.entries) {
            fruits += Fruit(n, n, Fruit.Type.random())

            n += 15
        }
    }

    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
    }

    override fun render(view: View, renderer: Renderer) {
        renderer.color = Colors.CSS.goldenrod
        renderer.fillRect(view.bounds)

        fruits.forEach { it.render(view, renderer) }

        renderer.color = Colors.CSS.lightgoldenrodyellow
        renderer.drawRect(jar)
    }
}

class Fruit(x: Double, y: Double, val type: Type) : Box(x, y, type.diameter, type.diameter), Renderable {
    override fun update(view: View, manager: StateManager, time: Time, input: Input) {

    }

    override fun render(view: View, renderer: Renderer) {
        renderer.color = type.color
        renderer.fillOval(this)

        renderer.color = type.color.darker()
        renderer.drawOval(this)
    }

    enum class Type(rgb: Int) {
        Sakuranbo(0x7F1F00),
        Ichigo(0xFF0000),
        Budou(0x7F00FF),
        Dekopon(0xFFAF00),
        Mikan(0xFF7F00),
        Ringo(0xFF003F),
        Nashi(0xAFFF00),
        Momo(0xFF7FAF),
        Pain(0xFFFF00),
        Meron(0x7FFF00),
        Suika(0x00AF00);

        val color = Color(rgb)

        val radius get() = (ordinal * 9.0) + 12

        val diameter get() = radius * 2

        companion object{
            fun random() = entries.random()
        }
    }
}