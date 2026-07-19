package com.jotape.inkshelf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.io.File

/**
 * Capa de um item da biblioteca, com um espaço reservado enquanto ela não existe.
 *
 * A capa é gerada em segundo plano pelo `CoverExtractor`, então é normal um item recém-varrido
 * ainda não ter arquivo — daí o marcador, em vez de um buraco na grade.
 */
@Composable
fun CoverImage(
    coverPath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholderColor: Color = Color(0xFF2A2A2E),
) {
    val coverFile = coverPath?.let(::File)?.takeIf { it.exists() && it.length() > 0L }

    Box(modifier = modifier.background(placeholderColor), contentAlignment = Alignment.Center) {
        if (coverFile != null) {
            AsyncImage(
                model = coverFile,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
