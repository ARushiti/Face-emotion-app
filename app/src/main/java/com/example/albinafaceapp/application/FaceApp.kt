package com.example.albinafaceapp.application

import android.app.Application


class FaceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this

    }

    companion object {
        lateinit var instance: FaceApp
    }
}