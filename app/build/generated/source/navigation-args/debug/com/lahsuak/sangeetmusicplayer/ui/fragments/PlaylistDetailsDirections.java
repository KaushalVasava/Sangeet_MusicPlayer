package com.lahsuak.sangeetmusicplayer.ui.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.navigation.ActionOnlyNavDirections;
import androidx.navigation.NavDirections;
import com.lahsuak.sangeetmusicplayer.R;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;

public class PlaylistDetailsDirections {
  private PlaylistDetailsDirections() {
  }

  @NonNull
  public static ActionPlaylistDetailsToPlayerFragment actionPlaylistDetailsToPlayerFragment(
      int position, int songId, int playlistPos) {
    return new ActionPlaylistDetailsToPlayerFragment(position, songId, playlistPos);
  }

  @NonNull
  public static NavDirections actionPlaylistDetailsToSelectionPlaylist() {
    return new ActionOnlyNavDirections(R.id.action_playlistDetails_to_selectionPlaylist);
  }

  public static class ActionPlaylistDetailsToPlayerFragment implements NavDirections {
    private final HashMap arguments = new HashMap();

    @SuppressWarnings("unchecked")
    private ActionPlaylistDetailsToPlayerFragment(int position, int songId, int playlistPos) {
      this.arguments.put("position", position);
      this.arguments.put("songId", songId);
      this.arguments.put("playlistPos", playlistPos);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public ActionPlaylistDetailsToPlayerFragment setPosition(int position) {
      this.arguments.put("position", position);
      return this;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public ActionPlaylistDetailsToPlayerFragment setSongId(int songId) {
      this.arguments.put("songId", songId);
      return this;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public ActionPlaylistDetailsToPlayerFragment setPlaylistPos(int playlistPos) {
      this.arguments.put("playlistPos", playlistPos);
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    @NonNull
    public Bundle getArguments() {
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

    @Override
    public int getActionId() {
      return R.id.action_playlistDetails_to_playerFragment;
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

    @Override
    public boolean equals(Object object) {
      if (this == object) {
          return true;
      }
      if (object == null || getClass() != object.getClass()) {
          return false;
      }
      ActionPlaylistDetailsToPlayerFragment that = (ActionPlaylistDetailsToPlayerFragment) object;
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
      if (getActionId() != that.getActionId()) {
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
      result = 31 * result + getActionId();
      return result;
    }

    @Override
    public String toString() {
      return "ActionPlaylistDetailsToPlayerFragment(actionId=" + getActionId() + "){"
          + "position=" + getPosition()
          + ", songId=" + getSongId()
          + ", playlistPos=" + getPlaylistPos()
          + "}";
    }
  }
}