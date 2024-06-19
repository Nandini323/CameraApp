package com.example.photocaptureapplication

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

import android.net.Uri
import android.os.Build
import android.os.Environment.isExternalStorageManager
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class MyViewModel():ViewModel() {
    private val _showBottomSheet = MutableStateFlow(false)
    val showBottomSheet: StateFlow<Boolean> = _showBottomSheet
    val VIDEO_CAPTURE_REQUEST_CODE = 1001
    private lateinit var imageCapture: ImageCapture
    private lateinit var videoCapture: VideoCapture<Recorder>
    private lateinit var scheduler: ScheduledExecutorService
    private val _launchIntent = MutableLiveData<Intent>()
    val launchIntent: LiveData<Intent> get() = _launchIntent

    private val _permissionGranted = MutableLiveData<Boolean>()
    val permissionGranted: LiveData<Boolean> get() = _permissionGranted

    var _videoUri = mutableStateOf<Uri?>(null)
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private var isTorchOn: Boolean = false
companion object{
    var cameraControl: CameraControl? = null
}

    private val REQUEST_VIDEO_CAPTURE = 1
    private val _items = MutableStateFlow(
        listOf(
            BottomSheetItem("Resolution", Icons.Default.Face),
            BottomSheetItem("Torch on/off", Icons.Default.Favorite),
            BottomSheetItem("Custom picture clicker", Icons.Default.Add),
        )
    )
    val items: StateFlow<List<BottomSheetItem>> = _items

    fun toggleBottomSheet(show: Boolean) {
        _showBottomSheet.value = show
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun onItemClick(itemIndex: Int, context: Context) {
        // Handle item click
        // For example, log the click or update some state
        viewModelScope.launch {
            if(itemIndex==0){
                showQualitySelectionDialog(context)
            }else if(itemIndex==1){
                toggleTorch(context)
            }else if(itemIndex==2){
                capturePhoto(context)
            }
        }
    }
    fun getVideoQuality(qualityString: String): Quality {
        return when (qualityString) {
            "LOWEST" -> Quality.LOWEST
            "HIGHEST" -> Quality.HIGHEST
            "SD" -> Quality.SD
            "HD" -> Quality.HD
            "FHD" -> Quality.FHD
            "UHD" -> Quality.UHD
            else -> Quality.HD  // Default quality
        }
    }



    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun cameraPermissionHandler() {
        val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)


        LaunchedEffect(Unit) {

            cameraPermissionState.launchPermissionRequest()

        }

        val cameraPermissionsGranted = cameraPermissionState.status.isGranted

        if (cameraPermissionsGranted) {
            audiorequest()
        } else {

        }
    }

    @Composable
    @OptIn(ExperimentalPermissionsApi::class)
    fun audiorequest(){
        val audioPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

        LaunchedEffect(Unit) {
            audioPermissionState.launchPermissionRequest()
        }

        val audioPermissionsGranted = audioPermissionState.status.isGranted

        if (audioPermissionsGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                checkAndRequestFileAccess()
            }else {
                storagepermissionhandler()
            }
        } else {


        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun checkAndRequestFileAccess() {
           if(isExternalStorageManager()){
               _permissionGranted.value = true
           }else{
            try {
                val intent = Intent().apply {
                    action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                    val packagename="com.example.photocaptureapplication"
                    data = Uri.fromParts("package",packagename, null)
                }
                _launchIntent.value = intent
            } catch (e: Exception) {
                val intent = Intent().apply {
                    action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                }
                _launchIntent.value = intent
            }
               }

    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun storagepermissionhandler(){
        val storagePermissionsState = rememberMultiplePermissionsState(
            permissions = listOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )

        LaunchedEffect(Unit) {
            storagePermissionsState.launchMultiplePermissionRequest()
        }

        val storagePermissionsGranted = storagePermissionsState.allPermissionsGranted

    }


    fun handleVideoCaptureResult(requestCode: Int, resultCode: Int, data: Intent?,context: Context) {
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == Activity.RESULT_OK) {
            val videoUri: Uri? = data?.data
            _videoUri.value = videoUri
            if (videoUri != null) {
                _videoUri.value = videoUri
                saveVideoToInternalStorage(videoUri,context)
            }
        }
    }
    private fun saveVideoToInternalStorage(videoUri: Uri, context: Context) {
        val inputStream = context.contentResolver.openInputStream(videoUri)
        val outputFile = File(context.filesDir, "captured_video.mp4")
        val outputStream = FileOutputStream(outputFile)
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        outputStream.close()
        inputStream?.close()
        Log.d("MyViewModel", "Video saved to: ${outputFile.absolutePath}")
    }


    fun initialize(imageCapture: ImageCapture, videoCapture: VideoCapture<Recorder>) {
        this.imageCapture = imageCapture
        this.videoCapture = videoCapture
    }

    fun startRecording(context: Context) {
        val videoFile = File(getOutputDirectory(context), "${System.currentTimeMillis()}.mp4")
        val outputOptions = FileOutputOptions.Builder(videoFile).build()
        val recording = videoCapture.output.prepareRecording(context, outputOptions)
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        scheduleImageCapture(context)
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            _videoUri.value = Uri.fromFile(videoFile)
                        } else {
                            Log.e("CameraViewModel", "Video capture error: ${recordEvent.error}")
                        }
                        stopScheduledImageCapture()
                    }
                }
            }
    }

    fun stopRecording() {
        // Logic to stop recording
    }

    private fun scheduleImageCapture(context: Context) {
        scheduler = Executors.newScheduledThreadPool(1)
        val captureTask = Runnable { capturePhoto(context) }
        scheduler.scheduleAtFixedRate(captureTask, 0, 5, TimeUnit.SECONDS)
    }

    private fun stopScheduledImageCapture() {
        if (::scheduler.isInitialized) {
            scheduler.shutdown()
        }
    }

    private fun capturePhoto(context: Context) {
        val photoFile = File(getOutputDirectory(context), "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    Log.d("CameraViewModel", "Photo capture succeeded: $savedUri")
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraViewModel", "Photo capture failed: ${exception.message}", exception)
                }
            }
        )
    }

    private fun getOutputDirectory(context: Context): File {
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, context.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun toggleTorch(context: Context) {
        try {

            isTorchOn = !isTorchOn
            cameraControl?.enableTorch(isTorchOn)
           // Toast.makeText(this, if (isTorchOn) "Torch is On" else "Torch is Off", Toast.LENGTH_SHORT).show()
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }





    private val _videoQuality = MutableLiveData<Quality>(Quality.HD)
    val videoQuality: LiveData<Quality> = _videoQuality

    fun setVideoQuality(quality: Quality) {
        _videoQuality.value = quality
    }

    fun initializeCamera(cameraProvider: ProcessCameraProvider, lifecycleOwner: LifecycleOwner, previewView: PreviewView,context: Context) {
        viewModelScope.launch {

            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return@launch

            try {
                val preview = Preview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                val imageCapture = ImageCapture.Builder().build()
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(_videoQuality.value ?: Quality.HD))
                    .build()
                val videoCapture = VideoCapture.withOutput(recorder)

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                    videoCapture
                )
                cameraControl = camera.cameraControl
                this@MyViewModel.imageCapture = imageCapture
                this@MyViewModel.videoCapture = videoCapture
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }

    fun showQualitySelectionDialog(context: Context) {
        val qualityOptions = listOf("LOWEST", "SD", "HD", "FHD", "UHD")

        val dialog = AlertDialog.Builder(context)
            .setTitle("Select Video Quality")
            .setItems(qualityOptions.toTypedArray()) { _, which ->
                val selectedQuality = qualityOptions[which]
                val quality = getVideoQuality(selectedQuality)
                setVideoQuality(quality)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }


}