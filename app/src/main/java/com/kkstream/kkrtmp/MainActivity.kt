package com.kkstream.kkrtmp

import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.haishinkit.event.Event
import com.haishinkit.event.EventUtils
import com.haishinkit.event.IEventListener
import com.haishinkit.media.AudioRecordSource
import com.haishinkit.media.Camera2Source
import com.haishinkit.rtmp.RtmpConnection
import com.haishinkit.rtmp.RtmpStream
import com.kkstream.kkrtmp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), IEventListener {

    private lateinit var connection: RtmpConnection
    private lateinit var stream: RtmpStream
    private lateinit var camera2Source: Camera2Source

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Step1. request device permission
        requestPermissionsInternal()
        // Step2. initialize rtmp connection, streaming source, and set to surface
        initialize()
        // Step3. register onClicked event listener
        registerListener()
    }

    private fun requestPermissionsInternal() {
        val requiredPermissions = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO)
        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // If any permissions are missing we want to just request them all.
                ActivityCompat.requestPermissions(this, requiredPermissions, 0x100)
                break
            }
        }
    }

    private fun initialize() {
        connection = RtmpConnection()
        stream = RtmpStream(connection)
        val audioSource = AudioRecordSource(this, false)
        stream.attachAudio(audioSource)
        camera2Source = Camera2Source(this)
        stream.attachVideo(camera2Source)
        connection.addEventListener(Event.RTMP_STATUS, this)

        binding.camera.attachStream(stream)
    }

    private fun registerListener() {
        binding.btnPublish.setOnClickListener {
            if (binding.btnPublish.text == "Stop") {
                connection.close()
                (it as AppCompatButton).text = "Publish"
            } else {
                connection.connect(RTMP_URL)
                (it as AppCompatButton).text = "Stop"
            }
        }

        binding.btnSwitch.setOnClickListener {
            camera2Source.switchCamera()
        }
    }

    /**
     * Once the connection is established, publish the stream name.
     */
    override fun handleEvent(event: Event) {
        Log.i(TAG, "event: $event")
        val data = EventUtils.toMap(event)
        val code = data["code"].toString()
        if (code == RtmpConnection.Code.CONNECT_SUCCESS.rawValue) {
            stream.publish(RTMP_STREAM_NAME)
        }
    }

    override fun onResume() {
        super.onResume()
        camera2Source.open(CameraCharacteristics.LENS_FACING_BACK)
    }

    override fun onPause() {
        super.onPause()
        // must close camera2Source to avoid OOM.
        camera2Source.close()
    }

    override fun onDestroy() {
        connection.removeEventListener(Event.RTMP_STATUS, this)
        connection.dispose()

        super.onDestroy()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val RTMP_URL = "rtmp://4d231a32459cdb390f90d2bb0d7b885a-main.jcl-qa-coco-d3.kkstream.tech:1935/live/stream"
        private const val RTMP_STREAM_NAME = "stream"
    }
}