package com.reactonelle.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import java.io.InputStream

/**
 * View customizada que renderiza a splash screen baseada na configuração
 */
class SplashView(context: Context) : FrameLayout(context) {
    
    private var config: SplashConfig = SplashConfig()
    private var logoView: ImageView? = null
    private var lottieView: LottieAnimationView? = null
    private var textView: TextView? = null
    private var contentContainer: LinearLayout? = null
    
    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
    }
    
    /**
     * Configura a splash screen com base no SplashConfig
     */
    fun setup(splashConfig: SplashConfig) {
        this.config = splashConfig
        
        // Define cor de fundo
        try {
            setBackgroundColor(Color.parseColor(config.backgroundColor))
        } catch (e: Exception) {
            setBackgroundColor(Color.parseColor("#0F172A"))
        }
        
        // Container central para alinhar conteúdo
        contentContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }
        addView(contentContainer)
        
        // Configura baseado no tipo
        when (config.type) {
            SplashConfig.SplashType.LOTTIE -> setupLottie()
            else -> setupStatic()
        }
        
        // Adiciona texto se configurado
        config.text?.let { textConfig ->
            if (textConfig.content.isNotEmpty()) {
                setupText(textConfig)
            }
        }
    }
    
    private fun setupStatic() {
        config.logo?.let { logoConfig ->
            if (logoConfig.src.isNotEmpty()) {
                try {
                    val assetPath = "splash/${logoConfig.src}"
                    val inputStream: InputStream = context.assets.open(assetPath)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    
                    logoView = ImageView(context).apply {
                        setImageBitmap(bitmap)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        
                        val widthPx = dpToPx(logoConfig.width)
                        val heightPx = dpToPx(logoConfig.height)
                        
                        layoutParams = LinearLayout.LayoutParams(widthPx, heightPx).apply {
                            gravity = Gravity.CENTER_HORIZONTAL
                        }
                        
                        // Prepara para animação
                        alpha = 0f
                        scaleX = 0.5f
                        scaleY = 0.5f
                    }
                    contentContainer?.addView(logoView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun setupLottie() {
        config.lottie?.let { lottieConfig ->
            if (lottieConfig.src.isNotEmpty()) {
                lottieView = LottieAnimationView(context).apply {
                    val assetPath = "splash/${lottieConfig.src}"
                    setAnimation(assetPath)
                    
                    repeatCount = if (lottieConfig.loop) LottieDrawable.INFINITE else 0
                    
                    val widthPx = dpToPx(lottieConfig.width)
                    val heightPx = dpToPx(lottieConfig.height)
                    
                    layoutParams = LinearLayout.LayoutParams(widthPx, heightPx).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                    
                    if (lottieConfig.autoPlay) {
                        playAnimation()
                    }
                }
                contentContainer?.addView(lottieView)
            }
        }
    }
    
    private fun setupText(textConfig: SplashConfig.TextConfig) {
        textView = TextView(context).apply {
            text = textConfig.content
            try {
                setTextColor(Color.parseColor(textConfig.color))
            } catch (e: Exception) {
                setTextColor(Color.WHITE)
            }
            textSize = textConfig.fontSize.toFloat()
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dpToPx(24)
            }
            
            // Prepara para animação
            alpha = 0f
        }
        contentContainer?.addView(textView)
    }
    
    /**
     * Inicia as animações de entrada
     */
    fun startEntranceAnimations(onComplete: (() -> Unit)? = null) {
        val animators = mutableListOf<Animator>()
        
        // Anima logo
        logoView?.let { logo ->
            config.logo?.let { logoConfig ->
                val logoAnimators = createAnimation(logo, logoConfig.animation)
                animators.addAll(logoAnimators)
            }
        }
        
        // Anima texto
        textView?.let { text ->
            config.text?.let { textConfig ->
                val textAnimators = createAnimation(text, textConfig.animation, startDelay = 300)
                animators.addAll(textAnimators)
            }
        }
        
        if (animators.isNotEmpty()) {
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(animators)
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            animatorSet.start()
        } else {
            onComplete?.invoke()
        }
    }
    
    private fun createAnimation(
        view: View, 
        type: SplashConfig.AnimationType,
        startDelay: Long = 0
    ): List<Animator> {
        val duration = 600L
        
        return when (type) {
            SplashConfig.AnimationType.FADE -> {
                listOf(
                    ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
                        this.duration = duration
                        this.startDelay = startDelay
                        interpolator = DecelerateInterpolator()
                    }
                )
            }
            SplashConfig.AnimationType.FADE_SCALE -> {
                listOf(
                    ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
                        this.duration = duration
                        this.startDelay = startDelay
                        interpolator = DecelerateInterpolator()
                    },
                    ObjectAnimator.ofFloat(view, "scaleX", 0.5f, 1f).apply {
                        this.duration = duration
                        this.startDelay = startDelay
                        interpolator = DecelerateInterpolator()
                    },
                    ObjectAnimator.ofFloat(view, "scaleY", 0.5f, 1f).apply {
                        this.duration = duration
                        this.startDelay = startDelay
                        interpolator = DecelerateInterpolator()
                    }
                )
            }
            SplashConfig.AnimationType.BOUNCE -> {
                view.translationY = -100f
                listOf(
                    ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
                        this.duration = 300
                        this.startDelay = startDelay
                    },
                    ObjectAnimator.ofFloat(view, "translationY", -100f, 0f).apply {
                        this.duration = duration
                        this.startDelay = startDelay
                        interpolator = BounceInterpolator()
                    }
                )
            }
            SplashConfig.AnimationType.SLIDE_UP -> {
                view.translationY = 200f
                listOf(
                    ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
                        this.duration = duration
                        this.startDelay = startDelay
                        interpolator = DecelerateInterpolator()
                    },
                    ObjectAnimator.ofFloat(view, "translationY", 200f, 0f).apply {
                        this.duration = duration
                        this.startDelay = startDelay
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                )
            }
            SplashConfig.AnimationType.PULSE -> {
                view.alpha = 1f
                listOf(
                    ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f).apply {
                        this.duration = 800
                        this.startDelay = startDelay
                        repeatCount = ObjectAnimator.INFINITE
                        interpolator = AccelerateDecelerateInterpolator()
                    },
                    ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f).apply {
                        this.duration = 800
                        this.startDelay = startDelay
                        repeatCount = ObjectAnimator.INFINITE
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                )
            }
            SplashConfig.AnimationType.NONE -> {
                view.alpha = 1f
                emptyList()
            }
        }
    }
    
    /**
     * Cria animação de saída para transição
     */
    fun animateOut(onComplete: () -> Unit) {
        val duration = 400L
        
        when (config.transition) {
            SplashConfig.TransitionType.FADE -> {
                animate()
                    .alpha(0f)
                    .setDuration(duration)
                    .withEndAction(onComplete)
                    .start()
            }
            SplashConfig.TransitionType.SLIDE -> {
                animate()
                    .translationY(-height.toFloat())
                    .alpha(0f)
                    .setDuration(duration)
                    .withEndAction(onComplete)
                    .start()
            }
            SplashConfig.TransitionType.ZOOM -> {
                animate()
                    .scaleX(2f)
                    .scaleY(2f)
                    .alpha(0f)
                    .setDuration(duration)
                    .withEndAction(onComplete)
                    .start()
            }
        }
    }
    
    /**
     * Para animações Lottie se estiverem rodando
     */
    fun stopLottie() {
        lottieView?.cancelAnimation()
    }
    
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
