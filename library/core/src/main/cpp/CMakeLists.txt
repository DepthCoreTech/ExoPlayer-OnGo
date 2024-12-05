# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

add_compile_options(-O3)

add_library( atphub SHARED IMPORTED )
set_target_properties(atphub PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/atphub/${ANDROID_ABI}/libatphub.so )

include_directories(${CMAKE_SOURCE_DIR}/../jniLibs/atphub/include)

aux_source_directory(. app_src)
set(DIR_SRCS ${app_src})

add_library( 	atphub-jni
             	SHARED
             	${DIR_SRCS}
			)


find_library( 	log-lib
              	log
			)

target_link_libraries(	atphub-jni
						${log-lib}
						atphub
						android
        			 )

