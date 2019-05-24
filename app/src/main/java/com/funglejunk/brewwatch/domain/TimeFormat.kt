package com.funglejunk.brewwatch.domain

import java.time.Duration

sealed class TimeFormat {

    abstract fun format(duration: Duration): String

    object Millis : TimeFormat() {
        private const val timeFormat = "%03d"

        override fun format(duration: Duration): String {
            val durationInMillis = duration.toMillis()
            val millis = durationInMillis % 1000
            return String.format(timeFormat, millis)
        }
    }

    object Seconds : TimeFormat() {
        private const val timeFormat = "%02d"

        override fun format(duration: Duration): String {
            val durationInMillis = duration.toMillis()
            val second = durationInMillis / 1000 % 60
            return String.format(timeFormat, second)
        }
    }

    object Minutes : TimeFormat() {
        private const val timeFormat = "%02d"

        override fun format(duration: Duration): String {
            val durationInMillis = duration.toMillis()
            val minute = durationInMillis / (1000 * 60) % 60
            return String.format(timeFormat, minute)
        }
    }

    object Hours : TimeFormat() {
        private const val timeFormat = "%02d"

        override fun format(duration: Duration): String {
            val durationInMillis = duration.toMillis()
            val hour = durationInMillis / (1000 * 60 * 60) % 24
            return String.format(timeFormat, hour)
        }
    }

}