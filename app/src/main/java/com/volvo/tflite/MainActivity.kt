package com.volvo.tflite

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognizerIntent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.tensorflow.lite.Interpreter
import java.util.*

class MainActivity : AppCompatActivity() {
    protected var tflite: Interpreter? = null
    var textView: TextView? = null
    var button: Button? = null
    var editText: EditText? = null

    private lateinit var serviceIntent: Intent
    private lateinit var requiredSDKPermissions: Array<String>

    private lateinit var mService: RecognizeSpeechService
    private var mBound: Boolean = false

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as RecognizeSpeechService.MyBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serviceIntent = Intent(this, RecognizeSpeechService::class.java)

        textView = findViewById<View>(R.id.text) as TextView
        editText = findViewById<View>(R.id.edit_text) as EditText
        editText!!.visibility = View.GONE
        button = findViewById<View>(R.id.button) as Button
        button!!.setOnClickListener {
            //startVoiceInput()
            if (isForegroundServiceRunning()) {
                stopService(serviceIntent)
                button!!.text = "Start service"
            } else {
                startFGService()
                button!!.text = "Stop service"
            }
        }
        preparePermissions()
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissions()
        }
        // Bind to LocalService
        Intent(this, RecognizeSpeechService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_IMPORTANT)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        mBound = false
    }

    private fun preparePermissions() {
        requiredSDKPermissions =
                arrayOf(Manifest.permission.RECORD_AUDIO)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkPermissions(): Boolean {
        val missingPermissions = ArrayList<String>()
        // check all required dynamic permissions
        for (permission in requiredSDKPermissions) {
            val result = ContextCompat.checkSelfPermission(this, permission)
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission)
            }
        }
        if (missingPermissions.isNotEmpty()) {
            // request all missing permissions
            val permissions = missingPermissions.toTypedArray()
            ActivityCompat.requestPermissions(
                    this, permissions,
                    REQUEST_CODE_ASK_PERMISSIONS
            )
            return false
        } else {
            val grantResults = IntArray(requiredSDKPermissions.size)
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED)
            onRequestPermissionsResult(
                    REQUEST_CODE_ASK_PERMISSIONS,
                    requiredSDKPermissions,
                    grantResults
            )
            return true
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_ASK_PERMISSIONS -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //startFGService()
                }
                return
            }
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hello, How can I help you?")
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT)
        } catch (a: ActivityNotFoundException) {
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_CODE_SPEECH_INPUT -> {
                if (resultCode == Activity.RESULT_OK && null != data) {
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    mService.nluClassify(result[0])
                }
            }
        }
    }

    private fun startFGService() {
        if (!isForegroundServiceRunning()) {
            debugToast("Service started")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } else {
            debugToast("Service is already running")
        }
    }

    @Suppress("DEPRECATION")
    private fun isForegroundServiceRunning(): Boolean {
        val manager =
                getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (RecognizeSpeechService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    companion object {
        private const val REQ_CODE_SPEECH_INPUT = 100
        private const val REQUEST_CODE_ASK_PERMISSIONS = 1
    }
}