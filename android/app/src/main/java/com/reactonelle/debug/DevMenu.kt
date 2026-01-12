package com.reactonelle.debug

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.webkit.WebView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Menu de desenvolvedor estilo Expo
 * Ativado ao balançar o dispositivo
 */
class DevMenu(
    private val activity: Activity,
    private val webView: WebView,
    private val currentUrl: String
) {
    
    companion object {
        // Flag global para habilitar/desabilitar o DevMenu
        @Volatile
        var isEnabled: Boolean = true
        
        // URL do servidor de desenvolvimento
        var devServerUrl: String = "http://localhost:5173"
    }
    
    private val menuOptions = listOf(
        "🔄 Reload" to ::reloadPage,
        "🔃 Hard Reload (Clear Cache)" to ::hardReload,
        "📋 Copy Device Info" to ::copyDeviceInfo,
        "🌐 Change Server URL" to ::changeServerUrl,
        "🔍 Toggle Remote Debugging" to ::toggleRemoteDebugging,
        "📊 Show Performance" to ::showPerformance,
        "🧹 Clear Storage" to ::clearStorage,
        "ℹ️ About" to ::showAbout
    )
    
    /**
     * Mostra o menu de desenvolvedor
     */
    fun show() {
        if (!isEnabled) return
        
        val items = menuOptions.map { it.first }.toTypedArray()
        
        MaterialAlertDialogBuilder(activity)
            .setTitle("🛠️ Reactonelle Dev Menu")
            .setItems(items) { dialog, which ->
                dialog.dismiss()
                menuOptions[which].second.invoke()
            }
            .setNegativeButton("Fechar", null)
            .show()
    }
    
    /**
     * Recarrega a página atual
     */
    private fun reloadPage() {
        activity.runOnUiThread {
            webView.reload()
            showToast("🔄 Recarregando...")
        }
    }
    
    /**
     * Recarrega limpando cache
     */
    private fun hardReload() {
        activity.runOnUiThread {
            webView.clearCache(true)
            webView.reload()
            showToast("🔃 Cache limpo e recarregando...")
        }
    }
    
    /**
     * Copia informações do dispositivo
     */
    private fun copyDeviceInfo() {
        val info = buildString {
            appendLine("=== Reactonelle Device Info ===")
            appendLine("Model: ${Build.MODEL}")
            appendLine("Manufacturer: ${Build.MANUFACTURER}")
            appendLine("Android Version: ${Build.VERSION.RELEASE}")
            appendLine("SDK: ${Build.VERSION.SDK_INT}")
            appendLine("Device: ${Build.DEVICE}")
            appendLine("Product: ${Build.PRODUCT}")
            appendLine("Current URL: $currentUrl")
            appendLine("Dev Server: ${devServerUrl}")
            appendLine("WebView Version: ${getWebViewVersion()}")
        }
        
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Device Info", info))
        showToast("📋 Info copiada!")
    }
    
    /**
     * Altera URL do servidor de desenvolvimento
     */
    private fun changeServerUrl() {
        val input = android.widget.EditText(activity).apply {
            setText(devServerUrl)
            hint = "http://localhost:5173"
            setPadding(48, 32, 48, 32)
        }
        
        MaterialAlertDialogBuilder(activity)
            .setTitle("🌐 Server URL")
            .setMessage("Digite a URL do servidor de desenvolvimento:")
            .setView(input)
            .setPositiveButton("Conectar") { _, _ ->
                val newUrl = input.text.toString().trim()
                if (newUrl.isNotEmpty()) {
                    devServerUrl = newUrl
                    activity.runOnUiThread {
                        webView.loadUrl(newUrl)
                    }
                    showToast("🌐 Conectando a $newUrl...")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    /**
     * Toggle debugging remoto do WebView
     */
    private fun toggleRemoteDebugging() {
        WebView.setWebContentsDebuggingEnabled(true)
        showToast("🔍 Remote debugging habilitado!\nAbra chrome://inspect")
    }
    
    /**
     * Mostra informações de performance
     */
    private fun showPerformance() {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        
        val info = buildString {
            appendLine("📊 Performance")
            appendLine("")
            appendLine("Memory: ${usedMemory}MB / ${maxMemory}MB")
            appendLine("Processors: ${runtime.availableProcessors()}")
        }
        
        MaterialAlertDialogBuilder(activity)
            .setTitle("📊 Performance")
            .setMessage(info)
            .setPositiveButton("OK", null)
            .setNeutralButton("🧹 GC") { _, _ ->
                System.gc()
                showToast("🧹 Garbage Collection executado")
            }
            .show()
    }
    
    /**
     * Limpa storage do WebView
     */
    private fun clearStorage() {
        MaterialAlertDialogBuilder(activity)
            .setTitle("🧹 Limpar Storage")
            .setMessage("Isso irá limpar:\n• LocalStorage\n• SessionStorage\n• Cookies\n• Cache\n\nContinuar?")
            .setPositiveButton("Limpar") { _, _ ->
                activity.runOnUiThread {
                    webView.clearCache(true)
                    webView.clearHistory()
                    
                    // Limpa localStorage e sessionStorage
                    webView.evaluateJavascript("""
                        localStorage.clear();
                        sessionStorage.clear();
                        'cleared';
                    """.trimIndent()) {
                        showToast("🧹 Storage limpo!")
                        webView.reload()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    /**
     * Mostra informações sobre o app
     */
    private fun showAbout() {
        val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
        
        val info = buildString {
            appendLine("🚀 Reactonelle")
            appendLine("")
            appendLine("Version: ${packageInfo.versionName}")
            appendLine("Build: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode}")
            appendLine("")
            appendLine("A potência do Nativo")
            appendLine("com a velocidade do React")
            appendLine("")
            appendLine("Shake para abrir este menu")
        }
        
        MaterialAlertDialogBuilder(activity)
            .setTitle("ℹ️ Sobre")
            .setMessage(info)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun getWebViewVersion(): String {
        return try {
            val packageInfo = activity.packageManager.getPackageInfo("com.google.android.webview", 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
}
