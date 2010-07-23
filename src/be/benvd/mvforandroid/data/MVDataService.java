/*
	Copyright (C) 2010 Ben Van Daele (vandaeleben@gmail.com)

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

package be.benvd.mvforandroid.data;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class MVDataService extends WakefulIntentService {

	private final String URL_USAGE = "https://mobilevikings.com/api/2.0/basic/usage.json";
	private final String URL_CREDIT = "https://mobilevikings.com/api/2.0/basic/sim_balance.json";
	private final String URL_TOPUPS = "https://mobilevikings.com/api/2.0/basic/top_up_history.json";

	private static final long RETRY_TIMEOUT = 30000;

	public static final String UPDATE_ACTION = "be.benvd.mvforandroid.data.Update";
	public static final String CREDIT_UPDATED = "be.benvd.mvforandroid.data.CreditUpdated";
	public static final String USAGE_UPDATED = "be.benvd.mvforandroid.data.UsageUpdated";
	public static final String TOPUPS_UPDATED = "be.benvd.mvforandroid.data.TopupsUpdated";

	private Intent creditBroadcast = new Intent(CREDIT_UPDATED);
	private Intent usageBroadcast = new Intent(USAGE_UPDATED);
	private Intent topupsBroadcast = new Intent(TOPUPS_UPDATED);

	private IBinder binder;
	private AlarmManager alarm = null;
	private PendingIntent wakefulWorkIntent = null;
	private SharedPreferences prefs;
	private DatabaseHelper helper;

	public class LocalBinder extends Binder {
		public MVDataService getService() {
			return MVDataService.this;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}

	public MVDataService() {
		super("MVDataService");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		binder = new LocalBinder();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		helper = new DatabaseHelper(this);
		alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		Intent i = new Intent(this, OnAlarmReceiver.class);
		wakefulWorkIntent = PendingIntent.getBroadcast(this, 0, i, 0);
		scheduleNextUpdate();
	}

	/**
	 * Schedules the next execution of doWakefulWork, using the frequency specified in the Preferences.
	 */
	private void scheduleNextUpdate() {
		long delay = Long.parseLong(prefs.getString("update_frequency", "86400000"));
		alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay, wakefulWorkIntent);
		Log.d("MVDataService", "Scheduled update in " + delay + "ms.");
	}

	/**
	 * Schedules the next execution of doWakefulWork using RETRY_TIMEOUT.
	 */
	private void retry() {
		alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + RETRY_TIMEOUT,
				wakefulWorkIntent);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	/**
	 * Does the actual work.
	 */
	@Override
	protected void doWakefulWork(Intent intent) {
		if (intent.getAction().equals(UPDATE_ACTION)) {
			try {
				updateCredit();
				updateUsage();
				updateTopups();
			} catch (ClientProtocolException e) {
				Log.e("MVDataService", "Exception in doWakefulWork", e);
				retry();
			} catch (IOException e) {
				Log.e("MVDataService", "Exception in doWakefulWork", e);
				retry();
			} catch (JSONException e) {
				Log.e("MVDataService", "Exception in doWakefulWork", e);
				retry();
			} finally {
				helper.close();
			}

			scheduleNextUpdate();
		}
	}

	private void updateCredit() throws ClientProtocolException, IOException, JSONException {
		if (prefs.getBoolean("auto_credit", false)) {
			String username = prefs.getString("username", null);
			String password = prefs.getString("password", null);
			String response = MVDataHelper.getTestResponse(username, password, URL_CREDIT);
			helper.credit.update(new JSONObject(response));
			sendBroadcast(creditBroadcast);
			Log.i("MVDataService", "Updated credit");
		}
	}

	private void updateUsage() throws ClientProtocolException, IOException, JSONException {
		if (prefs.getBoolean("auto_usage", false)) {
			String username = prefs.getString("username", null);
			String password = prefs.getString("password", null);
			String response = MVDataHelper.getTestResponse(username, password, URL_USAGE);
			helper.usage.update(new JSONArray(response), false);
			sendBroadcast(usageBroadcast);
			Log.i("MVDataService", "Updated usage");
		}
	}

	private void updateTopups() throws ClientProtocolException, IOException, JSONException {
		if (prefs.getBoolean("auto_topups", false)) {
			String username = prefs.getString("username", null);
			String password = prefs.getString("password", null);
			String response = MVDataHelper.getTestResponse(username, password, URL_TOPUPS);
			helper.topups.update(new JSONArray(response), false);
			sendBroadcast(topupsBroadcast);
			Log.i("MVDataService", "Updated topups");
		}
	}

}
