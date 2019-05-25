package com.funglejunk.brewwatch.domain.service

interface ServiceControllerInterface {

    fun onNewIntentAction(action: String)

    fun dispose()

}