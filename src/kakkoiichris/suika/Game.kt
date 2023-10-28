package kakkoiichris.suika

import kakkoiichris.hypergame.input.Button
import kakkoiichris.hypergame.input.Input
import kakkoiichris.hypergame.media.Colors
import kakkoiichris.hypergame.media.Renderable
import kakkoiichris.hypergame.media.Renderer
import kakkoiichris.hypergame.state.StateManager
import kakkoiichris.hypergame.util.Time
import kakkoiichris.hypergame.util.math.Box
import kakkoiichris.hypergame.util.math.QuadTree
import kakkoiichris.hypergame.util.math.Vector
import kakkoiichris.hypergame.util.math.clamp
import kakkoiichris.hypergame.view.Sketch
import kakkoiichris.hypergame.view.View
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints.*
import kotlin.random.Random

typealias Collision = Pair<Fruit, Fruit>

fun main() {
    val game = Game()

    game.open()
}

class Game : Sketch(1280, 720, "スイカゲーム") {
    private lateinit var jar: Box

    private val fruits = mutableListOf<Fruit>()

    private lateinit var heldFruit: Fruit

    private val collisions = mutableListOf<Collision>()

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

        heldFruit = Fruit(Vector(jar.centerX, view.height / 8.0), Fruit.Type.Sakuranbo, jar)
    }

    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        if (input.buttonDown(Button.LEFT)) {
            fruits += heldFruit

            heldFruit = Fruit.random(heldFruit.bounds.center, jar)
        }

        heldFruit.position.x = input.mouse.x.clamp(jar.left + heldFruit.type.radius, jar.right - heldFruit.type.radius)

        val tree = QuadTree<Fruit>(jar)

        fruits.forEach {
            it.update(view, manager, time, input)

            tree.insert(it)
        }

        collisions.clear()

        for (a in fruits) {
            val near = tree.queryPosition(a.bounds.copy().apply { resize(a.type.radius * 2) })

            for (b in near) {
                if (a === b) continue

                if (a.isColliding(b)) {
                    collisions += a to b

                    val distance = a.distanceTo(b)
                    val overlap = (distance - a.type.radius - b.type.radius) * .5
                    val diff = (a.position - b.position) * overlap / distance

                    a.position -= diff
                    b.position += diff
                }
            }
        }

        collisions.forEach { (a, b) ->
            val normal = a.position.normalTo(b.position)
            val tangent = normal.tangent

            val dna = a.velocity dot normal
            val dnb = b.velocity dot normal
            val dta = a.velocity dot tangent
            val dtb = b.velocity dot tangent

            val ma = (dna * (a.type.mass - b.type.mass) + 2 * b.type.mass * dnb) / (a.type.mass + b.type.mass)
            val mb = (dnb * (b.type.mass - a.type.mass) + 2 * a.type.mass * dna) / (a.type.mass + b.type.mass)

            a.velocity = (tangent * dta + normal * ma) * 0.9
            b.velocity = (tangent * dtb + normal * mb) * 0.9

            if (a.type === b.type && !(a.removed || b.removed)) {
                val center = (a.bounds.center + b.bounds.center) / 2.0

                fruits.add(Fruit(center, a.type.next(), jar))

                a.removed = true
                b.removed = true
            }
        }

        fruits.removeIf(Fruit::removed)
    }

    override fun render(view: View, renderer: Renderer) {
        renderer.color = Colors.CSS.goldenrod
        renderer.fillRect(view.bounds)

        renderer.color = Colors.white
        renderer.drawLine(heldFruit.position, heldFruit.position.copy(y = jar.bottom))

        heldFruit.render(view, renderer)

        fruits.forEach { it.render(view, renderer) }

        renderer.color = Colors.CSS.lightgoldenrodyellow
        renderer.drawRect(jar)
    }
}

class Fruit(
    override var position: Vector,
    val type: Type,
    private val jar: Box
) : Renderable, QuadTree.Element {
    override val bounds
        get() = Box(position.x - type.radius, position.y - type.radius, type.radius * 2, type.radius * 2)

    var velocity = Vector()

    private var acceleration = Vector()

    var removed = false

    var entered = false

    fun distanceTo(other: Fruit) =
        position.distanceTo(other.position)

    fun isColliding(other: Fruit) =
        (position.x - other.position.x) * (position.x - other.position.x) + (position.y - other.position.y) * (position.y - other.position.y) <= (type.radius + other.type.radius) * (type.radius + other.type.radius)

    override fun update(view: View, manager: StateManager, time: Time, input: Input) {
        position += velocity
        velocity += acceleration * time.delta

        if (position.x < jar.left + type.radius) {
            position.x = jar.left + type.radius
            velocity.x *= -0.25
        }

        if (position.x > jar.right - type.radius) {
            position.x = jar.right - type.radius
            velocity.x *= -0.25
        }

        if (position.y > jar.bottom - type.radius) {
            position.y = jar.bottom - type.radius
            velocity.y *= 0
        }

        if (jar.intersects(bounds)) {
            entered = true
        }

        if (entered && !jar.intersects(bounds)) {
            println("GAME OVER")

            view.close()
        }

        if (velocity.magnitude < .01) velocity.zero()

        acceleration = -velocity * .01 + gravity
    }

    override fun render(view: View, renderer: Renderer) {
        renderer.color = type.color
        renderer.fillOval(bounds)

        renderer.color = type.color.darker()
        renderer.drawOval(bounds)
    }

    companion object {
        val gravity = Vector(0.0, 0.4)

        fun random(position: Vector, jar: Box) =
            Fruit(position, Type.entries[Random.nextInt(5)], jar)
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

        val mass get() = (ordinal * 5.0) + 10

        fun next() =
            if (this === Suika)
                Suika
            else
                entries[ordinal + 1]
    }
}