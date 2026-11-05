package com.orvex.traductor

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

class MainActivity : ComponentActivity() {
    private val permissionGranted = mutableStateOf(false)
    private val status = mutableStateOf("Prepara el modelo para comenzar.")
    private val modelProgress = mutableFloatStateOf(0f)
    private val modelReady = mutableStateOf(false)
    private val busy = mutableStateOf(false)
    private val recognizedText = mutableStateOf("")
    private val modelName = "ggml-tiny.en.bin"

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted.value = granted
        status.value = if (granted) "Permiso concedido. Pulsa reconocer para iniciar." else "Permiso de micrófono denegado."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionGranted.value = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val model = File(filesDir, "models/$modelName")
        modelReady.value = model.exists() && model.length() > 10_000_000L

        setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("TRADUCTOR", style = MaterialTheme.typography.headlineMedium)
                        Text("V0.2 · Reconocimiento local de inglés")
                        HorizontalDivider()
                        Text("1. MODELO DE RECONOCIMIENTO", style = MaterialTheme.typography.titleMedium)
                        Text(if (modelReady.value) "Modelo tiny.en instalado. Puede funcionar sin Internet." else "Descarga inicial aproximada: 75 MB. Se guarda en el teléfono.")
                        if (busy.value && !modelReady.value && modelProgress.value > 0f) {
                            LinearProgressIndicator(progress = modelProgress.value, modifier = Modifier.fillMaxWidth())
                            Text("${(modelProgress.value * 100).toInt()}%")
                        }
                        Button(
                            enabled = !busy.value && !modelReady.value,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                scope.launch {
                                    busy.value = true
                                    modelProgress.floatValue = 0f
                                    status.value = "Descargando modelo..."
                                    try {
                                        downloadModel()
                                        modelReady.value = true
                                        status.value = "Modelo descargado correctamente."
                                    } catch (e: Exception) {
                                        status.value = "Error al descargar: ${e.message ?: "desconocido"}"
                                    } finally { busy.value = false }
                                }
                            }
                        ) { Text(if (modelReady.value) "MODELO INSTALADO" else "DESCARGAR MODELO") }

                        HorizontalDivider()
                        Text("2. CAPTURAR Y RECONOCER", style = MaterialTheme.typography.titleMedium)
                        Text("El teléfono escuchará durante 6 segundos y reconocerá las palabras en inglés.")
                        Button(
                            enabled = modelReady.value && !busy.value,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (!permissionGranted.value) {
                                    permissionRequest.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    scope.launch {
                                        busy.value = true
                                        recognizedText.value = ""
                                        status.value = "Escuchando 6 segundos..."
                                        try {
                                            val audio = withContext(Dispatchers.IO) { captureSixSeconds() }
                                            status.value = "Reconociendo en el teléfono..."
                                            val modelPath = File(filesDir, "models/$modelName").absolutePath
                                            val result = withContext(Dispatchers.Default) {
                                                WhisperBridge.transcribe(modelPath, audio)
                                            }
                                            recognizedText.value = result.ifBlank { "No se detectó voz." }
                                            status.value = "Proceso terminado. Audio no guardado."
                                        } catch (e: Exception) {
                                            status.value = "Error: ${e.message ?: "no se pudo procesar el audio"}"
                                        } finally { busy.value = false }
                                    }
                                }
                            }
                        ) { Text("GRABAR Y RECONOCER 6 SEGUNDOS") }

                        if (busy.value && modelReady.value) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Estado: ${status.value}")
                        Text("TEXTO RECONOCIDO", style = MaterialTheme.typography.titleMedium)
                        Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                            Text(recognizedText.value.ifBlank { "Aquí aparecerá el texto reconocido en inglés." }, Modifier.padding(16.dp))
                        }
                        Text("Privacidad: la captura se mantiene en memoria. No se guarda ni se envía audio al VPS.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    private suspend fun downloadModel() = withContext(Dispatchers.IO) {
        val directory = File(filesDir, "models")
        if (!directory.exists() && !directory.mkdirs()) throw IllegalStateException("No se pudo crear carpeta del modelo")
        val target = File(directory, modelName)
        val temp = File(directory, "$modelName.download")
        val connection = (URL("https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin").openConnection() as HttpURLConnection).apply {
            connectTimeout = 20000
            readTimeout = 60000
            instanceFollowRedirects = true
        }
        try {
            connection.connect()
            if (connection.responseCode !in 200..299) throw IllegalStateException("HTTP ${connection.responseCode}")
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                temp.outputStream().use { output ->
                    val buffer = ByteArray(65536)
                    var received = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        received += n
                        if (total > 0) modelProgress.floatValue = (received.toFloat() / total).coerceIn(0f, 1f)
                    }
                }
            }
            if (temp.length() < 10_000_000L) throw IllegalStateException("Modelo incompleto")
            if (!temp.renameTo(target)) { temp.copyTo(target, overwrite = true); temp.delete() }
            modelProgress.floatValue = 1f
        } finally {
            connection.disconnect()
            if (temp.exists()) temp.delete()
        }
    }

    private fun captureSixSeconds(): FloatArray {
        val rate = 16000
        val count = rate * 6
        val minBuffer = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuffer <= 0) throw IllegalStateException("No se pudo inicializar el micrófono")
        val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, max(minBuffer, count * 2))
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IllegalStateException("Micrófono no disponible")
        }
        val pcm = ShortArray(count)
        try {
            recorder.startRecording()
            var offset = 0
            while (offset < count) {
                val n = recorder.read(pcm, offset, count - offset)
                if (n < 0) throw IllegalStateException("Error de lectura de micrófono: $n")
                offset += n
            }
        } finally {
            try { recorder.stop() } catch (_: Exception) {}
            recorder.release()
        }
        return FloatArray(count) { pcm[it] / 32768.0f }
    }
}

object WhisperBridge {
    init { System.loadLibrary("traductor_whisper") }
    external fun transcribe(modelPath: String, samples: FloatArray): String
}
