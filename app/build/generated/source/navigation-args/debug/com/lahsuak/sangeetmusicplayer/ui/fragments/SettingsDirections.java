package com.lahsuak.sangeetmusicplayer.ui.fragments;

import androidx.annotation.NonNull;
import androidx.navigation.ActionOnlyNavDirections;
import androidx.navigation.NavDirections;
import com.lahsuak.sangeetmusicplayer.R;

public class SettingsDirections {
  private SettingsDirections() {
  }

  @NonNull
  public static NavDirections actionAllSettingsToFeedbackFragment() {
    return new ActionOnlyNavDirections(R.id.action_allSettings_to_feedbackFragment);
  }
}
