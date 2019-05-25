package com.funglejunk.brewwatch.domain.persistence

import com.funglejunk.brewwatch.domain.time.Timer
import io.reactivex.Single
import java.time.Duration

interface PersistenceRepo {

    fun persistDuration(duration: Duration): Single<Long>

    fun persistState(state: Timer.TimerState): Single<String>

    fun retrieveDuration(): Single<Duration>

    fun retrieveState(): Single<Timer.TimerState>

}