package com.qibla.compass.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.abs

class CompassSensorManager(
    private val context: Context,
    private val onHeadingChanged: (Float) -> Unit,
    private val onAccuracyChanged: (Boolean) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private var lastHeading = 0f
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private var wasAligned = false
    private var lastVibrateTime = 0L

    fun start() {
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        var newHeading = lastHeading

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            val azimuthRad = orientation[0]
            val azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
            newHeading = (azimuthDeg + 360f) % 360f
        } else {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, gravity, 0, 3)
                hasGravity = true
            } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                hasGeomagnetic = true
            }

            if (hasGravity && hasGeomagnetic) {
                val r = FloatArray(9)
                val i = FloatArray(9)
                if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(r, orientation)
                    val azimuthRad = orientation[0]
                    val azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                    newHeading = (azimuthDeg + 360f) % 360f
                }
            }
        }

        // Smooth circular interpolation (prevent 0° / 360° spin flip)
        var diff = newHeading - lastHeading
        while (diff < -180f) diff += 360f
        while (diff > 180f) diff -= 360f
        val smoothed = (lastHeading + 0.15f * diff + 360f) % 360f
        lastHeading = smoothed

        onHeadingChanged(smoothed)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        val isCalibrated = accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
        onAccuracyChanged(isCalibrated)
    }

    /**
     * Telefon tam Kıbleye döndüğünde dokunsal kilit titreşimi verir.
     */
    fun checkAlignmentVibration(heading: Float, qiblaAngle: Float) {
        var diff = abs(heading - qiblaAngle)
        if (diff > 180f) diff = 360f - diff

        val isNowAligned = diff <= 2.5f

        if (isNowAligned && !wasAligned) {
            val now = System.currentTimeMillis()
            if (now - lastVibrateTime > 1500L) {
                triggerHapticTick()
                lastVibrateTime = now
            }
        }
        wasAligned = isNowAligned
    }

    private fun triggerHapticTick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}
