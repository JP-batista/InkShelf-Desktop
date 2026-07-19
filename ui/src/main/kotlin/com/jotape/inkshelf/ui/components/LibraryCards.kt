package com.jotape.inkshelf.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jotape.inkshelf.model.FileItem
import com.jotape.inkshelf.model.FolderItem
import com.jotape.inkshelf.ui.theme.LocalInkPalette

private val CardShape = RoundedCornerShape(10.dp)

/** Proporção de capa de quadrinho — a mesma usada no app mobile. */
private const val COVER_ASPECT = 2f / 3f

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileCard(
    file: FileItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalInkPalette.current

    Column(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onToggleFavorite,
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(COVER_ASPECT)
                .clip(CardShape),
        ) {
            CoverImage(
                coverPath = file.coverPath,
                contentDescription = file.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(COVER_ASPECT),
            )

            if (file.isFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorito",
                    tint = palette.accent,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(18.dp),
                )
            }

            // A barra de progresso só aparece em quem foi começado e não terminado — num item
            // não lido ela seria uma linha vazia em toda capa da grade.
            if (file.isInProgress) {
                LinearProgressIndicator(
                    progress = { file.progress },
                    color = palette.accent,
                    trackColor = palette.bg.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp),
                )
            }
        }

        Text(
            text = file.title.substringBeforeLast('.'),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderCard(
    folder: FolderItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalInkPalette.current

    Column(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onToggleFavorite,
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(COVER_ASPECT)
                .clip(CardShape)
                .background(palette.surface2),
            contentAlignment = Alignment.Center,
        ) {
            if (folder.coverPath != null) {
                CoverImage(
                    coverPath = folder.coverPath,
                    contentDescription = folder.title,
                    modifier = Modifier.fillMaxWidth().aspectRatio(COVER_ASPECT),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = palette.accent.copy(alpha = 0.7f),
                    modifier = Modifier.size(40.dp),
                )
            }

            if (folder.isFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorita",
                    tint = palette.accent,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(18.dp),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = folder.title,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (folder.itemCount == 1) "1 item" else "${folder.itemCount} itens",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textTertiary,
                )
            }
        }
    }
}
