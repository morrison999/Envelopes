package com.envelopes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.envelopes.Address
import com.envelopes.EnvelopeConfig

@Composable
fun EnvelopePreview(
    envelope: EnvelopeConfig,
    returnAddress: Address,
    recipientAddress: Address?,
    modifier: Modifier = Modifier
) {
    val aspectRatio = (envelope.widthPt / envelope.heightPt).toFloat()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Envelope Preview (${envelope.name} - ${envelope.description})",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .shadow(4.dp, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFFCFCF9))
                .border(1.dp, Color(0xFFD0D0C8), RoundedCornerShape(4.dp))
                .padding(12.dp)
        ) {
            // Return Address at Top-Left
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(0.5f)
            ) {
                if (returnAddress.name.isNotBlank()) {
                    Text(
                        text = returnAddress.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = Color.Black
                    )
                }
                if (returnAddress.street.isNotBlank()) {
                    Text(
                        text = returnAddress.street,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = Color(0xFF222222)
                    )
                }
                if (returnAddress.street2.isNotBlank()) {
                    Text(
                        text = returnAddress.street2,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = Color(0xFF222222)
                    )
                }
                val rCityStateZip = returnAddress.cityStateZip()
                if (rCityStateZip.isNotBlank()) {
                    Text(
                        text = rCityStateZip,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = Color(0xFF222222)
                    )
                }
            }

            // Recipient Address or Window
            if (envelope.isWindowed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .fillMaxWidth(0.55f)
                        .fillMaxHeight(0.5f)
                        .border(1.dp, Color(0xFF9E9E9E), RoundedCornerShape(2.dp))
                        .background(Color(0xFFEFEFEA).copy(alpha = 0.6f))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Window Opening\n(Address appears via insert)",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = Color(0xFF666666),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else if (recipientAddress != null && recipientAddress.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .fillMaxWidth(0.55f)
                        .padding(bottom = 8.dp)
                ) {
                    if (recipientAddress.name.isNotBlank()) {
                        Text(
                            text = recipientAddress.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = Color.Black
                        )
                    }
                    if (recipientAddress.street.isNotBlank()) {
                        Text(
                            text = recipientAddress.street,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = Color(0xFF111111)
                        )
                    }
                    if (recipientAddress.street2.isNotBlank()) {
                        Text(
                            text = recipientAddress.street2,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = Color(0xFF111111)
                        )
                    }
                    val cityStateZip = recipientAddress.cityStateZip()
                    if (cityStateZip.isNotBlank()) {
                        Text(
                            text = cityStateZip,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = Color(0xFF111111)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .fillMaxWidth(0.55f)
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "Recipient Address Area",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = Color(0xFFB0B0A8)
                    )
                }
            }
        }
    }
}
