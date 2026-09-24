package com.envelopes

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Address(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val street: String = "",
    val street2: String = "",
    val city: String = "",
    val state: String = "",
    val zip: String = ""
) {
    fun formatSingleLine(): String {
        val cityStateZip = listOf(city, "$state $zip".trim()).filter { it.isNotBlank() }.joinToString(", ")
        val parts = listOf(name, street, street2, cityStateZip).filter { it.isNotBlank() }
        return parts.joinToString(", ")
    }

    fun cityStateZip(): String {
        val cityPart = if (city.isNotBlank() && state.isNotBlank()) "$city, $state" else "$city$state"
        return listOf(cityPart, zip).filter { it.isNotBlank() }.joinToString(" ")
    }

    fun isEmpty(): Boolean {
        return name.isBlank() && street.isBlank() && street2.isBlank() && city.isBlank() && state.isBlank() && zip.isBlank()
    }

    fun isNotEmpty(): Boolean = !isEmpty()

    fun matches(other: Address): Boolean {
        return name.trim().equals(other.name.trim(), ignoreCase = true) &&
                street.trim().equals(other.street.trim(), ignoreCase = true) &&
                street2.trim().equals(other.street2.trim(), ignoreCase = true) &&
                city.trim().equals(other.city.trim(), ignoreCase = true) &&
                state.trim().equals(other.state.trim(), ignoreCase = true) &&
                zip.trim().equals(other.zip.trim(), ignoreCase = true)
    }
}

@Serializable
data class AppData(
    val returnAddresses: List<Address> = EnvelopeConstants.DEFAULT_RETURN_ADDRESSES,
    val addressBook: List<Address> = emptyList(),
    val defaultReturnAddressId: String? = EnvelopeConstants.DEFAULT_RETURN_ADDRESSES.firstOrNull()?.id
)

data class EnvelopeConfig(
    val id: String,
    val name: String,
    val description: String,
    val widthPt: Double,
    val heightPt: Double,
    val recipientLeft: Double,
    val recipientBottom: Double,
    val filename: String,
    val isWindowed: Boolean
)

object EnvelopeConstants {
    const val INCH: Double = 72.0

    val DEFAULT_RETURN_ADDRESSES = listOf(
        Address(
            id = "default-1",
            name = "David Morrison",
            street = "1900 Grace Ave",
            street2 = "#242",
            city = "Harlingen",
            state = "TX",
            zip = "78550"
        ),
        Address(
            id = "default-2",
            name = "Barreras",
            street = "1900 Grace Ave",
            street2 = "#223",
            city = "Harlingen",
            state = "TX",
            zip = "78550"
        )
    )

    val DEFAULT_RETURN_ADDRESS = DEFAULT_RETURN_ADDRESSES[0]

    val ENVELOPES = mapOf(
        "1" to EnvelopeConfig(
            id = "1",
            name = "#10",
            description = "Standard Commercial Envelope (9.5\" x 4.125\")",
            widthPt = 9.5 * INCH,
            heightPt = 4.125 * INCH,
            recipientLeft = 4.25 * INCH,
            recipientBottom = 1.9 * INCH,
            filename = "envelope_no10.pdf",
            isWindowed = false
        ),
        "2" to EnvelopeConfig(
            id = "2",
            name = "#10 windowed",
            description = "#10 Windowed Envelope (9.5\" x 4.125\")",
            widthPt = 9.5 * INCH,
            heightPt = 4.125 * INCH,
            recipientLeft = 4.25 * INCH,
            recipientBottom = 1.9 * INCH,
            filename = "envelope_no10_windowed.pdf",
            isWindowed = true
        ),
        "3" to EnvelopeConfig(
            id = "3",
            name = "#9",
            description = "#9 Commercial Envelope (8.875\" x 3.875\")",
            widthPt = 8.875 * INCH,
            heightPt = 3.875 * INCH,
            recipientLeft = 3.875 * INCH,
            recipientBottom = 1.75 * INCH,
            filename = "envelope_no9.pdf",
            isWindowed = false
        ),
        "4" to EnvelopeConfig(
            id = "4",
            name = "#6",
            description = "#6 Personal / Commercial Envelope (6.5\" x 3.625\")",
            widthPt = 6.5 * INCH,
            heightPt = 3.625 * INCH,
            recipientLeft = 2.75 * INCH,
            recipientBottom = 1.5 * INCH,
            filename = "envelope_no6.pdf",
            isWindowed = false
        )
    )
}
