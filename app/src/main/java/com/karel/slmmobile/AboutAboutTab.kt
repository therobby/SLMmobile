package com.karel.slmmobile

import android.os.Bundle
import android.support.v4.app.Fragment
import kotlinx.android.synthetic.main.about_tab1.*

class AboutAboutTab : Fragment()  {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        about_Line_2.text = resources.getString(R.string.simple_linux_manager_about)

        about_version_number.text = BuildConfig.VERSION_NAME
    }
}