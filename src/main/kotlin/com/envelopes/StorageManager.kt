package com.envelopes

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

object StorageManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val storageDir: File by lazy {
        val userHome = System.getProperty("user.home") ?: "."
        val dir = File(userHome, ".envelopes")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    private val dataFile: File by lazy {
        File(storageDir, "data.json")
    }

    fun loadData(): AppData = loadData(dataFile)

    fun saveData(data: AppData) = saveData(data, dataFile)

    fun loadData(file: File): AppData {
        if (!file.exists()) {
            val initial = AppData()
            saveData(initial, file)
            return initial
        }

        val loaded = try {
            json.decodeFromString<AppData>(file.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            System.err.println("Error reading storage file: ${e.message}")
            // Keep the unreadable file so the next save doesn't destroy the user's data.
            val backup = backupCorruptFile(file)
            if (backup != null) {
                System.err.println("Unreadable data file moved to: ${backup.absolutePath}")
            }
            return AppData()
        }

        val withReturnAddresses = if (loaded.returnAddresses.isEmpty()) {
            loaded.copy(
                returnAddresses = EnvelopeConstants.DEFAULT_RETURN_ADDRESSES,
                defaultReturnAddressId = EnvelopeConstants.DEFAULT_RETURN_ADDRESSES.first().id
            )
        } else {
            loaded
        }

        val normalized = reissueDuplicateIds(withReturnAddresses)
        if (normalized != loaded) {
            saveData(normalized, file)
        }
        return normalized
    }

    fun saveData(data: AppData, file: File) {
        try {
            file.parentFile?.mkdirs()
            val content = json.encodeToString(data)
            // Write to a temp file and move it into place so a crash mid-write can't corrupt data.json.
            val tmp = File(file.parentFile ?: File("."), file.name + ".tmp")
            tmp.writeText(content, Charsets.UTF_8)
            try {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (e: AtomicMoveNotSupportedException) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: Exception) {
            System.err.println("Error saving storage file: ${e.message}")
        }
    }

    /**
     * Gives a fresh id to every address whose id is blank or already used earlier in its list,
     * so list keys and id-based edits/deletes stay unambiguous.
     */
    fun reissueDuplicateIds(data: AppData): AppData {
        fun dedupe(list: List<Address>): List<Address> {
            val seen = mutableSetOf<String>()
            return list.map { addr ->
                if (addr.id.isBlank() || !seen.add(addr.id)) {
                    val fresh = addr.copy(id = UUID.randomUUID().toString())
                    seen.add(fresh.id)
                    fresh
                } else {
                    addr
                }
            }
        }

        val returnAddresses = dedupe(data.returnAddresses)
        val defaultId = data.defaultReturnAddressId
            ?.takeIf { id -> returnAddresses.any { it.id == id } }
            ?: returnAddresses.firstOrNull()?.id

        return data.copy(
            returnAddresses = returnAddresses,
            addressBook = dedupe(data.addressBook),
            defaultReturnAddressId = defaultId
        )
    }

    private fun backupCorruptFile(file: File): File? {
        return try {
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss").format(Date())
            val backup = File(file.parentFile ?: File("."), "${file.name}.corrupt-$stamp.bak")
            Files.move(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING)
            backup
        } catch (e: Exception) {
            System.err.println("Could not back up unreadable data file: ${e.message}")
            null
        }
    }
}
