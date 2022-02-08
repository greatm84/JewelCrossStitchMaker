import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO


object Utils {

    fun resizeImage(bufferedImage: BufferedImage, width: Int, height: Int): BufferedImage {
        val outputImage = BufferedImage(width, height, bufferedImage.type)
        val g = outputImage.createGraphics()
        g.drawImage(bufferedImage, 0, 0, width, height, null)
        g.dispose()

        return outputImage
    }

    fun getImage(filename: String): Array<Array<Color>>? {
        try {
            val img = ImageIO.read(File(filename))
            return convertImageToArr(img)
        } catch (e: IOException) {
            println(e)
        }
        return null
    }

    fun saveImage(rgb: Array<Array<Color>>, filename: String) {
        val img = convertArrToImage(rgb)
        try {
            val outputFile = File(filename)
            ImageIO.write(img, filename.substring(filename.length - 3), outputFile)
        } catch (e: IOException) {
            println(e)
        }
    }

    fun convertImageToArr(img: BufferedImage): Array<Array<Color>> {
        val width = img.width
        val height = img.height
        val rgb: Array<Array<Color>> = Array(height) { Array(width) { Color.BLACK } }
        for (y in 0 until height) {
            for (x in 0 until width) {
                rgb[y][x] = Color(img.getRGB(x, y))
            }
        }
        return rgb
    }

    fun convertArrToImage(rgb: Array<Array<Color>>): BufferedImage {
        val width = rgb[0].size
        val height = rgb.size
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                img.setRGB(x, y, rgb[y][x].rgb)
            }
        }
        return img
    }

    fun reductionArrColors(rgb: Array<Array<Color>>, colorRange: Int): Array<Array<Color>> {
        val stepSize = 255 / colorRange + 1
        val stepList = mutableListOf(0)

        var i = 2
        var nextStep = stepSize
        while (nextStep <= 255) {
            stepList.add(nextStep)
            nextStep = stepSize * i++
        }
        stepList.add(255)

        val rStepList = stepList.asReversed()

        val width = rgb[0].size
        val height = rgb.size
        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = getRangeColor(rgb[y][x].red, rStepList)
                val g = getRangeColor(rgb[y][x].green, rStepList)
                val b = getRangeColor(rgb[y][x].blue, rStepList)
                rgb[y][x] = Color(r, g, b)
            }
        }
        return rgb
    }

    private fun getRangeColor(originValue: Int, rStepList: List<Int>): Int {
        rStepList.forEach {
            if (originValue >= it) {
                return it
            }
        }
        return 0
    }

    private fun getRangeColor(originValue: Int, range: Int): Int {
        originValue / range
        val first = originValue * range / 255
        return first * 255 / range
    }
}