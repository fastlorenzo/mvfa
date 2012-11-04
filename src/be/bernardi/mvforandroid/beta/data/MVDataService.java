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

package be.bernardi.mvforandroid.beta.data;

import java.io.IOException;
import java.net.UnknownHostException;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
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
import be.bernardi.mvforandroid.beta.Log;
import be.bernardi.mvforandroid.beta.R;
import be.bernardi.mvforandroid.beta.MainActivity;
import be.bernardi.mvforandroid.beta.activities.AuthActivity;
import be.bernardi.mvforandroid.beta.exception.BadLoginException;
import be.bernardi.mvforandroid.beta.exception.NoMsisdnException;
import be.bernardi.mvforandroid.beta.mvapi.TopUpHistory;
import be.bernardi.mvforandroid.beta.mvapi.Usage;
import be.bernardi.mvforandroid.beta.mvapi.MVAPIHelper;
import be.bernardi.mvforandroid.beta.mvapi.PricePlanDetails;

import com.commonsware.cwac.wakeful.WakefulIntentService;

/**
 * @author Lorenzo Bernardi
 */
public class MVDataService extends WakefulIntentService {

	public final static String		URL_USAGE				= "https://mobilevikings.com/api/2.0/basic/usage.json";
	public final static String		URL_CREDIT				= "https://mobilevikings.com/api/2.0/basic/sim_balance.json?add_price_plan=1";
	public final static String		URL_TOPUPS				= "https://mobilevikings.com/api/2.0/basic/top_up_history.json";
	public static final String		URL_PRICE_PLAN			= "https://mobilevikings.com/api/2.0/basic/price_plan_details.json";
	public static final String		URL_MSISDN				= "https://mobilevikings.com/api/2.0/basic/msisdn_list.json";
	public static final String		URL_POINTSSTAT			= "https://mobilevikings.com/api/2.0/basic/points/stats.json";

	public static final String		UPDATE_ALL				= "be.bernardi.mvforandroid.beta.data.Update";
	public static final String		UPDATE_CREDIT			= "be.bernardi.mvforandroid.beta.data.UpdateCredit";
	public static final String		UPDATE_USAGE			= "be.bernardi.mvforandroid.beta.data.UpdateUsage";
	public static final String		UPDATE_TOPUPS			= "be.bernardi.mvforandroid.beta.data.UpdateTopups";
	public static final String		UPDATE_USAGE_STARTTIME	= "be.bernardi.mvforandroid.beta.data.UsageStartTime";
	public static final String		UPDATE_USAGE_ENDTIME	= "be.bernardi.mvforandroid.beta.data.UsageEndTime";
	public static final String		UPDATE_MSISDNS			= "be.bernardi.mvforandroid.beta.data.UpdateMsisdns";
	public static final String		UPDATE_POINTSSTAT		= "be.bernardi.mvforandroid.beta.data.UpdatePointsStat";

	public static final String		CREDIT_UPDATED			= "be.bernardi.mvforandroid.beta.data.CreditUpdated";
	public static final String		USAGE_UPDATED			= "be.bernardi.mvforandroid.beta.data.UsageUpdated";
	public static final String		TOPUPS_UPDATED			= "be.bernardi.mvforandroid.beta.data.TopupsUpdated";
	public static final String		POINTSSTAT_UPDATED		= "be.bernardi.mvforandroid.beta.data.PointsStatUpdated";

	public static final String		EXCEPTION				= "be.bernardi.mvforandroid.beta.data.Exception";
	public static final String		STOP_SERVICE			= "be.bernardi.mvforandroid.beta.data.StopService";
	public static final String		START_SERVICE			= "be.bernardi.mvforandroid.beta.data.StartService";
	public static final String		SCHEDULE_SERVICE		= "be.bernardi.mvforandroid.beta.data.ScheduleService";

	public static final String		PRICE_PLAN_TOPUP_AMOUNT	= "price_plan_topup_amount";
	public static final String		PRICE_PLAN_DATA_AMOUNT	= "price_plan_data_amount";
	public static final String		PRICE_PLAN_SMS_AMOUNT	= "price_plan_sms_amount";
	public static final String		PRICE_PLAN_NAME			= "price_plan_name";

	private Intent					creditBroadcast			= new Intent(CREDIT_UPDATED);
	private Intent					usageBroadcast			= new Intent(USAGE_UPDATED);
	private Intent					topupsBroadcast			= new Intent(TOPUPS_UPDATED);
	private Intent					pointsStatBroadcast		= new Intent(POINTSSTAT_UPDATED);

	private Intent					exceptionBroadcast		= new Intent(EXCEPTION);
	private Intent					enableProgress			= new Intent(MainActivity.ENABLE_PROGRESS);
	private Intent					disableProgress			= new Intent(MainActivity.DISABLE_PROGRESS);

	private AlarmManager			alarm					= null;
	private PendingIntent			wakefulWorkIntent		= null;
	private SharedPreferences		prefs;
	private DatabaseHelper			helper;
	private MVAPIHelper				mvhelper;

	private static MVDataService	instance;

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
		try {
			mvhelper = new MVAPIHelper(prefs.getString("token", ""), prefs.getString("token_secret", ""));
		}
		catch(Exception e) {
			Log.w("Exception creating helper", e);
		}
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
		Log.d("Update scheduled in " + delay + "ms");
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
		Log.d("doWakefulWork:" + intent.getAction());
		String action = intent.getAction();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if(!(prefs.contains("token") && prefs.contains("token_secret"))) {
			Log.d("Credentials not set, starting intent");
			Intent i = new Intent(this, AuthActivity.class);
			i.putExtra("consumer_key", MainActivity.CONSUMER_KEY);
			i.putExtra("consumer_secret", MainActivity.CONSUMER_SECRET);
			i.putExtra("access_token_url", MainActivity.ACCESS_TOKEN_URL);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
		}
		else {
			String msisdn = "";
			if(intent.hasExtra("msisdn")) {
				msisdn = intent.getStringExtra("msisdn");
			}
			else {
				msisdn = prefs.getString("select_msisdn", "");
			}
			// mvhelper.setMsisdn(msisdn);
			try {
				if(msisdn.equals("") && (!action.equals(UPDATE_MSISDNS))){
					sendBroadcast(enableProgress);
					updateMsisdns();
					sendBroadcast(disableProgress);
					throw new NoMsisdnException();
				}
				if(action.equals(UPDATE_CREDIT)) {
					sendBroadcast(enableProgress);
					updateCredit(msisdn);
					sendBroadcast(disableProgress);
				}
				else if(action.equals(UPDATE_USAGE)) {
					sendBroadcast(enableProgress);
					updateUsage(msisdn, intent.getLongExtra(UPDATE_USAGE_STARTTIME, 0), intent.getLongExtra(UPDATE_USAGE_ENDTIME, 0));
					sendBroadcast(disableProgress);
				}
				else if(action.equals(UPDATE_TOPUPS)) {
					sendBroadcast(enableProgress);
					updateTopups(msisdn);
					sendBroadcast(disableProgress);
				}
				else if(action.equals(UPDATE_POINTSSTAT)) {
					sendBroadcast(enableProgress);
					updatePointsStat(msisdn);
					sendBroadcast(disableProgress);
				}
				else if(action.equals(UPDATE_ALL)) {
					if(prefs.getBoolean("auto_credit", true))
						updateCredit(msisdn);
					if(prefs.getBoolean("auto_usage", false))
						updateUsage(msisdn);
					if(prefs.getBoolean("auto_topups", false))
						updateTopups(msisdn);
					scheduleNextUpdate();
				}
				else if(action.equals(STOP_SERVICE)) {
					Log.d("Update canceled");
					alarm.cancel(wakefulWorkIntent);
					stopSelf();
				}
				else if(action.equals(SCHEDULE_SERVICE)) {
					scheduleNextUpdate();
				}
				else if(action.equals(UPDATE_MSISDNS)) {
					sendBroadcast(enableProgress);
					updateMsisdns();
					sendBroadcast(disableProgress);
				}
			}
			catch(UnknownHostException e) {
				Log.d("UnknownHostException:", e);
				exceptionBroadcast.putExtra("message", getString(R.string.exception_message));
				sendBroadcast(exceptionBroadcast);
				sendBroadcast(disableProgress);
			}
			catch(IOException e) {
				Log.d("IOException:", e);
				exceptionBroadcast.putExtra("message", e.getMessage());
				sendBroadcast(exceptionBroadcast);
				sendBroadcast(disableProgress);
			}
			catch(JSONException e) {
				Log.d("JSONException:", e);
				exceptionBroadcast.putExtra("message", e.getMessage());
				sendBroadcast(exceptionBroadcast);
				sendBroadcast(disableProgress);
			}
			catch(NumberFormatException e) {
				Log.d("NumberFormatException:", e);
				exceptionBroadcast.putExtra("message", e.getMessage());
				sendBroadcast(exceptionBroadcast);
				sendBroadcast(disableProgress);
			}
			catch(NoMsisdnException e) {
				Log.d("NoMsisdnException:", e);
				exceptionBroadcast.putExtra("message", getString(R.string.exception_msisdn_message));
				sendBroadcast(exceptionBroadcast);
				sendBroadcast(disableProgress);
			}
			catch(BadLoginException e) {
				Log.d("BadLoginException:", e);
				exceptionBroadcast.putExtra("message", getString(R.string.exception_badlogin_message));
				sendBroadcast(exceptionBroadcast);
				sendBroadcast(disableProgress);
			}
			catch(Exception e) {
				Log.d("Exception:", e);
				exceptionBroadcast.putExtra("message", e.getMessage());
				sendBroadcast(exceptionBroadcast);
				sendBroadcast(disableProgress);
			}
		}
	}

	private void updatePricePlan(String msisdn) throws JSONException, IOException, BadLoginException, OAuthMessageSignerException,
			OAuthExpectationFailedException, OAuthCommunicationException {
		PricePlanDetails pricePlan = mvhelper.getPricePlanDetails(msisdn);
		Editor edit = prefs.edit();
		edit.putString(PRICE_PLAN_NAME, pricePlan.getName());
		if(!pricePlan.getName().equals("Data")) {
			edit.putInt(PRICE_PLAN_SMS_AMOUNT, pricePlan.getSmsBundle().getAmount().intValue());
		}
		edit.putInt(PRICE_PLAN_DATA_AMOUNT, pricePlan.getDataBundle().getAmount().intValue());
		edit.putFloat(PRICE_PLAN_TOPUP_AMOUNT, pricePlan.getTopUpAmount());
		edit.commit();

		Log.d("Updated price plan");
	}

	public String[] getMsisdnList() throws JSONException, IOException, BadLoginException, OAuthMessageSignerException, OAuthExpectationFailedException,
			OAuthCommunicationException {
		String[] msisdns;
		JSONArray jsonArray = mvhelper.getMsisdnList();
		msisdns = new String[jsonArray.length()];
		for(int i = 0; i < jsonArray.length(); i++) {
			msisdns[i] = jsonArray.getString(i);
			Log.d("MSISDN:" + jsonArray.getString(i));
		}
		Log.d("Got MSISDN list");
		helper.msisdns.getMsisdnList();
		return msisdns;
	}

	public void updateMsisdns() throws JSONException, IOException, BadLoginException, OAuthMessageSignerException, OAuthExpectationFailedException,
			OAuthCommunicationException {
		JSONArray jsonArray = mvhelper.getMsisdnList();
		Log.d("Updating MSISDN list");
		helper.msisdns.update(jsonArray);
	}

	private void updateCredit(String msisdn) throws JSONException, IOException, BadLoginException, OAuthMessageSignerException,
			OAuthExpectationFailedException, OAuthCommunicationException {
		updatePricePlan(msisdn);
		helper.credit.update(mvhelper.getSimBalance(msisdn));
		sendBroadcast(creditBroadcast);
		Log.d("Updated credit");
	}

	private void updateUsage(String msisdn) throws IOException, JSONException, BadLoginException, OAuthMessageSignerException, OAuthExpectationFailedException,
			OAuthCommunicationException {
		updateUsage(msisdn, 0, 0);
	}

	private void updateUsage(String msisdn, long starttime, long endtime) throws IOException, JSONException, BadLoginException, OAuthMessageSignerException,
			OAuthExpectationFailedException, OAuthCommunicationException {
		Usage usage = mvhelper.getUsage(msisdn, starttime, endtime);
		helper.usage.update(usage);
		sendBroadcast(usageBroadcast);
		Log.d("Updated usage");
	}

	private void updateTopups(String msisdn) throws IOException, JSONException, NoMsisdnException, BadLoginException, OAuthMessageSignerException,
			OAuthExpectationFailedException, OAuthCommunicationException {
		if(!prefs.contains("select_msisdn"))
			throw new NoMsisdnException();
		TopUpHistory topup = mvhelper.getTopUpHistory(msisdn);
		helper.topups.update(topup);
		sendBroadcast(topupsBroadcast);
		Log.d("Updated topups");
	}

	private void updatePointsStat(String msisdn) throws JSONException, IOException, NoMsisdnException, BadLoginException {
		String username = prefs.getString("username", null);
		String password = prefs.getString("password", null);
		if(!prefs.contains("select_msisdn"))
			throw new NoMsisdnException();
		String response = MVDataHelper.getResponse(username, password, URL_POINTSSTAT);
		helper.pointsstat.update(new JSONObject(response), true);

		sendBroadcast(pointsStatBroadcast);
		Log.d("Updated points statistics");
	}

	private static String[] getStackTraceArray(Exception e) {
		StackTraceElement[] stackTraceElements = e.getStackTrace();
		String[] stackTracelines = new String[stackTraceElements.length];
		int i = 0;
		for(StackTraceElement se : stackTraceElements) {
			stackTracelines[i++] = se.toString();
		}
		return stackTracelines;
	}
}
