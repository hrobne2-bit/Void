package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.parseColorHex
import com.example.service.FloatingControllerContent
import com.example.data.ThemeEntity

@Composable
fun VelocityGraph(curve: String, accentColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Text(
                text = "VELOCITY CURVE GRAPH: $curve",
                color = Color.Gray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // Draw grid lines
                val gridAlpha = 0.05f
                for (i in 1..4) {
                    val y = size.height * (i / 5f)
                    drawLine(Color.White, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f, alpha = gridAlpha)
                    val x = size.width * (i / 5f)
                    drawLine(Color.White, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f, alpha = gridAlpha)
                }

                // Curve points calculations
                val pointsCount = 100
                val path = Path()

                val getEaseVal = { fraction: Float ->
                    when (curve) {
                        "LINEAR" -> fraction
                        "EASE_IN" -> fraction * fraction * fraction
                        "EASE_OUT" -> 1f - (1f - fraction) * (1f - fraction) * (1f - fraction)
                        "BOUNCE" -> {
                            val f = fraction
                            if (f < 0.3636f) {
                                7.5625f * f * f
                            } else if (f < 0.7272f) {
                                val f2 = f - 0.5454f
                                7.5625f * f2 * f2 + 0.75f
                            } else if (f < 0.9090f) {
                                val f2 = f - 0.8181f
                                7.5625f * f2 * f2 + 0.9375f
                            } else {
                                val f2 = f - 0.9545f
                                7.5625f * f2 * f2 + 0.984375f
                            }
                        }
                        "SPRING" -> {
                            if (fraction >= 1f) 1f else (1f - Math.exp(-4.5 * fraction) * Math.cos(9.0 * fraction)).toFloat()
                        }
                        else -> fraction
                    }
                }

                for (i in 0..pointsCount) {
                    val t = i.toFloat() / pointsCount
                    val easeVal = getEaseVal(t)
                    val x = t * size.width
                    // Invert y axis for Canvas coords
                    val y = size.height - (easeVal * size.height * 0.7f + size.height * 0.15f)

                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = accentColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoidThemeEditor(
    theme: ThemeEntity,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onThemeUpdate: ((ThemeEntity) -> ThemeEntity) -> Unit
) {
    val scrollState = rememberScrollState()

    val neonPresets = listOf(
        "#00FFCC" to "Cyan",
        "#FF007F" to "Pink",
        "#88C0D0" to "Frost",
        "#FFFFFF" to "White",
        "#FF3333" to "Red",
        "#FFCC00" to "Yellow",
        "#9933FF" to "Purple"
    )

    val currentAccentColor = remember(theme.btnColor) { parseColorHex(theme.btnColor, Color(0xFF00FFCC)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090909))
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (theme.id == 0) "Create Theme" else "Edit Theme",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Row {
                TextButton(onClick = onCancel, colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = currentAccentColor,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Preview Block
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LIVE PREVIEW",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                FloatingControllerContent(
                    theme = theme,
                    onClose = {},
                    onDrag = { _, _ -> }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // DESIGN PARAMETERS: Identity
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Identity", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = theme.name,
                    onValueChange = { name -> onThemeUpdate { it.copy(name = name) } },
                    label = { Text("Theme Name", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = currentAccentColor,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Background Style", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                val bgTypes = listOf("SOLID", "GRADIENT", "BLUR_ARTWORK")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    bgTypes.forEach { type ->
                        val selected = theme.bgType == type
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) currentAccentColor.copy(alpha = 0.2f) else Color(0xFF1A1A1A))
                                .border(1.dp, if (selected) currentAccentColor else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { onThemeUpdate { it.copy(bgType = type) } }
                        ) {
                            Text(
                                text = type.replace("_", " "),
                                color = if (selected) Color.White else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = theme.bgColor1,
                    onValueChange = { hex -> onThemeUpdate { it.copy(bgColor1 = hex) } },
                    label = { Text("Primary Background Color (Hex)", color = Color.Gray) },
                    placeholder = { Text("#000000", color = Color.DarkGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = currentAccentColor,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (theme.bgType == "GRADIENT") {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = theme.bgColor2,
                        onValueChange = { hex -> onThemeUpdate { it.copy(bgColor2 = hex) } },
                        label = { Text("Secondary Gradient Color (Hex)", color = Color.Gray) },
                        placeholder = { Text("#121212", color = Color.DarkGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = currentAccentColor,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Background Opacity: ${(theme.bgAlpha * 100).toInt()}%",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
                Slider(
                    value = theme.bgAlpha,
                    onValueChange = { alpha -> onThemeUpdate { it.copy(bgAlpha = alpha) } },
                    valueRange = 0.2f..1.0f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = currentAccentColor,
                        thumbColor = currentAccentColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DESIGN PARAMETERS: Accent & Button Configurations
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Accent & Buttons", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    neonPresets.forEach { (hex, name) ->
                        val isSelected = theme.btnColor.lowercase() == hex.lowercase()
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(parseColorHex(hex, Color.White))
                                .border(2.dp, if (isSelected) Color.White else Color.Transparent, CircleShape)
                                .clickable { onThemeUpdate { it.copy(btnColor = hex) } }
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (hex == "#FFFFFF") Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = theme.btnColor,
                    onValueChange = { hex -> onThemeUpdate { it.copy(btnColor = hex) } },
                    label = { Text("Custom Accent Color (Hex)", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = currentAccentColor,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Button Interaction Effects", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                val animTypes = listOf("NONE", "SCALE_PULSE", "ROTATE_GLOW")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    animTypes.forEach { anim ->
                        val selected = theme.btnAnimType == anim
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) currentAccentColor.copy(alpha = 0.2f) else Color(0xFF1A1A1A))
                                .border(1.dp, if (selected) currentAccentColor else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { onThemeUpdate { it.copy(btnAnimType = anim) } }
                        ) {
                            Text(
                                text = anim.replace("_", " "),
                                color = if (selected) Color.White else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (theme.btnAnimType != "NONE") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Interaction Cycle Speed: ${theme.btnAnimSpeed} ms",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    Slider(
                        value = theme.btnAnimSpeed.toFloat(),
                        onValueChange = { speed -> onThemeUpdate { it.copy(btnAnimSpeed = speed.toInt()) } },
                        valueRange = 100f..1000f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = currentAccentColor,
                            thumbColor = currentAccentColor
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Velocity Easing Curve", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                val curves = listOf("LINEAR", "EASE_IN", "EASE_OUT", "BOUNCE", "SPRING")
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        curves.take(3).forEach { curve ->
                            val isSelected = theme.animationCurve == curve
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) currentAccentColor.copy(alpha = 0.2f) else Color(0xFF1A1A1A))
                                    .border(1.dp, if (isSelected) currentAccentColor else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { onThemeUpdate { it.copy(animationCurve = curve) } }
                            ) {
                                Text(curve, color = if (isSelected) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        curves.drop(3).forEach { curve ->
                            val isSelected = theme.animationCurve == curve
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) currentAccentColor.copy(alpha = 0.2f) else Color(0xFF1A1A1A))
                                    .border(1.dp, if (isSelected) currentAccentColor else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { onThemeUpdate { it.copy(animationCurve = curve) } }
                            ) {
                                Text(curve, color = if (isSelected) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                // Draw real time vector velocity graph of curve
                VelocityGraph(curve = theme.animationCurve, accentColor = currentAccentColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DESIGN PARAMETERS: Icon Style & Dynamic Color Saturations
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Icon Pack Styles", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                val packs = listOf("DEFAULT", "SHARP", "OUTLINED", "MINIMAL")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    packs.forEach { pack ->
                        val selected = theme.iconPackName == pack
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) currentAccentColor.copy(alpha = 0.2f) else Color(0xFF1A1A1A))
                                .border(1.dp, if (selected) currentAccentColor else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { onThemeUpdate { it.copy(iconPackName = pack) } }
                        ) {
                            Text(
                                text = pack,
                                color = if (selected) Color.White else Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dynamic Color Extraction", color = Color.White, fontSize = 13.sp)
                        Text("Extract color from album cover dynamically", color = Color.Gray, fontSize = 10.sp)
                    }
                    Switch(
                        checked = theme.dynamicColorEnabled,
                        onCheckedChange = { toggle -> onThemeUpdate { it.copy(dynamicColorEnabled = toggle) } },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = currentAccentColor,
                            checkedTrackColor = currentAccentColor.copy(alpha = 0.4f)
                        )
                    )
                }

                if (theme.dynamicColorEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Dynamic Color Saturation: ${String.format("%.1f", theme.iconSaturation)}x",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = theme.iconSaturation,
                        onValueChange = { s -> onThemeUpdate { it.copy(iconSaturation = s) } },
                        valueRange = 0.0f..2.0f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = currentAccentColor,
                            thumbColor = currentAccentColor
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Dynamic Color Brightness: ${String.format("%.1f", theme.iconBrightness)}x",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = theme.iconBrightness,
                        onValueChange = { b -> onThemeUpdate { it.copy(iconBrightness = b) } },
                        valueRange = 0.0f..2.0f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = currentAccentColor,
                            thumbColor = currentAccentColor
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DESIGN PARAMETERS: Layout Sizing Metrics
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Pill & Menu Dimensions", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "One Line Pill Width: ${theme.pillWidth} dp",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
                Slider(
                    value = theme.pillWidth.toFloat(),
                    onValueChange = { w -> onThemeUpdate { it.copy(pillWidth = w.toInt()) } },
                    valueRange = 180f..280f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = currentAccentColor,
                        thumbColor = currentAccentColor
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "One Line Pill Height: ${theme.pillHeight} dp",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
                Slider(
                    value = theme.pillHeight.toFloat(),
                    onValueChange = { h -> onThemeUpdate { it.copy(pillHeight = h.toInt()) } },
                    valueRange = 44f..68f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = currentAccentColor,
                        thumbColor = currentAccentColor
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Expanded Menu Width: ${theme.expandedWidth} dp",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
                Slider(
                    value = theme.expandedWidth.toFloat(),
                    onValueChange = { ew -> onThemeUpdate { it.copy(expandedWidth = ew.toInt()) } },
                    valueRange = 240f..340f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = currentAccentColor,
                        thumbColor = currentAccentColor
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Expanded Menu Height: ${theme.expandedHeight} dp",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
                Slider(
                    value = theme.expandedHeight.toFloat(),
                    onValueChange = { eh -> onThemeUpdate { it.copy(expandedHeight = eh.toInt()) } },
                    valueRange = 160f..260f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = currentAccentColor,
                        thumbColor = currentAccentColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DESIGN PARAMETERS: Layout Scale & Typography
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Scale & Typography", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                val fonts = listOf("MINIMAL", "MONOSPACE", "SYSTEM")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fonts.forEach { font ->
                        val selected = theme.fontStyle == font
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) currentAccentColor.copy(alpha = 0.2f) else Color(0xFF1A1A1A))
                                .border(1.dp, if (selected) currentAccentColor else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { onThemeUpdate { it.copy(fontStyle = font) } }
                        ) {
                            Text(
                                text = font,
                                color = if (selected) Color.White else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Controller Global Scale: ${String.format("%.2f", theme.layoutScale)}x",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
                Slider(
                    value = theme.layoutScale,
                    onValueChange = { scale -> onThemeUpdate { it.copy(layoutScale = scale) } },
                    valueRange = 0.75f..1.3f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = currentAccentColor,
                        thumbColor = currentAccentColor
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Corner Roundness: ${theme.cornerRadius} dp",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
                Slider(
                    value = theme.cornerRadius.toFloat(),
                    onValueChange = { radius -> onThemeUpdate { it.copy(cornerRadius = radius.toInt()) } },
                    valueRange = 4f..40f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = currentAccentColor,
                        thumbColor = currentAccentColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DESIGN PARAMETERS: Gaming Mode Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gaming Mode (Safe Touch)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Reduces misclicks during gameplay", color = Color.Gray, fontSize = 10.sp)
                    }
                    Switch(
                        checked = theme.isGamingMode,
                        onCheckedChange = { toggle -> onThemeUpdate { it.copy(isGamingMode = toggle) } },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = currentAccentColor,
                            checkedTrackColor = currentAccentColor.copy(alpha = 0.4f)
                        )
                    )
                }

                if (theme.isGamingMode) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1212)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFF5555),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Under Gaming Mode: The pill remains unresponsive to single taps. You MUST double-tap to expand the menu, and double-tap buttons to trigger media commands.",
                                color = Color(0xFFFFCCCC),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DESIGN PARAMETERS: Visibility Toggles
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Visibility Options", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Album Artwork Picture", color = Color.White, fontSize = 13.sp)
                    Switch(
                        checked = theme.showArtwork,
                        onCheckedChange = { toggle -> onThemeUpdate { it.copy(showArtwork = toggle) } },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = currentAccentColor,
                            checkedTrackColor = currentAccentColor.copy(alpha = 0.4f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Progress & Timing Bars", color = Color.White, fontSize = 13.sp)
                    Switch(
                        checked = theme.showProgress,
                        onCheckedChange = { toggle -> onThemeUpdate { it.copy(showProgress = toggle) } },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = currentAccentColor,
                            checkedTrackColor = currentAccentColor.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}
