package com.lahsuak.sangeetmusicplayer.ui.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.navigation.NavDirections;
import com.lahsuak.sangeetmusicplayer.R;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;

public class PlayListFragmentDirections {
  private PlayListFragmentDirections() {
  }

  @NonNull
  public static ActionPlayListFragmentToPlaylistDetails actionPlayListFragmentToPlaylistDetails(
      int playlistPosition) {
    return new ActionPlayListFragmentToPlaylistDetails(playlistPosition);
  }

  public static class ActionPlayListFragmentToPlaylistDetails implements NavDirections {
    private final HashMap arguments = new HashMap();

    @SuppressWarnings("unchecked")
    private ActionPlayListFragmentToPlaylistDetails(int playlistPosition) {
      this.arguments.put("playlistPosition", playlistPosition);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public ActionPlayListFragmentToPlaylistDetails setPlaylistPosition(int playlistPosition) {
      this.arguments.put("playlistPosition", playlistPosition);
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    @NonNull
    public Bundle getArguments() {
      Bundle __result = new Bundle();
      if (arguments.containsKey("playlistPosition")) {
        int playlistPosition = (int) arguments.get("playlistPosition");
        __result.putInt("playlistPosition", playlistPosition);
      }
      return __result;
    }

    @Override
    public int getActionId() {
      return R.id.action_playListFragment_to_playlistDetails;
    }

    @SuppressWarnings("unchecked")
    public int getPlaylistPosition() {
      return (int) arguments.get("playlistPosition");
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
          return true;
      }
      if (object == null || getClass() != object.getClass()) {
          return false;
      }
      ActionPlayListFragmentToPlaylistDetails that = (ActionPlayListFragmentToPlaylistDetails) object;
      if (arguments.containsKey("playlistPosition") != that.arguments.containsKey("playlistPosition")) {
        return false;
      }
      if (getPlaylistPosition() != that.getPlaylistPosition()) {
        return false;
      }
      if (getActionId() != that.getActionId()) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      int result = 1;
      result = 31 * result + getPlaylistPosition();
      result = 31 * result + getActionId();
      return result;
    }

    @Override
    public String toString() {
      return "ActionPlayListFragmentToPlaylistDetails(actionId=" + getActionId() + "){"
          + "playlistPosition=" + getPlaylistPosition()
          + "}";
    }
  }
}
