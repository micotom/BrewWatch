package com.funglejunk.brewwatch.domain.service

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ServiceModel {

    interface ModelCategory<T> {

        val observable: Observable<T>

        fun send(content: T)

    }

    val command = object :
        ModelCategory<ServiceCommand> {

        private val subject = PublishSubject.create<ServiceCommand>()

        override val observable: Observable<ServiceCommand> = subject.hide()

        override fun send(content: ServiceCommand) {
            subject.onNext(content)
        }

    }

    val viewUpdate = object :
        ModelCategory<ServiceViewUpdate> {

        private val subject = PublishSubject.create<ServiceViewUpdate>()

        override val observable: Observable<ServiceViewUpdate> = subject.hide()

        override fun send(content: ServiceViewUpdate) {
            subject.onNext(content)
        }

    }

}