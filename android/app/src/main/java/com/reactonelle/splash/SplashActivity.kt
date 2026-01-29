package com.reactonelle.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.reactonelle.MainActivity

/**
 * Activity dedicada para exibir a splash screen customizável.
 * 
 * A splash screen é configurada através do arquivo splash_config.json
 * localizado em assets/splash_config.json.
 * 
 * ## Uso
 * 
 * 1. Configure a splash em `assets/splash_config.json`
 * 2. Coloque seus assets (logo, animações) em `assets/splash/`
 * 3. A splash será exibida automaticamente ao abrir o app
 * 4. Chame `Reactonelle.call('splash.hide')` no JavaScript para escondê-la
 * 
 * ## Tipos de Splash
 * 
 * - **static**: Exibe apenas logo com animações CSS
 * - **lottie**: Exibe animação Lottie
 * - **animated**: Combina logo com animações customizadas
 */
class SplashActivity : AppCompatActivity() {
    
    private lateinit var splashView: SplashView
    private lateinit var config: SplashConfig
    private var autoHideHandler: Handler? = null
    private var autoHideRunnable: Runnable? = null
    
    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configura tela cheia imersiva
        setupFullscreen()
        
        // Carrega configuração
        config = SplashConfig.load(this)
        
        // Cria e configura a view da splash
        splashView = SplashView(this)
        splashView.setup(config)
        setContentView(splashView)
        
        // Registra callback para esconder a splash
        SplashManager.registerHideCallback {
            hideSplash()
        }
        
        // Inicia animações de entrada
        splashView.post {
            splashView.startEntranceAnimations {
                // Animações concluídas
                if (config.autoHide) {
                    scheduleAutoHide()
                }
            }
        }
    }
    
    private fun setupFullscreen() {
        // Esconde barra de status e navegação
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = 
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        // Previne que a splash apareça brevemente antes de esconder as barras
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
    }
    
    private fun scheduleAutoHide() {
        autoHideHandler = Handler(Looper.getMainLooper())
        autoHideRunnable = Runnable {
            hideSplash()
        }
        autoHideHandler?.postDelayed(autoHideRunnable!!, config.duration)
    }
    
    private fun hideSplash() {
        // Cancela auto-hide se estiver agendado
        autoHideRunnable?.let { autoHideHandler?.removeCallbacks(it) }
        
        // Para animações Lottie
        splashView.stopLottie()
        
        // Anima saída e navega para MainActivity
        splashView.animateOut {
            navigateToMain()
        }
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        
        // Desabilita animação padrão de transição
        overridePendingTransition(0, 0)
        
        // Finaliza SplashActivity
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        SplashManager.unregisterHideCallback()
        autoHideRunnable?.let { autoHideHandler?.removeCallbacks(it) }
    }
    
    // Desabilita botão voltar na splash
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Não faz nada - usuário não pode sair da splash com back
    }
}
