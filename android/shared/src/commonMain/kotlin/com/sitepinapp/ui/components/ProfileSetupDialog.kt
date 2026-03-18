package com.sitepinapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileSetupDialog(
    currentName: String = "",
    currentTheme: String = "system",
    onDismiss: () -> Unit,
    onConfirm: (name: String, theme: String) -> Unit
) {
    var displayName by remember { mutableStateOf(currentName) }
    var selectedTheme by remember { mutableStateOf(currentTheme) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (currentName.isBlank()) "Set Up Profile" else "Edit Profile",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter your display name. This will be used to identify your pins and comments.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    FilterChip(
                        selected = selectedTheme == "system",
                        onClick = { selectedTheme = "system" },
                        label = { Text("System") },
                        leadingIcon = {
                            Icon(Icons.Default.Settings, contentDescription = null,
                                modifier = Modifier.height(16.dp))
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = selectedTheme == "light",
                        onClick = { selectedTheme = "light" },
                        label = { Text("Light") },
                        leadingIcon = {
                            Icon(Icons.Default.LightMode, contentDescription = null,
                                modifier = Modifier.height(16.dp))
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = selectedTheme == "dark",
                        onClick = { selectedTheme = "dark" },
                        label = { Text("Dark") },
                        leadingIcon = {
                            Icon(Icons.Default.DarkMode, contentDescription = null,
                                modifier = Modifier.height(16.dp))
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(displayName.trim(), selectedTheme) },
                enabled = displayName.trim().isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            if (currentName.isNotBlank()) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
