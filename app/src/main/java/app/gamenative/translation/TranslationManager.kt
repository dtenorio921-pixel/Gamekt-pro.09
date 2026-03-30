package app.gamenative.translation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import timber.log.Timber

object TranslationManager {

    const val REQUEST_MEDIA_PROJECTION = 1001
    var pendingResultCode: Int = -1
    var pendingResultData: Intent? = null
    var selectedLanguage: String = "pt"

    val supportedLanguages = mapOf(
        "Português" to "pt",
        "English" to "en",
        "Español" to "es",
        "Français" to "fr",
        "Deutsch" to "de",
        "Italiano" to "it",
        "日本語" to "ja",
        "한국어" to "ko",
        "中文" to "zh",
        "Русский" to "ru",
        "العربية" to "ar",
    )

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun requestOverlayPermission(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}")
        )
        activity.startActivity(intent)
    }

    fun requestScreenCapture(activity: Activity, launcher: ActivityResultLauncher<Intent>) {
        val projectionManager =
            activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        launcher.launch(projectionManager.createScreenCaptureIntent())
    }

    fun startTranslation(context: Context, resultCode: Int, resultData: Intent, language: String = selectedLanguage) {
        Timber.d("Starting game translation to: $language")
        GameTranslatorService.startTranslation(context, resultCode, resultData, language)
    }

    fun stopTranslation(context: Context) {
        Timber.d("Stopping game translation")
        GameTranslatorService.stopTranslation(context)
    }
}
