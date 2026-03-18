package com.sitepinapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sitepinapp.ui.theme.StatusInProgress
import com.sitepinapp.ui.theme.StatusOpen
import com.sitepinapp.ui.theme.StatusResolved

private data class StatusOption(
    val value: String,
    val label: String,
    val color: androidx.compose.ui.graphics.Color
)

private val statusOptions = listOf(
    StatusOption("open", "Open", StatusOpen),
    StatusOption("in_progress", "In Progress", StatusInProgress),
    StatusOption("resolved", "Resolved", StatusResolved)
)

@Composable
fun StatusPicker(
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        statusOptions.forEach { option ->
            val isSelected = option.value == selectedStatus
            FilterChip(
                selected = isSelected,
                onClick = { onStatusSelected(option.value) },
                label = { Text(option.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = option.color.copy(alpha = 0.2f),
                    selectedLabelColor = option.color
                )
            )
        }
    }
}

@Composable
fun StatusFilterPicker(
    selectedStatus: String?,
    onStatusSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedStatus == null,
            onClick = { onStatusSelected(null) },
            label = { Text("All") }
        )
        statusOptions.forEach { option ->
            val isSelected = option.value == selectedStatus
            FilterChip(
                selected = isSelected,
                onClick = {
                    onStatusSelected(if (isSelected) null else option.value)
                },
                label = { Text(option.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = option.color.copy(alpha = 0.2f),
                    selectedLabelColor = option.color
                )
            )
        }
    }
}
