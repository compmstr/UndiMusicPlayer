package com.undi.musicplayer;

//import android.R.integer;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
//import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class UndiMusicPlayerActivity extends Activity {
    /** Called when the activity is first created. */
    private Intent musicServiceIntent;
    private ServiceConnection musicConn;
    private boolean serviceBound;
    private IUndiMusicCallback musicCommandHandle;
    
/*    private class IncomingMessageHandler
    extends Handler{
      @Override
      public void handleMessage(Message msg){
        switch(msg.what){
        //We got a response from the music player about the status
        case MusicPlayerService.Message.GET_STATUS:
          onStatusUpdate((MusicPlayerService.MusicPlayerStatus)msg.getData().getSerializable("status"));
          break;
        default:
          super.handleMessage(msg);
        }
      }
    }*/
    
/*    private ServiceConnection musicPlayerServiceConn = new ServiceConnection(){
      @Override
      public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d("musicPlayerServiceConn", "Service Connected");
      }

      @Override
      public void onServiceDisconnected(ComponentName name) {
        Log.d("musicPlayerServiceConn", "Service Disconnected");
      }    
    };*/
    
    public void onGetStatusClicked(View view){
      Log.d("UndiMusicPlayerActivity", "Get Status Clicked!");
      if(this.serviceBound){
        new Thread(new Runnable(){
          public void run(){
            try {
              onStatusUpdate(musicCommandHandle.sendCommand(
                  new MusicPlayerCommand(
                      MusicPlayerService.MessageCode.GET_STATUS,
                  "get status")));
            } catch (RemoteException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          }
        }).start();
      }else{
        Log.d("UndiMusicPlayerActivity", "Not connected to service!");
      }
    }
    
    /**
     * Starts a new thread to run onFileListUpdate with the response from sendCommand
     * @param view
     */
    public void onGetFileListClicked(View view){
      Log.d("UndiMusicPlayerActivity", "Get File List Clicked!");
      if(this.serviceBound){
        new Thread(new Runnable(){
          public void run(){
            try {
              onFileListUpdate(musicCommandHandle.sendCommand(
                  new MusicPlayerCommand(
                      MusicPlayerService.MessageCode.GET_FILE_LIST,
                      "get files")));
            } catch (RemoteException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          }
        }).start();

      }else{
        Log.d("UndiMusicPlayerActivity", "Not connected to service!");
      }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Log.d("UndiMusicPlayerActivity", "Starting Music Player Service");
        musicServiceIntent = new Intent(getApplicationContext(), MusicPlayerService.class);
        musicServiceIntent.putExtra("extraData", "some data");
        this.startService(musicServiceIntent);
        //Bind the service
        //this.serviceBound = this.bindService(musicServiceIntent, musicPlayerServiceConn, Context.BIND_AUTO_CREATE);
        this.musicConn = new ServiceConnection(){
          @Override
          public void onServiceConnected(ComponentName name, IBinder service) {
            musicCommandHandle = IUndiMusicCallback.Stub.asInterface(service);
            serviceBound = true;
            Log.d("UndiMusicPlayerActivity", "Connected to service");
          }
          @Override
          public void onServiceDisconnected(ComponentName name) {
            musicCommandHandle = null;
            serviceBound = false;
            Log.d("UndiMusicPlayerActivity", "Disconnected from service");
          }          
        };
        this.serviceBound = this.bindService(musicServiceIntent, musicConn, Context.BIND_AUTO_CREATE);
        if(!this.serviceBound){
          Log.d("UndiMusicPlayerActivity", "Error binding to music service");
        }else{
          Log.d("UndiMusicPlayerActivity", "Bound to Music Player Service");
        }
        //Once we have a music command handle, get the file list 
        //(started in a thread so it doesn't stop the UI while waiting)
        new Thread(new Runnable(){
          public void run(){
            while(musicCommandHandle == null){
              try {
                Thread.sleep(1);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }
            onGetFileListClicked(null);            
          }
        }).start();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
      MenuInflater inf = getMenuInflater();
      inf.inflate(R.menu.playermenu, menu);
      return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
      switch(item.getItemId()){
      case R.id.menuExit:
        Log.d("UndiMusicPlayerActivity", "Exiting player");
        this.finish();
        break;
      case R.id.menuKillService:
        Log.d("UndiMusicPlayerActivity", "Stopping Music Service");
        this.serviceBound = false;
        this.unbindService(this.musicConn);
        this.stopService(musicServiceIntent);    
        break;
      }
      return true;
    }
    
    @Override
    public void onDestroy(){
      super.onDestroy();
      Log.d("UndiMusicPlayerActivity", "Exiting, disconnecting from music service");
      //this.stopService(musicServiceIntent);
      if(this.musicConn != null && this.serviceBound){
        this.unbindService(this.musicConn);
      }
    }
    
    private synchronized void onStatusUpdate(MusicPlayerResponse response){
      Log.d("UndiMusicPlayerActivity", "Got status back: " + response);
      StringBuilder statusStringB = new StringBuilder();
      switch(response.status.status){
      case PLAYING:
        statusStringB.append("PLAYING: ");
        statusStringB.append(response.status.file);
        break;
      case PAUSED:
        statusStringB.append("PAUSED: ");
        statusStringB.append(response.status.file);
        break;
      case STOPPED:
        statusStringB.append("STOPPED");
        break;
      }
      final String statusString = statusStringB.toString();
      final Context curContext = this;
      this.runOnUiThread(new Runnable(){
        public void run(){
          Toast.makeText(curContext, statusString, Toast.LENGTH_SHORT).show();
        }
      });
    }
    
    private synchronized void onPlayFileUpdate(MusicPlayerResponse response){
      Log.d("UndiMusicPlayerActivity", "Got response from play request: " + response.data);
    }
    
    /**
     * Posts a new message to the UI thread to display the file list
     * Passes in the main context and the main view, as well as the response from the music player
     * all set as final so they can be run threaded
     * @param response
     */
    private void onFileListUpdate(final MusicPlayerResponse response){
      final View mainView = findViewById(R.id.viewMainView);
      final Context curContext = this;
      //You can also use mainView.post(...
      this.runOnUiThread(new Runnable(){
        public void run(){
          DisplayFileList(mainView, curContext, response);
        }
      });

    }
    /**
     * Displays the list of files returned from the music player service
     *    in a new ListView inside the fileListContainer view
     * @param mainView The main view to grab the listview from
     * @param curContext The application context to use for resources
     * @param response The response from the music player service
     */
    private synchronized void DisplayFileList(final View mainView, final Context curContext, final MusicPlayerResponse response){
      Log.d("UndiMusicPlayerActivity", "Got file list: " + response.data); 
      String filesString = response.data;
      String[] rawFiles = filesString.split(";");
      String[] files = new String[rawFiles.length];
      String curFile;
      int filenameIndex;
      for(int i = 0; i < rawFiles.length; i++){
        curFile = rawFiles[i];
        filenameIndex = curFile.lastIndexOf('/');
        if(filenameIndex == -1){
          Log.d("UndiMusicPlayerActivity", "Invalid file, skipping: " + curFile);
          continue;
        }
        files[i] = curFile.substring(filenameIndex + 1);
      }
      LinearLayout fileListContainer = (LinearLayout) findViewById(R.id.fileListContainer);
      //Clear the list
      fileListContainer.removeAllViews();
      ListView fileList = new ListView(curContext);
      ArrayAdapter<String> filesArray = new ArrayAdapter<String>(curContext, android.R.layout.simple_list_item_1, files);
      fileList.setAdapter(filesArray);
      fileList.setOnItemClickListener(new OnItemClickListener(){

        @Override
        public void onItemClick(AdapterView<?> list, View v, int position,
            long id) {
          TextView selected = (TextView) list.getChildAt(position);
          String fileSelected = selected.getText().toString();
          Log.d("UndiMusicPlayerActivity", "File Selected: " + fileSelected);
          if(serviceBound){
            try {
              onPlayFileUpdate(musicCommandHandle.sendCommand(
                  new MusicPlayerCommand(
                      MusicPlayerService.MessageCode.PLAY_FILE,
                      fileSelected)));
            } catch (RemoteException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          }else{
            Log.d("UndiMusicPlayerActivity", "Not connected to service!");
          }
        }  
      });
      fileListContainer.addView(fileList);
    }
}