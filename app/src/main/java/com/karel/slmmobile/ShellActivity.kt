package com.karel.slmmobile

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_shell.*
import org.jetbrains.anko.defaultSharedPreferences
import kotlin.concurrent.thread

class ShellActivity : AppCompatActivity() {

    private val stringB: StringBuilder = StringBuilder()
    private val util = AppUtils(this)

    override fun onCreate(savedInstanceState: Bundle?) {

        util.changeTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shell)
        val toolbar = findViewById<Toolbar>(R.id.shell_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if(shell_terminal.text == "")
            stringB.append("SSH Shell")
        else
            stringB.append(shell_terminal.text)
        shell_terminal.movementMethod = ScrollingMovementMethod()
        shell_Break.setOnClickListener {
            onTerminalBreakClicked()
            util.vibrate(30, 1)
        }
        shell_sendButton.setOnClickListener {
            onTerminalSendClicked()
            util.vibrate(30, 1)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onTerminalSendClicked() {
        val command = shell_command.text.toString()
        Log.e("Recived: ", command)

        thread {
            val str = main.ssh.shellChannel(command)
            if (!str.isEmpty()) {
                Log.e("Send: ", str.toString())
                stringB.append("\n" + str)
                this.runOnUiThread { if(shell_terminal != null) shell_terminal.text = stringB }
            }
            this.runOnUiThread { if(shell_command != null) shell_command.text.clear() }
        }
    }
    private fun onTerminalBreakClicked() {
        //main.ssh.breakTerminal()
    }

    override fun onResume() {
        super.onResume()
        main.ssh.activityStatus[5] = true   // jak się soć robi to by ssh nie wyłączyło
        if(!defaultSharedPreferences.getString("Theme","Default").contains(util.theme,true))
            recreate()
    }

    override fun onStop() {
        super.onStop()
        main.ssh.activityStatus[5] = false  // jak się przejdzie do background to by się ssh wyłączyło
    }
}
