package com.funglejunk.brewwatch.domain.service

import com.funglejunk.brewwatch.domain.persistence.PersistenceRepo
import com.funglejunk.brewwatch.domain.ServiceRepository
import com.funglejunk.brewwatch.domain.time.Timer
import com.funglejunk.brewwatch.model.Constants
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import timber.log.Timber
import java.time.Duration

class ServiceController(private val serviceModel: ServiceModel, private val persistenceRepo: PersistenceRepo) :
    ServiceControllerInterface {

    private val disposables = CompositeDisposable()

    private val timer = Timer().withTicker {
        Timer.HandlerTicker(TimerService.TIMER_INTERVAL_MS) {
            onTick(it)
        }
    }.also {
        disposables.add(
            it.elapsedTimeObservable.subscribe {
                serviceModel.viewUpdate.send(ServiceViewUpdate.NewDuration(it))
            }
        )
    }

    override fun dispose() {
        disposables.dispose()
    }

    override fun onNewIntentAction(action: String) {
        Timber.d("action in controller: $action")
        when (action) {
            Constants.ServiceAction.Start.method -> onStartTimerActionReceived()
            Constants.ServiceAction.Pause.method -> onPauseTimerActionReceived()
            Constants.ServiceAction.Reset.method -> onResetTimerActionReceived()
            else -> throw IllegalArgumentException()
        }
    }

    private fun onStartTimerActionReceived() {
        serviceModel.command.send(ServiceCommand.AcquireWakeLock)
        getPersistedStateAndDuration()
            .doOnEvent { (state, duration), _ ->
                startTimer(state, duration)
            }
            .subscribeSafely()
    }

    private fun startTimer(state: Timer.TimerState, duration: Duration) {
        timer.start(state, duration.toMillis())
        serviceModel.viewUpdate.send(ServiceViewUpdate.NewState(Timer.TimerState.Running))
    }

    private fun onPauseTimerActionReceived() {
        serviceModel.command.send(ServiceCommand.ReleaseWakeLock)
        pauseTimer()
            .flatMap { elapsedTime ->
                persistStateAndDuration(Timer.TimerState.Paused, elapsedTime)
            }
            .doOnEvent { _, _ ->
                serviceModel.command.send(ServiceCommand.StopSelf)
            }
            .subscribeSafely()
    }

    private fun pauseTimer(): Single<Long> {
        return ServiceRepository.timerStateSubject.take(1).single(Timer.TimerState.Running)
            .map {
                timer.pause(it)
            }
            .doOnEvent { _, _ ->
                ServiceRepository.timerStateSubject.onNext(Timer.TimerState.Paused)
            }
    }

    private fun onResetTimerActionReceived() {
        serviceModel.command.send(ServiceCommand.ReleaseWakeLock)
        resetTimer()
        persistStateAndDuration(
            Timer.TimerState.Idle,
            Timer.TIME_NONE
        )
            .doOnEvent { _, _ ->
                serviceModel.command.send(ServiceCommand.StopSelf)
            }
            .subscribeSafely()
    }

    private fun resetTimer() {
        timer.reset()
        serviceModel.viewUpdate.send(ServiceViewUpdate.NewState(Timer.TimerState.Idle))
    }

    private fun getPersistedStateAndDuration() = persistenceRepo.retrieveState()
        .flatMap {
            it.zipToPairWith(persistenceRepo.retrieveDuration())
        }

    private fun persistStateAndDuration(state: Timer.TimerState, elapsedTime: Long): Single<String> {
        return persistenceRepo.persistDuration(Duration.ofMillis(elapsedTime))
            .flatMap {
                persistenceRepo.persistState(state)
            }
    }

    private fun <T, V> T.zipToPairWith(other: Single<V>): Single<Pair<T, V>> {
        return Single.just(this).zipWith(other, BiFunction<T, V, Pair<T, V>> { t, v ->
            Pair(t, v)
        })
    }

    private fun <T> Single<T>.subscribeSafely() {
        disposables.add(subscribe())
    }


}