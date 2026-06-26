package com.example

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.MediaStateHolder
import com.example.data.ThemeEntity
import com.example.data.ThemeRepository
import com.example.data.formatTime
import com.example.data.parseColorHex
import com.example.service.FloatingControllerService
import com.example.ui.ThemeViewModel
import com.example.ui.ThemeViewModelFactory
import com.example.ui.VoidThemeEditor
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val repository = ThemeRepository(database.themeDao())

        setContent {
            MyApplicationTheme {
                val themeViewModel: ThemeViewModel = viewModel(
                    factory = ThemeViewModelFactory(repository)
                )
                MainDashboardScreen(viewModel = themeViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(viewModel: ThemeViewModel) {
    val context = LocalContext.current
    val allThemes by viewModel.allThemes.collectAsStateWithLifecycle()
    val activeTheme by viewModel.activeTheme.collectAsStateWithLifecycle()
    val editingTheme by viewModel.editingTheme.collectAsStateWithLifecycle()

    // Permissions check states
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasNotificationPermission by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var isOverlayRunning by remember { mutableStateOf(false) }

    // Trigger permission checks on app resume
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    
    LaunchedEffect(lifecycleState) {
        if (lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
            hasOverlayPermission = Settings.canDrawOverlays(context)
            hasNotificationPermission = isNotificationListenerEnabled(context)
        }
    }

    // Theme export reference
    var themeToExport by remember { mutableStateOf<ThemeEntity?>(null) }

    // CreateDocument launcher for exporting themes
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val theme = themeToExport
        if (uri != null && theme != null) {
            val success = viewModel.exportTheme(context, theme, uri)
            if (success) {
                Toast.makeText(context, "Theme '${theme.name}' exported!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
        themeToExport = null
    }

    // OpenDocument launcher for importing themes
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importTheme(
                context = context,
                uri = uri,
                onSuccess = {
                    Toast.makeText(context, "Theme imported successfully!", Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    Toast.makeText(context, "Import failed: $err", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // Main Layout
    if (editingTheme != null) {
        VoidThemeEditor(
            theme = editingTheme!!,
            onSave = { viewModel.saveEditingTheme() },
            onCancel = { viewModel.cancelEditing() },
            onThemeUpdate = { updater -> viewModel.updateEditingTheme(updater) }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFF090909),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.img_void_logo_1782491566833),
                                contentDescription = "Void Logo",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "VOID",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                letterSpacing = 2.sp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF000000).copy(alpha = 0.8f)
                    )
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // 1. PERMISSION & SERVICE STATUS
                item {
                    PermissionStatusCard(
                        hasOverlay = hasOverlayPermission,
                        hasNotification = hasNotificationPermission,
                        isOverlayRunning = isOverlayRunning,
                        onToggleOverlay = { enabled ->
                            if (enabled) {
                                if (!hasOverlayPermission) {
                                    Toast.makeText(context, "Please grant Overlay permission first", Toast.LENGTH_LONG).show()
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                } else if (!hasNotificationPermission) {
                                    Toast.makeText(context, "Please grant Notification Listener access first", Toast.LENGTH_LONG).show()
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    context.startActivity(intent)
                                } else {
                                    context.startService(Intent(context, FloatingControllerService::class.java))
                                    isOverlayRunning = true
                                }
                            } else {
                                context.stopService(Intent(context, FloatingControllerService::class.java))
                                isOverlayRunning = false
                            }
                        },
                        onRequestOverlay = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        },
                        onRequestNotification = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        }
                    )
                }

                // 2. LIVE MUSIC STATE PREVIEW CARD
                item {
                    MusicPreviewCard(activeTheme ?: ThemeEntity.createDefault())
                }

                // 3. THEME SELECTION DESK HEADER
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Theme Selection",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Import button
                            TextButton(
                                onClick = { importLauncher.launch(arrayOf("*/*")) },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00FFCC))
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import", fontSize = 12.sp)
                            }
                            // Create button
                            Button(
                                onClick = { viewModel.startNewTheme() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E1E1E),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Create", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // 4. GRID OF SAVED THEMES
                item {
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        if (allThemes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF00FFCC))
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(allThemes) { theme ->
                                    ThemeItemCard(
                                        theme = theme,
                                        isActive = theme.isActive,
                                        onSelect = { viewModel.selectTheme(theme.id) },
                                        onEdit = { viewModel.startEditing(theme) },
                                        onDelete = { viewModel.deleteTheme(theme) },
                                        onExport = {
                                            themeToExport = theme
                                            exportLauncher.launch("${theme.name.replace(" ", "_").lowercase()}.voidtheme")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun PermissionStatusCard(
    hasOverlay: Boolean,
    hasNotification: Boolean,
    isOverlayRunning: Boolean,
    onToggleOverlay: (Boolean) -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestNotification: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "SYSTEM ACCESS DASHBOARD",
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Draw Over Other Apps Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Draw Over Other Apps", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Required for floating overlay widget", color = Color.Gray, fontSize = 11.sp)
                }
                if (hasOverlay) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Granted", color = Color(0xFF00FFCC)) },
                        leadingIcon = { Icon(Icons.Default.Done, null, tint = Color(0xFF00FFCC), modifier = Modifier.size(12.dp)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF00FFCC).copy(alpha = 0.08f))
                    )
                } else {
                    Button(
                        onClick = onRequestOverlay,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5555)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.05f))

            // Notification Access Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Music Session Access", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Required to control Spotify/VLC", color = Color.Gray, fontSize = 11.sp)
                }
                if (hasNotification) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Granted", color = Color(0xFF00FFCC)) },
                        leadingIcon = { Icon(Icons.Default.Done, null, tint = Color(0xFF00FFCC), modifier = Modifier.size(12.dp)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF00FFCC).copy(alpha = 0.08f))
                    )
                } else {
                    Button(
                        onClick = onRequestNotification,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5555)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.05f))

            // FLOATING CONTROLLER TOGGLE SYSTEM
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Floating Void Widget", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (isOverlayRunning) "Sleek overlay currently active" else "Launch overlay over other apps",
                        color = if (isOverlayRunning) Color(0xFF00FFCC) else Color.Gray,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = isOverlayRunning,
                    onCheckedChange = onToggleOverlay,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF00FFCC),
                        checkedTrackColor = Color(0xFF00FFCC).copy(alpha = 0.4f),
                        uncheckedThumbColor = Color.DarkGray,
                        uncheckedTrackColor = Color(0xFF1E1E1E)
                    )
                )
            }
        }
    }
}

@Composable
fun MusicPreviewCard(theme: ThemeEntity) {
    val trackTitle by MediaStateHolder.trackTitle.collectAsState()
    val trackArtist by MediaStateHolder.trackArtist.collectAsState()
    val trackArtwork by MediaStateHolder.trackArtwork.collectAsState()
    val isPlaying by MediaStateHolder.isPlaying.collectAsState()
    val trackDuration by MediaStateHolder.trackDuration.collectAsState()
    val trackPosition by MediaStateHolder.trackPosition.collectAsState()

    val accentColor = remember(theme.btnColor) { parseColorHex(theme.btnColor, Color(0xFF00FFCC)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ACTIVE SESSION PREVIEW",
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                ) {
                    if (trackArtwork != null) {
                        Image(
                            bitmap = trackArtwork!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_void_logo_1782491566833),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Track details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trackTitle,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = trackArtist,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Slider
            if (trackDuration > 0) {
                val progressFraction = trackPosition.toFloat() / trackDuration.toFloat()
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { progressFraction.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = accentColor,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(trackPosition), color = Color.Gray, fontSize = 10.sp)
                        Text(formatTime(trackDuration), color = Color.Gray, fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // App controllers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { MediaStateHolder.triggerSkipPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f))
                        .clickable { MediaStateHolder.triggerPlayPause() }
                ) {
                    Icon(
                        painter = if (isPlaying) {
                            painterResource(id = android.R.drawable.ic_media_pause)
                        } else {
                            painterResource(id = android.R.drawable.ic_media_play)
                        },
                        contentDescription = "PlayPause",
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = { MediaStateHolder.triggerSkipNext() }) {
                    Icon(Icons.Default.SkipNext, "Next", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ThemeItemCard(
    theme: ThemeEntity,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    val primaryColor = remember(theme.bgColor1) { parseColorHex(theme.bgColor1, Color.Black) }
    val secondaryColor = remember(theme.bgColor2) { parseColorHex(theme.bgColor2, Color.DarkGray) }
    val accentColor = remember(theme.btnColor) { parseColorHex(theme.btnColor, Color.White) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelect() }
            .border(
                width = if (isActive) 1.5.dp else 0.5.dp,
                color = if (isActive) accentColor else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Mini Preview Swatch
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(primaryColor, secondaryColor)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Mini Playpause button icon inside swatch
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Theme Name & Details
            Text(
                text = theme.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Style: ${theme.fontStyle.lowercase()}",
                color = Color.Gray,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Export Icon
                IconButton(
                    onClick = onExport,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export Theme",
                        tint = Color.LightGray.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Edit Button
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Theme",
                            tint = Color.LightGray.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Delete Button (only if not a default/first theme)
                    if (theme.id > 3) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Theme",
                                tint = Color(0xFFFF5555).copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Utility to verify if the notification listener service is enabled
fun isNotificationListenerEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    if (flat != null) {
        val names = flat.split(":")
        for (name in names) {
            val cn = ComponentName.unflattenFromString(name)
            if (cn != null && cn.packageName == pkgName) {
                return true
            }
        }
    }
    return false
}
