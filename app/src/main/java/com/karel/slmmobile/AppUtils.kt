package com.karel.slmmobile

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.util.TypedValue
import android.widget.TextView
import org.jetbrains.anko.defaultSharedPreferences
import kotlin.concurrent.thread

class AppUtils(val activity: Activity) {
    private val previousFolder = ArrayList<String>()
    private var path = ""

    var theme = ""

    fun vibrate(msec: Long, amplitude: Int) {
        val vib = activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26)
            vib.vibrate(VibrationEffect.createOneShot(msec, amplitude))
        else
            vib.vibrate(msec)
    }

    fun changeTheme() {
        activity.defaultSharedPreferences.getString("Theme", "Default").apply {
            when {
                contains("Default", true) || contains("Light Gray", true) -> {
                    activity.setTheme(R.style.lightDefault)
                    activity.defaultSharedPreferences.edit().putString("Theme", "Light Gray").apply()
                }
                contains("Light Red", true) -> {
                    activity.setTheme(R.style.redSoft)
                    activity.defaultSharedPreferences.edit().putString("Theme", "Light Red").apply()
                }
                contains("Light Blue", true) -> {
                    activity.setTheme(R.style.blueSoft)
                    activity.defaultSharedPreferences.edit().putString("Theme", "Light Blue").apply()
                }
                contains("Black White",true) -> {
                    activity.setTheme(R.style.whiteBlack)
                    activity.defaultSharedPreferences.edit().putString("Theme", "Black White").apply()
                }
            }

            theme = this
        }
    }

    fun pathSelector() : String {
        path = ""
        previousFolder.add("/")
        selectPopup(main.ssh.shellChannel("find / -maxdepth 1 -type d")
                .split("\n")
                .dropLastWhile { it.isBlank() }
                .dropWhile { it == previousFolder.last() }
                .toTypedArray()
                .plus("Select this")
                .plus("Go Back")
                .reversedArray())
        while(path == "");
        return path
    }

    private fun selectPopup(list: Array<String>) {
        Log.e("LIST", "$list")
        activity.runOnUiThread {
                val build = AlertDialog.Builder(activity)
                build.setTitle(previousFolder.last())
                        .setCancelable(true)
                        .setItems(list, DialogInterface.OnClickListener { _, which ->
                            if (list[which].contains("go back", true)) {
                                if (previousFolder.last() != "/") {
                                    previousFolder.remove(previousFolder.last())
                                    thread {
                                        selectPopup(main.ssh.shellChannel("find ${previousFolder.last()} -maxdepth 1 -type d")
                                                .split("\n")
                                                .dropLastWhile { it.isBlank() }
                                                .dropWhile { it == previousFolder.last() }
                                                .toTypedArray()
                                                .plus("Select this")
                                                .plus("Go Back")
                                                .reversedArray())
                                    }
                                    return@OnClickListener
                                }else{
                                    path = "drop"
                                }
                            } else if (list[which].contains("select this", true)) {
                                path = previousFolder.last()
                                previousFolder.clear()
                            } else {
                                previousFolder.add(list[which])
                                thread {
                                    selectPopup(main.ssh.shellChannel("find ${list[which]} -maxdepth 1 -type d")
                                            .split("\n")
                                            .dropLastWhile { it.isBlank() }
                                            .dropWhile { it == previousFolder.last() }
                                            .toTypedArray()
                                            .plus("Select this")
                                            .plus("Go Back")
                                            .reversedArray())
                                }
                                return@OnClickListener
                            }
                        })
                        .create()

                val data = TypedValue()
                activity.theme?.resolveAttribute(R.attr.colorPrimary, data, true)

                val d = build.show()
                d.findViewById<TextView>(activity.resources.getIdentifier("android:id/alertTitle", null, null)).setTextColor(data.data)
            }
    }
}