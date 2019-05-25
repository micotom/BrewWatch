package com.funglejunk.brewwatch.domain.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.funglejunk.brewwatch.domain.ServiceRepository
import com.funglejunk.brewwatch.domain.persistence.SharedPrefRepo
import com.funglejunk.brewwatch.model.Constants
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import timber.log.Timber

class TimerService : Service() {

    companion object {
        const val TIMER_INTERVAL_MS = 62L
    }

    private val disposables = CompositeDisposable()

    private val wakeLock: PowerManager.WakeLock by lazy {
        (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constants.WAKE_LOCK_TAG)
        }
    }

    private val serviceModel = ServiceModel()

    private val serviceController: ServiceControllerInterface by lazy {
        val persistenceRepo = SharedPrefRepo(this)
        ServiceController(serviceModel, persistenceRepo)
    }

    override fun onBind(intent: Intent?): IBinder? = throw UnsupportedOperationException()

    override fun onDestroy() {
        disposables.clear()
        serviceController.dispose()
        Timber.d("timer service destroyed")
        super.onDestroy()
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(Constants.SERVICE_ID, createNotification())

        disposables.addAll(
            createViewUpdateSubscription(),
            createCommandSubscription()
        )

        Timber.d("timer service created")
    }

    private fun createCommandSubscription(): Disposable? {
        return serviceModel.command.observable.subscribe { command ->
            when (command) {
                ServiceCommand.AcquireWakeLock -> acquireWakeLock()
                ServiceCommand.ReleaseWakeLock -> releaseWakeLock()
                ServiceCommand.StopSelf -> stopSelf()
            }
        }
    }

    private fun createViewUpdateSubscription(): Disposable? {
        return serviceModel.viewUpdate.observable.subscribe { update ->
            when (update) {
                is ServiceViewUpdate.NewState -> ServiceRepository.timerStateSubject.onNext(update.state)
                is ServiceViewUpdate.NewDuration -> ServiceRepository.timerDurationSubject.onNext(update.duration)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("timer service on start command with intent: $intent.")
        intent?.let {
            when (val action = intent.action) {
                null -> Timber.e("Service intent or action was null.")
                else -> handleIntent(action)
            }
        } ?: {
            Timber.e("Null intent in service.")
        }()
        return START_STICKY
    }

    private fun handleIntent(action: String) {
        serviceController.onNewIntentAction(action)
    }

    private fun acquireWakeLock() {
        wakeLock.acquireSafely()
    }

    private fun releaseWakeLock() {
        wakeLock.releaseSafely()
    }

    @SuppressLint("WakelockTimeout")
    private fun PowerManager.WakeLock.acquireSafely() {
        if (!isHeld) {
            Timber.d("acquiring wake lock")
            acquire()
        }
    }

    private fun PowerManager.WakeLock.releaseSafely() {
        if (isHeld) {
            release()
        }
    }

    private fun createNotification() =
        NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            "Brew Watch",
            NotificationManager.IMPORTANCE_DEFAULT
        ).also {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(it)
        }.run {
            NotificationCompat.Builder(this@TimerService, Constants.NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Brew Watch")
                .setContentText("Text")
                .build()
        }


}