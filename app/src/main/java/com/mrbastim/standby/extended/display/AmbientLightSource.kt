package com.mrbastim.standby.extended.display

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/** Listener exists only while a visible, resumed StandBy session requests automatic brightness. */
class AmbientLightSource(context: Context) {
    private val manager = context.getSystemService(SensorManager::class.java)
    val available: Boolean get() = manager?.getDefaultSensor(Sensor.TYPE_LIGHT) != null
    private var listener: SensorEventListener? = null

    fun start(onLux: (Float) -> Unit) {
        if (listener != null) return
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_LIGHT) ?: return
        val subscription = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                event.values.firstOrNull()?.let(onLux)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (manager.registerListener(subscription, sensor, 1_000_000)) listener = subscription
    }

    fun stop() {
        listener?.let { manager?.unregisterListener(it) }
        listener = null
    }
}
