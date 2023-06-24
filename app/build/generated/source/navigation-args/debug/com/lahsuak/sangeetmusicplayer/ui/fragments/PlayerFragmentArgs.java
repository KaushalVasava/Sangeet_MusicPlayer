package com.lahsuak.sangeetmusicplayer.ui.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.SavedStateHandle;
import androidx.navigation.NavArgs;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;

public class PlayerFragmentArgs implements NavArgs {
  private final HashMap arguments = new HashMap();

  private PlayerFragmentArgs() {
  }

  @SuppressWarnings("unchecked")
  private PlayerFragmentArgs(HashMap argumentsMap) {
    this.arguments.putAll(argumentsMap);
  }

  @NonNull
  @SuppressWarnings("unchecked")
  public static PlayerFragmentArgs fromBundle(@NonNull Bundle bundle) {
    PlayerFragmentArgs __result = new PlayerFragmentArgs();
    bundle.setClassLoader(PlayerFragmentArgs.class.getClassLoader());
    if (bundle.containsKey("position")) {
      int position;
      position = bundle.getInt("position");
      __result.arguments.put("position", position);
    } else {
      throw new IllegalArgumentException("Required argument \"position\" is missing and does not have an android:defaultValue");
    }
    if (bundle.containsKey("songId")) {
      int songId;
      songId = bundle.getInt("songId");
      __result.arguments.put("songId", songId);
    } else {
      throw new IllegalArgumentException("Required argument \"songId\" is missing and does not have an android:defaultValue");
    }
    if (bundle.containsKey("playlistPos")) {
      int playlistPos;
      playlistPos = bundle.getInt("playlistPos");
      __result.arguments.put("playlistPos", playlistPos);
    } else {
      throw new IllegalArgumentException("Required argument \"playlistPos\" is missing and does not have an android:defaultValue");
    }
    return __result;
  }

  @NonNull
  @SuppressWarnings("unchecked")
  public static PlayerFragmentArgs fromSavedStateHandle(
      @NonNull SavedStateHandle savedStateHandle) {
    PlayerFragmentArgs __result = new PlayerFragmentArgs();
    if (savedStateHandle.contains("position")) {
      int position;
      position = savedStateHandle.get("position");
      __result.arguments.put("position", position);
    } else {
      throw new IllegalArgumentException("Required argument \"position\" is missing and does not have an android:defaultValue");
    }
    if (savedStateHandle.contains("songId")) {
      int songId;
      songId = savedStateHandle.get("songId");
      __result.arguments.put("songId", songId);
    } else {
      throw new IllegalArgumentException("Required argument \"songId\" is missing and does not have an android:defaultValue");
    }
    if (savedStateHandle.contains("playlistPos")) {
      int playlistPos;
      playlistPos = savedStateHandle.get("playlistPos");
      __result.arguments.put("playlistPos", playlistPos);
    } else {
      throw new IllegalArgumentException("Required argument \"playlistPos\" is missing and does not have an android:defaultValue");
    }
    return __result;
  }

  @SuppressWarnings("unchecked")
  public int getPosition() {
    return (int) arguments.get("position");
  }

  @SuppressWarnings("unchecked")
  public int getSongId() {
    return (int) arguments.get("songId");
  }

  @SuppressWarnings("unchecked")
  public int getPlaylistPos() {
    return (int) arguments.get("playlistPos");
  }

  @SuppressWarnings("unchecked")
  @NonNull
  public Bundle toBundle() {
    Bundle __result = new Bundle();
    if (arguments.containsKey("position")) {
      int position = (int) arguments.get("position");
      __result.putInt("position", position);
    }
    if (arguments.containsKey("songId")) {
      int songId = (int) arguments.get("songId");
      __result.putInt("songId", songId);
    }
    if (arguments.containsKey("playlistPos")) {
      int playlistPos = (int) arguments.get("playlistPos");
      __result.putInt("playlistPos", playlistPos);
    }
    return __result;
  }

  @SuppressWarnings("unchecked")
  @NonNull
  public SavedStateHandle toSavedStateHandle() {
    SavedStateHandle __result = new SavedStateHandle();
    if (arguments.containsKey("position")) {
      int position = (int) arguments.get("position");
      __result.set("position", position);
    }
    if (arguments.containsKey("songId")) {
      int songId = (int) arguments.get("songId");
      __result.set("songId", songId);
    }
    if (arguments.containsKey("playlistPos")) {
      int playlistPos = (int) arguments.get("playlistPos");
      __result.set("playlistPos", playlistPos);
    }
    return __result;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
        return true;
    }
    if (object == null || getClass() != object.getClass()) {
        return false;
    }
    PlayerFragmentArgs that = (PlayerFragmentArgs) object;
    if (arguments.containsKey("position") != that.arguments.containsKey("position")) {
      return false;
    }
    if (getPosition() != that.getPosition()) {
      return false;
    }
    if (arguments.containsKey("songId") != that.arguments.containsKey("songId")) {
      return false;
    }
    if (getSongId() != that.getSongId()) {
      return false;
    }
    if (arguments.containsKey("playlistPos") != that.arguments.containsKey("playlistPos")) {
      return false;
    }
    if (getPlaylistPos() != that.getPlaylistPos()) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = 1;
    result = 31 * result + getPosition();
    result = 31 * result + getSongId();
    result = 31 * result + getPlaylistPos();
    return result;
  }

  @Override
  public String toString() {
    return "PlayerFragmentArgs{"
        + "position=" + getPosition()
        + ", songId=" + getSongId()
        + ", playlistPos=" + getPlaylistPos()
        + "}";
  }

  public static final class Builder {
    private final HashMap arguments = new HashMap();

    @SuppressWarnings("unchecked")
    public Builder(@NonNull PlayerFragmentArgs original) {
      this.arguments.putAll(original.arguments);
    }

    @SuppressWarnings("unchecked")
    public Builder(int position, int songId, int playlistPos) {
      this.arguments.put("position", position);
      this.arguments.put("songId", songId);
      this.arguments.put("playlistPos", playlistPos);
    }

    @NonNull
    public PlayerFragmentArgs build() {
      PlayerFragmentArgs result = new PlayerFragmentArgs(arguments);
      return result;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public Builder setPosition(int position) {
      this.arguments.put("position", position);
      return this;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public Builder setSongId(int songId) {
      this.arguments.put("songId", songId);
      return this;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public Builder setPlaylistPos(int playlistPos) {
      this.arguments.put("playlistPos", playlistPos);
      return this;
    }

    @SuppressWarnings({"unchecked","GetterOnBuilder"})
    public int getPosition() {
      return (int) arguments.get("position");
    }

    @SuppressWarnings({"unchecked","GetterOnBuilder"})
    public int getSongId() {
      return (int) arguments.get("songId");
    }

    @SuppressWarnings({"unchecked","GetterOnBuilder"})
    public int getPlaylistPos() {
      return (int) arguments.get("playlistPos");
    }
  }
}
