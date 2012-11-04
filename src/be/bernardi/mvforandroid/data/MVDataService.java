/*
	Copyright (C) 	2011 Lorenzo Bernardi (fastlorenzo@gmail.com)
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

package be.bernardi.mvforandroid.data;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import be.bernardi.mvforandroid.R;
import be.bernardi.mvforandroid.exception.BadLoginException;
import be.bernardi.mvforandroid.exception.NoMsisdnException;

import com.commonsware.cwac.wakeful.WakefulIntentService;

/**
 * @author Lorenzo Bernardi
 * This class is the handles the "service" of the application
 */
public class MVDataService extends WakefulIntentService {

	public final static String		URL_USAGE					= "https://mobilevikings.com/api/2.0/basic/usage.json";
	public final static String		URL_CREDIT					= "https://mobilevikings.com/api/2.0/basic/sim_balance.json?add_price_plan=1";
	public final static String		URL_TOPUPS					= "https://mobilevikings.com/api/2.0/basic/top_up_history.json";
	public static final String		URL_PRICE_PLAN				= "https://mobilevikings.com/api/2.0/basic/price_plan_details.json";
	public static final String		URL_MSISDN					= "https://mobilevikings.com/api/2.0/basic/msisdn_list.json";

	public static final String		UPDATE_ALL					= "be.bernardi.mvforandroid.data.Update";
	public static final String		UPDATE_CREDIT				= "be.bernardi.mvforandroid.data.UpdateCredit";
	public static final String		UPDATE_USAGE				= "be.bernardi.mvforandroid.data.UpdateUsage";
	public static final String		UPDATE_TOPUPS				= "be.bernardi.mvforandroid.data.UpdateTopups";
	public static final String		UPDATE_USAGE_STARTTIME		= "be.bernardi.mvforandroid.data.UsageStartTime";
	public static final String		UPDATE_USAGE_ENDTIME		= "be.bernardi.mvforandroid.data.UsageEndTime";
	public static final String		UPDATE_MSISDNS				= "be.bernardi.mvforandroid.data.UpdateMsisdns";

	public static final String		CREDIT_UPDATED				= "be.bernardi.mvforandroid.data.CreditUpdated";
	public static final String		USAGE_UPDATED				= "be.bernardi.mvforandroid.data.UsageUpdated";
	public static final String		TOPUPS_UPDATED				= "be.bernardi.mvforandroid.data.TopupsUpdated";
	public static final String		EXCEPTION					= "be.bernardi.mvforandroid.data.Exception";
	public static final String		STOP_SERVICE				= "be.bernardi.mvforandroid.data.StopService";
	public static final String		SCHEDULE_SERVICE			= "be.bernardi.mvforandroid.data.ScheduleService";

	private Intent					creditBroadcast				= new Intent(CREDIT_UPDATED);
	private Intent					usageBroadcast				= new Intent(USAGE_UPDATED);
	private Intent					topupsBroadcast				= new Intent(TOPUPS_UPDATED);
	private Intent					exceptionBroadcast			= new Intent(EXCEPTION);



	private AlarmManager			alarm						= null;
	private PendingIntent			wakefulWorkIntent			= null;
	private SharedPreferences		prefs;
	private DatabaseHelper			helper;

	private static String			price_plan;

	private static MVDataService	instance;

	
	/**
	 * 
	 */
	public static MVDataService getInstance() {
		if(instance == null) {
			instance = new MVDataService();
		}
		return instance;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		return null;
	}

	public MVDataService() {
		super("MVDataService");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		helper = new DatabaseHelper(this);
		alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		instance = this;

		Intent i = new Intent(this, OnAlarmReceiver.class);
		wakefulWorkIntent = PendingIntent.getBroadcast(this, 0, i, 0);
	}

	/**
	 * Schedules the next execution of doWakefulWork, using the frequency
	 * specified in the Preferences.
	 */
	private void scheduleNextUpdate() {
		long delay = Long.parseLong(prefs.getString("update_frequency", "86400000"));
		alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay, wakefulWorkIntent);
		if(this.getDebug()) {
			Log.d(MVDataService.class.getSimpleName(), "Update scheduled in " + delay + "ms");
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		helper.close();
	}

	/**
	 * Does the actual work.
	 */
	@Override
	protected void doWakefulWork(Intent intent) {
		String action = intent.getAction();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		try {
			if(action.equals(UPDATE_CREDIT)) {
				updateCredit();
			}
			else if(action.equals(UPDATE_TOPUPS)) {
				updateTopups();
			}
			else if(action.equals(UPDATE_USAGE)) {
				updateUsage(intent.getLongExtra(UPDATE_USAGE_STARTTIME, 0), intent.getLongExtra(UPDATE_USAGE_ENDTIME, 0));
			}
			else if(action.equals(UPDATE_ALL)) {
				if(prefs.getBoolean("auto_credit", true))
					updateCredit();
				if(prefs.getBoolean("auto_usage", false))
					updateUsage();
				if(prefs.getBoolean("auto_topups", false))
					updateTopups();
				scheduleNextUpdate();
			}
			else if(action.equals(STOP_SERVICE)) {
				if(this.getDebug()) {
					Log.d(MVDataService.class.getSimpleName(), "Update canceled");
				}
				alarm.cancel(wakefulWorkIntent);
				stopSelf();
			}
			else if(action.equals(SCHEDULE_SERVICE)) {
				scheduleNextUpdate();
			}
			else if(action.equals(UPDATE_MSISDNS)) {
				updateMsisdns();
			}
		}
		catch(IOException e) {
			exceptionBroadcast.putExtra(EXCEPTION, e);
			exceptionBroadcast.putExtra("message", e.getMessage());
			sendBroadcast(exceptionBroadcast);
		}
		catch(JSONException e) {
			exceptionBroadcast.putExtra(EXCEPTION, e);
			exceptionBroadcast.putExtra("message", e.getMessage());
			sendBroadcast(exceptionBroadcast);
		}
		catch(NumberFormatException e) {
			exceptionBroadcast.putExtra(EXCEPTION, e);
			exceptionBroadcast.putExtra("message", e.getMessage());
			sendBroadcast(exceptionBroadcast);
		}
		catch(NoMsisdnException e) {
			exceptionBroadcast.putExtra(EXCEPTION, e);
			exceptionBroadcast.putExtra("message", getString(R.string.exception_msisdn_message));
			sendBroadcast(exceptionBroadcast);
		}
		catch(BadLoginException e) {
			exceptionBroadcast.putExtra(EXCEPTION, e);
			exceptionBroadcast.putExtra("message", getString(R.string.exception_badlogin_message));
			sendBroadcast(exceptionBroadcast);
		}
		catch(Exception e) {
			exceptionBroadcast.putExtra(EXCEPTION, e);
			exceptionBroadcast.putExtra("message", e.getMessage());
			sendBroadcast(exceptionBroadcast);
		}
	}

	private void updatePricePlan() throws JSONException, IOException, NoMsisdnException, BadLoginException {
		String username = prefs.getString("username", null);
		String password = prefs.getString("password", null);
		if(prefs.getString("select_msisdn", "none").equals("none"))
			throw new NoMsisdnException();
		String response = MVDataHelper.getResponse(username, password, URL_PRICE_PLAN + "?msisdn=" + prefs.getString("select_msisdn", null));
		JSONObject json = new JSONObject(response);
		price_plan = json.getString("name");
		Editor edit = prefs.edit();
		edit.putString(MVDataHelper.PRICE_PLAN_NAME, json.getString("name"));
		if(price_plan.equals("Data")) {
			edit.putInt(MVDataHelper.PRICE_PLAN_DATA_AMOUNT, json.getJSONArray("bundles").getJSONObject(0).getInt("amount"));
		}
		else {
			edit.putInt(MVDataHelper.PRICE_PLAN_SMS_AMOUNT, json.getJSONArray("bundles").getJSONObject(0).getInt("amount"));
			edit.putInt(MVDataHelper.PRICE_PLAN_DATA_AMOUNT, json.getJSONArray("bundles").getJSONObject(1).getInt("amount"));
		}
		edit.putFloat(MVDataHelper.PRICE_PLAN_TOPUP_AMOUNT, Float.parseFloat(json.getString("top_up_amount")));
		edit.commit();
		if(this.getDebug()) {
			Log.d(MVDataService.class.getSimpleName(), "Updated price plan");
		}
	}

	public String[] getMsisdnList() throws JSONException, IOException, BadLoginException {
		String[] msisdns;
		// prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String username = prefs.getString("username", null);
		String password = prefs.getString("password", null);
		String response = MVDataHelper.getResponse(username, password, URL_MSISDN);
		JSONArray jsonArray = new JSONArray(response);
		msisdns = new String[jsonArray.length()];
		for(int i = 0; i < jsonArray.length(); i++) {
			msisdns[i] = jsonArray.getString(i);
			if(this.getDebug()) {
				Log.d(MVDataService.class.getSimpleName(), "MSISDN:" + jsonArray.getString(i));
			}
		}
		if(this.getDebug()) {
			Log.d(MVDataService.class.getSimpleName(), "Got MSISDN list");
		}
		helper.msisdns.getMsisdnList();
		return msisdns;
	}

	public void updateMsisdns() throws JSONException, IOException, BadLoginException {
		// Log.v(MVDataService.class.getSimpleName(), "Start updateMsisdns()");
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String username = prefs.getString("username", null);
		String password = prefs.getString("password", null);
		// Log.v(MVDataService.class.getSimpleName(), "username:"+username);
		// Log.v(MVDataService.class.getSimpleName(), "password:"+password);
		String response = MVDataHelper.getResponse(username, password, URL_MSISDN);
		JSONArray jsonArray = new JSONArray(response);
		if(this.getDebug()) {
			Log.d(MVDataService.class.getSimpleName(), "Updating MSISDN list");
		}
		helper.msisdns.update(jsonArray);
	}

	private void updateCredit() throws JSONException, IOException, NoMsisdnException, BadLoginException {
		updatePricePlan();
		String username = prefs.getString("username", null);
		String password = prefs.getString("password", null);
		if(prefs.getString("select_msisdn", "none").equals("none"))
			throw new NoMsisdnException();
		String response = MVDataHelper.getResponse(username, password, URL_CREDIT + "&msisdn=" + prefs.getString("select_msisdn", null));
		if(price_plan.equals("Data"))
			helper.credit.update(new JSONObject(response), true);
		else
			helper.credit.update(new JSONObject(response), false);
		sendBroadcast(creditBroadcast);
		if(this.getDebug()) {
			Log.d(MVDataService.class.getSimpleName(), "Updated credit");
		}
	}

	private void updateUsage() throws IOException, JSONException, NoMsisdnException, BadLoginException {
		updateUsage(0, 0);
	}

	private void updateUsage(long starttime, long endtime) throws IOException, JSONException, NoMsisdnException, BadLoginException {
		String username = prefs.getString("username", null);
		String password = prefs.getString("password", null);
		if(prefs.getString("select_msisdn", "none").equals("none"))
			throw new NoMsisdnException();
		String url = URL_USAGE + "?msisdn=" + prefs.getString("select_msisdn", null);
		if(starttime != 0 && endtime != 0) {
			SimpleDateFormat formatTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			String start = formatTime.format(new Date(starttime));
			String end = formatTime.format(new Date(endtime));
			url += "&from_date=" + start + "&until_date=" + end;
		}

		String response = MVDataHelper.getResponse(username, password, url);
		helper.usage.update(new JSONArray(response));
		sendBroadcast(usageBroadcast);
		if(this.getDebug()) {
			Log.d(MVDataService.class.getSimpleName(), "Updated usage");
		}
	}

	private void updateTopups() throws IOException, JSONException, NoMsisdnException, BadLoginException {
		String username = prefs.getString("username", null);
		String password = prefs.getString("password", null);
		if(prefs.getString("select_msisdn", "none").equals("none"))
			throw new NoMsisdnException();
		String response = MVDataHelper.getResponse(username, password, URL_TOPUPS + "?msisdn=" + prefs.getString("select_msisdn", null));
		helper.topups.update(new JSONArray(response), false);
		sendBroadcast(topupsBroadcast);
		if(this.getDebug()) {
			Log.d(MVDataService.class.getSimpleName(), "Updated topups");
		}
	}

	public boolean getDebug() {
		return prefs.getBoolean("set_debug", false);
	}
}
