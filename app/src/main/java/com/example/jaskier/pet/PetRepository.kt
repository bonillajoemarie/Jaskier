package com.example.jaskier.pet

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.petDataStore: DataStore<Preferences> by preferencesDataStore("pet_state")

class PetRepository(context: Context) {

    private val dataStore = context.applicationContext.petDataStore

    val stats: Flow<PetStats> = dataStore.data.map { it.toPetStats() }

    suspend fun refresh(now: Long) = update(now) { it }

    suspend fun feed(now: Long) = update(now) { it.fed() }

    suspend fun shower(now: Long) = update(now) { it.showered() }

    suspend fun brush(now: Long) = update(now) { it.brushed() }

    suspend fun heal(now: Long) = update(now) { it.medicined() }

    // Decay-then-transform in one atomic edit so concurrent actions can't
    // apply stale stats.
    private suspend fun update(now: Long, transform: (PetStats) -> PetStats) {
        dataStore.edit { prefs ->
            val current = prefs.toPetStats(defaultTimestamp = now)
            prefs.write(transform(current.decayedTo(now)))
        }
    }

    private fun Preferences.toPetStats(defaultTimestamp: Long = 0L) = PetStats(
        hunger = this[KEY_HUNGER] ?: STAT_MAX,
        cleanliness = this[KEY_CLEANLINESS] ?: STAT_MAX,
        teeth = this[KEY_TEETH] ?: STAT_MAX,
        lastUpdatedMillis = this[KEY_LAST_UPDATED] ?: defaultTimestamp,
    )

    private fun MutablePreferences.write(stats: PetStats) {
        this[KEY_HUNGER] = stats.hunger
        this[KEY_CLEANLINESS] = stats.cleanliness
        this[KEY_TEETH] = stats.teeth
        this[KEY_LAST_UPDATED] = stats.lastUpdatedMillis
    }

    private companion object {
        val KEY_HUNGER = floatPreferencesKey("hunger")
        val KEY_CLEANLINESS = floatPreferencesKey("cleanliness")
        val KEY_TEETH = floatPreferencesKey("teeth")
        val KEY_LAST_UPDATED = longPreferencesKey("last_updated_ms")
    }
}
