cmake_minimum_required(VERSION 3.22.1)
project(traductor_whisper LANGUAGES C CXX)
include(FetchContent)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
FetchContent_Declare(whisper_cpp
    GIT_REPOSITORY https://github.com/ggml-org/whisper.cpp.git
    GIT_TAG v1.7.6
)
FetchContent_MakeAvailable(whisper_cpp)
add_library(traductor_whisper SHARED whisper_jni.cpp)
find_library(log-lib log)
target_include_directories(traductor_whisper PRIVATE ${whisper_cpp_SOURCE_DIR}/include)
target_link_libraries(traductor_whisper PRIVATE whisper ${log-lib})
