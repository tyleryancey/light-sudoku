package dev.tyler.sudoku.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tyler.sudoku.data.Settings
import dev.tyler.sudoku.ui.theme.LocalSudokuPalette
import dev.tyler.sudoku.ui.theme.SudokuPalette

/**
 * All overlays are plain scrim boxes (no window Dialogs) so system back always
 * routes through LightActivity -> goBack() -> GameViewModel.onBackPressed().
 * Scrim tap closes, matching the prototype's outside-tap-dismiss.
 */
@Composable
fun GameOverlays(vm: GameViewModel, ui: GameUiState, onPastPuzzles: () -> Unit) {
    when (val ov = ui.overlay) {
        null -> {}
        Overlay.Menu -> BottomSheet(vm) { MenuMain(vm) }
        Overlay.HintPage -> BottomSheet(vm) { HintPage(vm) }
        Overlay.SettingsSheet -> CenterSheet(vm, scrollable = true) { SettingsSheet(vm, ui.settings) }
        Overlay.Help -> CenterSheet(vm, scrollable = true) { HelpSheet(vm) }
        Overlay.ConfirmReset -> CenterSheet(vm, scrollable = true) {
            ConfirmSheet(
                vm, "Reset puzzle",
                "This clears everything you have entered. The clues stay.",
            ) { vm.confirmReset() }
        }
        Overlay.ConfirmReveal -> CenterSheet(vm, scrollable = true) {
            ConfirmSheet(
                vm, "Reveal puzzle",
                "This fills in the whole solution and ends the puzzle.",
            ) { vm.confirmRevealPuzzle() }
        }
        is Overlay.Win -> CenterSheet(vm, scrollable = true) { WinSheet(vm, ov, onPastPuzzles) }
        Overlay.Paused -> PausedOverlay(vm)
    }
}

private val Scrim = Color(0x99000000)

@Composable
private fun BottomSheet(vm: GameViewModel, content: @Composable () -> Unit) {
    val pal = LocalSudokuPalette.current
    Box(Modifier.fillMaxSize().background(Scrim).clickable { vm.dismissOverlay() }) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(pal.btn)
                .clickable(enabled = false) {}   // swallow taps inside the sheet
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            // grabber
            Box(
                Modifier.align(Alignment.CenterHorizontally).width(36.dp).height(4.dp)
                    .clip(CircleShape).background(pal.btnLine)
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

// Every centered sheet opts into scrollable=true. Left at the default, overflow clips at BOTH ends
// at once (the Column is centered in a fillMaxSize Box) with no way to reach the hidden part.
@Composable
private fun CenterSheet(vm: GameViewModel, scrollable: Boolean = false, content: @Composable () -> Unit) {
    val pal = LocalSudokuPalette.current
    Box(
        Modifier.fillMaxSize().background(Scrim).clickable { vm.dismissOverlay() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 340.dp).fillMaxWidth().padding(16.dp)
                .clip(RoundedCornerShape(16.dp)).background(pal.btn)
                .clickable(enabled = false) {}
                .let { if (scrollable) it.verticalScroll(rememberScrollState()) else it }
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) { content() }
    }
}

@Composable
private fun SheetLabel(text: String) {
    val pal = LocalSudokuPalette.current
    Text(text.uppercase(), color = pal.txtDim, fontSize = 11.sp, letterSpacing = 2.sp,
        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
}

@Composable
private fun Tile(
    label: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    onClick: () -> Unit,
) {
    val pal = LocalSudokuPalette.current
    Column(
        modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(10.dp))
            .background(pal.bg).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label, color = pal.txt, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        if (caption != null) {
            Text(caption, color = pal.txtDim, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun MenuMain(vm: GameViewModel) {
    val pal = LocalSudokuPalette.current
    SheetLabel("Help")
    Tile("Hint") { vm.showHintPage() }
    // IntrinsicSize.Min so a label that wraps at a large font scale grows both tiles together,
    // the same equalization the sheet button pairs use.
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Tile("Check square", Modifier.weight(1f).fillMaxHeight()) { vm.checkCell() }
        Tile("Check puzzle", Modifier.weight(1f).fillMaxHeight()) { vm.checkPuzzle() }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(pal.btnLine))
    Spacer(Modifier.height(12.dp))
    SheetLabel("Reveal & reset")
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Tile("Reveal square", Modifier.weight(1f).fillMaxHeight()) { vm.revealCell() }
        Tile("Reveal puzzle", Modifier.weight(1f).fillMaxHeight()) { vm.requestRevealPuzzle() }
    }
    Tile("Reset puzzle") { vm.requestReset() }
}

@Composable
private fun HintPage(vm: GameViewModel) {
    val pal = LocalSudokuPalette.current
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).clickable { vm.showMenu() }.padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("‹", color = pal.txtDim, fontSize = 18.sp)
        Spacer(Modifier.width(6.dp))
        Text("Hint", color = pal.txtDim, fontSize = 14.sp)
    }
    Spacer(Modifier.height(10.dp))
    Tile("Point to a square", caption = "Highlight the next solvable square — you place the number") {
        vm.pointHint()
    }
    Tile("Fill in a square", caption = "Fill the next square in for you") { vm.fillHint() }
}

@Composable
private fun ToggleRow(label: String, on: Boolean, onToggle: () -> Unit) {
    val pal = LocalSudokuPalette.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onToggle)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = pal.txt, fontSize = 15.sp, modifier = Modifier.weight(1f))
        // pill toggle
        Box(
            Modifier.width(42.dp).height(24.dp).clip(RoundedCornerShape(12.dp))
                .background(if (on) pal.txt else pal.btnLine),
            contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(Modifier.padding(3.dp).size(18.dp).clip(CircleShape).background(pal.bg))
        }
    }
}

@Composable
private fun SettingsSheet(vm: GameViewModel, st: Settings) {
    val pal = LocalSudokuPalette.current
    Text("Settings", color = pal.txt, fontSize = 20.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp))
    // Five toggles, no section headers: with this few rows three headers cost ~60dp of a
    // 349dp sheet, and that was the difference between fitting the LP3's panel and
    // scrolling. The two highlight rows take back the "Highlight" prefix the old
    // "Highlighting" header used to supply.
    ToggleRow("Highlight row and column", on = st.rowcol) { vm.toggleSetting("rowcol") }
    ToggleRow("Highlight identical numbers", on = st.same) { vm.toggleSetting("same") }
    ToggleRow("Check guesses when entered", on = st.checkOnEntry) { vm.toggleSetting("checkOnEntry") }
    ToggleRow("Show timer", on = st.timer) { vm.toggleSetting("timer") }
    ToggleRow("Play sound on solve", on = st.sound) { vm.toggleSetting("sound") }
    Spacer(Modifier.height(8.dp))
    SolidButton("Done") { vm.dismissOverlay() }
}

@Composable
private fun HelpSheet(vm: GameViewModel) {
    val pal = LocalSudokuPalette.current
    Text("How to play", color = pal.txt, fontSize = 20.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp))
    // Short lines at 14sp/20sp: the old prose ran ~308dp against a ~254dp budget on the
    // LP3, so it always scrolled. Keep any edit here under ~11 lines.
    Text(
        "Fill every row, column, and 3×3 box with 1–9, no repeats.\n\n" +
            "Tap a square, then a number.\n" +
            "✎ toggles small pencil marks.\n" +
            "A fills those notes in for you.\n" +
            "↺ undoes. ▾ hides the keypad.\n" +
            "Flick the keypad to any edge.\n\n" +
            "The ⋯ menu can check, reveal, or reset.\n" +
            "New puzzles arrive each day.",
        color = pal.txtDim, fontSize = 14.sp, lineHeight = 20.sp,
    )
    Spacer(Modifier.height(16.dp))
    SolidButton("Got it") { vm.dismissOverlay() }
}

@Composable
private fun ConfirmSheet(vm: GameViewModel, title: String, message: String, onConfirm: () -> Unit) {
    val pal = LocalSudokuPalette.current
    Text(title, color = pal.txt, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text(message, color = pal.txtDim, fontSize = 15.sp, lineHeight = 21.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
    // IntrinsicSize.Min + fillMaxHeight: if either label wraps, both buttons grow together.
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GhostButton("Cancel", Modifier.weight(1f).fillMaxHeight(), PairedButtonPadding) {
            vm.dismissOverlay()
        }
        SolidButton(
            "Confirm", Modifier.weight(1f).fillMaxHeight(),
            fill = true, paddingH = PairedButtonPadding, onClick = onConfirm,
        )
    }
}

@Composable
private fun WinSheet(vm: GameViewModel, win: Overlay.Win, onPastPuzzles: () -> Unit) {
    val pal = LocalSudokuPalette.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Solved", color = pal.txt, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(win.subtitle, color = pal.txtDim, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
        Text(
            win.timeText, color = pal.txt, fontSize = 34.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 14.dp),
        )
        // "Past puzzles" wraps to two lines in its half-width slot where "Close" doesn't;
        // IntrinsicSize.Min sizes the row to the taller of the two and both fill it.
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GhostButton("Past puzzles", Modifier.weight(1f).fillMaxHeight(), PairedButtonPadding) {
                vm.dismissOverlay(); onPastPuzzles()
            }
            SolidButton(
                "Close", Modifier.weight(1f).fillMaxHeight(),
                fill = true, paddingH = PairedButtonPadding,
            ) { vm.dismissOverlay() }
        }
    }
}

@Composable
private fun PausedOverlay(vm: GameViewModel) {
    val pal = LocalSudokuPalette.current
    Column(
        Modifier.fillMaxSize().background(pal.bg).clickable { vm.dismissOverlay() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("PAUSED", color = pal.txtDim, fontSize = 14.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(10.dp))
        Text("Tap to resume", color = pal.txt, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    }
}

// Standalone buttons hug their label, so their horizontal padding is what gives them their shape.
// Paired buttons are weighted to half the row and don't need as much, and trimming it keeps a long
// label like "Past puzzles" on one line at the default font scale.
private val StandaloneButtonPadding = 20.dp
private val PairedButtonPadding = 14.dp

@Composable
private fun SolidButton(
    label: String,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
    paddingH: Dp = StandaloneButtonPadding,
    onClick: () -> Unit,
) {
    val pal = LocalSudokuPalette.current
    SheetButton(
        label, pal.txt, pal.bg,
        modifier.let { if (fill) it.fillMaxWidth() else it }, paddingH, onClick,
    )
}

@Composable
private fun GhostButton(
    label: String,
    modifier: Modifier = Modifier,
    paddingH: Dp = StandaloneButtonPadding,
    onClick: () -> Unit,
) {
    val pal = LocalSudokuPalette.current
    SheetButton(label, pal.bg, pal.txt, modifier.fillMaxWidth(), paddingH, onClick)
}

/**
 * A sheet button: a centered label on a filled tile.
 *
 * The [Box] matters. These used to be bare [Text]s, which meant a side-by-side pair
 * couldn't be equalized — a stretched Text draws at the top of its line box, and
 * textAlign only centers horizontally. So when one label wrapped ("Past puzzles" in the
 * win sheet) and its neighbour didn't, the two buttons rendered at different heights.
 * With a Box, the caller can hand both the same height (see the IntrinsicSize.Min rows
 * in [ConfirmSheet] and [WinSheet]) and the label stays centered in whatever it gets.
 */
@Composable
private fun SheetButton(
    label: String,
    bg: Color,
    ink: Color,
    modifier: Modifier,
    paddingH: Dp,
    onClick: () -> Unit,
) {
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(bg)
            .clickable(onClick = onClick).padding(horizontal = paddingH, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, color = ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}
