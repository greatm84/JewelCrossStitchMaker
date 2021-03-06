// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Color
import java.awt.FileDialog
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.print.Printable
import java.awt.print.PrinterException
import java.awt.print.PrinterJob
import java.io.File
import javax.imageio.ImageIO


enum class PrintStatus {
    NONE, BEGIN, ERROR, DONE
}

object PaperProperties {
    data class PaperProperty(
        val widthMm: Int,
        val heightMm: Int,
        val width: Int,
        val height: Int
    )

    val A4 = PaperProperty(292, 204, 578, 825)
}

private fun printDistance(color1: Int, color2: Int) {
    println("color1 $color1 - color2 $color2 dist is = ${Utils.getColorDistance(Color(color1), Color(color2))}")
}

@Composable
@Preview
fun app(windowScope: FrameWindowScope) {
    var txtAppStatus by rememberSaveable { mutableStateOf("") }
    var imageFilePath by rememberSaveable { mutableStateOf("") }
    var originBufferedImage by rememberSaveable { mutableStateOf(BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)) }
    var resizedBufferedImage by rememberSaveable {
        mutableStateOf(BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB))
    }
    var pixelatedBufferedImage by rememberSaveable {
        mutableStateOf(
            Utils.convertArrToImage(Array(1) { Array(1) { Color(0, 0, 0, 0) } }, BufferedImage.TYPE_INT_ARGB)
        )
    }

    var colorCountRankList by rememberSaveable { mutableStateOf(emptyList<Pair<Color, Int>>()) }
    var afterCountRankList by rememberSaveable { mutableStateOf(emptyList<Pair<Color, Int>>()) }
    var colorMap by rememberSaveable { mutableStateOf(hashMapOf<Int, Int>()) }
    var reductionColorList by rememberSaveable { mutableStateOf(emptyList<Color>()) }

    var colorLabelEnabled by rememberSaveable { mutableStateOf(false) }
    var btnPrintEnabled by rememberSaveable { mutableStateOf(false) }
    var colorReductionComboIndex by rememberSaveable { mutableStateOf(0) }
    val colorReductionItems = (200 downTo 10 step 10).toList()
    val scope = rememberCoroutineScope()

    printDistance(-16251626, -16382959)
    printDistance(-16448498, -16645368)

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(text = "App Status", modifier = Modifier.background(MaterialTheme.colors.background))
            Text(txtAppStatus)
            Spacer(Modifier.padding(5.dp))
            Text("Image file path is $imageFilePath")
            Spacer(Modifier.padding(5.dp))
            Button(onClick = {
                val dialog = FileDialog(windowScope.window, "Select Image file", FileDialog.LOAD)
                dialog.isVisible = true

                if (dialog.directory == null || dialog.file == null) return@Button

                imageFilePath = dialog.directory + dialog.file

                val imageFile = File(imageFilePath)
                val bufferedImage = ImageIO.read(imageFile)
                originBufferedImage = bufferedImage

                // width, height ??? A4 ?????? ?????? ????????? ??????  adjust the width and height so that they don't exceed A4
                var resizeWidth = originBufferedImage.width
                var resizeHeight = originBufferedImage.height

                while (PaperProperties.A4.width < resizeWidth) {
                    resizeWidth = resizeWidth * 9 / 10
                    resizeHeight = resizeHeight * 9 / 10
                }

                while (PaperProperties.A4.height < resizeHeight) {
                    resizeWidth = resizeWidth * 9 / 10
                    resizeHeight = resizeHeight * 9 / 10
                }

                resizedBufferedImage =
                    Utils.resizeImage(originBufferedImage, resizeWidth, resizeHeight)

            }) {
                Text("Select Image File")
            }
            Spacer(Modifier.padding(5.dp))
            Row {
                Column(Modifier.weight(1f)) { DrawImageView(resizedBufferedImage) }
                Spacer(Modifier.padding(5.dp))
                Column(Modifier.weight(0.4f)) {
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        colorLabelEnabled = false  // turn off color label
                        val imageArr = Utils.convertImageToArr(resizedBufferedImage)
                        val pixelated = Pixelator.pixelate(imageArr, 8, 16)
                        pixelatedBufferedImage = Utils.convertArrToImage(pixelated, resizedBufferedImage.type)
                        colorMap = Utils.generateColorMap(pixelated)

                        // prefix flow  if list item has close value with front, second images, so that merge them and increment count
                        val std = colorMap.toList().map { it.second }.std()

                        colorReductionComboIndex = 0
                        val colorDistanceThreshold = colorReductionItems[colorReductionComboIndex]
                        println("std is $std distThresh $colorDistanceThreshold")

                        reductionColorList = Utils.generateReductionColorList(
                            colorMap,
                            colorDistanceThreshold
                        ) { rankCountList, afterCountList ->
                            colorCountRankList = rankCountList
                            afterCountRankList = afterCountList
                        }

                        btnPrintEnabled = true
                    }) {
                        Text(">>")
                    }
                }
                Spacer(Modifier.padding(5.dp))
                Column(Modifier.weight(1f)) {
                    DrawPixelView(pixelatedBufferedImage, colorLabelEnabled, reductionColorList)
                }
                Spacer(Modifier.padding(5.dp))
                Row(Modifier.weight(1f)) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth()) {
                            val displayRankCount = 10
                            // Top rank colors
                            DrawColorRankList(colorCountRankList, displayRankCount)
                            Spacer(Modifier.padding(5.dp))
                            // After rank colors
                            DrawColorRankList(afterCountRankList, displayRankCount)
                        }
                        DrawColorReductionCombo(colorReductionItems, colorReductionComboIndex) {
                            // Do color map 0 - 255 rgb value  is original   divid by 10, range conver to 0 - 25
                            // If original value is 128, will be  128 / 10  = 12
                            colorReductionComboIndex = it
                            // get reduction value
                            val threshDist = colorReductionItems[colorReductionComboIndex]
                            println("threshDist $threshDist")

                            val arr = Utils.convertImageToArr(resizedBufferedImage)
                            val pixelated = Pixelator.pixelate(arr, 8, 16)
                            reductionColorList =
                                Utils.generateReductionColorList(
                                    colorMap,
                                    threshDist
                                ) { rankCountList, afterCountList ->
                                    colorCountRankList = rankCountList
                                    afterCountRankList = afterCountList
                                }
                            val reductionArr = Utils.reductionArrColors(pixelated, reductionColorList)
                            pixelatedBufferedImage = Utils.convertArrToImage(reductionArr, resizedBufferedImage.type)
                        }
                        DrawCheckBox(colorLabelEnabled) { colorLabelEnabled = it }
                    }
                }
            }
            Spacer(Modifier.padding(5.dp))
            Button(onClick = {
                scope.launch(Dispatchers.IO) {
                    val targetImage = if (colorLabelEnabled) {
                        Utils.generateColorLabelToImage(pixelatedBufferedImage, reductionColorList.map { it.rgb })
                    } else {
                        pixelatedBufferedImage
                    }
                    printWork(targetImage) {
                        when (it) {
                            PrintStatus.NONE -> {
                                btnPrintEnabled = true
                            }
                            PrintStatus.BEGIN -> {
                                txtAppStatus = "Printing..."
                                btnPrintEnabled = false
                            }
                            PrintStatus.ERROR -> {
                                txtAppStatus = "Last Print Got Some Error"
                                btnPrintEnabled = true
                            }
                            PrintStatus.DONE -> {
                                txtAppStatus = "Image Send Done"
                                btnPrintEnabled = true
                            }
                        }
                    }
                }
            }, enabled = btnPrintEnabled) {
                Text("Print")
            }
        }
    }
}

@Composable
fun DrawImageView(bufferedImage: BufferedImage) {
    Image(
        painter = BitmapPainter(image = bufferedImage.toComposeImageBitmap()),
        contentDescription = ""
    )
}

@Composable
fun DrawPixelView(bufferedImage: BufferedImage, colorLabelEnabled: Boolean, colorList: List<Color>) {
    if (colorLabelEnabled) {
        val newImage = Utils.generateColorLabelToImage(bufferedImage, colorList.map { it.rgb })
        Image(
            painter = BitmapPainter(image = newImage.toComposeImageBitmap()),
            contentDescription = ""
        )
    } else {
        Image(
            painter = BitmapPainter(image = bufferedImage.toComposeImageBitmap()),
            contentDescription = ""
        )
    }
}

@Composable
fun DrawColorRankList(
    colorCountPairList: List<Pair<Color, Int>>,
    displaySize: Int
) {
    Column(Modifier.width(90.dp)) {
        Text("${colorCountPairList.size}")
        LazyColumn(Modifier.fillMaxWidth()) {
            items(items = colorCountPairList.take(displaySize)) {
                DrawColorRankItem(modifier = Modifier.fillMaxWidth().heightIn(20.dp, 50.dp), it)
            }
        }
    }
}

@Composable
fun DrawColorRankItem(
    modifier: Modifier,
    colorCount: Pair<Color, Int>
) {
    val image = Utils.convertArrToImage(Array(50) { Array(50) { colorCount.first } }, BufferedImage.TYPE_INT_ARGB)
    Row(modifier = modifier) {
        Image(
            modifier = Modifier.width(30.dp),
            painter = BitmapPainter(image = image.toComposeImageBitmap()),
            contentDescription = ""
        )
        val content = "${colorCount.first.rgb}\n${colorCount.second}"
        Text(modifier = Modifier.width(60.dp), text = content, fontSize = 8.sp)
    }
}

@Composable
fun DrawColorReductionCombo(items: List<Int>, selectedIndex: Int, selectedIndexChanged: (Int) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.background(MaterialTheme.colors.background)) {
        Text(text = "thresh ${items[selectedIndex]}", modifier = Modifier.width(200.dp).clickable { expanded = true })
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEachIndexed { index, s ->
                DropdownMenuItem(onClick = {
                    selectedIndexChanged(index)
                    expanded = false
                }) {
                    Text(text = s.toString())
                }
            }
        }
    }
}

@Composable
fun DrawCheckBox(checked: Boolean, onCheckChanged: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = onCheckChanged)
        Spacer(Modifier.padding(5.dp))
        Text(text = "Draw Color Label")
    }
}

private fun printWork(image: Image, statusChanged: (PrintStatus) -> Unit) {
    statusChanged(PrintStatus.BEGIN)

    val job = PrinterJob.getPrinterJob()
    job.setPrintable { graphics, pageFormat, pageIndex ->

        if (pageIndex != 0) {
            statusChanged(PrintStatus.ERROR)
            return@setPrintable Printable.NO_SUCH_PAGE
        }

        val width = image.getWidth(null)
        val height = image.getHeight(null)

        val ableX = pageFormat.imageableX.toInt()
        val ableY = pageFormat.imageableY.toInt()
        val ableWidth = pageFormat.imageableWidth.toInt()
        val ableHeight = pageFormat.imageableHeight.toInt()

        val beginX = ableX + ((ableWidth - width) / 2)
        val beginY = ableY + ((ableHeight - height) / 2)

        graphics.drawImage(image, beginX, beginY, width, height, null)

        return@setPrintable Printable.PAGE_EXISTS
    }

    if (job.printDialog()) {
        try {
            job.print()
            statusChanged(PrintStatus.DONE)
        } catch (e: PrinterException) {
            e.printStackTrace()
        }
    } else {
        println("Canceled?")
    }

    statusChanged(PrintStatus.NONE)
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "JewelCrossStitch") {
        app(this)
    }
}