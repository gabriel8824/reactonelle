package com.reactonelle.bridge.handlers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.core.content.ContextCompat
import com.reactonelle.bridge.BridgeHandler
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Date

/**
 * Handler para gravação de áudio básico
 */
class MicrophoneStartHandler : BridgeHandler {
    
    companion object {
        var mediaRecorder: MediaRecorder? = null
        var audioFile: File? = null
    }

    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            onError("Microphone permission not granted")
            return
        }

        if (mediaRecorder != null) {
            onError("Recording already in progress")
            return
        }

        try {
            val fileName = "audio_${Date().time}.m4a"
            audioFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), fileName)
            
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile!!.absolutePath)
                prepare()
                start()
            }
            
            onSuccess(JSONObject().put("status", "recording").put("file", audioFile!!.name))
            
        } catch (e: IOException) {
            mediaRecorder = null
            audioFile = null
            onError("Recording failed: ${e.message}")
        }
    }
}

class MicrophoneStopHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val recorder = MicrophoneStartHandler.mediaRecorder
        val file = MicrophoneStartHandler.audioFile

        if (recorder == null) {
            onError("No recording in progress")
            return
        }

        try {
            recorder.stop()
            recorder.release()
        } catch (e: Exception) {
            // Pode falhar se parar muito rápido
        } finally {
            MicrophoneStartHandler.mediaRecorder = null
        }
        
        // Retorna base64 se solicitado
        val includeBase64 = payload.optBoolean("base64", false)
        
        Thread {
            try {
                val response = JSONObject().apply {
                    put("uri", file?.toURI().toString())
                    put("path", file?.absolutePath)
                    
                    if (includeBase64 && file != null && file.exists()) {
                        val bytes = file.readBytes()
                        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        put("base64", "data:audio/mp4;base64,$base64")
                    }
                }
                
                Handler(Looper.getMainLooper()).post {
                    onSuccess(response)
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    onError("Failed to process audio file")
                }
            }
        }.start()
    }
}
