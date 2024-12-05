#include <jni.h>
#include <string.h>
#include <errno.h>
#include <stdio.h>
#include <math.h>
#include <android/log.h>
#include <aaudio/AAudio.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>


#define logI(...) __android_log_print(ANDROID_LOG_INFO, "[ATPhub]", __VA_ARGS__)
#define logE(...) __android_log_print(ANDROID_LOG_ERROR, "[ATPhub]", __VA_ARGS__)

#include "atphub.h"

JNIEXPORT  jint JNICALL Java_tech_depthcore_atphub_ATPhub_isServiceRunningJNI(
        JNIEnv* env,
        jclass obj) {

    return atp_is_hub_service_running();
}

JNIEXPORT  jint JNICALL Java_tech_depthcore_atphub_ATPhub_startServiceJNI(
        JNIEnv* env,
        jclass obj,
        jbyteArray buffer, jint length, jint deviceMixing_2chs, jint deviceMixing_6chs, jint exteralMixing ) {
  int rst;

  if( ( rst = atp_set_device_mixing_mode( deviceMixing_2chs, deviceMixing_6chs ) ) < 0 ) {
    logE( "Error!!! setting device mixing mode: %d", rst );
  }

  if( ( rst = atp_set_external_speaker_mixing_mode( exteralMixing ) ) < 0 ) {
    logE( "Error!!! setting external speaker mixing mode: %d", rst );
  }

  jbyte *buff = (*env)->GetByteArrayElements( env, buffer, 0 );

  return atp_start_hub_service( "eth0,wlan0", 0, 300, buff, length, "242:52" );
}

JNIEXPORT  jint JNICALL Java_tech_depthcore_atphub_ATPhub_stopServiceJNI(
        JNIEnv* env,
        jclass obj) {

  return atp_stop_hub_service( );
}

JNIEXPORT  jint JNICALL Java_tech_depthcore_atphub_ATPhub_openPipeJNI(
    JNIEnv* env,
    jclass obj,
    jint sampleRate, jint channels) {

  return atp_open_streaming_pipeline( sampleRate, 16, channels );
}

JNIEXPORT  jint JNICALL Java_tech_depthcore_atphub_ATPhub_closePipeJNI(
    JNIEnv* env,
    jclass obj ) {

  return atp_close_streaming_pipeline();
}

JNIEXPORT  jint JNICALL Java_tech_depthcore_atphub_ATPhub_resumePipeJNI(
    JNIEnv* env,
    jclass obj ) {

  return atp_resume_streaming_pipeline();
}

JNIEXPORT  jint JNICALL Java_tech_depthcore_atphub_ATPhub_pausePipeJNI(
    JNIEnv* env,
    jclass obj ) {

  return atp_pause_streaming_pipeline();
}



JNIEXPORT  jint JNICALL Java_tech_depthcore_atphub_ATPhub_pipeForwardAndConvertJNI(
    JNIEnv* env,
    jclass obj,
    jbyteArray buffer, jint length ) {

  jbyte *buff = (*env)->GetByteArrayElements( env, buffer, 0 );

  return atp_pipe_forward_and_convert_data( buff, length );
}

JNIEXPORT  jint JNICALL Java_tech_depthcore_atphub_ATPhub_setVolumeJNI(
    JNIEnv* env,
    jclass obj, jint volume ) {
  return atp_set_volume( volume );
}

JNIEXPORT  jint JNICALL Java_tech_depthcore_atphub_ATPhub_getConnectingSpeakersNumberJNI(
        JNIEnv* env,
        jclass obj ) {
    return atp_get_connecting_speakers_number( );
}


JNIEXPORT  jint JNICALL Java_tech_depthcore_atphub_ATPhub_getSpeakerProfileDataJNI(
    JNIEnv* env,
    jclass obj,
    jbyteArray buffer, jint length ) {

  jbyte *buff = (*env)->GetByteArrayElements( env, buffer, 0 );

  return atp_get_speaker_profile_data( buff, length );
}

JNIEXPORT  jint JNICALL Java_tech_depthcore_atphub_ATPhub_setSpeakerPositionsJNI( JNIEnv* env, jclass obj,
    jstring spkNameL, jstring spkNameR, jstring spkNameSL, jstring spkNameSR ) {

      jbyte *spkL  = NULL;
      jbyte *spkR  = NULL;
      jbyte *spkSL = NULL;
      jbyte *spkSR = NULL;

      if( spkNameL != NULL ) {
        spkL = (*env)->GetStringUTFChars( env, spkNameL,  0 );
      }

      if( spkNameR != NULL ) {
        spkR = (*env)->GetStringUTFChars( env, spkNameR,  0 );
      }

      if( spkNameSL != NULL ) {
        spkSL = (*env)->GetStringUTFChars( env, spkNameSL, 0 );
      }

      if( spkNameSR != NULL ) {
        spkSR = (*env)->GetStringUTFChars( env, spkNameSR, 0 );
      }

      atp_set_speaker_positions( spkL, spkR, spkSL, spkSR );

      if( spkNameL != NULL ) {
        (*env)->ReleaseStringUTFChars(env, spkNameL, spkL);
      }
      if( spkNameR != NULL ) {
        (*env)->ReleaseStringUTFChars( env, spkNameR,  spkR  );
      }
      if( spkNameSL != NULL ) {
        (*env)->ReleaseStringUTFChars( env, spkNameSL, spkSL );
      }
      if( spkNameSR != NULL ) {
        (*env)->ReleaseStringUTFChars( env, spkNameSR, spkSR );
      }
      return 0;
}

JNIEXPORT  jint JNICALL Java_tech_depthcore_atphub_ATPhub_switchOnSpeakerPositioningModeJNI( JNIEnv* env, jclass obj ) {
    return atp_switch_on_speaker_positioning_mode();
}

JNIEXPORT  jint JNICALL Java_tech_depthcore_atphub_ATPhub_switchOffSpeakerPositioningModeJNI( JNIEnv* env, jclass obj ) {
    return atp_switch_off_speaker_positioning_mode();
}


JNIEXPORT  jint JNICALL Java_tech_depthcore_atphub_ATPhub_voicePositioningJNI( JNIEnv* env, jclass obj, jstring spkName, jstring voiceFilePath ) {

    jbyte *name     = NULL;
    jbyte *filePath = NULL;

    if( spkName != NULL ) {
        name = (*env)->GetStringUTFChars( env, spkName,  0 );
    }
    if( voiceFilePath != NULL ) {
        filePath = (*env)->GetStringUTFChars( env, voiceFilePath,  0 );
    }

    int rst = atp_set_voice_positioning( name, filePath );

    if( spkName != NULL ) {
        (*env)->ReleaseStringUTFChars( env, spkName, name );
    }
    if( voiceFilePath != NULL ) {
        (*env)->ReleaseStringUTFChars( env, voiceFilePath, filePath );
    }

    return rst;
}
