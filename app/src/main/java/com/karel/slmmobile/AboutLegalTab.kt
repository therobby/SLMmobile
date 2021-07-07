package com.karel.slmmobile

import android.os.Bundle
import android.support.v4.app.Fragment
import kotlinx.android.synthetic.main.about_tab2.*

class AboutLegalTab : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        about_legal.text = resources.getString(R.string.legal)
    }

}