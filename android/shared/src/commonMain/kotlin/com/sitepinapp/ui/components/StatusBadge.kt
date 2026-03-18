package com.sitepinapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitepinapp.ui.theme.StatusInProgress
import com.sitepinapp.ui.theme.StatusOpen
import com.sitepinapp.ui.theme.StatusResolved

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, label) = when (status.lowercase()) {
        "open" -> StatusOpen to "Open"
        "in_progress" -> StatusInProgress to "In Progress"
        "resolved" -> StatusResolved to "Resolved"
        else -> StatusOpen to status.replaceFirstChar { it.uppercase() }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
