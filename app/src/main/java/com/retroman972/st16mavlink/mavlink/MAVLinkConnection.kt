package com.retroman972.st16mavlink.mavlink

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class StickData(
    val roll: Int,      // -1000 to 1000
    val pitch: Int,     // -1000 to 1000
    val throttle: Int,  // 0 to 1000
    val yaw: Int,       // -1000 to 1000
    val buttons: Int = 0
)

class MAVLinkConnection(
    private val ipAddress: String,
    private val port: Int
) {
    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var systemId: Int = 1
    private var componentId: Int = 1
    private var messageSequence: Int = 0

    suspend fun connect() = withContext(Dispatchers.IO) {
        try {
            socket = Socket(ipAddress, port)
            inputStream = socket?.getInputStream()
            outputStream = socket?.getOutputStream()
            Timber.i("Socket MAVLink connecté: $ipAddress:$port")
        } catch (e: Exception) {
            Timber.e(e, "Erreur de connexion MAVLink")
            throw e
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
            Timber.i("Socket MAVLink fermé")
        } catch (e: Exception) {
            Timber.e(e, "Erreur lors de la fermeture du socket")
        }
    }

    suspend fun sendManualControl(stickData: StickData) = withContext(Dispatchers.IO) {
        try {
            val payload = createManualControlPayload(stickData)
            val packet = createMAVLinkPacket(69, payload) // MANUAL_CONTROL message ID = 69
            outputStream?.write(packet)
            outputStream?.flush()
        } catch (e: Exception) {
            Timber.e(e, "Erreur lors de l'envoi du message MANUAL_CONTROL")
        }
    }

    private fun createManualControlPayload(stickData: StickData): ByteArray {
        val buffer = ByteBuffer.allocate(11).order(ByteOrder.LITTLE_ENDIAN)
        
        buffer.putInt(1) // Target system ID
        buffer.putShort(stickData.roll.toShort())
        buffer.putShort(stickData.pitch.toShort())
        buffer.putShort(stickData.throttle.toShort())
        buffer.putShort(stickData.yaw.toShort())
        buffer.putShort(stickData.buttons.toShort())
        
        return buffer.array()
    }

    private fun createMAVLinkPacket(messageId: Int, payload: ByteArray): ByteArray {
        val payloadSize = payload.size
        val frameSize = 9 + payloadSize + 2 // header + payload + checksum

        val packet = ByteArray(frameSize)
        var index = 0

        // STX (start byte)
        packet[index++] = 0xFD.toByte()

        // Payload length
        packet[index++] = payloadSize.toByte()

        // Sequence
        packet[index++] = (messageSequence++ % 256).toByte()

        // System ID
        packet[index++] = systemId.toByte()

        // Component ID
        packet[index++] = componentId.toByte()

        // Message ID (3 bytes for MAVLink 2.0)
        packet[index++] = (messageId and 0xFF).toByte()
        packet[index++] = ((messageId shr 8) and 0xFF).toByte()
        packet[index++] = ((messageId shr 16) and 0xFF).toByte()

        // Flags (MAVLink 2.0)
        packet[index++] = 0x00.toByte()

        // Payload
        System.arraycopy(payload, 0, packet, index, payloadSize)
        index += payloadSize

        // Checksum (CRC16)
        val checksum = calculateChecksum(packet, index)
        packet[index] = (checksum and 0xFF).toByte()
        packet[index + 1] = ((checksum shr 8) and 0xFF).toByte()

        return packet
    }

    private fun calculateChecksum(packet: ByteArray, endIndex: Int): Int {
        var crc = 0xFFFF
        val polynomial = 0xEF01

        for (i in 1 until endIndex) {
            var tmp = packet[i].toInt() and 0xFF
            tmp = tmp xor (crc and 0xFF)
            tmp = tmp xor ((tmp shl 4) and 0xFF)

            crc = ((crc shr 8) and 0xFF) xor ((tmp shl 8) and 0xFFFF) xor 
                  ((tmp shl 3) and 0xFFFF) xor 
                  ((tmp shr 4) and 0xFFFF)
            crc = crc and 0xFFFF
        }

        return crc
    }
}
