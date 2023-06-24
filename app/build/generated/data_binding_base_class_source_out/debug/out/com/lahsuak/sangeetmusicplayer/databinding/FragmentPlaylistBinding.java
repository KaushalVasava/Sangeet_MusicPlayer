// Generated by view binder compiler. Do not edit!
package com.lahsuak.sangeetmusicplayer.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lahsuak.sangeetmusicplayer.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class FragmentPlaylistBinding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final FloatingActionButton btnPlaylist;

  @NonNull
  public final RecyclerView playlistRecyclerView;

  private FragmentPlaylistBinding(@NonNull ConstraintLayout rootView,
      @NonNull FloatingActionButton btnPlaylist, @NonNull RecyclerView playlistRecyclerView) {
    this.rootView = rootView;
    this.btnPlaylist = btnPlaylist;
    this.playlistRecyclerView = playlistRecyclerView;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static FragmentPlaylistBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FragmentPlaylistBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.fragment_playlist, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FragmentPlaylistBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.btnPlaylist;
      FloatingActionButton btnPlaylist = ViewBindings.findChildViewById(rootView, id);
      if (btnPlaylist == null) {
        break missingId;
      }

      id = R.id.playlistRecyclerView;
      RecyclerView playlistRecyclerView = ViewBindings.findChildViewById(rootView, id);
      if (playlistRecyclerView == null) {
        break missingId;
      }

      return new FragmentPlaylistBinding((ConstraintLayout) rootView, btnPlaylist,
          playlistRecyclerView);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}