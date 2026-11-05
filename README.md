# TRADUCTOR Android — V0.2

**Package:** `com.orvex.traductor` · **Versión:** 0.2.0

## Función de este hito
- Descarga el modelo inglés `ggml-tiny.en.bin` (aprox. 75 MB) al almacenamiento privado de la app.
- Captura 6 segundos de audio mono a 16 kHz.
- Ejecuta Whisper localmente mediante whisper.cpp + C++/JNI.
- Muestra el texto reconocido en inglés.

El audio se mantiene en memoria: no se guarda ni se envía al VPS. La descarga inicial del modelo y la primera compilación sí necesitan Internet. Tras descargar el modelo, el reconocimiento puede funcionar sin conexión.

## Requisitos
- Android Studio, JDK 17 y Android SDK Platform 35.
- NDK `27.0.12077973`.
- CMake `3.22.1`.
- Teléfono Android ARM64; compilación configurada para `arm64-v8a`.

## Abrir y compilar
1. Descomprime el ZIP.
2. En Android Studio selecciona **Open** y elige `TRADUCTOR-ANDROID-V0.2`.
3. Espera la sincronización Gradle.
4. Instala las versiones de NDK y CMake indicadas si Android Studio las solicita.
5. Conecta el Redmi 12C con depuración USB y pulsa **Run**.

La primera compilación CMake descarga whisper.cpp desde GitHub y puede tardar varios minutos.

## Prueba
1. Abre TRADUCTOR y concede permiso de micrófono.
2. Pulsa **DESCARGAR MODELO**.
3. Reproduce una frase clara en inglés cerca del teléfono.
4. Pulsa **GRABAR Y RECONOCER 6 SEGUNDOS**.
5. Espera el texto reconocido.

## Límites de V0.2
Todavía no incluye traducción al español, voz TTS, escucha continua ni salida Bluetooth. El modelo tiny.en está orientado a inglés. El ZIP contiene el código fuente; no incluye APK precompilada ni el modelo.
