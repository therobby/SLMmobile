package com.karel.slmmobile

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.system.exitProcess


class SSHService {
    var username: String = ""
    var hostname: String = ""
    var port: Int = 22
    var password = ""

    private var jsch = JSch()
    private var sessions = ArrayList<Session>()
    private var session: Session? = null
    private var rootSession: Session? = null

    private var stopExecute = false
    private val timeoutTime = 5 * 4 //min
    private var inactivityTimeout = 0
    val activityStatus = ArrayList<Boolean>(7)

    var root = false   //jak jest root

    init {
        for (i in 0 until 8) {
            activityStatus.add(false)
        }
    }

    fun rootConnect(password: String): String {
        val prop = Properties()
        rootSession = jsch.getSession("root", hostname, port)
        prop.put("StrictHostKeyChecking", "no")
        prop.put("PreferredAuthentications", "password")
        rootSession?.setPassword(password)
        rootSession?.setConfig(prop)

        try {
            rootSession?.connect()
            val connectedRoot = rootSession!!.isConnected
            Thread.sleep(75)

            if (!connectedRoot)
                return "Auth fail"

        } catch (e: Exception) {
            Log.e("Root Connect:", e.toString())
            if (e is com.jcraft.jsch.JSchException) {
                if (e.toString().contains("Auth fail"))
                    return "Auth fail"
                else if (e.toString().contains("network"))
                    return "No Network"
                else
                    return "Unknown error"
            }
        }
        root = true
        return "Done"
    }

    fun connect(username: String,
                password: String,
                hostname: String,
                port: Int = 22): String {
        this.username = username
        this.hostname = hostname
        this.port = port
        this.password = password

        val prop = Properties()
        sessions.add(jsch.getSession(username, hostname, port))

        prop.put("StrictHostKeyChecking", "no")
        prop.put("PreferredAuthentications", "password")

        session = sessions.last()

        session?.setConfig(prop)
        session?.setPassword(password)

        try {
            session?.connect(10000)

            if (!session?.isConnected!!) {
                return "Connection failed"
            }

            val str = shellChannel("whoami")
            Log.e("Init_SSH:", str.toString())
            Log.e("Init_SSH:", str.length.toString())

            if (str.isBlank())
                return "Connection failed"

            if (str.contains("root") && str.length <= 5) {
                root = true
                rootSession = session
            }
        } catch (e: Exception) {
            Log.e("Init_SSH:", e.toString())
            if (e.toString().contains("Auth fail"))
                return "Auth fail"
            else if (e.toString().contains("network"))
                return "No Network"
            else if (e.toString().contains("timeout"))
                return "Connection timeout"
            else
                return "Unknown error"
        }
        main.serviceConnected = true
        dropConnection()
        return "Done"
    }

    fun shellChannel(command: String, autoReady: Boolean = true, debug: Boolean = false): StringBuilder { // śmieszna metodka do wykonywania komendów, zwracająca StringBuildera
        val consolChannel: ChannelShell // kanał
        val out: OutputStream   //gówno do zbierania outputu
        val prnt: PrintStream   // do wysyłania komandów
        val endOfCommand = "SLM_END"    //bo muszę wiedzieć gdzie sie kończy output
        val beginOfCommand = "SLM_START"    // i gdzie się zaczyna
        val ret = StringBuilder()   //  tu będzie output zwracany
        var Break = false       // a to jak sie coś przytka, do późniejszego wyjebania
        var int = 5         // jak się zapętli to muszę mieć jakieś gówno które to będzie sprawdzało

        if (!stopExecute) {
            try {   // jak się coś wyjebie to niech nie wywala całego programu
                if (root) {
                    consolChannel = rootSession!!.openChannel("shell") as ChannelShell
                } else {
                    consolChannel = session!!.openChannel("shell") as ChannelShell
                }
                out = consolChannel.outputStream
                prnt = PrintStream(out)
                consolChannel.setPty(true)  //robimy sobie witrualną konsolkę o wyjebanych w kosmos rozmiarach
                consolChannel.setPtySize(5000000, 5000000, 5000000, 5000000)

                prnt.println("HISTFILE=~/.slm_history;HISTSIZE=1000;export LC_MESSAGES=en_US.utf8;unalias -a")
                prnt.flush()        // weź i wpierdol tam te wyżej gówno, poczym je spuść bo śmierdzi

                val commandline = "echo $beginOfCommand;$command;echo $endOfCommand"    // połącz komendę z tymi gównami do znalezienia początku/końca komendy
                consolChannel.connect() //podłacz się kanałem
                prnt.println(commandline)   // wpierdol to gówno w kanał
                prnt.flush()    //spuść
                prnt.close()    // i zamknij ten strumień gówna

                val inn = consolChannel.inputStream
                val reader = BufferedReader(InputStreamReader(inn)) // tym gównem będziemy czytać input

                val output: ArrayList<String> = ArrayList()     // najpierw wpierdalamy tutej output

                while (!Break) {    // to jest straszne gówno mające na celu zebranie outputu
                    val str = reader.readLine() // i to coś będzie zwracać ten output
                    if (debug)
                        Log.e("Shell_channel: outline", str ?: "NULL")
                    if (str == endOfCommand)    //jak konic to wyjdź z pęteli
                        Break = true
                    if (!Break) //jak ni konic to dodaj output do tablicki
                        output.add(str)
                    if (stopExecute) {
                        out.write(3)
                        out.flush()
                        Break = true
                        stopExecute = false
                    }
                    if (int > 0) { // to to sprawda jak się zapętli nullem
                        if (str == null)
                            int--
                    } else {    // i jak się zapętli to zrywa połączenie bo oznacza to że się coś zjebało
                        out.write(3)
                        out.flush()
                        stopExecute = false
                        session?.disconnect()
                        return StringBuilder()
                    }
                }

                output.remove("HISTFILE=~/.slm_history;HISTSIZE=1000;export LC_MESSAGES=en_US.utf8;unalias -a")
                output.remove(commandline)  // usuń niepotrzebne gówno
                output.remove(endOfCommand)
                while (output.contains(beginOfCommand))
                    output.removeAt(0)

                output.forEach {
                    ret.append(it + "\n")   // przenieś to do jednego stringa
                }

                reader.close()  // zamknij readera
                consolChannel.disconnect()  // i rozłącz kanała
            } catch (e: Exception) {
                Log.e("Shell_channel:", e.toString())
                if (e.toString().contains("session is down") || e.toString().contains("kotlin.KotlinNullPointerException"))
                    close()
                //ready = false
            }
        }
        // (autoReady && connected)
        //ready = true
        return ret
    }

    fun close() {
        session?.disconnect()
        stopExecute = true
    }

    fun checkConnection(): Boolean {
        if (session == null)
            return false
        else
            return session?.isConnected!!
    }

    private fun dropConnection() {       // Metodka do dropowania połączonka jak się nic nie dzieje przez <timeoutTime> minutków
        thread {
            while (inactivityTimeout < timeoutTime) {
                if (activityStatus.contains(true))
                    inactivityTimeout = 0
                else {
                    inactivityTimeout++
                    Thread.sleep(15000)
                }
            }
            //close()
            //main.exit()
            Log.e("SSH","Inactivity quit")
            exitProcess(0)
        }
    }
}

/*class SSHService : Service() {

    //SSH Values
    private val operator = BinderOperator()

    var username: String = ""
    var hostname: String = ""
    var port: Int = 22
    var password = ""

    private var jsch = JSch()
    private var session: Session? = null
    private var rootSession: Session? = null

    private var stopExecute = false

    var root = false   //jak jest root
    var connected = false
    var ready = false

    override fun onBind(intent: Intent): IBinder? {
        return operator
    }

    inner class BinderOperator : Binder() {
        fun getService(): SSHService {
            return this@SSHService
        }
    }

    fun rootConnect(password: String): String {
        val prop = Properties()
        rootSession = jsch.getSession("root", hostname, port)
        prop.put("StrictHostKeyChecking", "no")
        rootSession?.setPassword(password)
        rootSession?.setConfig(prop)

        try {
            rootSession?.connect()
            val connectedRoot = rootSession!!.isConnected
            Thread.sleep(75)

            if (!connectedRoot)
                return "Auth fail"

        } catch (e: Exception) {
            Log.e("Root Connect:", e.toString())
            if (e is com.jcraft.jsch.JSchException) {
                return "Auth fail"
            }
        }
        root = true
        return "Done"
    }

    fun connect(username: String,
                password: String,
                hostname: String,
                port: Int = 22) {
        this.username = username
        this.hostname = hostname
        this.port = port
        this.password = password

        val prop = Properties()
        session = jsch.getSession(username, hostname, port)
        prop.put("StrictHostKeyChecking", "no")
        session?.setPassword(password)
        session?.setConfig(prop)

        /*val ui = object : MyUserInfo() {
            override fun showMessage(message: String?) {
                showMessagePopup(message)
            }

            override fun promptYesNo(str: String?): Boolean {
                return yesNoPopup(str)
            }
        }

        session?.userInfo = ui*/

        thread {
            try {
                session?.connect()
                connected = session!!.isConnected
                Thread.sleep(75)

                val str = shellChannel("whoami")
                Log.e("Init_SSH:", str.toString())
                Log.e("Init_SSH:", str.length.toString())
                if (str.contains("root") && str.length <= 5) {
                    root = true
                    rootSession = session
                }

                ready = true
            } catch (e: Exception) {
                connected = false
                ready = false
                Log.e("Init_SSH:", e.toString())
            }
        }
    }

    fun executeChannel(command: String, autoReady:Boolean = true , delay: Long = 75, debug : Boolean = false): StringBuilder {
        ready = false
        val output = StringBuilder()
        val boas = ByteArrayOutputStream()
        try {
            val channelssh = session?.openChannel("exec") as ChannelExec
            channelssh.setPty(true)
            channelssh.setPtySize(50000, 50000, 50000, 50000)
            channelssh.outputStream = boas
            channelssh.setCommand("export LC_MESSAGES=en_US.utf8 ; unalias -a ; $command")
            channelssh.connect()
            Thread.sleep(delay)
            channelssh.disconnect()
            output.append(boas.toString())
            if(debug)
                Log.e("Exec-channel",output.toString())
        } catch (e: Exception) {
            connected = false
            ready = false
            Log.e("SSH:",e.toString())
        }
        if(autoReady)
            ready = true
        return output
    }

fun shellChannel(command: String, autoReady: Boolean = true, debug : Boolean = false): StringBuilder {
        val consolChannel: ChannelShell
        val out: OutputStream
        val prnt: PrintStream
        val endOfCommand = "SLM_END"
        val beginOfCommand = "SLM_START"
        val ret = StringBuilder()
        var Break = false
        var int = 5
        ready = false

        try {
            if (root) {
                consolChannel = rootSession!!.openChannel("shell") as ChannelShell
            } else {
                consolChannel = session!!.openChannel("shell") as ChannelShell
            }
            out = consolChannel.outputStream
            prnt = PrintStream(out)
            consolChannel.setPty(true)
            consolChannel.setPtySize(5000000, 5000000, 5000000, 5000000)

            prnt.println("HISTFILE=~/.slm_history;HISTSIZE=1000;export LC_MESSAGES=en_US.utf8;unalias -a")
            prnt.flush()

            val commandline = "echo $beginOfCommand;$command;echo $endOfCommand"
            consolChannel.connect()
            prnt.println(commandline)
            prnt.flush()
            prnt.close()

            val inn = consolChannel.inputStream
            val reader = BufferedReader(InputStreamReader(inn))

            val output: ArrayList<String> = ArrayList()

            while (!Break) {
                val str = reader.readLine()
                if(debug)
                    Log.e("Shell_channel: outline", str ?: "NULL")
                if (str == endOfCommand)
                    Break = true
                if (!Break)
                    output.add(str)
                if (stopExecute) {
                    out.write(3)
                    out.flush()
                    Break = true
                    stopExecute = false
                }
                if (int > 0) {
                    if (str == null)
                        int--
                } else {
                    out.write(3)
                    out.flush()
                    Break = true
                    stopExecute = false
                    close()
                }
            }

            output.remove("HISTFILE=~/.slm_history;HISTSIZE=1000;export LC_MESSAGES=en_US.utf8;unalias -a")
            output.remove(commandline)
            output.remove(endOfCommand)

            while (output.contains(beginOfCommand))
                output.removeAt(0)

            output.forEach {
                ret.append(it + "\n")
            }

            reader.close()
            consolChannel.disconnect()
        } catch (e: Exception) {
            Log.e("Shell_channel:", e.toString())
            connected = false
            ready = false
        }
        if (autoReady && connected)
            ready = true
        return ret
    }

    fun breakTerminal() {
        thread {
            if (connected) {
                stopExecute = true
            }
        }
    }

    fun checkConnection() {
        connected = session!!.isConnected
    }

    fun close() {
        Log.e("SSH", "Closed")
        session?.disconnect()
        rootSession?.disconnect()
    }
}*/