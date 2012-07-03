package com.undi.musicplayer;

import android.os.Parcel;
import android.os.Parcelable;

public class MusicPlayerCommand
implements Parcelable{
  public final MusicPlayerService.MessageCode message;
  public final String data;
  MusicPlayerCommand(MusicPlayerService.MessageCode message, String data){
    this.message = message;
    this.data = data;
  }
  public MusicPlayerCommand(Parcel source) {
    this.message = MusicPlayerService.MessageCode.values()[source.readInt()];
    this.data = source.readString();
  }
  @Override
  public int describeContents() {
    // TODO Auto-generated method stub
    return 0;
  }
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    //Parcel contains:
    //Message ordinal
    dest.writeInt(this.message.ordinal());
    //Data
    dest.writeString(this.data);
    
  }
  public final static Parcelable.Creator<MusicPlayerCommand> CREATOR = new Parcelable.Creator<MusicPlayerCommand>(){

    @Override
    public MusicPlayerCommand createFromParcel(Parcel source) {
      // TODO Auto-generated method stub
      return new MusicPlayerCommand(source);
    }

    @Override
    public MusicPlayerCommand[] newArray(int size) {
      // TODO Auto-generated method stub
      return null;
    }
    
  };
}