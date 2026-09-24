package com.envelopes

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

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

    fun loadData(): AppData {
        return try {
            if (dataFile.exists()) {
                val content = dataFile.readText(Charsets.UTF_8)
                val loaded = json.decodeFromString<AppData>(content)
                if (loaded.returnAddresses.isEmpty()) {
                    AppData(
                        returnAddresses = EnvelopeConstants.DEFAULT_RETURN_ADDRESSES,
                        addressBook = loaded.addressBook,
                        defaultReturnAddressId = EnvelopeConstants.DEFAULT_RETURN_ADDRESSES.first().id
                    )
                } else {
                    loaded
                }
            } else {
                val initial = AppData()
                saveData(initial)
                initial
            }
        } catch (e: Exception) {
            System.err.println("Error reading storage file: ${e.message}")
            val fallback = AppData()
            fallback
        }
    }

    fun saveData(data: AppData) {
        try {
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            val content = json.encodeToString(data)
            dataFile.writeText(content, Charsets.UTF_8)
        } catch (e: Exception) {
            System.err.println("Error saving storage file: ${e.message}")
        }
    }
}
