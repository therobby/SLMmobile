package com.karel.slmmobile

import android.app.Fragment
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.TabLayout
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.menu.MenuBuilder
import android.util.Log
import android.util.TypedValue
import android.view.*
import kotlinx.android.synthetic.main.backups_fragment.*

/**
 * Created by karel on 12.04.18.
 */
class BackupsFragment : Fragment() {
    //http://www.truiton.com/2015/06/android-tabs-example-fragments-viewpager/
    private var util: AppUtils? = null
    private val tab1 = BackupTab1()
    private val tab2 = BackupTab2()

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //return super.onCreateView(inflater, container, savedInstanceState)

        return inflater?.inflate(R.layout.backups_fragment, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {

            retainInstance = false

            util = AppUtils(activity)

            configTabLayout()
        } else
            super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
        Log.e("Backup", "VCreat")
    }

    private fun configTabLayout() {
        backup_tabs.removeAllTabs()
        backup_tabs.addTab(backup_tabs.newTab().setText("Basic options"))
        backup_tabs.addTab(backup_tabs.newTab().setText("Advanced options"))

        //(activity as AppCompatActivity).supportFragmentManager

        val adapter = backupTabAdapter((this.activity as AppCompatActivity).supportFragmentManager, backup_tabs.tabCount)
        backup_pager.clearOnPageChangeListeners()
        backup_pager.adapter = adapter
        backup_pager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(backup_tabs))
        backup_tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                backup_pager.currentItem = tab.position
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }
        })
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        menu?.let {
            if(menu is MenuBuilder){
                try{
                    val f = menu.javaClass.getDeclaredField("mOptionalIconsVisible")
                    f.isAccessible = true
                    f.setBoolean(menu,true)
                }catch (e:Exception){}
                val data = TypedValue()
                activity?.theme?.resolveAttribute(R.attr.colorAccent, data, true)

                for(item in 0 until menu.size()){
                    val menuItem = menu.getItem(item)
                    menuItem.icon.setColorFilter(data.data, PorterDuff.Mode.SRC_ATOP)
                }
            }
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        menu?.clear()
        inflater?.inflate(R.menu.save, menu)
    }

    inner class backupTabAdapter(fm: FragmentManager, private var tabCount: Int) : FragmentStatePagerAdapter(fm) {

        override fun getItem(position: Int): android.support.v4.app.Fragment? {
            when (position) {
                0 -> return tab1
                1 -> return tab2
                else -> return null
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

    override fun onDestroyView() {
        super.onDestroyView()
        onDestroyOptionsMenu()
        setMenuVisibility(false)
    }
}