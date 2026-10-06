package com.example.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.MediaCardItem
import com.example.data.MediaDataParser
import com.example.data.MediaItemCategory
import com.example.ui.theme.AmberYellow
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ZenOledBlack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Vista flotante deslizable en efecto 3D estilo Cover Flow cinemático,
 * idéntica a la composición y estética de la imagen de referencia (Dune, The Batman, John Wick...).
 * Incluye perspectiva 3D (rotationY + cameraDistance), reflejo realista sobre suelo de espejo negro,
 * control de navegación con flechas y barra de desplazamiento interactiva con indicador brillante.
 */
@Composable
fun Floating3dCoverFlowOverlay(
    items: List<MediaCardItem>,
    currentSongTitle: String = "",
    currentCoverUrl: String = "",
    playbackState: String = "",
    isPlaybackActive: Boolean = false,
    keepOpenDuringPlayback: Boolean = false,
    hostVolume: Int = 8,
    isHostMuted: Boolean = false,
    onSetHostVolume: ((Int) -> Unit)? = null,
    onPreviousTrack: (() -> Unit)? = null,
    onTogglePlayPause: (() -> Unit)? = null,
    onNextTrack: (() -> Unit)? = null,
    onToggleKeepOpenDuringPlayback: ((Boolean) -> Unit)? = null,
    onDismiss: () -> Unit,
    onPlayMedia: (MediaCardItem) -> Unit
) {
    val isSampleMode = items.isEmpty()
    val displayCards = remember(items, currentSongTitle, currentCoverUrl) {
        val baseList = if (items.isNotEmpty()) items else MediaDataParser.getCinematicPresetCards(currentSongTitle, currentCoverUrl)
        // Deduplicación estricta para garantizar que la canción actualmente reproduciéndose jamás aparezca duplicada
        val deduplicated = mutableListOf<MediaCardItem>()
        val seen = mutableSetOf<String>()
        for (card in baseList) {
            val isCurrentMatch = currentSongTitle.isNotBlank() && MediaDataParser.isSameTrack(card.title, card.artist, currentSongTitle, "")
            val key = if (isCurrentMatch) "CURRENT_SONG_UNIQUE_KEY" else "${card.title.trim().lowercase()}|${card.artist.trim().lowercase()}"
            if (seen.add(key)) {
                val finalCard = if (isCurrentMatch && card.category != MediaItemCategory.CURRENT) {
                    card.copy(category = MediaItemCategory.CURRENT)
                } else card
                deduplicated.add(finalCard)
            }
        }
        deduplicated
    }

    val pageCount = displayCards.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val density = LocalDensity.current

    // Estado local reactivo de volumen del Host (0 a 15)
    var localVolume by remember(hostVolume) { mutableIntStateOf(hostVolume.coerceIn(0, 15)) }

    // Animación de saltos de rebote (keyframes) exclusiva para la portada que se está reproduciendo actualmente
    val bounceInfiniteTransition = rememberInfiniteTransition(label = "now_playing_bounce")
    val bounceYOffset by bounceInfiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 850
                0f at 0 with FastOutSlowInEasing
                -20f at 280 with FastOutLinearInEasing
                -25f at 380 with LinearOutSlowInEasing
                -22f at 430 with FastOutLinearInEasing
                0f at 640 with FastOutSlowInEasing
                -6f at 720 with FastOutSlowInEasing
                0f at 800 with LinearOutSlowInEasing
                0f at 850 with LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "bounceYOffset"
    )

    // Mantener la pantalla encendida mientras la cola esté abierta y la opción de mantener en reproducción esté activa
    val shouldKeepScreenOn = keepOpenDuringPlayback && isPlaybackActive
    DisposableEffect(shouldKeepScreenOn) {
        if (shouldKeepScreenOn) {
            view.keepScreenOn = true
        }
        onDispose {
            view.keepScreenOn = false
        }
    }

    // Pulsing glow for the active center border
    val infiniteTransition = rememberInfiniteTransition(label = "glow_3d")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    fun haptic(feedbackConstant: Int = HapticFeedbackConstants.KEYBOARD_TAP) {
        view.performHapticFeedback(feedbackConstant)
    }

    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("coverflow_3d_prefs", android.content.Context.MODE_PRIVATE)
    }

    // Dimensiones y espaciado personalizables de las portadas (persistidos en SharedPreferences)
    var cardWidthVal by remember {
        mutableFloatStateOf(prefs.getFloat("card_width", 155f))
    }
    var cardHeightVal by remember {
        mutableFloatStateOf(prefs.getFloat("card_height", 215f))
    }
    var slotWidthVal by remember {
        mutableFloatStateOf(prefs.getFloat("slot_width", 76f))
    }
    var tiltAngleVal by remember {
        mutableFloatStateOf(prefs.getFloat("tilt_angle", 36f))
    }
    var showSizeEditor by remember { mutableStateOf(false) }

    // Auto-cierre por inactividad tras 10 segundos sin interacción
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var secondsRemaining by remember { mutableIntStateOf(10) }

    fun recordActivity() {
        lastInteractionTime = System.currentTimeMillis()
    }

    // Estado e indicador de apertura de canción
    var openingCard by remember { mutableStateOf<MediaCardItem?>(null) }
    var openingTimestamp by remember { mutableLongStateOf(0L) }

    LaunchedEffect(openingTimestamp) {
        if (openingCard != null) {
            delay(2800L)
            openingCard = null
        }
    }

    val onSelectCard: (MediaCardItem, Int) -> Unit = { selected, page ->
        recordActivity()
        haptic(HapticFeedbackConstants.LONG_PRESS)
        openingCard = selected
        openingTimestamp = System.currentTimeMillis()
        Toast.makeText(context, "Abriendo: ${selected.title}", Toast.LENGTH_SHORT).show()
        onPlayMedia(selected)
        if (page != pagerState.currentPage) {
            scope.launch {
                pagerState.animateScrollToPage(page)
            }
        }
    }

    // Cualquier desplazamiento del pager o cambio de página cuenta como actividad
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        recordActivity()
    }

    // Cuenta regresiva de 10 segundos para cerrar automáticamente
    // Se pausa mientras el editor esté abierto O mientras esté reproduciendo música si la opción está activada
    val isPlaybackKeepOpenActive = keepOpenDuringPlayback && isPlaybackActive
    LaunchedEffect(lastInteractionTime, showSizeEditor, isPlaybackKeepOpenActive) {
        if (showSizeEditor || isPlaybackKeepOpenActive) {
            secondsRemaining = 10
            return@LaunchedEffect
        }
        secondsRemaining = 10
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
        }
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZenOledBlack.copy(alpha = 0.96f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(pass = PointerEventPass.Initial)
                        recordActivity()
                    }
                }
            }
            .testTag("floating_3d_coverflow_root")
    ) {
        // Atmospheric Ambient Background Lights
        Box(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CyanAccent.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.4f),
                        radius = size.height * 0.7f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(PurpleAccent.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.2f, size.height * 0.3f),
                        radius = size.height * 0.5f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFE11D48).copy(alpha = 0.06f), Color.Transparent),
                        center = Offset(size.width * 0.8f, size.height * 0.3f),
                        radius = size.height * 0.5f
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP BAR: Clean & uncluttered — Opening track status on left (if active) + Controls on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status discreto si se está abriendo una canción
                if (openingCard != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NeonGreen.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, NeonGreen)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "ABRIENDO: ${openingCard?.title?.uppercase()}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = NeonGreen
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Right controls: Pin Toggle + Adjustment Button + Inactivity Countdown Badge + Close button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón para fijar/mantener cola abierta durante reproducción
                    if (onToggleKeepOpenDuringPlayback != null) {
                        Surface(
                            shape = CircleShape,
                            color = if (keepOpenDuringPlayback) {
                                if (isPlaybackActive) NeonGreen.copy(alpha = 0.25f) else CyanAccent.copy(alpha = 0.25f)
                            } else Color(0x33222222),
                            border = BorderStroke(
                                1.dp,
                                if (keepOpenDuringPlayback) {
                                    if (isPlaybackActive) NeonGreen else CyanAccent
                                } else Color(0x33FFFFFF)
                            ),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    haptic()
                                    recordActivity()
                                    val newState = !keepOpenDuringPlayback
                                    onToggleKeepOpenDuringPlayback(newState)
                                    Toast.makeText(
                                        context,
                                        if (newState) "Fijada: La cola se mantendrá abierta mientras haya música"
                                        else "Cierre automático reactivado (10s)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .testTag("toggle_keep_open_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Fijar cola abierta en reproducción",
                                    tint = if (keepOpenDuringPlayback) {
                                        if (isPlaybackActive) NeonGreen else CyanAccent
                                    } else TextSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    // Pequeñito botón de ajuste de tamaño y espaciado de portadas
                    Surface(
                        shape = CircleShape,
                        color = if (showSizeEditor) CyanAccent.copy(alpha = 0.25f) else Color(0x33222222),
                        border = BorderStroke(1.dp, if (showSizeEditor) CyanAccent else Color(0x33FFFFFF)),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                haptic()
                                recordActivity()
                                showSizeEditor = !showSizeEditor
                            }
                            .testTag("coverflow_size_settings_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ajustar Tamaño y Espaciado",
                                tint = if (showSizeEditor) CyanAccent else TextPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    // Auto-close countdown pill or Live Playback Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPlaybackKeepOpenActive) NeonGreen.copy(alpha = 0.15f) else Color(0x221E293B),
                        border = BorderStroke(
                            1.dp,
                            when {
                                isPlaybackKeepOpenActive -> NeonGreen.copy(alpha = 0.7f)
                                showSizeEditor -> CyanAccent.copy(alpha = 0.6f)
                                secondsRemaining <= 3 -> Color(0x99EF4444)
                                else -> CyanAccent.copy(alpha = 0.35f)
                            }
                        ),
                        modifier = Modifier.clickable {
                            if (isPlaybackKeepOpenActive) {
                                Toast.makeText(
                                    context,
                                    "Cola fija activa: abierta mientras el host reproduzca música",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    isPlaybackKeepOpenActive -> Icons.Default.GraphicEq
                                    showSizeEditor -> Icons.Default.Timer
                                    else -> Icons.Default.Timer
                                },
                                contentDescription = null,
                                tint = when {
                                    isPlaybackKeepOpenActive -> NeonGreen
                                    showSizeEditor -> CyanAccent
                                    secondsRemaining <= 3 -> Color(0xFFEF4444)
                                    else -> CyanAccent
                                },
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = when {
                                    isPlaybackKeepOpenActive -> "EN VIVO"
                                    showSizeEditor -> "PAUSA"
                                    else -> "${secondsRemaining}s"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = when {
                                    isPlaybackKeepOpenActive -> NeonGreen
                                    showSizeEditor -> CyanAccent
                                    secondsRemaining <= 3 -> Color(0xFFEF4444)
                                    else -> TextSecondary
                                }
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0x33222222),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                haptic()
                                onDismiss()
                            }
                            .testTag("close_3d_coverflow_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // CENTER: 3D Perspective Carousel Stage with Floor Reflection
            // Compact, overlapping layout with user-customizable card dimensions and spacing
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val containerWidth = maxWidth
                val cardWidth = cardWidthVal.dp
                val cardHeight = cardHeightVal.dp
                val reflectionHeight = (cardHeightVal * 0.26f).coerceIn(28f, 90f).dp
                // slotWidth defines spacing between consecutive card centers.
                val slotWidth = slotWidthVal.dp
                val horizontalPadding = ((containerWidth - slotWidth) / 2).coerceAtLeast(0.dp)

                // Floating Cybernetic HUD banner indicating song opening
                androidx.compose.animation.AnimatedVisibility(
                    visible = openingCard != null,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(300f)
                        .padding(top = 4.dp)
                ) {
                    openingCard?.let { opened ->
                        Surface(
                            shape = CutCornerShape(10.dp),
                            color = Color(0xFF021B10),
                            border = BorderStroke(1.5.dp, NeonGreen),
                            modifier = Modifier.shadow(12.dp, shape = CutCornerShape(10.dp), ambientColor = NeonGreen, spotColor = NeonGreen)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "ABRIENDO CANCIÓN: ${opened.title.uppercase()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    pageSize = PageSize.Fixed(slotWidth),
                    contentPadding = PaddingValues(horizontal = horizontalPadding),
                    pageSpacing = 0.dp,
                    beyondViewportPageCount = 5,
                    modifier = Modifier.fillMaxSize(),
                    flingBehavior = PagerDefaults.flingBehavior(state = pagerState)
                ) { page ->
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                    val card = displayCards.getOrNull(page) ?: return@HorizontalPager

                    val absOffset = pageOffset.absoluteValue

                    // 3D Matrix Transformations:
                    // Rotation Y with user-customizable tilt angle
                    val rotationY = (pageOffset * -tiltAngleVal).coerceIn(-(tiltAngleVal + 12f), tiltAngleVal + 12f)

                    // Scale: Center is 1.16x, smoothly descends for flanking cards
                    val scale = (1.16f - (absOffset.coerceAtMost(3.5f) * 0.12f)).coerceIn(0.72f, 1.16f)

                    // Dimming: Center is 1.0, progressive depth fade
                    val alpha = (1.0f - (absOffset.coerceAtMost(4f) * 0.12f)).coerceIn(0.42f, 1.0f)

                    // Stacking zIndex: closer to center is always on top
                    val zIndexValue = 100f - absOffset * 10f

                    val isCentered = absOffset < 0.35f

                    Box(
                        modifier = Modifier
                            .zIndex(zIndexValue)
                            .graphicsLayer {
                                this.cameraDistance = 14f * density.density
                                this.rotationY = rotationY
                                this.scaleX = scale
                                this.scaleY = scale
                                this.alpha = alpha
                            }
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val isCardCurrentlyPlaying = (card.category == MediaItemCategory.CURRENT ||
                            (currentSongTitle.isNotBlank() && card.title.equals(currentSongTitle, ignoreCase = true))) &&
                            (isPlaybackActive || currentSongTitle.isNotBlank())

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // 1. MAIN 3D POSTER / ALBUM CARD: Tap anywhere on cover opens song
                            // Salto de rebote animado solo para la portada de la canción actualmente en reproducción
                            CoverFlowCardItem(
                                card = card,
                                isCentered = isCentered,
                                isCurrentlyPlaying = isCardCurrentlyPlaying,
                                isOpeningThisCard = openingCard?.id == card.id,
                                glowAlpha = if (isCentered) glowAlpha else 0f,
                                width = cardWidth,
                                height = cardHeight,
                                modifier = Modifier
                                    .graphicsLayer {
                                        if (isCardCurrentlyPlaying) {
                                            translationY = bounceYOffset * density.density
                                        }
                                    }
                                    .pointerInput(card.id, page) {
                                        detectTapGestures(
                                            onTap = {
                                                // Ensure swipe gestures are not mixed with simple tap
                                                if (!pagerState.isScrollInProgress) {
                                                    onSelectCard(card, page)
                                                }
                                            }
                                        )
                                    }
                            )

                            // 2. FLOOR MIRROR REFLECTION (Reflejo sobre piso de cristal negro)
                            MirrorFloorReflection(
                                card = card,
                                isCentered = isCentered,
                                width = cardWidth,
                                height = reflectionHeight,
                                modifier = Modifier.graphicsLayer {
                                    if (isCardCurrentlyPlaying) {
                                        this.alpha = (1f - (bounceYOffset.absoluteValue / 30f) * 0.45f).coerceIn(0.35f, 1f)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // BOTTOM CONTROL DECK: Multimedia Controls (Prev, Play/Pause, Next) + Host Volume Slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Flecha < para desplazar carátulas 3D
                Surface(
                    shape = CircleShape,
                    color = Color(0x221E293B),
                    border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            recordActivity()
                            haptic()
                            scope.launch {
                                val prev = (pagerState.currentPage - 1).coerceAtLeast(0)
                                pagerState.animateScrollToPage(prev)
                            }
                        }
                        .testTag("coverflow_nav_left")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Carátula Anterior",
                            tint = if (pagerState.currentPage > 0) CyanAccent else Color(0x554B5563),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // BOTÓN MULTIMEDIA: ANTERIOR PISTA
                Surface(
                    shape = CircleShape,
                    color = Color(0x331E293B),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            recordActivity()
                            haptic()
                            onPreviousTrack?.invoke()
                        }
                        .testTag("coverflow_prev_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Pista Anterior",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // BOTÓN MULTIMEDIA PRINCIPAL: PLAY / PAUSA
                Surface(
                    shape = CircleShape,
                    color = if (isPlaybackActive) NeonGreen.copy(alpha = 0.22f) else CyanAccent.copy(alpha = 0.22f),
                    border = BorderStroke(1.5.dp, if (isPlaybackActive) NeonGreen else CyanAccent),
                    modifier = Modifier
                        .size(48.dp)
                        .clickable {
                            recordActivity()
                            haptic(HapticFeedbackConstants.LONG_PRESS)
                            onTogglePlayPause?.invoke()
                        }
                        .testTag("coverflow_play_pause_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaybackActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaybackActive) "Pausar" else "Reproducir",
                            tint = if (isPlaybackActive) NeonGreen else CyanAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // BOTÓN MULTIMEDIA: SIGUIENTE PISTA
                Surface(
                    shape = CircleShape,
                    color = Color(0x331E293B),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            recordActivity()
                            haptic()
                            onNextTrack?.invoke()
                        }
                        .testTag("coverflow_next_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Pista Siguiente",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Flecha > para desplazar carátulas 3D
                Surface(
                    shape = CircleShape,
                    color = Color(0x221E293B),
                    border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            recordActivity()
                            haptic()
                            scope.launch {
                                val next = (pagerState.currentPage + 1).coerceAtMost(pageCount - 1)
                                pagerState.animateScrollToPage(next)
                            }
                        }
                        .testTag("coverflow_nav_right")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Carátula Siguiente",
                            tint = if (pagerState.currentPage < pageCount - 1) CyanAccent else Color(0x554B5563),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Divisor Vertical
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(Color(0x33FFFFFF))
                )

                Spacer(modifier = Modifier.width(16.dp))

                // CONTROL DESLIZANTE DE VOLUMEN DEL HOST
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x221E293B),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .widthIn(min = 180.dp, max = 280.dp)
                        .height(40.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isHostMuted || localVolume == 0 -> Icons.Default.VolumeOff
                                localVolume < 7 -> Icons.Default.VolumeDown
                                else -> Icons.Default.VolumeUp
                            },
                            contentDescription = "Volumen",
                            tint = if (isHostMuted) Color(0xFFEF4444) else CyanAccent,
                            modifier = Modifier.size(19.dp)
                        )

                        Slider(
                            value = localVolume.toFloat(),
                            onValueChange = { newVol ->
                                recordActivity()
                                localVolume = newVol.roundToInt().coerceIn(0, 15)
                                onSetHostVolume?.invoke(localVolume)
                            },
                            valueRange = 0f..15f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanAccent,
                                activeTrackColor = CyanAccent,
                                inactiveTrackColor = Color(0x3338BDF8)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                        )

                        Text(
                            text = "Vol $localVolume",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyanAccent,
                            modifier = Modifier.widthIn(min = 36.dp)
                        )
                    }
                }
            }
        }

        // PANEL FLOTANTE DE AJUSTES: Redimensionar y espaciar portadas 3D
        AnimatedVisibility(
            visible = showSizeEditor,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 62.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xF20F172A),
                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 340.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        recordActivity()
                    }
                    .testTag("coverflow_size_editor_panel")
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header con Título, Botón Restablecer y Botón Cerrar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "AJUSTES DE PORTADA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CyanAccent,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Botón Restablecer
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0x33334155),
                                border = BorderStroke(0.5.dp, Color(0x44FFFFFF)),
                                modifier = Modifier
                                    .clickable {
                                        haptic()
                                        recordActivity()
                                        cardWidthVal = 155f
                                        cardHeightVal = 215f
                                        slotWidthVal = 76f
                                        tiltAngleVal = 36f
                                        prefs.edit()
                                            .putFloat("card_width", 155f)
                                            .putFloat("card_height", 215f)
                                            .putFloat("slot_width", 76f)
                                            .putFloat("tilt_angle", 36f)
                                            .apply()
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Restablecer",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "Reset",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // Botón cerrar panel
                            Surface(
                                shape = CircleShape,
                                color = Color(0x22FFFFFF),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        haptic()
                                        recordActivity()
                                        showSizeEditor = false
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cerrar editor",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 1. Ancho de portada Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ancho de portada",
                                fontSize = 11.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${cardWidthVal.roundToInt()} dp",
                                fontSize = 11.sp,
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = cardWidthVal,
                            onValueChange = {
                                cardWidthVal = it
                                recordActivity()
                                prefs.edit().putFloat("card_width", it).apply()
                            },
                            valueRange = 90f..240f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanAccent,
                                activeTrackColor = CyanAccent,
                                inactiveTrackColor = Color(0x3338BDF8)
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }

                    // 2. Alto de portada Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Alto de portada",
                                fontSize = 11.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${cardHeightVal.roundToInt()} dp",
                                fontSize = 11.sp,
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = cardHeightVal,
                            onValueChange = {
                                cardHeightVal = it
                                recordActivity()
                                prefs.edit().putFloat("card_height", it).apply()
                            },
                            valueRange = 130f..320f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanAccent,
                                activeTrackColor = CyanAccent,
                                inactiveTrackColor = Color(0x3338BDF8)
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }

                    // 3. Espaciado entre portadas Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Espaciado (Paso)",
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Menor = Más traslape 3D",
                                    fontSize = 9.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "${slotWidthVal.roundToInt()} dp",
                                fontSize = 11.sp,
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = slotWidthVal,
                            onValueChange = {
                                slotWidthVal = it
                                recordActivity()
                                prefs.edit().putFloat("slot_width", it).apply()
                            },
                            valueRange = 30f..180f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanAccent,
                                activeTrackColor = CyanAccent,
                                inactiveTrackColor = Color(0x3338BDF8)
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }

                    // 4. Inclinación 3D Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Inclinación 3D",
                                fontSize = 11.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${tiltAngleVal.roundToInt()}°",
                                fontSize = 11.sp,
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = tiltAngleVal,
                            onValueChange = {
                                tiltAngleVal = it
                                recordActivity()
                                prefs.edit().putFloat("tilt_angle", it).apply()
                            },
                            valueRange = 10f..60f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanAccent,
                                activeTrackColor = CyanAccent,
                                inactiveTrackColor = Color(0x3338BDF8)
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }

                    // Opción: Mantener cola de reproducción abierta durante la música
                    if (onToggleKeepOpenDuringPlayback != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x221E293B),
                            border = BorderStroke(1.dp, if (keepOpenDuringPlayback) CyanAccent.copy(alpha = 0.5f) else Color(0x22FFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Mantener cola en reproducción",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Mantiene la pantalla abierta sin auto-cierre mientras el host reproduzca música",
                                        fontSize = 9.5.sp,
                                        color = TextMuted
                                    )
                                }
                                androidx.compose.material3.Switch(
                                    checked = keepOpenDuringPlayback,
                                    onCheckedChange = {
                                        recordActivity()
                                        onToggleKeepOpenDuringPlayback(it)
                                    },
                                    colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = CyanAccent)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta individual del Coverflow 3D.
 */
@Composable
private fun CoverFlowCardItem(
    card: MediaCardItem,
    isCentered: Boolean,
    isCurrentlyPlaying: Boolean = false,
    isOpeningThisCard: Boolean = false,
    glowAlpha: Float,
    width: Dp = 155.dp,
    height: Dp = 215.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141822))
            .border(
                width = if (isOpeningThisCard || isCurrentlyPlaying) 3.dp else if (isCentered) 2.5.dp else 1.dp,
                color = if (isOpeningThisCard || isCurrentlyPlaying) NeonGreen else if (isCentered) CyanAccent.copy(alpha = glowAlpha) else Color(0x2AFFFFFF),
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("coverflow_card_${card.id}")
    ) {
        // Poster / Cover Art
        if (card.coverUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(card.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = card.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Elegant Stylized Gradient Poster with Typography
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A),
                                Color(0xFF020617)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        imageVector = if (card.category == MediaItemCategory.PLAYLIST) Icons.Default.QueueMusic else Icons.Default.Album,
                        contentDescription = null,
                        tint = CyanAccent.copy(alpha = 0.7f),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = card.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Dark Vignette at bottom for text contrast
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // Overlay Text: Title & Artist / Subtitle
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = card.title,
                fontSize = if (isCentered) 13.sp else 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.5.sp
            )
            Text(
                text = card.displaySubtitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                color = if (card.isSample) AmberYellow.copy(alpha = 0.85f) else if (isCentered) CyanAccent else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Indicador EN VIVO cuando esta carátula es la canción en reproducción
        if (isCurrentlyPlaying) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xD9052E16),
                border = BorderStroke(1.dp, NeonGreen),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "EN VIVO",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.6.sp
                    )
                }
            }
        }

        // Indicador visual de apertura al hacer tap en cualquier parte de la portada
        if (isOpeningThisCard) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = NeonGreen.copy(alpha = 0.95f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = ZenOledBlack,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "ABRIENDO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = ZenOledBlack,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.6.sp
                    )
                }
            }
        }
    }
}

/**
 * Reflejo invertido sobre el suelo de cristal negro (Floor Mirror Reflection),
 * replicando exactamente la atmósfera del render cinemático del usuario.
 */
@Composable
private fun MirrorFloorReflection(
    card: MediaCardItem,
    isCentered: Boolean,
    width: Dp = 155.dp,
    height: Dp = 55.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .graphicsLayer {
                scaleY = -1f // Flips vertically!
                alpha = if (isCentered) 0.38f else 0.22f
            }
    ) {
        // Mirrored image content
        if (card.coverUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(card.coverUrl)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
            )
        }

        // Glossy gradient fade mask from transparent (top of reflection) to total black (bottom)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            ZenOledBlack.copy(alpha = 0.65f),
                            ZenOledBlack
                        )
                    )
                )
        )
    }
}
