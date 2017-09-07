package io.metjka.vortex.ui.connections

import io.metjka.vortex.ui.ComponentLoader
import io.metjka.vortex.ui.NodeBlockContainer
import io.metjka.vortex.ui.blocks.NodeBlock
import javafx.fxml.FXML
import javafx.geometry.Point2D
import javafx.scene.shape.Circle
import java.util.*

class OutputDot<T>(block: NodeBlock) : ConnectionDot(block), Target, ComponentLoader {
    @FXML
    lateinit var circle: Circle

    val typedValue: T? = null

    val connections: MutableList<Connection> = mutableListOf()

    init {
        loadFXML("InputDot")
    }

    fun getOppositeConnectionDots(): ArrayList<InputDot<*>> {
        val list = ArrayList<InputDot<*>>()
        for (c in this.connections) {
            c.endDot?.let {
                list.add(it)
            }
        }
        return list
    }

    fun addConnection(connection: Connection) {
        connections.add(connection)
    }

    fun dropConnection(connection: Connection) {
        if (this.connections.contains(connection)) {
            this.connections.remove(connection)
        }
    }

    override fun hasConnection(): Boolean {
        return connections.isNotEmpty()
    }

    override fun getAttachmentPoint(): Point2D {
        return topLevelPane.sceneToLocal(localToScene(Point2D(0.0, 0.0)))
    }

    override fun getContainer(): NodeBlockContainer {
        return block.topLevelPane
    }

    fun onUpdate() {
        block.update()
    }

    override fun removeConnections() {
        while (this.connections.isNotEmpty()) {
            val connection = this.connections.removeAt(0)
            connection.remove()
        }
    }

    override fun getAssociatedDot(): ConnectionDot {
        return this
    }
}
