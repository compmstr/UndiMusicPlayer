package com.undi.musicplayer;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class MusicPlayerService extends Service{
  public static enum MessageCode{
    GET_FILE_LIST, GET_PLAYLISTS, GET_STATUS,
    PLAY_FILE, PAUSE, RESTART, PREV, NEXT, SHUFFLE, REPEAT_ONE, REPEAT_ALL,
    ERROR;
  };
  public static enum PlayerStatus{
    PLAYING, PAUSED, STOPPED
  }
  
  public static class Flag{
    public final static int SHUFFLE = 0x1;
    public final static int REPEAT_ONE = 0x2;
    public final static int REPEAT_ALL =0x4;
    
    public static boolean checkFlag(int value, int flag){
      return ((value & flag) != 0);
    }
    
    public static int setFlag(int value, int flag){
      return (value | flag);
    }
    public static int clearFlag(int value, int flag){
      return (value & ~flag);
    }
  };
  
  private File[] musicFiles;
  private String[] playlist;
  private int currentFileInPlaylist;
  
  private MediaPlayer mPlayer = null;
  
  //private String status;
  private MusicPlayerStatus status;
  private int flags;
  
  private BroadcastReceiver eventReceiver = new BroadcastReceiver(){
    @Override
    public void onReceive(Context context, Intent intent) {
      Log.d("MusicPlayerService", "Got a broadcast: " + intent.getAction() + " -- " + intent.getScheme()
          + " -- " + intent.getDataString());
    }
  };
  
  //final RemoteCallbackList<IUndiMusicCallback> mCallback
  //  = new RemoteCallbackList<IUndiMusicCallback>();
  private IUndiMusicCallback.Stub mRemoteServiceStub = new IUndiMusicCallback.Stub(){

    @Override
    public MusicPlayerResponse sendCommand(MusicPlayerCommand command)
        throws RemoteException {
      Log.d("MusicPlayerService", "Got a Command: " + command.message.toString() + " - " + command.data);
      switch(command.message){
      case GET_STATUS:
        return new MusicPlayerResponse(status, "Here's your Status", command.message);
      case GET_FILE_LIST:
        StringBuilder fileList = new StringBuilder();
        if(musicFiles == null){
          Log.e("MusicPlayerService", "musicFiles list is null (Should be at least empty array)");
          throw new RemoteException();
        }
        for(File file : musicFiles){
          fileList.append(file.getAbsolutePath()).append(";");
        }
        return new MusicPlayerResponse(status, fileList.toString(), command.message);
      case PLAY_FILE:
        String fileRequested = command.data;
        Log.d("MusicPlayerService", "File requested to play: " + fileRequested);
        String fileToPlay = null;
        for(File file : musicFiles){
          if(file.getName().equals(fileRequested)){
            fileToPlay = file.getAbsolutePath();
            break;
          }
        }
        if(fileToPlay == null){
          return new MusicPlayerResponse(status, "Error: Can't find file: " + fileRequested, MessageCode.ERROR);
        }
        playlist = new String[1];
        playlist[0] = fileToPlay;
        currentFileInPlaylist = 0;
        playFile(playlist[currentFileInPlaylist]);
        return new MusicPlayerResponse(status, "Starting to play file: " + fileToPlay, command.message);
      }
      return null;
    }
    
  };
  
  private MediaPlayer.OnErrorListener mediaPlayerError = new MediaPlayer.OnErrorListener() {  
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
      if(what == MediaPlayer.MEDIA_ERROR_SERVER_DIED){
        Log.e("MusicPlayerService", "Connection to audio player died");
        mPlayer = null;
      }
      mPlayer = null;
      return false;
    }
  };
  
  private MediaPlayer.OnCompletionListener mediaPlayerFinished = new MediaPlayer.OnCompletionListener() {   
    @Override
    public void onCompletion(MediaPlayer mp) {
      playNextFile();
    }
  };
  
  private void playNextFile(){
    boolean endOfPlaylist = (this.playlist.length <= (currentFileInPlaylist + 1));
    if(Flag.checkFlag(status.flags, Flag.REPEAT_ONE)){
      playFile(playlist[currentFileInPlaylist]);
      return;
    }
    if(endOfPlaylist){
      if(Flag.checkFlag(this.status.flags, Flag.REPEAT_ALL)){
        currentFileInPlaylist = 0;
        playFile(playlist[0]);
        return;
      }else{
        this.status.status = PlayerStatus.STOPPED;
        this.status.file = "";
        mPlayer.reset();
      }
    }else{
      currentFileInPlaylist++;
      playFile(playlist[currentFileInPlaylist]);
      return;
    }
  }
  
  private void playFile(String fileToPlay){
    this.status.file = fileToPlay;
    if(mPlayer == null){
      mPlayer = new MediaPlayer();
      mPlayer.setOnErrorListener(mediaPlayerError);
      mPlayer.setOnCompletionListener(mediaPlayerFinished);
    }
    mPlayer.reset();
    try {
      mPlayer.setDataSource(fileToPlay);
      mPlayer.prepare();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (IllegalStateException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    startPlayback();
  }
  
  private void startPlayback(){
    mPlayer.start();
    if(mPlayer.isPlaying()){
      this.status.status = PlayerStatus.PLAYING;
    }else{
      this.status.status = PlayerStatus.STOPPED;
    }
  }
  
  private void stopPlayback(){
    if(mPlayer.isPlaying()){
      mPlayer.stop();
      this.status.status = PlayerStatus.STOPPED;
    }
  }
  
  private void pausePlayback(){
    if(mPlayer.isPlaying()){
      mPlayer.pause();
      this.status.status = PlayerStatus.PAUSED;
    }
  }
  
  @Override
  public IBinder onBind(Intent intent) {
    Log.d("MusicPlayerService", "onBind");
    return this.mRemoteServiceStub;
  }
  
  //Handle to the notification manager
  NotificationManager mNM;
  
  public static class MusicPlayerStatus
  implements Serializable{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    //Possible values:
    /* Playing
     * Paused
     * Stopped - set to this on startup, with blank file
     * 
     */
    public PlayerStatus status;
    //If not Stopped or Working, will contain the filename playing
    public String file;
    //An or'd list of bit flags
    public int flags;
    
    public String toString(){
      return "[MusicPlayerStatus - " + status + " : " + (file == "" ? "[No File]" : file) + " --Flags: " + flags + "]";
    }
    
    public MusicPlayerStatus(PlayerStatus status, String file, int flags){
      this.status = status;
      this.file = file;
      this.flags = flags;
    }
  }

  @Override
  public void onCreate(){
    Log.d("MusicPlayerService", "Music Player Service onCreate");
    mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    showNotification();
    //Set up all the actions we want to be notified of
    IntentFilter iFilter = new IntentFilter();
    iFilter.addAction(Intent.ACTION_HEADSET_PLUG);
    //iFilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
    //iFilter.addAction(Intent.ACTION_ANSWER);
    iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
    iFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
    iFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
    iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
    //the file one is needed to get the MEDIA_* intents... trying to find documentation saying that
    iFilter.addDataScheme("file");
    Looper myLooper = Looper.myLooper();
    if(myLooper == null){
      Log.d("MusicPlayerService", "Music player service has no looper");
    }
    this.registerReceiver(this.eventReceiver, iFilter, null, new Handler(myLooper));
  }
  @Override
  public void onDestroy(){
    this.unregisterReceiver(this.eventReceiver);
    Log.d("MusicPlayerService", "Music Player Service onDestroy");
    //Stop the notifications
    mNM.cancel(R.string.musicplayer_service_started);
    //Tell the user we stopped
    Toast.makeText(this, R.string.musicplayer_service_stopped, Toast.LENGTH_SHORT).show();
    if(mPlayer.isPlaying()){
      mPlayer.stop();
    }
    mPlayer.release();
  }

  private void getFileList(){
    String sdCardState = Environment.getExternalStorageState();
    Log.d("MusicPlayerService", "SD Card State: " + sdCardState);
    File sdCard = Environment.getExternalStorageDirectory();
    File musicDir;
    if(sdCard.isDirectory()){
      Log.d("MusicPlayerService", "SD Card Path: " + sdCard.getAbsolutePath());
      musicDir = new File(sdCard.getAbsolutePath() + "/Music/");
      this.musicFiles = musicDir.listFiles(new FileFilter(){
        private final String[] extensions = 
          new String[] {"mp3", "ogg", "wav", "mid", "midi", "3gp"};
        public boolean accept(File pathname){
          for(String curSuffix : extensions){
            if(pathname.getName().toLowerCase().endsWith(curSuffix)){
              return true;
            }
          }
          return false;
        }
      });
      if(this.musicFiles == null){
        this.musicFiles = new File[0];
      }
      Log.d("MusicPlayerService", "Music files in Music Directory: " + musicFiles);
    }
  }
  
  @Override
  public void onStart(Intent intent, int startid){
    this.getFileList();
    this.status = new MusicPlayerStatus(PlayerStatus.STOPPED, "", 0);
  }
  
  public void showNotification(){
    CharSequence text = getText(R.string.musicplayer_service_started);
    Notification notification = new Notification(R.drawable.ic_launcher,
        text,
        System.currentTimeMillis());
    //Activity to start up when notification is clicked
    PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, com.undi.musicplayer.UndiMusicPlayerActivity.class), 0);
    notification.setLatestEventInfo(this, getText(R.string.app_name), text, contentIntent);
    mNM.notify(R.string.musicplayer_service_started, notification);
  }

}
