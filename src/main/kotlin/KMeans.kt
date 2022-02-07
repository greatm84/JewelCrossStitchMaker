import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO


// https://gist.github.com/AliZafar120/baa9448e8d081f138e19a126957ecd34


class KMeans {

    companion object {
        val MODE_CONTINUOUS = 1
        val MODE_ITERATIVE = 2
    }

    lateinit var clusters: Array<Cluster?>

//    @JvmStatic
//    fun main(args: Array<String>) {
//
//        // parse arguments
//        val src = "C:\\Users\\User\\Desktop\\Image\\Input\\Lenna.png"
//        val k = 15
//        val m = "-1"
//        var mode = 1
//        if (m == "-c") {
//            mode = MODE_ITERATIVE
//        } else if (m == "-c") {
//            mode = MODE_CONTINUOUS
//        }
//
//        // create new KMeans object
//        val kmeans = KMeans()
//        // call the function to actually start the clustering
//        val dstImage: BufferedImage = kmeans.calculate(
//            loadImage(src),
//            k, mode
//        )
//        // save the resulting image ]
//        val frame = JFrame()
//        val lblimage = JLabel(ImageIcon(dstImage))
//        val mainPanel = JPanel(BorderLayout())
//        mainPanel.add(lblimage)
//        // add more components here
//        frame.add(mainPanel)
//        frame.isVisible = true
//        frame.setSize(300, 400)
//
//        //saveImage(dst, dstImage);
//    }

    fun calculate(
        image: BufferedImage?,
        k: Int, mode: Int
    ): BufferedImage {
        val start = System.currentTimeMillis()
        val w = image!!.width
        val h = image.height
        // create clusters
        clusters = createClusters(image, k)
        // create cluster lookup table
        val lut = IntArray(w * h)
        Arrays.fill(lut, -1)

        // at first loop all pixels will move their clusters
        var pixelChangedCluster = true
        // loop until all clusters are stable!
        var loops = 0
        while (pixelChangedCluster) {
            pixelChangedCluster = false
            loops++
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val pixel = image.getRGB(x, y)
                    val cluster = findMinimalCluster(pixel)
                    if (lut[w * y + x] != cluster!!.id) {
                        // cluster changed
                        if (mode == MODE_CONTINUOUS) {
                            if (lut[w * y + x] != -1) {
                                // remove from possible previous
                                // cluster
                                clusters[lut[w * y + x]]!!.removePixel(
                                    pixel
                                )
                            }
                            // add pixel to cluster
                            cluster.addPixel(pixel)
                        }
                        // continue looping
                        pixelChangedCluster = true

                        // update lut
                        lut[w * y + x] = cluster.id
                    }
                }
            }
            if (mode == MODE_ITERATIVE) {
                // update clusters
                for (i in clusters.indices) {
                    clusters[i]!!.clear()
                }
                for (y in 0 until h) {
                    for (x in 0 until w) {
                        val clusterId = lut[w * y + x]
                        // add pixels to cluster
                        clusters[clusterId]!!.addPixel(
                            image.getRGB(x, y)
                        )
                    }
                }
            }
        }
        // create result image
        val result = BufferedImage(
            w, h,
            BufferedImage.TYPE_INT_RGB
        )
        for (y in 0 until h) {
            for (x in 0 until w) {
                val clusterId = lut[w * y + x]
                result.setRGB(x, y, clusters[clusterId]!!.rGB)
            }
        }
        val end = System.currentTimeMillis()
        println(
            "Clustered to " + k
                    + " clusters in " + loops
                    + " loops in " + (end - start) + " ms."
        )
        return result
    }

    fun createClusters(image: BufferedImage?, k: Int): Array<Cluster?> {
        // Here the clusters are taken with specific steps,
        // so the result looks always same with same image.
        // You can randomize the cluster centers, if you like.
        val result = arrayOfNulls<Cluster>(k)
        var x = 0
        var y = 0
        val dx = image!!.width / k
        val dy = image.height / k
        for (i in 0 until k) {
            result[i] = Cluster(i, image.getRGB(x, y))
            x += dx
            y += dy
        }
        return result
    }

    fun findMinimalCluster(rgb: Int): Cluster? {
        var cluster: Cluster? = null
        var min = Int.MAX_VALUE
        for (i in clusters.indices) {
            val distance = clusters[i]!!.distance(rgb)
            if (distance < min) {
                min = distance
                cluster = clusters[i]
            }
        }
        return cluster
    }

    fun saveImage(
        filename: String,
        image: BufferedImage?
    ) {
        val file = File(filename)
        try {
            ImageIO.write(image, "png", file)
        } catch (e: Exception) {
            println(
                (e.toString() + " Image '" + filename
                        + "' saving failed.")
            )
        }
    }

    fun loadImage(filename: String): BufferedImage? {
        var result: BufferedImage? = null
        try {
            result = ImageIO.read(File(filename))
        } catch (e: Exception) {
            println(
                (e.toString() + " Image '"
                        + filename + "' not found.")
            )
        }
        return result
    }

    class Cluster(id: Int, rgb: Int) {
        var id: Int
        var pixelCount = 0
        var red: Int
        var green: Int
        var blue: Int
        var reds = 0
        var greens = 0
        var blues = 0

        init {
            val r = rgb shr 16 and 0x000000FF
            val g = rgb shr 8 and 0x000000FF
            val b = rgb shr 0 and 0x000000FF
            red = r
            green = g
            blue = b
            this.id = id
            addPixel(rgb)
        }

        fun clear() {
            red = 0
            green = 0
            blue = 0
            reds = 0
            greens = 0
            blues = 0
            pixelCount = 0
        }

        val rGB: Int
            get() {
                val r = reds / pixelCount
                val g = greens / pixelCount
                val b = blues / pixelCount
                return -0x1000000 or (r shl 16) or (g shl 8) or b
            }

        fun addPixel(color: Int) {
            val r = color shr 16 and 0x000000FF
            val g = color shr 8 and 0x000000FF
            val b = color shr 0 and 0x000000FF
            reds += r
            greens += g
            blues += b
            pixelCount++
            red = reds / pixelCount
            green = greens / pixelCount
            blue = blues / pixelCount
        }

        fun removePixel(color: Int) {
            val r = color shr 16 and 0x000000FF
            val g = color shr 8 and 0x000000FF
            val b = color shr 0 and 0x000000FF
            reds -= r
            greens -= g
            blues -= b
            pixelCount--
            red = reds / pixelCount
            green = greens / pixelCount
            blue = blues / pixelCount
        }

        fun distance(color: Int): Int {
            val r = color shr 16 and 0x000000FF
            val g = color shr 8 and 0x000000FF
            val b = color shr 0 and 0x000000FF
            val rx = Math.abs(red - r)
            val gx = Math.abs(green - g)
            val bx = Math.abs(blue - b)
            return (rx + gx + bx) / 3
        }
    }
}