import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.pow
import kotlin.math.sqrt


object Utils {

    fun resizeImage(bufferedImage: BufferedImage, width: Int, height: Int): BufferedImage {
        val outputImage = BufferedImage(width, height, bufferedImage.type)
        val g = outputImage.createGraphics()
        g.drawImage(bufferedImage, 0, 0, width, height, null)
        g.dispose()

        return outputImage
    }

//    fun getImage(filename: String): Array<Array<Color>>? {
//        try {
//            val img = ImageIO.read(File(filename))
//            return convertImageToArr(img)
//        } catch (e: IOException) {
//            println(e)
//        }
//        return null
//    }
//
//    fun saveImage(rgb: Array<Array<Color>>, filename: String) {
//        val img = convertArrToImage(rgb)
//        try {
//            val outputFile = File(filename)
//            ImageIO.write(img, filename.substring(filename.length - 3), outputFile)
//        } catch (e: IOException) {
//            println(e)
//        }
//    }

    fun convertImageToArr(img: BufferedImage): Array<Array<Color>> {
        val width = img.width
        val height = img.height
        val rgb: Array<Array<Color>> = Array(height) { Array(width) { Color(0, 0, 0, 0) } }
        for (y in 0 until height) {
            for (x in 0 until width) {
                rgb[y][x] = Color(img.getRGB(x, y), true)
            }
        }
        return rgb
    }

    fun convertArrToImage(rgb: Array<Array<Color>>, imageType: Int): BufferedImage {
        val width = rgb[0].size
        val height = rgb.size
        val img = BufferedImage(width, height, imageType)
        for (y in 0 until height) {
            for (x in 0 until width) {
                img.setRGB(x, y, rgb[y][x].rgb)
            }
        }
        return img
    }

    fun generateReductionColorList(
        rgb: Array<Array<Color>>,
        processTakeCount: Int,
        colorProcessCallback: ((rankColorCountPairList: List<Pair<Color, Int>>, afterColorCountPairList: List<Pair<Color, Int>>) -> Unit)? = null
    ): List<Color> {
        // sort by frequently  then take bought colorCount
        val colorMap = hashMapOf<Int, Int>()   // color, count
        val width = rgb[0].size
        val height = rgb.size
        for (y in 0 until height) {
            for (x in 0 until width) {
                val key = rgb[y][x].rgb
                val prevCount = colorMap.getOrPut(key) { 0 }
                colorMap[key] = prevCount + 1
            }
        }

        val threshCount = 150

        // prefix flow  if list item has close value with front, second images, so that merge them and increment count
        val std = colorMap.toList().map { it.second }.std()
        val distThreshold = (std / threshCount)
        println("std is $std distThresh $distThreshold")

        val sortedFreqList = colorMap.toList().sortedByDescending { it.second }.take(threshCount).toMutableList()

        // don't look only above cell   loop all above
        sortedFreqList.forEachIndexed { index, pair ->
            if (index == 0) return@forEachIndexed

            for (i in 1..sortedFreqList.size) {
                if (i >= index) break

                // find non-zero prevPair
                val prevPair = sortedFreqList[index - i]
                if (prevPair.second == 0) continue

                // if current value is close to previous value   remove this key then add count to previous map key
                val colorDistance = getColorDistance(Color(prevPair.first), Color(pair.first))
                if (colorDistance < distThreshold) {
                    colorMap[prevPair.first] = colorMap[prevPair.first]!! + colorMap[pair.first]!!
                    colorMap[pair.first] = 0
                }
            }
        }

        val remainColorPairList = colorMap.toList().sortedByDescending { it.second }

        colorProcessCallback?.invoke(
            sortedFreqList.map { Color(it.first) to it.second }.take(processTakeCount),
            remainColorPairList.map { Color(it.first) to it.second }.take(processTakeCount)
        )

        return remainColorPairList.map { Color(it.first) }
    }

    fun reductionArrColors(
        rgb: Array<Array<Color>>,
        colorList: List<Color>,
        colorCount: Int
    ): Array<Array<Color>> {
        val useColorList = colorList.take(colorCount)

        println("useColorList count ${useColorList.size}")

        val width = rgb[0].size
        val height = rgb.size

        for (y in 0 until height) {
            for (x in 0 until width) {
                rgb[y][x] = getInsteadColor(rgb[y][x], useColorList)
            }
        }

        return rgb
    }

    private fun getInsteadColor(src: Color, colorList: List<Color>): Color {
        val colorDistList = mutableListOf<Pair<Color, Double>>()

        colorList.forEach {
            colorDistList.add(it to getColorDistance(src, it))
        }

        return colorDistList.minByOrNull { it.second }!!.first
    }

    fun getColorDistance(color1: Color, color2: Color): Double {
        val rmean = ((color1.red + color2.red) / 2).toDouble()
        val r = color1.red - color2.red
        val g = color1.green - color2.green
        val b = color1.blue - color2.blue
        val weightR = 2 + rmean / 256
        val weightG = 4.0
        val weightB = 2 + (255 - rmean) / 256
        return sqrt(weightR * r * r + weightG * g * g + weightB * b * b)
    }
}

fun IntArray.std(): Double {
    val std = this.fold(0.0) { a, b -> a + (b - this.average()).pow(2) }
    return sqrt(std / 10)
}

fun List<Int>.std(): Double {
    val std = this.fold(0.0) { a, b -> a + (b - this.average()).pow(2) }
    return sqrt(std / 10)
}