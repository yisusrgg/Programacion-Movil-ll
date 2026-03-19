package com.example.futbolitopocket.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ricknout.composesensors.accelerometer.isAccelerometerSensorAvailable
import dev.ricknout.composesensors.accelerometer.rememberAccelerometerSensorValueAsState
import kotlinx.coroutines.android.awaitFrame

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun FutbolitoScreen(){
    // Check if accelerometer sensor is available
    val available = isAccelerometerSensorAvailable()
    if(!available){
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Acelerómetro no disponible")
        }
        return
    }

    //val sensor = getAccelerometerSensor()
    // Remember accelerometer sensor value as State that updates as SensorEvents arrive
    val sensorValue by rememberAccelerometerSensorValueAsState()
    var equipoArriba by remember { mutableIntStateOf(0) }
    var equipoAbajo by remember { mutableIntStateOf(0) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        //Valores de la pantalla entera
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        // Estados de Física (Posición y Velocidad)
        var posX by remember { mutableFloatStateOf(width / 2) }
        var posY by remember { mutableFloatStateOf(height / 2) }
        var velX by remember { mutableFloatStateOf(0f) }
        var velY by remember { mutableFloatStateOf(0f) }

        // Definimos el tamaño de las porterías (40% del ancho de la pantalla)
        val anchoPorteria = width * 0.4f
        val altoPorteria = 40f
        val anchoSinPorteriaIndividual = (width - anchoPorteria) / 2
        val limiteIzqPorteria = anchoSinPorteriaIndividual
        val limiteDerPorteria = anchoSinPorteriaIndividual + anchoPorteria
        val radioPelota = 35f


        LaunchedEffect(Unit) {
            while (true) {
                awaitFrame()
                val (x, y, z) = sensorValue.value
                val rebote = -0.6f

                //Aceleracion
                velX -= x * 0.5f
                velY += y * 0.5f
                // Friccion
                velX *= 0.98f
                velY *= 0.98f
                // Movimiento
                posX += velX
                posY += velY

                // Choca con paredes derecha izquierda
                if (posX - radioPelota < 0) {
                    posX = radioPelota
                    velX *= rebote
                } else if (posX + radioPelota > width) {
                    posX = width - radioPelota
                    velX *= rebote
                }

                //choca arriba
                if (posY - radioPelota < 0) {
                    //si entra o no en la porteria
                    if (posX >= limiteIzqPorteria && posX <= limiteDerPorteria) {
                        equipoAbajo++
                        //Se reinicia la posicion de la pelota y los valores
                        posX = width / 2
                        posY = height / 2
                        velX = 0f
                        velY = 0f
                    } else {
                        posY = radioPelota
                        velY *= rebote
                    }
                }
                //choca abajo
                if (posY + radioPelota > height) {
                    if(posX>= limiteIzqPorteria && posX<=limiteDerPorteria){
                        equipoArriba++ // ¡Gol!
                        posX = width / 2
                        posY = height / 2
                        velX = 0f
                        velY = 0f
                    } else {
                        posY = height - radioPelota
                        velY *= rebote
                    }
                }
            }
        }


        Canvas(modifier = Modifier.fillMaxSize()) {
            //Cancha
            drawRect(
                color = Color(0xFF8BC34A),
                size = Size(width, height)
            )
            // Lineas de la cancha
            drawRect(
                color = Color.White,
                size = Size(width, height),
                style = Stroke(width = 12f)
            )
            drawLine(
                color = Color.White,
                start = Offset(0f, height / 2), end = Offset(width, height / 2),
                strokeWidth = 12f
            )
            // Circulo central de la cancha
            drawCircle(
                color = Color.White,
                radius = 100f,
                center = Offset(width / 2, height / 2),
                style = Stroke(width = 12f)
            )

            // Porteria Arriba
            drawRect(
                color = Color.Blue,
                topLeft = Offset(limiteIzqPorteria, 0f),
                size = Size(anchoPorteria, altoPorteria)
            )
            // Porteria Abajo
            drawRect(
                color = Color.Yellow,
                topLeft = Offset(limiteIzqPorteria, height - altoPorteria),
                size = Size(anchoPorteria, altoPorteria)
            )

            // Pelota
            drawCircle(
                color = Color.Black,
                radius = radioPelota,
                center = Offset(posX, posY)
            )
            drawCircle(
                color = Color.White,
                radius = radioPelota * 0.7f,
                center = Offset(posX, posY)
            )
        }

        // Marcadores
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Pumas: $equipoArriba",
                fontSize = 24.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 30.dp)
            )
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "América: $equipoAbajo",
                fontSize = 24.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 30.dp)
            )
        }
    }
}