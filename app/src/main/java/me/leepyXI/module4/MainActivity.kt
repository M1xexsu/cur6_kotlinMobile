package me.leepyXI.module4

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.leepyXI.module4.ui.theme.MyApplicationTheme
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                LocationScreen(fusedLocationClient)
            }
        }
    }
}

@Composable
fun LocationScreen(fusedLocationClient: FusedLocationProviderClient) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var addressText by remember { mutableStateOf("Нажмите кнопку, чтобы узнать адрес") }
    var coordinatesText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            scope.launch {
                fetchLocation(context, fusedLocationClient, 
                    onLoading = { isLoading = it },
                    onSuccess = { addr, coords -> 
                        addressText = addr
                        coordinatesText = coords
                        errorMessage = null
                    },
                    onError = { errorMessage = it }
                )
            }
        } else {
            errorMessage = "Разрешение на геолокацию отклонено"
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .systemBarsPadding()
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(50.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Определяем местоположение...")
            } else {
                Text(
                    text = addressText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                    color = if (errorMessage != null) MaterialTheme.colorScheme.error else Color.Unspecified
                )

                if (coordinatesText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = coordinatesText,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val permissions = arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        if (permissions.all {
                                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                            }) {
                            scope.launch {
                                fetchLocation(context, fusedLocationClient,
                                    onLoading = { isLoading = it },
                                    onSuccess = { addr, coords ->
                                        addressText = addr
                                        coordinatesText = coords
                                        errorMessage = null
                                    },
                                    onError = { errorMessage = it }
                                )
                            }
                        } else {
                            permissionLauncher.launch(permissions)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Получить мой адрес", fontSize = 16.sp)
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
suspend fun fetchLocation(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onLoading: (Boolean) -> Unit,
    onSuccess: (String, String) -> Unit,
    onError: (String) -> Unit
) {
    onLoading(true)

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    if (!isGpsEnabled) {
        onLoading(false)
        onError("GPS выключен. Пожалуйста, включите его в настройках.")
        return
    }

    try {
        val location = withContext(Dispatchers.IO) {
            val task = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            )
            try {
                com.google.android.gms.tasks.Tasks.await(task)
            } catch (e: Exception) {
                null
            }
        }

        if (location != null) {
            val lat = location.latitude
            val lng = location.longitude
            val coordsString = String.format(Locale.US, "Lat: %.5f / Lng: %.5f", lat, lng)

            val address = withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale("ru"))
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val street = addr.thoroughfare ?: ""
                        val house = addr.subThoroughfare ?: ""
                        val city = addr.locality ?: addr.adminArea ?: ""
                        val country = addr.countryName ?: ""
                        
                        listOfNotNull(street.takeIf { it.isNotBlank() }, house.takeIf { it.isNotBlank() }, city, country)
                            .joinToString(", ")
                    } else {
                        "Адрес не найден"
                    }
                } catch (e: Exception) {
                    "Ошибка при получении адреса (проверьте интернет)"
                }
            }

            onSuccess(address, coordsString)
        } else {
            onError("Не удалось получить координаты. Попробуйте еще раз.")
        }
    } catch (e: Exception) {
        onError("Произошла ошибка: ${e.localizedMessage}")
    } finally {
        onLoading(false)
    }
}
