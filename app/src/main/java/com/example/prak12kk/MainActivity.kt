package com.example.prak12kk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.*
import com.example.prak12kk.ui.theme.Prak12kkTheme
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import androidx.compose.material3.TopAppBar


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Prak12kkTheme {
                MainScreen()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prak12kk") },
                navigationIcon = {
                    IconButton(onClick = { /* Handle navigation icon click */ }) {
                        Icon(Icons.Default.Menu, contentDescription = null)
                    }
                }
            )
        },
        content = { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "imageDownloader",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("imageDownloader") { ImageDownloadScreen(navController) }
                composable("info") { InfoScreen() }
                composable("settings") { SettingsScreen() }
            }
        },
        bottomBar = {
            BottomAppBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    label = { Text("Downloader") },
                    selected = false,
                    onClick = { navController.navigate("imageDownloader") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    label = { Text("Info") },
                    selected = false,
                    onClick = { navController.navigate("info") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = { navController.navigate("settings") }
                )
            }
        }
    )
}

@Composable
fun ImageDownloadScreen(navController: NavHostController) {
    var url by remember { mutableStateOf("") }
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Enter Image URL") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (url.isNotEmpty()) {
                    val workManager = WorkManager.getInstance(context)
                    val inputData = workDataOf("imageUrl" to url)

                    val downloadWorkRequest = OneTimeWorkRequestBuilder<ImageDownloadWorker>()
                        .setInputData(inputData)
                        .build()
                    workManager.enqueue(downloadWorkRequest)

                    workManager.getWorkInfoByIdLiveData(downloadWorkRequest.id)
                        .observe(lifecycleOwner) { workInfo ->
                            if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
                                val imagePath = workInfo.outputData.getString("imagePath")
                                if (imagePath != null) {
                                    val imageBitmap = BitmapFactory.decodeFile(imagePath)
                                    bitmap = imageBitmap
                                    Toast.makeText(
                                        context,
                                        "Image downloaded to: $imagePath",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Download Image")
        }

        Spacer(modifier = Modifier.height(16.dp))

        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(400.dp)
            )
        }
    }
}

class ImageDownloadWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val imageUrl = inputData.getString("imageUrl") ?: return Result.failure()
        return try {
            val bitmap = downloadImage(imageUrl)
            bitmap?.let {
                val file = saveImageToDisk(it, applicationContext)
                val outputData = workDataOf("imagePath" to file.absolutePath)
                Result.success(outputData)
            } ?: Result.failure()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

@Composable
fun InfoScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Information Screen", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Settings Screen", style = MaterialTheme.typography.titleLarge)
    }
}

private fun downloadImage(url: String): Bitmap? {
    return try {
        val inputStream: InputStream = URL(url).openStream()
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun saveImageToDisk(bitmap: Bitmap, context: Context): File {
    val directory =
        File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "downloadedImages")
    if (!directory.exists()) {
        directory.mkdirs()
    }
    val file = File(directory, "downloaded_image.png")
    val outputStream = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    outputStream.flush()
    outputStream.close()
    return file
}
