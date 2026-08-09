package com.example.jaskier.pet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PetUiState(
    val hunger: Float = STAT_MAX,
    val cleanliness: Float = STAT_MAX,
    val teeth: Float = STAT_MAX,
    val mood: PetMood = PetMood.HAPPY,
)

enum class PetEvent { FED, SHOWERED, BRUSHED, HEALED }

class PetViewModel(
    private val repository: PetRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    val uiState: StateFlow<PetUiState> = repository.stats
        .map {
            PetUiState(
                hunger = it.hunger,
                cleanliness = it.cleanliness,
                teeth = it.teeth,
                mood = it.mood,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetUiState())

    private val _events = MutableSharedFlow<PetEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<PetEvent> = _events

    init {
        viewModelScope.launch {
            while (isActive) {
                repository.refresh(clock())
                delay(TICK_MILLIS)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { repository.refresh(clock()) }
    }

    fun feed() = act(PetEvent.FED) { repository.feed(it) }

    fun shower() = act(PetEvent.SHOWERED) { repository.shower(it) }

    fun brush() = act(PetEvent.BRUSHED) { repository.brush(it) }

    fun heal() = act(PetEvent.HEALED) { repository.heal(it) }

    private fun act(event: PetEvent, action: suspend (Long) -> Unit) {
        viewModelScope.launch {
            action(clock())
            _events.emit(event)
        }
    }

    class Factory(private val repository: PetRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PetViewModel(repository) as T
    }

    private companion object {
        const val TICK_MILLIS = 30_000L
    }
}
