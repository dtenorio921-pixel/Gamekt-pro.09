package app.gamenative.gamefixes

import android.content.Context
import app.gamenative.data.GameSource
import com.winlator.box86_64.Box86_64Preset
import com.winlator.container.Container
import com.winlator.fexcore.FEXCorePreset
import timber.log.Timber

/**
 * Automatic per-game performance optimizer.
 *
 * Applies optimal container settings for known games based on their weight/requirements.
 * Designed for ARM64 devices with 4 GB RAM (e.g. Moto G35 / Snapdragon 6 Gen 3 + Adreno 710).
 *
 * Settings are applied once when the game container is first prepared, and only when
 * the field still holds its factory default value (i.e. the user hasn't changed it manually).
 */
object AutoGameOptimizer {

    private const val TAG = "AutoGameOptimizer"

    enum class GameWeight { LIGHT, MEDIUM, HEAVY, EXTREME }

    /**
     * A performance profile for a specific game.
     *
     * @param weight        Rough weight class — used to decide resolution and FPS cap.
     * @param screenSize    Override resolution. null = keep the container default (1280x720).
     * @param fpsCap        Target FPS cap injected into DXVK_FRAME_RATE. null = keep default.
     * @param box64Preset   Box64 dynarec preset to use.
     * @param fexcorePreset FEXCore emulator preset to use.
     * @param extraEnvVars  Additional env-var key/value pairs merged into the container env vars.
     * @param dxvkOverrides Key/value pairs to patch inside the DXVK wrapper config string.
     * @param description   Human-readable game name (for logs and the web dashboard).
     */
    data class GameProfile(
        val weight: GameWeight,
        val screenSize: String? = null,
        val fpsCap: Int? = null,
        val box64Preset: String = Box86_64Preset.PERFORMANCE,
        val fexcorePreset: String = FEXCorePreset.PERFORMANCE,
        val extraEnvVars: Map<String, String> = emptyMap(),
        val dxvkOverrides: Map<String, String> = emptyMap(),
        val description: String = "",
    )

    // ── Steam profiles  (AppID → GameProfile) ──────────────────────────────────────

    val steamProfiles: Map<String, GameProfile> = mapOf(

        // ── Light (DX8/DX9, low VRAM) ─────────────────────────────────────────────
        "70"      to GameProfile(GameWeight.LIGHT,  description = "Half-Life"),
        "220"     to GameProfile(GameWeight.LIGHT,  description = "Half-Life 2"),
        "240"     to GameProfile(GameWeight.LIGHT,  description = "Counter-Strike: Source"),
        "400"     to GameProfile(GameWeight.LIGHT,  description = "Portal"),
        "620"     to GameProfile(GameWeight.LIGHT,  description = "Portal 2"),
        "4000"    to GameProfile(GameWeight.LIGHT,  description = "Garry's Mod"),

        // ── Medium (DX11, moderate GPU) ────────────────────────────────────────────
        "550"     to GameProfile(
            weight = GameWeight.MEDIUM,
            description = "Left 4 Dead 2",
            fpsCap = 60,
            extraEnvVars = mapOf("DXVK_FRAME_RATE" to "60"),
        ),
        "570"     to GameProfile(
            weight = GameWeight.MEDIUM,
            description = "Dota 2",
            fpsCap = 60,
            extraEnvVars = mapOf("DXVK_FRAME_RATE" to "60"),
        ),
        "730"     to GameProfile(
            weight = GameWeight.MEDIUM,
            description = "CS:GO / CS2",
            fpsCap = 60,
            extraEnvVars = mapOf("DXVK_FRAME_RATE" to "60"),
        ),
        "8930"    to GameProfile(
            weight = GameWeight.MEDIUM,
            description = "Civilization V",
        ),
        "49520"   to GameProfile(
            weight = GameWeight.MEDIUM,
            description = "Borderlands 2",
        ),

        // ── Heavy (DX11, high GPU demand) ──────────────────────────────────────────
        "107410"  to GameProfile(
            weight = GameWeight.HEAVY,
            screenSize = "960x540",
            fpsCap = 30,
            description = "Arma 3",
            extraEnvVars = mapOf("DXVK_FRAME_RATE" to "30"),
            dxvkOverrides = mapOf("framerate" to "30"),
        ),
        "271590"  to GameProfile(
            weight = GameWeight.HEAVY,
            screenSize = "960x540",
            fpsCap = 30,
            description = "Grand Theft Auto V",
            extraEnvVars = mapOf("DXVK_FRAME_RATE" to "30"),
            dxvkOverrides = mapOf("videoMemorySize" to "1024", "framerate" to "30"),
        ),
        "292030"  to GameProfile(
            weight = GameWeight.HEAVY,
            screenSize = "960x540",
            fpsCap = 30,
            description = "The Witcher 3: Wild Hunt",
            extraEnvVars = mapOf("DXVK_FRAME_RATE" to "30"),
            dxvkOverrides = mapOf("videoMemorySize" to "1024", "framerate" to "30"),
        ),
        "374320"  to GameProfile(
            weight = GameWeight.HEAVY,
            screenSize = "960x540",
            fpsCap = 30,
            description = "Dark Souls III",
            extraEnvVars = mapOf("DXVK_FRAME_RATE" to "30"),
            dxvkOverrides = mapOf("framerate" to "30"),
        ),
        "379720"  to GameProfile(
            weight = GameWeight.HEAVY,
            screenSize = "960x540",
            fpsCap = 30,
            description = "DOOM (2016)",
            extraEnvVars = mapOf("DXVK_FRAME_RATE" to "30"),
        ),
        "524220"  to GameProfile(
            weight = GameWeight.HEAVY,
            screenSize = "960x540",
            fpsCap = 30,
            description = "Nioh: Complete Edition",
            extraEnvVars = mapOf("DXVK_FRAME_RATE" to "30"),
        ),
        "578080"  to GameProfile(
            weight = GameWeight.HEAVY,
            screenSize = "960x540",
            fpsCap = 30,
            description = "PUBG: Battlegrounds",
            extraEnvVars = mapOf("DXVK_FRAME_RATE" to "30"),
            dxvkOverrides = mapOf("framerate" to "30"),
        ),
        "582010"  to GameProfile(
            weight = GameWeight.HEAVY,
            screenSize = "960x540",
            fpsCap = 30,
            description = "Monster Hunter: World",
            extraEnvVars = mapOf("DXVK_FRAME_RATE" to "30"),
            dxvkOverrides = mapOf("videoMemorySize" to "1024", "framerate" to "30"),
        ),
        "814380"  to GameProfile(
            weight = GameWeight.HEAVY,
            screenSize = "960x540",
            fpsCap = 30,
            description = "Sekiro: Shadows Die Twice",
            extraEnvVars = mapOf("DXVK_FRAME_RATE" to "30"),
        ),

        // ── Extreme (DX12 / Vulkan, very high demand) ──────────────────────────────
        "1091500" to GameProfile(
            weight = GameWeight.EXTREME,
            screenSize = "800x450",
            fpsCap = 30,
            description = "Cyberpunk 2077",
            extraEnvVars = mapOf("DXVK_FRAME_RATE" to "30"),
            dxvkOverrides = mapOf("videoMemorySize" to "1024", "framerate" to "30", "vkd3dLevel" to "12_0"),
        ),
        "1174180" to GameProfile(
            weight = GameWeight.EXTREME,
            screenSize = "800x450",
            fpsCap = 30,
            description = "Red Dead Redemption 2",
            extraEnvVars = mapOf("DXVK_FRAME_RATE" to "30"),
            dxvkOverrides = mapOf("videoMemorySize" to "1024", "framerate" to "30"),
        ),
        "1245620" to GameProfile(
            weight = GameWeight.EXTREME,
            screenSize = "960x540",
            fpsCap = 30,
            description = "Elden Ring",
            extraEnvVars = mapOf("DXVK_FRAME_RATE" to "30"),
            dxvkOverrides = mapOf("videoMemorySize" to "1024", "framerate" to "30"),
        ),
        "1293830" to GameProfile(
            weight = GameWeight.EXTREME,
            screenSize = "800x450",
            fpsCap = 30,
            description = "Forza Horizon 4",
            extraEnvVars = mapOf("DXVK_FRAME_RATE" to "30"),
            dxvkOverrides = mapOf("videoMemorySize" to "1024", "framerate" to "30"),
        ),
        "1551360" to GameProfile(
            weight = GameWeight.EXTREME,
            screenSize = "800x450",
            fpsCap = 30,
            description = "Forza Horizon 5",
            extraEnvVars = mapOf("DXVK_FRAME_RATE" to "30"),
            dxvkOverrides = mapOf("videoMemorySize" to "1024", "framerate" to "30"),
        ),
        "1517290" to GameProfile(
            weight = GameWeight.EXTREME,
            screenSize = "960x540",
            fpsCap = 30,
            description = "Battlefield 2042",
            extraEnvVars = mapOf("DXVK_FRAME_RATE" to "30"),
            dxvkOverrides = mapOf("videoMemorySize" to "1024", "framerate" to "30"),
        ),
        "1086940" to GameProfile(
            weight = GameWeight.EXTREME,
            screenSize = "960x540",
            fpsCap = 30,
            description = "Baldur's Gate 3",
            extraEnvVars = mapOf("DXVK_FRAME_RATE" to "30"),
            dxvkOverrides = mapOf("videoMemorySize" to "1024", "framerate" to "30"),
        ),
        "1716740" to GameProfile(
            weight = GameWeight.EXTREME,
            screenSize = "800x450",
            fpsCap = 30,
            description = "God of War",
            extraEnvVars = mapOf("DXVK_FRAME_RATE" to "30"),
            dxvkOverrides = mapOf("videoMemorySize" to "1024", "framerate" to "30"),
        ),
        "1259420" to GameProfile(
            weight = GameWeight.EXTREME,
            screenSize = "800x450",
            fpsCap = 30,
            description = "Call of Duty: Modern Warfare II",
            extraEnvVars = mapOf("DXVK_FRAME_RATE" to "30"),
            dxvkOverrides = mapOf("videoMemorySize" to "1024", "framerate" to "30"),
        ),
    )

    // ── Apply logic ──────────────────────────────────────────────────────────────────

    /**
     * Looks up [gameId] in the Steam profile table and applies the matching profile
     * to [container].  Returns true when a profile was found and applied.
     */
    fun applyForSteam(gameId: String, container: Container, context: Context): Boolean {
        val profile = steamProfiles[gameId] ?: run {
            Timber.tag(TAG).d("No auto-optimizer profile for Steam game $gameId")
            return false
        }

        Timber.tag(TAG).i(
            "Applying ${profile.weight} auto-optimizer profile for '${profile.description}' (appId=$gameId)"
        )

        applyProfile(profile, container)
        return true
    }

    private fun applyProfile(profile: GameProfile, container: Container) {
        // ── Resolution ──────────────────────────────────────────────────────────────
        profile.screenSize?.let { size ->
            if (container.screenSize == Container.DEFAULT_SCREEN_SIZE) {
                container.screenSize = size
                Timber.tag(TAG).d("screenSize → $size")
            }
        }

        // ── Box64 preset ────────────────────────────────────────────────────────────
        val currentB64 = container.box64Preset ?: Box86_64Preset.COMPATIBILITY
        if (currentB64 == Box86_64Preset.COMPATIBILITY || currentB64 == Box86_64Preset.INTERMEDIATE) {
            container.setBox64Preset(profile.box64Preset)
            Timber.tag(TAG).d("box64Preset → ${profile.box64Preset}")
        }

        // ── Box86 preset ────────────────────────────────────────────────────────────
        val currentB86 = container.box86Preset ?: Box86_64Preset.COMPATIBILITY
        if (currentB86 == Box86_64Preset.COMPATIBILITY || currentB86 == Box86_64Preset.INTERMEDIATE) {
            container.setBox86Preset(profile.box64Preset)
            Timber.tag(TAG).d("box86Preset → ${profile.box64Preset}")
        }

        // ── FEXCore preset ──────────────────────────────────────────────────────────
        val currentFex = container.getFEXCorePreset() ?: FEXCorePreset.COMPATIBILITY
        if (currentFex == FEXCorePreset.COMPATIBILITY || currentFex == FEXCorePreset.INTERMEDIATE) {
            container.setFEXCorePreset(profile.fexcorePreset)
            Timber.tag(TAG).d("fexcorePreset → ${profile.fexcorePreset}")
        }

        // ── Environment variables (merge) ───────────────────────────────────────────
        if (profile.extraEnvVars.isNotEmpty()) {
            val envMap = parseSpaceSeparatedKV(container.envVars).toMutableMap()
            profile.extraEnvVars.forEach { (k, v) -> envMap[k] = v }
            container.envVars = envMap.entries.joinToString(" ") { "${it.key}=${it.value}" }
            Timber.tag(TAG).d("envVars updated with ${profile.extraEnvVars}")
        }

        // ── DXVK wrapper config (patch specific keys) ───────────────────────────────
        if (profile.dxvkOverrides.isNotEmpty()) {
            val dxvkMap = parseCommaSeparatedKV(container.dxWrapperConfig).toMutableMap()
            profile.dxvkOverrides.forEach { (k, v) -> dxvkMap[k] = v }
            container.setDXWrapperConfig(dxvkMap.entries.joinToString(",") { "${it.key}=${it.value}" })
            Timber.tag(TAG).d("dxwrapperConfig patched with ${profile.dxvkOverrides}")
        }

        container.saveData()
        Timber.tag(TAG).i("Container saved with auto-optimizer settings (${profile.weight})")
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────────

    private fun parseSpaceSeparatedKV(str: String): Map<String, String> =
        str.trim().split(" ").mapNotNull { token ->
            val idx = token.indexOf('=')
            if (idx > 0) token.substring(0, idx) to token.substring(idx + 1) else null
        }.toMap()

    private fun parseCommaSeparatedKV(str: String): Map<String, String> =
        str.trim().split(",").mapNotNull { token ->
            val idx = token.indexOf('=')
            if (idx > 0) token.substring(0, idx) to token.substring(idx + 1) else null
        }.toMap()
}
