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



![Screenshot_20240619-190316](https://github.com/Nandini323/CameraApp/assets/145770282/610896f1-13a0-4322-b564-160e046f2867)
![Screenshot_20240619-190321](https://github.com/Nandini323/CameraApp/assets/145770282/42674bf0-9447-496d-bbf9-79def0ecde98)
![Screenshot_20240619-190329](https://github.com/Nandini323/CameraApp/assets/145770282/c2053006-27e4-4318-b4af-1892a735a7a0)
![Screenshot_20240619-190340](https://github.com/Nandini323/CameraApp/assets/145770282/28fd2b2d-381c-4219-bff5-dd025535bdbc)
![Screenshot_20240619-190349](https://github.com/Nandini323/CameraApp/assets/145770282/8255e656-87ca-4f64-904b-933c35a2bc6a)
![Screenshot_20240619-190416](https://github.com/Nandini323/CameraApp/assets/145770282/b24632aa-839d-497b-8984-1c155bad291a)
![Screenshot_20240619-190533](https://github.com/Nandini323/CameraApp/assets/145770282/d696df67-58bd-405d-a541-7e61a4c1a6e5)
![Screenshot_20240619-190539](https://github.com/Nandini323/CameraApp/assets/145770282/16f38be1-268b-4de8-86c6-500ef9a30c09)


