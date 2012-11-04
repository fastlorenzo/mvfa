package be.bernardi.mvforandroid.beta;

import be.bernardi.mvforandroid.beta.R;
import be.bernardi.mvforandroid.beta.activities.AuthActivity;
import be.bernardi.mvforandroid.beta.data.DatabaseHelper;
import be.bernardi.mvforandroid.beta.data.MVDataService;
import be.bernardi.mvforandroid.beta.exception.BadLoginException;
import be.bernardi.mvforandroid.beta.fragments.CreditFragment;
import be.bernardi.mvforandroid.beta.fragments.TopupsFragment;
import be.bernardi.mvforandroid.beta.fragments.UsageFragment;
import be.bernardi.mvforandroid.beta.mvapi.XAuthHelper;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.Window;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {
	private SharedPreferences		prefs;

	private ViewPager				mViewPager;
	private TabsAdapter				mTabsAdapter;

	public static final String		CONSUMER_KEY		= "VhbdZYB3kC2C6phf4E";
	public static final String		CONSUMER_SECRET		= "wM98njrvyqRckSvTHadcRxUyUBDnuddx";
	public static final String		ACCESS_TOKEN_URL	= "https://mobilevikings.com/api/2.0/oauth/access_token/";
	public static final String		ANALYTICS_UA		= "UA-28043165-1";

	public static final String		DISABLE_PROGRESS	= "be.bernardi.mvforandroid.beta.disable_progress";
	public static final String		ENABLE_PROGRESS		= "be.bernardi.mvforandroid.beta.enable_progress";
	public static final String		SHOW_MESSAGE		= "be.bernardi.mvforandroid.beta.show_message";

	private GoogleAnalyticsTracker	tracker;

	private BroadcastReceiver		mainReceiver		= new BroadcastReceiver() {
															@Override
															public void onReceive(Context context, Intent intent) {
																if(intent.getAction().equals(ENABLE_PROGRESS)) {
																	setProgressBarIndeterminateVisibility(Boolean.TRUE);
																	MainMenuFragment.setRefreshActionItemState(true);
																	Log.d("ENABLE_PROGRESS");
																}
																else if(intent.getAction().equals(DISABLE_PROGRESS)) {
																	setProgressBarIndeterminateVisibility(false);
																	MainMenuFragment.setRefreshActionItemState(false);
																	setProgressBarIndeterminateVisibility(Boolean.FALSE);
																	Log.d("DISABLE_PROGRESS");
																}
																else if(intent.getAction().equals(SHOW_MESSAGE)) {
																	showMessage(intent.getExtras().getString("message"));
																}
																else if(intent.getAction().equals(MVDataService.EXCEPTION)) {
																	showMessage(intent.getExtras().getString("message"));
																	Log.d("Received: " + intent.getAction());
																}
															}
														};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_PROGRESS);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// set titles and fragments for view pager
		ActionBar.Tab tab1 = getSupportActionBar().newTab().setText(getApplicationContext().getString(R.string.title_credit));
		ActionBar.Tab tab2 = getSupportActionBar().newTab().setText(getApplicationContext().getString(R.string.title_usage));
		ActionBar.Tab tab3 = getSupportActionBar().newTab().setText(getApplicationContext().getString(R.string.title_topups));

		mViewPager = (ViewPager) findViewById(R.id.pager);
		mTabsAdapter = new TabsAdapter(this, getSupportActionBar(), mViewPager);
		mTabsAdapter.addTab(tab1, CreditFragment.class);
		mTabsAdapter.addTab(tab2, UsageFragment.class);
		mTabsAdapter.addTab(tab3, TopupsFragment.class);

		if(savedInstanceState != null) {
			getSupportActionBar().setSelectedNavigationItem(savedInstanceState.getInt("index"));
		}

		// Make sure the two menu fragments are created.
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		Fragment mainMenu = fm.findFragmentByTag("mainMenu");
		if(mainMenu == null) {
			mainMenu = new MainMenuFragment(this);
			ft.add(mainMenu, "mainMenu");
		}
		ft.commit();

		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.setDebug(true);
		// tracker.setAnonymizeIp(true);
		tracker.startNewSession(MainActivity.ANALYTICS_UA, this);
		tracker.setCustomVar(1, this.getVersionName(), this.getVersionName());

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if(!(prefs.contains("token") && prefs.contains("token_secret"))) {
			if(prefs.contains("username") && prefs.contains("password")) {
				XAuthHelper authHelper = new XAuthHelper(CONSUMER_KEY, CONSUMER_SECRET, ACCESS_TOKEN_URL);
				try {
					if(authHelper.performAuth(prefs.getString("username", ""), prefs.getString("password", ""))) {
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
						Editor ed = prefs.edit();
						String token = authHelper.getToken();
						String token_secret = authHelper.getToken_secret();
						ed.putString("token", token);
						ed.putString("token_secret", token_secret);
						ed.commit();
						Intent i = new Intent(MainActivity.this, MVDataService.class);
						i.setAction(MVDataService.UPDATE_MSISDNS);
						WakefulIntentService.sendWakefulWork(MainActivity.this, i);
						i.setAction(MVDataService.SCHEDULE_SERVICE);
						WakefulIntentService.sendWakefulWork(MainActivity.this, i);
						sendBroadcast(new Intent(MVDataService.CREDIT_UPDATED));
						Editor edit = prefs.edit();
						edit.remove("username").remove("password").commit();
						setTitle(prefs.getString("select_msisdn", getString(R.string.select_msisdn_click)));
					}
					else {
						throw new Exception("Error getting access token");
					}
				}
				catch(BadLoginException e) {
					Toast.makeText(MainActivity.this, getString(R.string.exception_badlogin_message), Toast.LENGTH_LONG).show();
					Editor edit = prefs.edit();
					edit.remove("username").remove("password").commit();
					setTitle(getString(R.string.select_msisdn_click));
					getToken();
				}
				catch(Exception e) {
					Log.w("Exception in Auth dialog", e);
				}
			}
			else {
				setTitle(getString(R.string.select_msisdn_click));
				getToken();
			}
		}
		else {
			setTitle(prefs.getString("select_msisdn", getString(R.string.select_msisdn_click)));
		}

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("index", getSupportActionBar().getSelectedNavigationIndex());
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// TextView title = (TextView)
		// findViewById(R.id.actionbar_compat_title);
		// if(title != null) {
		// title.setOnTouchListener(new OnTouchListener() {
		// @Override
		// public boolean onTouch(View v, MotionEvent event) {
		// if(title_clicked)
		// return false;
		// if(!(prefs.contains("token") && prefs.contains("token_secret"))) {
		// getToken();
		// return false;
		// }
		// title_clicked = true;
		// AlertDialog.Builder msisdn_builder = new
		// AlertDialog.Builder(MainActivity.this);
		// msisdn_builder.setTitle(getResources().getString(R.string.select_msisdn));
		// DatabaseHelper helper = new DatabaseHelper(MainActivity.this);
		// final String[] msisdns = helper.msisdns.getMsisdnList();
		// helper.close();
		// msisdn_builder.setSingleChoiceItems(msisdns, -1, new
		// DialogInterface.OnClickListener() {
		//
		// public void onClick(DialogInterface dialog, int item) {
		// if(MVDataService.getDebug(getApplicationContext()))
		// Log.d("Selected: " + msisdns[item]);
		// SharedPreferences.Editor ed = prefs.edit();
		// ed.putString("select_msisdn", msisdns[item]);
		// if(ed.commit()) {
		// setTitle(prefs.getString("select_msisdn",
		// getString(R.string.select_msisdn_click)));
		// Intent i = new Intent(MainActivity.this, MVDataService.class);
		// i.setAction(MVDataService.UPDATE_CREDIT);
		// WakefulIntentService.sendWakefulWork(MainActivity.this, i);
		// }
		// title_clicked = false;
		// dialog.dismiss();
		// }
		// });
		//
		// msisdn_builder.setNegativeButton(getResources().getString(R.string.res_cancel),
		// new DialogInterface.OnClickListener() {
		// public void onClick(DialogInterface dialog, int id) {
		// title_clicked = false;
		// }
		// });
		// AlertDialog msisdn_alert = msisdn_builder.create();
		// msisdn_alert.show();
		// return true;
		// }
		// });
		// }
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mainReceiver);
	}

	@Override
	protected void onResume() {
		super.onResume();
		try {
			registerReceiver(mainReceiver, new IntentFilter(DISABLE_PROGRESS));
			registerReceiver(mainReceiver, new IntentFilter(ENABLE_PROGRESS));
			registerReceiver(mainReceiver, new IntentFilter(SHOW_MESSAGE));
			registerReceiver(mainReceiver, new IntentFilter(MVDataService.EXCEPTION));
		}
		catch(Exception e) {
			Log.w(e.getMessage());
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			unregisterReceiver(mainReceiver);
		}
		catch(Exception e) {
			Log.w(e.getMessage());
		}
		tracker.stopSession();
	}

	private void getToken() {
		Intent i = new Intent(this, AuthActivity.class);
		i.putExtra("consumer_key", CONSUMER_KEY);
		i.putExtra("consumer_secret", CONSUMER_SECRET);
		i.putExtra("access_token_url", ACCESS_TOKEN_URL);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(i);
	}

	protected void showAboutDialog() {
		tracker.trackPageView("/about");
		tracker.dispatch();
		Builder builder = new Builder(this);
		builder.setIcon(android.R.drawable.ic_dialog_info);
		builder.setTitle(getString(R.string.about));
		builder.setMessage(getAboutMessage());
		builder.setPositiveButton(getString(android.R.string.ok), null);
		AlertDialog dialog = builder.create();
		dialog.show();
		allowClickableLinks(dialog);
	}

	private CharSequence getAboutMessage() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(getString(R.string.app_name));
		stringBuilder.append(" ");
		stringBuilder.append(getVersionName());
		stringBuilder.append("\n\n");
		stringBuilder.append("Bug tracker:\n");
		stringBuilder.append("https://redmine.djzio.eu/projects/mvfa\n");
		stringBuilder.append("http://djzio.be/projects/mvfa\n");
		stringBuilder.append("zio@djzio.be\n@fastlorenzo");
		stringBuilder.append("\n\n");
		stringBuilder.append("Original application by:\n");
		stringBuilder.append("@benvandaele\n");

		SpannableStringBuilder message = new SpannableStringBuilder(stringBuilder.toString());
		Linkify.addLinks(message, Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS);

		int twitterStart = stringBuilder.toString().indexOf("@benvandaele");
		int twitterEnd = twitterStart + "@benvandaele".length();
		message.setSpan(new URLSpan("http://twitter.com/benvandaele"), twitterStart, twitterEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		int twitterStartz = stringBuilder.toString().indexOf("@fastlorenzo");
		int twitterEndz = twitterStartz + "@fastlorenzo".length();
		message.setSpan(new URLSpan("http://twitter.com/fastlorenzo"), twitterStartz, twitterEndz, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		return message;
	}

	private String getVersionName() {
		try {
			ComponentName componentName = new ComponentName(this, MainActivity.class);
			PackageInfo info = getPackageManager().getPackageInfo(componentName.getPackageName(), 0);
			return info.versionName;
		}
		catch(NameNotFoundException e) {
			// Won't happen, versionName is present in the manifest!
			return "";
		}
	}

	private void allowClickableLinks(AlertDialog dialog) {
		TextView message = (TextView) dialog.findViewById(android.R.id.message);
		message.setMovementMethod(LinkMovementMethod.getInstance());
	}

	protected void showMessage(String message) {
		Log.d("showMessage: " + message);
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}

	protected GoogleAnalyticsTracker getTracker() {
		return tracker;
	}

	protected int getTabSelected() {
		return mTabsAdapter.getPosition();
	}

}