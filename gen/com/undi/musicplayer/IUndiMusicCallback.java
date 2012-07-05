/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/corey/stuff/programming/android/UndiMusicPlayer/src/com/undi/musicplayer/IUndiMusicCallback.aidl
 */
package com.undi.musicplayer;
public interface IUndiMusicCallback extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.undi.musicplayer.IUndiMusicCallback
{
private static final java.lang.String DESCRIPTOR = "com.undi.musicplayer.IUndiMusicCallback";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.undi.musicplayer.IUndiMusicCallback interface,
 * generating a proxy if needed.
 */
public static com.undi.musicplayer.IUndiMusicCallback asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.undi.musicplayer.IUndiMusicCallback))) {
return ((com.undi.musicplayer.IUndiMusicCallback)iin);
}
return new com.undi.musicplayer.IUndiMusicCallback.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_sendCommand:
{
data.enforceInterface(DESCRIPTOR);
com.undi.musicplayer.MusicPlayerCommand _arg0;
if ((0!=data.readInt())) {
_arg0 = com.undi.musicplayer.MusicPlayerCommand.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
com.undi.musicplayer.MusicPlayerResponse _result = this.sendCommand(_arg0);
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.undi.musicplayer.IUndiMusicCallback
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
public com.undi.musicplayer.MusicPlayerResponse sendCommand(com.undi.musicplayer.MusicPlayerCommand command) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
com.undi.musicplayer.MusicPlayerResponse _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((command!=null)) {
_data.writeInt(1);
command.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_sendCommand, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = com.undi.musicplayer.MusicPlayerResponse.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_sendCommand = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
public com.undi.musicplayer.MusicPlayerResponse sendCommand(com.undi.musicplayer.MusicPlayerCommand command) throws android.os.RemoteException;
}
