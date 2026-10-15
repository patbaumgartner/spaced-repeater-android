package com.patbaumgartner.android.spacedrepeater.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed interface CardBlock {
    data class Text(val text: String) : CardBlock
    data class Image(val path: String, val alt: String) : CardBlock
}

private val IMAGE_MARKDOWN = Regex("""!\[([^\]]*)]\(\s*([^)\s]+)\s*\)""")

/** Splits card markdown into text and `![alt](path)` image blocks, preserving order. */
fun parseCardBlocks(markdown: String): List<CardBlock> {
    val blocks = ArrayList<CardBlock>()
    var cursor = 0
    fun addText(raw: String) {
        val text = raw.trim()
        if (text.isNotEmpty()) blocks += CardBlock.Text(text)
    }
    for (match in IMAGE_MARKDOWN.findAll(markdown)) {
        addText(markdown.substring(cursor, match.range.first))
        blocks += CardBlock.Image(path = match.groupValues[2], alt = match.groupValues[1])
        cursor = match.range.last + 1
    }
    addText(markdown.substring(cursor))
    return blocks
}

@Composable
internal fun CardContent(
    markdown: String,
    style: TextStyle,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(markdown) { parseCardBlocks(markdown) }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        for (block in blocks) {
            when (block) {
                is CardBlock.Text -> Text(block.text, style = style, color = color, textAlign = TextAlign.Center)
                is CardBlock.Image -> CardImage(block, style, color)
            }
        }
    }
}

@Composable
private fun CardImage(block: CardBlock.Image, fallbackStyle: TextStyle, color: Color) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, block.path) {
        value = withContext(Dispatchers.IO) { decodeSampled(block.path, maxDimension = 1280) }
    }
    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = block.alt.ifBlank { null },
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
    } else if (block.alt.isNotBlank()) {
        Text("[${block.alt}]", style = fallbackStyle, color = color, textAlign = TextAlign.Center)
    }
}

private fun decodeSampled(path: String, maxDimension: Int): ImageBitmap? {
    val file = File(path.removePrefix("file://"))
    if (!file.isFile) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDimension) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeFile(file.path, options)?.asImageBitmap()
}
