package com.funglejunk.brewwatch.domain.service

import com.funglejunk.brewwatch.domain.time.Timer
import java.time.Duration

sealed class ServiceViewUpdate {

    data class NewState(val state: Timer.TimerState) : ServiceViewUpdate()

    data class NewDuration(val duration: Duration) : ServiceViewUpdate()

}