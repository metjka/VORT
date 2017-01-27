package com.metjka.vort.ui.components.blocks

import com.google.common.collect.ImmutableList
import com.metjka.vort.precessing.BlurProcessing
import com.metjka.vort.precessing.FastImage
import com.metjka.vort.precessing.Kernel
import com.metjka.vort.ui.ToplevelPane
import com.metjka.vort.ui.Type
import com.metjka.vort.ui.components.connections.InputAnchor
import com.metjka.vort.ui.components.connections.OutputAnchor
import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.layout.VBox
import mu.KotlinLogging
import rx.Single
import rx.schedulers.Schedulers
import java.util.*

class BlurBlock(toplevelPane: ToplevelPane) : ValueBlock<FastImage>(toplevelPane, "BlurBlock") {

    val log = KotlinLogging.logger { }

    val inputAnchor: InputAnchor = InputAnchor(this, Type.IMAGE)
    val outputAnchor: OutputAnchor = OutputAnchor(this, 0, Type.IMAGE)

    var method: Method? = null

    enum class Method {
        BOX_BLUR, GAUSSIAN_3, GAUSSIAN_5, SOBEL, SHARPEN, SEPIA
    }

    @FXML
    var methodComboBox: ComboBox<String>? = null

    @FXML
    var inputSpace: VBox? = null

    @FXML
    var outputSpace: VBox? = null

    init {
        inputSpace?.children?.add(0, inputAnchor)
        outputSpace?.children?.add(outputAnchor)

        methodComboBox?.selectionModel?.select(0)
        method = Method.BOX_BLUR

        methodComboBox?.selectionModel?.selectedItemProperty()?.addListener { observableValue, oldValue, newValue ->
            method = getMethod(newValue)
            update()
        }

    }

    private fun getMethod(method: String): Method {
        when (method) {
            "BOX BLUR" -> return Method.BOX_BLUR
            "GAUSSIAN3" -> return Method.GAUSSIAN_3
            "GAUSSIAN5" -> return Method.GAUSSIAN_5
            "SHARPEN" -> return Method.SHARPEN
            "SOBEL" -> return Method.SOBEL
            "SEPIA" -> return Method.SEPIA
            else -> throw IllegalArgumentException("Wrong method name!")
        }
    }

    fun getKernel(): Kernel {
        when (method) {
            Method.BOX_BLUR -> return BlurProcessing.BOX_BLUR
            Method.GAUSSIAN_3 -> return BlurProcessing.GAUSSIAN3_BLUR
            Method.GAUSSIAN_5 -> return BlurProcessing.GAUSSIAN5_BLUR
            Method.SHARPEN -> return BlurProcessing.SHARPEN
            Method.SOBEL -> return BlurProcessing.SOBEL
            Method.SEPIA -> return BlurProcessing.SEPIA
            else -> throw IllegalArgumentException("Wrong method!")
        }
    }

    override fun update() {
        inputAnchor.invalidateVisualState()
        outputAnchor.invalidateVisualState()
        log.info("On update in BlurBox: {}", hashCode())


        if (inputAnchor.oppositeAnchor.isPresent) {
            val oppositeAnchor = inputAnchor.oppositeAnchor.get()
            val block = oppositeAnchor.block
            if (block is ValueBlock<*>) {
                val value = block.getValue(oppositeAnchor.position) as FastImage
                val blurProcessing = BlurProcessing(value)
                Single.fromCallable { blurProcessing.blur(getKernel()) }
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.trampoline())
                        .subscribe(
                                { image ->
                                    log.info("Sending message downstream from BlurBlock: {}", hashCode())
                                    value1 = image
                                    sendUpdateDownSteam()

                                },
                                { log.error("Can`t process image!", it) }
                        )
            }

        }
    }

    override fun getAllInputs(): MutableList<InputAnchor> {
        return ImmutableList.of(inputAnchor)
    }

    override fun getAllOutputs(): MutableList<OutputAnchor> {
        return ImmutableList.of(outputAnchor)
    }

    override fun getValue(position: Int): FastImage {
        when (position) {
            0 -> return value1
        }
        throw IllegalArgumentException("Wrong position!")
    }

    override fun getNewCopy(): Optional<Block> {
        throw UnsupportedOperationException("not implemented")
    }
}
