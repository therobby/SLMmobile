package com.karel.slmmobile

import android.animation.LayoutTransition
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PorterDuff
import android.opengl.Visibility
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.support.v4.content.ContextCompat
import android.support.v7.view.menu.MenuBuilder
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.util.LayoutDirection
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.*
import kotlinx.android.synthetic.main.activity_main_scene2.*
import kotlinx.android.synthetic.main.activity_samba.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onItemSelectedListener
import org.w3c.dom.Text
import kotlin.concurrent.thread

class sambaActivity : AppCompatActivity() {

    private val conf = Config() // klasa configu szamby
    private val files = ArrayList<String>() // pliki wykryte w folderku /tmp/slm/samba
    private var saveButton = false
    private var testparmButton = false
    private val util = AppUtils(this)

    override fun onCreate(savedInstanceState: Bundle?) {

        util.changeTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_samba)

        val toolbar = findViewById<Toolbar>(R.id.Samba_Toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        Samba_layout.layoutTransition = LayoutTransition()

        refreshSpinner()    // wykrywanie plików w folderku i wwalanie ich w spinerka (taki dropdown jak klikniesz to masz wybór czegoś tam)

        Samba_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {    // jak wybierzesz jakieś coś w spinerku
                util.vibrate(28, 1)
                conf.clear()    // to wyczyść dotychczasowo załadowane dane
                if (Samba_spinner.selectedItem.toString().contains("Add share", true)) {    // jak jest to nowy share
                    Log.e("Samba", "ADD SHARE")
                    conf.addTitle("New Share")
                    conf.newSharename()     // to zrób pole na nowy sharename
                    conf.createNewOption()  // i dodaj linijkę na wpisanie nowej opcji
                    conf.setView()          // i to wyświetl
                } else {
                    Log.e("Samba", "${Samba_spinner.selectedItemPosition} ${files[Samba_spinner.selectedItemPosition]}")
                    thread {
                        // jak nie to wyświetl to co jest w tym share
                        Looper.prepare()    // to gówno jest tu tylko dlatego że na części telefonków (np wspaniała moto z play) się o to pluje że nima i ni będzie działać
                        loadConfiguration(main.ssh.shellChannel("cat /tmp/slm/samba/${files[Samba_spinner.selectedItemPosition]}").toString())
                    }
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                // to jest tu tylko dlatego że musi.. nic poza tym nie robi (w sumie to nic nie robi)
            }
        }

        Samba_addoption.setOnClickListener {
            util.vibrate(28, 1)
            conf.addNext()  // dodaj kolejną opcję
        }

    }

    private fun refreshShareFiles() {
        // zrób pliki w folderze /tmp/slm/samba z smb.conf (każdy plik to pojedynczy share
        main.ssh.shellChannel("if [[ ! -d /tmp/slm/samba ]];then mkdir /tmp/slm/samba;fi; rm -f /tmp/slm/samba/*;" +
                " grep -n -e '\\[' /etc/samba/smb.conf |  grep -v global | grep -v homes | grep -v printers | grep -v 'print\\\$' | cut -d:" +
                " -f1 > /tmp/slm/shares; ilosc=\$(wc -l /tmp/slm/shares | cut -d\" \" -f1); for (( i=1; \$i<=\$ilosc; i++ ));do line1=\$(sed -n \"\$i\"p /tmp/slm/shares);" +
                " helper=\$((\$i + 1)); line2=\$(sed -n \"\$helper\"p /tmp/slm/shares); line2=\$((\$line2 - 1)); if [[ \$i == \$ilosc ]];then line2=\$(wc -l /etc/samba/smb.conf | cut -d\" \" -f1); fi;" +
                " sed -n \"\$line1\",\"\$line2\"p /etc/samba/smb.conf > /tmp/slm/samba/share_\$line1-\$line2; done ", true, true)

    }

    private fun refreshSpinner() {
        val shares = ArrayList<String>()    // tablica z nazwami sherów
        thread {
            refreshShareFiles()
            conf.clear()    // dla pewności wyczyść konfig
            files.clear()   // wycześć wczytane pliki
            main.ssh
                    .shellChannel("ls -A1 /tmp/slm/samba/", true, true)
                    .toString()
                    .split("\n")
                    .forEach { if (it.isNotBlank()) files.add(it) }
            // pojebana komenda która wykonuje komendę, zbiera output(ilość plików[sherów]), przerabia w tablicę stringów i dla każdego elementu sprawdza czy nie jest pusty.
            // Jak nie jest to dodaje do plików.

            for (i in files) {  // dla każdego shera bierze jego nazwę bez [] i dodaje do tablicy z nazwami sherów
                Log.e("Samba", "File: $i")
                shares.add(main.ssh.shellChannel("sed -n 1p /tmp/slm/samba/$i").toString().replace("[", "").replace("]", "").replace(" ", ""))
            }

            shares.add("Add share") // dodaje na koniec opcję dodania własnego shera

            val adapter = ArrayAdapter<String>(this, R.layout.sipn_l2, shares)  // takie gówno potrzebne do dodania elementów w spinerku
            adapter.setDropDownViewResource(R.layout.sipn_l2)  // to co wyżej
            runOnUiThread { Samba_spinner.adapter = adapter }   // dodawanko elementów do spinerka (czemu spinner nie może mieć czegoś takiego jak .add() !?)
        }
    }

    private fun loadConfiguration(shareData: String) {
        // wczytujemy opcje z shera
        val data = shareData.split("\n")    // przerabiamy na tablicę (do tego trzeba zrobić nową zmienną bo arg w funkcjach/metodach w kotlinie są constant i uj)

        conf.addTitle(data[0].drop(1).dropLast(1), files[Samba_spinner.selectedItemPosition])  // dodaj tytuła o nazwie shera
        conf.addOption("Share name", data[0].drop(1).dropLast(1))   // dodaj opcję zmiany nazwy tego shera


        for (i in 1 until data.size) {  // dodaj resztę opcji
            conf.addOption(data[i].takeWhile { it != '=' }, data[i].takeLastWhile { it != '=' }/*.drop(1)*/)
            Log.e("Samba", "Added $i/${data.size} element\n${data[i]}")
            Log.e("Samba", "${data[i].takeWhile { it != '=' }}, ${data[i].takeLastWhile { it != '=' }}")
        }

        Log.e("Samba", "Setting elements")
        conf.setView()  // wyświetl
        Log.e("Samba", "Done")

    }

    // klasa od configa szamby
    inner class Config {
        private val tv = TypedValue()
        private val title = ArrayList<LinearLayout>()  // nazwa shera, tablica jest tylko dla zachowania kompatybilności
        private val newConfig = ArrayList<LinearLayout>()   // zawiera noew wpisy konfiguracji
        private val editable = ArrayList<LinearLayout>()    // zawiera wszystkie te editTexty i textViewy
        private val clickable = ArrayList<LinearLayout>()   // zawiera wszystkie checkBoxy
        private val toDelete = ArrayList<String>()    // zawiera elementy do usunięcia z shera

        // poebana metodka do dodawania gówna do szamba(opcji do samby)
        fun addOption(option_: String, value_: String = "") {
            if (option_.isEmpty())  // jak pusta opcja to ni rób nic
                return
            val option = option_.dropWhile { it == ' ' }.dropLastWhile { it == ' ' }    // nazwa opcji
            val value = value_.dropWhile { it == ' ' }.dropLastWhile { it == ' ' }     // wartość tej opcji

            // tu jest ciekawie, na opcji wykonaj: jeśli zawiera [te opcje niżej contanis("blablalba",true) to dodaj checkboxa, jak nie to editView,
            // a jak jest pusta(wątpię, ale wolę być pewny bo dziwne rzeczy się tu działy) to nic nie rób (ignoreCase true oznacza że nie zwraca uwagi na wielkość liter)

            val checkable = resources.getStringArray(R.array.samba_options_checkable)

            option.apply {
                when {
                    option == "path" -> {
                        val path = util.pathSelector()
                        val layout = editable.last()
                        Log.e("Samba", path)
                        if (path != "drop") {
                            val textView = TextView(this@sambaActivity)
                            textView.text = path
                            textView.textSize = 22f
                            val textName = TextView(this@sambaActivity)
                            textName.text = "path"
                            textName.textSize = 22f
                            runOnUiThread {
                                layout.removeAllViewsInLayout()
                                layout.addView(textName)
                                layout.addView(textView)
                                textView.setOnLongClickListener {
                                    deleteOptionPopup(layout, editable)
                                    true
                                }
                            }
                        }
                    }
                    checkable.contains(option) -> {
                        createLayout(clickable)
                        value.apply {
                            when {
                            // jak zawiera yes lub true to dodaj zaznaczonego checkboxa
                                contains("yes", true) || contains("true", true) -> {
                                    val box = checkBox(option, true)
                                    val layout = clickable.last()
                                    var deleted = false
                                    box.setOnLongClickListener {
                                        if (!deleted) {
                                            util.vibrate(28, 1)
                                            val build = AlertDialog.Builder(this@sambaActivity)
                                            build.setTitle(resources.getString(R.string.delete))
                                                    .setCancelable(false)
                                                    .setPositiveButton(resources.getString(R.string.button_yes), DialogInterface.OnClickListener { dialog, _ ->
                                                        util.vibrate(28, 1)
                                                        dialog.dismiss()
                                                        layout.backgroundColor = ContextCompat.getColor(this@sambaActivity, R.color.colorBRed)
                                                        deleted = true
                                                        toDelete.add(option)

                                                    })
                                                    .setNegativeButton(resources.getString(R.string.button_no), DialogInterface.OnClickListener { dialog, _ ->
                                                        // jak nie to zamknij popup
                                                        util.vibrate(28, 1)
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
                                        true
                                    }
                                    clickable.last().addView(box)
                                    clickable.last().addView(createSeparator())
                                }
                            // jak nie to dodaj odznaczonego
                                else -> {
                                    clickable.last().addView(checkBox(option, false))
                                    clickable.last().addView(createSeparator())
                                }
                            }
                        }
                    }
                    this.isBlank() -> return
                    else -> {
                        createLayout(editable)
                        // dodaj editView
                        val txtV = textView(option)
                        val editT = editText(value)
                        val layout = editable.last()
                        var deleted = false
                        txtV.setOnLongClickListener {
                            if (!deleted) {
                                util.vibrate(28, 1)
                                val build = AlertDialog.Builder(this@sambaActivity)
                                build.setTitle(resources.getString(R.string.delete))
                                        .setCancelable(false)
                                        .setPositiveButton(resources.getString(R.string.button_yes), DialogInterface.OnClickListener { dialog, _ ->
                                            util.vibrate(28, 1)
                                            dialog.dismiss()
                                            layout.backgroundColor = ContextCompat.getColor(this@sambaActivity, R.color.colorBRed)
                                            deleted = true
                                            toDelete.add(option)

                                        })
                                        .setNegativeButton(resources.getString(R.string.button_no), DialogInterface.OnClickListener { dialog, _ ->
                                            // jak nie to zamknij popup
                                            util.vibrate(28, 1)
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
                            true
                        }
                        editT.setOnLongClickListener {
                            if (!deleted) {
                                util.vibrate(28, 1)
                                val build = AlertDialog.Builder(this@sambaActivity)
                                build.setTitle(resources.getString(R.string.delete))
                                        .setCancelable(false)
                                        .setPositiveButton(resources.getString(R.string.button_yes), DialogInterface.OnClickListener { dialog, _ ->
                                            util.vibrate(28, 1)
                                            dialog.dismiss()
                                            layout.backgroundColor = ContextCompat.getColor(this@sambaActivity, R.color.colorBRed)
                                            deleted = true
                                            toDelete.add(option)

                                        })
                                        .setNegativeButton(resources.getString(R.string.button_no), DialogInterface.OnClickListener { dialog, _ ->
                                            // jak nie to zamknij popup
                                            util.vibrate(28, 1)
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
                            true
                        }

                        editable.last().addView(txtV)
                        editable.last().addView(editT)
                        //editable.last().addView(createSeparator())
                    }
                }
            }
        }

        // dodaje tytuła do wyświetlenia
        fun addTitle(title: String, file: String = "") {
            createLayout(this.title)
            if (file.isNotBlank()) {
                this.title.last().setOnLongClickListener {
                    deleteOptionPopup(this.title.last(), this.title, file)
                    true
                }
            }
            this.title.last().addView(textView(title, Gravity.CENTER))
            this.title.last().addView(createSeparator())
        }

        // czyści wyświetlany konfig
        fun clear() {
            clearLayout()
            editable.clear()
            title.clear()
            newConfig.clear()
            clickable.clear()
            toDelete.clear()
        }

        // czyści tylko to co widać na ekranie
        private fun clearLayout() {
            runOnUiThread { Samba_layout.removeAllViews() }
        }

        // wypisuje na ekran
        fun setView() {
            runOnUiThread {
                Samba_layout.addView(title.last())
                if (editable.isNotEmpty())
                    editable.forEach { Samba_layout.addView(it) }
                if (newConfig.isNotEmpty())
                    newConfig.forEach { Samba_layout.addView(it) }
                if (clickable.isNotEmpty())
                    clickable.forEach { Samba_layout.addView(it) }
            }
        }

        // tworzy nowego separatorka
        private fun createSeparator(): LinearLayout {
            val separator = LinearLayout(this@sambaActivity)    // ta czerna linia oddzielająca opcje

            separator.tag = "Samba_separator"
            separator.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
            separator.orientation = LinearLayout.HORIZONTAL
            separator.backgroundColor = ContextCompat.getColor(this@sambaActivity, R.color.colorBBlack)

            return separator
        }

        // tworzy nowy layout
        private fun createLayout(type: ArrayList<LinearLayout>, vertical: Boolean = true) {
            val layout = LinearLayout(this@sambaActivity)
            layout.id = View.generateViewId()
            layout.tag = "Samba_layout_${editable.size}"
            layout.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            if (vertical)
                layout.orientation = LinearLayout.VERTICAL
            else
                layout.orientation = LinearLayout.HORIZONTAL
            //theme.resolveAttribute(android.R.attr.colorForeground,tv,true)
            //layout.backgroundColor = tv.data //ContextCompat.getDrawable(this@sambaActivity, R.drawable.button)
            layout.isClickable = true
            layout.isFocusable = true
            type.add(layout)
        }

        // zrób nowy textView
        private fun textView(option: String, gravity: Int = Gravity.START, parms: TableLayout.LayoutParams = TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)): TextView {  //optionName
            val txt = TextView(this@sambaActivity)

            theme.resolveAttribute(android.R.attr.textColorPrimary, tv, true)

            txt.tag = "Samba_textView_${editable.size}"
            txt.id = View.generateViewId()
            txt.text = option
            txt.textSize = 22f
            txt.textColor = tv.data
            txt.maxLines = 1
            //txt.width = 1
            txt.gravity = gravity   //Gravity.START
            txt.layoutParams = parms

            //runOnUiThread { editable.last().addView(txt) }
            return txt
        }

        // zrób nowy editText
        private fun editText(value: String, parms: TableLayout.LayoutParams = TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)): EditText {

            Log.e("Samba", "EditText: $value")
            val txt = EditText(this@sambaActivity)

            theme.resolveAttribute(android.R.attr.textColorPrimary, tv, true)

            txt.id = View.generateViewId()
            txt.tag = "Samba_editText_${editable.size}"
            txt.setText(value)
            txt.textSize = 18f
            txt.textColor = tv.data
            txt.maxLines = 1
            txt.gravity = Gravity.START
            txt.layoutParams = parms

            //runOnUiThread { editable.last().addView(txt2) }
            return txt
        }

        //zrób nowy checkBox
        private fun checkBox(option: String, checked: Boolean = false, parms: TableLayout.LayoutParams = TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)): CheckBox {    //checkText
            val txt = CheckBox(this@sambaActivity)

            theme.resolveAttribute(android.R.attr.textColorPrimary, tv, true)

            txt.id = View.generateViewId()
            txt.tag = "Samba_checkBox_${clickable.size}"
            txt.isChecked = checked
            txt.text = option
            txt.textSize = 22f
            txt.textColor = tv.data
            txt.setPadding(0, 0, 7, 0)
            txt.layoutDirection = View.LAYOUT_DIRECTION_RTL
            txt.gravity = Gravity.START
            txt.layoutParams = parms

            //runOnUiThread { clickable.last().addView(txt2) }
            return txt
        }

        // zwróć text z EditTexta lub TextViewa (jak nie jest to ani to, ani to to zwróć null)
        private fun getText(text: Any): String? {
            when (text) {
                is EditText -> return text.text.toString()
                is TextView -> return text.text.toString()
            }
            return null
        }

        // to je poebana metoda zwracająca paczkę (tablicę) shera (zawiera na kolejnych pozycjach: [sharename], opcja, opcja, itd...)
        fun getData(): ArrayList<String> {
            val data = ArrayList<String>()  // paczucha na dane
            //Log.e("Samba", editable.toString())
            editable.forEach {
                if (it.getChildAt(0) != null) {
                    val txt1 = getText(it.getChildAt(0))
                    val txt2 = getText(it.getChildAt(1))
                    if (txt1 != null && txt2 != null && txt1.contains("share name", true))
                        data.add("[$txt2]")
                    else if (txt1 != null && txt2 != null)
                        data.add("$txt1 = $txt2")
                }
            }
            Log.e("Samba", newConfig.toString())
            newConfig.forEach {
                if (it.getChildAt(0) != null) {
                    val child = it.getChildAt(0) as LinearLayout
                    var txt1 = ""
                    var txt2 = ""
                    if (child.getChildAt(0) is Spinner)
                        txt1 = (child.getChildAt(0) as Spinner).selectedItem.toString()
                    else
                        txt1 = getText(child.getChildAt(0)) ?: ""
                    if (child.getChildAt(1) is CheckBox) {
                        txt2 = (child.getChildAt(1) as CheckBox).isChecked.toString()
                    } else {
                        txt2 = getText(child.getChildAt(1)) ?: ""
                    }
                    if (txt1.contains("share name", true))
                        data.add("[$txt2]")
                    else if (txt1.isNotEmpty() && txt2.isNotEmpty())
                        data.add("$txt1 = $txt2")

                }
            }
            clickable.forEach {
                if (it.getChildAt(0) != null && it.getChildAt(0) is CheckBox) {
                    val child = it.getChildAt(0) as CheckBox
                    data.add("${child.text} = ${child.isChecked}")
                }
            }

            data.forEach {
                if (it.isBlank())
                    data.remove(it)
            }

            // zwróć paczuchę
            return data
        }

        // jak tworzony jest nowy sharename to to się wykonuje (a przynajmniej powinno)
        fun newSharename() {

            createLayout(newConfig)
            val layout = LinearLayout(this@sambaActivity)

            //theme.resolveAttribute(android.R.attr.textColorPrimary,tv,true)

            layout.tag = "Samba_newOptionSharename"
            layout.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            layout.orientation = LinearLayout.HORIZONTAL
            //layout.backgroundColor = tv.data//ContextCompat.getDrawable(this@sambaActivity, R.drawable.button)
            layout.isClickable = true
            layout.isFocusable = true

            layout.addView(textView("Share name:", Gravity.START, TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)))  //120
            layout.addView(editText("", TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)))

            newConfig.last().addView(layout)
            //newConfig.last().addView(createSeparator())
        }

        // dodaj nową opcję
        fun createNewOption() {

            val checkable = resources.getStringArray(R.array.samba_options_checkable)

            createLayout(newConfig)
            val thisLayout = newConfig.last()
            val layout = LinearLayout(this@sambaActivity)

            //theme.resolveAttribute(android.R.attr.textColorPrimary,tv,true)

            layout.tag = "Samba_newOption"
            layout.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 110)//, ViewGroup.LayoutParams.MATCH_PARENT)
            layout.orientation = LinearLayout.HORIZONTAL
            //layout.backgroundColor = tv.data//ContextCompat.getDrawable(this@sambaActivity, R.drawable.button)
            layout.isClickable = true
            layout.isFocusable = true

            val spin = Spinner(this@sambaActivity)
            /*spin.layoutParams = TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            val adapter = ArrayAdapter<String>(this@sambaActivity, android.R.layout.simple_spinner_item, fixcheckable + writeable)  // takie gówno potrzebne do dodania elementów w spinerku
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)  // to co wyżej
            spin.adapter = adapter     // dodawanko elementów do spinerka (czemu spinner nie może mieć czegoś takiego jak .add() !?)
            */

            val adapter = ArrayAdapter.createFromResource(this@sambaActivity, R.array.samba_spinner, R.layout.sipn_l)
            adapter.setDropDownViewResource(R.layout.sipn_l)
            spin.adapter = adapter

            layout.addView(spin)
            layout.addView(editText("", TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 110)))//, ViewGroup.LayoutParams.MATCH_PARENT)))


            spin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    util.vibrate(28, 1)
                    if (spin.selectedItem == "path") {
                        thread {
                            val path = util.pathSelector()
                            Log.e("Samba", path)
                            if (path != "drop") {
                                val textView = TextView(this@sambaActivity)
                                textView.text = path
                                textView.textSize = 22f
                                runOnUiThread {
                                    layout.removeAllViewsInLayout()
                                    layout.addView(spin)
                                    layout.addView(textView)
                                    textView.setOnLongClickListener {
                                        deleteOptionPopup(thisLayout, newConfig)
                                        true
                                    }
                                }
                            }
                        }
                    } else if (checkable.contains(spin.selectedItem)) {
                        layout.removeAllViewsInLayout()
                        layout.addView(spin)
                        val box = checkBox("", false, TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
                        box.setOnLongClickListener {
                            deleteOptionPopup(thisLayout, newConfig)
                            true
                        }
                        layout.addView(box)
                    } else {
                        layout.removeAllViewsInLayout()
                        layout.addView(spin)
                        val edit = editText("", TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
                        edit.setOnLongClickListener {
                            deleteOptionPopup(thisLayout, newConfig)
                            true
                        }
                        layout.addView(edit)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    //
                }
            }

            layout.setOnLongClickListener {
                deleteOptionPopup(thisLayout, newConfig)
                true
            }
            newConfig.last().addView(layout)
            //newConfig.last().addView(createSeparator())
        }

        private fun deleteOptionPopup(layout: LinearLayout, from: ArrayList<LinearLayout>, file: String = "") {
            val build = AlertDialog.Builder(this@sambaActivity)
            build.setTitle(resources.getString(R.string.delete))
                    .setCancelable(false)
                    .setPositiveButton(resources.getString(R.string.button_yes), DialogInterface.OnClickListener { dialog, _ ->
                        util.vibrate(28, 1)
                        dialog.dismiss()

                        if (from == newConfig) {
                            from.remove(layout)
                            Samba_layout.removeView(layout)
                            //clearLayout()
                            //setView()
                        } else if (from == title) {
                            thread {
                                main.ssh.shellChannel("echo > /tmp/slm/samba/$file")
                                updateSambaConfig(file, false)
                                refreshSpinner()
                            }
                        }
                    })
                    .setNegativeButton(resources.getString(R.string.button_no), DialogInterface.OnClickListener { dialog, _ ->
                        // jak nie to zamknij popup
                        util.vibrate(28, 1)
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

        // dodaj kolejną opcję
        fun addNext() {
            createNewOption()
            runOnUiThread { Samba_layout.addView(newConfig.last()) }
            //clearLayout()
            //setView()
        }

        // zapisz opcje do pliku
        fun save(file: String, dataPack: ArrayList<String>, new: Boolean = false) {
            // nie mam zamiaru wysilać się nad opisywaniem komend w main.ssh.shellChannel (po prostu zajeło by mi to troche czasu, a i tak bym tego nie ogarnął)

            if (new)
            // jak nowy share
                main.ssh.shellChannel("if [[ ! -f /tmp/slm/samba/$file ]];then echo > /tmp/slm/samba/$file;fi", true, true)
            else
            // jak modyfikacja istniejącego
                main.ssh.shellChannel("if [[ ! -f /tmp/slm/samba/$file ]];then touch /tmp/slm/samba/$file;fi", true, true)

            main.ssh.shellChannel("sed -i '1 s/^.*$/${dataPack[0]}/g' /tmp/slm/samba/$file", true, true)

            for (i in 1 until dataPack.size) {
                if (dataPack[i].isNotEmpty()) {
                    // dla każdej opcji która jest, wykonaj to coś niżej
                    main.ssh.shellChannel("val=\$(egrep -n -i \"^[[:blank:]]*${dataPack[i].takeWhile { it != '=' }}\" /tmp/slm/samba/$file | cut -d: -f1);" +
                            " if [[ ! -z \$val ]];then sed -i \"\$val\"' s#^.*\$#${dataPack[i].takeWhile { it != '=' }} = " +
                            "${dataPack[i].takeLastWhile { it != '=' }}#g' /tmp/slm/samba/$file; else echo \"${dataPack[i].takeWhile { it != '=' }} = " +
                            "${dataPack[i].takeLastWhile { it != '=' }}\" >> /tmp/slm/samba/$file;fi", true, true)
                }
            }
            toDelete.forEach {
                // jak jest jakaś opcja do usunięcia to wywal ją z pliku
                main.ssh.shellChannel("sed -i 's/^.*$it.*=.*\$//g' /tmp/slm/samba/$file", true, true)
            }

            // zaktualizuj konfig samby
            updateSambaConfig(file, new)
        }

        private fun updateSambaConfig(file: String, newShare: Boolean = false) {
            if (newShare) {
                // jak jest to nowy share
                main.ssh.shellChannel("echo >> /etc/samba/smb.conf; cat /tmp/slm/samba/$file >> /etc/samba/smb.conf")
            } else {
                // jak nie to
                val last = file.takeLastWhile { it.isDigit() }.toInt()  // ostatnia linia w smb.conf tego shera
                val first = file.dropLastWhile { it != '-' }.dropLast(1).takeLastWhile { it.isDigit() }.toInt() // pierwsza linia w smb.conf tego shera
                Log.e("Samba", "File $file INT: $first $last")

                main.ssh.shellChannel("sed -i '/^\$/d' /tmp/slm/samba/$file")
                main.ssh.shellChannel("sed -n 1,${first - 1}p /etc/samba/smb.conf > /tmp/slm/before;" +
                        "sed -n '${last + 1},\$p' /etc/samba/smb.conf > /tmp/slm/after", true, true)
                main.ssh.shellChannel("cat /tmp/slm/before > /tmp/slm/new_cfg; cat /tmp/slm/samba/$file >> /tmp/slm/new_cfg; echo >> /tmp/slm/new_cfg;" +
                        " cat /tmp/slm/after >> /tmp/slm/new_cfg; cat /tmp/slm/new_cfg > /etc/samba/smb.conf", true, true)
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.let {
            if (menu is MenuBuilder) {
                try {
                    val f = menu.javaClass.getDeclaredField("mOptionalIconsVisible")
                    f.isAccessible = true
                    f.setBoolean(menu, true)
                } catch (e: Exception) {
                }
                val data = TypedValue()
                theme.resolveAttribute(R.attr.colorAccent, data, true)

                for (item in 0 until menu.size()) {
                    val menuItem = menu.getItem(item)
                    menuItem.icon.setColorFilter(data.data, PorterDuff.Mode.SRC_ATOP)
                }
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.samba, menu)

        return super.onCreateOptionsMenu(menu)
    }


    // jak klikniesz szczałkę to się cofnij (to zachowanie jest takie jak byś kliknął androidowy wstecz)
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when (item?.itemId) {
            android.R.id.home -> {
                util.vibrate(28, 1)
                onBackPressed()
            }

            R.id.Samba_save -> {
                if (!saveButton) {
                    saveButton = true
                    util.vibrate(28, 1)
                    toast("Saving...")
                    val data = conf.getData()   // zrób paczkę zawierającą nowy konfig danego shera (lub nowy share)
                    thread {
                        //Log.e("Samba", "${Samba_spinner.selectedItemPosition} ${files.size}")
                        if (Samba_spinner.selectedItemPosition < files.size) {  // sprawdzanko czy wybrałeś istniejącego shera czy nowego
                            val file = files[Samba_spinner.selectedItemPosition]    // nazwa pliku to share_linia1-linia2 gdzie linia to linia w smb.config od-do
                            conf.save(file, data)   // jak modyfikowanko
                        } else {
                            val file = data[0]  // nazwa pliku to nazwa shera
                            conf.save(file.drop(1).dropLast(1), data, true)   // jak nowego
                        }
                        refreshSpinner()    //odświerz spinnerka
                        runOnUiThread { toast("Saved!") }
                        saveButton = false
                    }
                    Log.e("Samba", data.toString())
                }
            }

            R.id.Samba_testparm -> {
                if (!testparmButton) {
                    testparmButton = true
                    util.vibrate(28, 1)
                    thread {
                        val out = main.ssh.shellChannel("testparm -sl", true, true)
                        val message = ArrayList<String>()
                        val title: String
                        var toDisplay = ""
                        if (out.contains("Loaded services file OK", true)) {
                            message.add("Loaded services file OK!")
                            title = "Check OK!"
                        } else {
                            val tmp = out.split("\n")
                            tmp.forEach {
                                Log.e("TESTPARM", it)
                                when {
                                    it.contains("WARNING:") || it.contains("NOTE:") -> {
                                        message.add(it)
                                    }
                                    it.contains("set_variable_helper", true) -> {
                                        message.add(tmp[tmp.indexOf(it) - 1])
                                        message.add(it)
                                    }
                                }
                            }
                            title = "Check failed!"
                        }

                        message.forEach {
                            toDisplay += "$it\n"
                        }

                        Log.e("TESTPRANK", message.toString())

                        runOnUiThread {
                            val build = AlertDialog.Builder(this)
                            build.setTitle(title)
                                    .setMessage(toDisplay)
                                    .setCancelable(false)
                                    .setNeutralButton(resources.getString(R.string.ok), DialogInterface.OnClickListener { dialog, _ ->
                                        util.vibrate(28, 1)
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
                    }
                    testparmButton = false
                }

            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        main.ssh.activityStatus[7] = true   // jak się soć robi to by ssh nie wyłączyło
        if (!defaultSharedPreferences.getString("Theme", "Default").contains(util.theme, true))
            recreate()
    }

    override fun onStop() {
        super.onStop()
        main.ssh.activityStatus[7] = false  // jak się przejdzie do background to by się ssh wyłączyło
    }

}
