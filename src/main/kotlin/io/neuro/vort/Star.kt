package io.neuro.vort

import java.util.concurrent.ThreadLocalRandom

class Star(fastABGRRGB: FastABGRImage) : ProcessingFilter(fastABGRRGB) {

    val fastGRB = fastABGRRGB

    val random: ThreadLocalRandom = ThreadLocalRandom.current()

    fun spiralGalaxy(number: Int, centerX: Int, centerY: Int, centerDensity: Int, edgeDensity: Int,
                     cycles: Int, spiralHarms: Int, powerFrom: Int, powerTo: Int, radiusFrom: Int, radiusTo: Int) {

        val num = 2.0 * Math.PI / spiralHarms

        var startAngle = 0.0

        for (i: Int in 1..spiralHarms) {
            for (j: Int in 0..(number / i)) {

                val power: Double = random.nextDouble(powerFrom, powerTo)
                val radius = random.nextInt(radiusFrom, radiusTo)

                val pair: Pair<Int, Int> = spiralDistortion(centerX, centerY, centerDensity, edgeDensity, cycles, startAngle, 0.05)
                addStar(pair.first, pair.second, power, radius)
                startAngle += num
            }
        }
    }

    private fun addStar(x: Int, y: Int, power: Double, radius: Int) {
        val num1 = Math.min(Math.min(4 * (power * radius).toInt(), (this.fastGRB.width - 1) / 2), (this.fastGRB.height - 1) / 2)
        for (i: Int in (x - num1)..(x + num1) - 1) {
            for (j: Int in (y - num1)..(y + num1) - 1) {
                val sx = this.seamlessCoordination(i, fastGRB.width)
                val sy = this.seamlessCoordination(j, fastGRB.height)
                val val1_1: Double = Math.abs(sx - x).toDouble()
                val val1_2: Double = Math.abs(sy - y).toDouble()
                val val2_1: Double = fastGRB.width - val1_1
                val val2_2: Double = fastGRB.height - val1_2
                var num2 = Math.sqrt(Math.pow(Math.min(val1_1, val2_1), 2.0) + Math.pow(Math.min(val1_2, val2_2), 2.0))
                if (num2 < 0.001) {
                    num2 = 0.001
                }
                addPixel(sx, sy, ((power * radius) / num2))
            }
        }
    }

    private fun spiralDistortion(x: Int, y: Int, centerDensity: Int, edgeDensity: Int, cycles: Int, startAngle: Double, perturbation: Double): Pair<Int, Int> {
        val val1: Double = Math.PI * cycles * 2.0
        val val2: Double = random.nextDouble() * val1
        val val3: Double = centerDensity + edgeDensity * val2
        val val4: Double = val3 + random.nextDouble() * (val3 * perturbation)
        val val5: Double = startAngle + val2
        val val6: Int = (val4 * Math.cos(val5)).toInt()
        val val7: Int = (val4 * Math.sin(val5)).toInt()

        val valX = seamlessCoordination(x + val6, fastGRB.width - 1)
        val valY = seamlessCoordination(y + val7, fastGRB.height - 1)

        val pair = Pair<Int, Int>(valX, valY)
        return pair
    }

    private fun seamlessCoordination(i: Int, side: Int): Int {
        var num = i % side
        if (num < 0) {
            num += side
        }
        return num
    }

}