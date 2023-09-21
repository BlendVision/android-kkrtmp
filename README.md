## RTMP sample app
This sample project includes how to integrate the streaming library and push the streaming to the RTMP server.

### Overall
- Step0. import library dependency 
- Step1. request device permission
- Step2. initialize rtmp connection, streaming source, and set to surface
- Step3. register onClicked event listener
- final. resource controll

#### How to import library dependency
```groovy=
implementation 'com.github.shogo4405.HaishinKit~kt:haishinkit:0.11.2'
```

#### ui layout and pre-define variables
```xml=
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <com.haishinkit.view.HkSurfaceView
        android:id="@+id/camera"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <androidx.appcompat.widget.AppCompatButton
        android:id="@+id/btn_publish"
        android:text="Publish"
        android:textSize="24dp"
        android:layout_margin="16dp"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toStartOf="@+id/btn_switch"
        app:layout_constraintBottom_toBottomOf="parent"/>

    <androidx.appcompat.widget.AppCompatButton
        android:id="@+id/btn_switch"
        android:text="Switch"
        android:textSize="24dp"
        android:layout_margin="16dp"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:layout_constraintStart_toEndOf="@+id/btn_publish"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"/>

</androidx.constraintlayout.widget.ConstraintLayout>
```
```kotlin=
private lateinit var connection: RtmpConnection
private lateinit var stream: RtmpStream
private lateinit var camera2Source: Camera2Source

private lateinit var binding: ActivityMainBinding
```

#### How to request deivce permision
```kotlin=
// add these in AndroidManifest.xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />

// request in activity
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
```
#### How to initialize rtmp connection, streaming source, and set to surface
```kotlin=
// create instance
connection = RtmpConnection()
stream = RtmpStream(connection)
// get the camera and microphone devices
val audioSource = AudioRecordSource(this, false)
stream.attachAudio(audioSource)
camera2Source = Camera2Source(this)
stream.attachVideo(camera2Source)
// add the RTMP status listener
connection.addEventListener(Event.RTMP_STATUS, this)

// attach the instanve to view
binding.camera.attachStream(stream)
```
Listener
```kotlin=
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
```

#### How to register onClicked event listener
```kotlin=
// publish/stop rtmp streaming
binding.btnPublish.setOnClickListener {
    if (binding.btnPublish.text == "Stop") {
        connection.close()
        (it as AppCompatButton).text = "Publish"
    } else {
        connection.connect(RTMP_URL)
        (it as AppCompatButton).text = "Stop"
    }
}
// flip front/back camera
binding.btnSwitch.setOnClickListener {
    camera2Source.switchCamera()
}
```

#### How to controll the device resource
We recommand to open the camera when onResume lifecycle and close it when onPause to avoid OOM. And make sure the connection be dispose() when you finish the stream pushing.
```kotlin=
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
```
