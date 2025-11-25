package com.example.microphoneapp.data

import android.media.AudioDeviceInfo

class DeviceInfo(
    private val deviceInfo: AudioDeviceInfo,
    private val isLast: Boolean = false
) {

    fun print(): String {
        return buildString {
            val start =
                "****************************************************[ID:${deviceInfo.id}]****************************************************\n"
            val maxLength = start.length
            append(start)
            append(wrapText("productName", deviceInfo.productName, maxLength))
            append(wrapText("address", deviceInfo.address, maxLength))
            append(
                wrapText(
                    "audioDescriptors", deviceInfo.audioDescriptors.toList(), maxLength
                )
            )
            append(wrapText("audioProfiles", deviceInfo.audioProfiles.toList(), maxLength))
            append(
                wrapText(
                    "encapsulationMetadataTypes",
                    deviceInfo.encapsulationMetadataTypes.toList(),
                    maxLength
                )
            )
            append(
                wrapText(
                    "encapsulationModes", deviceInfo.encapsulationModes.toList(), maxLength
                )
            )
            append(wrapText("encodings", deviceInfo.encodings.toList(), maxLength))
            append(wrapText("sampleRates", deviceInfo.sampleRates.toList(), maxLength))
            append(
                wrapText(
                    "channelIndexMasks", deviceInfo.channelIndexMasks.toList(), maxLength
                )
            )
            append(wrapText("channelCounts", deviceInfo.channelCounts.toList(), maxLength))
            append(wrapText("channelMasks", deviceInfo.channelMasks.toList(), maxLength))
            append("*".repeat(maxLength))
            if (!isLast) append("\n\n")
        }
    }

    private fun wrapText(label: String, value: Any?, totalWidth: Int): String {
        val valueStr = value?.toString() ?: "null"
        val prefix = "* $label : "
        val suffix = "*"
        val availableWidth = totalWidth - prefix.length - suffix.length
        val builder = StringBuilder()

        var index = 0
        var firstLine = true

        while (index < valueStr.length) {
            val end = (index + availableWidth).coerceAtMost(valueStr.length)
            val part = valueStr.substring(index, end)
            val padding = " ".repeat(availableWidth - part.length)

            if (firstLine) {
                // 第一行包含 prefix
                builder.append(prefix).append(part).append(padding).append(suffix).append("\n")
                firstLine = false
            } else {
                // 後續行補空白與 prefix 長度相同，保持寬度
                val indent = " ".repeat(prefix.length - 1) // 因為第一行有 '*'，縮一個
                builder.append("*").append(indent).append(part)

                // 計算目前長度，補空白讓右側 '*' 對齊
                val currentLength = builder.substring(builder.lastIndexOf("\n") + 1).length
                if (currentLength < totalWidth - 1) {
                    builder.append(" ".repeat(totalWidth - 1 - currentLength))
                }

                builder.append(suffix).append("\n")
            }

            index += availableWidth
        }

        // 若內容太短（僅一行）
        if (valueStr.isEmpty()) {
            val padding = " ".repeat(availableWidth - valueStr.length)
            builder.append(prefix).append(padding).append(suffix).append("\n")
        }

        return builder.toString()
    }
}