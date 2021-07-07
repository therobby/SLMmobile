package com.karel.slmmobile

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_slm_settings.*
import org.jetbrains.anko.defaultSharedPreferences


class SlmSettingsActivity : AppCompatActivity() {

    private var first_select_dist = true
    private var first_select_theme = true
    private val util = AppUtils(this)

    override fun onCreate(savedInstanceState: Bundle?) {

        util.changeTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slm_settings)
        val toolbar = findViewById<Toolbar>(R.id.slmSettingsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val adapterDist = ArrayAdapter<String>(this,R.layout.spinner_item,resources.getStringArray(R.array.pref_choose_distro))
        adapterDist.setDropDownViewResource(R.layout.spinner_item)
        slmSettings_Chose_Distro.adapter = adapterDist

        val adapterTheme = ArrayAdapter<String>(this,R.layout.spinner_item,resources.getStringArray(R.array.themes))
        adapterTheme.setDropDownViewResource(R.layout.spinner_item)
        slmSettings_Chose_theme.adapter = adapterTheme

        slmSettings_Chose_Distro.setSelection(defaultSharedPreferences.getInt("distro_value", 0))
        slmSettings_Chose_theme.setSelection(resources.getStringArray(R.array.themes).indexOf(defaultSharedPreferences.getString("Theme", "Light Gray")))

        if (defaultSharedPreferences.getString("password", "") != "") {
            slmSettings_Remember_Password.isChecked = true
        }

        slmSettings_Chose_Distro.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val dist = resources.getStringArray(R.array.pref_choose_distro)
                Log.e("Settings", slmSettings_Chose_Distro.selectedItem.toString() + " " + dist.indexOf(slmSettings_Chose_Distro.selectedItem.toString()).toString())
                defaultSharedPreferences.edit().putInt("distro_value", dist.indexOf(slmSettings_Chose_Distro.selectedItem.toString())).apply()
                if (!first_select_dist)
                    util.vibrate(28, 1)
                else
                    first_select_dist = false
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                Log.e("Settings", slmSettings_Chose_Distro.selectedItem.toString() + " Nothing")
            }
        }

        slmSettings_Chose_theme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                if (!first_select_theme) {
                    //util.vibrate(28, 1)
                    defaultSharedPreferences.edit().putString("Theme", slmSettings_Chose_theme.selectedItem.toString()).apply()
                    Log.e("THEME",slmSettings_Chose_theme.selectedItem.toString())
                    if(!defaultSharedPreferences.getString("Theme","Default").contains(util.theme,true))
                        recreate()
                }
                else
                    first_select_theme = false
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        slmSettings_Remember_Password.setOnClickListener {
            Log.e("Settings", slmSettings_Remember_Password.isChecked.toString())
            if (!slmSettings_Remember_Password.isChecked) {
                defaultSharedPreferences.edit().putString("password", "").apply()
            } else {
                defaultSharedPreferences.edit().putString("password", main.ssh.password).apply()
            }
            util.vibrate(30, 1)
        }

        slmSettings_shell.setOnClickListener {
            util.vibrate(30, 1)
            val intent = Intent()
            intent.setClassName(this, ShellActivity::class.java.name)
            startActivity(intent)
        }

        slmSettings_auth_button.setOnClickListener {
            util.vibrate(30, 1)
            val intent = Intent()
            intent.setClassName(this, AuthActivity::class.java.name)
            startActivity(intent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                util.vibrate(28, 1)
                onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        main.ssh.activityStatus[6] = true   // jak się soć robi to by ssh nie wyłączyło
    }

    override fun onStop() {
        super.onStop()
        main.ssh.activityStatus[6] = false  // jak się przejdzie do background to by się ssh wyłączyło
    }
}
