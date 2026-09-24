package com.envelopes

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class StorageManagerTest {

    @Test
    fun testAddressMatches() {
        val addr1 = Address(
            name = "John Doe",
            street = "123 Main St",
            city = "Austin",
            state = "TX",
            zip = "78701"
        )
        val addr2 = Address(
            name = " john doe ",
            street = "123 main st",
            city = "AUSTIN",
            state = "tx",
            zip = "78701"
        )
        val addr3 = Address(
            name = "Jane Doe",
            street = "123 Main St",
            city = "Austin",
            state = "TX",
            zip = "78701"
        )

        assertTrue(addr1.matches(addr2))
        assertFalse(addr1.matches(addr3))
    }

    @Test
    fun testAddressEmptiness() {
        val emptyAddr = Address()
        assertTrue(emptyAddr.isEmpty())
        assertFalse(emptyAddr.isNotEmpty())

        val filledAddr = Address(name = "Test")
        assertFalse(filledAddr.isEmpty())
        assertTrue(filledAddr.isNotEmpty())
    }

    @Test
    fun testAppDataDefaults() {
        val appData = AppData()
        assertEquals(2, appData.returnAddresses.size)
        assertEquals(0, appData.addressBook.size)
        assertEquals("default-1", appData.defaultReturnAddressId)
    }

    private fun tempDir(): File = Files.createTempDirectory("envelopes-test").toFile().apply { deleteOnExit() }

    @Test
    fun testReissueDuplicateIds() {
        val a = Address(id = "dup", name = "A", street = "1 St")
        val b = Address(id = "dup", name = "B", street = "2 St")
        val c = Address(id = "", name = "C", street = "3 St")
        val ret = Address(id = "r1", name = "Ret", street = "9 St")
        val data = AppData(
            returnAddresses = listOf(ret, ret.copy(name = "Ret 2")),
            addressBook = listOf(a, b, c),
            defaultReturnAddressId = "r1"
        )

        val fixed = StorageManager.reissueDuplicateIds(data)

        assertEquals(3, fixed.addressBook.map { it.id }.toSet().size)
        assertTrue(fixed.addressBook.none { it.id.isBlank() })
        assertEquals("dup", fixed.addressBook[0].id)
        assertEquals("B", fixed.addressBook[1].name)
        assertEquals(2, fixed.returnAddresses.map { it.id }.toSet().size)
        assertEquals("r1", fixed.defaultReturnAddressId)

        val clean = AppData(returnAddresses = listOf(ret), addressBook = listOf(a), defaultReturnAddressId = "r1")
        assertEquals(clean, StorageManager.reissueDuplicateIds(clean))
    }

    @Test
    fun testLoadFixesDuplicateIdsAndPersists() {
        val file = File(tempDir(), "data.json")
        val a = Address(id = "dup", name = "A", street = "1 St")
        StorageManager.saveData(AppData(addressBook = listOf(a, a.copy(name = "B"))), file)

        val loaded = StorageManager.loadData(file)
        assertEquals(2, loaded.addressBook.map { it.id }.toSet().size)
        assertEquals(loaded, StorageManager.loadData(file))
    }

    @Test
    fun testCorruptFileIsBackedUpNotOverwritten() {
        val dir = tempDir()
        val file = File(dir, "data.json")
        file.writeText("{ this is not json")

        val loaded = StorageManager.loadData(file)
        assertEquals(AppData().returnAddresses, loaded.returnAddresses)

        StorageManager.saveData(loaded, file)
        val backups = dir.listFiles { f -> f.name.startsWith("data.json.corrupt-") }!!
        assertEquals(1, backups.size)
        assertEquals("{ this is not json", backups[0].readText())
    }

    @Test
    fun testSaveRoundTripLeavesNoTempFile() {
        val dir = tempDir()
        val file = File(dir, "data.json")
        val data = AppData(addressBook = listOf(Address(name = "Zoë", street = "1 St")))
        StorageManager.saveData(data, file)
        StorageManager.saveData(data, file)

        assertEquals(data, StorageManager.loadData(file))
        assertFalse(File(dir, "data.json.tmp").exists())
        assertNotEquals(0L, file.length())
    }
}
