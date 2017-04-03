package io.metjka.vortex.ui.connections

import io.metjka.vortex.ui.ComponentLoader
import io.metjka.vortex.ui.NodeBlockContainer
import io.metjka.vortex.ui.Type
import io.metjka.vortex.ui.blocks.NodeBlock
import javafx.fxml.FXML
import javafx.geometry.Point2D
import javafx.scene.shape.Circle
import java.util.*

class InputDot<T>(block: NodeBlock, image: Type) : ConnectionDot<T>(block), ComponentLoader {

    @FXML
    lateinit var circle: Circle

    init {
        loadFXML("InputDot")

    }

    override fun hasConnection(): Boolean {
        return connection.isPresent
    }

    override fun getAttachmentPoint(): Point2D {
        return topLevelPane.sceneToLocal(localToScene(Point2D(0.0, 0.0)))
    }

    override fun getContainer(): NodeBlockContainer {
        return block.topLevelPane
    }

    override fun removeConnections() {
        if (connection.isPresent) {
            val con = connection.get()
            connection = Optional.empty()
            con.remove()
        }
    }

    fun onUpdate() {
        block.update()
    }

}