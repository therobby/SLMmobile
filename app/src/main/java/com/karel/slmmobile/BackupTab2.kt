package com.karel.slmmobile

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v4.app.Fragment
import android.support.v7.view.menu.MenuBuilder
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_backup_tab2.*
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.textColor
import kotlin.concurrent.thread


class BackupTab2 : Fragment() {

    private val pathArray = ArrayList<EditText>()
    private var previousFolder = ArrayList<String>()
    private lateinit var utils: AppUtils

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_backup_tab2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        retainInstance = false
        utils = AppUtils(activity!!)

        loadPath()
        backup_tab2_fab.setOnClickListener {
            Log.e("Backup_tab2", "FAB")
            vibrate(28, 1)
            addPath("")
        }

        backup_tab2_fab.setOnLongClickListener {
            thread {
                //previousFolder.add("/")
                //selectPopup(main.ssh.shellChannel("find / -maxdepth 1 -type d").split("\n").dropLastWhile { it.isBlank() }.dropWhile { it == previousFolder.last() }.toTypedArray().plus("Select this").plus("Go Back").reversedArray())
                val path = utils.pathSelector()
                Log.e("Backup_tab2", path)
                if(path != "drop")
                    activity?.runOnUiThread { addPath(path) }
            }
            true
        }
        refreshView()
    }

    private fun save() {
        //val data = ArrayList<String>()
        if (isVisible)
            thread {
                main.ssh.shellChannel("sed -i '/^[[:space:]]*export[[:space:]]BM_TARBALL_DIRECTORIES.*/c\\\\#export BM_TARBALL_DIRECTORIES=\"\"' /etc/backup-manager.conf;" +
                        "sed -i '/^[[:space:]]*export[[:space:]]BM_TARBALL_TARGETS.*/d' /etc/backup-manager.conf;" +
                        "sed -i '/^[[:space:]]*BM_TARBALL_TARGETS/d' /etc/backup-manager.conf;" +
                        "sed -i '/^[[:space:]]*declare -a BM_TARBALL_TARGETS/d' /etc/backup-manager.conf", true, true)

                main.ssh.shellChannel("echo \"declare -a BM_TARBALL_TARGETS\" >> /etc/backup-manager.conf", true, true)

                pathArray.forEach {
                    if (it.text.toString().trim().isNotBlank())
                        main.ssh.shellChannel("echo \"BM_TARBALL_TARGETS+=('\"${it.text}\"')\" >> /etc/backup-manager.conf", true, true)
                    //data.add(it.text.toString())
                }

                main.ssh.shellChannel("echo \"export BM_TARBALL_TARGETS\" >> /etc/backup-manager.conf", true, true)

                activity?.runOnUiThread { toast("Saved!") }
            }
    }

    private fun loadPath() {
        thread {
            val oldMethod = main.ssh.shellChannel("grep '^[[:space:]]*export[[:space:]]*BM_TARBALL_DIRECTORIES.*\$' /etc/backup-manager.conf >> /dev/null && echo true")
            if (oldMethod.contains("true", true)) {
                alert("Do you want to ???")
            } else {
                fillPath(false)
            }
        }
    }

    private fun fillPath(oldMethod: Boolean) {
        val output = ArrayList<String>()
        when (oldMethod) {
            true -> {
                var data = main.ssh.shellChannel("grep --color=never '^[[:blank:]]*export[[:blank:]]*BM_TARBALL_DIRECTORIES=' /etc/backup-manager.conf", true, true)
                        .toString()
                        .replace("\n", "")
                data = data.dropWhile { it != '=' }.drop(1)
                if (data[0] == '\'' || data[0] == '\"')
                    data = data.drop(1)
                if (data.last() == '\'' || data.last() == '\"')
                    data = data.dropLast(1)

                data.split(" ").forEach { output.add(it) }
            }
            else -> {
                var data = main.ssh.shellChannel("grep --color=never '^[[:blank:]]*BM_TARBALL_TARGETS.*\$' /etc/backup-manager.conf", true, true)
                        .toString()
                        .split("\n")

                data = data.dropLastWhile { it.trim().isBlank() }
                data.forEach { output.add(it) }

                Log.e("DATA TAB2", output.toString())

                for (i in 0 until output.size) {
                    output[i] = output[i].dropWhile { it != '=' }.drop(1)
                }

                for (i in 0 until output.size) {
                    output[i] = output[i].trim()
                    if (output[i][0] == '\'' || output[i][0] == '\"')
                        output[i] = output[i].drop(1)
                    else if (output[i][0] == '(' || output[i][1] == '\'' || output[i][1] == '\"')
                        output[i] = output[i].drop(2)
                    if (output[i].last() == '\'' || output[i].last() == '\"')
                        output[i] = output[i].dropLast(1)
                    else if (output[i].last() == ')' || output[i][output.size - 2] == '\"' || output[i][output.size - 2] == '\'')
                        output[i] = output[i].dropLast(2)
                }
            }
        }

        Log.e("TAB2 OUTPUT", output.toString())
        output.forEach { activity?.runOnUiThread { addPath(it) } }
    }

    private fun refreshView() {
        backup_tab2_layout.removeAllViewsInLayout()
        Log.e("Backup_tab2", pathArray.size.toString())
        pathArray.forEach { backup_tab2_layout.addView(it) }
    }

    private fun addPath(path: String) {
        val txt = EditText(context)
        txt.textSize = 18f
        txt.setText(path)
        txt.textSize = 18f
        txt.maxLines = 1
        txt.gravity = Gravity.START
        txt.layoutParams = TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        pathArray.add(txt)
        backup_tab2_layout.addView(txt)

        txt.setOnLongClickListener {
            activity?.runOnUiThread {
                alert_s(txt)
            }
            true
        }
        //refreshView()
    }

    private fun selectPopup(list: Array<String>) {
        Log.e("LIST", list.toString())
        activity?.runOnUiThread {
            if (list.isEmpty()) {
                addPath(previousFolder.last())
                previousFolder.clear()
            } else {
                val build = AlertDialog.Builder(activity)
                build.setTitle(previousFolder.last())
                        .setCancelable(true)
                        .setItems(list, DialogInterface.OnClickListener { dialog, which ->
                            if (list[which].contains("go back", true)) {
                                if (previousFolder.last() != "/") {
                                    previousFolder.remove(previousFolder.last())
                                    thread { selectPopup(main.ssh.shellChannel("find ${previousFolder.last()} -maxdepth 1 -type d").split("\n").dropLastWhile { it.isBlank() }.dropWhile { it == previousFolder.last() }.toTypedArray().plus("Select this").plus("Go Back").reversedArray()) }
                                }
                            } else if (list[which].contains("select this", true)) {
                                addPath(previousFolder.last())
                                previousFolder.clear()
                            } else {
                                previousFolder.add(list[which])
                                thread { selectPopup(main.ssh.shellChannel("find ${list[which]} -maxdepth 1 -type d").split("\n").dropLastWhile { it.isBlank() }.dropWhile { it == previousFolder.last() }.toTypedArray().plus("Select this").plus("Go Back").reversedArray()) }
                            }
                            //Log.e("Select", "$dialog $which")
                        })
                        .create()

                val data = TypedValue()
                activity?.theme?.resolveAttribute(R.attr.colorPrimary, data, true)

                val d = build.show()
                d.findViewById<TextView>(resources.getIdentifier("android:id/alertTitle", null, null)).setTextColor(data.data)
            }
        }
    }

    private fun alert_s(txt: EditText) {
        Log.e("STATUS", "Alert_e")

        val build = AlertDialog.Builder(activity)
        build.setTitle(resources.getString(R.string.delete))
                .setCancelable(true)
                .setPositiveButton(resources.getString(R.string.button_yes), DialogInterface.OnClickListener { dialog, _ ->
                    vibrate(28, 1)
                    backup_tab2_layout.removeView(txt)
                    pathArray.remove(txt)
                    dialog.dismiss()
                })
                .setNegativeButton(resources.getString(R.string.button_no), DialogInterface.OnClickListener { dialog, _ ->
                    vibrate(28, 1)
                    dialog.dismiss()
                })
                .create()

        val data = TypedValue()
        activity?.theme?.resolveAttribute(R.attr.colorPrimary, data, true)

        val d = build.show()
        d.findViewById<TextView>(resources.getIdentifier("android:id/alertTitle", null, null)).setTextColor(data.data)
        d.findViewById<TextView>(resources.getIdentifier("android:id/button1", null, null)).setTextColor(data.data)
        d.findViewById<TextView>(resources.getIdentifier("android:id/button2", null, null)).setTextColor(data.data)
        d.findViewById<TextView>(resources.getIdentifier("android:id/button3", null, null)).setTextColor(data.data)

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

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_save -> {
                save()
                vibrate(28, 1)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun alert(info: String) {
        activity?.runOnUiThread {
            val build = AlertDialog.Builder(activity)
            build.setTitle(info)
                    .setCancelable(false)
                    .setPositiveButton(resources.getString(R.string.button_yes), DialogInterface.OnClickListener { dialog, _ ->
                        vibrate(28, 1)
                        dialog.dismiss()
                        thread { fillPath(true) }
                    })
                    .setNegativeButton(resources.getString(R.string.button_no), DialogInterface.OnClickListener { dialog, _ ->
                        vibrate(28, 1)
                        dialog.dismiss()
                    })
                    .create()

            val data = TypedValue()
            activity?.theme?.resolveAttribute(R.attr.colorPrimary, data, true)

            val d = build.show()
            d.findViewById<TextView>(resources.getIdentifier("android:id/alertTitle", null, null)).setTextColor(data.data)
        }
    }

    private fun vibrate(msec: Long, amplitude: Int) {
        val vib = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26)
            vib.vibrate(VibrationEffect.createOneShot(msec, amplitude))
        else
            vib.vibrate(msec)
    }
}
