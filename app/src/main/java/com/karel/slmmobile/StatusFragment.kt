package com.karel.slmmobile

import android.animation.LayoutTransition
import android.app.AlertDialog
import android.app.Fragment
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.*
import android.text.Editable
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_libvirt.*
import kotlinx.android.synthetic.main.status_fragment.*
import org.jetbrains.anko.*
import kotlin.concurrent.thread

/**
 * Created by Karel on 28.03.2018.
 */
class StatusFragment : Fragment() {
    var button = -1
    var bClicked = false
    var container: ViewGroup? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //return super.onCreateView(inflater, container, savedInstanceState)

        this.container = container
        return inflater?.inflate(R.layout.status_fragment, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Status_scrollLayout.layoutTransition = LayoutTransition()

        Status_HostnameLinearLayout.setOnClickListener { vibrate(30, 1) }
        Status_HostnameLinearLayout.setOnLongClickListener { vibrate(30, 1); editAlert(); true }
        Status_UnameLinearLayout.setOnClickListener { vibrate(30, 1) }
        Status_UptimeLinearLayout.setOnLongClickListener { vibrate(30, 1); powerPopup(); true }
        Status_UptimeLinearLayout.setOnClickListener { vibrate(30, 1) }
        Status_CPULinearLayout.setOnClickListener { vibrate(30, 1) }
        Status_CPUUsageLinearLayout.setOnClickListener { vibrate(30, 1) }
        Status_RAMLinearLayout.setOnClickListener { vibrate(30, 1) }
        Status_OSLinearLayout.setOnClickListener { vibrate(30, 1) }
        Status_VirtualizedLinearLayout.setOnClickListener { vibrate(30, 1) }
        Status_UpdatesLinearLayout.setOnClickListener { vibrate(30, 1); updatesClick() }
        Status_UpdatesLinearLayout.setOnLongClickListener { vibrate(30, 1); updatesLongClick(); true }

        thread {
            var intA = 5
            var Hostname = true
            var Uname = true
            var Cpu = true
            var Os = true
            var Virtualized = true
            var Ram = true
            var CpuUsage = true
            var Uptime = true
            var maxRam = 0
            val maxRamS: String

            Thread.sleep(250)

            while (!(!this.isVisible || intA <= 0)) {

                // HOSTNAME
                if (Hostname) {
                    try {
                        val output = main.ssh.shellChannel("hostname", false)
                        this.runOnUiThread { if (!output.isBlank()) if (Status_HostameView != null) Status_HostameView.text = output }
                        if (output.isNotEmpty())
                            intA--
                    } catch (e: Exception) {
                        //this.runOnUiThread { toast("${resources.getString(R.string.error)}\n$e") }
                        Log.e("Status Error", e.toString())
                        Hostname = false
                    }
                } else
                    this.runOnUiThread { if (Status_HostameView != null) Status_HostameView.text = resources.getString(R.string.error) }

                // UNAME
                if (Uname) {
                    try {
                        val output = main.ssh.shellChannel("uname -r", false)
                        this.runOnUiThread { if (!output.isBlank()) if (Status_UnameView != null) Status_UnameView.text = output }
                        if (output.isNotEmpty())
                            intA--

                    } catch (e: Exception) {
                        //this.runOnUiThread { toast("${resources.getString(R.string.error)}\n$e") }
                        Log.e("Status Error", e.toString())
                        Uname = false
                    }
                } else
                    this.runOnUiThread { if (Status_UnameView != null) Status_UnameView.text = resources.getString(R.string.error) }

                // CPU MODEL
                if (Cpu) {
                    try {
                        val output = main.ssh.shellChannel("cat /proc/cpuinfo | grep \"model name\" | head -n1 | cut -d \":\" -f 2", false).toString().dropWhile { !it.isLetterOrDigit() }
                        this.runOnUiThread { if (!output.isBlank()) if (Status_CPUView != null) Status_CPUView.text = output }
                        if (output.isNotEmpty())
                            intA--
                    } catch (e: Exception) {
                        //this.runOnUiThread { toast("${resources.getString(R.string.error)}\n$e") }
                        Log.e("Status Error", e.toString())
                        Cpu = false
                    }
                } else
                    this.runOnUiThread { if (Status_CPUView != null) Status_CPUView.text = resources.getString(R.string.error) }

                // OS
                if (Os) {
                    try {
                        val output = main.ssh.shellChannel("cat /etc/os-release | grep PRETTY_NAME | cut -d= -f2 || lsb_release -a | grep Description | cut -d: -f2 || cat /etc/issue | sed -n 1p | sed 's!\\\\r.*$!!g'", false).toString().replace("\"", "")
                        this.runOnUiThread { if (!output.isBlank()) if (Status_OSView != null) Status_OSView.text = output }
                        if (output.isNotEmpty())
                            intA--
                    } catch (e: Exception) {
                        //this.runOnUiThread { toast("${resources.getString(R.string.error)}\n$e") }
                        Log.e("Status Error", e.toString())
                        Os = false
                    }
                } else
                    this.runOnUiThread { if (Status_OSView != null) Status_OSView.text = resources.getString(R.string.error) }

                // IS VIRTUALIZED
                if (Virtualized) {
                    try {
                        val output = main.ssh.shellChannel("lscpu | grep \"Hypervisor vendor\" | cut -d\":\" -f2 | sed 's/ //g'", false)
                        this.runOnUiThread {
                            val no = "NO"
                            if (Status_VirtualizedView != null)
                                if (!output.isBlank()) {
                                    Status_VirtualizedView.text = output
                                } else
                                    Status_VirtualizedView.text = no
                        }
                        if (output.isNotEmpty())
                            intA--
                    } catch (e: Exception) {
                        //this.runOnUiThread { toast("${resources.getString(R.string.error)}\n$e") }
                        Log.e("Status Error", e.toString())
                        Virtualized = false
                    }
                } else
                    this.runOnUiThread { if (Status_VirtualizedView != null) Status_VirtualizedView.text = resources.getString(R.string.error) }
            }


            // GET RAM INSTALLED IN CONNECTED MACHINE
            try {                                           //cat /proc/meminfo | sed -n 1p | awk '{ print $2 }'
                maxRamS = main.ssh.shellChannel("command free -k | sed -n 2p | awk '{ print $2 }'").toString().takeWhile { it.isDigit() }
                if (maxRamS == "")
                    Ram = false
                else
                    maxRam = maxRamS.toInt()
            } catch (e: Exception) {
                //this.runOnUiThread { toast("${resources.getString(R.string.error)}\n$e") }
                Log.e("Status Error", e.toString())
            }

            while (this.isVisible && !main.disconnectService) {
                var minRam: Int

                // UPTIME
                if (Uptime) {
                    try {
                        val output = main.ssh.shellChannel("uptime -p", false)
                        this.runOnUiThread { if (!output.isBlank()) if (Status_UptimeView != null) Status_UptimeView.text = output }
                    } catch (e: Exception) {
                        //this.runOnUiThread { toast("${resources.getString(R.string.error)}\n$e") }
                        Log.e("Status Error", e.toString())
                        Uptime = false
                    }
                } else
                    this.runOnUiThread { if (Status_UptimeView != null) Status_UptimeView.text = resources.getString(R.string.error) }

                // RAM USAGE
                if (Ram) {
                    try {
                        minRam = main.ssh.shellChannel("command free -k | sed -n 2p | awk '{ print \$3 }'").toString().takeWhile { it.isDigit() }.toInt()
                        //minRam = maxRam - minRam
                        val output = main.ssh.shellChannel("free -h  | sed -n 2p | awk '{ print \$3\"/\"\$2 }'", false)
                        this.runOnUiThread {
                            if (!output.isBlank())
                                if (Status_RAMView != null) {
                                    Status_RAMView.text = output
                                    Status_RAMProgressBar.max = maxRam
                                    Status_RAMProgressBar.progress = minRam
                                    if (maxRam - minRam <= maxRam * 0.15) {
                                        Status_RAMProgressBar.progressDrawable.setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN)
                                    } else if (maxRam - minRam <= maxRam * 0.5) {
                                        Status_RAMProgressBar.progressDrawable.setColorFilter(Color.YELLOW, android.graphics.PorterDuff.Mode.SRC_IN)
                                    } else {
                                        Status_RAMProgressBar.progressDrawable.setColorFilter(Color.GREEN, android.graphics.PorterDuff.Mode.SRC_IN)
                                    }
                                }
                        }
                    } catch (e: Exception) {
                        //this.runOnUiThread { toast("${resources.getString(R.string.error)}\n$e") }
                        Log.e("Status Error", e.toString())
                        Ram = false
                    }
                } else
                    this.runOnUiThread { if (Status_RAMView != null) Status_RAMView.text = resources.getString(R.string.error) }

                // CPU USAGE
                if (CpuUsage) {
                    try {
                        //cat <(grep 'cpu ' /proc/stat) <(sleep 1 && grep 'cpu ' /proc/stat) | awk -v RS="" '{print ($13-$2+$15-$4)*100/($13-$2+$15-$4+$16-$5) "%"}'\n"
                        val output = main.ssh.shellChannel("cat <(grep 'cpu ' /proc/stat) <(sleep 1 && grep 'cpu ' /proc/stat) | awk -v RS=\"\" '{print (\$13-\$2+\$15-\$4)*100/(\$13-\$2+\$15-\$4+\$16-\$5) \"%\"}'", false)
                        this.runOnUiThread {
                            if (!output.isBlank()) {

                                if (Status_CPUUsageView != null) {
                                    Status_CPUUsageView.text = output
                                    var a = "0"
                                    if (output.contains(".")) {
                                        a = output.take(output.indexOf(".")).toString()
                                        Log.e("Status cpu", a)
                                        if (!a.isBlank()) {
                                            var progress = 0
                                            val out = a.takeWhile { it.isDigit() }
                                            if (!out.isBlank())
                                                progress = out.toInt()

                                            if (a.isBlank())
                                                Status_CPUProgressBar.progress = 0
                                            else
                                                Status_CPUProgressBar.progress = progress
                                        } else {
                                            Status_CPUProgressBar.progress = 0
                                        }
                                    } else {
                                        if (output.contains("%")) {
                                            a = output.take(output.indexOf("%")).toString()
                                            val progress = a.takeWhile { it.isDigit() }.toInt()
                                            if (a.isBlank())
                                                Status_CPUProgressBar.progress = 0
                                            else
                                                Status_CPUProgressBar.progress = progress
                                        } else
                                            Status_CPUProgressBar.progress = 0
                                    }

                                    /*this.runOnUiThread {
                                        ObjectAnimator.ofObject(auth_linearLayout, "backgroundColor", android.animation.ArgbEvaluator(), android.graphics.Color.argb(172,255,0,0), android.graphics.Color.argb(172,255,255,255))
                                                .setDuration(400)
                                                .start()
                                    } TODO this https://stackoverflow.com/questions/30766755/smooth-progress-bar-animation/30766886*/

                                    if (a.toInt() >= 85) {
                                        Status_CPUProgressBar.progressDrawable.setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN)
                                    } else if (a.toInt() >= 50) {
                                        Status_CPUProgressBar.progressDrawable.setColorFilter(Color.YELLOW, android.graphics.PorterDuff.Mode.SRC_IN)
                                    } else {
                                        Status_CPUProgressBar.progressDrawable.setColorFilter(Color.GREEN, android.graphics.PorterDuff.Mode.SRC_IN)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        //this.runOnUiThread { toast("${resources.getString(R.string.error)}\n$e") }
                        Log.e("Status Error", e.toString())
                        CpuUsage = false
                    }
                } else
                    this.runOnUiThread { if (Status_CPUUsageView != null) Status_CPUUsageView.text = resources.getString(R.string.error) }
            }
        }
    }

    private fun updatesCheck() {
        thread {
            try {
                this.runOnUiThread { if (Status_UpdatesView != null) Status_UpdatesView.text = resources.getString(R.string.checking___) }

                val updateCommand = resources.getStringArray(R.array.check_updates)
                //"dnf check-update | wc -l"
                Log.e("STATUS_UPDATE", updateCommand[defaultSharedPreferences.getInt("distro_value", 0)])
                val updates = main.ssh.shellChannel(updateCommand[defaultSharedPreferences.getInt("distro_value", 0)]).toString().lines()

                Log.e("STATUS_UPDATES",updates.toString())

                if (updates.isEmpty()) {
                    this.runOnUiThread {
                        if (!main.ssh.root) toast(resources.getString(R.string.authorization_req))
                        this.runOnUiThread { if (Status_UpdatesView != null) Status_UpdatesView.text = resources.getString(R.string.tap_to_update) }
                    }
                } else {
                    var upd = 0
                    updates.forEach {
                        if (it.toIntOrNull() != null) {
                            upd = it.toInt()
                        }
                    }
                    val message = upd.toString() + " " + resources.getString(R.string.updates_available)
                    this.runOnUiThread { if (Status_UpdatesView != null) Status_UpdatesView.text = message }
                }
            } catch (e: Exception) {
                //this.runOnUiThread { toast("${resources.getString(R.string.error)}\n$e") }
                Log.e("Status Error", e.toString())
            }
            bClicked = false
        }
    }

    private fun updatesClick() {
        if (!bClicked) {
            bClicked = true
            updatesCheck()
        }
    }

    private fun updatesLongClick() {
        if (!bClicked) {
            bClicked = true
            this.runOnUiThread { if (Status_UpdatesView != null) Status_UpdatesView.text = "Updating..." }
            alert_s()
        }
    }

    private fun update() {
        if (main.ssh.root) {
            thread {
                val updateCommand = resources.getStringArray(R.array.update_commands)
                var out = ""
                Log.e("STATUS_UPDATE_LONG", updateCommand[defaultSharedPreferences.getInt("distro_value", 0)])
                out = main.ssh.shellChannel(updateCommand[defaultSharedPreferences.getInt("distro_value", 0)] + " && echo OK || echo Error").toString()
                while (out.isBlank());
                val list = out.split("\n").dropWhile { it == "" }
                for (i in list) {
                    Log.e("STATUS_UPDATE_LONG", i)
                }
                if (list[list.size - 2].contains("OK")) {
                    this.runOnUiThread { alert("Updated successfuly") }

                    Log.e("STATUS_UPDATE_LONG", "Success ${list[list.size - 2]}")
                } else {
                    this.runOnUiThread { alert("Error while updating") }
                    Log.e("STATUS_UPDATE_LONG", "Error ${list[list.size - 2]}")
                }
            }
        } else {
            if (!main.ssh.root) toast(resources.getString(R.string.authorization_req))
            this.runOnUiThread { if (Status_UpdatesView != null) Status_UpdatesView.text = resources.getString(R.string.tap_to_update) }
        }
    }

    private fun alert_s() {
        Log.e("STATUS", "Alert_e")
        val build = AlertDialog.Builder(activity)
        build.setTitle(resources.getString(R.string.usure))
                .setCancelable(false)
                .setPositiveButton(resources.getString(R.string.button_yes), DialogInterface.OnClickListener { dialog, _ ->
                    vibrate(28, 1)
                    dialog.dismiss()
                    update()
                })
                .setNegativeButton(resources.getString(R.string.button_no), DialogInterface.OnClickListener { dialog, _ ->
                    vibrate(28, 1)
                    updatesCheck()
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

    private fun alert(info: String) {
        val build = AlertDialog.Builder(activity)
        build.setTitle(info)
                .setCancelable(false)
                .setNeutralButton(resources.getString(R.string.ok), DialogInterface.OnClickListener { dialog, _ ->
                    vibrate(28, 1)
                    dialog.dismiss()
                    updatesCheck()
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

    private fun editAlert() {
        Log.e("STATUS", "Alert_edit")
        val build = AlertDialog.Builder(activity)
        val edit = EditText(activity)
        val tv = TypedValue()
        activity.theme.resolveAttribute(android.R.attr.colorPrimary, tv, true)
        edit.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        edit.setTextColor(tv.data)
        edit.linkTextColor = tv.data
        edit.highlightColor = tv.data

        build.setView(edit)
        build.setTitle(resources.getString(R.string.set_hostname))
                .setCancelable(true)
                .setPositiveButton(resources.getString(R.string.ok), DialogInterface.OnClickListener { dialog, _ ->
                    thread {
                        vibrate(28, 1)
                        dialog.dismiss()
                        val newHostname = edit.text.toString()
                        main.ssh.shellChannel("hostnamectl set-hostname $newHostname")
                        try {
                            val output = main.ssh.shellChannel("hostname", false)
                            this.runOnUiThread { if (!output.isBlank()) if (Status_HostameView != null) Status_HostameView.text = output }
                        } catch (e: Exception) {
                            //this.runOnUiThread { toast("${resources.getString(R.string.error)}\n$e") }
                            Log.e("Status HError", e.toString())
                        }
                    }
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

    private fun powerPopup() {
        val build = AlertDialog.Builder(activity)
        val dist = resources.getStringArray(R.array.power_actions)
        build.setTitle("Actions")
                .setCancelable(true)
                .setItems(dist, DialogInterface.OnClickListener { _, i ->
                    when (i) {
                        0 -> {
                            //reboot
                            vibrate(28, 1)
                            thread { main.ssh.shellChannel("systemctl reboot") }
                        }
                        1 -> {
                            //shutdown
                            vibrate(28, 1)
                            thread { main.ssh.shellChannel("systemctl isolate poweroff.target") }
                        }
                    }
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

    private fun vibrate(msec: Long, amplitude: Int) {
        val vib = activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26)
            vib.vibrate(VibrationEffect.createOneShot(msec, amplitude))
        else
            vib.vibrate(msec)
    }
}
