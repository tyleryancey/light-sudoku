package dev.tyler.sudoku.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import dev.tyler.sudoku.data.DateKeys
import dev.tyler.sudoku.ui.archive.ArchiveScreen
import dev.tyler.sudoku.ui.game.GameResult
import dev.tyler.sudoku.ui.game.GameScreen
import dev.tyler.sudoku.ui.theme.LocalSudokuPalette
import dev.tyler.sudoku.ui.theme.SudokuSurface

/**
 * Second line of the home tagline. A taste call — change [TaglineIndex] to try another.
 *
 * Keep each under ~60 characters: that wraps to at most two lines at 14sp in 280dp, which
 * is what the screen's height budget assumes (~353dp of the LP3's ~389dp content box).
 * Keep them free of claims a setting can falsify, too — the previous copy promised "no
 * clock pressure" and a "quiet" game, both of which Show timer and Play sound on solve
 * turn into lies.
 */
private val Taglines = listOf(
    "Nobody wins. You just finish, and that's lovely.",
    "Somewhere in there, a 7 is waiting for you.",
    "Nine numbers, endlessly rearranged. That's the whole trick.",
    "Eighty-one squares. No notifications.",
)
private const val TaglineIndex = 0

@InitialScreen
class HomeScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    private fun openGame(difficulty: String) {
        val today = DateKeys.today(System.currentTimeMillis())
        navigateTo({ sa -> GameScreen(sa, today, difficulty) }) { result ->
            if (result == GameResult.OpenArchive) openArchive()
        }
    }

    private fun openArchive() {
        navigateTo({ sa -> ArchiveScreen(sa) })
    }

    @Composable
    override fun Content() {
        SudokuSurface {
            val pal = LocalSudokuPalette.current
            // Budgeted to ~353dp of the LP3's ~389dp content box (413dp panel, zero insets,
            // less 24dp of vertical padding) so nothing scrolls at font_scale 1. The
            // "CHOOSE YOUR PUZZLE" header that used to sit above the buttons is gone —
            // three labelled buttons don't need announcing, and it bought back ~23dp.
            // verticalScroll stays as a safety net for larger font scales; the fix for a
            // screen that scrolls is shorter content, not a deleted modifier.
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Sudoku", color = pal.txt, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Numbers, in boxes.",
                    color = pal.txt, fontSize = 18.sp, textAlign = TextAlign.Center,
                )
                Text(
                    Taglines[TaglineIndex],
                    color = pal.txtDim, fontSize = 14.sp, textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 2.dp).widthIn(max = 280.dp),
                )
                Spacer(Modifier.height(12.dp))

                listOf("easy" to "Easy", "medium" to "Medium", "hard" to "Hard").forEach { (key, label) ->
                    Box(
                        Modifier.fillMaxWidth().widthIn(max = 300.dp).padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(34.dp))
                            .border(2.dp, pal.frame, RoundedCornerShape(34.dp))
                            .clickable { openGame(key) }
                            .height(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, color = pal.txt, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    DateKeys.today(System.currentTimeMillis()),
                    color = pal.txt, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Past puzzles", color = pal.txtDim, fontSize = 15.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .clickable { openArchive() }.padding(8.dp),
                )
            }
        }
    }
}
