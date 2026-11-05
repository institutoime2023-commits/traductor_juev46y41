# Instalación — TRADUCTOR V0.2

En Android Studio > SDK Manager instala Android SDK Platform 35, NDK 27.0.12077973 y CMake 3.22.1. Configura Gradle para usar JDK 17.

Abre el proyecto descomprimido, espera la sincronización y conecta el teléfono con depuración USB. Pulsa Run y concede permiso de micrófono. Descarga el modelo desde el botón de la app.

La primera compilación descarga whisper.cpp desde GitHub. No modifica Node, Docker, Nginx, n8n ni ningún servicio del VPS.

Si aparece un error, comparte el primer bloque completo de la ventana Build; no cambies versiones ni archivos al azar.
