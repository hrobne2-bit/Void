package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.MediaStateHolder
import com.example.data.ThemeEntity
import com.example.data.ThemeRepository
import com.example.data.formatTime
import com.example.data.parseColorHex
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingControllerService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private lateinit var themeRepository: ThemeRepository
    private val currentThemeState = mutableStateOf<ThemeEntity?>(null)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        val database = AppDatabase.getDatabase(this)
        themeRepository = ThemeRepository(database.themeDao())

        // Observe active theme updates dynamically
        lifecycleScope.launch {
            themeRepository.activeTheme.collectLatest { theme ->
                currentThemeState.value = theme ?: ThemeEntity.createDefault()
            }
        }

        setupFloatingWindow()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        return START_STICKY
    }

    private fun setupFloatingWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 150
            y = 350
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingControllerService)
            setViewTreeViewModelStoreOwner(this@FloatingControllerService)
            setViewTreeSavedStateRegistryOwner(this@FloatingControllerService)

            setContent {
                val theme = currentThemeState.value ?: ThemeEntity.createDefault()
                FloatingControllerContent(
                    theme = theme,
                    onClose = { stopSelf() },
                    onDrag = { dx, dy ->
                        this@FloatingControllerService.layoutParams?.let { params ->
                            params.x += dx
                            params.y += dy
                            windowManager?.updateViewLayout(this, params)
                        }
                    }
                )
            }
        }

        try {
            windowManager?.addView(composeView, layoutParams)
        } catch (e: Exception) {
            Log.e("FloatingController", "Failed to add floating view: ${e.message}")
        }
    }

    override fun onDestroy() {
        composeView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // Already removed or not added
            }
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }
}

// Custom Easing Mapper
fun getEasing(curve: String): Easing {
    return when (curve) {
        "LINEAR" -> LinearEasing
        "EASE_IN" -> FastOutLinearInEasing
        "EASE_OUT" -> LinearOutSlowInEasing
        "BOUNCE" -> Easing { fraction ->
            if (fraction < 0.3636f) {
                7.5625f * fraction * fraction
            } else if (fraction < 0.7272f) {
                val f = fraction - 0.5454f
                7.5625f * f * f + 0.75f
            } else if (fraction < 0.9090f) {
                val f = fraction - 0.8181f
                7.5625f * f * f + 0.9375f
            } else {
                val f = fraction - 0.9545f
                7.5625f * f * f + 0.984375f
            }
        }
        "SPRING" -> Easing { fraction ->
            // High-fidelity spring easing function
            val t = fraction
            if (t >= 1f) 1f else (1f - Math.exp(-4.5 * t) * Math.cos(9.0 * t)).toFloat()
        }
        else -> FastOutSlowInEasing
    }
}

// Extractor of vibrant color from the album artwork with custom Saturation/Brightness modulations
fun getDynamicIconColor(artwork: Bitmap?, baseColorHex: String, saturationScale: Float, brightnessScale: Float): Color {
    val baseColor = try {
        android.graphics.Color.parseColor(baseColorHex)
    } catch (e: Exception) {
        android.graphics.Color.WHITE
    }
    
    val extractedColor = if (artwork != null) {
        var redSum = 0L
        var greenSum = 0L
        var blueSum = 0L
        var count = 0
        val stepX = (artwork.width / 10).coerceAtLeast(1)
        val stepY = (artwork.height / 10).coerceAtLeast(1)
        
        for (x in stepX until artwork.width step stepX) {
            for (y in stepY until artwork.height step stepY) {
                val pixel = artwork.getPixel(x, y)
                val alpha = android.graphics.Color.alpha(pixel)
                if (alpha > 200) {
                    redSum += android.graphics.Color.red(pixel)
                    greenSum += android.graphics.Color.green(pixel)
                    blueSum += android.graphics.Color.blue(pixel)
                    count++
                }
            }
        }
        
        if (count > 0) {
            android.graphics.Color.rgb(
                (redSum / count).toInt(),
                (greenSum / count).toInt(),
                (blueSum / count).toInt()
            )
        } else {
            baseColor
        }
    } else {
        baseColor
    }
    
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(extractedColor, hsv)
    hsv[1] = (hsv[1] * saturationScale).coerceIn(0f, 1f)
    hsv[2] = (hsv[2] * brightnessScale).coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

// Custom click modifier supporting Gaming Mode (unresponsive to single, works with double-tap)
fun Modifier.gamingClickable(
    isGamingMode: Boolean,
    onAction: () -> Unit
): Modifier = this.pointerInput(isGamingMode) {
    detectTapGestures(
        onTap = {
            if (!isGamingMode) {
                onAction()
            }
        },
        onDoubleTap = {
            if (isGamingMode) {
                onAction()
            }
        }
    )
}

@Composable
fun PackIcon(
    iconType: String, // PLAY, PAUSE, PREVIOUS, NEXT, CLOSE, SETTINGS
    packName: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    when (packName) {
        "SHARP" -> {
            androidx.compose.foundation.Canvas(modifier = modifier) {
                when (iconType) {
                    "PLAY" -> {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width * 0.25f, size.height * 0.2f)
                            lineTo(size.width * 0.85f, size.height * 0.5f)
                            lineTo(size.width * 0.25f, size.height * 0.8f)
                            close()
                        }
                        drawPath(path, color = tint)
                    }
                    "PAUSE" -> {
                        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.2f), size = androidx.compose.ui.geometry.Size(size.width * 0.2f, size.height * 0.6f))
                        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.6f, size.height * 0.2f), size = androidx.compose.ui.geometry.Size(size.width * 0.2f, size.height * 0.6f))
                    }
                    "PREVIOUS" -> {
                        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.2f), size = androidx.compose.ui.geometry.Size(size.width * 0.15f, size.height * 0.6f))
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width * 0.85f, size.height * 0.2f)
                            lineTo(size.width * 0.35f, size.height * 0.5f)
                            lineTo(size.width * 0.85f, size.height * 0.8f)
                            close()
                        }
                        drawPath(path, color = tint)
                    }
                    "NEXT" -> {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width * 0.15f, size.height * 0.2f)
                            lineTo(size.width * 0.65f, size.height * 0.5f)
                            lineTo(size.width * 0.15f, size.height * 0.8f)
                            close()
                        }
                        drawPath(path, color = tint)
                        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.7f, size.height * 0.2f), size = androidx.compose.ui.geometry.Size(size.width * 0.15f, size.height * 0.6f))
                    }
                    "SETTINGS" -> {
                        drawCircle(color = tint, radius = size.minDimension * 0.35f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                        drawCircle(color = tint, radius = size.minDimension * 0.12f)
                    }
                    else -> {
                        drawLine(color = tint, start = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.25f), end = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.75f), strokeWidth = 2.dp.toPx())
                        drawLine(color = tint, start = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.25f), end = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.75f), strokeWidth = 2.dp.toPx())
                    }
                }
            }
        }
        "MINIMAL" -> {
            androidx.compose.foundation.Canvas(modifier = modifier) {
                when (iconType) {
                    "PLAY" -> {
                        drawCircle(color = tint, radius = size.minDimension * 0.4f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                        drawCircle(color = tint, radius = size.minDimension * 0.12f)
                    }
                    "PAUSE" -> {
                        drawCircle(color = tint, center = androidx.compose.ui.geometry.Offset(size.width * 0.35f, size.height * 0.5f), radius = size.minDimension * 0.12f)
                        drawCircle(color = tint, center = androidx.compose.ui.geometry.Offset(size.width * 0.65f, size.height * 0.5f), radius = size.minDimension * 0.12f)
                    }
                    "PREVIOUS" -> {
                        drawCircle(color = tint, center = androidx.compose.ui.geometry.Offset(size.width * 0.3f, size.height * 0.5f), radius = size.minDimension * 0.1f)
                        drawLine(color = tint, start = androidx.compose.ui.geometry.Offset(size.width * 0.7f, size.height * 0.3f), end = androidx.compose.ui.geometry.Offset(size.width * 0.7f, size.height * 0.7f), strokeWidth = 2.dp.toPx())
                    }
                    "NEXT" -> {
                        drawLine(color = tint, start = androidx.compose.ui.geometry.Offset(size.width * 0.3f, size.height * 0.3f), end = androidx.compose.ui.geometry.Offset(size.width * 0.3f, size.height * 0.7f), strokeWidth = 2.dp.toPx())
                        drawCircle(color = tint, center = androidx.compose.ui.geometry.Offset(size.width * 0.7f, size.height * 0.5f), radius = size.minDimension * 0.1f)
                    }
                    "SETTINGS" -> {
                        drawRect(color = tint, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                    }
                    else -> {
                        drawLine(color = tint, start = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.25f), end = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.75f), strokeWidth = 2.dp.toPx())
                        drawLine(color = tint, start = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.25f), end = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.75f), strokeWidth = 2.dp.toPx())
                    }
                }
            }
        }
        "OUTLINED" -> {
            androidx.compose.foundation.Canvas(modifier = modifier) {
                when (iconType) {
                    "PLAY" -> {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width * 0.32f, size.height * 0.25f)
                            lineTo(size.width * 0.78f, size.height * 0.5f)
                            lineTo(size.width * 0.32f, size.height * 0.75f)
                            close()
                        }
                        drawPath(path, color = tint, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8.dp.toPx()))
                    }
                    "PAUSE" -> {
                        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.25f), size = androidx.compose.ui.geometry.Size(size.width * 0.12f, size.height * 0.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
                        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.62f, size.height * 0.25f), size = androidx.compose.ui.geometry.Size(size.width * 0.12f, size.height * 0.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
                    }
                    "PREVIOUS" -> {
                        drawLine(color = tint, start = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.25f), end = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.75f), strokeWidth = 2.dp.toPx())
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width * 0.8f, size.height * 0.25f)
                            lineTo(size.width * 0.35f, size.height * 0.5f)
                            lineTo(size.width * 0.8f, size.height * 0.75f)
                            close()
                        }
                        drawPath(path, color = tint, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
                    }
                    "NEXT" -> {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width * 0.2f, size.height * 0.25f)
                            lineTo(size.width * 0.65f, size.height * 0.5f)
                            lineTo(size.width * 0.2f, size.height * 0.75f)
                            close()
                        }
                        drawPath(path, color = tint, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
                        drawLine(color = tint, start = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.25f), end = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.75f), strokeWidth = 2.dp.toPx())
                    }
                    "SETTINGS" -> {
                        drawCircle(color = tint, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
                    }
                    else -> {
                        drawLine(color = tint, start = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.25f), end = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.75f), strokeWidth = 2.dp.toPx())
                        drawLine(color = tint, start = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.25f), end = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.75f), strokeWidth = 2.dp.toPx())
                    }
                }
            }
        }
        else -> {
            val vector = when (iconType) {
                "PLAY" -> Icons.Default.PlayArrow
                "PAUSE" -> Icons.Default.Pause
                "PREVIOUS" -> Icons.Default.SkipPrevious
                "NEXT" -> Icons.Default.SkipNext
                "SETTINGS" -> Icons.Default.Settings
                else -> Icons.Default.Close
            }
            Icon(
                imageVector = vector,
                contentDescription = null,
                tint = tint,
                modifier = modifier
            )
        }
    }
}

@Composable
fun FloatingControllerContent(
    theme: ThemeEntity,
    onClose: () -> Unit,
    onDrag: (Int, Int) -> Unit
) {
    val trackTitle by MediaStateHolder.trackTitle.collectAsState()
    val trackArtist by MediaStateHolder.trackArtist.collectAsState()
    val trackArtwork by MediaStateHolder.trackArtwork.collectAsState()
    val isPlaying by MediaStateHolder.isPlaying.collectAsState()
    val trackDuration by MediaStateHolder.trackDuration.collectAsState()
    val trackPosition by MediaStateHolder.trackPosition.collectAsState()

    val trackArtworkLocal = trackArtwork

    var isExpanded by remember { mutableStateOf(false) }

    // Custom Font styling
    val fontFamily = when (theme.fontStyle) {
        "MONOSPACE" -> FontFamily.Monospace
        "MINIMAL" -> FontFamily.SansSerif
        else -> FontFamily.Default
    }
    val fontWeight = if (theme.fontStyle == "MINIMAL") FontWeight.Light else FontWeight.Normal

    // Colors: base colors adjusted by user or cover-art prominent color if selected
    val finalAccentColor = remember(trackArtworkLocal, theme.btnColor, theme.dynamicColorEnabled, theme.iconSaturation, theme.iconBrightness) {
        if (theme.dynamicColorEnabled) {
            getDynamicIconColor(trackArtworkLocal, theme.btnColor, theme.iconSaturation, theme.iconBrightness)
        } else {
            parseColorHex(theme.btnColor, Color.White)
        }
    }

    val primaryBgColor = remember(theme.bgColor1) { parseColorHex(theme.bgColor1, Color.Black) }
    val secondaryBgColor = remember(theme.bgColor2) { parseColorHex(theme.bgColor2, Color(0xFF121212)) }
    val buttonTint = finalAccentColor
    val textTint = remember(theme.textColor) { parseColorHex(theme.textColor, Color.LightGray) }

    // Background configuration
    val backgroundModifier = when (theme.bgType) {
        "SOLID" -> Modifier.background(primaryBgColor.copy(alpha = theme.bgAlpha))
        "GRADIENT" -> Modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(primaryBgColor, secondaryBgColor)
            ),
            alpha = theme.bgAlpha
        )
        "BLUR_ARTWORK" -> {
            if (trackArtworkLocal != null) {
                Modifier
                    .background(Color.Black.copy(alpha = 0.3f))
                    .drawWithContent { drawContent() }
            } else {
                Modifier.background(primaryBgColor.copy(alpha = theme.bgAlpha))
            }
        }
        else -> Modifier.background(Color.Transparent)
    }

    // Interactive button animation pulse
    val scalePulse by animateFloatAsState(
        targetValue = if (isPlaying && theme.btnAnimType == "SCALE_PULSE") 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(theme.btnAnimSpeed, easing = getEasing(theme.animationCurve)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Continuous cover art rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "artwork_rotation_transition")
    val rotationAngle by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(theme.btnAnimSpeed * 15, easing = getEasing(theme.animationCurve)),
                repeatMode = RepeatMode.Restart
            ),
            label = "art_rot"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val controllerBorder = if (theme.btnAnimType == "ROTATE_GLOW" && isPlaying) {
        Modifier.border(
            width = 2.dp,
            brush = Brush.sweepGradient(
                colors = listOf(buttonTint, Color.Transparent, buttonTint, Color.Transparent)
            ),
            shape = RoundedCornerShape(theme.cornerRadius.dp)
        )
    } else {
        Modifier.border(0.5.dp, textTint.copy(alpha = 0.15f), RoundedCornerShape(theme.cornerRadius.dp))
    }

    Box(
        modifier = Modifier
            .scale(theme.layoutScale)
            .pointerInput(theme.isGamingMode) {
                detectTapGestures(
                    onTap = {
                        if (!theme.isGamingMode) {
                            isExpanded = !isExpanded
                        }
                    },
                    onDoubleTap = {
                        if (theme.isGamingMode) {
                            isExpanded = !isExpanded
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x.toInt(), dragAmount.y.toInt())
                }
            }
            .clip(RoundedCornerShape(theme.cornerRadius.dp))
            .then(backgroundModifier)
            .then(controllerBorder)
            .width(if (isExpanded) theme.expandedWidth.dp else theme.pillWidth.dp)
            .height(if (isExpanded) theme.expandedHeight.dp else theme.pillHeight.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Blur background artwork if requested
        if (theme.bgType == "BLUR_ARTWORK" && trackArtworkLocal != null) {
            Image(
                bitmap = trackArtworkLocal.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                25f, 25f, android.graphics.Shader.TileMode.CLAMP
                            ).asComposeRenderEffect()
                        }
                    },
                contentScale = ContentScale.Crop,
                alpha = theme.bgAlpha * 0.7f
            )
        }

        if (!isExpanded) {
            // COLLAPSED: 1 single pill bar layout
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Rotating Artwork Circle (Corner radius controlled by cornerRadius theme attribute)
                Box(
                    modifier = Modifier
                        .size((theme.pillHeight * 0.72f).dp)
                        .clip(RoundedCornerShape(theme.cornerRadius.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .rotate(rotationAngle)
                ) {
                    if (trackArtworkLocal != null) {
                        Image(
                            bitmap = trackArtworkLocal.asImageBitmap(),
                            contentDescription = "Rotating Track Artwork",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_void_logo_1782491566833),
                            contentDescription = "Default Void Artwork",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Song details: sliced to exactly first 10 characters + ongoing position
                val shortTitle = remember(trackTitle) {
                    if (trackTitle.length > 10) trackTitle.take(10) + "..." else trackTitle
                }
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = shortTitle,
                        color = textTint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = formatTime(trackPosition),
                        color = textTint.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontFamily = fontFamily
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Compact Controls: Prev, Play/Pause, Next beside each other
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .gamingClickable(theme.isGamingMode) {
                                MediaStateHolder.triggerSkipPrevious()
                            }
                    ) {
                        PackIcon(
                            iconType = "PREVIOUS",
                            packName = theme.iconPackName,
                            tint = buttonTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .scale(scalePulse)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(buttonTint.copy(alpha = 0.12f))
                            .gamingClickable(theme.isGamingMode) {
                                MediaStateHolder.triggerPlayPause()
                            }
                    ) {
                        PackIcon(
                            iconType = if (isPlaying) "PAUSE" else "PLAY",
                            packName = theme.iconPackName,
                            tint = buttonTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .gamingClickable(theme.isGamingMode) {
                                MediaStateHolder.triggerSkipNext()
                            }
                    ) {
                        PackIcon(
                            iconType = "NEXT",
                            packName = theme.iconPackName,
                            tint = buttonTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            // EXPANDED: "smol menu" showing more detailed choices, customization shortcut, and normal play indicators
            val context = LocalContext.current
            val openSettings = {
                val intent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                isExpanded = false
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Minimal drag handle, Status, Settings shortcut and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minimal indicator
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(3.dp)
                            .background(textTint.copy(alpha = 0.25f), CircleShape)
                    )

                    if (theme.isGamingMode) {
                        Text(
                            text = "GAME MODE",
                            color = buttonTint,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .border(0.5.dp, buttonTint.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Settings / Theme Customization Shortcut
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .clickable { openSettings() }
                        ) {
                            PackIcon(
                                iconType = "SETTINGS",
                                packName = theme.iconPackName,
                                tint = textTint.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Close service
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .clickable { onClose() }
                        ) {
                            PackIcon(
                                iconType = "CLOSE",
                                packName = theme.iconPackName,
                                tint = textTint.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Middle Section: Metadata details (Artwork, full song title, artist)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (theme.showArtwork) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            if (trackArtworkLocal != null) {
                                Image(
                                    bitmap = trackArtworkLocal.asImageBitmap(),
                                    contentDescription = "Artwork Detail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.img_void_logo_1782491566833),
                                    contentDescription = "Default Void Artwork",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = trackTitle,
                            color = textTint,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = fontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = trackArtist,
                            color = textTint.copy(alpha = 0.65f),
                            fontSize = 11.sp,
                            fontFamily = fontFamily,
                            fontWeight = fontWeight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Progress Bar Timeline (if showProgress active)
                if (theme.showProgress && trackDuration > 0) {
                    val progressFraction = trackPosition.toFloat() / trackDuration.toFloat()
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { progressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .clip(CircleShape),
                            color = buttonTint,
                            trackColor = textTint.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(trackPosition),
                                color = textTint.copy(alpha = 0.5f),
                                fontSize = 8.sp,
                                fontFamily = fontFamily
                            )
                            Text(
                                text = formatTime(trackDuration),
                                color = textTint.copy(alpha = 0.5f),
                                fontSize = 8.sp,
                                fontFamily = fontFamily
                            )
                        }
                    }
                }

                // Controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Track (respects Gaming Mode double-tap)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .gamingClickable(theme.isGamingMode) {
                                MediaStateHolder.triggerSkipPrevious()
                            }
                    ) {
                        PackIcon(
                            iconType = "PREVIOUS",
                            packName = theme.iconPackName,
                            tint = buttonTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    // Play / Pause Track (respects Gaming Mode double-tap)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .scale(scalePulse)
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(buttonTint.copy(alpha = 0.12f))
                            .gamingClickable(theme.isGamingMode) {
                                MediaStateHolder.triggerPlayPause()
                            }
                    ) {
                        PackIcon(
                            iconType = if (isPlaying) "PAUSE" else "PLAY",
                            packName = theme.iconPackName,
                            tint = buttonTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    // Next Track (respects Gaming Mode double-tap)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .gamingClickable(theme.isGamingMode) {
                                MediaStateHolder.triggerSkipNext()
                            }
                    ) {
                        PackIcon(
                            iconType = "NEXT",
                            packName = theme.iconPackName,
                            tint = buttonTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
