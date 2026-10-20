package com.example.ui.savings

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.SavingsEntryEntity
import com.example.data.local.entity.SavingsItemEntity
import com.example.data.savings.MarketRates
import com.example.data.savings.SavingsDetails
import com.example.data.savings.SavingsRepository
import com.example.data.savings.SavingsStatus
import com.example.data.savings.SavingsSummary
import com.example.data.savings.SavingsValuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavingsViewModelFactory(
    private val repository: SavingsRepository,
    private val prefs: SharedPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavingsViewModel::class.java)) {
            return SavingsViewModel(repository, prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

/**
 * State for the Savings tab. The page filters (holder, statuses, grouping, collapsed sections)
 * are remembered in [prefs] so the page opens the way the user left it.
 */
class SavingsViewModel(
    private val repository: SavingsRepository,
    private val prefs: SharedPreferences
) : ViewModel() {

    val items: StateFlow<List<SavingsItemEntity>> =
        repository.items.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val entriesByItem: StateFlow<Map<Long, List<SavingsEntryEntity>>> =
        repository.entries.map { list -> list.groupBy { it.itemId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val holders: StateFlow<List<String>> =
        repository.holders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _rates = MutableStateFlow(MarketRates())
    val rates: StateFlow<MarketRates> = _rates.asStateFlow()

    private val _holderFilter = MutableStateFlow(prefs.getString(KEY_HOLDER, null))
    val holderFilter: StateFlow<String?> = _holderFilter.asStateFlow()

    private val _statusFilter = MutableStateFlow(
        prefs.getStringSet(KEY_STATUSES, null)?.map { SavingsStatus.from(it) }?.toSet() ?: setOf(SavingsStatus.ACTIVE)
    )
    val statusFilter: StateFlow<Set<SavingsStatus>> = _statusFilter.asStateFlow()

    private val _groupByHolder = MutableStateFlow(prefs.getBoolean(KEY_GROUP_BY_HOLDER, false))
    val groupByHolder: StateFlow<Boolean> = _groupByHolder.asStateFlow()

    private val _collapsed = MutableStateFlow(prefs.getStringSet(KEY_COLLAPSED, emptySet())!!.toSet())
    val collapsedGroups: StateFlow<Set<String>> = _collapsed.asStateFlow()

    /** Records after the holder and status filters; everything on the page is built from these. */
    val visibleItems: StateFlow<List<SavingsItemEntity>> =
        combine(items, _holderFilter, _statusFilter) { all, holder, statuses -> SavingsSummary.filter(all, holder, statuses) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _undo = MutableStateFlow<SavingsRepository.DeletedItem?>(null)
    val undo: StateFlow<SavingsRepository.DeletedItem?> = _undo.asStateFlow()

    init {
        // FD and RD values grow every day; bring the cached values up to today.
        viewModelScope.launch {
            repository.refreshAll()
            _rates.value = repository.rates()
        }
    }

    fun setHolderFilter(holder: String?) {
        _holderFilter.value = holder
        prefs.edit { putString(KEY_HOLDER, holder) }
    }

    fun toggleStatus(status: SavingsStatus) {
        val next = _statusFilter.value.let { if (status in it) it - status else it + status }
        if (next.isEmpty()) return // at least one status stays selected
        _statusFilter.value = next
        prefs.edit { putStringSet(KEY_STATUSES, next.map { it.name }.toSet()) }
    }

    fun setGroupByHolder(enabled: Boolean) {
        _groupByHolder.value = enabled
        prefs.edit { putBoolean(KEY_GROUP_BY_HOLDER, enabled) }
    }

    fun toggleCollapsed(key: String) {
        val next = _collapsed.value.let { if (key in it) it - key else it + key }
        _collapsed.value = next
        prefs.edit { putStringSet(KEY_COLLAPSED, next) }
    }

    /** Adds a new record (with its suggested history) or saves edits to an existing one. */
    fun saveItem(item: SavingsItemEntity, details: SavingsDetails, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = if (item.id == 0L) {
                repository.addItem(item, details, SavingsValuation.suggestedEntries(details, item.startDate, now), now)
            } else {
                repository.updateItem(item, details, now)
                item.id
            }
            onSaved(id)
        }
    }

    fun deleteItem(item: SavingsItemEntity) {
        viewModelScope.launch { _undo.value = repository.deleteItemForUndo(item) }
    }

    fun undoDelete(deleted: SavingsRepository.DeletedItem) {
        viewModelScope.launch { repository.restore(deleted) }
    }

    fun clearUndo() {
        _undo.value = null
    }

    fun saveEntry(entry: SavingsEntryEntity) {
        viewModelScope.launch {
            if (entry.id == 0L) repository.addEntry(entry) else repository.updateEntry(entry)
        }
    }

    fun deleteEntry(entry: SavingsEntryEntity) {
        viewModelScope.launch { repository.deleteEntry(entry) }
    }

    fun setRate(key: String, value: Double) {
        viewModelScope.launch {
            repository.setRate(key, value)
            _rates.value = repository.rates()
        }
    }

    fun renameHolder(oldName: String, newName: String) {
        viewModelScope.launch {
            repository.renameHolder(oldName, newName)
            if (_holderFilter.value.equals(oldName, ignoreCase = true)) setHolderFilter(newName.trim())
        }
    }

    companion object {
        const val PREFS_NAME = "savings_prefs"
        private const val KEY_HOLDER = "holder_filter"
        private const val KEY_STATUSES = "status_filter"
        private const val KEY_GROUP_BY_HOLDER = "group_by_holder"
        private const val KEY_COLLAPSED = "collapsed_groups"
    }
}
