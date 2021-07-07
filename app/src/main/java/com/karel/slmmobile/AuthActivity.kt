package com.karel.slmmobile

import android.content.Context
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v7.widget.Toolbar
import android.transition.Scene
import android.transition.Transition
import android.transition.TransitionInflater
import android.util.Log
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_auth.*
import kotlinx.android.synthetic.main.activity_auth_1.*
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast
import kotlin.concurrent.thread

class AuthActivity : AppCompatActivity() {

    private var scene1 : Scene? = null
    private var scene2 : Scene? = null
    private var mTransition: Transition? = null
    private var clicked = false
    private val util = AppUtils(this)

    override fun onCreate(savedInstanceState: Bundle?) {

        util.changeTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)



        mTransition = TransitionInflater.from(baseContext).inflateTransition(R.transition.auth_transition)
        scene1 = Scene.getSceneForLayout(auth_main, R.layout.activity_auth_1, baseContext)
        scene2 = Scene.getSceneForLayout(auth_main, R.layout.activity_auth_2, baseContext)

        if (main.ssh.root) {
            scene2?.enter()
            val toolbar = findViewById<Toolbar>(R.id.auth_toolbar2)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        } else {
            scene1?.enter()
            val toolbar = findViewById<Toolbar>(R.id.auth_toolbar1)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
        if (!main.ssh.root) {
            auth_button.setOnClickListener { if(!clicked) onEnterClicked() }
        }
    }

    private fun onEnterClicked() {
        util.vibrate(30,1)
        clicked = true
        thread {
            val out = main.ssh.rootConnect(auth_editText.text.toString())//main.ssh.execCommand(auth_editText.text.toString(), true)
            //val out = main.ssh.auth(auth_editText.text.toString())
            //this.runOnUiThread { toast(out) }
            Log.e("AUTH", out)

            if (!main.ssh.root) {
                this.runOnUiThread { toast("Authentication failure") }
                thread {

                    this.runOnUiThread {
                        android.animation.ObjectAnimator.ofObject(auth_linearLayout, "backgroundColor", android.animation.ArgbEvaluator(), android.graphics.Color.argb(172,255,255,255), android.graphics.Color.argb(172,255,82,82))
                                .setDuration(400)
                                .start()
                    }
                    Thread.sleep(500)
                    thread {
                        this.runOnUiThread {
                            android.animation.ObjectAnimator.ofObject(auth_linearLayout, "backgroundColor", android.animation.ArgbEvaluator(), android.graphics.Color.argb(172,255,82,82), android.graphics.Color.argb(172,255,255,255))
                                    .setDuration(400)
                                    .start()
                        }
                    }
                }
            }
            else{
                this.runOnUiThread { android.transition.TransitionManager.go(scene2, mTransition) }
            }
            clicked = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                util.vibrate(28,1)
                onBackPressed()
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
