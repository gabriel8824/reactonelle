package com.reactonelle.bridge.handlers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.reactonelle.bridge.BridgeHandler
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

const val REQUEST_IMAGE_CAPTURE = 1001
const val REQUEST_PICK_IMAGE = 1002

/**
 * Handler para tirar foto com a câmera
 * Payload: { quality?: 0-100, facing?: 'front'|'back' }
 * Retorna: { base64: string, uri: string, width: number, height: number }
 */
class CameraPhotoHandler : BridgeHandler {
    
    companion object {
        private var pendingCallback: ((JSONObject?) -> Unit)? = null
        private var pendingErrorCallback: ((String) -> Unit)? = null
        private var tempPhotoUri: Uri? = null
        private var photoQuality: Int = 80
        
        fun getTempUri(): Uri? = tempPhotoUri
        
        fun handleActivityResult(context: Context, success: Boolean, uri: Uri?) {
            if (success && uri != null) {
                // Processar imagem em background thread
                Thread {
                    try {
                        val result = processImage(context, uri)
                        Handler(Looper.getMainLooper()).post {
                            pendingCallback?.invoke(result)
                            cleanup()
                        }
                    } catch (e: Exception) {
                        Handler(Looper.getMainLooper()).post {
                            pendingErrorCallback?.invoke(e.message ?: "Failed to process image")
                            cleanup()
                        }
                    }
                }.start()
            } else {
                pendingErrorCallback?.invoke("Camera cancelled")
                cleanup()
            }
        }
        
        private fun processImage(context: Context, uri: Uri): JSONObject {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap == null) throw Exception("Failed to decode image")
            
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, photoQuality, outputStream)
            val bytes = outputStream.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            
            return JSONObject().apply {
                put("uri", uri.toString())
                put("base64", "data:image/jpeg;base64,$base64")
                put("width", bitmap.width)
                put("height", bitmap.height)
                put("size", bytes.size)
            }
        }
        
        private fun cleanup() {
            pendingCallback = null
            pendingErrorCallback = null
        }
    }
    
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        photoQuality = payload.optInt("quality", 80).coerceIn(1, 100)
        val facing = payload.optString("facing", "back")
        
        if (context !is Activity) {
            onError("Context must be an Activity")
            return
        }
        
        // Verifica permissão de câmera
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            onError("Camera permission not granted. Request permission first.")
            return
        }
        
        Handler(Looper.getMainLooper()).post {
            try {
                pendingCallback = onSuccess
                pendingErrorCallback = onError
                
                // Cria arquivo temporário para a foto
                val photoFile = createImageFile(context)
                tempPhotoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, tempPhotoUri)
                    if (facing == "front") {
                        putExtra("android.intent.extras.CAMERA_FACING", 1)
                        putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                        putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
                    }
                }
                
                context.startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
                
            } catch (e: Exception) {
                onError(e.message ?: "Failed to open camera")
            }
        }
    }
    
    private fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }
}

/**
 * Handler para escolher imagem/vídeo da galeria
 * Payload: { type?: 'image'|'video'|'any', multiple?: boolean }
 * Retorna: { files: [{ uri, name, type, size }] }
 */
class GalleryPickHandler : BridgeHandler {
    
    companion object {
        private var pendingCallback: ((JSONObject?) -> Unit)? = null
        private var pendingErrorCallback: ((String) -> Unit)? = null
        
        fun handleActivityResult(data: Intent?) {
            if (data == null) {
                pendingErrorCallback?.invoke("No image selected")
                cleanup()
                return
            }
            
            try {
                val files = JSONArray()
                
                // Múltiplas seleções
                data.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        files.put(createFileInfo(uri))
                    }
                }
                
                // Seleção única
                data.data?.let { uri ->
                    if (files.length() == 0) {
                        files.put(createFileInfo(uri))
                    }
                }
                
                pendingCallback?.invoke(JSONObject().put("files", files))
                
            } catch (e: Exception) {
                pendingErrorCallback?.invoke(e.message ?: "Failed to process selection")
            }
            
            cleanup()
        }
        
        private fun createFileInfo(uri: Uri): JSONObject {
            return JSONObject().apply {
                put("uri", uri.toString())
                put("name", uri.lastPathSegment ?: "file")
            }
        }
        
        private fun cleanup() {
            pendingCallback = null
            pendingErrorCallback = null
        }
    }
    
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val type = payload.optString("type", "image")
        val multiple = payload.optBoolean("multiple", false)
        
        if (context !is Activity) {
            onError("Context must be an Activity")
            return
        }
        
        Handler(Looper.getMainLooper()).post {
            try {
                pendingCallback = onSuccess
                pendingErrorCallback = onError
                
                val mimeType = when (type) {
                    "video" -> "video/*"
                    "any" -> "*/*"
                    else -> "image/*"
                }
                
                val intent = if (type == "image" || type == "video") {
                     val action = Intent.ACTION_PICK
                     val contentUri = if (type == "video") 
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI 
                     else 
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                     
                     Intent(action, contentUri).apply {
                         // ACTION_PICK geralmente não suporta EXTRA_ALLOW_MULTIPLE nativamente em todas as versões antigas como ACTION_GET_CONTENT
                         // Mas vamos tentar manter se o usuário pediu
                         if (multiple) {
                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                         }
                     }
                } else {
                    Intent(Intent.ACTION_GET_CONTENT).apply {
                        this.type = mimeType
                        addCategory(Intent.CATEGORY_OPENABLE)
                        if (multiple) {
                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        }
                    }
                }
                
                // Para ACTION_PICK não usamos createChooser se quisermos a galeria nativa direta
                // Para GET_CONTENT o chooser é opcional mas bom
                context.startActivityForResult(intent, REQUEST_PICK_IMAGE)
                
            } catch (e: Exception) {
                onError(e.message ?: "Failed to open gallery")
            }
        }
    }
}
