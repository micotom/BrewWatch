package com.funglejunk.brewwatch.model

import androidx.lifecycle.Observer
import java.time.Duration

interface TimerDurationViewModelConsumer : Observer<Duration> {

    override fun onChanged(duration: Duration?) {
        duration?.let { onTimerDurationUpdate(it) } ?: onTimeDurationUpdateNull()
    }

    fun onTimerDurationUpdate(duration: Duration)

    fun onTimeDurationUpdateNull()

}