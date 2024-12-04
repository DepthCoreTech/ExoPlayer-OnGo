#if defined (__cplusplus)
extern "C" {
#endif


#ifndef ATP_HUB_H
#define ATP_HUB_H

#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <sys/types.h>

//The stereo signals(2 channels) are not transmitted to the sink. Device speakers are silent when TPLAY is working.
#define DEVICE_2CHS_DENY        0

//When TPLAY is working, the device speakers and external speakers play stereo signals synchronously.
#define DEVICE_2CHS_PASS_ALL    1

//The 5.1 surround sound signals(6 channels) are not transmitted to the sink. Device speakers are silent when TPLAY is working.
#define DEVICE_6CHS_DENY        0

//When TPLAY is working,  signals for all 5.1 surround sound channels are transmitted to the sink.
#define DEVICE_6CHS_PASS_ALL    1

//When TPLAY is working,  signals for Center and LFE channel of 5.1 surround sound are transmitted to the sink.
#define DEVICE_6CHS_PASS_C_LFE  2

//No mixing, the signals for FL/FR/SL/SR channels of 5.1 surround sound are directly transmitted to respective external speakers.
#define EXTERNAL_4SPK_MIXING_6CHS_NONE                  1

//Mixing FL=FL+C+LFE and FR=FR+C+LEF, then transmit FL / FR / SL / SR channels to respective external speakers.
#define EXTERNAL_4SPK_MIXING_6CHS_CLFE_FLFR             2

//Mixing FL=FL+C+LFE and FR=FR+C+LEF and SL=SL+LFE and SR=SR+LFE, then transmit FL / FR / SL / SR channels to respective external speakers.
#define EXTERNAL_4SPK_MIXING_6CHS_CLFE_FLFR_LFE_SLSR    3

//Mixing FL=FL+LFE, FR=FR+LEF, SL=SL+LFE, SR=SR+LFE , then transmit FL / FR / SL / SR channels to respective external speakers.
#define EXTERNAL_4SPK_MIXING_6CHS_LFE_FLFRSLSR          4

int atp_major_version();
int atp_minor_version();
int atp_build_version();
int atp_is_hub_service_running();
int atp_set_device_mixing_mode( int audio2chs, int audio6chs );
int atp_set_external_speaker_mixing_mode( int exter4spk );
int atp_start_hub_service( char *ifName, int deviceSpeakerDelayMs, int externalSpeakerDelayMs, char *positionBuffer, int positionBufferLength, char *debug );
int atp_stop_hub_service();
int atp_open_streaming_pipeline( int sampleRate, int bitDepth, int channels );
int atp_close_streaming_pipeline();
int atp_resume_streaming_pipeline();
int atp_pause_streaming_pipeline();
int atp_pipe_forward_and_convert_data( char* buffer, int length );
int atp_set_volume( int volume );
int atp_get_speaker_profile_data( char *buffer, int length );
int atp_set_speaker_positions( char *spkName_L, char *spkName_R, char *spkName_SL, char *spkName_SR );
int atp_switch_on_speaker_positioning_mode();
int atp_switch_off_speaker_positioning_mode();
int atp_set_voice_positioning( char *spkName, char *voiceFilePath );
int atp_get_connecting_speakers_number();

#endif

#if defined (__cplusplus)
}
#endif
