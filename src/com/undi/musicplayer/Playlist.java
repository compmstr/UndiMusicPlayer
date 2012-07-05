package com.undi.musicplayer;

import java.util.Vector;

public class Playlist {
  private Vector<String> files;
  private int flags;
  
  private int currentFile;
  
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
    public static int toggleFlag(int value, int flag){
      return (value ^ flag);
    }
  };
  
  public Playlist(){
    this(new Vector<String>());
  }
  public Playlist(Vector<String> files){
    this(files, 0);
  }
  public Playlist(Vector<String> files, int flags){
    this.files = files;
    this.flags = flags;
  }
  public void setFlags(int flags){
    this.flags = flags;
  }
  public int getFlags(int flags){
    return this.flags;
  }
  
  public void addFile(String newFile){
    this.files.add(newFile);
  }
  
  /**
   * Returns the file path for the next file in the playlist
   * @return the filename, or "" if there are no more files to play
   */
  public String nextFile(){
    boolean endOfPlaylist = ((this.currentFile + 1) >= this.files.size());
    if(this.isRepeatOne()){
      return this.files.get(this.currentFile);
    }
    if(endOfPlaylist){
      if(this.isRepeatAll()){
        this.currentFile = 0;
      }else{
        return "";
      }
    }else{
      this.currentFile++;
    }
    return this.files.get(this.currentFile);
  }
  
  public boolean isShuffle(){
    return Flag.checkFlag(this.flags, Flag.SHUFFLE);
  }
  public boolean isRepeatAll(){
    return Flag.checkFlag(this.flags, Flag.REPEAT_ALL);
  }
  public boolean isRepeatOne(){
    return Flag.checkFlag(this.flags, Flag.REPEAT_ONE);
  }
}
