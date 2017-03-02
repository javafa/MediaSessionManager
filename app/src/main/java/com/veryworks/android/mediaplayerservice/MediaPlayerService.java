package com.veryworks.android.mediaplayerservice;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.media.MediaPlayer;
import android.media.Rating;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import static android.app.PendingIntent.getService;

/*
    인터넷에서 퍼왔으나 출처를 모름 ;
    버전별 예외처리만 추가 -> icon resource 사용하는 부분 res_id -> Icon 으로 변경
 */

public class MediaPlayerService extends Service {
    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_REWIND = "action_rewind";
    public static final String ACTION_FAST_FORWARD = "action_fast_foward";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";

    // 1. 미디어플레이어 사용 API 세팅
    private MediaPlayer mMediaPlayer;
    private MediaSessionManager mManager;
    private MediaSession mSession;
    private MediaController mController;

    // 2. Intent Action 에 넘어온 명령어를 분기시키는 함수
    private void handleIntent( Intent intent ) {
        if( intent == null || intent.getAction() == null )
            return;
        String action = intent.getAction();
        if( action.equalsIgnoreCase( ACTION_PLAY ) ) {

            mController.getTransportControls().play();

        } else if( action.equalsIgnoreCase( ACTION_PAUSE ) ) {
            mController.getTransportControls().pause();
        } else if( action.equalsIgnoreCase( ACTION_FAST_FORWARD ) ) {
            mController.getTransportControls().fastForward();
        } else if( action.equalsIgnoreCase( ACTION_REWIND ) ) {
            mController.getTransportControls().rewind();
        } else if( action.equalsIgnoreCase( ACTION_PREVIOUS ) ) {
            mController.getTransportControls().skipToPrevious();
        } else if( action.equalsIgnoreCase( ACTION_NEXT ) ) {
            mController.getTransportControls().skipToNext();
        } else if( action.equalsIgnoreCase( ACTION_STOP ) ) {
            mController.getTransportControls().stop();
        }
    }

    // Notification.Action -> API Level 19
    // Activity 에서의 클릭 버튼 생성
    private Notification.Action generateAction(int icon, String title, String intentAction ) {
        Intent intent = new Intent( getApplicationContext(), MediaPlayerService.class );
        intent.setAction( intentAction );
        // PendingIntent : 실행 대상이 되는 인텐트를 지연시키는 역할
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);

        // 버전별 Action Builder
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            Icon iconTemp = Icon.createWithResource(getBaseContext(),icon);
            return new Notification.Action.Builder(iconTemp, title, pendingIntent).build();
        }else {
            return new Notification.Action.Builder(icon, title, pendingIntent).build();
        }
    }

    // 노티바를 생성하는 함수
    private void buildNotification( Notification.Action action , String action_flag) {
        // 노티바 모양
        Notification.MediaStyle style = new Notification.MediaStyle();

        // 노티바 전체를 클릭했을 때 실행되는 메인 intent
        Intent intent = new Intent( getApplicationContext(), MediaPlayerService.class );
        intent.setAction( ACTION_STOP );
        PendingIntent pendingIntent = getService(getApplicationContext(), 1, intent, 0);

        // 노티바 생성
        Notification.Builder builder = new Notification.Builder( this );

        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle( "Beautiful" )
                .setContentText( "Crush" );

        // 퍼즈일 경우만 노티 삭제 가능
        if(ACTION_PAUSE.equals(action_flag)) {
            builder.setDeleteIntent(pendingIntent);
            builder.setOngoing(true);
        }

        builder.setStyle(style);

        builder.addAction( generateAction( android.R.drawable.ic_media_previous, "Previous", ACTION_PREVIOUS ) );
        builder.addAction( generateAction( android.R.drawable.ic_media_rew, "Rewind", ACTION_REWIND ) );
        builder.addAction( action );
        builder.addAction( generateAction( android.R.drawable.ic_media_ff, "Fast Foward", ACTION_FAST_FORWARD ) );
        builder.addAction( generateAction( android.R.drawable.ic_media_next, "Next", ACTION_NEXT ) );

        // OS 별로 액션의 개수가 정해저 있으므로
        // Action 의 중요도에 따라 꼭 보여져야 되는 Action을 앞쪽에 배치한다. 번호는 순서대로 0번부터...
        style.setShowActionsInCompactView(1,2,3,0,4);

        NotificationManager notificationManager = (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE );
        // 노티바를 화면에 보여준다
        notificationManager.notify( 1, builder.build() );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if( mManager == null ) {
            initMediaSessions();
        }

        handleIntent( intent );
        return super.onStartCommand(intent, flags, startId);
    }

    private void initMediaSessions() {
        mMediaPlayer = new MediaPlayer();

        Uri musicUri = Uri.parse("음원 uri");

        try {
            mMediaPlayer.setDataSource(musicUri.getPath());
            mMediaPlayer.prepare();
        }catch(Exception e){
            e.printStackTrace();
        }

        mSession = new MediaSession(getApplicationContext(), "Media Player Session");

        mController = new MediaController(getApplicationContext(), mSession.getSessionToken());

        mSession.setCallback(new MediaSession.Callback(){

            @Override
            public void onPlay() {
                super.onPlay();
                Log.e( "MediaPlayerService", "onPlay");
                buildNotification( generateAction( android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE ), ACTION_PAUSE );
            }

            @Override
            public void onPause() {
                super.onPause();
                Log.e( "MediaPlayerService", "onPause");
                buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY), ACTION_PLAY);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                Log.e( "MediaPlayerService", "onSkipToNext");
                //Change media here
                buildNotification( generateAction( android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE ), ACTION_PAUSE );
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                Log.e( "MediaPlayerService", "onSkipToPrevious");
                //Change media here
                buildNotification( generateAction( android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE ), ACTION_PAUSE );
            }

            @Override
            public void onFastForward() {
                super.onFastForward();
                Log.e( "MediaPlayerService", "onFastForward");
                //Manipulate current media here
            }

            @Override
            public void onRewind() {
                super.onRewind();
                Log.e( "MediaPlayerService", "onRewind");
                //Manipulate current media here
            }

            @Override
            public void onStop() {
                super.onStop();
                Log.e( "MediaPlayerService", "onStop");
                //Stop media player here
                NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel( 1 );
                Intent intent = new Intent( getApplicationContext(), MediaPlayerService.class );
                stopService( intent );
            }

            @Override
            public void onSeekTo(long pos) {
                super.onSeekTo(pos);
            }

            @Override
            public void onSetRating(Rating rating) {
                super.onSetRating(rating);
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
