package com.volvo.tflite

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import ca.rmen.porterstemmer.PorterStemmer
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*


class RecognizeSpeechService : Service(), RecognitionListener {

    private var message: String = ""
    private lateinit var speechRecognizer: SpeechRecognizer
    protected var tflite: Interpreter? = null
    var words = arrayOf("a", "afternoon", "are", "assist", "befor", "broken", "bunk", "can", "check", "day", "diesel", "distanc", "drink",
            "eat", "empti", "even", "famish", "far", "farther", "fill", "food", "for", "fuel", "ga", "go", "good", "hello", "help", "hey",
            "hi", "how", "hungri", "indic", "is", "left", "light", "long", "lot", "me", "morn", "much", "near", "on", "petrol", "place",
            "pump", "refil", "remain", "restaur", "right", "side", "so", "starv", "station", "tank", "thank", "the", "thirsti", "to", "ton",
            "travel", "turn", "we", "what", "you")


    private val mBinder: IBinder = MyBinder()
    private var original_volume_level: Int? = null
    private lateinit var audioManager: AudioManager

    inner class MyBinder : Binder() {
        fun getService(): RecognizeSpeechService = this@RecognizeSpeechService
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        Log.d(TAG, "Service created")
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        // starting foreground service with notification in Android >= O
        // otherwise system kills the service
        startForeground()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show()
        Log.i(TAG, "onStartCommand()")
        startForeground()
        startListening()

        try {
            tflite = Interpreter(loadModelFile())
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return START_REDELIVER_INTENT
    }


    override fun onDestroy() {
        super.onDestroy()
        Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Service destroyed")
        speechRecognizer.cancel()
        speechRecognizer.destroy()
    }

    override fun onReadyForSpeech(bundle: Bundle) {
        Log.d(TAG, "SpeechRecognizer onReadyForSpeech")
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "SpeechRecognizer onBeginningOfSpeech")
    }

    override fun onRmsChanged(v: Float) {
        Log.d(TAG, "SpeechRecognizer onRmsChanged: ")
    }

    override fun onBufferReceived(bytes: ByteArray) {
        Log.d(TAG, "SpeechRecognizer onBufferReceived: ")
    }

    override fun onEndOfSpeech() {
        Log.d(TAG, "SpeechRecognizer onEndOfSpeech: ")
    }

    override fun onError(i: Int) {
        Log.d(TAG, "SpeechRecognizer onError: Error code:  $i")
        unmuteAudio()
        //SpeechRecognizer errors
        when (i) {
            2 -> startListening() //ERROR_NETWORK which shouldn't stop us, as it should work offline
            7 -> startListening() //ERROR_NO_MATCH
            else -> Toast.makeText(applicationContext, "Listening error", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResults(results: Bundle) {
        Log.d(TAG, "SpeechRecognizer onResults: ")
        unmuteAudio()
        message = ""
        val matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        this.nluClassify(matches[0])
        startListening()
    }

    override fun onPartialResults(bundle: Bundle) {}
    override fun onEvent(i: Int, bundle: Bundle) {}

    private fun startListening() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext)
        speechRecognizer.setRecognitionListener(this)
        val voice = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        voice.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, javaClass.getPackage().name)
        voice.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        voice.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10)
        //original_volume_level = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        muteAudio()
        speechRecognizer.startListening(voice)
    }

    private fun muteAudio() {
//        audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true)
//        audioManager.setStreamMute(AudioManager.STREAM_ALARM, true)
//        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true)
//        audioManager.setStreamMute(AudioManager.STREAM_RING, true)
//        audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true)
        //audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, -100, 0)
    }

    private fun unmuteAudio() {
//        audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false)
//        audioManager.setStreamMute(AudioManager.STREAM_ALARM, false)
//        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false)
//        audioManager.setStreamMute(AudioManager.STREAM_RING, false)
//        audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false)
    }

    fun nluClassify(stringToClassify: String) {
        Log.d(TAG, "nlu_classify: String from recognizer: $stringToClassify")
        if (stringToClassify.isNotEmpty()) {
            val inputArray = stringConverter(stringToClassify)
            val probArray = Array(1) { FloatArray(7) }
            tflite!!.run(inputArray, probArray)
            val index = highestpredictionindex(probArray)

            var nluIntent = ""
            when (index) {
                0 -> nluIntent = "Distance"
                1 -> nluIntent = "FuelStation"
                2 -> nluIntent = "Greetings"
                3 -> nluIntent = "LeftIndicator"
                4 -> nluIntent = "Restaurant"
                5 -> nluIntent = "RightIndicator"
                6 -> nluIntent = "Thanking"
            }
            message = "${message}\nIntent = ${nluIntent}"
            Toast.makeText(App.context, message, Toast.LENGTH_LONG).show()
            Log.d(TAG, "nlu_classify: Recognized intent: $nluIntent")
        }
    }

    private fun stringConverter(str: String?): Array<FloatArray> {
        val tokens = ArrayList<String>()
        val defaultTokenizer = StringTokenizer(str)
        while (defaultTokenizer.hasMoreTokens()) {
            val porterStemmer = PorterStemmer()
            val stem = porterStemmer.stemWord(defaultTokenizer.nextToken().toLowerCase())
            tokens.add(stem)
        }
        val len = words.size
        val bag = Array(1) { FloatArray(len) }
        for (i in words.indices) {
            for (j in tokens.indices) {
                if (words[i] == tokens[j]) {
                    bag[0][i] = 1f
                }
            }
        }
        return bag
    }

    private fun highestpredictionindex(input: Array<FloatArray>): Int {
        var maxAt = 0
        for (i in input[0].indices) {
            maxAt = if (input[0][i] > input[0][maxAt]) i else maxAt
        }
        message = message + "Accuracy = " + input[0][maxAt]
        return maxAt
    }

    private fun startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForeground(
                    SERVICE_NOTIFICATION_ID,
                    prepareForegroundServiceNotification(applicationContext)
            )
        else startForeground(SERVICE_NOTIFICATION_ID, Notification())
    }

    @Throws(IOException::class)
    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = App.context!!.assets.openFd(MODEL_PATH)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }


    companion object {
        private const val TAG = "RecognizeSpeechService"
        private const val MODEL_PATH = "nlu.tflite"
    }
}