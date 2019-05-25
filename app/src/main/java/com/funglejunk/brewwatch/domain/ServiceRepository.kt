package com.funglejunk.brewwatch.domain

import com.funglejunk.brewwatch.domain.persistence.PersistenceRepo
import com.funglejunk.brewwatch.domain.time.Timer
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.time.Duration

object ServiceRepository {

    val timerDurationSubject = PublishSubject.create<Duration>()

    val timerDurationObservable: Observable<Duration> = timerDurationSubject.hide()

    val timerStateSubject = BehaviorSubject.create<Timer.TimerState>()

    val timerStateObservable: Observable<Timer.TimerState> = timerStateSubject.hide()

    fun initFromRepo(persistenceRepo: PersistenceRepo): Single<Pair<Timer.TimerState, Duration>> {
        return persistenceRepo.retrieveState()
            .doOnEvent { state, _ ->
                timerStateSubject.onNext(state)
            }
            .flatMap {
                Single.just(it).zipWith(
                    persistenceRepo.retrieveDuration().doOnEvent { duration, _ ->
                        timerDurationSubject.onNext(duration)
                    },
                    BiFunction<Timer.TimerState, Duration, Pair<Timer.TimerState, Duration>> {
                        state, duration -> state to duration
                    }
                )
            }
    }

}