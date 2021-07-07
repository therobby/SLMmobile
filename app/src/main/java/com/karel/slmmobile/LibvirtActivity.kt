package com.karel.slmmobile

import android.animation.LayoutTransition
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v4.content.ContextCompat
import android.support.v7.view.menu.MenuBuilder
import android.support.v7.widget.Toolbar
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_libvirt.*
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.textColor
import kotlin.concurrent.thread

class LibvirtActivity : AppCompatActivity() {

    private val tv = TypedValue()
    private val util = AppUtils(this)

    override fun onCreate(savedInstanceState: Bundle?) {

        util.changeTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_libvirt)

        val toolbar = findViewById<Toolbar>(R.id.vms_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        vms_LinearLayout.layoutTransition = LayoutTransition()


        pleaseWait()
        list()
    }

    private fun pleaseWait(){
        val parent = LinearLayout(this)
        parent.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        parent.orientation = LinearLayout.VERTICAL
        parent.background = ContextCompat.getDrawable(this, R.drawable.button)
        parent.isClickable = true
        parent.isFocusable = true

        //theme.resolveAttribute(android.R.attr.textColorPrimary,tv,true)

        val txt = TextView(this)
        txt.text = resources.getString(R.string.please_wait)
        txt.textSize = 22f
        //txt.textColor = tv.data
        txt.width = 1
        txt.gravity = Gravity.START
        txt.layoutParams = TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)

        parent.addView(txt)

        this.runOnUiThread { if(vms_LinearLayout != null) vms_LinearLayout.removeAllViewsInLayout() }
        if(vms_LinearLayout != null)
            vms_LinearLayout.addView(parent)
    }

    private fun list(){
        thread {
            if (main.ssh.root) {
                val listOutput = main.ssh.shellChannel("virsh list --all | wc -l").toString()
                Log.e("VMS", listOutput)
                val vmsCount = listOutput.takeWhile { it.isDigit() }.toInt() - 3
                val vmsName = ArrayList<String>()
                val vmsState = ArrayList<String>()

                if (vmsCount <= 0) {
                    val parent = LinearLayout(this)
                    parent.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    parent.orientation = LinearLayout.VERTICAL
                    parent.background = ContextCompat.getDrawable(this, R.drawable.button)
                    parent.isClickable = true
                    parent.isFocusable = true

                    val txt = TextView(this)

                    //theme.resolveAttribute(android.R.attr.textColorPrimary,tv,true)

                    txt.text = "No pussy detected!"
                    txt.textSize = 22f
                    //txt.textColor = tv.data
                    txt.width = 1
                    txt.gravity = Gravity.START
                    txt.layoutParams = TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)

                    parent.addView(txt)

                    this.runOnUiThread { if(vms_LinearLayout != null) vms_LinearLayout.removeAllViewsInLayout() }
                    this.runOnUiThread { if(vms_LinearLayout != null) vms_LinearLayout.addView(parent) }
                }
                else {
                    for (i in 1..vmsCount) {
                        vmsName.add(main.ssh.shellChannel("virsh list --name --all | sed -n ${i}p").toString().replace('\n', ' '))
                        vmsState.add(main.ssh.shellChannel("virsh domstate ${vmsName[i - 1]}").toString().replace('\n', ' '))
                    }

                    this.runOnUiThread { if(vms_LinearLayout != null) vms_LinearLayout.removeAllViewsInLayout() }
                    for (i in 1..vmsCount) {
                        val parent = LinearLayout(this)
                        parent.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        parent.orientation = LinearLayout.VERTICAL
                        parent.background = ContextCompat.getDrawable(this, R.drawable.button)
                        parent.isClickable = true
                        parent.isFocusable = true

                        if(i % 2 == 0) {
                            theme.resolveAttribute(android.R.attr.colorForeground, tv, true)
                            parent.backgroundColor = tv.data
                        }

                        //theme.resolveAttribute(android.R.attr.textColorPrimary,tv,true)

                        parent.setOnLongClickListener {
                            util.vibrate(28,1)
                            alert(vmsName[i - 1], vmsState[i - 1].contains("running"))
                            true
                        }

                        val txt = TextView(this)

                        Log.e("VMS", vmsName[i - 1])
                        txt.id = View.generateViewId()
                        txt.tag = "vms_name_$i"
                        txt.text = vmsName[i - 1]
                        txt.textSize = 22f
                        //txt.textColor = tv.data
                        txt.width = 1
                        txt.gravity = Gravity.START
                        txt.layoutParams = TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)

                        val txt2 = TextView(this)

                        if (vmsState[i - 1].contains("running")) {
                            parent.backgroundColor = ContextCompat.getColor(this, R.color.colorBGreen)
                        }

                        Log.e("VMS", vmsState[i - 1])
                        txt2.tag = "vms_state_$i"
                        txt2.text = vmsState[i - 1]
                        txt2.textSize = 18f
                        //txt2.textColor = tv.data
                        txt2.width = 1
                        txt2.gravity = Gravity.START
                        txt2.layoutParams = TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)

                        parent.addView(txt)
                        parent.addView(txt2)

                        this.runOnUiThread { if (vms_LinearLayout != null) vms_LinearLayout.addView(parent) }
                    }
                }
            } else {
                val parent = LinearLayout(this)
                parent.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                parent.orientation = LinearLayout.VERTICAL
                parent.background = ContextCompat.getDrawable(this, R.drawable.button)
                parent.isClickable = true
                parent.isFocusable = true

                val txt = TextView(this)

                txt.text = resources.getString(R.string.authorization_req)
                txt.textSize = 22f
                //txt.textColor = Color.BLACK
                txt.width = 1
                txt.gravity = Gravity.START
                txt.layoutParams = TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)

                parent.addView(txt)

                this.runOnUiThread { if(vms_LinearLayout != null) vms_LinearLayout.removeAllViewsInLayout() }
                this.runOnUiThread { if(vms_LinearLayout != null) vms_LinearLayout.addView(parent) }
            }
        }
    }

    private fun alert(virt_name: String, running: Boolean) {
        if (running) {
            val dist = resources.getStringArray(R.array.vms_options_running)
            val build = AlertDialog.Builder(this)
            build.setTitle(virt_name)
                    .setCancelable(true)
                    .setItems(dist, DialogInterface.OnClickListener { _, i ->
                        when (i) {
                            0 -> {
                                alert_s("virsh shutdown $virt_name")
                                util.vibrate(28,1)
                            }
                            1 -> {
                                alert_s("virsh reboot $virt_name")
                                util.vibrate(28,1)
                            }
                            2 -> {
                                alert_s("virsh destroy $virt_name")
                                util.vibrate(28,1)
                            }
                        }
                    })
                    .create()

            val data = TypedValue()
            theme.resolveAttribute(R.attr.colorPrimary, data, true)

            val d = build.show()
            d.findViewById<TextView>(resources.getIdentifier("android:id/alertTitle", null, null)).setTextColor(data.data)
            d.findViewById<TextView>(resources.getIdentifier("android:id/button1", null, null)).setTextColor(data.data)
            d.findViewById<TextView>(resources.getIdentifier("android:id/button2", null, null)).setTextColor(data.data)
            d.findViewById<TextView>(resources.getIdentifier("android:id/button3", null, null)).setTextColor(data.data)
        } else {
            val dist = resources.getStringArray(R.array.vms_options_not_running)
            val build = AlertDialog.Builder(this)
            build.setTitle(virt_name)
                    .setCancelable(true)
                    .setItems(dist, DialogInterface.OnClickListener { _, i ->
                        when (i) {
                            0 -> {
                                thread {
                                    main.ssh.shellChannel("virsh start $virt_name")
                                    list()
                                    util.vibrate(28,1)
                                }
                            }
                        }
                    })
                    .create()

            val data = TypedValue()
            theme.resolveAttribute(R.attr.colorPrimary, data, true)

            val d = build.show()
            d.findViewById<TextView>(resources.getIdentifier("android:id/alertTitle", null, null)).setTextColor(data.data)
            d.findViewById<TextView>(resources.getIdentifier("android:id/button1", null, null)).setTextColor(data.data)
            d.findViewById<TextView>(resources.getIdentifier("android:id/button2", null, null)).setTextColor(data.data)
            d.findViewById<TextView>(resources.getIdentifier("android:id/button3", null, null)).setTextColor(data.data)
        }
    }

    private fun alert_s(command: String) {
        val build = AlertDialog.Builder(this)
        build.setTitle(resources.getString(R.string.usure))
                .setCancelable(true)
                .setPositiveButton(resources.getString(R.string.button_yes), DialogInterface.OnClickListener { dialog, _ ->
                    thread {
                        main.ssh.shellChannel(command)
                        list()
                        util.vibrate(28,1)
                    }
                    dialog.dismiss()
                })
                .setNegativeButton(resources.getString(R.string.button_no), DialogInterface.OnClickListener { dialog, _ ->
                    dialog.dismiss()
                })
                .create()

        val data = TypedValue()
        theme.resolveAttribute(R.attr.colorPrimary, data, true)

        val d = build.show()
        d.findViewById<TextView>(resources.getIdentifier("android:id/alertTitle", null, null)).setTextColor(data.data)
        d.findViewById<TextView>(resources.getIdentifier("android:id/button1", null, null)).setTextColor(data.data)
        d.findViewById<TextView>(resources.getIdentifier("android:id/button2", null, null)).setTextColor(data.data)
        d.findViewById<TextView>(resources.getIdentifier("android:id/button3", null, null)).setTextColor(data.data)

    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.let {
            if(menu is MenuBuilder){
                try{
                    val f = menu.javaClass.getDeclaredField("mOptionalIconsVisible")
                    f.isAccessible = true
                    f.setBoolean(menu,true)
                }catch (e:Exception){}
                val data = TypedValue()
                theme.resolveAttribute(R.attr.colorAccent, data, true)

                for(item in 0 until menu.size()){
                    val menuItem = menu.getItem(item)
                    menuItem.icon.setColorFilter(data.data,PorterDuff.Mode.SRC_ATOP)
                }
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.refresh,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.menu_refresh ->{
                pleaseWait()
                list()
                util.vibrate(30,1)
            }
            android.R.id.home -> {
                onBackPressed()
                util.vibrate(30,1)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        main.ssh.activityStatus[3] = true   // jak się soć robi to by ssh nie wyłączyło
        if(!defaultSharedPreferences.getString("Theme","Default").contains(util.theme,true))
            recreate()
    }

    override fun onStop() {
        super.onStop()
        main.ssh.activityStatus[3] = false  // jak się przejdzie do background to by się ssh wyłączyło
    }

}
