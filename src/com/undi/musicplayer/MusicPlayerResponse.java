package com.undi.musicplayer;

import com.undi.musicplayer.MusicPlayerService.PlayerStatus;

import android.os.Parcel;
import android.os.Parcelable;

public class MusicPlayerResponse
implements Parcelable{
  public final MusicPlayerService.MusicPlayerStatus status;
  public final String data;
  public final MusicPlayerService.MessageCode message;
  public MusicPlayerResponse(MusicPlayerService.MusicPlayerStatus status, String data, MusicPlayerService.MessageCode message){
    this.status = status;
    this.data = data;
    this.message = message;
  }
  
  public String toString(){
    return "[MusicPlayerResponse - Message: " + this.message.name() + " - " + this.status.toString() + " - Data: " + this.data + "]";
  }
  
  @Override
  public int describeContents() {
    // TODO Auto-generated method stub
    return 0;
  }
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    //Parcel contains:
    //status, file, flags
    dest.writeInt(this.status.status.ordinal());
    dest.writeString(this.status.file);
    dest.writeInt(this.status.flags);
    //data
    dest.writeString(this.data);
    //message ordinal
    dest.writeInt(this.message.ordinal());
  }
  
  MusicPlayerResponse(Parcel in){
    this.status = new MusicPlayerService.MusicPlayerStatus(PlayerStatus.values()[in.readInt()], in.readString(), in.readInt());
    this.data = in.readString();
    this.message = MusicPlayerService.MessageCode.values()[in.readInt()];
  }
  
  public static final Parcelable.Creator<MusicPlayerResponse> CREATOR =
     new Parcelable.Creator<MusicPlayerResponse>(){

      @Override
      public MusicPlayerResponse createFromParcel(Parcel source) {
        return new MusicPlayerResponse(source);
      }

      @Override
      public MusicPlayerResponse[] newArray(int size) {
        // TODO Auto-generated method stub
        return null;
      }
  
  };
}