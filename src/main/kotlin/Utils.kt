import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
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

    fun generateColorMap(rgb: Array<Array<Color>>): HashMap<Int, Int> {
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
        return colorMap
    }

    fun generateReductionColorList(
        srcColorMap: HashMap<Int, Int>,
        distThreshold: Int,
        colorProcessCallback: ((rankColorCountPairList: List<Pair<Color, Int>>, afterColorCountPairList: List<Pair<Color, Int>>) -> Unit)? = null
    ): List<Color> {
        val colorMap = srcColorMap.toMutableMap()

        val sortedFreqList = colorMap.toList().sortedByDescending { it.second }

        // don't look only above cell   loop all above
        sortedFreqList.forEachIndexed { index, pair ->
            val key = pair.first
            val count = colorMap[pair.first]
            if (count == 0) {
                return@forEachIndexed
            }
            if (index == sortedFreqList.size - 1) {
                return@forEachIndexed
            }

            for (i in index + 1 until sortedFreqList.size) {

                // find non-zero prevPair
                val nextKey = sortedFreqList[i].first
                val nextCount = colorMap[nextKey]
                if (nextCount == 0) continue

                // if current value is close to previous value   remove this key then add count to previous map key
                val colorDistance = getColorDistance(Color(nextKey), Color(key))
                if (colorDistance < distThreshold) {
                    colorMap[key] = count!! + nextCount!!
                    colorMap[nextKey] = 0
                }
            }
        }

        val remainColorPairList = colorMap.toList().filter { it.second > 0 }.sortedByDescending { it.second }

        colorProcessCallback?.invoke(
            sortedFreqList.map { Color(it.first) to it.second },
            remainColorPairList.map { Color(it.first) to it.second }
        )

        return remainColorPairList.map { Color(it.first) }
    }

    fun reductionArrColors(
        rgb: Array<Array<Color>>,
        useColorList: List<Color>,
    ): Array<Array<Color>> {
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

    fun deepCopy(bi: BufferedImage): BufferedImage {
        val cm: ColorModel = bi.colorModel
        val isAlphaPremultiplied: Boolean = cm.isAlphaPremultiplied()
        val raster = bi.copyData(null)
        return BufferedImage(cm, raster, isAlphaPremultiplied, null)
    }

    fun generateColorLabelToImage(bi: BufferedImage, colorRankList: List<Int>): BufferedImage {
        val labelList = mutableListOf<Char>()
        labelList.addAll(('A'..'Z'))
        labelList.addAll(('a'..'z'))
        val colorLabelList = mutableListOf<Pair<Int, String>>()
        colorRankList.take(labelList.size).forEachIndexed { index, color ->
            colorLabelList.add(color to labelList[index].toString())
        }
        val colorMap = colorLabelList.toMap()

        val newImage = deepCopy(bi)
        val font = Font("Arial", Font.PLAIN, 7)

        val g = newImage.graphics
        g.font = font

        val width = bi.width
        val height = bi.height
        val yOffset = 8
        for (y in 0 until height step 8) {
            for (x in 0 until width step 8) {
                val label = colorMap[bi.getRGB(x, y)] ?: continue
                // TODO get color is wrong
                g.color = Color.WHITE
                g.drawString(label, x + 1, y + yOffset)
                g.color = Color.BLACK
                g.drawString(label, x, y - 1 + yOffset)
            }
        }

        return newImage
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