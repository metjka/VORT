package io.metjka.vortex.precessing

import java.awt.Color

abstract class ProcessingFilter(fastImage: FastImage) {

    val width = fastImage.width
    val height = fastImage.height

    val fa = fastImage

    val pixels = DoubleArray(width * height)

    fun setPixel(x: Int, y: Int, rgb: Double) {
        pixels[x + y * width] = rgb
    }

    fun getPixel(x: Int, y: Int): Double {
        return pixels[x + y * width]
    }

    fun addPixel(x: Int, y: Int, rgb: Double) {
        val pixel: Double = getPixel(x, y)
        setPixel(x, y, pixel + rgb)
    }

    fun gray(): IntArray {
        val argbArray: IntArray = IntArray(width * height)
        for (i: Int in 0..pixels.size - 1) {
            var f: Double = pixels[i]
            if (f < 0.0)
                f = 0.0
            if (f > 255)
                f = 255.0

            val rgb = Color(f.toInt(), f.toInt(), f.toInt(), 255).rgb
            argbArray[i] = rgb
        }
        return argbArray
    }

    fun color(colorList: List<Color>): IntArray {

        val argbArray = IntArray(width * height)
        for (i: Int in 0..pixels.size - 1) {
            var f: Double = pixels[i]
            if (f < 0.0)
                f = 0.0
            if (f > 255)
                f = 255.0

            val num2 = f / 255
            val num3 = (colorList.size - 1) * num2

            val index1: Int = num2.toInt()
            var index2 = index1 + 1
            if (index2 == colorList.size) {
                index2 -= 1
            }
            var value: Double
            if (num2 > index1) {
                value = num2 - Math.floor(num2).toInt()
            } else {
                value = num3 - index1
            }
            val r = (colorList[index1].red * (1.0 - value) + colorList[index2].red * value).toInt()
            val g = (colorList[index1].green * (1.0 - value) + colorList[index2].green * value).toInt()
            val b = (colorList[index1].blue * (1.0 - value) + colorList[index2].blue * value).toInt()

            argbArray[i] = Color(r, g, b, 255).rgb
        }
        return argbArray
    }


}
