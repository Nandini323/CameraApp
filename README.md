Updates to keyboard shortcuts … On Thursday, 1 August 2024, Drive keyboard shortcuts will be updated to give you first-letter navigation.Learn more
MVVM Architecture :
Why we are using MVVM Architecture?
MVVM helps you separate concerns by keeping your UI code separate from your business logic code.
MVVM allows you to separate the view from its data model. In other words, you can use one-way data binding to keep the two in sync. It makes it easier to test your code because it’s modular and loosely-coupled

What is Coroutines ?
Coroutines : Is light wight threads for asynchronous programming, Coroutines not only open the doors to asynchronous programming, but also provide a wealth of other possibilities such as concurrency, actors, etc.

What is the Coroutines benefits?
Writing an asynchronous code is sequential manner.
Costing of create coroutines are much cheaper to crate threads.
Don't be over engineered to use observable pattern, when no need to use it.
parent coroutine can automatically manage the life cycle of its child coroutines for you.

Add Coroutines to gradle file

 // Coroutines
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.5.2")
	
CameraX :
CameraX is a Jetpack library, built to help make camera app development easier. For new apps, we recommend starting with CameraX. It provides a consistent, easy-to-use API that works across the vast majority of Android devices, with backward-compatibility to Android 5.0 (API level 21).

CameraX structure:
You can use CameraX to interface with a device’s camera through an abstraction called a use case. The following use cases are available:

Preview: accepts a surface for displaying a preview, such as a PreviewView.
Image capture: captures and saves a photo.
Video capture: capture video and audio with VideoCapture
Use cases can be combined and active concurrently. For example, an app can let the user view the image that the camera sees using a preview use case, have an image analysis use case that determines whether the people in the photo are smiling, and include an image capture use case to take a picture 

Camera initialization :
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
