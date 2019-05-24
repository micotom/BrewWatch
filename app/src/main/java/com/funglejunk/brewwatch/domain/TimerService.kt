package com.funglejunk.brewwatch.domain

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.funglejunk.brewwatch.model.Constants
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.time.Duration

class TimerService : Service() {

    private companion object {
        const val TIMER_INTERVAL_MS = 62L
    }

    private val timer = Timer().withTicker {
        Timer.HandlerTicker(TIMER_INTERVAL_MS) {
            onTick(it)
        }
    }

    private val disposables = CompositeDisposable()

    private val persistenceRepo: PersistenceRepo by lazy {
        SharedPrefRepo(this)
    }

    private val wakeLock: PowerManager.WakeLock by lazy {
        (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constants.WAKE_LOCK_TAG)
        }
    }

    private val foregroundNotification: Notification by lazy {
        NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            "Brew Watch",
            NotificationManager.IMPORTANCE_DEFAULT
        ).also {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(it)
        }
        NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Brew Watch")
            .setContentText("Text")
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = throw UnsupportedOperationException()

    override fun onCreate() {
        super.onCreate()
        startForeground(Constants.SERVICE_ID, foregroundNotification)
        disposables.addAll(
            timer.elapsedTimeObservable.subscribeOn(Schedulers.computation()).subscribe {
                ServiceRepository.timerDurationSubject.onNext(it)
            }
        )
        Timber.d("timer service created")
    }

    override fun onDestroy() {
        disposables.clear()
        Timber.d("timer service destroyed")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("timer service on start command with intent: $intent")
        intent?.let {
            when (val action = intent.action) {
                null -> Timber.e("Service intent or action was null.")
                else -> handleIntent(action)
            }
        } ?: {
            Timber.e("No offset key given in service intent")
        }()
        return START_STICKY
    }

    private fun handleIntent(action: String) {
        when (action) {
            Constants.ServiceAction.Start.method -> onStartTimerActionReceived()
            Constants.ServiceAction.Pause.method -> onPauseTimerActionReceived()
            Constants.ServiceAction.Reset.method -> onResetTimerActionReceived()
        }
    }

    private fun onStartTimerActionReceived() {
        wakeLock.acquireSafely()
        persistenceRepo.retrieveState().flatMap {
            it.zipToPairWith(persistenceRepo.retrieveDuration())
        }.doOnEvent { (state, duration), _ ->
            startTimer(state, duration)
        }.subscribeSafely()
    }


    private fun startTimer(state: Timer.TimerState, duration: Duration) {
        timer.start(state, duration.toMillis())
        ServiceRepository.timerStateSubject.onNext(Timer.TimerState.Running)
    }

    private fun onPauseTimerActionReceived() {
        wakeLock.releaseSafely()
        pauseTimer().flatMap { elapsedTime ->
            persistenceRepo.persistDuration(Duration.ofMillis(elapsedTime)).doOnEvent { _, _ ->
                persistenceRepo.persistState(Timer.TimerState.Paused)
            }.doOnEvent { _, _ ->
                stopSelf()
            }
        }.subscribeSafely()
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
        wakeLock.releaseSafely()
        resetTimer()
        persistenceRepo.persistDuration(Duration.ofMillis(Timer.TIME_NONE)).flatMap {
            persistenceRepo.persistState(Timer.TimerState.Idle)
        }.doOnEvent { _, _ -> stopSelf() }.subscribeSafely()
    }

    private fun resetTimer() {
        timer.reset()
        ServiceRepository.timerStateSubject.onNext(Timer.TimerState.Idle)
    }

    @SuppressLint("WakelockTimeout")
    private fun PowerManager.WakeLock.acquireSafely() {
        if (!isHeld) {
            acquire()
        }
    }

    private fun PowerManager.WakeLock.releaseSafely() {
        if (isHeld) {
            release()
        }
    }

    private fun <T> Single<T>.subscribeSafely() {
        disposables.add(subscribe())
    }

    private fun <T, V> T.zipToPairWith(other: Single<V>): Single<Pair<T, V>> {
        return Single.just(this).zipWith(other, BiFunction<T, V, Pair<T, V>> {
            t, v -> Pair(t, v)
        })
    }

}