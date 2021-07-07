package com.karel.slmmobile

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.util.TypedValue
import android.widget.TextView

/**
 * Created by Karel on 26.04.2018.
 */
class AlertUSure : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val build = AlertDialog.Builder(activity)
        build.setTitle(resources.getString(R.string.warning))
                .setMessage(resources.getString(R.string.distro_warning_message))
                .setCancelable(true)
                .setPositiveButton(resources.getString(R.string.button_yes), DialogInterface.OnClickListener { dialog, _ ->

                    dialog.dismiss()
                })
                .setNegativeButton(resources.getString(R.string.button_no), DialogInterface.OnClickListener { dialog, _ ->

                    dialog.dismiss()
                })

        val r = build.create()


        val data = TypedValue()
        activity?.theme?.resolveAttribute(R.attr.colorPrimary, data, true)

        val d = build.show()
        d.findViewById<TextView>(resources.getIdentifier("android:id/alertTitle", null, null)).setTextColor(data.data)
        d.findViewById<TextView>(resources.getIdentifier("android:id/button1", null, null)).setTextColor(data.data)
        d.findViewById<TextView>(resources.getIdentifier("android:id/button2", null, null)).setTextColor(data.data)
        d.findViewById<TextView>(resources.getIdentifier("android:id/button3", null, null)).setTextColor(data.data)
        return r
    }

}