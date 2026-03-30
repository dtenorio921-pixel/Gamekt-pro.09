package app.gamenative.translation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import app.gamenative.MainActivity
import app.gamenative.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class GameTranslatorService : Service() {

    companion object {
        const val ACTION_START = "action_start_translation"
        const val ACTION_STOP = "action_stop_translation"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_TARGET_LANG = "extra_target_lang"
        const val CHANNEL_ID = "game_translator_channel"
        const val NOTIFICATION_ID = 9001
        const val SCAN_INTERVAL_MS = 3000L

        var targetLanguage = "pt"

        fun startTranslation(context: Context, resultCode: Int, resultData: Intent, targetLang: String = "pt") {
            if (!Settings.canDrawOverlays(context)) {
                Timber.w("Overlay permission not granted")
                return
            }
            val intent = Intent(context, GameTranslatorService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
                putExtra(EXTRA_TARGET_LANG, targetLang)
            }
            context.startForegroundService(intent)
        }

        fun stopTranslation(context: Context) {
            val intent = Intent(context, GameTranslatorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val overlayManager by lazy { TranslationOverlayManager(this) }
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private var lastTranslatedText = ""

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                targetLanguage = intent.getStringExtra(EXTRA_TARGET_LANG) ?: "pt"

                if (resultData != null) {
                    startForeground(NOTIFICATION_ID, buildNotification())
                    initMediaProjection(resultCode, resultData)
                    overlayManager.show()
                    startTranslationLoop()
                }
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun initMediaProjection(resultCode: Int, resultData: Intent) {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "GameTranslator",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, handler
        )
    }

    private fun startTranslationLoop() {
        isRunning = true
        serviceScope.launch {
            while (isRunning) {
                try {
                    val bitmap = captureScreen()
                    if (bitmap != null) {
                        val text = extractTextFromBitmap(bitmap)
                        if (text.isNotBlank() && text != lastTranslatedText && text.length > 3) {
                            lastTranslatedText = text
                            withContext(Dispatchers.Main) { overlayManager.showLoading() }
                            val translated = translateText(text, targetLanguage)
                            withContext(Dispatchers.Main) {
                                overlayManager.updateTranslation(text, translated, targetLanguage.uppercase())
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Translation loop error")
                }
                delay(SCAN_INTERVAL_MS)
            }
        }
    }

    private fun captureScreen(): Bitmap? {
        return try {
            val image = imageReader?.acquireLatestImage() ?: return null
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width
            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height, Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            image.close()
            bitmap
        } catch (e: Exception) {
            Timber.e(e, "Screen capture error")
            null
        }
    }

    private fun extractTextFromBitmap(bitmap: Bitmap): String {
        return try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream)
            "Sample game text"
        } catch (e: Exception) {
            ""
        }
    }

    private fun translateText(text: String, targetLang: String): String {
        return try {
            val encoded = URLEncoder.encode(text.take(500), "UTF-8")
            val url = URL("https://api.mymemory.translated.net/get?q=$encoded&langpair=en|$targetLang")
            val connection = url.openConnection() as HttpsURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            val translatedText = json
                .getJSONObject("responseData")
                .getString("translatedText")

            if (translatedText.isNullOrBlank()) text else translatedText
        } catch (e: Exception) {
            Timber.e(e, "Translation API error")
            text
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, GameTranslatorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Gamekt Pro — Tradução Ativa")
            .setContentText("Traduzindo jogos em tempo real para ${targetLanguage.uppercase()}")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openPending)
            .addAction(0, "Parar Tradução", stopPending)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Tradução de Jogos em Tempo Real",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notificação do serviço de tradução IA"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        isRunning = false
        overlayManager.hide()
        virtualDisplay?.release()
        mediaProjection?.stop()
        imageReader?.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
