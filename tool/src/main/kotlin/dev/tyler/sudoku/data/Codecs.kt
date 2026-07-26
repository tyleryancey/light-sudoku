package dev.tyler.sudoku.data

import dev.tyler.sudoku.engine.SudokuEngine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** All settings toggles. Defaults ALL OFF, matching the prototype. */
data class Settings(
    val rowcol: Boolean = false,
    val same: Boolean = false,
    val checkOnEntry: Boolean = false,
    val timer: Boolean = false,
    val sound: Boolean = false,
    /** Preferred floating-keypad margin — the enum name of the UI's KeypadDock (TOP/BOTTOM/LEFT/RIGHT).
     *  Stored as a String so this data-layer type stays independent of the UI enum. */
    val keypadMargin: String = "BOTTOM",
)

/**
 * The stored shape. [v] stays at 2 even though box/conflicts/autoStart/plain were dropped in v1.4:
 * [Codecs.decodeSettings] treats any other version as a full reset, so bumping it would silently
 * wipe every existing install's timer/sound/keypadMargin. The retired keys still present in older
 * blobs are skipped by the decoder's ignoreUnknownKeys instead.
 */
@Serializable
internal data class SettingsDto(
    @SerialName("__v") val v: Int = 2,
    val rowcol: Boolean = false, val same: Boolean = false,
    val checkOnEntry: Boolean = false, val timer: Boolean = false,
    val sound: Boolean = false,
    // New in v1.3; defaulted so older __v=2 blobs (without it) still decode to BOTTOM, not a reset.
    val keypadMargin: String = "BOTTOM",
)

/**
 * Read-only view of the one retired key that still has to influence decoding.
 *
 * Plain mode was a master override: while it was on, the board drew no peer tint, no
 * identical-number tint and no check dots, whatever the individual flags said. Dropping the
 * setting without looking at it would hand those users the opposite of what they had — a grid
 * that suddenly lights up — so [Codecs.decodeSettings] reads `plain` from the stored blob one
 * last time and clears the flags it used to mask. Kept out of [SettingsDto] deliberately: this
 * is decode-only, and re-emitting the key would resurrect a setting that no longer exists.
 */
@Serializable
private data class RetiredSettingsDto(val plain: Boolean = false)

/**
 * Per-puzzle progress; field names v/c/l/a/t/s/r carry over from the prototype.
 * ca/cr are the auto-candidate diff layers (added/removed bits over the live auto set);
 * they default to empty so progress saved before this feature still decodes.
 */
@Serializable
data class ProgressDto(
    val v: List<Int>, val c: List<Int>, val l: List<Int>,
    val a: Int, val t: Int, val s: Int, val r: Int,
    val ca: List<Int> = emptyList(), val cr: List<Int> = emptyList(),
)

@Serializable
internal data class PuzzleDto(
    val p: List<Int>, val s: List<Int>,
    val clues: Int, val maxTier: Int, val solv: Boolean,
)

@Serializable
data class IndexEntry(val status: String, val time: Int? = null)

object Codecs {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encodeSettings(s: Settings): String = json.encodeToString(
        SettingsDto(
            rowcol = s.rowcol, same = s.same, checkOnEntry = s.checkOnEntry,
            timer = s.timer, sound = s.sound, keypadMargin = s.keypadMargin,
        )
    )

    fun decodeSettings(raw: String?): Settings {
        if (raw == null) return Settings()
        val dto = runCatching { json.decodeFromString<SettingsDto>(raw) }.getOrNull()
            ?: return Settings()
        if (dto.v != 2) return Settings()
        // One-shot plain-mode migration: a blob still carrying plain=true describes a player who
        // was seeing a bare grid, so honour what was on screen rather than what the masked flags
        // said. Self-retiring — the first settings write re-encodes without `plain`, after which
        // this is a no-op and the toggles behave normally again.
        val wasPlain = runCatching { json.decodeFromString<RetiredSettingsDto>(raw) }.getOrNull()?.plain == true
        return Settings(
            rowcol = dto.rowcol && !wasPlain,
            same = dto.same && !wasPlain,
            checkOnEntry = dto.checkOnEntry && !wasPlain,
            timer = dto.timer, sound = dto.sound, keypadMargin = dto.keypadMargin,
        )
    }

    fun encodeProgress(p: ProgressDto): String = json.encodeToString(p)

    fun decodeProgress(raw: String?): ProgressDto? =
        raw?.let { runCatching { json.decodeFromString<ProgressDto>(it) }.getOrNull() }

    fun encodePuzzle(p: SudokuEngine.Puzzle): String = json.encodeToString(
        PuzzleDto(p.puzzle.toList(), p.solution.toList(), p.clues, p.maxTier, p.solvableLogically)
    )

    fun decodePuzzle(raw: String?): SudokuEngine.Puzzle? {
        val dto = raw?.let { runCatching { json.decodeFromString<PuzzleDto>(it) }.getOrNull() }
            ?: return null
        if (dto.p.size != 81 || dto.s.size != 81) return null
        return SudokuEngine.Puzzle(
            dto.p.toIntArray(), dto.s.toIntArray(), dto.clues, dto.maxTier, dto.solv
        )
    }

    fun encodeIndex(ix: Map<String, IndexEntry>): String = json.encodeToString(ix)

    fun decodeIndex(raw: String?): Map<String, IndexEntry> =
        raw?.let { runCatching { json.decodeFromString<Map<String, IndexEntry>>(it) }.getOrNull() }
            ?: emptyMap()
}
