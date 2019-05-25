package com.funglejunk.brewwatch.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.funglejunk.brewwatch.domain.ServiceRepository
import com.funglejunk.brewwatch.domain.persistence.PersistenceRepo
import com.funglejunk.brewwatch.domain.time.Timer
import io.reactivex.disposables.CompositeDisposable
import java.time.Duration

class TimerViewModel : ViewModel(), TimerViewModelInterface {

    private var isInitializedFromRepo = false

    override val currentTimerState: LiveData<Timer.TimerState> = MutableLiveData(
        Timer.TimerState.Idle)

    override val currentDuration: LiveData<Duration> = MutableLiveData()

    private val repositoryDisposables = CompositeDisposable()

    init {
        repositoryDisposables.addAll(
            ServiceRepository.timerDurationObservable.subscribe {
                updateTime(it)
            },
            ServiceRepository.timerStateObservable.subscribe {
                updateTimerState(it)
            }
        )
    }

    override fun initFromPersistence(persistenceRepo: PersistenceRepo) {
        if (!isInitializedFromRepo) {
            repositoryDisposables.add(
                ServiceRepository.initFromRepo(persistenceRepo).subscribe { (state, duration) ->
                    updateTimerState(state)
                    updateTime(duration)
                    isInitializedFromRepo = true
                }
            )
        }
    }

    override fun onCleared() {
        repositoryDisposables.dispose()
        super.onCleared()
    }

    override fun updateTime(duration: Duration) {
        (currentDuration as MutableLiveData).postValue(duration)
    }

    override fun updateTimerState(state: Timer.TimerState) {
        (currentTimerState as MutableLiveData).postValue(state)
    }

}