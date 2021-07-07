package com.karel.slmmobile

import android.content.Context
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v4.app.Fragment
import android.support.v7.view.menu.MenuBuilder
import android.util.Log
import android.util.TypedValue
import android.view.*
import kotlinx.android.synthetic.main.backups_fragment.*
import kotlinx.android.synthetic.main.fragment_backup_tab1.*
import kotlin.concurrent.thread
import org.jetbrains.anko.*
import org.jetbrains.anko.support.v4.toast


class BackupTab1 : Fragment() {

    private var output = listOf<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_backup_tab1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //setHasOptionsMenu(true)
        retainInstance = false
        setHasOptionsMenu(true)

        thread {
            output = main.ssh.shellChannel("echo `grep '^[[:blank:]]*export[[:blank:]]*BM_TARBALL_DIRECTORIES=' /etc/backup-manager.conf | cut -d= -f2;" +
                    " echo [backup-manager-config-split]; grep '^[[:blank:]]*export[[:blank:]]*BM_REPOSITORY_ROOT=' /etc/backup-manager.conf | cut -d= -f2;" +
                    " echo [backup-manager-config-split]; grep '^[[:blank:]]*export[[:blank:]]*BM_ARCHIVE_FREQUENCY=' /etc/backup-manager.conf | cut -d= -f2;" +
                    " echo [backup-manager-config-split]; grep '^[[:blank:]]*export[[:blank:]]*BM_ARCHIVE_PREFIX=' /etc/backup-manager.conf | cut -d= -f2;" +
                    " echo [backup-manager-config-split]; grep '^[[:blank:]]*export[[:blank:]]*BM_TARBALL_NAMEFORMAT=' /etc/backup-manager.conf | cut -d= -f2;" +
                    " echo [backup-manager-config-split]; grep '^[[:blank:]]*export[[:blank:]]*BM_TARBALL_FILETYPE=' /etc/backup-manager.conf | cut -d= -f2;" +
                    " echo [backup-manager-config-split]; grep '^[[:blank:]]*export[[:blank:]]*BM_TARBALLINC_MASTERDATETYPE=' /etc/backup-manager.conf | cut -d= -f2`", true, true)
                    .toString()
                    .replace("\n", "")
                    .split("[backup-manager-config-split]")

            output.forEach { it.drop(2); it.dropLast(1) }
            activity?.runOnUiThread { setInterface() }
        }
    }

    private fun saveB() {
        //Log.e("Tab1","$isAdded - $isDetached - $isHidden - $isResumed - $isRemoving - $isVisible")
        if(isVisible) {    // jakis sposob na rozwiazanie problemu...
            val data = ArrayList<String>()
            data.add(backup_tab1_destination.text.toString())
            data.add(backup_tab1_filenameprefix.text.toString())
            when {
                backup_tab1_format_long.isChecked -> {
                    data.add("long")
                }
                else -> {
                    data.add("short")
                }
            }

            when {
                backup_tab1_master_weekly.isChecked -> {
                    data.add("weekly")
                }
                else -> {
                    data.add("monthly")
                }
            }

            when {
                backup_tab1_incremental_daily.isChecked -> {
                    data.add("daily")
                }
                else -> {
                    data.add("hourly")
                }
            }
            data.add(resources.getStringArray(R.array.backup_manager_filetype)[backup_tab1_spinner_file.selectedItemPosition])

            Log.e("SAVE data", data.toString())
            thread {
                main.ssh.shellChannel("sed -i '/^[[:space:]]*export BM_REPOSITORY_ROOT/c\\export BM_REPOSITORY_ROOT=\"${data[0]}\"' /etc/backup-manager.conf", true, true)
                main.ssh.shellChannel("sed -i '/^[[:space:]]*export BM_ARCHIVE_PREFIX/c\\export BM_ARCHIVE_PREFIX=\"${data[1]}\"' /etc/backup-manager.conf", true, true)
                main.ssh.shellChannel("sed -i '/^[[:space:]]*export BM_TARBALL_NAMEFORMAT/c\\export BM_TARBALL_NAMEFORMAT=\"${data[2]}\"' /etc/backup-manager.conf", true, true)

                main.ssh.shellChannel("sed -i '/^[[:space:]]*export BM_TARBALLINC_MASTERDATETYPE/c\\export BM_TARBALLINC_MASTERDATETYPE=\"${data[3]}\"' /etc/backup-manager.conf", true, true)

                main.ssh.shellChannel("sed -i '/^[[:space:]]*export BM_ARCHIVE_FREQUENCY/c\\export BM_ARCHIVE_FREQUENCY=\"${data[4]}\"' /etc/backup-manager.conf", true, true)
                main.ssh.shellChannel("sed -i '/^[[:space:]]*export BM_TARBALL_FILETYPE/c\\export BM_TARBALL_FILETYPE=\"${data[5]}\"' /etc/backup-manager.conf", true, true)
                activity?.runOnUiThread { toast("Saved!") }
            }
        }
    }

    /*override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.save, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }*/

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

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_save -> {
                Log.e("TAB1","TWICE?")
                saveB()
                vibrate(28, 1)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setInterface() {
        Log.e("Backup_tab1", output.toString())
        val files = resources.getStringArray(R.array.backup_manager_filetype)

        backup_tab1_destination.setText(output[1].replace("\"", "").trim())


        if (output[2].contains("daily", true))
            backup_tab1_incremental_daily.isChecked = true
        else
            backup_tab1_incremental_hourly.isChecked = true

        backup_tab1_filenameprefix.setText(output[3].replace("\"", "").replace(" ", ""))
        if (output[4].contains("long", true))
            backup_tab1_format_long.isChecked = true
        else
            backup_tab1_format_short.isChecked = true
        files.forEach {
            Log.e("FILE TYPE", "$it ${output[5]}")
            if (it.contains(output[5].replace("\"", "").trim(), true)) {
                backup_tab1_spinner_file.setSelection(files.indexOf(it))
            }
        }
        if (output[6].contains("weekly", true))
            backup_tab1_master_weekly.isChecked = true
        else
            backup_tab1_master_monthly.isChecked = true

    }

    private fun vibrate(msec: Long, amplitude: Int) {
        val vib = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26)
            vib.vibrate(VibrationEffect.createOneShot(msec, amplitude))
        else
            vib.vibrate(msec)
    }

}// Required empty public constructor
// co ??
