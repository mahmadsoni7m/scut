package tj.soni.editor.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tj.soni.editor.data.AppDatabase
import tj.soni.editor.data.ProjectEntity

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).projectDao()
    val projects: StateFlow<List<ProjectEntity>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var tab by mutableStateOf(0)
    var opened by mutableStateOf<ProjectEntity?>(null)

    fun createAndOpen(name: String) {
        viewModelScope.launch {
            val p = ProjectEntity(name = name)
            val id = dao.insert(p)
            opened = p.copy(id = id)
            tab = 1
        }
    }

    fun create(name: String, data: String) {
        viewModelScope.launch { dao.insert(ProjectEntity(name = name, data = data)) }
    }

    fun saveData(p: ProjectEntity, data: String) {
        viewModelScope.launch { dao.update(p.copy(data = data, modifiedAt = System.currentTimeMillis())) }
    }

    fun rename(p: ProjectEntity, name: String) {
        viewModelScope.launch { dao.update(p.copy(name = name, modifiedAt = System.currentTimeMillis())) }
    }

    fun duplicate(p: ProjectEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.insert(p.copy(id = 0, name = p.name + " (2)", createdAt = now, modifiedAt = now))
        }
    }

    fun delete(p: ProjectEntity) {
        viewModelScope.launch { dao.delete(p) }
        if (opened?.id == p.id) opened = null
    }
}
