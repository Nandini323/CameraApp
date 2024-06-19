package com.example.photocaptureapplication
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.photocaptureapplication.MyViewModel.Companion.cameraControl
import com.example.photocaptureapplication.ui.theme.Purple80
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch


@RequiresApi(Build.VERSION_CODES.M)
@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun MainScreenView(viewModel: MyViewModel) {


    val items by viewModel.items.collectAsState()
    val showBottomSheet by viewModel.showBottomSheet.collectAsState()
    val context = LocalContext.current
    val previewView = remember { androidx.camera.view.PreviewView(context) }
    val coroutineScope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val videoQuality by viewModel.videoQuality.observeAsState(Quality.HD)


    LaunchedEffect(cameraProviderFuture, videoQuality) {
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
        viewModel.initializeCamera(cameraProvider, lifecycleOwner, previewView,context)
    }

    Scaffold(

        topBar = {
            TopAppBar(
                title = { Text(text = "CameraClicker") },
                navigationIcon = { /* Add navigation icon if needed */ },
                actions = {
                    IconButton(onClick = { viewModel.toggleBottomSheet(true)}) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "settings")
                    }
                }
            )
        },
        content = { padding ->
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
//            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Purple80)
                        .clickable {

                            if (isRecording) {
                                coroutineScope.launch {
                                    viewModel.stopRecording()
                                }
                                isRecording = false
                            } else {
                                coroutineScope.launch {
                                    viewModel.startRecording(context)
                                }
                                isRecording = true
                            }
                           // viewModel.dispatchTakeVideoIntent(context, REQUEST_VIDEO_CAPTURE)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(0.dp, 10.dp, 0.dp, 20.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.cam_ra),
                            contentDescription = null,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        /*Text(
                            text = "You can click the pictures as well as record the videos from this application.",
                            fontSize = 16.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )*/
                        Text(if (isRecording) "Stop Recording" else "Start Recording",
                                fontSize = 16.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Center )
                    }
                }
            }
        }
    )

    if (showBottomSheet) {
        ModalBottomSheet(onDismissRequest = { viewModel.toggleBottomSheet(false) }) {

            bottomsheetContent(
                items = items,
                onHideButtonClick = {
                    viewModel.toggleBottomSheet(false)

                                    },
                onItemClick = { itemIndex ->
                    viewModel.onItemClick(itemIndex,context)
                    // Handle item click
                    Log.d("Dataview","Item $itemIndex clicked")
                    // Perform actions based on the clicked item
//                    showBottomSheet = false  // Optionally hide the bottom sheet after click
                },viewModel
            )
        }
    }

//added this for permissionrequest
    viewModel.cameraPermissionHandler()
}

data class BottomSheetItem(val text: String, val icon: ImageVector)

@Composable
fun bottomsheetContent(items: List<BottomSheetItem>, onHideButtonClick: () -> Unit, onItemClick: (Int) -> Unit,viewModel: MyViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        itemsIndexed(items) { index, item ->
            ListItem(
                modifier = Modifier.clickable { onItemClick(index)
                    /*cameralaunch(viewModel)*/},
                headlineContent = { Text(text = item.text) },
                leadingContent = { Icon(imageVector = item.icon, contentDescription = null) }
            )
        }

        item {
            Button(modifier = Modifier.fillMaxWidth(), onClick = { onHideButtonClick() }) {
                Text(text = "Hide")
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SampleAlertDialog(
        showDialog: Boolean,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
    ) {
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { onDismiss() },
                title = { Text(text = "Permission Required") },
                text = { Text("This application needs camera and storage permissions to function properly.") },
                confirmButton = {
                    Button(onClick = { onConfirm() }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    Button(onClick = { onDismiss() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}



