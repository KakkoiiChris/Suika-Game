package kakkoiichris.suika

import kakkoiichris.hypergame.input.Button
import kakkoiichris.hypergame.input.Input
import kakkoiichris.hypergame.media.Colors
import kakkoiichris.hypergame.media.Renderable
import kakkoiichris.hypergame.media.Renderer
import kakkoiichris.hypergame.state.StateManager
import kakkoiichris.hypergame.util.Time
import kakkoiichris.hypergame.util.math.Box
import kakkoiichris.hypergame.util.math.Vector
import kakkoiichris.hypergame.util.math.clamp
import kakkoiichris.hypergame.view.Sketch
import kakkoiichris.hypergame.view.View
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints.*

fun main() {
    val game = Game()

    game.open()
}

class Game : Sketch(1280, 720, "スイカゲーム") {
    private lateinit var jar: Box

    private val fruits = mutableListOf<Fruit>()

    private var heldFruit = Fruit(0.0, 0.0, Fruit.Type.Sakuranbo)

    private lateinit var dropPos: Vector

    override fun swapTo(view: View) {
        with(view.renderer) {
            setRenderingHints(
                mapOf(
                    KEY_ANTIALIASING to VALUE_ANTIALIAS_ON,
                    KEY_TEXT_ANTIALIASING to VALUE_TEXT_ANTIALIAS_ON
                )
            )

            stroke = BasicStroke(8F)
        }

        jar = Box(view.width / 3.0, view.height / 4.0, view.width / 3.0, 2.75 * (view.height / 4.0))

        dropPos = Vector(jar.centerX, view.height / 8.0)
    }

    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        if (input.buttonDown(Button.LEFT)) {
            fruits += heldFruit

            heldFruit = Fruit(0.0, 0.0, Fruit.Type.Sakuranbo)
        }

        fruits.forEach {
            it.update(view, manager, time, input)

            if (it.bottom > jar.bottom) {
                it.bottom = jar.bottom
            }

            if (it.left < jar.left) {
                it.left = jar.left
            }

            if (it.right > jar.right) {
                it.right = jar.right
            }
        }

        for (ia in fruits.indices) {
            for (ib in fruits.indices.drop(ia + 1)) {
                val a = fruits[ia].takeIf { !it.removed } ?: continue
                val b = fruits[ib].takeIf { !it.removed } ?: continue

                if (a.intersects(b) && a.type === b.type) {
                    a.removed = true
                    b.removed = true

                    val center = (a.position + b.position) / 2.0

                    fruits.add(Fruit(center.x, center.y, a.type.next()))
                }
            }
        }

        fruits.removeIf(Fruit::removed)

        dropPos.x = input.mouse.x.clamp(jar.left + heldFruit.type.radius, jar.right - heldFruit.type.radius)

        heldFruit.center = dropPos
    }

    override fun render(view: View, renderer: Renderer) {
        renderer.color = Colors.CSS.goldenrod
        renderer.fillRect(view.bounds)

        renderer.color = Colors.white
        renderer.drawLine(dropPos, dropPos.copy(y = jar.bottom))

        heldFruit.render(view, renderer)

        fruits.forEach { it.render(view, renderer) }

        renderer.color = Colors.CSS.lightgoldenrodyellow
        renderer.drawRect(jar)
    }
}

class Fruit(x: Double, y: Double, val type: Type) : Box(x, y, type.diameter, type.diameter), Renderable {
    private var velocity = Vector()

    var removed = false

    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        position += velocity
        velocity += gravity * time.delta
    }

    override fun render(view: View, renderer: Renderer) {
        renderer.color = type.color
        renderer.fillOval(this)

        renderer.color = type.color.darker()
        renderer.drawOval(this)
    }

    companion object {
        val gravity = Vector(0.0, 0.4)
        fun random() =
            Fruit(0.0, 0.0, Type.random())
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

        fun next() =
            if (this === Suika)
                Suika
            else
                entries[ordinal + 1]

        companion object {
            fun random() = entries.random()
        }
    }
}