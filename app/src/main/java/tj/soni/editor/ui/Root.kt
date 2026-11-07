package tj.soni.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tj.soni.editor.R

private val SoniColors = darkColorScheme(
    primary = Color(0xFF8B6CFF),
    primaryContainer = Color(0xFF2B2154),
    background = Color(0xFF0E0F13),
    surface = Color(0xFF16181F),
    surfaceVariant = Color(0xFF1E212B)
)

@Composable
fun SoniRoot(vm: AppViewModel) {
    MaterialTheme(colorScheme = SoniColors) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = { SoniBottomBar(vm.tab) { vm.tab = it } }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (vm.tab) {
                    0 -> HomeScreen(vm)
                    1 -> VideoScreen(vm)
                    2 -> PhotoScreen()
                    3 -> MotionScreen()
                    4 -> AudioScreen()
                    5 -> AiScreen()
                    6 -> ProjectsScreen(vm)
                    else -> SettingsScreen()
                }
            }
        }
    }
}

@Composable
fun SoniBottomBar(sel: Int, onSel: (Int) -> Unit) {
    val items = listOf(
        Icons.Default.Home to R.string.nav_home,
        Icons.Default.PlayArrow to R.string.nav_video,
        Icons.Default.Edit to R.string.nav_photo,
        Icons.Default.Star to R.string.nav_motion,
        Icons.Default.Favorite to R.string.nav_audio,
        Icons.Default.Build to R.string.nav_ai,
        Icons.AutoMirrored.Filled.List to R.string.nav_projects,
        Icons.Default.Settings to R.string.nav_settings
    )
    Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.navigationBarsPadding().horizontalScroll(rememberScrollState()).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEachIndexed { i, (icon, label) ->
                Column(
                    Modifier.clip(RoundedCornerShape(14.dp))
                        .background(if (i == sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { onSel(i) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(icon, contentDescription = null)
                    Text(stringResource(label), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun NameDialog(title: String, initial: String, onOk: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, label = { Text(stringResource(R.string.project_name)) }) },
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onOk(text.trim()) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } }
    )
}
