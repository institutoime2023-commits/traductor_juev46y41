#include <jni.h>
#include <string>
#include <vector>
#include "whisper.h"

extern "C"
JNIEXPORT jstring JNICALL
Java_com_orvex_traductor_WhisperBridge_transcribe(
        JNIEnv *env, jobject, jstring model_path, jfloatArray audio_samples) {
    const char *path = env->GetStringUTFChars(model_path, nullptr);
    if (!path) return env->NewStringUTF("Error: ruta del modelo inválida.");
    jsize length = env->GetArrayLength(audio_samples);
    std::vector<float> samples(static_cast<size_t>(length));
    env->GetFloatArrayRegion(audio_samples, 0, length, samples.data());

    whisper_context_params cp = whisper_context_default_params();
    cp.use_gpu = false;
    whisper_context *ctx = whisper_init_from_file_with_params(path, cp);
    env->ReleaseStringUTFChars(model_path, path);
    if (!ctx) return env->NewStringUTF("Error: no se pudo cargar el modelo tiny.en.");

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.n_threads = 4;
    params.translate = false;
    params.no_context = true;
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.language = "en";

    int rc = whisper_full(ctx, params, samples.data(), static_cast<int>(samples.size()));
    std::string output;
    if (rc == 0) {
        for (int i = 0; i < whisper_full_n_segments(ctx); ++i) {
            const char *part = whisper_full_get_segment_text(ctx, i);
            if (part) output += part;
        }
    } else output = "Error durante el reconocimiento de voz.";
    whisper_free(ctx);
    return env->NewStringUTF(output.c_str());
}
