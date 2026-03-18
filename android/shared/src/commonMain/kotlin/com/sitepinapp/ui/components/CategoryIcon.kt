package com.sitepinapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.sitepinapp.data.model.PinCategory

fun PinCategory.toIcon(): ImageVector = when (this) {
    PinCategory.GENERAL -> Icons.Default.Build
    PinCategory.ELECTRICAL -> Icons.Default.Bolt
    PinCategory.PLUMBING -> Icons.Default.Water
    PinCategory.STRUCTURAL -> Icons.Default.Domain
    PinCategory.FINISHING -> Icons.Default.Brush
    PinCategory.HVAC -> Icons.Default.AcUnit
    PinCategory.SAFETY -> Icons.Default.Warning
}

@Composable
fun CategoryIcon(
    category: PinCategory,
    modifier: Modifier = Modifier,
    tint: Color = category.color
) {
    Icon(
        imageVector = category.toIcon(),
        contentDescription = category.label,
        modifier = modifier,
        tint = tint
    )
}
