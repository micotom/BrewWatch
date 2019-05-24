package com.funglejunk.brewwatch.model

object Constants {

    sealed class ServiceAction(val method: String) {
        object Start : ServiceAction("com.funglejunk.brewwatch.START_ACTION")
        object Pause : ServiceAction("com.funglejunk.brewwatch.PAUSE_ACTION")
        object Reset : ServiceAction("com.funglejunk.brewwatch.RESET_ACTION")
    }

    const val SERVICE_ID = 3283
    const val NOTIFICATION_CHANNEL_ID = "com.funglejunk.brewwatch.NOT_CHANNEL_ID"

    const val PREFS_STATE_KEY = "com.funglejunk.brewwatch.PREFS_STATE_KEY"
    const val PREFS_DURATION_KEY = "com.funglejunk.brewwatch.PREFS_DURATION_KEY"

    const val WAKE_LOCK_TAG = "com.funglejunk.brewwatch:WAKE_LOCK"

}