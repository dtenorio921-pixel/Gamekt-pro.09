package app.gamenative.translation

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import timber.log.Timber
import java.util.Locale

class TranslationOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: FrameLayout? = null
    private var translationText: TextView? = null
    private var speakButton: Button? = null
    private var isShowing = false
    private var lastTranslatedText = ""
    private var lastTargetLang = "pt"
    private var tts: TextToSpeech? = null
    private var isSpeaking = false

    private val layoutParams = WindowManager.LayoutParams(
        600,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        x = 0
        y = 100
    }

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Timber.d("TTS initialized successfully")
            } else {
                Timber.w("TTS initialization failed")
            }
        }
    }

    fun show() {
        if (isShowing) return
        try {
            val container = FrameLayout(context).apply {
                setBackgroundColor(Color.argb(210, 20, 20, 20))
                setPadding(20, 12, 20, 12)
            }

            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }

            val headerLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val titleText = TextView(context).apply {
                text = "🌐 Gamekt Pro — Tradução IA"
                setTextColor(Color.parseColor("#4FC3F7"))
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            speakButton = Button(context).apply {
                text = "🔊 Ouvir"
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#1565C0"))
                textSize = 10f
                setPadding(16, 8, 16, 8)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener { speakTranslation() }
            }

            headerLayout.addView(titleText)
            headerLayout.addView(speakButton)

            val divider = TextView(context).apply {
                setBackgroundColor(Color.parseColor("#333333"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).apply { setMargins(0, 8, 0, 8) }
            }

            val scrollView = ScrollView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    300
                )
            }

            translationText = TextView(context).apply {
                text = "🔄 Tradução IA iniciada..."
                setTextColor(Color.WHITE)
                textSize = 13f
                setPadding(0, 4, 0, 4)
            }

            scrollView.addView(translationText)

            layout.addView(headerLayout)
            layout.addView(divider)
            layout.addView(scrollView)
            container.addView(layout)

            var dX = 0f
            var dY = 0f
            container.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dX = layoutParams.x - event.rawX
                        dY = layoutParams.y - event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = (event.rawX + dX).toInt()
                        layoutParams.y = (event.rawY + dY).toInt()
                        windowManager.updateViewLayout(container, layoutParams)
                        true
                    }
                    else -> false
                }
            }

            overlayView = container
            windowManager.addView(container, layoutParams)
            isShowing = true
            Timber.d("Translation overlay shown")
        } catch (e: Exception) {
            Timber.e(e, "Failed to show translation overlay")
        }
    }

    fun updateTranslation(original: String, translated: String, targetLang: String) {
        lastTranslatedText = translated
        lastTargetLang = targetLang
        translationText?.post {
            translationText?.text = "[$targetLang] $translated\n\n───────────────\n[Original] $original"
            speakButton?.isEnabled = true
        }
    }

    fun showLoading() {
        translationText?.post {
            translationText?.text = "🔄 Traduzindo..."
            speakButton?.isEnabled = false
        }
    }

    private fun speakTranslation() {
        if (lastTranslatedText.isBlank()) return
        try {
            val locale = when (lastTargetLang.lowercase()) {
                "pt" -> Locale("pt", "BR")
                "en" -> Locale.ENGLISH
                "es" -> Locale("es", "ES")
                "fr" -> Locale.FRENCH
                "de" -> Locale.GERMAN
                "it" -> Locale.ITALIAN
                "ja" -> Locale.JAPANESE
                "ko" -> Locale.KOREAN
                "zh" -> Locale.CHINESE
                "ru" -> Locale("ru", "RU")
                else -> Locale.ENGLISH
            }

            tts?.language = locale

            if (isSpeaking) {
                tts?.stop()
                isSpeaking = false
                speakButton?.text = "🔊 Ouvir"
            } else {
                tts?.speak(lastTranslatedText, TextToSpeech.QUEUE_FLUSH, null, "translation_tts")
                isSpeaking = true
                speakButton?.text = "⏹ Parar"

                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        speakButton?.post {
                            isSpeaking = false
                            speakButton?.text = "🔊 Ouvir"
                        }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        speakButton?.post {
                            isSpeaking = false
                            speakButton?.text = "🔊 Ouvir"
                        }
                    }
                })
            }
        } catch (e: Exception) {
            Timber.e(e, "TTS speak error")
        }
    }

    fun hide() {
        try {
            tts?.stop()
            tts?.shutdown()
            overlayView?.let {
                windowManager.removeView(it)
                overlayView = null
            }
            isShowing = false
        } catch (e: Exception) {
            Timber.e(e, "Failed to hide translation overlay")
        }
    }

    fun isVisible() = isShowing
}
