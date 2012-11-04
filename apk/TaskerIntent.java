// Version 1.1

// For usage examples see http://tasker.dinglisch.net/invoketasks.html

package net.dinglisch.android.tasker;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.os.Process;
import android.util.Log;

public class TaskerIntent extends Intent {

	// 3 Tasker versions
	public final static String	TASKER_PACKAGE				= "net.dinglisch.android.tasker";
	public final static String	TASKER_PACKAGE_MARKET		= TASKER_PACKAGE + "m";
	public final static String	TASKER_PACKAGE_CUPCAKE		= TASKER_PACKAGE + "cupcake";

	// Market download URLs
	private final static String	MARKET_DOWNLOAD_URL_PREFIX	= "market://search?q=pname:";
	private final static String	TASKER_MARKET_URL			= MARKET_DOWNLOAD_URL_PREFIX + TASKER_PACKAGE_MARKET;
	private final static String	TASKER_MARKET_URL_CUPCAKE	= MARKET_DOWNLOAD_URL_PREFIX + TASKER_PACKAGE_CUPCAKE;

	// Direct-purchase version
	private final static String	TASKER_DOWNLOAD_URL			= "http://tasker.dinglisch.net/download.html";

	// Intent actions
	public final static String	ACTION_TASK					= TASKER_PACKAGE + ".ACTION_TASK";
	public final static String	ACTION_TASK_COMPLETE		= TASKER_PACKAGE + ".ACTION_TASK_COMPLETE";
	public final static String	ACTION_TASK_SELECT			= TASKER_PACKAGE + ".ACTION_TASK_SELECT";

	// Intent parameters
	public final static String	EXTRA_ACTION_INDEX_PREFIX	= "action";
	public final static String	TASK_NAME_DATA_SCHEME		= "task";
	public final static String	EXTRA_TASK_NAME				= "task_name";
	public final static String	EXTRA_TASK_PRIORITY			= "task_priority";
	public final static String	EXTRA_SUCCESS_FLAG			= "success";
	public final static String	EXTRA_PARAM_LIST			= "params";

	// Intent data

	public final static String	TASK_ID_SCHEME				= "id";

	// For particular actions

	public final static String	DEFAULT_ENCRYPTION_KEY		= "default";
	public final static String	ENCRYPTED_AFFIX				= "tec";
	public final static int		MAX_NO_ARGS					= 6;

	// Bundle keys
	// Only useful for Tasker
	public final static String	ACTION_CODE					= "action";
	public final static String	APP_ARG_PREFIX				= "app:";
	public final static String	ICON_ARG_PREFIX				= "icn:";
	public final static String	ARG_INDEX_PREFIX			= "arg:";

	// Misc
	private final static String	PERMISSION_RUN_TASKS		= TASKER_PACKAGE + ".PERMISSION_RUN_TASKS";

	private final static int	CUPCAKE_SDK_VERSION			= 3;

	// -------------------------- PRIVATE VARS ---------------------------- //

	private final static String	TAG							= "TaskerIntent";

	private final static String	EXTRA_INTENT_VERSION_NUMBER	= "version_number";
	private final static String	INTENT_VERSION_NUMBER		= "1.1";

	// Inclusive values
	private final static int	MIN_PRIORITY				= 0;
	private final static int	MAX_PRIORITY				= 10;

	// For generating random names
	private static Random		rand						= new Random();

	// Tracking state
	private int					actionCount					= 0;
	private int					argCount;

	// -------------------------- PUBLIC METHODS ---------------------------- //

	public static boolean validatePriority(int pri) {
		return ((pri >= MIN_PRIORITY) || (pri <= MAX_PRIORITY));
	}

	// Check if Tasker installed (it may not be enabled...)
	// Use receiverExists if sending Intents

	public static boolean taskerInstalled(Context context) {
		boolean foundFlag = false;

		try {
			context.getPackageManager().getPackageInfo(TASKER_PACKAGE, 0);
			foundFlag = true;
		}
		catch(Exception e) {
		}

		try {
			context.getPackageManager().getPackageInfo(TASKER_PACKAGE_MARKET, 0);
			foundFlag = true;
		}
		catch(Exception e) {
		}

		return foundFlag;
	}

	// Use with startActivity to retrieve Tasker from Android market
	public static Intent getTaskerInstallIntent(boolean marketFlag) {

		return new Intent(Intent.ACTION_VIEW, Uri.parse(marketFlag ? ((SDKVersion() == CUPCAKE_SDK_VERSION) ? TASKER_MARKET_URL_CUPCAKE : TASKER_MARKET_URL)
				: TASKER_DOWNLOAD_URL));
	}

	public static int SDKVersion() {
		try {
			Field f = android.os.Build.VERSION.class.getField("SDK_INT");
			return f.getInt(null);
		}
		catch(Exception e) {
			return CUPCAKE_SDK_VERSION;
		}
	}

	public static IntentFilter getCompletionFilter(String taskName) {

		IntentFilter filter = new IntentFilter(TaskerIntent.ACTION_TASK_COMPLETE);

		filter.addDataScheme(TASK_NAME_DATA_SCHEME);
		filter.addDataPath(taskName, PatternMatcher.PATTERN_LITERAL);

		return filter;
	}

	public static Intent getTaskSelectIntent() {
		return new Intent(ACTION_TASK_SELECT).setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
				| Intent.FLAG_ACTIVITY_NO_HISTORY);
	}

	public static boolean havePermission(Context c) {
		return c.checkPermission(PERMISSION_RUN_TASKS, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED;
	}

	public TaskerIntent() {
		super(ACTION_TASK);
		setRandomData();
		putMetaExtras(getRandomString());
	}

	public TaskerIntent(String taskName) {
		super(ACTION_TASK);
		setRandomData();
		putMetaExtras(taskName);
	}

	public TaskerIntent setTaskPriority(int priority) {

		if(validatePriority(priority))
			putExtra(EXTRA_TASK_PRIORITY, priority);
		else
			Log.e(TAG, "priority out of range: " + MIN_PRIORITY + ":" + MAX_PRIORITY);

		return this;
	}

	public TaskerIntent addParameter(String param) {

		ArrayList<String> params;

		if(hasExtra(EXTRA_PARAM_LIST))
			params = getStringArrayListExtra(EXTRA_PARAM_LIST);
		else {
			params = new ArrayList<String>();
			putStringArrayListExtra(EXTRA_PARAM_LIST, params);
		}

		params.add(param);

		return this;
	}

	public TaskerIntent addAction(int code) {

		actionCount++;
		argCount = 1;

		Bundle actionBundle = new Bundle();

		actionBundle.putInt(ACTION_CODE, code);

		// Add action bundle to intent
		putExtra(EXTRA_ACTION_INDEX_PREFIX + Integer.toString(actionCount), actionBundle);

		return this;
	}

	// string arg
	public TaskerIntent addArg(String arg) {

		Bundle b = getActionBundle();

		if(b != null)
			b.putString(ARG_INDEX_PREFIX + Integer.toString(argCount++), arg);

		return this;
	}

	// int arg
	public TaskerIntent addArg(int arg) {
		Bundle b = getActionBundle();

		if(b != null)
			b.putInt(ARG_INDEX_PREFIX + Integer.toString(argCount++), arg);

		return this;
	}

	// boolean arg
	public TaskerIntent addArg(boolean arg) {
		Bundle b = getActionBundle();

		if(b != null)
			b.putBoolean(ARG_INDEX_PREFIX + Integer.toString(argCount++), arg);

		return this;
	}

	// Application arg
	public TaskerIntent addArg(String pkg, String cls) {
		Bundle b = getActionBundle();

		if(b != null) {
			StringBuilder builder = new StringBuilder();
			builder.append(APP_ARG_PREFIX).append(pkg).append(",").append("cls");
			b.putString(ARG_INDEX_PREFIX + Integer.toString(argCount++), b.toString());
		}

		return this;
	}

	public IntentFilter getCompletionFilter() {
		return getCompletionFilter(getTaskName());
	}

	public String getTaskName() {
		return getStringExtra(EXTRA_TASK_NAME);
	}

	// Tasker must be installed and enabled for this to be true
	public boolean receiverExists(Context context) {
		List<ResolveInfo> recs = context.getPackageManager().queryBroadcastReceivers(this, 0);
		return ((recs != null) && (recs.size() > 0));
	}

	// -------------------- PRIVATE METHODS -------------------- //

	private String getRandomString() {
		return Long.toString(rand.nextLong());
	}

	// so that if multiple TaskerIntents are used in PendingIntents there's
	// virtually no
	// clash chance
	private void setRandomData() {
		setData(Uri.parse(TASK_ID_SCHEME + ":" + getRandomString()));
	}

	private Bundle getActionBundle() {

		Bundle toReturn = null;

		if(argCount > MAX_NO_ARGS)
			Log.e(TAG, "maximum number of arguments exceeded (" + MAX_NO_ARGS + ")");
		else {
			String key = EXTRA_ACTION_INDEX_PREFIX + Integer.toString(actionCount);

			if(this.hasExtra(key))
				toReturn = getBundleExtra(key);
			else
				Log.e(TAG, "no actions added yet");
		}

		return toReturn;
	}

	private void putMetaExtras(String taskName) {
		putExtra(EXTRA_INTENT_VERSION_NUMBER, INTENT_VERSION_NUMBER);
		putExtra(EXTRA_TASK_NAME, taskName);
	}
}
