@echo off
"D:\\androidSDK\\cmake\\3.22.1\\bin\\cmake.exe" ^
  "-HD:\\opencv\\opencv\\libcxx_helper" ^
  "-DCMAKE_SYSTEM_NAME=Android" ^
  "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON" ^
  "-DCMAKE_SYSTEM_VERSION=21" ^
  "-DANDROID_PLATFORM=android-21" ^
  "-DANDROID_ABI=armeabi-v7a" ^
  "-DCMAKE_ANDROID_ARCH_ABI=armeabi-v7a" ^
  "-DANDROID_NDK=D:\\androidSDK\\ndk\\21.1.6352462" ^
  "-DCMAKE_ANDROID_NDK=D:\\androidSDK\\ndk\\21.1.6352462" ^
  "-DCMAKE_TOOLCHAIN_FILE=D:\\androidSDK\\ndk\\21.1.6352462\\build\\cmake\\android.toolchain.cmake" ^
  "-DCMAKE_MAKE_PROGRAM=D:\\androidSDK\\cmake\\3.22.1\\bin\\ninja.exe" ^
  "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=D:\\opencv\\opencv\\build\\intermediates\\cxx\\Debug\\4m242y3z\\obj\\armeabi-v7a" ^
  "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=D:\\opencv\\opencv\\build\\intermediates\\cxx\\Debug\\4m242y3z\\obj\\armeabi-v7a" ^
  "-DCMAKE_BUILD_TYPE=Debug" ^
  "-BD:\\opencv\\opencv\\.cxx\\Debug\\4m242y3z\\armeabi-v7a" ^
  -GNinja ^
  "-DANDROID_STL=c++_shared"
