package com.mrbastim.standbymode.ui.standby

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrbastim.standbymode.R
import com.mrbastim.standbymode.data.settings.StandBySettings
import com.mrbastim.standbymode.system.MusicRepository
import com.mrbastim.standbymode.system.MusicState

@Composable
fun CompactMusicWidget() {
    if (LocalInspectionMode.current) {
        MusicWidgetContent(MusicState(accessGranted = true))
        return
    }
    val context = LocalContext.current
    val repository = remember(context) { MusicRepository(context) }
    val state by repository.state.collectAsStateWithLifecycle(initialValue = MusicState())
    MusicWidgetContent(
        state = state,
        onToggle = repository::togglePlayback,
        onPrevious = repository::previous,
        onNext = repository::next,
        onGrantAccess = {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, R.string.music_settings_unavailable, Toast.LENGTH_SHORT).show()
            }
        },
    )
}

@Composable
private fun MusicWidgetContent(
    state: MusicState,
    onToggle: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onGrantAccess: () -> Unit = {},
) {
    val theme = LocalStandByTheme.current
    BoxWithConstraints(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        val artworkSize = (this.maxHeight.value * 0.30f).coerceIn(40f, 108f).dp
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val artwork = state.artwork
            if (artwork != null) {
                val bitmap = remember(artwork) { artwork.asImageBitmap() }
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(artworkSize).clip(RoundedCornerShape(18.dp)),
                )
            } else {
                Box(
                    Modifier.size(artworkSize)
                        .background(theme.secondary.copy(alpha = 0.08f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("♫", color = theme.accent, fontSize = 36.sp)
                }
            }
            Text(
                text = when {
                    !state.accessGranted -> stringResource(R.string.widget_music)
                    state.hasSession -> state.title.ifBlank { stringResource(R.string.music_unknown_track) }
                    else -> stringResource(R.string.music_nothing_playing)
                },
                color = theme.primary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = when {
                    !state.accessGranted -> stringResource(R.string.music_access_description)
                    state.hasSession -> state.artist
                    else -> stringResource(R.string.music_start_in_player)
                },
                color = theme.secondary,
                fontSize = 14.sp,
                maxLines = if (state.hasSession) 1 else 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            if (!state.accessGranted) {
                TextButton(onClick = onGrantAccess) {
                    Text(stringResource(R.string.music_grant_access), color = theme.accent)
                }
            } else if (state.hasSession) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MusicControl(
                        type = MusicControlType.PREVIOUS,
                        label = stringResource(R.string.music_previous),
                        enabled = state.canSkipPrevious,
                        onClick = onPrevious,
                    )
                    MusicControl(
                        type = if (state.isPlaying) MusicControlType.PAUSE else MusicControlType.PLAY,
                        label = stringResource(if (state.isPlaying) R.string.music_pause else R.string.music_play),
                        enabled = state.canToggle,
                        prominent = true,
                        onClick = onToggle,
                    )
                    MusicControl(
                        type = MusicControlType.NEXT,
                        label = stringResource(R.string.music_next),
                        enabled = state.canSkipNext,
                        onClick = onNext,
                    )
                }
            }
        }
    }
}

private enum class MusicControlType { PREVIOUS, PLAY, PAUSE, NEXT }

@Composable
private fun MusicControl(
    type: MusicControlType,
    label: String,
    enabled: Boolean,
    prominent: Boolean = false,
    onClick: () -> Unit,
) {
    val theme = LocalStandByTheme.current
    val tint = (if (prominent) theme.accent else theme.primary)
        .copy(alpha = if (enabled) 1f else 0.25f)
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(if (prominent) 56.dp else 48.dp)
            .background(
                if (prominent) theme.accent.copy(alpha = 0.10f) else androidx.compose.ui.graphics.Color.Transparent,
                CircleShape,
            )
            .semantics { contentDescription = label },
    ) {
        Canvas(Modifier.size(26.dp)) {
            fun rect(left: Float, top: Float, right: Float, bottom: Float) {
                drawRect(
                    tint,
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * left, size.height * top),
                    size = androidx.compose.ui.geometry.Size(size.width * (right - left), size.height * (bottom - top)),
                )
            }
            fun triangle(left: Float, right: Float) {
                drawPath(
                    Path().apply {
                        moveTo(size.width * left, size.height * 0.18f)
                        lineTo(size.width * right, size.height * 0.5f)
                        lineTo(size.width * left, size.height * 0.82f)
                        close()
                    },
                    tint,
                )
            }
            when (type) {
                MusicControlType.PLAY -> triangle(0.25f, 0.82f)
                MusicControlType.PAUSE -> {
                    rect(0.22f, 0.18f, 0.40f, 0.82f)
                    rect(0.60f, 0.18f, 0.78f, 0.82f)
                }
                MusicControlType.PREVIOUS -> {
                    rect(0.18f, 0.18f, 0.30f, 0.82f)
                    triangle(0.82f, 0.30f)
                }
                MusicControlType.NEXT -> {
                    triangle(0.18f, 0.70f)
                    rect(0.70f, 0.18f, 0.82f, 0.82f)
                }
            }
        }
    }
}

@Preview(name = "Музыка", locale = "ru", widthDp = 400, heightDp = 360)
@Composable
private fun MusicWidgetPreview() {
    StandByThemeProvider(StandBySettings()) {
        Box(Modifier.background(LocalStandByTheme.current.background)) {
            MusicWidgetContent(
                MusicState(
                    accessGranted = true,
                    hasSession = true,
                    title = "Название трека",
                    artist = "Исполнитель",
                    isPlaying = true,
                    canToggle = true,
                    canSkipPrevious = true,
                    canSkipNext = true,
                )
            )
        }
    }
}
