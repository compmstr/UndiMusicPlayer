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
    PLAY_FILE, PLAY_PLAYLIST, PLAY, PAUSE, STOP, RESTART, PREV, NEXT, SHUFFLE, REPEAT_ONE, REPEAT_ALL,
    ERROR;
  };
  public static enum PlayerStatus{
    PLAYING, PAUSED, STOPPED
  }
  public final static String MUSICDIR = "media/audio";
  
  private File[] musicFiles;
  private Playlist mPlaylist = null;
  
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
      //Log.d("MusicPlayerService", "Got a Command: " + command.message.toString() + " - " + command.data);
      switch(command.message){
      case GET_STATUS:
        updateStatus();
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
        return onPlayFile(command);
      case PAUSE:
        if(status.status == PlayerStatus.PLAYING){
          pausePlayback();
          return new MusicPlayerResponse(status, "Paused", command.message);
        }else if(status.status == PlayerStatus.PAUSED){
          startPlayback();
          return new MusicPlayerResponse(status, "Playing", command.message);
        }else{
          return new MusicPlayerResponse(status, "Stopped", command.message);
        }
      case STOP:
        stopPlayback();
        return new MusicPlayerResponse(status, "Stopped", command.message);
      case PLAY:
        startPlayback();
        return new MusicPlayerResponse(status, "Playing", command.message);
      }
      return null;
    }
    
  };
  
  private MusicPlayerResponse onPlayFile(MusicPlayerCommand command){
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
    mPlaylist = new Playlist();
    mPlaylist.addFile(fileToPlay);
    //playFile(playlist[currentFileInPlaylist]);
    playFile(mPlaylist.nextFile());
    return new MusicPlayerResponse(status, "Starting to play file: " + fileToPlay, command.message);
  }

  private void updateStatus(){
    if(mPlayer == null){
      return;
    }
    if(mPlayer.isPlaying()){
      this.status.pos = mPlayer.getCurrentPosition();
      this.status.duration = mPlayer.getDuration();
    }
  }
  
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
    if(mPlayer == null){
      return;
    }
    String nextFileToPlay = mPlaylist.nextFile();
    if(nextFileToPlay == ""){
      this.status.status = PlayerStatus.STOPPED;
      this.status.file = "";
      mPlayer.reset();
    }else{
      playFile(nextFileToPlay);
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
    if(mPlayer == null){
      return;
    }
    mPlayer.start();
    if(mPlayer.isPlaying()){
      this.status.status = PlayerStatus.PLAYING;
    }else{
      this.status.status = PlayerStatus.STOPPED;
    }
  }
  
  private void stopPlayback(){
    if(mPlayer == null){
      return;
    }
    mPlayer.stop();
    this.status.status = PlayerStatus.STOPPED;
  }
  
  private void pausePlayback(){
    if(mPlayer == null){
      return;
    }
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
    
    /**
     * Position we're at in the stream
     */
    public int pos;
    /**
     * Total size of the stream
     */
    public int duration;
    
    public String toString(){
      return "[MusicPlayerStatus - " + status + " : " + (file == "" ? "[No File]" : file) +
      " --Position: " + this.pos + ":" + this.duration +
      " --Flags: " + flags + "]";
    }
    
    public MusicPlayerStatus(PlayerStatus status, String file, int flags, int pos, int duration){
      this.status = status;
      this.file = file;
      this.flags = flags;
      this.pos = pos;
      this.duration = duration;
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
      musicDir = new File(sdCard.getAbsolutePath() + "/" + MUSICDIR + "/");
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
    this.status = new MusicPlayerStatus(PlayerStatus.STOPPED, "", 0, 0, 0);
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
