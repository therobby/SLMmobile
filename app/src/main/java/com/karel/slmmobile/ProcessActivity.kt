package com.karel.slmmobile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_process.*
import org.jetbrains.anko.defaultSharedPreferences
import kotlin.concurrent.thread

class ProcessActivity : AppCompatActivity() {

    private val util = AppUtils(this)

    override fun onCreate(savedInstanceState: Bundle?) {

        util.changeTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_process)

        val toolbar = findViewById<Toolbar>(R.id.Process_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        Process_owner.text = intent.getStringExtra("User")
        Process_pid.text = intent.getStringExtra("Pid")
        Process_process.text = intent.getStringExtra("Cmd")

        Process_kill.setOnClickListener {
            thread {
                Log.e("Process kill", "kill -9 ${Process_pid.text}")
                main.ssh.shellChannel("kill -9 ${Process_pid.text}")
                this.runOnUiThread { onBackPressed() }
            }
        }

        Process_stop.setOnClickListener {
            thread{
                Log.e("Process kill", "kill -1 ${Process_pid.text}")
                main.ssh.shellChannel("kill -1 ${Process_pid.text}")
                this.runOnUiThread { onBackPressed() }
            }
        }

        Process_term.setOnClickListener {
            thread {
                Log.e("Process kill", "kill -15 ${Process_pid.text}")
                main.ssh.shellChannel("kill -15 ${Process_pid.text}")
                this.runOnUiThread {
                    setResult(69, Intent())
                    onBackPressed()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            android.R.id.home -> {
                onBackPressed()
                util.vibrate(30,1)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        if(!defaultSharedPreferences.getString("Theme","Default").contains(util.theme,true))
            recreate()
    }
}
