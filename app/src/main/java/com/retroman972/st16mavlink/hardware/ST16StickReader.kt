package com.retroman972.st16mavlink.hardware

import android.content.Context
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import com.retroman972.st16mavlink.mavlink.StickData
import timber.log.Timber

/**
 * Reads joystick/stick input from the Yuneec ST16 controller.
 * The ST16 exposes its sticks as a standard Android InputDevice.
 */
class ST16StickReader(private val context: Context) {

    private var lastStickData = StickData(0, 0, 0, 0, 0)
    private val inputDeviceListener = object : View.OnGenericMotionListener {
        override fun onGenericMotion(v: View?, event: MotionEvent?): Boolean {
            if (event != null) {
                updateStickValues(event)
            }
            return true
        }
    }

    fun initialize() {
        Timber.d("Initialisation du lecteur de sticks ST16")
        
        // Get all input devices and find joystick/gamepad
        val inputDevices = InputDevice.getDeviceIds()
        for (deviceId in inputDevices) {
            val device = InputDevice.getDevice(deviceId)
            Timber.d("Appareil détecté: ${device.name} (source: ${device.sources})")
            
            if ((device.sources and InputDevice.TOOL_TYPE_UNKNOWN) != 0) {
                Timber.i("Joystick/Gamepad trouvé: ${device.name}")
            }
        }
    }

    fun cleanup() {
        Timber.d("Nettoyage du lecteur de sticks")
    }

    fun readSticks(): StickData {
        return lastStickData
    }

    private fun updateStickValues(event: MotionEvent) {
        // Read analog stick values from the device
        // ST16 typically has:
        // - Left stick X (axis 0 or AXIS_X): Roll
        // - Left stick Y (axis 1 or AXIS_Y): Pitch
        // - Right stick X (axis 3 or AXIS_RZ): Yaw
        // - Right stick Y (axis 4 or AXIS_THROTTLE): Throttle (or Z axis)

        val axisValues = FloatArray(MotionEvent.TOOL_TYPE_UNKNOWN)

        // Left stick (Roll and Pitch)
        val axisX = getAxisValue(event, MotionEvent.AXIS_X) // Roll
        val axisY = getAxisValue(event, MotionEvent.AXIS_Y) // Pitch

        // Right stick (Yaw and Throttle)
        val axisRz = getAxisValue(event, MotionEvent.AXIS_RZ) // Yaw
        val axisZ = getAxisValue(event, MotionEvent.AXIS_Z) // Throttle

        // Convert normalized values (-1.0 to 1.0) to MAVLink range (-1000 to 1000 or 0 to 1000)
        val roll = (axisX * 1000).toInt()
        val pitch = (-axisY * 1000).toInt() // Invert Y for intuitive control
        val yaw = (axisRz * 1000).toInt()
        val throttle = ((axisZ + 1.0) / 2.0 * 1000).toInt() // Convert -1..1 to 0..1000

        lastStickData = StickData(
            roll = roll.coerceIn(-1000, 1000),
            pitch = pitch.coerceIn(-1000, 1000),
            throttle = throttle.coerceIn(0, 1000),
            yaw = yaw.coerceIn(-1000, 1000),
            buttons = 0
        )

        Timber.v("Sticks: R=$roll P=$pitch T=$throttle Y=$yaw")
    }

    private fun getAxisValue(event: MotionEvent, axis: Int): Float {
        return event.getAxisValue(axis).coerceIn(-1.0f, 1.0f)
    }

    /**
     * Scans for Yuneec ST16 input device directly.
     * Alternative method to read from device file if needed.
     */
    fun scanYuneecDevices(): List<InputDevice> {
        val devices = mutableListOf<InputDevice>()
        val inputDeviceIds = InputDevice.getDeviceIds()

        for (deviceId in inputDeviceIds) {
            val device = InputDevice.getDevice(deviceId)
            if (device.name.contains("ST16", ignoreCase = true) ||
                device.name.contains("Yuneec", ignoreCase = true)) {
                devices.add(device)
                Timber.i("Dispositif Yuneec trouvé: ${device.name}")
            }
        }

        return devices
    }
}
