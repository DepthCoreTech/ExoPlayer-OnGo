package tech.depthcore.atphub;

import android.content.Context;
import android.content.res.AssetManager;

import com.google.android.exoplayer2.DeviceInfo;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.util.Log;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class ATPhub {

    static {
        System.loadLibrary( "atphub-jni" );
    }

    static public final String[] PositioningVoiceFiles = {
            "channel_l_mono.wav",
            "channel_r_mono.wav",
            "channel_sl_mono.wav",
            "channel_sr_mono.wav"
    };
    static public final String TAG = "[ATPhub]";


    ///////// JNI functions /////////////////////////////////////////////////////////////////////
    static private native int isServiceRunningJNI( );

    static private native int startServiceJNI( byte[] buffer, int length, int deviceMixing_2chs, int deviceMixing_6chs, int exteralMixing  );

    static private native int stopServiceJNI( );

    static private native int openPipeJNI( int sampleRate, int channels );

    static private native int closePipeJNI( );

    static private native int resumePipeJNI( );

    static private native int pausePipeJNI( );

    static private native int pipeForwardAndConvertJNI( byte[] buffer, int length );

    static private native int getConnectingSpeakersNumberJNI();

    static private native int setVolumeJNI( int vol );

    static private native int getSpeakerProfileDataJNI( byte[] buffer, int length );

    static private native int setSpeakerPositionsJNI( String spkNameL, String spkNameR, String spkNameSL, String spkNameSR );

    static private native int voicePositioningJNI( String spkName, String voiceFilePath );

    static private native int switchOnSpeakerPositioningModeJNI( );

    static private native int switchOffSpeakerPositioningModeJNI( );
    /////////////////////////////////////////////////////////////////////////////////////////////

    static private final String SpeakerProfileFileName = new String( "atp_spkprofile.dat" );

    static private byte[] audioBuffer = new byte[ 102400 ];
    static private byte[] profileBuffer = new byte[ 20480 ];
    static private ExoPlayer player = null;
    static private Player.Listener listener = null;
    static private int volumeLocalDevice = -1;
    static private int volumeExternalSpeaker = 0;
    static private int maxVolumeLocalDevice = 15;
    static private int sampleRateOfPipeline = 0;
    static private int channelsOfPipeline = 0;
    static private int preloadAudioPackage = 0;
    static private boolean isStartAndStopProcessing = false;
    static private final Object startAndStopProcessLock = new Object();

    static private void copyVoiceFilesToCache( Context context ) {
        AssetManager assets = context.getAssets( );
        InputStream input;
        OutputStream output;
        boolean isExist;
        byte[] buff = new byte[ 1024 ];
        int len;
        File cacheDir = context.getFilesDir( );

        for( int i = 0; i < 4; i++ ) {
            try {
                isExist = false;
                try {
                    context.openFileInput( PositioningVoiceFiles[ i ] ).close( );
                    isExist = true;
                } catch( FileNotFoundException fne ) {
                }

                if( !isExist ) {
                    Log.d( TAG, "..........copying voice file: " + PositioningVoiceFiles[ i ] );
                    input = assets.open( "atphub/" + PositioningVoiceFiles[ i ] );
                    output = context.openFileOutput( PositioningVoiceFiles[ i ], Context.MODE_PRIVATE );

                    while( ( len = input.read( buff ) ) != -1 ) {
                        output.write( buff, 0, len );
                    }

                    output.close( );
                    input.close( );
                }
            } catch( IOException e ) {
                Log.e( TAG, "Error!!! copying voice files to cache directory: " + PositioningVoiceFiles[ i ], e );
            }
        }
    }

    static private int readSpeakerProfileFromFile( Context context ) {

        try {
            DataInputStream inputStream = new DataInputStream( context.openFileInput( SpeakerProfileFileName ) );
            inputStream.readFully( profileBuffer );
            inputStream.close( );
        } catch( FileNotFoundException fne ) {
            return 0;
        } catch( IOException e ) {
            Log.e( TAG, "Error!!! reading speaker profile from file: '" + SpeakerProfileFileName + "'.", e );
            return 0;
        }

        return 20480;
    }

    static public boolean isServiceRunning() {
        return isServiceRunningJNI() == 1;
    }

    static public int startService( Context context, int deviceMixing2chs, int deviceMixing6chs, int externalMixing ) {

        synchronized( startAndStopProcessLock ) {
            if( isStartAndStopProcessing ) {
                return 0;
            }

            if( isServiceRunning() ) {
                return 0;
            }

            isStartAndStopProcessing = true;

            new Thread() {
                public void run() {
                    try {
                        copyVoiceFilesToCache( context );
                        int length = readSpeakerProfileFromFile( context );
                        int rst = startServiceJNI( profileBuffer, length, deviceMixing2chs, deviceMixing6chs, externalMixing );
                        if( rst != 0 ) {
                            Log.e( TAG, "Error!!! start AndroTPlay error: " + rst );
                        }
                    } finally {
                        isStartAndStopProcessing = false;
                    }
                }
            }.start();

            return 0;
        }

    }

    static public int stopService( Context context ) {
        synchronized( startAndStopProcessLock ) {
            if( isStartAndStopProcessing ) {
                return 0;
            }

            if( !isServiceRunning( ) ) {
                return 0;
            }

            isStartAndStopProcessing = true;

            new Thread( ) {
                public void run( ) {
                    try {
                        saveSpeakerProfileToFile( context );
                        stopServiceJNI( );
                    } finally {
                        isStartAndStopProcessing = false;
                    }
                }
            }.start( );

            return 0;
        }

    }

    static public int setSpeakerPositions( String spkNameL, String spkNameR, String spkNameSL, String spkNameSR ) {
        return setSpeakerPositionsJNI( spkNameL, spkNameR, spkNameSL, spkNameSR );
    }

    static public int startPipe( int sampleRate, int channels ) {
        Log.d( TAG, "........startPipe........" );
        sampleRateOfPipeline = sampleRate;
        channelsOfPipeline = channels;
        int rst;

        if( !isServiceRunning() ) {
            Log.w( TAG, "WARNING!!! The 'startPipe' operation is skipped because AndroTPlay is not running." );
            return -1;
        }

        if( ( rst = openPipeJNI( sampleRateOfPipeline, channelsOfPipeline ) ) == 0 ) {
            setVolumeJNI( volumeExternalSpeaker );
        }

        return rst;
    }

    static public int stopPipe( ) {
        Log.d( TAG, "........stopPipe........" );

        return closePipeJNI( );
    }

    static public int pausePipe( ) {
        Log.d( TAG, "........pausePipe........" );
        if( !isServiceRunning() ) {
            Log.w( TAG, "WARNING!!! The 'pausePipe' operation is skipped because AndroTPlay is not running." );
            return -1;
        }

        return pausePipeJNI( );
    }

    static public int resumePipe( ) {
        Log.d( TAG, "........resumePipe........" );
        if( !isServiceRunning() ) {
            Log.w( TAG, "WARNING!!! The 'resumePipe' operation is skipped because AndroTPlay is not running." );
            return -1;
        }

        return resumePipeJNI( );
    }

    static public int getConnectingSpeakersNumber() {
        return getConnectingSpeakersNumberJNI();
    }

    static public ByteBuffer redirectToPipe( ByteBuffer inputByteBuffer, boolean isPlaying ) {
        
        int length = inputByteBuffer.remaining( );
        inputByteBuffer.get( audioBuffer, 0, length );

        if( isPlaying ) {
            if( preloadAudioPackage != 0 ) {
                Log.d( TAG, "preloadAudioPackage.......done......" + preloadAudioPackage );
                preloadAudioPackage = 0;
            }
            length = pipeForwardAndConvertJNI( audioBuffer, length );
            if( length < 0 ) {
                Log.e( TAG, "Error!!! redirectToPipe failed: %d" + length );
                return inputByteBuffer;
            }
        } else {
            if( preloadAudioPackage == 0 ) {
                Log.d( TAG, "preloadAudioPackage.......start......" );
            }
            preloadAudioPackage++;
        }

        ByteBuffer audioByteBuffer = ByteBuffer.wrap( audioBuffer, 0, length );
        audioByteBuffer.order( ByteOrder.LITTLE_ENDIAN );
        return audioByteBuffer;
    }

    static public void setExoPlayer( ExoPlayer playerParam ) {
        if( playerParam != null ) {

            if( !isServiceRunning() ) {
                Log.w( TAG, "WARNING!!! The 'setExoPlayer' operation is skipped because AndroTPlay is not running." );
                return;
            }

            if( player == null ) {
                player = playerParam;
                DeviceInfo dev = player.getDeviceInfo( );
                maxVolumeLocalDevice = dev.maxVolume;
                setVolume( player.getDeviceVolume( ) );

                listener = new Player.Listener( ) {
                    public void onDeviceVolumeChanged( int volDev, boolean muted ) {
                        Log.d( TAG, "........onDeviceVolumeChanged changed: volDev=" + volDev + ", muted=" + muted );
                        if( muted ) {
                            setVolume( 0 );
                        } else {
                            setVolume( volDev );
                        }
                    }
                };
                player.addListener( listener );
                Log.d( TAG, "........bindExoPlayer: volumeLocal=" + volumeLocalDevice + "/" + maxVolumeLocalDevice + ", volumeSpk=" + volumeExternalSpeaker + "" );
            }
        } else {
            if( player != null ) {
                Log.d( TAG, "........unbindExoPlayer" );
                player.removeListener( listener );
                listener = null;
                player = null;
            }
        }
    }

    static public int setVolume( int volDev ) {
        if( !isServiceRunning() ) {
            Log.w( TAG, "WARNING!!! The 'setVolume' operation is skipped because AndroTPlay is not running." );
            return -1;
        }

        volumeLocalDevice = volDev;
        volumeExternalSpeaker = convertToExternalVolume( volumeLocalDevice );
        return setVolumeJNI( volumeExternalSpeaker );
    }

    static private int convertToExternalVolume( int volDev ) {
        return ( volDev * 100 ) / maxVolumeLocalDevice;
    }

    public static final class SpeakerProfile {

        public final String speakerName;    //format: SPK-XX, XX(SpeakerOrder) is the number of connected different speakerId.  E.g.: SPK-01, SPK-02...
        public int positionId;              //ID_SPEAKER_L / R / SL / SR
        public int speakerStatus;           //1: connected

        public SpeakerProfile( DataInputStream input ) throws IOException {
            byte[] buff = new byte[ 256 ];
            int len = 0;

            len = input.readShort( );
            input.readFully( buff, 0, len );
            speakerName = new String( buff, 0, len );

            positionId = input.readShort( );

            speakerStatus = input.readShort( );

            len = input.readShort( );
            input.readFully( buff, 0, len );

        }

        public SpeakerProfile( String name, int pid, int status ) {
            speakerName = name;
            positionId = pid;
            speakerStatus = status;
        }
    }

    public static int reloadSpeakerProfileList( List< SpeakerProfile > profileList ) {
        int number;
        int rst = 0;
        DataInputStream dataInput;
        SpeakerProfile spk;

        if( ( rst = getSpeakerProfileDataJNI( profileBuffer, 20480 ) ) > 0 ) {
            try {
                dataInput = new DataInputStream( new ByteArrayInputStream( profileBuffer ) );
                number = dataInput.readShort( );

                for( int i = 0; i < number; i++ ) {
                    profileList.add( new SpeakerProfile( dataInput ) );
                }
                dataInput.close( );
            } catch( Exception e ) {
                Log.e( TAG, "Error!!! reloadSpeakerProfileList failed.", e );
                rst = -3;
            }
        } else {
            Log.e( TAG, "Error!!! reloadSpeakerProfileList.getSpeakerProfileData failed: " + rst );
        }
        return rst;
    }

    public static int saveSpeakerProfileToFile( Context context ) {
        int number;
        int rst = 0;
        SpeakerProfile spk;

        if( ( rst = getSpeakerProfileDataJNI( profileBuffer, 20480 ) ) > 0 ) {
            try {
                FileOutputStream outputStream = context.openFileOutput( SpeakerProfileFileName, Context.MODE_PRIVATE );
                outputStream.write( profileBuffer );
                outputStream.close( );
            } catch( Exception e ) {
                Log.e( TAG, "Error!!! saveSpeakerProfileToFile failed.", e );
                rst = -3;
            }
        } else {
            Log.e( TAG, "Error!!! getSpeakerProfileDataJNI failed: " + rst );
        }
        return rst;
    }

    static public void voicePositioning( int positionId, String cachePath, String speakerName ) {
        if( !isServiceRunning() ) {
            Log.w( TAG, "WARNING!!! The 'voicePositioning' operation is skipped because AndroTPlay is not running." );
            return;
        }

        if( positionId >= 0 && positionId <= 3 ) {
            voicePositioningJNI( speakerName, cachePath + "/" + PositioningVoiceFiles[ positionId ] );
        } else {
            voicePositioningJNI( null, null );
        }
    }

    static public void switchOnSpeakerPositioningMode( ) {
        if( !isServiceRunning() ) {
            Log.w( TAG, "WARNING!!! The 'switchOnSpeakerPositioningMode' operation is skipped because AndroTPlay is not running." );
            return;
        }

        switchOnSpeakerPositioningModeJNI( );
    }

    static public void switchOffSpeakerPositioningMode( ) {
        if( !isServiceRunning() ) {
            Log.w( TAG, "WARNING!!! The 'switchOffSpeakerPositioningMode' operation is skipped because AndroTPlay is not running." );
            return;
        }

        switchOffSpeakerPositioningModeJNI( );
    }


}