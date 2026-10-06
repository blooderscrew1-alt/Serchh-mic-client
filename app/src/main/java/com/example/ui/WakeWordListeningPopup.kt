package com.example.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.voice.ClientMicState
import com.example.voice.ClientUiState
import com.example.voice.WakeWordPopupPhase
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val NeonGreen = Color(0xFF00E676)
private val NeonEmerald = Color(0xFF10B981)
private val CyanAccent = Color(0xFF06B6D4)
private val ElectricBlue = Color(0xFF3B82F6)
private val AmberGlow = Color(0xFFFFB300)
private val GoldAccent = Color(0xFFFFD54F)
private val PurpleGlow = Color(0xFFA855F7)

/**
 * Animated Popup that opens upon Wake-Word activation ("Música" etc.) with a loading/listening
 * 🎧 animation capturing words in real-time, and smoothly transforms into a giant animated 😉 emoji
 * with 📨 confirmation when the command is successfully sent to the host.
 * Fully responsive and scrollable in both Portrait and Landscape orientations.
 */
@Composable
fun WakeWordListeningPopup(
    state: ClientUiState,
    onDismiss: () -> Unit,
    onManualCancel: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenHeight = configuration.screenHeightDp.dp
    val isVisible = state.showWakeWordPopup || state.isWakeWordActive || state.wakeWordPopupPhase == WakeWordPopupPhase.SUCCESS

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(140)) + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)),
        exit = fadeOut(tween(140)) + scaleOut(tween(120))
    ) {
        // Full screen modal overlay backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99050811))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            // Popup Card Container
            Surface(
                modifier = Modifier
                    .fillMaxWidth(if (isLandscape) 0.94f else 0.92f)
                    .widthIn(max = if (isLandscape) 660.dp else 440.dp)
                    .heightIn(max = screenHeight - (if (isLandscape) 16.dp else 36.dp))
                    .padding(if (isLandscape) 6.dp else 16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Intercept clicks inside card
                    )
                    .shadow(
                        elevation = 28.dp,
                        shape = RoundedCornerShape(if (isLandscape) 24.dp else 32.dp),
                        ambientColor = if (state.wakeWordPopupPhase == WakeWordPopupPhase.SUCCESS) NeonGreen.copy(alpha = 0.5f) else CyanAccent.copy(alpha = 0.4f),
                        spotColor = if (state.wakeWordPopupPhase == WakeWordPopupPhase.SUCCESS) NeonEmerald else ElectricBlue
                    ),
                shape = RoundedCornerShape(if (isLandscape) 24.dp else 32.dp),
                color = Color(0xF20B101E),
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = if (state.wakeWordPopupPhase == WakeWordPopupPhase.SUCCESS) {
                            listOf(NeonGreen, GoldAccent, NeonEmerald)
                        } else {
                            listOf(CyanAccent, ElectricBlue, PurpleGlow)
                        }
                    )
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isLandscape) 12.dp else 20.dp)
                ) {
                    // Top-right Close Button
                    IconButton(
                        onClick = onManualCancel,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                            .background(Color(0x2AFFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedContent(
                            targetState = state.wakeWordPopupPhase,
                            transitionSpec = {
                                (fadeIn(tween(300)) + scaleIn(spring(dampingRatio = 0.7f, stiffness = 300f)))
                                    .togetherWith(fadeOut(tween(200)) + scaleOut(tween(180)))
                            },
                            label = "WakeWordPopupPhaseTransition"
                        ) { phase ->
                            when (phase) {
                                WakeWordPopupPhase.SUCCESS -> {
                                    // 2. 😉 GIANT WINKING EMOJI & 📨 COMMAND SENT SUCCESS VIEW
                                    GiantWinkEmojiSuccessView(
                                        commandSummary = state.popupCommandSummary.ifBlank { state.lastRecognizedText },
                                        isLandscape = isLandscape,
                                        onDismiss = onDismiss
                                    )
                                }
                                else -> {
                                    // 1. 🎧 LOADING / LISTENING ANIMATION & LIVE CAPTIONS VIEW
                                    ListeningLoadingVoiceView(
                                        state = state,
                                        isLandscape = isLandscape,
                                        onCancel = onManualCancel
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Phase 1: 🎧 Listening & Loading Animated Visualizer
 * Shows rotating holographic radar arcs, pulsing reactive soundwave rings, floating 🎧 headphones,
 * live real-time partial voice recognition captions and dynamic audio energy bars.
 */
@Composable
private fun ListeningLoadingVoiceView(
    state: ClientUiState,
    isLandscape: Boolean = false,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "listeningWaves")
    
    // Rotating loading ring angle
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulsing audio wave expansion
    val wavePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wavePulse"
    )

    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingAlpha"
    )

    val normalizedRms = (state.rmsLevel / 10f).coerceIn(0f, 1f)
    val reactiveScale = 1.0f + (normalizedRms * 0.22f)

    if (isLandscape) {
        // Landscape 2-column side-by-side layout: Visualizer on Left, Live Captured Words on Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // LEFT COLUMN: Visualizer + Status Pill + Wake word info
            Column(
                modifier = Modifier.weight(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Status Top Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x3306B6D4))
                        .border(1.dp, CyanAccent.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (state.wakeWordPopupPhase == WakeWordPopupPhase.SENDING) GoldAccent else NeonGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.wakeWordPopupPhase == WakeWordPopupPhase.SENDING) {
                            "📡 ENVIANDO..."
                        } else if (state.wakeWindowRemainingSecs > 0) {
                            "🎧 ESCUCHANDO (${state.wakeWindowRemainingSecs}s)"
                        } else {
                            "🎧 ESCUCHANDO"
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                }

                // Compact 🎧 Loading Visualizer
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(115.dp)
                        .scale(reactiveScale)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(rotationAngle)
                    ) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    CyanAccent.copy(alpha = 0.3f),
                                    CyanAccent,
                                    ElectricBlue,
                                    PurpleGlow
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 290f,
                            useCenter = false,
                            style = Stroke(width = 5f, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = GoldAccent.copy(alpha = 0.6f),
                            startAngle = 180f,
                            sweepAngle = 60f,
                            useCenter = false,
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                        )
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val baseRadius = size.minDimension * 0.32f

                        drawCircle(
                            color = CyanAccent.copy(alpha = (0.25f * (1.3f - wavePulse)).coerceIn(0.05f, 0.3f)),
                            radius = baseRadius * wavePulse * 1.35f,
                            center = center,
                            style = Stroke(width = 2.5f)
                        )
                        drawCircle(
                            color = ElectricBlue.copy(alpha = (0.35f * (1.2f - wavePulse)).coerceIn(0.08f, 0.4f)),
                            radius = baseRadius * wavePulse,
                            center = center,
                            style = Stroke(width = 2f)
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xEE1E293B),
                                    Color(0xF00F172A),
                                    Color(0xFF020617)
                                ),
                                center = center,
                                radius = baseRadius
                            ),
                            radius = baseRadius,
                            center = center
                        )
                        drawCircle(
                            color = CyanAccent.copy(alpha = 0.5f),
                            radius = baseRadius,
                            center = center,
                            style = Stroke(width = 2f)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = "Escuchando",
                            tint = CyanAccent,
                            modifier = Modifier
                                .size(34.dp)
                                .scale(wavePulse.coerceIn(0.95f, 1.12f))
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.height(10.dp)
                        ) {
                            val heights = listOf(
                                0.4f + (normalizedRms * 0.6f) * wavePulse,
                                0.7f + (normalizedRms * 0.8f),
                                1.0f + (normalizedRms * 1.0f) * wavePulse,
                                0.6f + (normalizedRms * 0.7f),
                                0.3f + (normalizedRms * 0.5f) * wavePulse
                            )
                            heights.forEach { h ->
                                Box(
                                    modifier = Modifier
                                        .width(2.5.dp)
                                        .height(10.dp * h.coerceIn(0.2f, 1.2f))
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(Brush.verticalGradient(listOf(GoldAccent, CyanAccent)))
                                )
                            }
                        }
                    }
                }

                // Wake word info indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Voz: ",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "'${state.wakeWord}'",
                        color = GoldAccent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // RIGHT COLUMN: Title + Real-Time Live Word Captions Card
            Column(
                modifier = Modifier.weight(1.35f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🎧 Te escucho, dime tu orden",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                val hasPartial = state.partialText.isNotBlank()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x3D1E293B))
                        .border(1.5.dp, CyanAccent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (hasPartial) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "\"${state.partialText}\"",
                                    color = Color(0xFFF8FAFC),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(16.dp)
                                        .background(CyanAccent.copy(alpha = breathingAlpha), RoundedCornerShape(1.dp))
                                )
                            }
                        } else {
                            Text(
                                text = "💬 Ej: \"Pon Queen\", \"Sube volumen\", \"Pausa\", \"Siguiente\"...",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = if (hasPartial) NeonGreen else CyanAccent.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (hasPartial) "Captando voz en vivo..." else "Palabra clave detectada • Esperando orden...",
                                color = if (hasPartial) NeonGreen else Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Portrait Layout
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Top Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x3306B6D4))
                    .border(1.dp, CyanAccent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (state.wakeWordPopupPhase == WakeWordPopupPhase.SENDING) GoldAccent else NeonGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.wakeWordPopupPhase == WakeWordPopupPhase.SENDING) {
                        "📡 ENVIANDO A REPRODUCTOR..."
                    } else if (state.wakeWindowRemainingSecs > 0) {
                        "🎧 ESCUCHANDO (${state.wakeWindowRemainingSecs}s)"
                    } else {
                        "🎧 ESCUCHANDO ACTIVO"
                    },
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Center 🎧 Loading & Audio Visualizer Canvas
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .scale(reactiveScale)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotationAngle)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = (size.minDimension / 2f) - 10f

                    // Outer rotating gradient arcs (loading style)
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                CyanAccent.copy(alpha = 0.3f),
                                CyanAccent,
                                ElectricBlue,
                                PurpleGlow
                            )
                        ),
                        startAngle = 0f,
                        sweepAngle = 290f,
                        useCenter = false,
                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                    )

                    // Secondary counter-spinning dashed arc
                    drawArc(
                        color = GoldAccent.copy(alpha = 0.6f),
                        startAngle = 180f,
                        sweepAngle = 60f,
                        useCenter = false,
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                }

                // Pulsing sonar concentric rings
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseRadius = size.minDimension * 0.32f

                    // Outer sonar pulse ring
                    drawCircle(
                        color = CyanAccent.copy(alpha = (0.25f * (1.3f - wavePulse)).coerceIn(0.05f, 0.3f)),
                        radius = baseRadius * wavePulse * 1.35f,
                        center = center,
                        style = Stroke(width = 3f)
                    )

                    // Inner sonar pulse ring
                    drawCircle(
                        color = ElectricBlue.copy(alpha = (0.35f * (1.2f - wavePulse)).coerceIn(0.08f, 0.4f)),
                        radius = baseRadius * wavePulse,
                        center = center,
                        style = Stroke(width = 2.5f)
                    )

                    // Central glowing disk
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xEE1E293B),
                                Color(0xF00F172A),
                                Color(0xFF020617)
                            ),
                            center = center,
                            radius = baseRadius
                        ),
                        radius = baseRadius,
                        center = center
                    )

                    drawCircle(
                        color = CyanAccent.copy(alpha = 0.5f),
                        radius = baseRadius,
                        center = center,
                        style = Stroke(width = 2.5f)
                    )
                }

                // Headphone 🎧 Center Icon & Equalizer Waves
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "Escuchando",
                        tint = CyanAccent,
                        modifier = Modifier
                            .size(44.dp)
                            .scale(wavePulse.coerceIn(0.95f, 1.12f))
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))

                    // Mini Animated Equalizer bars underneath headphones
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.height(14.dp)
                    ) {
                        val heights = listOf(
                            0.4f + (normalizedRms * 0.6f) * wavePulse,
                            0.7f + (normalizedRms * 0.8f),
                            1.0f + (normalizedRms * 1.0f) * wavePulse,
                            0.6f + (normalizedRms * 0.7f),
                            0.3f + (normalizedRms * 0.5f) * wavePulse
                        )
                        heights.forEach { h ->
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height((14.dp * h.coerceIn(0.2f, 1.2f)))
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(GoldAccent, CyanAccent)
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Instruction Title
            Text(
                text = "🎧 Te escucho, dime tu orden",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Live Real-Time Captions / Word Capture Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x3D1E293B))
                    .border(1.5.dp, CyanAccent.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                val hasPartial = state.partialText.isNotBlank()
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (hasPartial) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "\"${state.partialText}\"",
                                color = Color(0xFFF8FAFC),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            // Blinking glowing cursor
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(18.dp)
                                    .background(CyanAccent.copy(alpha = breathingAlpha), RoundedCornerShape(1.dp))
                            )
                        }
                    } else {
                        Text(
                            text = "💬 Ej: \"Pon Queen\", \"Sube volumen\", \"Pausa\", \"Siguiente\"...",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Signal indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = if (hasPartial) NeonGreen else CyanAccent.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasPartial) "Captando voz en vivo..." else "Palabra clave detectada • Esperando...",
                            color = if (hasPartial) NeonGreen else Color(0xFFCBD5E1),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Wake word info indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Activado por voz: ",
                    color = Color(0xFF64748B),
                    fontSize = 11.5.sp
                )
                Text(
                    text = "'${state.wakeWord}'",
                    color = GoldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Phase 2: 😉 GIANT WINKING EMOJI & 📨 COMMAND SENT SUCCESS VIEW
 * Renders an extra-large, lively animated 😉 winking face with celebratory sparkles,
 * pulsing green aura, and confirmation banner indicating that the command was sent.
 */
@Composable
private fun GiantWinkEmojiSuccessView(
    commandSummary: String,
    isLandscape: Boolean = false,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "winkCelebration")

    // Playful bouncy bobbing animation
    val bounceScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounceScale"
    )

    // Playful tilt
    val tiltAngle by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tiltAngle"
    )

    // Wink eye curvature animation
    val winkProgress by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "winkProgress"
    )

    // Sparkle star rotation
    val sparkleRotate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkleRotate"
    )

    // Glowing aura expanding pulse
    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraPulse"
    )

    if (isLandscape) {
        // Landscape side-by-side success view
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // LEFT: Animated 😉 Winking Emoji
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(125.dp)
                    .scale(bounceScale)
                    .rotate(tiltAngle)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val auraRadius = (size.minDimension / 2f) * auraPulse

                    drawCircle(
                        color = NeonGreen.copy(alpha = (0.28f * (1.3f - auraPulse)).coerceIn(0.04f, 0.35f)),
                        radius = auraRadius * 1.15f,
                        center = center,
                        style = Stroke(width = 3.5f)
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GoldAccent.copy(alpha = 0.4f),
                                AmberGlow.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = auraRadius
                        ),
                        radius = auraRadius,
                        center = center
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(sparkleRotate)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val starDist = size.minDimension * 0.46f

                    for (i in 0..3) {
                        val angle = (i * 90f) * (PI / 180f)
                        val sx = center.x + (starDist * cos(angle)).toFloat()
                        val sy = center.y + (starDist * sin(angle)).toFloat()
                        drawSparkleStar(Offset(sx, sy), 8f, GoldAccent)
                    }
                }

                Canvas(
                    modifier = Modifier.fillMaxSize(0.86f)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val faceRadius = size.minDimension / 2f

                    // 1. 3D Spherical Face Base
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFEE58),
                                Color(0xFFFFCA28),
                                Color(0xFFFFA000),
                                Color(0xFFFF8F00)
                            ),
                            center = Offset(center.x - (faceRadius * 0.22f), center.y - (faceRadius * 0.28f)),
                            radius = faceRadius * 1.25f
                        ),
                        radius = faceRadius,
                        center = center
                    )

                    drawCircle(
                        color = Color(0xFFE65100).copy(alpha = 0.4f),
                        radius = faceRadius,
                        center = center,
                        style = Stroke(width = 3f)
                    )

                    drawOval(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.0f))
                        ),
                        topLeft = Offset(center.x - (faceRadius * 0.55f), center.y - (faceRadius * 0.82f)),
                        size = Size(faceRadius * 1.1f, faceRadius * 0.42f)
                    )

                    // Cheeks
                    val blushRadius = faceRadius * 0.22f
                    val blushY = center.y + (faceRadius * 0.15f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFF4081).copy(alpha = 0.45f), Color.Transparent),
                            center = Offset(center.x - (faceRadius * 0.58f), blushY),
                            radius = blushRadius
                        ),
                        radius = blushRadius,
                        center = Offset(center.x - (faceRadius * 0.58f), blushY)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFF4081).copy(alpha = 0.45f), Color.Transparent),
                            center = Offset(center.x + (faceRadius * 0.58f), blushY),
                            radius = blushRadius
                        ),
                        radius = blushRadius,
                        center = Offset(center.x + (faceRadius * 0.58f), blushY)
                    )

                    // LEFT EYE
                    val leftEyeX = center.x - (faceRadius * 0.35f)
                    val leftEyeY = center.y - (faceRadius * 0.18f)
                    val eyeRadius = faceRadius * 0.16f

                    drawCircle(color = Color(0xFF261908), radius = eyeRadius, center = Offset(leftEyeX, leftEyeY))
                    drawCircle(color = Color.White, radius = eyeRadius * 0.42f, center = Offset(leftEyeX - (eyeRadius * 0.35f), leftEyeY - (eyeRadius * 0.35f)))
                    drawCircle(color = Color.White.copy(alpha = 0.8f), radius = eyeRadius * 0.22f, center = Offset(leftEyeX + (eyeRadius * 0.35f), leftEyeY + (eyeRadius * 0.35f)))

                    val leftBrowPath = Path().apply {
                        moveTo(leftEyeX - (eyeRadius * 1.3f), leftEyeY - (eyeRadius * 1.5f))
                        quadraticBezierTo(leftEyeX, leftEyeY - (eyeRadius * 2.1f), leftEyeX + (eyeRadius * 1.2f), leftEyeY - (eyeRadius * 1.3f))
                    }
                    drawPath(path = leftBrowPath, color = Color(0xFF4E342E), style = Stroke(width = 4.5f, cap = StrokeCap.Round))

                    // RIGHT EYE (Wink)
                    val rightEyeX = center.x + (faceRadius * 0.35f)
                    val rightEyeY = center.y - (faceRadius * 0.18f)
                    val winkWidth = faceRadius * 0.34f * winkProgress

                    val winkPath = Path().apply {
                        moveTo(rightEyeX - (winkWidth * 0.9f), rightEyeY + 3f)
                        quadraticBezierTo(rightEyeX, rightEyeY - (faceRadius * 0.22f * winkProgress), rightEyeX + (winkWidth * 0.9f), rightEyeY + 3f)
                    }
                    drawPath(path = winkPath, color = Color(0xFF261908), style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))

                    val rightBrowPath = Path().apply {
                        moveTo(rightEyeX - (winkWidth * 0.8f), rightEyeY - (faceRadius * 0.28f))
                        quadraticBezierTo(rightEyeX, rightEyeY - (faceRadius * 0.38f), rightEyeX + (winkWidth * 0.9f), rightEyeY - (faceRadius * 0.24f))
                    }
                    drawPath(path = rightBrowPath, color = Color(0xFF4E342E), style = Stroke(width = 4.5f, cap = StrokeCap.Round))

                    // MOUTH
                    val mouthWidth = faceRadius * 0.72f
                    val mouthY = center.y + (faceRadius * 0.26f)

                    val mouthPath = Path().apply {
                        moveTo(center.x - (mouthWidth / 2f), mouthY)
                        quadraticBezierTo(center.x, mouthY + (faceRadius * 0.48f), center.x + (mouthWidth / 2f), mouthY)
                        close()
                    }
                    drawPath(path = mouthPath, color = Color(0xFF2E1500), style = Fill)

                    val tonguePath = Path().apply {
                        moveTo(center.x - (mouthWidth * 0.32f), mouthY + (faceRadius * 0.24f))
                        quadraticBezierTo(center.x, mouthY + (faceRadius * 0.48f), center.x + (mouthWidth * 0.32f), mouthY + (faceRadius * 0.24f))
                        close()
                    }
                    drawPath(path = tonguePath, color = Color(0xFFFF5252), style = Fill)

                    val lipLinePath = Path().apply {
                        moveTo(center.x - (mouthWidth / 2f) - 3f, mouthY)
                        quadraticBezierTo(center.x, mouthY + (faceRadius * 0.08f), center.x + (mouthWidth / 2f) + 3f, mouthY)
                    }
                    drawPath(path = lipLinePath, color = Color(0xFF2E1500), style = Stroke(width = 5.5f, cap = StrokeCap.Round))
                }
            }

            // RIGHT: Success Header + Summary Card
            Column(
                modifier = Modifier.weight(1.3f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x3300E676))
                        .border(1.5.dp, NeonGreen, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(text = "📨", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "¡COMANDO ENVIADO!",
                        color = NeonGreen,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x3300E676))
                        .border(1.5.dp, NeonGreen.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MarkEmailRead,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Orden confirmada en el Host",
                                color = NeonGreen,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (commandSummary.isNotBlank()) "\"$commandSummary\"" else "\"Petición procesada correctamente\"",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = "Cerrando automáticamente ✨",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    } else {
        // Portrait Layout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top 📨 Success Header Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x3300E676))
                    .border(1.5.dp, NeonGreen, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "📨",
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "¡COMANDO ENVIADO CON ÉXITO!",
                    color = NeonGreen,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // THE GIANT ANIMATED 😉 WINK EMOJI
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(180.dp)
                    .scale(bounceScale)
                    .rotate(tiltAngle)
            ) {
                // Radiant Golden / Green celebration aura backdrop
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val auraRadius = (size.minDimension / 2f) * auraPulse

                    // Outer celebratory shockwave ring
                    drawCircle(
                        color = NeonGreen.copy(alpha = (0.28f * (1.3f - auraPulse)).coerceIn(0.04f, 0.35f)),
                        radius = auraRadius * 1.15f,
                        center = center,
                        style = Stroke(width = 4f)
                    )

                    // Golden warm ambient halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GoldAccent.copy(alpha = 0.4f),
                                AmberGlow.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = auraRadius
                        ),
                        radius = auraRadius,
                        center = center
                    )
                }

                // Floating Sparkle Stars ✨
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(sparkleRotate)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val starDist = size.minDimension * 0.46f

                    // 4 Golden sparkle stars around the emoji
                    for (i in 0..3) {
                        val angle = (i * 90f) * (PI / 180f)
                        val sx = center.x + (starDist * cos(angle)).toFloat()
                        val sy = center.y + (starDist * sin(angle)).toFloat()
                        drawSparkleStar(Offset(sx, sy), 10f, GoldAccent)
                    }
                }

                // Custom High-Fidelity 😉 Winking Emoji Face Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize(0.86f)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val faceRadius = size.minDimension / 2f

                    // 1. 3D Spherical Face Base (Golden-Yellow Gradient)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFEE58), // Highlight bright yellow
                                Color(0xFFFFCA28), // Core yellow
                                Color(0xFFFFA000), // Rich amber edge
                                Color(0xFFFF8F00)  // Deep shadow edge
                            ),
                            center = Offset(center.x - (faceRadius * 0.22f), center.y - (faceRadius * 0.28f)),
                            radius = faceRadius * 1.25f
                        ),
                        radius = faceRadius,
                        center = center
                    )

                    // Face Outline Rim
                    drawCircle(
                        color = Color(0xFFE65100).copy(alpha = 0.4f),
                        radius = faceRadius,
                        center = center,
                        style = Stroke(width = 3.5f)
                    )

                    // Top 3D Specular Shine (Glossy reflex)
                    drawOval(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.0f)
                            )
                        ),
                        topLeft = Offset(center.x - (faceRadius * 0.55f), center.y - (faceRadius * 0.82f)),
                        size = Size(faceRadius * 1.1f, faceRadius * 0.42f)
                    )

                    // 2. Rosy Blushing Cheeks 😊
                    val blushRadius = faceRadius * 0.22f
                    val blushY = center.y + (faceRadius * 0.15f)
                    // Left blush
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFF4081).copy(alpha = 0.45f), Color.Transparent),
                            center = Offset(center.x - (faceRadius * 0.58f), blushY),
                            radius = blushRadius
                        ),
                        radius = blushRadius,
                        center = Offset(center.x - (faceRadius * 0.58f), blushY)
                    )
                    // Right blush
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFF4081).copy(alpha = 0.45f), Color.Transparent),
                            center = Offset(center.x + (faceRadius * 0.58f), blushY),
                            radius = blushRadius
                        ),
                        radius = blushRadius,
                        center = Offset(center.x + (faceRadius * 0.58f), blushY)
                    )

                    // 3. LEFT EYE (Open, smiling & sparkling)
                    val leftEyeX = center.x - (faceRadius * 0.35f)
                    val leftEyeY = center.y - (faceRadius * 0.18f)
                    val eyeRadius = faceRadius * 0.16f

                    // Deep dark brown/black eye circle
                    drawCircle(
                        color = Color(0xFF261908),
                        radius = eyeRadius,
                        center = Offset(leftEyeX, leftEyeY)
                    )
                    // Sparkling white gleam dot
                    drawCircle(
                        color = Color.White,
                        radius = eyeRadius * 0.42f,
                        center = Offset(leftEyeX - (eyeRadius * 0.35f), leftEyeY - (eyeRadius * 0.35f))
                    )
                    // Secondary tiny gleam dot
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = eyeRadius * 0.22f,
                        center = Offset(leftEyeX + (eyeRadius * 0.35f), leftEyeY + (eyeRadius * 0.35f))
                    )

                    // Left Eyebrow (Joyful arch)
                    val leftBrowPath = Path().apply {
                        moveTo(leftEyeX - (eyeRadius * 1.3f), leftEyeY - (eyeRadius * 1.5f))
                        quadraticBezierTo(
                            leftEyeX,
                            leftEyeY - (eyeRadius * 2.1f),
                            leftEyeX + (eyeRadius * 1.2f),
                            leftEyeY - (eyeRadius * 1.3f)
                        )
                    }
                    drawPath(
                        path = leftBrowPath,
                        color = Color(0xFF4E342E),
                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                    )

                    // 4. RIGHT EYE (Playful Animated Wink 😉)
                    val rightEyeX = center.x + (faceRadius * 0.35f)
                    val rightEyeY = center.y - (faceRadius * 0.18f)
                    val winkWidth = faceRadius * 0.34f * winkProgress

                    // Playful curved wink stroke
                    val winkPath = Path().apply {
                        moveTo(rightEyeX - (winkWidth * 0.9f), rightEyeY + 4f)
                        quadraticBezierTo(
                            rightEyeX,
                            rightEyeY - (faceRadius * 0.22f * winkProgress),
                            rightEyeX + (winkWidth * 0.9f),
                            rightEyeY + 4f
                        )
                    }
                    drawPath(
                        path = winkPath,
                        color = Color(0xFF261908),
                        style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Right Eyebrow (Winking expressive tilt)
                    val rightBrowPath = Path().apply {
                        moveTo(rightEyeX - (winkWidth * 0.8f), rightEyeY - (faceRadius * 0.28f))
                        quadraticBezierTo(
                            rightEyeX,
                            rightEyeY - (faceRadius * 0.38f),
                            rightEyeX + (winkWidth * 0.9f),
                            rightEyeY - (faceRadius * 0.24f)
                        )
                    }
                    drawPath(
                        path = rightBrowPath,
                        color = Color(0xFF4E342E),
                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                    )

                    // 5. WIDE JOYFUL SMILE WITH TONGUE / ROSY ACCENT
                    val mouthWidth = faceRadius * 0.72f
                    val mouthY = center.y + (faceRadius * 0.26f)

                    // Open smiling mouth path
                    val mouthPath = Path().apply {
                        moveTo(center.x - (mouthWidth / 2f), mouthY)
                        quadraticBezierTo(
                            center.x,
                            mouthY + (faceRadius * 0.48f),
                            center.x + (mouthWidth / 2f),
                            mouthY
                        )
                        close()
                    }

                    // Mouth dark cavity
                    drawPath(
                        path = mouthPath,
                        color = Color(0xFF2E1500),
                        style = Fill
                    )

                    // Rosy tongue accent inside mouth
                    val tonguePath = Path().apply {
                        moveTo(center.x - (mouthWidth * 0.32f), mouthY + (faceRadius * 0.24f))
                        quadraticBezierTo(
                            center.x,
                            mouthY + (faceRadius * 0.48f),
                            center.x + (mouthWidth * 0.32f),
                            mouthY + (faceRadius * 0.24f)
                        )
                        close()
                    }
                    drawPath(
                        path = tonguePath,
                        color = Color(0xFFFF5252),
                        style = Fill
                    )

                    // Smile top outline lip line
                    val lipLinePath = Path().apply {
                        moveTo(center.x - (mouthWidth / 2f) - 4f, mouthY)
                        quadraticBezierTo(
                            center.x,
                            mouthY + (faceRadius * 0.08f),
                            center.x + (mouthWidth / 2f) + 4f,
                            mouthY
                        )
                    }
                    drawPath(
                        path = lipLinePath,
                        color = Color(0xFF2E1500),
                        style = Stroke(width = 6.5f, cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Confirmed Command Summary Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x3300E676))
                    .border(1.5.dp, NeonGreen.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MarkEmailRead,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Orden confirmada en el Host",
                            color = NeonGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (commandSummary.isNotBlank()) "\"$commandSummary\"" else "\"Petición procesada correctamente\"",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Auto-dismiss countdown indicator bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cerrando automáticamente ✨",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Draws a 4-point golden sparkle star
 */
private fun DrawScope.drawSparkleStar(center: Offset, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - size)
        quadraticBezierTo(center.x, center.y, center.x + size, center.y)
        quadraticBezierTo(center.x, center.y, center.x, center.y + size)
        quadraticBezierTo(center.x, center.y, center.x - size, center.y)
        quadraticBezierTo(center.x, center.y, center.x, center.y - size)
        close()
    }
    drawPath(path = path, color = color, style = Fill)
}

