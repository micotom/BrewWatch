package com.funglejunk.brewwatch.model

import androidx.lifecycle.Observer
import com.funglejunk.brewwatch.domain.Timer

interface TimerStateViewModelConsumer : Observer<Timer.TimerState> {

    override fun onChanged(state: Timer.TimerState?) {
        state?.let { onTimerStateUpdate(it) } ?: onTimerStateUpdateNull()
    }

    fun onTimerStateUpdate(state: Timer.TimerState)

    fun onTimerStateUpdateNull()

}