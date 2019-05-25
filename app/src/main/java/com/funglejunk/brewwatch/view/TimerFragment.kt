package com.funglejunk.brewwatch.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.funglejunk.brewwatch.R
import com.funglejunk.brewwatch.domain.persistence.SharedPrefRepo
import com.funglejunk.brewwatch.domain.time.TimeFormat
import com.funglejunk.brewwatch.domain.time.Timer
import com.funglejunk.brewwatch.domain.service.TimerService
import com.funglejunk.brewwatch.model.Constants
import com.funglejunk.brewwatch.model.TimerDurationViewModelConsumer
import com.funglejunk.brewwatch.model.TimerStateViewModelConsumer
import com.funglejunk.brewwatch.model.TimerViewModel
import com.robinhood.ticker.TickerUtils
import kotlinx.android.synthetic.main.timer_fragment.*
import timber.log.Timber
import java.time.Duration

class TimerFragment : Fragment() {

    private lateinit var viewModel: ViewModel
    private val blinkAnimator = BlinkAnimator(500L)

    private val durationObserver = object : TimerDurationViewModelConsumer {

        override fun onTimerDurationUpdate(duration: Duration) {
            hours_ticker.text = with(TimeFormat.Hours) {
                format(duration)
            }
            minutes_ticker.text = with(TimeFormat.Minutes) {
                format(duration)
            }
            seconds_ticker.text = with(TimeFormat.Seconds) {
                format(duration)
            }
            millis_ticker.text = with(TimeFormat.Millis) {
                format(duration)
            }
            val durationInMillis = duration.toMillis()
            val timePassed = durationInMillis / 1000 % 60
            progress_circle.setCurrentProgress(timePassed.toDouble())
        }

        override fun onTimeDurationUpdateNull() {
            Timber.e("time update was null")
        }

    }

    private val stateObserver = object : TimerStateViewModelConsumer {

        override fun onTimerStateUpdate(state: Timer.TimerState) {
            updateButtonState(state)
            when (state) {
                Timer.TimerState.Paused -> blinkAnimator.startAnimation(ticker_layout)
                else -> blinkAnimator.stopAnimation(ticker_layout)
            }
        }

        override fun onTimerStateUpdateNull() {
            Timber.e("timer state was null")
        }

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initializeViewModel()
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProviders.of(this).get(TimerViewModel::class.java)
            .also {
                it.currentDuration.observe(viewLifecycleOwner, durationObserver)
            }
            .also {
                it.currentTimerState.observe(viewLifecycleOwner, stateObserver)
            }
            .apply {
                context?.let {
                    initFromPersistence(SharedPrefRepo(it))
                } ?: {
                    Timber.e("Error initializing service repo: Context is null!")
                }()
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.timer_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeTickerViews()
        initializeViewEventHandlers()
        progress_circle.maxProgress = 60.0
    }

    private fun initializeTickerViews() {
        with(TickerUtils.provideNumberList()) {
            hours_ticker.setCharacterLists(this)
            minutes_ticker.setCharacterLists(this)
            seconds_ticker.setCharacterLists(this)
        }
        with(AccelerateInterpolator()) {
            hours_ticker.animationInterpolator = this
            minutes_ticker.animationInterpolator = this
            seconds_ticker.animationInterpolator = this
        }
    }

    private fun initializeViewEventHandlers() {
        start_button.setOnClickListener { startButtonPressed() }
        pause_button.setOnClickListener { pauseButtonPressed() }
        reset_button.setOnClickListener { resetButtonPressed() }
    }

    private fun startButtonPressed() {
        sendServiceIntent(Constants.ServiceAction.Start)
    }

    private fun pauseButtonPressed() {
        sendServiceIntent(Constants.ServiceAction.Pause)
    }

    private fun resetButtonPressed() {
        sendServiceIntent(Constants.ServiceAction.Reset)
    }

    private fun sendServiceIntent(serviceAction: Constants.ServiceAction) {
        val serviceIntent = Intent(activity, TimerService::class.java).apply {
            action = serviceAction.method
        }
        @Suppress("MoveVariableDeclarationIntoWhen")
        val serviceWillStop =
            serviceAction == Constants.ServiceAction.Pause || serviceAction == Constants.ServiceAction.Reset
        val onActivityNull = {
            Timber.e("Error forwarding service intent, param in between is null")
        }
        when (serviceWillStop) {
            true -> activity?.startService(serviceIntent) ?: onActivityNull()
            false -> activity?.startForegroundService(serviceIntent) ?: onActivityNull()
        }
    }

    private fun updateButtonState(newState: Timer.TimerState) {
        when (newState) {
            Timer.TimerState.Idle -> {
                start_button.visibility = View.VISIBLE
                pause_button.visibility = View.GONE
            }
            Timer.TimerState.Running -> {
                start_button.visibility = View.GONE
                pause_button.visibility = View.VISIBLE
            }
            Timer.TimerState.Paused -> {
                start_button.visibility = View.VISIBLE
                pause_button.visibility = View.GONE
            }
        }
    }

}