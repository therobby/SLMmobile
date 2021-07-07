package com.karel.slmmobile

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_donate.*
import org.jetbrains.anko.defaultSharedPreferences

class DonateActivity : AppCompatActivity() {

    private val util = AppUtils(this)

    override fun onCreate(savedInstanceState: Bundle?) {

        util.changeTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donate)
        val toolbar = findViewById<Toolbar>(R.id.donare_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        donate_dolars.text = defaultSharedPreferences.getInt("dolars", 0).toString()

        donate_dolars_get.setOnClickListener {
            var dolars = donate_dolars.text.toString().toInt()
            dolars++
            defaultSharedPreferences.edit().putInt("dolars", dolars).apply()
            donate_dolars.text = dolars.toString()
            util.vibrate(30,1)
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

    override fun onResume() {
        super.onResume()
        main.ssh.activityStatus[1] = true   // jak się soć robi to by ssh nie wyłączyło
        if(!defaultSharedPreferences.getString("Theme","Default").contains(util.theme,true))
            recreate()
    }

    override fun onStop() {
        super.onStop()
        main.ssh.activityStatus[1] = false  // jak się przejdzie do background to by się ssh wyłączyło
    }
}
