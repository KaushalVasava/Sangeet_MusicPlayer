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

public class PlaylistDetailsArgs implements NavArgs {
  private final HashMap arguments = new HashMap();

  private PlaylistDetailsArgs() {
  }

  @SuppressWarnings("unchecked")
  private PlaylistDetailsArgs(HashMap argumentsMap) {
    this.arguments.putAll(argumentsMap);
  }

  @NonNull
  @SuppressWarnings("unchecked")
  public static PlaylistDetailsArgs fromBundle(@NonNull Bundle bundle) {
    PlaylistDetailsArgs __result = new PlaylistDetailsArgs();
    bundle.setClassLoader(PlaylistDetailsArgs.class.getClassLoader());
    if (bundle.containsKey("playlistPosition")) {
      int playlistPosition;
      playlistPosition = bundle.getInt("playlistPosition");
      __result.arguments.put("playlistPosition", playlistPosition);
    } else {
      throw new IllegalArgumentException("Required argument \"playlistPosition\" is missing and does not have an android:defaultValue");
    }
    return __result;
  }

  @NonNull
  @SuppressWarnings("unchecked")
  public static PlaylistDetailsArgs fromSavedStateHandle(
      @NonNull SavedStateHandle savedStateHandle) {
    PlaylistDetailsArgs __result = new PlaylistDetailsArgs();
    if (savedStateHandle.contains("playlistPosition")) {
      int playlistPosition;
      playlistPosition = savedStateHandle.get("playlistPosition");
      __result.arguments.put("playlistPosition", playlistPosition);
    } else {
      throw new IllegalArgumentException("Required argument \"playlistPosition\" is missing and does not have an android:defaultValue");
    }
    return __result;
  }

  @SuppressWarnings("unchecked")
  public int getPlaylistPosition() {
    return (int) arguments.get("playlistPosition");
  }

  @SuppressWarnings("unchecked")
  @NonNull
  public Bundle toBundle() {
    Bundle __result = new Bundle();
    if (arguments.containsKey("playlistPosition")) {
      int playlistPosition = (int) arguments.get("playlistPosition");
      __result.putInt("playlistPosition", playlistPosition);
    }
    return __result;
  }

  @SuppressWarnings("unchecked")
  @NonNull
  public SavedStateHandle toSavedStateHandle() {
    SavedStateHandle __result = new SavedStateHandle();
    if (arguments.containsKey("playlistPosition")) {
      int playlistPosition = (int) arguments.get("playlistPosition");
      __result.set("playlistPosition", playlistPosition);
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
    PlaylistDetailsArgs that = (PlaylistDetailsArgs) object;
    if (arguments.containsKey("playlistPosition") != that.arguments.containsKey("playlistPosition")) {
      return false;
    }
    if (getPlaylistPosition() != that.getPlaylistPosition()) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = 1;
    result = 31 * result + getPlaylistPosition();
    return result;
  }

  @Override
  public String toString() {
    return "PlaylistDetailsArgs{"
        + "playlistPosition=" + getPlaylistPosition()
        + "}";
  }

  public static final class Builder {
    private final HashMap arguments = new HashMap();

    @SuppressWarnings("unchecked")
    public Builder(@NonNull PlaylistDetailsArgs original) {
      this.arguments.putAll(original.arguments);
    }

    @SuppressWarnings("unchecked")
    public Builder(int playlistPosition) {
      this.arguments.put("playlistPosition", playlistPosition);
    }

    @NonNull
    public PlaylistDetailsArgs build() {
      PlaylistDetailsArgs result = new PlaylistDetailsArgs(arguments);
      return result;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public Builder setPlaylistPosition(int playlistPosition) {
      this.arguments.put("playlistPosition", playlistPosition);
      return this;
    }

    @SuppressWarnings({"unchecked","GetterOnBuilder"})
    public int getPlaylistPosition() {
      return (int) arguments.get("playlistPosition");
    }
  }
}
