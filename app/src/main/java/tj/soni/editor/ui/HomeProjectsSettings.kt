package tj.soni.editor.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import tj.soni.editor.R
import tj.soni.editor.data.ProjectEntity

@Composable
fun HomeScreen(vm: AppViewModel) {
    val projects by vm.projects.collectAsState()
    var showNew by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.home_tagline), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = { showNew = true }, Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.new_project))
        }
        OutlinedButton(onClick = { vm.opened = null; vm.tab = 1 }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.import_media)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { vm.tab = 1 }, Modifier.weight(1f)) { Text(stringResource(R.string.nav_video)) }
            FilledTonalButton(onClick = { vm.tab = 2 }, Modifier.weight(1f)) { Text(stringResource(R.string.nav_photo)) }
            FilledTonalButton(onClick = { vm.tab = 4 }, Modifier.weight(1f)) { Text(stringResource(R.string.nav_audio)) }
        }
        Text(stringResource(R.string.recent_projects), style = MaterialTheme.typography.titleMedium)
        if (projects.isEmpty()) Text(stringResource(R.string.no_projects))
        projects.take(3).forEach { p ->
            Card(Modifier.fillMaxWidth().clickable { vm.opened = p; vm.tab = 1 }) {
                Text(p.name, Modifier.padding(16.dp))
            }
        }
        Text(stringResource(R.string.offline_note), style = MaterialTheme.typography.bodySmall)
    }
    if (showNew) NameDialog(stringResource(R.string.new_project), "", { vm.createAndOpen(it); showNew = false }, { showNew = false })
}

@Composable
fun ProjectsScreen(vm: AppViewModel) {
    val projects by vm.projects.collectAsState()
    var renaming by remember { mutableStateOf<ProjectEntity?>(null) }
    if (projects.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_projects)) }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(projects, key = { it.id }) { p ->
            Card(Modifier.fillMaxWidth().clickable { vm.opened = p; vm.tab = 1 }) {
                Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name, Modifier.weight(1f))
                    IconButton(onClick = { renaming = p }) { Icon(Icons.Default.Edit, stringResource(R.string.rename)) }
                    IconButton(onClick = { vm.duplicate(p) }) { Icon(Icons.Default.Add, stringResource(R.string.duplicate)) }
                    IconButton(onClick = { vm.delete(p) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                }
            }
        }
    }
    renaming?.let { p -> NameDialog(stringResource(R.string.rename), p.name, { vm.rename(p, it); renaming = null }, { renaming = null }) }
}

@Composable
fun SettingsScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
        listOf("tg" to R.string.lang_tg, "ru" to R.string.lang_ru, "en" to R.string.lang_en).forEach { (tag, label) ->
            OutlinedButton(
                onClick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag)) },
                Modifier.fillMaxWidth()
            ) { Text(stringResource(label)) }
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.offline_note), style = MaterialTheme.typography.bodySmall)
        Text("v1.0.0", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun AiScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.nav_ai), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) { Text(stringResource(R.string.ai_unavailable), Modifier.padding(16.dp)) }
    }
}
