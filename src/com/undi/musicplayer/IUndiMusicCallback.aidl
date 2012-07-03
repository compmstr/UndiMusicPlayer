package com.undi.musicplayer;
import com.undi.musicplayer.MusicPlayerResponse;
import com.undi.musicplayer.MusicPlayerCommand;

interface IUndiMusicCallback{
  MusicPlayerResponse sendCommand(in MusicPlayerCommand command);
}