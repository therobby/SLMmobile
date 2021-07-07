package com.karel.slmmobile

import android.animation.LayoutTransition
import android.app.AlertDialog
import android.app.Fragment
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v4.content.ContextCompat
import android.support.v7.view.menu.MenuBuilder
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.services_fragment.*
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.runOnUiThread
import kotlin.concurrent.thread

/**
 * Created by karel on 12.04.18.
 */
class ServicesFragment : Fragment() {

    var runLoop = false
    val statusCommand = "if [[ \$(systemctl status \$service 2>/dev/null) == '' ]];then echo 'Not installed'; elif [[ \$(systemctl status \$service | egrep '^.*Loaded:.*not-found.*\$') ]];then echo 'Not installed'; elif [[ \$(systemctl status \$service | egrep '^.*Active:.*failed.*\$') ]];then echo 'Failed'; elif [[ \$(systemctl status \$service | egrep '^.*Active.*dead.*\$') ]];then echo 'Not running'; elif [[ \$(systemctl status \$service | egrep '^.*Active.*(running).*\$') ]];then echo 'Running'; elif [[ \$(systemctl status \$service | egrep '^.*Active.*(exited).*\$') ]];then echo \"Running\"; else echo 'Not running'; fi"

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //return super.onCreateView(inflater, container, savedInstanceState)

        return inflater?.inflate(R.layout.services_fragment, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        setHasOptionsMenu(true)
        Services_scrollLayout.layoutTransition = LayoutTransition()

        thread {
            loadServices()
        }

        autoRefresh()
    }

    private fun autoRefresh() {
        thread {
            while (Services_scrollLayout != null && runLoop) {
                Log.e("AutoLoop", "loop")
                Thread.sleep(5000)
                if (isVisible && defaultSharedPreferences.getBoolean("Service ref", false)) {
                    Log.e("AutoLoop", "ref")
                    defaultSharedPreferences.edit().putBoolean("Service ref", false).apply()
                    loadServices()
                }
            }
        }
    }

    private fun loadServices() {
        if (isVisible)
            service_nfs()
        if (isVisible)
            service_libvirt()
        if (isVisible)
            service_docker()
        if (isVisible)
            service_samba()
        if (isVisible)
            service_ftp()
        if (isVisible)
            service_apache()
        if (isVisible)
            service_sql()
    }

    private fun service_nfs() {
        val serviceName = resources.getStringArray(R.array.nfs_names)[defaultSharedPreferences.getInt("distro_value", 0)]
        this.runOnUiThread { if (Services_service1_ServiceName != null) Services_service1_ServiceName.text = serviceName }
        thread {
            var Status = main.ssh.shellChannel("service=$serviceName; $statusCommand", true, true).toString()
            Log.e("NFS STATUS", Status)
            if (isVisible && !Status.contains("Running", true)) {
                when (defaultSharedPreferences.getString("Service 1", "")) {
                    "Installing...", "Restarting...", "Stopping...", "Starting...", "Uninstalling..." -> {
                        Status = defaultSharedPreferences.getString("Service 1", "")
                    }
                }
                this.runOnUiThread {
                    if (Services_service1_PidValue != null) Services_service1_PidValue.text = "N/A"
                    if (Services_service1_PortValue != null) Services_service1_PortValue.text = "N/A"
                }
            } else {
                if (isVisible) {
                    val Pid = main.ssh.shellChannel("IN=\$(ps aux | grep ${resources.getStringArray(R.array.nfs_pids)[defaultSharedPreferences.getInt("distro_value", 0)]} | grep -v grep | echo \$(xargs -L1 echo|awk '{ print \$2}'));echo \$IN | sed 's/ /,/g'", true, true)
                    this.runOnUiThread { if (Services_service1_PidValue != null) Services_service1_PidValue.text = Pid.toString().replace("\n", "") }
                }
            }
            this.runOnUiThread { if (Services_service1_ServiceStatus != null) Services_service1_ServiceStatus.text = Status.replace("\n", "") }
            bcolor(Services_service1_LinearLayout, Status.replace("\n", ""))
        }

        if (Services_service1_LinearLayout != null)
            Services_service1_LinearLayout.setOnLongClickListener {
                optionsPopup("nfs", Services_service1_ServiceStatus.text.toString().replace("\n", ""))
                true
            }
    }

    private fun service_libvirt() {
        val serviceName = resources.getStringArray(R.array.libvirt_names)[defaultSharedPreferences.getInt("distro_value", 0)]
        this.runOnUiThread { if (Services_service2_ServiceName != null) Services_service2_ServiceName.text = serviceName }
        thread {
            var Status = main.ssh.shellChannel("service=$serviceName; $statusCommand", true, true).toString()
            if (isVisible && !Status.contains("Running", true)) {
                this.runOnUiThread {
                    when (defaultSharedPreferences.getString("Service 2", "")) {
                        "Installing...", "Restarting...", "Stopping...", "Starting...", "Uninstalling..." -> {
                            Status = defaultSharedPreferences.getString("Service 2", "")
                        }
                    }
                    if (Services_service2_PidValue != null) Services_service2_PidValue.text = "N/A"
                    if (Services_service2_PortValue != null) Services_service2_PortValue.text = "N/A"
                }
            } else {
                if (isVisible) {
                    val Pid = main.ssh.shellChannel("IN=\$(ps aux | grep ${resources.getStringArray(R.array.libvirt_pids)[defaultSharedPreferences.getInt("distro_value", 0)]} | grep -v grep | echo \$(xargs -L1 echo|awk '{ print \$2}'));echo \$IN | sed 's/ /,/g'", true, true)
                    this.runOnUiThread { if (Services_service2_PidValue != null) Services_service2_PidValue.text = Pid.toString().replace("\n", "") }
                }
            }
            this.runOnUiThread { if (Services_service2_ServiceStatus != null) Services_service2_ServiceStatus.text = Status.replace("\n", "") }
            bcolor(Services_service2_LinearLayout, Status.replace("\n", ""))
        }

        if (Services_service2_LinearLayout != null)
            Services_service2_LinearLayout.setOnLongClickListener {
                optionsPopup("libvirt", Services_service2_ServiceStatus.text.toString().replace("\n", ""))
                true
            }


    }

    private fun service_docker() {
        val serviceName = resources.getStringArray(R.array.docker_names)[defaultSharedPreferences.getInt("distro_value", 0)]
        this.runOnUiThread { if (Services_service3_ServiceName != null) Services_service3_ServiceName.text = serviceName }
        thread {
            var Status = main.ssh.shellChannel("service=$serviceName; $statusCommand", true, true).toString()
            if (isVisible && !Status.contains("Running", true)) {
                when (defaultSharedPreferences.getString("Service 3", "")) {
                    "Installing...", "Restarting...", "Stopping...", "Starting...", "Uninstalling..." -> {
                        Log.e("Stat", "not running")
                        Status = defaultSharedPreferences.getString("Service 3", "")
                    }
                }
                this.runOnUiThread {
                    if (Services_service3_PidValue != null) Services_service3_PidValue.text = "N/A"
                    if (Services_service3_PortValue != null) Services_service3_PortValue.text = "N/A"
                }
            } else {
                if (isVisible) {
                    val Pid = main.ssh.shellChannel("IN=\$(ps aux | grep ${resources.getStringArray(R.array.docker_pids)[defaultSharedPreferences.getInt("distro_value", 0)]} | grep -v grep | echo \$(xargs -L1 echo|awk '{ print \$2}'));echo \$IN | sed 's/ /,/g'", true, true)
                    this.runOnUiThread { if (Services_service3_PidValue != null) Services_service3_PidValue.text = Pid.toString().replace("\n", "") }
                }
            }
            this.runOnUiThread { if (Services_service3_ServiceStatus != null) Services_service3_ServiceStatus.text = Status.replace("\n", "") }
            bcolor(Services_service3_LinearLayout, Status.replace("\n", ""))
        }

        if (Services_service3_LinearLayout != null)
            Services_service3_LinearLayout.setOnLongClickListener {
                optionsPopup("docker", Services_service3_ServiceStatus.text.toString().replace("\n", ""))
                true
            }
    }

    private fun service_samba() {
        val serviceName = resources.getStringArray(R.array.samba_names)[defaultSharedPreferences.getInt("distro_value", 0)]
        this.runOnUiThread { if (Services_service4_ServiceName != null) Services_service4_ServiceName.text = serviceName }
        thread {
            var Status = main.ssh.shellChannel("service=$serviceName; $statusCommand", true, true).toString()
            if (isVisible && !Status.contains("Running", true)) {
                when (defaultSharedPreferences.getString("Service 4", "")) {
                    "Installing...", "Restarting...", "Stopping...", "Starting...", "Uninstalling..." -> {
                        Status = defaultSharedPreferences.getString("Service 4", "")
                    }
                }
                this.runOnUiThread {
                    if (Services_service4_PidValue != null) Services_service4_PidValue.text = "N/A"
                    if (Services_service4_PortValue != null) Services_service4_PortValue.text = "N/A"
                }
            } else {
                if (isVisible) {
                    val Pid = main.ssh.shellChannel("IN=\$(ps aux | grep ${resources.getStringArray(R.array.samba_pids)[defaultSharedPreferences.getInt("distro_value", 0)]} | grep -v grep | echo \$(xargs -L1 echo|awk '{ print \$2}'));echo \$IN | sed 's/ /,/g'", true, true)
                    this.runOnUiThread { if (Services_service4_PidValue != null) Services_service4_PidValue.text = Pid.toString().replace("\n", "") }
                }
            }
            this.runOnUiThread { if (Services_service4_ServiceStatus != null) Services_service4_ServiceStatus.text = Status.replace("\n", "") }
            bcolor(Services_service4_LinearLayout, Status.replace("\n", ""))
        }

        if (Services_service4_LinearLayout != null)
            Services_service4_LinearLayout.setOnLongClickListener {
                optionsPopup("samba", Services_service4_ServiceStatus.text.toString().replace("\n", ""))
                true
            }
    }

    private fun service_ftp() {
        val serviceName = resources.getStringArray(R.array.ftp_names)[defaultSharedPreferences.getInt("distro_value", 0)]
        this.runOnUiThread { if (Services_service5_ServiceName != null) Services_service5_ServiceName.text = serviceName }
        thread {
            var Status = main.ssh.shellChannel("service=$serviceName; $statusCommand", true, true).toString()
            if (isVisible && !Status.contains("Running", true)) {
                when (defaultSharedPreferences.getString("Service 5", "")) {
                    "Installing...", "Restarting...", "Stopping...", "Starting...", "Uninstalling..." -> {
                        Status = defaultSharedPreferences.getString("Service 5", "")
                    }
                }
                this.runOnUiThread {
                    if (Services_service5_PidValue != null) Services_service5_PidValue.text = "N/A"
                    if (Services_service5_PortValue != null) Services_service5_PortValue.text = "N/A"
                }
            } else {
                if (isVisible) {
                    val Pid = main.ssh.shellChannel("IN=\$(ps aux | grep ${resources.getStringArray(R.array.ftp_pids)[defaultSharedPreferences.getInt("distro_value", 0)]} | grep -v grep | echo \$(xargs -L1 echo|awk '{ print \$2}'));echo \$IN | sed 's/ /,/g'", true, true)
                    this.runOnUiThread { if (Services_service5_PidValue != null) Services_service5_PidValue.text = Pid.toString().replace("\n", "") }
                }
            }
            this.runOnUiThread { if (Services_service5_ServiceStatus != null) Services_service5_ServiceStatus.text = Status.replace("\n", "") }
            bcolor(Services_service5_LinearLayout, Status.replace("\n", ""))
        }

        if (Services_service5_LinearLayout != null)
            Services_service5_LinearLayout.setOnLongClickListener {
                optionsPopup("ftp", Services_service5_ServiceStatus.text.toString().replace("\n", ""))
                true
            }
    }

    private fun service_apache() {
        val serviceName = resources.getStringArray(R.array.apache_names)[defaultSharedPreferences.getInt("distro_value", 0)]
        this.runOnUiThread { if (Services_service6_ServiceName != null) Services_service6_ServiceName.text = serviceName }
        thread {
            var Status = main.ssh.shellChannel("service=$serviceName; $statusCommand", true, true).toString()
            if (isVisible && !Status.contains("Running", true)) {
                when (defaultSharedPreferences.getString("Service 6", "")) {
                    "Installing...", "Restarting...", "Stopping...", "Starting...", "Uninstalling..." -> {
                        Status = defaultSharedPreferences.getString("Service 6", "")
                    }
                }
                this.runOnUiThread {
                    if (Services_service6_PidValue != null) Services_service6_PidValue.text = "N/A"
                    if (Services_service6_PortValue != null) Services_service6_PortValue.text = "N/A"
                }
            } else {
                if (isVisible) {
                    val Pid = main.ssh.shellChannel("IN=\$(ps aux | grep ${resources.getStringArray(R.array.apache_pids)[defaultSharedPreferences.getInt("distro_value", 0)]} | grep -v grep | echo \$(xargs -L1 echo|awk '{ print \$2}'));echo \$IN | sed 's/ /,/g'", true, true)
                    this.runOnUiThread { if (Services_service6_PidValue != null) Services_service6_PidValue.text = Pid.toString().replace("\n", "") }
                }
            }
            this.runOnUiThread { if (Services_service6_ServiceStatus != null) Services_service6_ServiceStatus.text = Status.replace("\n", "") }
            bcolor(Services_service6_LinearLayout, Status.replace("\n", ""))
        }

        if (Services_service6_LinearLayout != null)
            Services_service6_LinearLayout.setOnLongClickListener {
                optionsPopup("apache", Services_service6_ServiceStatus.text.toString().replace("\n", ""))
                true
            }
    }

    private fun service_sql() {
        val serviceName = resources.getStringArray(R.array.sql_names)[defaultSharedPreferences.getInt("distro_value", 0)]
        this.runOnUiThread { if (Services_service7_ServiceName != null) Services_service7_ServiceName.text = serviceName }
        thread {
            var Status = main.ssh.shellChannel("service=$serviceName; $statusCommand", true, true).toString()
            if (isVisible && !Status.contains("Running", true)) {
                when (defaultSharedPreferences.getString("Service 7", "")) {
                    "Installing...", "Restarting...", "Stopping...", "Starting...", "Uninstalling..." -> {
                        Status = defaultSharedPreferences.getString("Service 7", "")
                    }
                }
                this.runOnUiThread {
                    if (Services_service7_PidValue != null) Services_service7_PidValue.text = "N/A"
                    if (Services_service7_PortValue != null) Services_service7_PortValue.text = "N/A"
                }
            } else {
                if (isVisible) {
                    val Pid = main.ssh.shellChannel("IN=\$(ps aux | grep ${resources.getStringArray(R.array.sql_pids)[defaultSharedPreferences.getInt("distro_value", 0)]} | grep -v grep | echo \$(xargs -L1 echo|awk '{ print \$2}'));echo \$IN | sed 's/ /,/g'", true, true).toString()
                    this.runOnUiThread { if (Services_service7_PidValue != null) Services_service7_PidValue.text = Pid.replace("\n", "") }
                }
            }
            this.runOnUiThread { if (Services_service7_ServiceStatus != null) Services_service7_ServiceStatus.text = Status.replace("\n", "") }
            bcolor(Services_service7_LinearLayout, Status.replace("\n", ""))
        }

        if (Services_service7_LinearLayout != null)
            Services_service7_LinearLayout.setOnLongClickListener {
                optionsPopup("sql", Services_service7_ServiceStatus.text.toString().replace("\n", ""))
                true
            }
    }

    private fun bcolor(llayout: LinearLayout?, status: String) {
        if (llayout != null) {
            Log.e("bcolor", "status")
            when (status) {
                "Running" -> {
                    if (isVisible)
                        this.runOnUiThread { llayout.backgroundColor = ContextCompat.getColor(activity, R.color.colorBGreen) }
                    Log.e("bcolor", "green")
                }
                "Not running" -> {
                    if (isVisible)
                        this.runOnUiThread { llayout.backgroundColor = ContextCompat.getColor(activity, R.color.transparent) }
                    Log.e("bcolor", "transparent")
                }
                "Not installed" -> {
                    if (isVisible)
                        this.runOnUiThread { llayout.backgroundColor = ContextCompat.getColor(activity, R.color.colorBBlue) }
                    Log.e("bcolor", "blue")
                }
                "Installing..." -> {
                    if (isVisible)
                        this.runOnUiThread { llayout.backgroundColor = ContextCompat.getColor(activity, R.color.colorBLime) }
                    Log.e("bcolor", "lime")
                }
                "Uninstalling..." -> {
                    if (isVisible)
                        this.runOnUiThread { llayout.backgroundColor = ContextCompat.getColor(activity, R.color.colorBLime) }
                    Log.e("bcolor", "lime")
                }
                "Starting..." -> {
                    if (isVisible)
                        this.runOnUiThread { llayout.backgroundColor = ContextCompat.getColor(activity, R.color.colorBLime) }
                    Log.e("bcolor", "lime")
                }
                "Stopping..." -> {
                    if (isVisible)
                        this.runOnUiThread { llayout.backgroundColor = ContextCompat.getColor(activity, R.color.colorBLime) }
                    Log.e("bcolor", "lime")
                }
                "Restarting..." -> {
                    if (isVisible)
                        this.runOnUiThread { llayout.backgroundColor = ContextCompat.getColor(activity, R.color.colorBLime) }
                    Log.e("bcolor", "lime")
                }
            }
        }
    }


    private fun installService(service: String) {
        val status = "Installing..."
        Log.e("Services", status)
        thread {
            val command: String
            when (service) {
                resources.getStringArray(R.array.docker_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                    command = resources.getStringArray(R.array.docker_install_commands)[defaultSharedPreferences.getInt("distro_value", 0)]
                    this.runOnUiThread { if (Services_service3_ServiceStatus != null) Services_service3_ServiceStatus.text = status }
                    defaultSharedPreferences.edit().putString("Service 3", status).apply()
                    bcolor(Services_service3_LinearLayout, status)
                    main.ssh.shellChannel(command)
                    defaultSharedPreferences.edit().putString("Service 3", "").apply()
                }
                resources.getStringArray(R.array.libvirt_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                    command = resources.getStringArray(R.array.libvirt_install_commands)[defaultSharedPreferences.getInt("distro_value", 0)]
                    this.runOnUiThread { if (Services_service2_ServiceStatus != null) Services_service2_ServiceStatus.text = status }
                    defaultSharedPreferences.edit().putString("Service 2", status).apply()
                    bcolor(Services_service2_LinearLayout, status)
                    main.ssh.shellChannel(command)
                    defaultSharedPreferences.edit().putString("Service 2", "").apply()
                }
                resources.getStringArray(R.array.nfs_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                    command = resources.getStringArray(R.array.nfs_install_commands)[defaultSharedPreferences.getInt("distro_value", 0)]
                    this.runOnUiThread { if (Services_service1_ServiceStatus != null) Services_service1_ServiceStatus.text = status }
                    defaultSharedPreferences.edit().putString("Service 1", status).apply()
                    bcolor(Services_service1_LinearLayout, status)
                    main.ssh.shellChannel(command)
                    defaultSharedPreferences.edit().putString("Service 1", "").apply()
                }
                resources.getStringArray(R.array.ftp_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                    command = resources.getStringArray(R.array.ftp_install_commands)[defaultSharedPreferences.getInt("distro_value", 0)]
                    this.runOnUiThread { if (Services_service5_ServiceStatus != null) Services_service5_ServiceStatus.text = status }
                    defaultSharedPreferences.edit().putString("Service 5", status).apply()
                    bcolor(Services_service5_LinearLayout, status)
                    main.ssh.shellChannel(command)
                    defaultSharedPreferences.edit().putString("Service 5", "").apply()
                }
                resources.getStringArray(R.array.samba_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                    command = resources.getStringArray(R.array.samba_install_commands)[defaultSharedPreferences.getInt("distro_value", 0)]
                    this.runOnUiThread { if (Services_service4_ServiceStatus != null) Services_service4_ServiceStatus.text = status }
                    defaultSharedPreferences.edit().putString("Service 4", status).apply()
                    bcolor(Services_service4_LinearLayout, status)
                    main.ssh.shellChannel(command)
                    defaultSharedPreferences.edit().putString("Service 4", "").apply()
                }
                resources.getStringArray(R.array.apache_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                    command = resources.getStringArray(R.array.apache_install_commands)[defaultSharedPreferences.getInt("distro_value", 0)]
                    this.runOnUiThread { if (Services_service6_ServiceStatus != null) Services_service6_ServiceStatus.text = status }
                    defaultSharedPreferences.edit().putString("Service 6", status).apply()
                    bcolor(Services_service6_LinearLayout, status)
                    main.ssh.shellChannel(command)
                    defaultSharedPreferences.edit().putString("Service 6", "").apply()
                }
                resources.getStringArray(R.array.sql_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                    command = resources.getStringArray(R.array.sql_install_commands)[defaultSharedPreferences.getInt("distro_value", 0)]
                    this.runOnUiThread { if (Services_service7_ServiceStatus != null) Services_service7_ServiceStatus.text = status }
                    defaultSharedPreferences.edit().putString("Service 7", status).apply()
                    bcolor(Services_service7_LinearLayout, status)
                    main.ssh.shellChannel(command)
                    defaultSharedPreferences.edit().putString("Service 7", "").apply()
                }

                else -> return@thread
            }


            Log.e("INSTALLING", command)

            defaultSharedPreferences.edit().putBoolean("Service ref", true).apply()
        }
    }

    private fun actionService(service: String, action: String) {
        val status: String
        when (action) {
            "restart" -> {
                status = "Restarting..."
            }
            "stop" -> {
                status = "Stopping..."
            }
            "start" -> {
                status = "Starting..."
            }
            else -> {
                Log.e("Services", "Wrong action!")
                return
            }
        }

        thread {
            val command = "systemctl $action $service"
            Log.e("Command: ", command)
            when (service) {
                resources.getStringArray(R.array.docker_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                    this.runOnUiThread { if (Services_service3_ServiceStatus != null) Services_service3_ServiceStatus.text = status }
                    defaultSharedPreferences.edit().putString("Service 3", status).apply()
                    bcolor(Services_service3_LinearLayout, status)
                    main.ssh.shellChannel(command)
                    defaultSharedPreferences.edit().putString("Service 3", "").apply()
                }
                resources.getStringArray(R.array.libvirt_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                    this.runOnUiThread { if (Services_service2_ServiceStatus != null) Services_service2_ServiceStatus.text = status }
                    defaultSharedPreferences.edit().putString("Service 2", status).apply()
                    bcolor(Services_service2_LinearLayout, status)
                    main.ssh.shellChannel(command)
                    defaultSharedPreferences.edit().putString("Service 2", "").apply()
                }
                resources.getStringArray(R.array.nfs_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                    this.runOnUiThread { if (Services_service1_ServiceStatus != null) Services_service1_ServiceStatus.text = status }
                    defaultSharedPreferences.edit().putString("Service 1", status).apply()
                    bcolor(Services_service1_LinearLayout, status)
                    main.ssh.shellChannel(command)
                    defaultSharedPreferences.edit().putString("Service 1", "").apply()
                }
                resources.getStringArray(R.array.ftp_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                    this.runOnUiThread { if (Services_service5_ServiceStatus != null) Services_service5_ServiceStatus.text = status }
                    defaultSharedPreferences.edit().putString("Service 5", status).apply()
                    bcolor(Services_service5_LinearLayout, status)
                    main.ssh.shellChannel(command)
                    defaultSharedPreferences.edit().putString("Service 5", "").apply()
                }
                resources.getStringArray(R.array.samba_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                    this.runOnUiThread { if (Services_service4_ServiceStatus != null) Services_service4_ServiceStatus.text = status }
                    defaultSharedPreferences.edit().putString("Service 4", status).apply()
                    bcolor(Services_service4_LinearLayout, status)
                    main.ssh.shellChannel(command)
                    defaultSharedPreferences.edit().putString("Service 4", "").apply()
                }
                resources.getStringArray(R.array.apache_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                    this.runOnUiThread { if (Services_service6_ServiceStatus != null) Services_service6_ServiceStatus.text = status }
                    defaultSharedPreferences.edit().putString("Service 6", status).apply()
                    bcolor(Services_service6_LinearLayout, status)
                    main.ssh.shellChannel(command)
                    defaultSharedPreferences.edit().putString("Service 6", "").apply()
                }
                resources.getStringArray(R.array.sql_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                    this.runOnUiThread { if (Services_service7_ServiceStatus != null) Services_service7_ServiceStatus.text = status }
                    defaultSharedPreferences.edit().putString("Service 7", status).apply()
                    bcolor(Services_service7_LinearLayout, status)
                    main.ssh.shellChannel(command)
                    defaultSharedPreferences.edit().putString("Service 7", "").apply()
                }

                else -> return@thread
            }

            Log.e("Action $action on", command)
            defaultSharedPreferences.edit().putBoolean("Service ref", true).apply()
        }
    }

    private fun uninstallService(service: String) {
        Log.e("UNINSTALLING", service)
        val status = "Uninstalling..."
        thread {
            val command: String
            when (service) {
                resources.getStringArray(R.array.docker_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                    command = resources.getStringArray(R.array.docker_uninstall_commands)[defaultSharedPreferences.getInt("distro_value", 0)]
                    this.runOnUiThread { if (Services_service3_ServiceStatus != null) Services_service3_ServiceStatus.text = status }
                    defaultSharedPreferences.edit().putString("Service 3", status).apply()
                    bcolor(Services_service3_LinearLayout, status)
                    main.ssh.shellChannel(command)
                    defaultSharedPreferences.edit().putString("Service 3", "").apply()
                }
                resources.getStringArray(R.array.libvirt_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                    command = resources.getStringArray(R.array.libvirt_uninstall_commands)[defaultSharedPreferences.getInt("distro_value", 0)]
                    this.runOnUiThread { if (Services_service2_ServiceStatus != null) Services_service2_ServiceStatus.text = status }
                    defaultSharedPreferences.edit().putString("Service 2", status).apply()
                    bcolor(Services_service2_LinearLayout, status)
                    main.ssh.shellChannel(command)
                    defaultSharedPreferences.edit().putString("Service 2", "").apply()
                }
                resources.getStringArray(R.array.nfs_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                    command = resources.getStringArray(R.array.nfs_uninstall_commands)[defaultSharedPreferences.getInt("distro_value", 0)]
                    this.runOnUiThread { if (Services_service1_ServiceStatus != null) Services_service1_ServiceStatus.text = status }
                    defaultSharedPreferences.edit().putString("Service 1", status).apply()
                    bcolor(Services_service1_LinearLayout, status)
                    main.ssh.shellChannel(command)
                    defaultSharedPreferences.edit().putString("Service 1", "").apply()
                }
                resources.getStringArray(R.array.ftp_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                    command = resources.getStringArray(R.array.ftp_uninstall_commands)[defaultSharedPreferences.getInt("distro_value", 0)]
                    this.runOnUiThread { if (Services_service5_ServiceStatus != null) Services_service5_ServiceStatus.text = status }
                    defaultSharedPreferences.edit().putString("Service 5", status).apply()
                    bcolor(Services_service5_LinearLayout, status)
                    main.ssh.shellChannel(command)
                    defaultSharedPreferences.edit().putString("Service 5", "").apply()
                }
                resources.getStringArray(R.array.samba_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                    command = resources.getStringArray(R.array.samba_uninstall_commands)[defaultSharedPreferences.getInt("distro_value", 0)]
                    this.runOnUiThread { if (Services_service4_ServiceStatus != null) Services_service4_ServiceStatus.text = status }
                    defaultSharedPreferences.edit().putString("Service 4", status).apply()
                    bcolor(Services_service4_LinearLayout, status)
                    main.ssh.shellChannel(command)
                    defaultSharedPreferences.edit().putString("Service 4", "").apply()
                }
                resources.getStringArray(R.array.apache_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                    command = resources.getStringArray(R.array.apache_uninstall_commands)[defaultSharedPreferences.getInt("distro_value", 0)]
                    this.runOnUiThread { if (Services_service6_ServiceStatus != null) Services_service6_ServiceStatus.text = status }
                    defaultSharedPreferences.edit().putString("Service 6", status).apply()
                    bcolor(Services_service6_LinearLayout, status)
                    main.ssh.shellChannel(command)
                    defaultSharedPreferences.edit().putString("Service 6", "").apply()
                }
                resources.getStringArray(R.array.sql_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                    command = resources.getStringArray(R.array.sql_uninstall_commands)[defaultSharedPreferences.getInt("distro_value", 0)]
                    this.runOnUiThread { if (Services_service7_ServiceStatus != null) Services_service7_ServiceStatus.text = status }
                    defaultSharedPreferences.edit().putString("Service 7", status).apply()
                    bcolor(Services_service7_LinearLayout, status)
                    main.ssh.shellChannel(command)
                    defaultSharedPreferences.edit().putString("Service 7", "").apply()
                }

                else -> return@thread
            }

            Log.e("Uninstalled", service)
            defaultSharedPreferences.edit().putBoolean("Service ref", true).apply()
        }
    }

    private fun refresh(module: String) {
        Log.e("Refresh", module)
        when (module) {
            resources.getStringArray(R.array.docker_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                service_docker()
            }
            resources.getStringArray(R.array.libvirt_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                service_libvirt()
            }
            resources.getStringArray(R.array.nfs_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                service_nfs()
            }
            resources.getStringArray(R.array.ftp_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                service_ftp()
            }
            resources.getStringArray(R.array.samba_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                service_samba()
            }
            resources.getStringArray(R.array.apache_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                service_apache()
            }
            resources.getStringArray(R.array.sql_names)[defaultSharedPreferences.getInt("distro_value", 0)] -> {
                service_sql()
            }
        }
    }

    private fun optionsPopup(module: String, status: String) {
        val dist: Array<String>
        val build = AlertDialog.Builder(activity)
        var service = ""
        when (module) {
            "samba" -> {
                service = resources.getStringArray(R.array.samba_names)[defaultSharedPreferences.getInt("distro_value", 0)]
            }
            "ftp" -> {
                service = resources.getStringArray(R.array.ftp_names)[defaultSharedPreferences.getInt("distro_value", 0)]
            }
            "libvirt" -> {
                service = resources.getStringArray(R.array.libvirt_names)[defaultSharedPreferences.getInt("distro_value", 0)]
            }
            "nfs" -> {
                service = resources.getStringArray(R.array.nfs_names)[defaultSharedPreferences.getInt("distro_value", 0)]
            }
            "docker" -> {
                service = resources.getStringArray(R.array.docker_names)[defaultSharedPreferences.getInt("distro_value", 0)]
            }
            "apache" -> {
                service = resources.getStringArray(R.array.apache_names)[defaultSharedPreferences.getInt("distro_value", 0)]
            }
            "sql" -> {
                service = resources.getStringArray(R.array.sql_names)[defaultSharedPreferences.getInt("distro_value", 0)]
            }
        }

        if (status.contains("not running", true)) {
            dist = resources.getStringArray(R.array.services_options_not_running)
            build.setTitle(module + " options")
                    .setCancelable(true)
                    .setItems(dist, DialogInterface.OnClickListener { _, i ->
                        when (i) {
                            0 -> {

                                //Start
                                vibrate(28, 1)
                                //resources.getStringArray(R.array.samba_names)[defaultSharedPreferences.getInt("distro_value", 0)]
                                //actionService(module, "start")
                                actionService(service, "start")
                            }
                            1 -> {
                                //Config
                                vibrate(28, 1)
                                configSelector(module)
                            }
                            2 -> {
                                //Wypierdol
                                vibrate(28, 1)
                                uninstallService(service)
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
        } else if (status.contains("not installed", true)) {
            dist = resources.getStringArray(R.array.services_options_uninstalled)
            build.setTitle(module + " options")
                    .setCancelable(true)
                    .setItems(dist, DialogInterface.OnClickListener { _, i ->
                        when (i) {
                            0 -> {
                                //Install
                                vibrate(28, 1)
                                installService(service)
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
        } else {
            dist = resources.getStringArray(R.array.services_options_running)
            build.setTitle(module + " options")
                    .setCancelable(true)
                    .setItems(dist, DialogInterface.OnClickListener { _, i ->
                        when (i) {
                            0 -> {
                                //Stop
                                vibrate(28, 1)
                                actionService(service, "stop")
                            }
                            1 -> {
                                //Restart
                                vibrate(28, 1)
                                actionService(service, "restart")
                            }
                            2 -> {
                                //Config
                                vibrate(28, 1)
                                configSelector(module)
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
    }

    private fun configSelector(module: String) {
        Log.e("Services", "Config: $module")
        when {
            module.contains("libvirt") -> {
                val intent = Intent()
                intent.setClassName(activity, LibvirtActivity::class.java.name)
                runLoop = false
                startActivity(intent)
            }
            module.contains("samba") -> {
                val intent = Intent()
                intent.setClassName(activity, sambaActivity::class.java.name)
                runLoop = false
                startActivity(intent)
            }
        }
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
        inflater?.inflate(R.menu.refresh, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_refresh -> {
                loadServices()
                vibrate(28, 1)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        runLoop = true
        autoRefresh()
    }

    private fun vibrate(msec: Long, amplitude: Int) {
        val vib = activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26)
            vib.vibrate(VibrationEffect.createOneShot(msec, amplitude))
        else
            vib.vibrate(msec)
    }
}