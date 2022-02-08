import java.awt.Color
import kotlin.math.pow

object Pixelator {

    private var pixeled: Array<Array<Color>>? = null

    fun pixelate(rgb: Array<Array<Color>>, newPixelSize: Int, flattenAmount: Int): Array<Array<Color>> {
        val width = rgb[0].size
        val height = rgb.size

        pixeled = Array(height) { Array(width) { Color.BLACK } }
        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                if (x + newPixelSize > width && y + newPixelSize > height) {
                    setAverage(x, y, newPixelSize, width, height, rgb, flattenAmount)
                } else if (x + newPixelSize > width) {
                    setAverage(x, y, newPixelSize, width, y + newPixelSize, rgb, flattenAmount)
                } else if (y + newPixelSize > height) {
                    setAverage(x, y, newPixelSize, x + newPixelSize, height, rgb, flattenAmount)
                } else {
                    setAverage(
                        x,
                        y,
                        newPixelSize,
                        x + newPixelSize,
                        y + newPixelSize,
                        rgb,
                        flattenAmount
                    )
                }
                x += newPixelSize
            }
            y += newPixelSize
        }
        return pixeled!!
    }

    private fun setAverage(
        x: Int,
        y: Int,
        newPixelSize: Int,
        xBound: Int,
        yBound: Int,
        rgb: Array<Array<Color>>,
        flattenAmount: Int
    ) {
        var rav = 0
        var gav = 0
        var bav = 0
        for (sy in y until yBound) {
            for (sx in x until xBound) {
                rav += rgb[sy][sx].red
                gav += rgb[sy][sx].green
                bav += rgb[sy][sx].blue
            }
        }
        val pixelSizeSquared = newPixelSize * newPixelSize
        rav /= pixelSizeSquared
        gav /= pixelSizeSquared
        bav /= pixelSizeSquared
        for (sy in y until yBound) {
            for (sx in x until xBound) {
//                pixeled!![sy][sx] = getFlatColor(rav, gav, bav, flattenAmount)
                pixeled!![sy][sx] = Color(rav, gav, bav)
            }
        }
    }

    private fun getFlatColor(r: Int, g: Int, b: Int, flattenAmount: Int): Color {
        return if (flattenAmount in 1..7) {
            val roundingFactor = 2.0.pow((flattenAmount + 1).toDouble()).toInt()
            var rRound = (r.toDouble() / roundingFactor.toDouble() + 0.5).toInt() * roundingFactor
            var gRound = (g.toDouble() / roundingFactor.toDouble() + 0.5).toInt() * roundingFactor
            var bRound = (b.toDouble() / roundingFactor.toDouble() + 0.5).toInt() * roundingFactor
            if (rRound != 0) {
                rRound--
            }
            if (gRound != 0) {
                gRound--
            }
            if (bRound != 0) {
                bRound--
            }
            Color(rRound, gRound, bRound)
        } else {
            Color(r, g, b)
        }
    }
}