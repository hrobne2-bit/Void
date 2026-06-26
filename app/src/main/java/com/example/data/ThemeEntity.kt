package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONObject

@Entity(tableName = "themes")
data class ThemeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val bgColor1: String = "#000000",
    val bgColor2: String = "#121212",
    val bgType: String = "GRADIENT", // SOLID, GRADIENT, BLUR_ARTWORK, TRANSPARENT
    val bgAlpha: Float = 0.85f,
    val btnColor: String = "#FFFFFF",
    val btnAnimType: String = "SCALE_PULSE", // NONE, SCALE_PULSE, ROTATE_GLOW, RIPPLE
    val btnAnimSpeed: Int = 300,
    val textColor: String = "#E0E0E0",
    val cornerRadius: Int = 24,
    val layoutScale: Float = 1.0f,
    val showArtwork: Boolean = true,
    val showProgress: Boolean = true,
    val fontStyle: String = "MINIMAL", // MINIMAL, MONOSPACE, SYSTEM
    val isActive: Boolean = false,
    
    // Versatile layout controls
    val pillWidth: Int = 220,
    val pillHeight: Int = 54,
    val expandedWidth: Int = 280,
    val expandedHeight: Int = 220,
    
    // Gaming mode setting
    val isGamingMode: Boolean = false,
    
    // Easing curve controls
    val animationCurve: String = "SPRING", // LINEAR, EASE_IN, EASE_OUT, BOUNCE, SPRING
    
    // Icon configuration
    val iconPackName: String = "DEFAULT", // DEFAULT, SHARP, OUTLINED, MINIMAL
    val dynamicColorEnabled: Boolean = false,
    val iconSaturation: Float = 1.0f,
    val iconBrightness: Float = 1.0f
) {
    fun toJsonString(): String {
        val json = JSONObject()
        json.put("name", name)
        json.put("bgColor1", bgColor1)
        json.put("bgColor2", bgColor2)
        json.put("bgType", bgType)
        json.put("bgAlpha", bgAlpha.toDouble())
        json.put("btnColor", btnColor)
        json.put("btnAnimType", btnAnimType)
        json.put("btnAnimSpeed", btnAnimSpeed)
        json.put("textColor", textColor)
        json.put("cornerRadius", cornerRadius)
        json.put("layoutScale", layoutScale.toDouble())
        json.put("showArtwork", showArtwork)
        json.put("showProgress", showProgress)
        json.put("fontStyle", fontStyle)
        
        // New fields
        json.put("pillWidth", pillWidth)
        json.put("pillHeight", pillHeight)
        json.put("expandedWidth", expandedWidth)
        json.put("expandedHeight", expandedHeight)
        json.put("isGamingMode", isGamingMode)
        json.put("animationCurve", animationCurve)
        json.put("iconPackName", iconPackName)
        json.put("dynamicColorEnabled", dynamicColorEnabled)
        json.put("iconSaturation", iconSaturation.toDouble())
        json.put("iconBrightness", iconBrightness.toDouble())
        return json.toString()
    }

    companion object {
        fun fromJsonString(jsonStr: String): ThemeEntity? {
            return try {
                val json = JSONObject(jsonStr)
                ThemeEntity(
                    name = json.optString("name", "Imported Theme"),
                    bgColor1 = json.optString("bgColor1", "#000000"),
                    bgColor2 = json.optString("bgColor2", "#121212"),
                    bgType = json.optString("bgType", "GRADIENT"),
                    bgAlpha = json.optDouble("bgAlpha", 0.85).toFloat(),
                    btnColor = json.optString("btnColor", "#FFFFFF"),
                    btnAnimType = json.optString("btnAnimType", "SCALE_PULSE"),
                    btnAnimSpeed = json.optInt("btnAnimSpeed", 300),
                    textColor = json.optString("textColor", "#E0E0E0"),
                    cornerRadius = json.optInt("cornerRadius", 24),
                    layoutScale = json.optDouble("layoutScale", 1.0).toFloat(),
                    showArtwork = json.optBoolean("showArtwork", true),
                    showProgress = json.optBoolean("showProgress", true),
                    fontStyle = json.optString("fontStyle", "MINIMAL"),
                    
                    // New fields parsing
                    pillWidth = json.optInt("pillWidth", 220),
                    pillHeight = json.optInt("pillHeight", 54),
                    expandedWidth = json.optInt("expandedWidth", 280),
                    expandedHeight = json.optInt("expandedHeight", 220),
                    isGamingMode = json.optBoolean("isGamingMode", false),
                    animationCurve = json.optString("animationCurve", "SPRING"),
                    iconPackName = json.optString("iconPackName", "DEFAULT"),
                    dynamicColorEnabled = json.optBoolean("dynamicColorEnabled", false),
                    iconSaturation = json.optDouble("iconSaturation", 1.0).toFloat(),
                    iconBrightness = json.optDouble("iconBrightness", 1.0).toFloat()
                )
            } catch (e: Exception) {
                null
            }
        }

        fun createDefault(): ThemeEntity {
            return ThemeEntity(
                name = "Void Abyss",
                bgColor1 = "#000000",
                bgColor2 = "#121212",
                bgType = "GRADIENT",
                bgAlpha = 0.9f,
                btnColor = "#00FFCC", // Cyan accent
                btnAnimType = "SCALE_PULSE",
                btnAnimSpeed = 250,
                textColor = "#FFFFFF",
                cornerRadius = 24,
                layoutScale = 1.0f,
                showArtwork = true,
                showProgress = true,
                fontStyle = "MINIMAL",
                isActive = true,
                pillWidth = 240,
                pillHeight = 56,
                expandedWidth = 280,
                expandedHeight = 220,
                isGamingMode = false,
                animationCurve = "SPRING",
                iconPackName = "DEFAULT",
                dynamicColorEnabled = false,
                iconSaturation = 1.0f,
                iconBrightness = 1.0f
            )
        }

        fun createNord(): ThemeEntity {
            return ThemeEntity(
                name = "Nordic Forest",
                bgColor1 = "#2E3440",
                bgColor2 = "#3B4252",
                bgType = "GRADIENT",
                bgAlpha = 0.95f,
                btnColor = "#88C0D0", // Frost blue
                btnAnimType = "SCALE_PULSE",
                btnAnimSpeed = 300,
                textColor = "#ECEFF4",
                cornerRadius = 16,
                layoutScale = 1.0f,
                showArtwork = true,
                showProgress = true,
                fontStyle = "MONOSPACE",
                isActive = false,
                pillWidth = 240,
                pillHeight = 56,
                expandedWidth = 280,
                expandedHeight = 220,
                isGamingMode = false,
                animationCurve = "EASE_OUT",
                iconPackName = "OUTLINED",
                dynamicColorEnabled = false,
                iconSaturation = 1.0f,
                iconBrightness = 1.0f
            )
        }

        fun createNebula(): ThemeEntity {
            return ThemeEntity(
                name = "Cyber Nebula",
                bgColor1 = "#1A0933",
                bgColor2 = "#0F021A",
                bgType = "GRADIENT",
                bgAlpha = 0.8f,
                btnColor = "#FF007F", // Neon Pink
                btnAnimType = "ROTATE_GLOW",
                btnAnimSpeed = 350,
                textColor = "#EAE6FF",
                cornerRadius = 32,
                layoutScale = 1.0f,
                showArtwork = true,
                showProgress = true,
                fontStyle = "SYSTEM",
                isActive = false,
                pillWidth = 250,
                pillHeight = 60,
                expandedWidth = 300,
                expandedHeight = 240,
                isGamingMode = false,
                animationCurve = "BOUNCE",
                iconPackName = "MINIMAL",
                dynamicColorEnabled = true,
                iconSaturation = 1.2f,
                iconBrightness = 0.9f
            )
        }
    }
}
