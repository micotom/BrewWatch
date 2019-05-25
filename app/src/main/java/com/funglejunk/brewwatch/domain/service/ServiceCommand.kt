package com.funglejunk.brewwatch.domain.service

sealed class ServiceCommand {

    object AcquireWakeLock : ServiceCommand()

    object ReleaseWakeLock : ServiceCommand()

    object StopSelf : ServiceCommand()

}