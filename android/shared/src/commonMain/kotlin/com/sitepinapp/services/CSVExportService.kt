package com.sitepinapp.services

import com.sitepinapp.data.model.PinCategory
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object CSVExportService {

    fun buildCSV(snapshot: ProjectSnapshot): String {
        val sb = StringBuilder()
        // UTF-8 BOM for Excel compatibility
        sb.append('\uFEFF')
        sb.appendLine("Pin #,Title,Description,Category,Status,Author,Location,Height,Width,Page,Document,Photos,Comments,Created")

        var pinNumber = 1
        for (doc in snapshot.documents) {
            for (pin in doc.pins.sortedBy { it.createdAt }) {
                sb.append(pinNumber).append(',')
                sb.append(escapeCSV(pin.title)).append(',')
                sb.append(escapeCSV(pin.description)).append(',')
                sb.append(escapeCSV(categoryLabel(pin.category))).append(',')
                sb.append(escapeCSV(statusLabel(pin.status))).append(',')
                sb.append(escapeCSV(pin.author)).append(',')
                sb.append(escapeCSV(pin.location)).append(',')
                sb.append(escapeCSV(pin.height)).append(',')
                sb.append(escapeCSV(pin.width)).append(',')
                sb.append(pin.pageIndex + 1).append(',')
                sb.append(escapeCSV(pin.documentName)).append(',')
                sb.append(pin.photoCount).append(',')
                sb.append(pin.commentCount).append(',')
                val dateStr = formatDate(pin.createdAt)
                sb.appendLine(escapeCSV(dateStr))
                pinNumber++
            }
        }
        return sb.toString()
    }

    private fun categoryLabel(raw: String): String =
        PinCategory.fromString(raw).label

    private fun statusLabel(raw: String): String = when (raw) {
        "open" -> "Open"
        "in_progress" -> "In Progress"
        "resolved" -> "Resolved"
        else -> raw
    }

    private fun escapeCSV(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun formatDate(epochMillis: Long): String {
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${local.year}-${local.monthNumber.toString().padStart(2, '0')}-${local.dayOfMonth.toString().padStart(2, '0')} ${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
    }
}
