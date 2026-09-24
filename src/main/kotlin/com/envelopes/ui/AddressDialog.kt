package com.envelopes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.envelopes.Address

@Composable
fun AddressEditDialog(
    title: String,
    initialAddress: Address? = null,
    onDismiss: () -> Unit,
    onSave: (Address) -> Unit
) {
    var name by remember { mutableStateOf(initialAddress?.name ?: "") }
    var street by remember { mutableStateOf(initialAddress?.street ?: "") }
    var street2 by remember { mutableStateOf(initialAddress?.street2 ?: "") }
    var city by remember { mutableStateOf(initialAddress?.city ?: "") }
    var state by remember { mutableStateOf(initialAddress?.state ?: "") }
    var zip by remember { mutableStateOf(initialAddress?.zip ?: "") }
    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(480.dp)
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; showError = false },
                    label = { Text("Full Name / Organization *") },
                    isError = showError && name.isBlank(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it; showError = false },
                    label = { Text("Street Address *") },
                    isError = showError && street.isBlank(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = street2,
                    onValueChange = { street2 = it },
                    label = { Text("Street Address Line 2 (Apt / Ste / Unit)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        singleLine = true,
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = state,
                        onValueChange = { state = it.uppercase() },
                        label = { Text("State") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = zip,
                        onValueChange = { zip = it },
                        label = { Text("ZIP") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                }

                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please enter both a Name and Street Address.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank() || street.isBlank()) {
                                showError = true
                            } else {
                                val updated = (initialAddress ?: Address()).copy(
                                    name = name.trim(),
                                    street = street.trim(),
                                    street2 = street2.trim(),
                                    city = city.trim(),
                                    state = state.trim(),
                                    zip = zip.trim()
                                )
                                onSave(updated)
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
