package com.sitepinapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sitepinapp.data.model.Pin
import com.sitepinapp.data.model.PinCategory
import com.sitepinapp.ui.components.CategoryIcon
import com.sitepinapp.ui.components.StatusBadge
import com.sitepinapp.ui.theme.StatusInProgress
import com.sitepinapp.ui.theme.StatusOpen
import com.sitepinapp.ui.theme.StatusResolved
import com.sitepinapp.viewmodel.DashboardViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@Composable
fun PinDashboardContent(
    projectId: Long,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel { DashboardViewModel(projectId) }
) {
    val todoPins by viewModel.todoPins.collectAsState()
    val donePins by viewModel.donePins.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val openCount by viewModel.openCount.collectAsState()
    val inProgressCount by viewModel.inProgressCount.collectAsState()
    val resolvedCount by viewModel.resolvedCount.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Stats row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(label = "Total", count = totalCount, color = MaterialTheme.colorScheme.primary)
            StatCard(label = "Open", count = openCount, color = StatusOpen)
            StatCard(label = "In Progress", count = inProgressCount, color = StatusInProgress)
            StatCard(label = "Resolved", count = resolvedCount, color = StatusResolved)
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()

        // Tabs
        TabRow(
            selectedTabIndex = pagerState.currentPage
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text("To Do (${todoPins.size})") }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text("Done (${donePins.size})") }
            )
        }

        // Pager content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f, fill = false)
        ) { page ->
            val pins = if (page == 0) todoPins else donePins
            if (pins.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (page == 0) "All caught up!" else "No resolved pins yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    items(pins, key = { it.id }) { pin ->
                        DashboardPinRow(
                            pin = pin,
                            onStatusChange = { newStatus ->
                                viewModel.updatePinStatus(pin.id, newStatus)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DashboardPinRow(
    pin: Pin,
    onStatusChange: (String) -> Unit
) {
    val category = PinCategory.fromString(pin.category)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(
                category = category,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pin.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (pin.pinDescription.isNotBlank()) {
                    Text(
                        text = pin.pinDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusBadge(status = pin.status)
            Spacer(modifier = Modifier.width(4.dp))

            // Quick status actions
            when (pin.status) {
                "open" -> {
                    IconButton(
                        onClick = { onStatusChange("in_progress") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Start",
                            tint = StatusInProgress,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                "in_progress" -> {
                    IconButton(
                        onClick = { onStatusChange("resolved") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Resolve",
                            tint = StatusResolved,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                "resolved" -> {
                    IconButton(
                        onClick = { onStatusChange("open") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Reopen",
                            tint = StatusOpen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
