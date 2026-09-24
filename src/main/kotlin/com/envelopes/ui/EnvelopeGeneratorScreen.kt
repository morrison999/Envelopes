package com.envelopes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.envelopes.Address
import com.envelopes.EnvelopeConstants
import com.envelopes.EnvelopePrinter
import com.envelopes.PdfGenerator
import java.awt.Desktop
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

enum class PendingAction {
    GENERATE_PDF,
    PRINT_DIRECT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvelopeGeneratorScreen(
    returnAddresses: List<Address>,
    selectedReturnAddressId: String?,
    addressBook: List<Address>,
    currentRecipient: Address,
    onRecipientChange: (Address) -> Unit,
    onSelectReturnAddress: (String) -> Unit,
    onAddReturnAddress: () -> Unit,
    onSaveToAddressBook: (Address) -> Unit
) {
    val envelopeList = remember { EnvelopeConstants.ENVELOPES.values.toList() }
    var selectedEnvelopeId by remember { mutableStateOf(envelopeList.first().id) }
    val selectedEnvelope = envelopeList.firstOrNull { it.id == selectedEnvelopeId } ?: envelopeList.first()

    val returnAddress = returnAddresses.firstOrNull { it.id == selectedReturnAddressId }
        ?: returnAddresses.firstOrNull()
        ?: EnvelopeConstants.DEFAULT_RETURN_ADDRESS

    var rotateChecked by remember { mutableStateOf(true) }
    var saveToAddressBookChecked by remember { mutableStateOf(true) }
    var promptSaveDialogAction by remember { mutableStateOf<Pair<Address, PendingAction>?>(null) }
    var generatedFileSuccess by remember { mutableStateOf<File?>(null) }
    var printSuccessNotice by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Check if current recipient already exists in address book
    val isRecipientInBook = remember(addressBook, currentRecipient) {
        addressBook.any { it.matches(currentRecipient) }
    }

    val leftScrollState = rememberScrollState()
    val rightScrollState = rememberScrollState()

    fun executePrint(recipientToUse: Address?, shouldSaveToBook: Boolean) {
        try {
            if (shouldSaveToBook && recipientToUse != null && recipientToUse.isNotEmpty() && !isRecipientInBook) {
                onSaveToAddressBook(recipientToUse)
            }
            val printed = EnvelopePrinter.printEnvelope(
                envelopeConfig = selectedEnvelope,
                recipient = recipientToUse,
                returnAddr = returnAddress,
                rotate90 = rotateChecked
            )
            if (printed) {
                printSuccessNotice = true
            }
        } catch (e: Exception) {
            errorMessage = "Printing error: ${e.message}"
        }
    }

    fun executePdfGeneration(recipientToUse: Address?, shouldSaveToBook: Boolean) {
        try {
            if (shouldSaveToBook && recipientToUse != null && recipientToUse.isNotEmpty() && !isRecipientInBook) {
                onSaveToAddressBook(recipientToUse)
            }

            val chooser = JFileChooser().apply {
                dialogTitle = "Save Envelope PDF"
                fileFilter = FileNameExtensionFilter("PDF Files (*.pdf)", "pdf")
                selectedFile = File(selectedEnvelope.filename)
            }

            val userSelection = chooser.showSaveDialog(null)
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                var targetFile = chooser.selectedFile
                if (!targetFile.name.lowercase().endsWith(".pdf")) {
                    targetFile = File(targetFile.parentFile, targetFile.name + ".pdf")
                }

                val result = PdfGenerator.generateEnvelopePdf(
                    envelopeConfig = selectedEnvelope,
                    recipient = recipientToUse,
                    returnAddr = returnAddress,
                    outputFile = targetFile,
                    rotate90 = rotateChecked
                )
                generatedFileSuccess = result
            }
        } catch (e: Exception) {
            errorMessage = "Error generating PDF: ${e.message}"
        }
    }

    fun handleAction(action: PendingAction) {
        if (selectedEnvelope.isWindowed) {
            when (action) {
                PendingAction.GENERATE_PDF -> executePdfGeneration(null, false)
                PendingAction.PRINT_DIRECT -> executePrint(null, false)
            }
        } else {
            if (currentRecipient.name.isBlank() && currentRecipient.street.isBlank()) {
                errorMessage = "Please enter at least a Recipient Name or Street Address."
                return
            }

            if (!isRecipientInBook && currentRecipient.isNotEmpty()) {
                if (saveToAddressBookChecked) {
                    when (action) {
                        PendingAction.GENERATE_PDF -> executePdfGeneration(currentRecipient, true)
                        PendingAction.PRINT_DIRECT -> executePrint(currentRecipient, true)
                    }
                } else {
                    promptSaveDialogAction = currentRecipient to action
                }
            } else {
                when (action) {
                    PendingAction.GENERATE_PDF -> executePdfGeneration(currentRecipient, false)
                    PendingAction.PRINT_DIRECT -> executePrint(currentRecipient, false)
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left Column: Configuration & Inputs
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .verticalScroll(leftScrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Envelope Configuration",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Section 1: Return Address
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. Return Address",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = onAddReturnAddress) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Address")
                        }
                    }

                    var returnDropdownExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = returnDropdownExpanded,
                        onExpandedChange = { returnDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = "${returnAddress.name} (${returnAddress.street}, ${returnAddress.city})",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Sender") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = returnDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = returnDropdownExpanded,
                            onDismissRequest = { returnDropdownExpanded = false }
                        ) {
                            returnAddresses.forEach { addr ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(addr.name, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                "${addr.street}, ${addr.cityStateZip()}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        onSelectReturnAddress(addr.id)
                                        returnDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Envelope Size Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. Envelope Size",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    envelopeList.forEach { env ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = env.id == selectedEnvelopeId,
                                onClick = { selectedEnvelopeId = env.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = env.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = env.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Recipient Address
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3. Recipient Address",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (!selectedEnvelope.isWindowed && addressBook.isNotEmpty()) {
                            var bookDropdownExpanded by remember { mutableStateOf(false) }
                            Box {
                                FilledTonalButton(
                                    onClick = { bookDropdownExpanded = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("From Address Book")
                                }
                                DropdownMenu(
                                    expanded = bookDropdownExpanded,
                                    onDismissRequest = { bookDropdownExpanded = false }
                                ) {
                                    addressBook.forEach { contact ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(contact.name, style = MaterialTheme.typography.bodyMedium)
                                                    Text(
                                                        "${contact.street}, ${contact.cityStateZip()}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                onRecipientChange(contact)
                                                bookDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (selectedEnvelope.isWindowed) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Windowed envelope selected: Recipient address entry is not required because the address appears through the window cutout.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = currentRecipient.name,
                            onValueChange = { onRecipientChange(currentRecipient.copy(name = it)) },
                            label = { Text("Recipient Name / Company") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = currentRecipient.street,
                            onValueChange = { onRecipientChange(currentRecipient.copy(street = it)) },
                            label = { Text("Street Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = currentRecipient.street2,
                            onValueChange = { onRecipientChange(currentRecipient.copy(street2 = it)) },
                            label = { Text("Address Line 2 (Suite/Unit/Apt)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = currentRecipient.city,
                                onValueChange = { onRecipientChange(currentRecipient.copy(city = it)) },
                                label = { Text("City") },
                                singleLine = true,
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = currentRecipient.state,
                                onValueChange = { onRecipientChange(currentRecipient.copy(state = it.uppercase())) },
                                label = { Text("State") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = currentRecipient.zip,
                                onValueChange = { onRecipientChange(currentRecipient.copy(zip = it)) },
                                label = { Text("ZIP") },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f)
                            )
                        }

                        if (!isRecipientInBook && currentRecipient.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = saveToAddressBookChecked,
                                    onCheckedChange = { saveToAddressBookChecked = it }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Save this recipient to Address Book for future envelopes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { onRecipientChange(Address()) },
                                enabled = currentRecipient.isNotEmpty()
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Fields")
                            }
                        }
                    }
                }
            }
        }

        // Right Column: Live Envelope Visual Preview AND Action Buttons Under Preview
        Column(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight()
                .verticalScroll(rightScrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Real-Time Layout Preview",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    EnvelopePreview(
                        envelope = selectedEnvelope,
                        returnAddress = returnAddress,
                        recipientAddress = if (selectedEnvelope.isWindowed) null else currentRecipient,
                        modifier = Modifier.padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Dimensions: ${selectedEnvelope.description}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ACTIONS DIRECTLY UNDER PREVIEW
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Output Options",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Rotate Checkbox (Checked by default)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = rotateChecked,
                            onCheckedChange = { rotateChecked = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Rotate",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Rotates envelope 90° for standard printer tray feeding",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 1. Direct System Print Button
                    Button(
                        onClick = { handleAction(PendingAction.PRINT_DIRECT) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Print to Printer")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Print Directly to Printer...", style = MaterialTheme.typography.titleMedium)
                    }

                    // 2. Generate PDF Button
                    FilledTonalButton(
                        onClick = { handleAction(PendingAction.GENERATE_PDF) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Generate PDF")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate / Save PDF...", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    // Offer to Save to Address Book Confirmation Dialog
    promptSaveDialogAction?.let { (addr, action) ->
        AlertDialog(
            onDismissRequest = { promptSaveDialogAction = null },
            title = { Text("Save to Address Book?") },
            text = { Text("'${addr.name}' is not currently in your Address Book. Would you like to save it for future envelopes?") },
            confirmButton = {
                Button(
                    onClick = {
                        promptSaveDialogAction = null
                        when (action) {
                            PendingAction.GENERATE_PDF -> executePdfGeneration(addr, true)
                            PendingAction.PRINT_DIRECT -> executePrint(addr, true)
                        }
                    }
                ) {
                    Text("Save & Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        promptSaveDialogAction = null
                        when (action) {
                            PendingAction.GENERATE_PDF -> executePdfGeneration(addr, false)
                            PendingAction.PRINT_DIRECT -> executePrint(addr, false)
                        }
                    }
                ) {
                    Text("Continue Without Saving")
                }
            }
        )
    }

    // Print Success Notice Dialog
    if (printSuccessNotice) {
        AlertDialog(
            onDismissRequest = { printSuccessNotice = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Print Job Sent") },
            text = { Text("The envelope print job was sent to your system printer.") },
            confirmButton = {
                Button(onClick = { printSuccessNotice = false }) {
                    Text("OK")
                }
            }
        )
    }

    // PDF Generation Success Dialog
    generatedFileSuccess?.let { file ->
        AlertDialog(
            onDismissRequest = { generatedFileSuccess = null },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Envelope PDF Created") },
            text = {
                Column {
                    Text("PDF has been successfully generated at:")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = file.absolutePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            if (Desktop.isDesktopSupported()) {
                                Desktop.getDesktop().open(file)
                            }
                        } catch (e: Exception) {
                            System.err.println("Could not open file automatically: ${e.message}")
                        }
                        generatedFileSuccess = null
                    }
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open PDF")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            try {
                                if (Desktop.isDesktopSupported()) {
                                    Desktop.getDesktop().open(file.parentFile ?: file)
                                }
                            } catch (e: Exception) {
                                System.err.println("Could not open directory: ${e.message}")
                            }
                            generatedFileSuccess = null
                        }
                    ) {
                        Text("Show in Folder")
                    }
                    TextButton(onClick = { generatedFileSuccess = null }) {
                        Text("Done")
                    }
                }
            }
        )
    }

    // Error Dialog
    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            icon = { Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Notice") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}
