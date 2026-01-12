package com.reactonelle.debug

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detector de shake gesture para ativar o menu de desenvolvedor
 */
class ShakeDetector(
    private val context: Context,
    private val onShake: () -> Unit
) : SensorEventListener {
    
    companion object {
        private const val SHAKE_THRESHOLD = 12.0f  // Sensibilidade do shake
        private const val SHAKE_TIME_LAPSE = 500   // Tempo mínimo entre shakes (ms)
    }
    
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastShakeTime: Long = 0
    private var isRegistered = false
    
    /**
     * Inicia a detecção de shake
     */
    fun start() {
        if (isRegistered) return
        
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        accelerometer?.let { sensor ->
            sensorManager?.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
            isRegistered = true
        }
    }
    
    /**
     * Para a detecção de shake
     */
    fun stop() {
        if (!isRegistered) return
        
        sensorManager?.unregisterListener(this)
        isRegistered = false
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            val x = sensorEvent.values[0]
            val y = sensorEvent.values[1]
            val z = sensorEvent.values[2]
            
            // Calcula a aceleração total (removendo a gravidade aproximadamente)
            val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat() - SensorManager.GRAVITY_EARTH
            
            if (acceleration > SHAKE_THRESHOLD) {
                val currentTime = System.currentTimeMillis()
                
                if (currentTime - lastShakeTime > SHAKE_TIME_LAPSE) {
                    lastShakeTime = currentTime
                    onShake()
                }
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Não precisamos tratar mudanças de precisão
    }
}
