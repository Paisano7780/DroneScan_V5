package com.dronescan.msdksample // ¡IMPORTANTE! Este paquete DEBE coincidir con tu applicationId y la estructura de carpetas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dronescan.msdksample.ui.theme.DroneScan_V5Theme // Asegúrate de que esta ruta sea correcta

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Envuelve tu contenido Compose con tu tema de aplicación
            DroneScan_V5Theme {
                // Un contenedor de superficie que usa el color de fondo del tema
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Aquí puedes poner tu interfaz de usuario inicial.
                    // Por ahora, solo un saludo simple.
                    Greeting("DJI DroneScan App")
                }
            }
        }
    }
}

// Una función Composable simple para mostrar un texto
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

// Una vista previa de tu Composable para verla en el editor de Android Studio
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DroneScan_V5Theme {
        Greeting("Android")
    }
}
