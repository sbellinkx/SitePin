package com.sitepinapp.data.model

import androidx.compose.ui.graphics.Color

enum class PinCategory(
    val label: String,
    val color: Color
) {
    GENERAL("General", Color(0xFF607D8B)),
    ELECTRICAL("Electrical", Color(0xFFFFC107)),
    PLUMBING("Plumbing", Color(0xFF2196F3)),
    STRUCTURAL("Structural", Color(0xFF795548)),
    FINISHING("Finishing", Color(0xFF9C27B0)),
    HVAC("HVAC", Color(0xFF00BCD4)),
    SAFETY("Safety", Color(0xFFFF5722));

    companion object {
        fun fromString(value: String): PinCategory =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: GENERAL
    }
}
