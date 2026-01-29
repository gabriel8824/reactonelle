package com.reactonelle.splash

import android.content.Context
import org.json.JSONObject
import java.io.InputStream

/**
 * Configuração da Splash Screen carregada do arquivo splash_config.json
 */
data class SplashConfig(
    val type: SplashType = SplashType.STATIC,
    val backgroundColor: String = "#0F172A",
    val logo: LogoConfig? = null,
    val lottie: LottieConfig? = null,
    val text: TextConfig? = null,
    val duration: Long = 3000,
    val transition: TransitionType = TransitionType.FADE,
    val autoHide: Boolean = false
) {
    enum class SplashType {
        STATIC, LOTTIE, ANIMATED
    }
    
    enum class TransitionType {
        FADE, SLIDE, ZOOM
    }
    
    enum class AnimationType {
        NONE, FADE, FADE_SCALE, BOUNCE, SLIDE_UP, PULSE
    }
    
    data class LogoConfig(
        val src: String = "",
        val width: Int = 150,
        val height: Int = 150,
        val animation: AnimationType = AnimationType.FADE_SCALE
    )
    
    data class LottieConfig(
        val src: String = "",
        val width: Int = 300,
        val height: Int = 300,
        val loop: Boolean = false,
        val autoPlay: Boolean = true
    )
    
    data class TextConfig(
        val content: String = "",
        val color: String = "#FFFFFF",
        val fontSize: Int = 16,
        val animation: AnimationType = AnimationType.NONE
    )
    
    companion object {
        private const val CONFIG_FILE = "splash_config.json"
        
        /**
         * Carrega a configuração do arquivo splash_config.json nos assets
         */
        fun load(context: Context): SplashConfig {
            return try {
                val inputStream: InputStream = context.assets.open(CONFIG_FILE)
                val json = inputStream.bufferedReader().use { it.readText() }
                parse(json)
            } catch (e: Exception) {
                // Retorna configuração padrão se não encontrar o arquivo
                SplashConfig()
            }
        }
        
        private fun parse(json: String): SplashConfig {
            val obj = JSONObject(json)
            
            val type = when (obj.optString("type", "static").lowercase()) {
                "lottie" -> SplashType.LOTTIE
                "animated" -> SplashType.ANIMATED
                else -> SplashType.STATIC
            }
            
            val transition = when (obj.optString("transition", "fade").lowercase()) {
                "slide" -> TransitionType.SLIDE
                "zoom" -> TransitionType.ZOOM
                else -> TransitionType.FADE
            }
            
            val logoConfig = obj.optJSONObject("logo")?.let { logoObj ->
                LogoConfig(
                    src = logoObj.optString("src", ""),
                    width = logoObj.optInt("width", 150),
                    height = logoObj.optInt("height", 150),
                    animation = parseAnimation(logoObj.optString("animation", "fade-scale"))
                )
            }
            
            val lottieConfig = obj.optJSONObject("lottie")?.let { lottieObj ->
                LottieConfig(
                    src = lottieObj.optString("src", ""),
                    width = lottieObj.optInt("width", 300),
                    height = lottieObj.optInt("height", 300),
                    loop = lottieObj.optBoolean("loop", false),
                    autoPlay = lottieObj.optBoolean("autoPlay", true)
                )
            }
            
            val textConfig = obj.optJSONObject("text")?.let { textObj ->
                TextConfig(
                    content = textObj.optString("content", ""),
                    color = textObj.optString("color", "#FFFFFF"),
                    fontSize = textObj.optInt("fontSize", 16),
                    animation = parseAnimation(textObj.optString("animation", "none"))
                )
            }
            
            return SplashConfig(
                type = type,
                backgroundColor = obj.optString("backgroundColor", "#0F172A"),
                logo = logoConfig,
                lottie = lottieConfig,
                text = textConfig,
                duration = obj.optLong("duration", 3000),
                transition = transition,
                autoHide = obj.optBoolean("autoHide", false)
            )
        }
        
        private fun parseAnimation(value: String): AnimationType {
            return when (value.lowercase().replace("-", "_")) {
                "fade" -> AnimationType.FADE
                "fade_scale" -> AnimationType.FADE_SCALE
                "bounce" -> AnimationType.BOUNCE
                "slide_up" -> AnimationType.SLIDE_UP
                "pulse" -> AnimationType.PULSE
                else -> AnimationType.NONE
            }
        }
    }
}
