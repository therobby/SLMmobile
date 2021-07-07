package com.karel.slmmobile

import android.content.Context
import android.os.*
import android.support.design.widget.TabLayout
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.Html
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_about.*
import kotlinx.android.synthetic.main.backups_fragment.*
import org.jetbrains.anko.defaultSharedPreferences

class AboutActivity : AppCompatActivity() {

    private val util = AppUtils(this)
    private val tab1 = AboutAboutTab()
    private val tab2 = AboutLegalTab()

    override fun onCreate(savedInstanceState: Bundle?) {
        util.changeTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        configTabLayout()
    }

    private fun configTabLayout() {
        about_tabs.removeAllTabs()
        about_tabs.addTab(about_tabs.newTab().setText("Basic options"))
        about_tabs.addTab(about_tabs.newTab().setText("Advanced options"))

        //(activity as AppCompatActivity).supportFragmentManager

        val adapter = AboutTabAdapter(supportFragmentManager, about_tabs.tabCount)
        about_pager.clearOnPageChangeListeners()
        about_pager.adapter = adapter
        about_pager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(backup_tabs))
        about_tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                about_pager.currentItem = tab.position
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }
        })
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

    inner class AboutTabAdapter(fm: FragmentManager, private var tabCount: Int) : FragmentStatePagerAdapter(fm) {

        override fun getItem(position: Int): android.support.v4.app.Fragment? {
            return when (position) {
                0 -> tab1
                1 -> tab2
                else -> null
            }
        }

        override fun getCount(): Int {
            return tabCount
        }

        override fun saveState(): Parcelable? {
            return null
        }

        override fun restoreState(state: Parcelable?, loader: ClassLoader?) {
        }
    }

    override fun onResume() {
        super.onResume()
        main.ssh.activityStatus[0] = true   // jak się soć robi to by ssh nie wyłączyło
    }

    override fun onStop() {
        super.onStop()
        main.ssh.activityStatus[0] = false  // jak się przejdzie do background to by się ssh wyłączyło
    }
}
