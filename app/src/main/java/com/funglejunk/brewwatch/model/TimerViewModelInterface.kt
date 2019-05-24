package com.funglejunk.brewwatch.model

import androidx.lifecycle.LiveData
import com.funglejunk.brewwatch.domain.PersistenceRepo
import com.funglejunk.brewwatch.domain.Timer
import java.time.Duration

interface TimerViewModelInterface {

    val currentDuration: LiveData<Duration>

    val currentTimerState: LiveData<Timer.TimerState>

    fun initFromPersistence(persistenceRepo: PersistenceRepo)

    fun updateTime(duration: Duration)

    fun updateTimerState(state: Timer.TimerState)

}