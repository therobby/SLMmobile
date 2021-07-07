package com.karel.slmmobile

import android.content.Intent
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.transition.Scene
import android.transition.Transition
import android.transition.TransitionInflater
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import java.util.regex.Pattern
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private var scene1: Scene? = null
    private var scene2: Scene? = null
    private var mTransition: Transition? = null
    private var util = AppUtils(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        util.changeTheme()

        setContentView(R.layout.activity_main)


        mTransition = TransitionInflater.from(this).inflateTransition(R.transition.login_transition)
        scene1 = Scene.getSceneForLayout(main_activity, R.layout.activity_main_scene1, this)
        scene2 = Scene.getSceneForLayout(main_activity, R.layout.activity_main_scene2, this)

        scene1?.enter()
        thread {
            Thread.sleep(800)
            this.runOnUiThread {
                TransitionManager.go(scene2, mTransition)
                onOn()
            }
        }

    }

    private fun onOn() {
        Log.e("Login", "onOn")
        val hostname = defaultSharedPreferences.getString("hostname", "")
        val username = defaultSharedPreferences.getString("username", "")
        val port = defaultSharedPreferences.getString("port", "22")
        Login_hostname.setText(hostname)
        Login_username.setText(username)
        Login_port.setText(port)
        val passwd = defaultSharedPreferences.getString("password", "")
        if (passwd != "") {
            Login_password.setText(passwd)
        }
        Log.e("Login", "\n$hostname \n$username \n$port \n$passwd")
    }

    fun onLoginClicked(view: View) {
        toast(resources.getString(R.string.Connecting))
        thread {
            Looper.prepare()
            val reason : String
            util.vibrate(30, 1)
            if(!(Login_username.text.isBlank() || Login_hostname.text.isBlank())) {
                val ip = Login_hostname.text.toString()
                val IP_ADDRESS = Pattern.compile(
                        "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                                + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                                + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                                + "|[1-9][0-9]|[0-9]))")
                val matcher = IP_ADDRESS.matcher(ip)
                if(matcher.matches())
                    reason = main.ssh.connect(Login_username.text.toString(), Login_password.text.toString(), ip, Login_port.text.toString().takeWhile { it.isDigit() }.toInt())
                else
                    reason = "Invalid ip format"
                //while (!(main.serviceConnected && main.ssh.checkConnection()))
                //   Thread.sleep(1010)
            }
            else
                reason = "Username and Hostname are required"

            if (main.ssh.checkConnection() && main.serviceConnected) {
                this.runOnUiThread { toast(resources.getString(R.string.Connected)) }

                if(defaultSharedPreferences.getString("hostname"," ").toString().trim() != Login_hostname.text.toString().trim())
                    defaultSharedPreferences.edit().putString("password","").apply()

                defaultSharedPreferences.edit()
                        .putString("hostname", Login_hostname.text.toString())
                        .putString("username", Login_username.text.toString())
                        .putString("port", Login_port.text.toString())
                        .apply()

                this.runOnUiThread {
                    val i = Intent(this, DrawerActivity::class.java)
                    startActivity(i)
                }
            } else {
                this.runOnUiThread { toast(reason) }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        try {
            main.ssh.activityStatus[4] = true   // jak się soć robi to by ssh nie wyłączyło
            if (main.ssh.checkConnection())
                onOn()
        } catch (e: Exception) {
        }
        if(!defaultSharedPreferences.getString("Theme","Default").contains(util.theme,true))
            recreate()
    }

    override fun onStop() {
        super.onStop()
        try {
            main.ssh.activityStatus[4] = false  // jak się przejdzie do background to by się ssh wyłączyło
        } catch (e: Exception) { }
    }

    override fun onDestroy() {
        super.onDestroy()
        main.ssh.close()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            if (main.serviceConnected && !main.ssh.checkConnection()) {
                //toast(resources.getString(R.string.Connection_lost))
                Log.e("Login", "???")
                main.serviceConnected = false
                thread {
                    Thread.sleep(500)
                    this.runOnUiThread {
                        onOn()
                    }
                }
            }
        }
    }

}