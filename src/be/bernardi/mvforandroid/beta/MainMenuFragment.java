package be.bernardi.mvforandroid.beta;

import java.util.Calendar;
import java.util.GregorianCalendar;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import be.bernardi.mvforandroid.beta.activities.SettingsActivity;
import be.bernardi.mvforandroid.beta.data.MVDataService;
import be.bernardi.mvforandroid.beta.fragments.TopupsFragment;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.telephony.SmsManager;
import android.view.MenuInflater;
import android.view.View;
import android.widget.DatePicker;

public class MainMenuFragment extends Fragment {

	private static MainActivity	mainActivity;

	// private static Menu menu;

	public MainMenuFragment() {
	}

	public MainMenuFragment(MainActivity main) {
		mainActivity = main;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menuS, MenuInflater inflater) {
		inflater.inflate(R.menu.main, menuS);
		// menu = menuS;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if(mainActivity != null) {
			final GoogleAnalyticsTracker tracker = mainActivity.getTracker();
			Log.d("onOptionsItemSelected");
			switch(item.getItemId()) {
				case R.id.settings:
					Intent intent = new Intent(getActivity(), SettingsActivity.class);
					startActivity(intent);
					return true;
				case R.id.about:
					mainActivity.showAboutDialog();
					return true;
				case R.id.update_msisdns:
					tracker.trackEvent("Update", // Category
							"Click", // Action
							"MSISDNS", // Label
							0); // Value
					Intent update = new Intent(getActivity(), MVDataService.class);
					update.setAction(MVDataService.UPDATE_MSISDNS);
					WakefulIntentService.sendWakefulWork(getActivity(), update);
					Log.d("Send: UPDATE_MSISDNS");
					return true;
				case R.id.update:
					Intent i = new Intent(getActivity(), MVDataService.class);
					switch(mainActivity.getTabSelected()) {
						case 0:
							tracker.trackEvent("Update", // Category
									"Click", // Action
									"Credit", // Label
									0); // Value
							i.setAction(MVDataService.UPDATE_CREDIT);
							WakefulIntentService.sendWakefulWork(getActivity(), i);
							break;
						case 1:
							tracker.trackEvent("Update", // Category
									"Click", // Action
									"Usage", // Label
									0); // Value
							OnDateSetListener datePicked = new OnDateSetListener() {
								@Override
								public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
									Calendar lower = GregorianCalendar.getInstance();
									lower.set(Calendar.YEAR, year);
									lower.set(Calendar.MONTH, monthOfYear);
									lower.set(Calendar.DAY_OF_MONTH, dayOfMonth);
									lower.set(Calendar.HOUR_OF_DAY, 0);
									lower.set(Calendar.MINUTE, 0);
									lower.set(Calendar.SECOND, 0);

									Calendar upper = (Calendar) lower.clone();
									upper.add(Calendar.DAY_OF_MONTH, 1);

									Intent i = new Intent(getActivity(), MVDataService.class);
									i.setAction(MVDataService.UPDATE_USAGE);
									i.putExtra(MVDataService.UPDATE_USAGE_STARTTIME, lower.getTimeInMillis());
									i.putExtra(MVDataService.UPDATE_USAGE_ENDTIME, upper.getTimeInMillis());
									WakefulIntentService.sendWakefulWork(getActivity(), i);
								}
							};
							Calendar c = GregorianCalendar.getInstance();
							DatePickerDialog datePicker = new DatePickerDialog(getActivity(), datePicked, c.get(Calendar.YEAR), c.get(Calendar.MONTH),
									c.get(Calendar.DAY_OF_MONTH));
							datePicker.show();
							break;
						case 2:
							tracker.trackEvent("Update", // Category
									"Click", // Action
									"Topups", // Label
									0); // Value
							i.setAction(MVDataService.UPDATE_TOPUPS);
							WakefulIntentService.sendWakefulWork(getActivity(), i);
							break;
						case 3:
							tracker.trackEvent("Update", // Category
									"Click", // Action
									"Points statistics", // Label
									0); // Value
							i.setAction(MVDataService.UPDATE_POINTSSTAT);
							WakefulIntentService.sendWakefulWork(getActivity(), i);
							break;

					}
					tracker.dispatch();
					return true;
				case R.id.topup_button:
					tracker.trackEvent("Topup", // Category
							"Click", // Action
							"Button", // Label
							0); // Value
					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					builder.setTitle(getResources().getString(R.string.choose_amount));
					builder.setSingleChoiceItems(R.array.topup, -1, new OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							final String selected = getResources().getStringArray(R.array.topup_values)[item];
							AlertDialog.Builder builder2 = new AlertDialog.Builder(getActivity());
							builder2.setMessage(getResources().getString(R.string.confirm_topup, selected)).setCancelable(true)
									.setPositiveButton(getResources().getString(R.string.res_positive), new OnClickListener() {
										public void onClick(DialogInterface dialog, int id) {
											// Send message
											SmsManager sms = SmsManager.getDefault();
											try {
												sms.sendTextMessage(TopupsFragment.TOPUP_PHONE_NUMBER, null, "SIM TOPUP " + selected, null, null);
												Intent resultBroadcast = new Intent(MainActivity.SHOW_MESSAGE);
												resultBroadcast.putExtra("message", getString(R.string.topup_sent));
												getActivity().sendBroadcast(resultBroadcast);
												tracker.trackEvent("Topup", // Category
														"Click", // Action
														"Done", // Label
														Integer.parseInt(selected)); // Value
											}
											catch(Exception e) {
												Intent resultBroadcast = new Intent(MainActivity.SHOW_MESSAGE);
												resultBroadcast.putExtra("message", getString(R.string.error_sms));
												getActivity().sendBroadcast(resultBroadcast);
											}
											dialog.dismiss();
										}
									}).setNegativeButton(getResources().getString(R.string.res_negative), new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int id) {
										}
									});
							AlertDialog alert2 = builder2.create();
							alert2.show();
							dialog.dismiss();
						}
					});

					builder.setNegativeButton(getResources().getString(R.string.res_cancel), new OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
						}
					});
					AlertDialog alert = builder.create();
					alert.show();
				default:
					return false;

			}
		}
		return false;
	}

	public static void setRefreshActionItemState(boolean refreshing) {
		View refreshButton = mainActivity.findViewById(R.id.update);
		if(refreshButton != null) {
			// refreshButton.setVisibility(refreshing ? View.GONE :
			// View.VISIBLE);
			refreshButton.setEnabled(!refreshing);
		}
	}
}
