package com.dronescan.msdksample

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.dronescan.msdksample.ui.theme.DroneScan_V5Theme
import dji.v5.common.error.IDJIError
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback
import dji.v5.manager.aircraft.AircraftManager
import dji.v5.manager.aircraft.camera.CameraManager
import dji.v5.manager.KeyManager
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.key.model.Scope
import dji.v5.common.key.model.product.ProductKey
import dji.v5.manager.interfaces.IDeviceManager.DeviceConnectionState
import dji.v5.manager.interfaces.IDeviceManager.DeviceConnectionState.CONNECTED
import dji.v5.manager.interfaces.IDeviceManager.DeviceConnectionState.DISCONNECTED
import dji.v5.manager.interfaces.IDeviceManager.DeviceConnectionState.CONNECTING
import dji.v5.manager.interfaces.IDeviceManager.DeviceConnectionState.DISCONNECTING
import dji.v5.manager.interfaces.IDeviceManager.DeviceConnectionState.NOT_ACTIVATED
import dji.v5.manager.interfaces.IDeviceManager.DeviceConnectionState.NOT_SUPPORTED
import dji.v5.manager.interfaces.IDeviceManager.DeviceConnectionState.INITIALIZING
import dji.v5.manager.interfaces.IDeviceManager.DeviceConnectionState.UNKNOWN
import dji.v5.manager.aircraft.camera.enums.CameraMode
import dji.v5.manager.aircraft.camera.enums.CameraShootPhotoMode
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.ToastUtils

// MainActivity.kt
class MainActivity : ComponentActivity() {

    // Permisos necesarios para el DJI SDK
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            ToastUtils.showToast("Todos los permisos concedidos. Inicializando SDK de DJI...")
            initDJISDK()
        } else {
            ToastUtils.showToast("Algunos permisos no fueron concedidos. La aplicación podría no funcionar correctamente.")
            Log.e(TAG, "Permissions not granted.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DroneScan_V5Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(AppScreen.LOADING) }
                    var loadingMessage by remember { mutableStateOf("Iniciando aplicación...") }
                    var djiProduct by remember { mutableStateOf<AircraftManager?>(null) }

                    DisposableEffect(Unit) {
                        val productConnectionListener = object : CommonCallbacks.KeyListener<DeviceConnectionState> {
                            override fun onUpdate(value: DeviceConnectionState) {
                                Log.d(TAG, "Product connection state: $value")
                                when (value) {
                                    CONNECTED -> {
                                        djiProduct = AircraftManager.getInstance()
                                        loadingMessage = "Drone conectado: ${djiProduct?.productModel?.name ?: "Desconocido"}"
                                        ToastUtils.showToast(loadingMessage)
                                        currentScreen = AppScreen.SCAN
                                    }
                                    DISCONNECTED -> {
                                        djiProduct = null
                                        loadingMessage = "Drone desconectado."
                                        ToastUtils.showToast(loadingMessage)
                                        currentScreen = AppScreen.LOADING
                                    }
                                    CONNECTING -> {
                                        loadingMessage = "Conectando al drone..."
                                    }
                                    DISCONNECTING -> {
                                        loadingMessage = "Desconectando del drone..."
                                    }
                                    NOT_ACTIVATED -> {
                                        loadingMessage = "SDK no activado. Por favor, asegúrese de tener una conexión a internet."
                                    }
                                    NOT_SUPPORTED -> {
                                        loadingMessage = "Dispositivo no soportado."
                                    }
                                    INITIALIZING -> {
                                        loadingMessage = "Inicializando conexión..."
                                    }
                                    UNKNOWN -> {
                                        loadingMessage = "Estado de conexión desconocido."
                                    }
                                }
                            }
                        }
                        KeyManager.getInstance().listen(ProductKey.KeyConnection, Scope.PRODUCT, productConnectionListener)

                        onDispose {
                            KeyManager.getInstance().cancelListen(ProductKey.KeyConnection, productConnectionListener)
                        }
                    }

                    when (currentScreen) {
                        AppScreen.LOADING -> {
                            LoadingScreen(loadingMessage)
                        }
                        AppScreen.SCAN -> {
                            ScanScreen(djiProduct) { bitmap ->
                                ToastUtils.showToast("Foto capturada y lista para procesar!")
                            }
                        }
                    }
                }
            }
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        for (permission in REQUIRED_PERMISSIONS) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            ToastUtils.showToast("Todos los permisos ya concedidos. Inicializando SDK de DJI...")
            initDJISDK()
        }
    }

    private fun initDJISDK() {
        SDKManager.getInstance().init(applicationContext, object : SDKManagerCallback {
            override fun onRegister(error: IDJIError?) {
                if (error == null) {
                    ToastUtils.showToast("SDK de DJI registrado exitosamente.")
                    Log.d(TAG, "SDK registered successfully.")
                } else {
                    ToastUtils.showToast("Fallo al registrar el SDK de DJI: ${error.description()}")
                    Log.e(TAG, "SDK registration failed: ${error.description()}")
                }
            }

            override fun onProductDisconnect(productId: Int) {
            }

            override fun onProductConnect(productId: Int) {
            }

            override fun onProductChanged(product: AircraftManager?) {
            }

            override fun onComponentChange(
                componentKey: dji.v5.common.key.model.DJIKey<*, *>,
                oldComponent: dji.v5.common.key.model.BaseComponent?,
                newComponent: dji.v5.common.key.model.BaseComponent?
            ) {
            }

            override fun onInitProcess(event: SDKManager.DJISDKInitEvent, totalProcess: Int) {
                Log.d(TAG, "Init process: ${event.name}, progress: $totalProcess%")
            }

            override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                Log.d(TAG, "Database download progress: $current / $total")
            }
        })
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

enum class AppScreen {
    LOADING,
    SCAN
}

@Composable
fun LoadingScreen(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))
            Image(
                painter = painterResource(id = R.drawable.dji_logo),
                contentDescription = "DJI Logo",
                modifier = Modifier.size(120.dp)
            )
        }
    }
}

@Composable
fun ScanScreen(product: AircraftManager?, onPhotoCaptured: (Bitmap) -> Unit) {
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var scanningText by remember { mutableStateOf("Esperando drone...") }
    val handler = remember { Handler(Looper.getMainLooper()) }
    val context = LocalContext.current

    LaunchedEffect(product) {
        if (product != null) {
            scanningText = "Drone conectado: ${product.productModel?.name ?: "Desconocido"}"
            val cameraManager = CameraManager.getInstance()
            if (cameraManager != null) {
                cameraManager.setCameraMode(CameraMode.SHOOT_PHOTO, object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        ToastUtils.showToast("Cámara en modo foto.")
                        cameraManager.setShootPhotoMode(CameraShootPhotoMode.SINGLE, object : CommonCallbacks.CompletionCallback {
                            override fun onSuccess() {
                                ToastUtils.showToast("Modo de disparo: Sencillo.")
                                handler.postDelayed({
                                    val dummyBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.placeholder_drone_image)
                                    capturedImage = dummyBitmap
                                    onPhotoCaptured(dummyBitmap)
                                    scanningText = "Escaneo completado. Procesando imagen..."

                                    handler.postDelayed({
                                        val scannedCode = "QR_CODE_EXAMPLE_12345"
                                        scanningText = "Código detectado: $scannedCode"
                                        ToastUtils.showToast("Código detectado: $scannedCode")
                                    }, 2000)
                                }, 5000)
                            }
                            override fun onFailure(error: IDJIError) {
                                ToastUtils.showToast("Error al establecer modo de disparo: ${error.description()}")
                            }
                        })
                    }
                    override fun onFailure(error: IDJIError) {
                        ToastUtils.showToast("Error al establecer modo foto: ${error.description()}")
                    }
                })
            } else {
                ToastUtils.showToast("CameraManager no disponible.")
            }
        } else {
            scanningText = "Esperando conexión con el drone..."
            capturedImage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = scanningText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = capturedImage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                capturedImage?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Captured Drone View",
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .aspectRatio(16f / 9f)
                            .background(Color.Gray)
                    )
                }
            }

            AnimatedVisibility(
                visible = capturedImage == null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DroneScan_V5Theme {
        LoadingScreen("Iniciando aplicación...")
    }
}
