/*
 * Copyright (C) 2016 The CyanogenMod Project
 *               2017 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.bootleg;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class ChargingSoundsSettings extends SettingsPreferenceFragment /* implements
        Preference.OnPreferenceChangeListener*/ {

    private static final String TAG = "ChargingSoundsSettings";
    private static final String KEY_VIBRATION_ON_CHARGE_STATE_CHANGED = "vibration_on_charge_state_changed";
    private static final String KEY_CHARGING_SOUNDS_RINGTONE = "charging_sounds_ringtone";

    // Used for power notification uri string if set to silent
    private static final String RINGTONE_SILENT_URI_STRING = "silent";

    // Request code for charging notification ringtone picker
    private static final int REQUEST_CODE_CHARGING_NOTIFICATIONS_RINGTONE = 1;

    private Preference mChargingSoundsRingtone;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.charging_sounds_settings);

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            removePreference(KEY_VIBRATION_ON_CHARGE_STATE_CHANGED);
        }

        mChargingSoundsRingtone = findPreference(KEY_CHARGING_SOUNDS_RINGTONE);
        String curTone = Settings.Global.getString(getContentResolver(),
                Settings.Global.CHARGING_SOUNDS_RINGTONE);
        if (curTone == null) {
            updateChargingRingtone(Settings.System.DEFAULT_NOTIFICATION_URI.toString(), true);
        } else {
            updateChargingRingtone(curTone, false);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.BOOTLEG;
    }

    private void updateChargingRingtone(String toneUriString, boolean persist) {
        final String toneName;

        if (toneUriString != null && !toneUriString.equals(RINGTONE_SILENT_URI_STRING)) {
            final Ringtone ringtone = RingtoneManager.getRingtone(getActivity(),
                    Uri.parse(toneUriString));
            if (ringtone != null) {
                toneName = ringtone.getTitle(getActivity());
            } else {
                // Unlikely to ever happen, but is possible if the ringtone
                // previously chosen is removed during an upgrade
                toneName = "";
                toneUriString = Settings.System.DEFAULT_NOTIFICATION_URI.toString();
                persist = true;
            }
        } else {
            // Silent
            toneName = getString(R.string.charging_sounds_ringtone_silent);
            toneUriString = RINGTONE_SILENT_URI_STRING;
        }

        mChargingSoundsRingtone.setSummary(toneName);
        if (persist) {
            Settings.Global.putString(getContentResolver(),
                    Settings.Global.CHARGING_SOUNDS_RINGTONE, toneUriString);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mChargingSoundsRingtone) {
            launchNotificationSoundPicker(REQUEST_CODE_CHARGING_NOTIFICATIONS_RINGTONE,
                    Settings.Global.getString(getContentResolver(),
                    Settings.Global.CHARGING_SOUNDS_RINGTONE));
        }
        return super.onPreferenceTreeClick(preference);
    }

    private void launchNotificationSoundPicker(int requestCode, String toneUriString) {
        final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);

        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
                getString(R.string.charging_sounds_ringtone_title));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
                RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                Settings.System.DEFAULT_NOTIFICATION_URI);
        if (toneUriString != null && !toneUriString.equals(RINGTONE_SILENT_URI_STRING)) {
            Uri uri = Uri.parse(toneUriString);
            if (uri != null) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri);
            }
        }
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CHARGING_NOTIFICATIONS_RINGTONE
                && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            updateChargingRingtone(uri != null ? uri.toString() : null, true);
        }
    }
}
