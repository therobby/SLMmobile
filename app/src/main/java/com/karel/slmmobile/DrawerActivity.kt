package com.karel.slmmobile

import android.content.Intent
import android.os.*
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_about.*
import kotlinx.android.synthetic.main.activity_drawer.*
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.toast
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class DrawerActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var currentFragment = "status_fragment"
    private var del = false
    private var chackingIsRunning = false
    private var background = false
    private val util = AppUtils(this)
    private var firstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {

        util.changeTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawer)
        setSupportActionBar(about_toolbar)

        //https://stackoverflow.com/questions/2482848/how-to-change-current-theme-at-runtime-in-android

        val toggle = ActionBarDrawerToggle(this, drawer_layout, about_toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        //toggle.setHomeAsUpIndicator(R.drawable.menu)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
        checkConnection()

        findDistro()
        nav_view.menu.getItem(0).isChecked = true
        loadStatus()
    }

    private fun findDistro() {
        thread {
            val sp = defaultSharedPreferences
            val dist = resources.getStringArray(R.array.pref_choose_distro)
            val distro = main.ssh.shellChannel("cat /etc/os-release | grep ID | cut -d= -f2").toString().replace(" ", "").replace("\n", "")
            Log.e("Distro", distro)
            for (i in 0 until dist.size) {
                Log.e("Distro", dist[i])
                if (dist[i].contains("/")) {
                    val list = dist[i].split("/")
                    for (j in list) {
                        if (distro.contains(j, true)) {
                            sp.edit().putString("choose_distro", i.toString()).apply()
                            sp.edit().putInt("distro_value", i).apply()
                            return@thread
                        }
                    }
                } else if (distro.contains(dist[i], true)) {
                    sp.edit().putString("choose_distro", i.toString()).apply()
                    sp.edit().putInt("distro_value", i).apply()
                    return@thread
                }
            }
            val dialog = AlertDistroWarning()
            dialog.show(fragmentManager, "distro_warning")
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            if (del) {
                //main.disconnectService = true
                //main.ssh.close()
                //finish()
                //exitProcess(0)
                finishActivity(0)
                val homeIntent = Intent(Intent.ACTION_MAIN)
                homeIntent.addCategory(Intent.CATEGORY_HOME)
                homeIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(homeIntent)
                exitProcess(0)
            } else {
                if (!del) {
                    toast(resources.getString(R.string.twice_to_quit))
                    del = true
                    thread { Thread.sleep(300); del = false }
                }
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_services -> {
                if (currentFragment != "services_fragment") {
                    loadServices()
                    currentFragment = "services_fragment"
                }
            }
            R.id.nav_status -> {
                if (currentFragment != "status_fragment") {
                    loadStatus()
                    currentFragment = "status_fragment"
                }
            }
            R.id.nav_backups -> {
                if (currentFragment != "backups_fragment") {
                    loadBackups()
                    currentFragment = "backups_fragment"
                }
            }
            R.id.nav_settings -> {
                loadSettings()
            }

            R.id.nav_cron -> {
                if (currentFragment != "cron_fragment") {
                    loadCron()
                    currentFragment = "cron_fragment"
                }
            }
            R.id.nav_about -> {
                loadAbout()
            }
            R.id.nav_donate -> {
                loadDonate()
            }
            R.id.nav_logout -> {
                main.restart = true
            }
            R.id.nav_processes -> {
                if (currentFragment != "process_fragment") {
                    loadProcesses()
                    currentFragment = "process_fragment"
                }
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun loadDonate() {
        val intent = Intent()
        intent.setClassName(this, DonateActivity::class.java.name)
        startActivity(intent)
        util.vibrate(28, 1)

    }

    private fun loadAbout() {
        val intent = Intent()
        intent.setClassName(this, AboutActivity::class.java.name)
        startActivity(intent)
        util.vibrate(28, 1)
    }

    private fun loadSettings() {
        val intent = Intent()
        intent.setClassName(this, SlmSettingsActivity::class.java.name)
        startActivity(intent)
        util.vibrate(28, 1)
    }

    private fun loadBackups() {
        val transaction = fragmentManager.beginTransaction()
        val frag = BackupsFragment()
        transaction.replace(R.id.layout_main, frag)
                //.addToBackStack(null)
                .commit()
        about_toolbar.title = "Backups"
        util.vibrate(28, 1)
    }

    private fun loadServices() {
        val transaction = fragmentManager.beginTransaction()
        val frag = ServicesFragment()
        transaction.replace(R.id.layout_main, frag)
                //.addToBackStack(null)
                .commit()
        about_toolbar.title = "Services"
        util.vibrate(28, 1)
    }

    private fun loadCron() {
        val transaction = fragmentManager.beginTransaction()
        val frag = CronFragment()
        transaction.replace(R.id.layout_main, frag)
                //.addToBackStack(null)
                .commit()
        about_toolbar.title = "Cron"
        util.vibrate(28, 1)
    }

    private fun loadStatus() {
        val transaction = fragmentManager.beginTransaction()
        val frag = StatusFragment()
        transaction.replace(R.id.layout_main, frag)
                //.addToBackStack(null)
                .commit()
        about_toolbar.title = "Status"
        if(!firstLoad) {
            util.vibrate(28, 1)
            firstLoad = false
        }
    }

    private fun loadProcesses() {
        val transaction = fragmentManager.beginTransaction()
        val frag = ProcessesFragment()
        transaction.replace(R.id.layout_main, frag)
                //.addToBackStack(null)
                .commit()
        about_toolbar.title = "Processes"
        util.vibrate(28, 1)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState?.putString("currentFragment", currentFragment)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        currentFragment = savedInstanceState?.getString("currentFragment")!!

        when (currentFragment) {
            "status_fragment" -> {
                loadStatus()
            }
            "cron_fragment" -> {
                loadCron()
            }
            "services_fragment" -> {
                loadServices()
            }
            "backups_fragment" -> {
                loadBackups()
            }
            "process_fragment" -> {
                loadProcesses()
            }
        }
        if (!chackingIsRunning)
            checkConnection()
    }

    private fun checkConnection() {
        chackingIsRunning = true
        thread {
            Looper.prepare()
            while (main.ssh.checkConnection() && !main.disconnectService && !main.restart) {
                Thread.sleep(500)
            }
            if (main.restart) {
                main.disconnectService = true
                main.ssh.close()
                //finish()
                Log.e("Loop Check","Restart")
                exitProcess(0)
            }

            this.runOnUiThread {
                if (!background)
                    toast(resources.getString(R.string.Connection_lost))
            }
            //if(background)
            main.ssh.close()
            //Thread.sleep(200)
            //main.ssh.restart()
            finish()
            /*else {
                val i = Intent(this, MainActivity::class.java)
                startActivity(i)
                main.restartSSH()
            }*/
            chackingIsRunning = false
        }
    }

    override fun onResume() {
        super.onResume()
        main.ssh.activityStatus[2] = true   // jak się soć robi to by ssh nie wyłączyło
        if(!defaultSharedPreferences.getString("Theme","Default").contains(util.theme,true))
            recreate()
    }

    override fun onStop() {
        super.onStop()
        main.ssh.activityStatus[2] = false  // jak się przejdzie do background to by się ssh wyłączyło
    }

    /*override fun onStop() {
        super.onStop()
        Log.e("App", "Going background")
        thread {
            background = true
            for (i in 0..500) {
                Thread.sleep(1000)
                if (!background)
                    break
            }
            if (background) {
                main.ssh.close()
                main.restartSSH()
                //val h = Intent(Intent.ACTION_MAIN)
                //h.addCategory(Intent.CATEGORY_HOME)
                //h.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                //startActivity(h)
                //finishAndRemoveTask()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.e("App", "Going foreground")
        background = false
    }*/


}
