package com.envelopes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
}
