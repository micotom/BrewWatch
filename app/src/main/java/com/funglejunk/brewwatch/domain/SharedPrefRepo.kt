package com.funglejunk.brewwatch.domain

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.funglejunk.brewwatch.model.Constants
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.time.Duration

class SharedPrefRepo(context: Context) : PersistenceRepo {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    override fun persistDuration(duration: Duration): Single<Long> {
        return Single.fromCallable {
            val persistedValue = duration.toMillis()
            prefs.persist {
                putLong(Constants.PREFS_DURATION_KEY, persistedValue)
            }
            persistedValue
        }.subscribeOn(Schedulers.io())
    }

    override fun persistState(state: Timer.TimerState): Single<String> {
        return Single.fromCallable {
            val persistedState = state.javaClass.simpleName
            prefs.persist {
                putString(Constants.PREFS_STATE_KEY, persistedState)
            }
            persistedState
        }.subscribeOn(Schedulers.io())
    }

    override fun retrieveDuration(): Single<Duration> {
        return Single.fromCallable {
            prefs.getLong(Constants.PREFS_DURATION_KEY, Timer.TIME_NONE)
        }.map {
            Duration.ofMillis(it)
        }.subscribeOn(Schedulers.io())
    }

    override fun retrieveState(): Single<Timer.TimerState> {
        return Single.fromCallable {
            prefs.getString(Constants.PREFS_STATE_KEY, Timer.TimerState.Idle.javaClass.simpleName)
        }.map {
            when (it) {
                Timer.TimerState.Idle.javaClass.simpleName -> Timer.TimerState.Idle
                Timer.TimerState.Paused.javaClass.simpleName -> Timer.TimerState.Paused
                Timer.TimerState.Running.javaClass.simpleName -> Timer.TimerState.Running
                else -> throw IllegalArgumentException()
            }
        }.subscribeOn(Schedulers.io())
    }

    private fun SharedPreferences.persist(f: SharedPreferences.Editor.() -> SharedPreferences.Editor) =
        edit().f().apply()

}