/*
	Copyright (C) 2011 Lorenzo Bernardi (fastlorenzo@gmail.com)
	2010 Ben Van Daele (vandaeleben@gmail.com)

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package be.bernardi.mvforandroid;

import be.bernardi.mvforandroid.data.DatabaseHelper;
import be.bernardi.mvforandroid.data.MVDataService;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener, OnPreferenceClickListener {

	public static final String	OPEN_APP				= "0";
	public static final String	UPDATE_DATA				= "1";

	public static final int		WIDGET_BG_BLACK			= 0;
	public static final int		WIDGET_BG_TRANSPARENT	= 1;

	private Preference			autoCreditPreference;
	private Preference			autoUsagePreference;
	private Preference			autoTopupsPreference;
	private Preference			updateFrequencyPreference;
	private Preference			widgetActionPreference;
	private Preference			widgetBackgroundPreference;
	private ListPreference		selectMsisdnPreference;
	private Preference			setDebugPreference;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);

		findPreferences();
		setMsisdns(selectMsisdnPreference);

		updatePreferences();
	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if(key.equals("auto_credit") || key.equals("auto_usage") || key.equals("auto_topups")) {
			switch(getNumEnabledAutoUpdates(prefs)) {
				case 0: {
					// An auto update preference has changed, and as a result
					// none of them are enabled anymore. This
					// means
					// we don't need to auto update anything and we can stop the
					// service.
					stopService();
					break;
				}
				case 1: {
					// At this point, exactly one of the auto updates is
					// enabled, so the possibility exists that it was
					// enabled just now, and as such that the service is stopped
					// and needs to be rescheduled. In other
					// words, if the currently modified preference is enabled,
					// it is also the *only* auto update
					// preference
					// that is enabled. Therefore the service has to be started.
					if(prefs.getBoolean(key, false))
						startService();
					break;
				}
			}
		}

		if(key.equals("auto_credit")) {
			updateCreditPreference(prefs);
		}
		else if(key.equals("auto_usage")) {
			updateUsagePreference(prefs);
		}
		else if(key.equals("auto_topups")) {
			updateTopupsPreference(prefs);
		}
		else if(key.equals("update_frequency")) {
			updateFrequencyPreference();
			if(getNumEnabledAutoUpdates(prefs) != 0) {
				stopService();
				startService();
			}
		}
		else if(key.equals("widget_action")) {
			updateWidgetActionPreference();
		}
		else if(key.equals("widget_background")) {
			updateWidgetBackgroundPreference();
		}
		else if(key.equals("select_msisdn")) {
			updateSelectMsisdnPreference();
		}
		else if(key.equals("set_debug")) {
			updateDebugPreference(prefs);
		}
	}

	private void findPreferences() {
		autoCreditPreference = getPreferenceScreen().findPreference("auto_credit");
		autoUsagePreference = getPreferenceScreen().findPreference("auto_usage");
		autoTopupsPreference = getPreferenceScreen().findPreference("auto_topups");
		updateFrequencyPreference = getPreferenceScreen().findPreference("update_frequency");
		widgetActionPreference = getPreferenceScreen().findPreference("widget_action");
		widgetBackgroundPreference = getPreferenceScreen().findPreference("widget_background");
		selectMsisdnPreference = (ListPreference) getPreferenceScreen().findPreference("select_msisdn");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String[] msisdns = new String[1];
		msisdns[0] = prefs.getString("select_msisdn", "none");
		selectMsisdnPreference.setEntries(msisdns);
		selectMsisdnPreference.setEntryValues(msisdns);
		setDebugPreference = getPreferenceScreen().findPreference("set_debug");

	}

	private void updatePreferences() {
		updateCreditPreference(getPreferenceScreen().getSharedPreferences());
		updateUsagePreference(getPreferenceScreen().getSharedPreferences());
		updateTopupsPreference(getPreferenceScreen().getSharedPreferences());
		updateFrequencyPreference();
		updateWidgetActionPreference();
		updateWidgetBackgroundPreference();
		updateSelectMsisdnPreference();
		updateDebugPreference(getPreferenceScreen().getSharedPreferences());
	}

	private int getNumEnabledAutoUpdates(SharedPreferences prefs) {
		int result = 0;
		if(prefs.getBoolean("auto_credit", false))
			result++;
		if(prefs.getBoolean("auto_usage", false))
			result++;
		if(prefs.getBoolean("auto_topups", false))
			result++;
		return result;
	}

	private void updateCreditPreference(SharedPreferences sharedPreferences) {
		boolean autoCredit = sharedPreferences.getBoolean("auto_credit", false);
		autoCreditPreference.setSummary(autoCredit ? getString(R.string.settings_auto_credit_enabled) : getString(R.string.settings_auto_credit_disabled));
	}

	private void updateUsagePreference(SharedPreferences sharedPreferences) {
		boolean autoUsage = sharedPreferences.getBoolean("auto_usage", false);
		autoUsagePreference.setSummary(autoUsage ? getString(R.string.settings_auto_usage_enabled) : getString(R.string.settings_auto_usage_disabled));
	}

	private void updateTopupsPreference(SharedPreferences sharedPreferences) {
		boolean autoTopups = sharedPreferences.getBoolean("auto_topups", false);
		autoTopupsPreference.setSummary(autoTopups ? getString(R.string.settings_auto_topups_enabled) : getString(R.string.settings_auto_topups_disabled));
	}

	private void updateFrequencyPreference() {
		updateFrequencyPreference.setSummary(getString(R.string.settings_frequency, ((ListPreference) updateFrequencyPreference).getEntry()));
	}

	private void updateWidgetActionPreference() {
		widgetActionPreference.setSummary(getString(R.string.settings_widget_action, ((ListPreference) widgetActionPreference).getEntry()));
	}

	private void updateWidgetBackgroundPreference() {
		widgetBackgroundPreference.setSummary(getString(R.string.settings_widget_background, ((ListPreference) widgetBackgroundPreference).getEntry()));
	}

	private void updateSelectMsisdnPreference() {
		selectMsisdnPreference.setSummary(getString(R.string.settings_select_msisdn, ((ListPreference) selectMsisdnPreference).getEntry()));
	}

	private void updateDebugPreference(SharedPreferences sharedPreferences) {
		boolean setDebug = sharedPreferences.getBoolean("set_debug", false);
		setDebugPreference.setSummary(setDebug ? getString(R.string.settings_set_debug_enabled) : getString(R.string.settings_set_debug_disabled));
	}
	
	private void stopService() {
		Intent stop = new Intent(this, MVDataService.class);
		stop.setAction(MVDataService.STOP_SERVICE);
		WakefulIntentService.sendWakefulWork(this, stop);
	}

	private void startService() {
		Intent start = new Intent(this, MVDataService.class);
		start.setAction(MVDataService.SCHEDULE_SERVICE);
		WakefulIntentService.sendWakefulWork(this, start);
	}

	public void setMsisdns(ListPreference preference) {
		DatabaseHelper helper = new DatabaseHelper(this);
		String[] msisdns;
		msisdns = helper.msisdns.getMsisdnList();
		if(msisdns != null) {
			Log.v(MVDataService.class.getSimpleName(), "" + msisdns.length);
			int size = msisdns.length;
			do {
				Log.v(MVDataService.class.getSimpleName(), "msisdn:" + msisdns[size - 1] + "size:" + size);
				size--;
			}
			while(size > 0);
			preference.setEntries(msisdns);
			preference.setEntryValues(msisdns);
		}
		helper.close();
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		setMsisdns((ListPreference) preference);
		return true;
	}

}
