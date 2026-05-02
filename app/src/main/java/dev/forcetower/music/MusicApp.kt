package dev.forcetower.music

import android.app.Application
import timber.log.Timber

class MusicApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}