package com.herdmanager.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom test runner that launches the app with [HiltTestApplication]
 * so the test APK gets Hilt's test component. Use in defaultConfig.testInstrumentationRunner.
 */
class HerdManagerHiltTestRunner : AndroidJUnitRunner() {

    override fun newApplication(cl: ClassLoader?, appName: String?, context: Context?): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
