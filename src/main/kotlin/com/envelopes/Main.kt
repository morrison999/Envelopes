package com.envelopes

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.envelopes.ui.AddressBookScreen
import com.envelopes.ui.AddressEditDialog
import com.envelopes.ui.EnvelopeGeneratorScreen
import com.envelopes.ui.ReturnAddressesScreen
import java.awt.GraphicsEnvironment
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date
import java.util.Scanner
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JOptionPane
import kotlin.system.exitProcess

enum class AppTab(val title: String) {
    GENERATE("Generate Envelope"),
    RETURN_ADDRESSES("Return Addresses"),
    ADDRESS_BOOK("Address Book")
}

fun main(args: Array<String>) {
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        // Runs on whichever thread crashed (often the UI thread), so never block here waiting for input.
        handleFatalError("Uncaught exception in thread '${thread.name}'", throwable, waitForEnter = false)
    }

    try {
        println("=".repeat(60))
        println("         Envelopes - PDF & Direct Print Generator")
        println("                      Version 1.0.0")
        println("=".repeat(60))
        println("Runtime Environment:")
        println("  Java Version : ${System.getProperty("java.version")} (${System.getProperty("java.vendor")})")
        println("  OS Name/Arch : ${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})")
        println("  User Home    : ${System.getProperty("user.home")}")
        println("  Working Dir  : ${System.getProperty("user.dir")}")
        println("=".repeat(60))

        if (args.contains("--cli") || GraphicsEnvironment.isHeadless()) {
            println("Running in CLI mode...")
            runCliMode()
            return
        }

        println("Initializing Compose Desktop UI...")
        runComposeApp()
    } catch (t: Throwable) {
        handleFatalError("Fatal error during application startup", t, waitForEnter = true)
    }
}

private val fatalErrorReported = AtomicBoolean(false)

fun handleFatalError(title: String, t: Throwable, waitForEnter: Boolean) {
    // A second crash while reporting the first (e.g. in the dialog) should not re-enter or loop.
    if (!fatalErrorReported.compareAndSet(false, true)) {
        t.printStackTrace(System.err)
        return
    }

    System.err.println("\n" + "=".repeat(60))
    System.err.println(" [FATAL ERROR] $title")
    System.err.println("=".repeat(60))
    t.printStackTrace(System.err)

    try {
        val home = System.getProperty("user.home")
        val logDir = File(home, ".envelopes")
        logDir.mkdirs()
        val logFile = File(logDir, "error.log")
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        t.printStackTrace(pw)
        logFile.appendText(
            "=".repeat(60) + "\nError: $title\nTime: ${Date()}\nOS: ${System.getProperty("os.name")}\nJava: ${System.getProperty("java.version")}\n\n$sw\n"
        )
        System.err.println("\nCrash details saved to: ${logFile.absolutePath}")
    } catch (logEx: Exception) {
        System.err.println("Could not write error.log: ${logEx.message}")
    }

    try {
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(
                null,
                "An unexpected error occurred:\n\n${t.localizedMessage ?: t.javaClass.name}\n\nCheck the console or ~/.envelopes/error.log for full details.",
                "Envelopes - Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    } catch (_: Throwable) {}

    if (waitForEnter) {
        println("\n" + "-".repeat(60))
        print("Press ENTER to exit...")
        try {
            val scanner = Scanner(System.`in`)
            if (scanner.hasNextLine()) {
                scanner.nextLine()
            }
        } catch (_: Exception) {}
    }

    exitProcess(1)
}

@OptIn(ExperimentalMaterial3Api::class)
fun runComposeApp() {
    application {
        val windowState = rememberWindowState(
            size = DpSize(1020.dp, 760.dp),
            position = WindowPosition(Alignment.Center)
        )

        var appData by remember { mutableStateOf(StorageManager.loadData()) }
        var currentTab by remember { mutableStateOf(AppTab.GENERATE) }

        // Recipient address state for active editing / generation
        var currentRecipient by remember { mutableStateOf(Address()) }

        // Dialog states
        var showAddReturnAddressDialog by remember { mutableStateOf(false) }
        var editingReturnAddress by remember { mutableStateOf<Address?>(null) }

        var showAddAddressBookDialog by remember { mutableStateOf(false) }
        var editingAddressBookEntry by remember { mutableStateOf<Address?>(null) }

        var showAboutDialog by remember { mutableStateOf(false) }

        fun updateData(newData: AppData) {
            appData = newData
            StorageManager.saveData(newData)
        }

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Envelopes - PDF Generator"
        ) {
            // Desktop Menu Bar
            MenuBar {
                Menu("File", mnemonic = 'F') {
                    Item("New Envelope", onClick = {
                        currentRecipient = Address()
                        currentTab = AppTab.GENERATE
                    })
                    Separator()
                    Item("Exit", onClick = ::exitApplication)
                }

                Menu("Return Addresses", mnemonic = 'R') {
                    Item("Manage Return Addresses...", onClick = { currentTab = AppTab.RETURN_ADDRESSES })
                    Item("Add Return Address...", onClick = { showAddReturnAddressDialog = true })
                }

                Menu("Address Book", mnemonic = 'A') {
                    Item("Manage Address Book...", onClick = { currentTab = AppTab.ADDRESS_BOOK })
                    Item("Add Recipient...", onClick = { showAddAddressBookDialog = true })
                }

                Menu("Help", mnemonic = 'H') {
                    Item("About Envelopes", onClick = { showAboutDialog = true })
                }
            }

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = androidx.compose.ui.graphics.Color(0xFF1E56A0),
                    secondary = androidx.compose.ui.graphics.Color(0xFF163172),
                    tertiary = androidx.compose.ui.graphics.Color(0xFF005B96),
                    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE8ECEF)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Navigation Tab Bar
                        TabRow(
                            selectedTabIndex = currentTab.ordinal,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Tab(
                                selected = currentTab == AppTab.GENERATE,
                                onClick = { currentTab = AppTab.GENERATE },
                                text = { Text(AppTab.GENERATE.title) },
                                icon = { Icon(Icons.Default.Mail, contentDescription = null) }
                            )
                            Tab(
                                selected = currentTab == AppTab.RETURN_ADDRESSES,
                                onClick = { currentTab = AppTab.RETURN_ADDRESSES },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(AppTab.RETURN_ADDRESSES.title)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Badge { Text(appData.returnAddresses.size.toString()) }
                                    }
                                },
                                icon = { Icon(Icons.Default.Home, contentDescription = null) }
                            )
                            Tab(
                                selected = currentTab == AppTab.ADDRESS_BOOK,
                                onClick = { currentTab = AppTab.ADDRESS_BOOK },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(AppTab.ADDRESS_BOOK.title)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Badge { Text(appData.addressBook.size.toString()) }
                                    }
                                },
                                icon = { Icon(Icons.Default.Contacts, contentDescription = null) }
                            )
                        }

                        // Tab Content
                        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                            when (currentTab) {
                                AppTab.GENERATE -> {
                                    EnvelopeGeneratorScreen(
                                        returnAddresses = appData.returnAddresses,
                                        selectedReturnAddressId = appData.defaultReturnAddressId,
                                        addressBook = appData.addressBook,
                                        currentRecipient = currentRecipient,
                                        onRecipientChange = { currentRecipient = it },
                                        onSelectReturnAddress = { selectedId ->
                                            updateData(appData.copy(defaultReturnAddressId = selectedId))
                                        },
                                        onAddReturnAddress = { showAddReturnAddressDialog = true },
                                        onSaveToAddressBook = { newContact ->
                                            val existingIndex = appData.addressBook.indexOfFirst { it.matches(newContact) }
                                            val updatedBook = if (existingIndex >= 0) {
                                                appData.addressBook.toMutableList().apply { set(existingIndex, newContact) }
                                            } else {
                                                // Guard against an edited book entry that still carries the original's id.
                                                val idTaken = appData.addressBook.any { it.id == newContact.id }
                                                appData.addressBook + if (idTaken) newContact.copy(id = UUID.randomUUID().toString()) else newContact
                                            }
                                            updateData(appData.copy(addressBook = updatedBook))
                                        },
                                        onUpdateAddressBookEntry = { updatedContact ->
                                            val list = appData.addressBook.map { if (it.id == updatedContact.id) updatedContact else it }
                                            updateData(appData.copy(addressBook = list))
                                        }
                                    )
                                }
                                AppTab.RETURN_ADDRESSES -> {
                                    ReturnAddressesScreen(
                                        returnAddresses = appData.returnAddresses,
                                        defaultAddressId = appData.defaultReturnAddressId,
                                        onAdd = { showAddReturnAddressDialog = true },
                                        onEdit = { editingReturnAddress = it },
                                        onDelete = { toDelete ->
                                            val filtered = appData.returnAddresses.filter { it.id != toDelete.id }
                                            val newDefault = if (appData.defaultReturnAddressId == toDelete.id) {
                                                filtered.firstOrNull()?.id
                                            } else {
                                                appData.defaultReturnAddressId
                                            }
                                            updateData(appData.copy(returnAddresses = filtered, defaultReturnAddressId = newDefault))
                                        },
                                        onSetDefault = { toDefault ->
                                            updateData(appData.copy(defaultReturnAddressId = toDefault.id))
                                        }
                                    )
                                }
                                AppTab.ADDRESS_BOOK -> {
                                    AddressBookScreen(
                                        addressBook = appData.addressBook,
                                        onAdd = { showAddAddressBookDialog = true },
                                        onEdit = { editingAddressBookEntry = it },
                                        onDelete = { toDelete ->
                                            val filtered = appData.addressBook.filter { it.id != toDelete.id }
                                            updateData(appData.copy(addressBook = filtered))
                                        },
                                        onUseAddress = { selectedAddress ->
                                            currentRecipient = selectedAddress
                                            currentTab = AppTab.GENERATE
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Dialog: Add Return Address
                    if (showAddReturnAddressDialog) {
                        AddressEditDialog(
                            title = "Add Return Address",
                            onDismiss = { showAddReturnAddressDialog = false },
                            onSave = { newAddr ->
                                val updated = appData.returnAddresses + newAddr
                                updateData(appData.copy(returnAddresses = updated))
                                showAddReturnAddressDialog = false
                            }
                        )
                    }

                    // Dialog: Edit Return Address
                    editingReturnAddress?.let { addr ->
                        AddressEditDialog(
                            title = "Edit Return Address",
                            initialAddress = addr,
                            onDismiss = { editingReturnAddress = null },
                            onSave = { updatedAddr ->
                                val list = appData.returnAddresses.map { if (it.id == updatedAddr.id) updatedAddr else it }
                                updateData(appData.copy(returnAddresses = list))
                                editingReturnAddress = null
                            }
                        )
                    }

                    // Dialog: Add Address Book Entry
                    if (showAddAddressBookDialog) {
                        AddressEditDialog(
                            title = "Add Recipient to Address Book",
                            onDismiss = { showAddAddressBookDialog = false },
                            onSave = { newContact ->
                                val updated = appData.addressBook + newContact
                                updateData(appData.copy(addressBook = updated))
                                showAddAddressBookDialog = false
                            }
                        )
                    }

                    // Dialog: Edit Address Book Entry
                    editingAddressBookEntry?.let { contact ->
                        AddressEditDialog(
                            title = "Edit Address Book Entry",
                            initialAddress = contact,
                            onDismiss = { editingAddressBookEntry = null },
                            onSave = { updatedContact ->
                                val list = appData.addressBook.map { if (it.id == updatedContact.id) updatedContact else it }
                                updateData(appData.copy(addressBook = list))
                                editingAddressBookEntry = null
                            }
                        )
                    }

                    // Dialog: About
                    if (showAboutDialog) {
                        AlertDialog(
                            onDismissRequest = { showAboutDialog = false },
                            icon = { Icon(Icons.Default.Mail, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            title = { Text("Envelopes PDF Generator") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Version 1.0.0")
                                    Text("A modern, zero-dependency PDF envelope generator with integrated return address management, recipient address book, and direct system printing.")
                                    Text("Supports #10, #10 Windowed, #9, and #6 commercial envelope standards.")
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showAboutDialog = false }) {
                                    Text("Close")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

fun runCliMode() {
    val scanner = Scanner(System.`in`)
    val appData = StorageManager.loadData()
    val returnAddresses = appData.returnAddresses

    println("\n" + "=".repeat(50))
    println("             SELECT RETURN ADDRESS")
    println("=".repeat(50))

    returnAddresses.forEachIndexed { idx, addr ->
        val formatted = addr.formatSingleLine()
        val defaultStr = if (addr.id == appData.defaultReturnAddressId || (appData.defaultReturnAddressId == null && idx == 0)) " (Default)" else ""
        println("  [${idx + 1}] $formatted$defaultStr")
    }

    val selectedReturnAddr: Address
    while (true) {
        print("\nSelect return address (1-${returnAddresses.size}) [default: 1]: ")
        val line = if (scanner.hasNextLine()) scanner.nextLine().trim() else ""
        if (line.isEmpty()) {
            val defaultId = appData.defaultReturnAddressId
            selectedReturnAddr = returnAddresses.firstOrNull { it.id == defaultId } ?: returnAddresses[0]
            break
        }
        val intVal = line.toIntOrNull()
        if (intVal != null && intVal in 1..returnAddresses.size) {
            selectedReturnAddr = returnAddresses[intVal - 1]
            break
        }
        println("Invalid choice. Please enter a number between 1 and ${returnAddresses.size}.")
    }

    println("\n" + "=".repeat(50))
    println("             SELECT ENVELOPE SIZE")
    println("=".repeat(50))

    val envelopes = EnvelopeConstants.ENVELOPES
    envelopes.forEach { (key, env) ->
        println("  [$key] ${env.name} - ${env.description}")
    }

    val selectedEnvelope: EnvelopeConfig
    while (true) {
        print("\nSelect envelope (1-4): ")
        val choice = if (scanner.hasNextLine()) scanner.nextLine().trim() else ""
        if (envelopes.containsKey(choice)) {
            selectedEnvelope = envelopes.getValue(choice)
            break
        }
        println("Invalid choice. Please enter 1, 2, 3, or 4.")
    }

    val recipient = if (selectedEnvelope.isWindowed) {
        println("\nWindowed envelope selected. Skipping recipient address prompt.")
        null
    } else {
        println("\n" + "=".repeat(50))
        println("        ENVELOPE RECIPIENT ADDRESS ENTRY")
        println("=".repeat(50))

        print("Name: ")
        val name = if (scanner.hasNextLine()) scanner.nextLine().trim() else ""

        print("Street address: ")
        val street = if (scanner.hasNextLine()) scanner.nextLine().trim() else ""

        print("Street address line 2 (optional): ")
        val street2 = if (scanner.hasNextLine()) scanner.nextLine().trim() else ""

        print("City: ")
        val city = if (scanner.hasNextLine()) scanner.nextLine().trim() else ""

        print("State: ")
        val state = if (scanner.hasNextLine()) scanner.nextLine().trim() else ""

        print("Zip: ")
        val zip = if (scanner.hasNextLine()) scanner.nextLine().trim() else ""

        val addr = Address(
            name = name,
            street = street,
            street2 = street2,
            city = city,
            state = state,
            zip = zip
        )

        val isInBook = appData.addressBook.any { it.matches(addr) }
        if (!isInBook && addr.isNotEmpty()) {
            print("\nRecipient '${addr.name}' is not in your Address Book. Save it? (Y/n) [default: Y]: ")
            val saveChoice = if (scanner.hasNextLine()) scanner.nextLine().trim().lowercase() else ""
            if (saveChoice.isEmpty() || saveChoice == "y" || saveChoice == "yes") {
                val updatedBook = appData.addressBook + addr
                StorageManager.saveData(appData.copy(addressBook = updatedBook))
                println("Saved to Address Book!")
            }
        }
        addr
    }

    val generatedFile = PdfGenerator.generateEnvelopePdf(
        selectedEnvelope,
        recipient,
        selectedReturnAddr,
        outputFile = File(PdfGenerator.defaultOutputDir(), selectedEnvelope.filename)
    )
    println("\nEnvelope PDF generated successfully: ${generatedFile.absoluteFile.normalize().path}")
}
