package com.funglejunk.brewwatch.domain.time

import android.os.Handler
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.time.Duration

class Timer {

    companion object {
        const val TIME_NONE = 0L
    }

    sealed class TimerState {
        object Idle : TimerState()
        object Running : TimerState()
        object Paused : TimerState()
    }

    interface Ticker {
        val onTick: (Long) -> Unit
        fun start()
        fun stop()
    }

    class HandlerTicker(private val intervalMs: Long, override val onTick: (Long) -> Unit) :
        Ticker {

        private val handler = Handler()

        private val tickRunnable = object: Runnable {
            override fun run() {
                onTick(intervalMs)
                handler.postDelayed(this, intervalMs)
            }
        }

        override fun start() {
            handler.postDelayed(tickRunnable, intervalMs)
        }

        override fun stop() {
            handler.removeCallbacks(tickRunnable)
        }

    }

    private var currentTimeMs = TIME_NONE

    private val currentTimeSubject = PublishSubject.create<Duration>()
    val elapsedTimeObservable: Observable<Duration> = currentTimeSubject.hide()

    private var ticker: Ticker? = null

    fun withTicker(f: Timer.() -> Ticker): Timer {
        ticker = this.f()
        return this
    }

    fun start(persistedState: TimerState, persistedElapsedTime: Long) {
        currentTimeMs = persistedElapsedTime
        currentTimeSubject.onNext(Duration.ofMillis(currentTimeMs))
        when (persistedState) {
            TimerState.Idle, TimerState.Paused -> startTicker()
            TimerState.Running -> throw IllegalStateException()
        }
    }

    fun pause(currentState: TimerState): Long {
        return when (currentState) {
            TimerState.Running -> {
                pauseTicker()
                currentTimeMs
            }
            TimerState.Idle, TimerState.Paused -> throw IllegalStateException()
        }
    }

    fun reset() {
        resetTicker()
        currentTimeMs = TIME_NONE
        currentTimeSubject.onNext(Duration.ofMillis(currentTimeMs))
    }

    private fun startTicker() {
        ticker?.start() ?: {
            Timber.e("Missing ticker instance in timer")
        }()
    }

    private fun pauseTicker() {
        ticker?.stop() ?: {
            Timber.e("Missing ticker instance in timer")
        }()
    }

    private fun resetTicker() {
        ticker?.stop() ?: {
            Timber.e("Missing ticker instance in timer")
        }()
    }

    fun onTick(elapsedTime: Long) {
        currentTimeMs += elapsedTime
        currentTimeSubject.onNext(Duration.ofMillis(currentTimeMs))
    }

}