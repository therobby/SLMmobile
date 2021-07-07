package com.karel.slmmobile

import android.animation.LayoutTransition
import android.app.Fragment
import android.content.Context
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
import android.widget.TableLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_processes.*
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.textColor
import kotlin.concurrent.thread

/**
 * Created by Karel on 14.05.2018.
 */
class ProcessesFragment : Fragment() {

    private val tv = TypedValue()

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //return super.onCreateView(inflater, container, savedInstanceState)

        return inflater?.inflate(R.layout.fragment_processes, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        Processes_layout.layoutTransition = LayoutTransition()

        loadProcesses()
    }

    private fun refresh() {
        if (Processes_layout != null) {
            Processes_layout.removeAllViewsInLayout()
        }
        thread {
            Thread.sleep(200)
            loadProcesses()
        }
    }

    private fun loadProcesses() {
        thread {
            var output: List<String>
            try {
                output = main.ssh.shellChannel("ps -o user,pid,cmd -ax | grep -v \"user,pid,cmd\" | grep -v grep").split("\n").drop(1)
            } catch (e: Exception) {
                output = listOf("")
            }

            if (output.size > 2) {
                try {
                    for (i in 0 until output.size) {
                        if (!output[i].isBlank()) {
                            val parent = LinearLayout(activity)
                            parent.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            parent.orientation = LinearLayout.VERTICAL
                            parent.background = ContextCompat.getDrawable(activity, R.drawable.button)
                            parent.isClickable = true
                            parent.isFocusable = true
                            if (i % 2 == 0) {
                                activity.theme.resolveAttribute(android.R.attr.colorForeground, tv, true)
                                parent.backgroundColor = tv.data
                            }


                            var line = output[i]
                            var flag = 0
                            Log.e("Process", line)
                            val user = line.takeWhile { flag++;it != ' ' }

                            if (flag > 0)
                                flag--
                            Log.e("Process1_drop", flag.toString())
                            line = line.removeRange(0, flag)
                            flag = 0

                            line.takeWhile { flag++; !it.isDigit() }

                            if (flag > 0)
                                flag--
                            line = line.removeRange(0, flag)
                            flag = 0

                            val pid = line.takeWhile { flag++;it != ' ' }

                            if (flag > 0)
                                flag--
                            Log.e("Process2_drop", flag.toString())
                            line = line.removeRange(0, flag)
                            flag = 0

                            line.takeWhile { flag++; it != ' ' }
                            if (flag > 0)
                                flag--
                            line = line.removeRange(0, flag)
                            flag = 0

                            val cmd = line

                            parent.setOnClickListener {
                                val intent = Intent()
                                intent.setClassName(activity, ProcessActivity::class.java.name)
                                intent.putExtra("User", user)
                                intent.putExtra("Pid", pid)
                                intent.putExtra("Cmd", cmd)
                                startActivityForResult(intent, 69)
                            }

                            val layout = LinearLayout(activity)

                            layout.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            layout.orientation = LinearLayout.HORIZONTAL

                            activity.theme.resolveAttribute(android.R.attr.textColorPrimary, tv, true)

                            val txt = TextView(activity)

                            Log.e("Process1", user)
                            txt.tag = "process_user_$i"
                            txt.text = user
                            txt.textSize = 22f
                            txt.textColor = tv.data
                            txt.maxLines = 1
                            txt.gravity = Gravity.START
                            txt.layoutParams = TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f)

                            val txt2 = TextView(activity)

                            Log.e("Process2", pid)
                            txt2.tag = "process_pid_$i"
                            txt2.text = pid
                            txt2.textSize = 18f
                            txt2.textColor = tv.data
                            txt2.maxLines = 1
                            txt2.gravity = Gravity.END
                            txt2.layoutParams = TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f)


                            val txt3 = TextView(activity)

                            Log.e("Process3", cmd)
                            txt3.tag = "process_cmd_$i"
                            txt3.text = cmd
                            txt3.textSize = 18f
                            txt3.textColor = tv.data
                            txt3.maxLines = 1
                            txt3.gravity = Gravity.START
                            txt3.layoutParams = TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)


                            layout.addView(txt)
                            layout.addView(txt2)
                            parent.addView(layout)
                            parent.addView(txt3)

                            this.runOnUiThread { if (Processes_layout != null) Processes_layout.addView(parent) }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Processes_tab", "$e")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            69 -> {
                Log.e("Res", "1")
                refresh()
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

    /*override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
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
        return super.onPrepareOptionsMenu(menu)
    }*/

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.refresh, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_refresh -> {
                refresh()
                vibrate(28, 1)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun vibrate(msec: Long, amplitude: Int) {
        val vib = activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26)
            vib.vibrate(VibrationEffect.createOneShot(msec, amplitude))
        else
            vib.vibrate(msec)
    }
}