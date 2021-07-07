package com.karel.slmmobile

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import org.jetbrains.anko.defaultSharedPreferences
import kotlin.reflect.KClass
import kotlin.system.exitProcess

/**
 * Created by Karel on 28.03.2018.
 */
class main : Application(){
    companion object {
        var ssh : SSHService = SSHService()

        var serviceConnected = false
        var disconnectService = false
        var restart = false

    }

    override fun onCreate() {
        super.onCreate()

        defaultSharedPreferences.getBoolean("firstStart", true).apply {
            when{
                true -> {
                    defaultSharedPreferences.edit().putString("appliedTheme","Default").apply()
                    setTheme(R.style.lightDefault)
                }
            }
        }

        //val intent = Intent(this, SSHService::class.java)
        //startService(intent)
        //bindService(intent, SSHServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onTerminate() {

        main.ssh.close()

        defaultSharedPreferences.getBoolean("firstStart", true).apply {
            when{
                false -> {
                    defaultSharedPreferences.edit().putBoolean("firstStart",false).apply()
                }
            }
        }

        super.onTerminate()
    }

    /*private val SSHServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as SSHService.BinderOperator
            ssh = binder.getService()
            serviceConnected = true
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            serviceConnected = false
        }
    }*/
}