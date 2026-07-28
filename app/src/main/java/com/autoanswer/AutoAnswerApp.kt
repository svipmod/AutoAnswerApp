package com.autoanswer

import android.app.Application
import com.autoanswer.util.PreferenceManager

class AutoAnswerApp : Application() {
    lateinit var preferenceManager: PreferenceManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferenceManager = PreferenceManager(this)
    }

    companion object {
        lateinit var instance: AutoAnswerApp
            private set
    }
}
