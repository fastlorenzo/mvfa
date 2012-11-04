package be.bernardi.mvforandroid.beta.fragments;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import be.bernardi.mvforandroid.beta.Log;
import be.bernardi.mvforandroid.beta.R;
import be.bernardi.mvforandroid.beta.data.DatabaseHelper;
import be.bernardi.mvforandroid.beta.data.MVDataService;
import be.bernardi.mvforandroid.beta.data.DatabaseHelper.UsageDB;
import be.bernardi.mvforandroid.beta.exception.NoMsisdnException;
import be.bernardi.mvforandroid.beta.mvapi.Usage;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class UsageFragment extends ListFragment {

	private DatabaseHelper		helper;
	private UsageAdapter		adapter;
	private Cursor				model;
	private SharedPreferences	prefs;

	private BroadcastReceiver	updatedReceiver	= new BroadcastReceiver() {
													@Override
													public void onReceive(Context context, Intent intent) {
														Log.d("UsageFragment: Broadcast received");
														model = helper.usage.get(UsageDB.ORDER_BY_DATE, false, prefs.getString("select_msisdn", ""));
														adapter = new UsageAdapter(model);
														setListAdapter(adapter);
													}
												};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		helper = new DatabaseHelper(getActivity());
		prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.tab_usage, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		registerForContextMenu(getListView());
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if(!prefs.contains("select_msisdn")) {
			Intent exceptionBroadcast = new Intent(MVDataService.EXCEPTION);
			exceptionBroadcast.putExtra(MVDataService.EXCEPTION, new NoMsisdnException());
			exceptionBroadcast.putExtra("message", getString(R.string.exception_msisdn_message));
			getActivity().sendBroadcast(exceptionBroadcast);
		}
		if(adapter == null) {
			model = helper.usage.get(UsageDB.ORDER_BY_DATE, false, prefs.getString("select_msisdn", ""));
			adapter = new UsageAdapter(model);
			setListAdapter(adapter);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		try {
			getActivity().unregisterReceiver(updatedReceiver);
		}
		catch(Exception e) {
			Log.e(getClass().getSimpleName() + ": " + e.getMessage(), e);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		try {
			getActivity().registerReceiver(updatedReceiver, new IntentFilter(MVDataService.USAGE_UPDATED));
		}
		catch(Exception e) {
			Log.e(getClass().getSimpleName() + ": " + e.getMessage(), e);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			getActivity().unregisterReceiver(updatedReceiver);
		}
		catch(Exception e) {
			Log.e(getClass().getSimpleName() + ": " + e.getMessage(), e);
		}
		helper.close();
	}

	// @Override
	// public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo
	// menuInfo) {
	// MenuInflater inflater = getActivity().getMenuInflater();
	// inflater.inflate(R.menu.menu, menu);
	// }

	@Override
	public void onListItemClick(ListView lv, View v, int position, long id) {
		Cursor c = helper.usage.get(id);
		if(c.moveToFirst()) {
			if(helper.usage.getType(c) == Usage.TYPE_DATA)
				return;

			String contact = helper.usage.getContact(c);
			String contactId = getContactIdFromNumber(getActivity(), contact);
			if(!contactId.equals("")) {
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				intent.setData(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, Uri.encode(contactId)));
				startActivity(intent);
			}
			else {
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_DIAL);
				intent.setData(Uri.parse("tel:" + contact));
				startActivity(intent);
			}
		}
		c.close();
	}

	private static String getContactIdFromNumber(Context context, String number) {
		// define the columns I want the query to return
		String[] projection = new String[] { ContactsContract.Contacts._ID };

		// encode the phone number and build the filter URI
		Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(getContactNameFromNumber(context, number)));

		// query time
		Cursor c = context.getContentResolver().query(contactUri, projection, null, null, null);

		// if the query returns 1 or more results
		// return the first result
		if(c.moveToFirst()) {
			String id = c.getString(c.getColumnIndex(ContactsContract.Contacts._ID));
			return id;
		}

		// return empty string if not found
		return "";
	}

	public static String getContactNameFromNumber(Context context, String number) {
		// define the columns I want the query to return
		String[] projection = new String[] { ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.PhoneLookup.NUMBER };

		// encode the phone number and build the filter URI
		Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

		// query time
		Cursor c = context.getContentResolver().query(contactUri, projection, null, null, null);

		// if the query returns 1 or more results
		// return the first result
		if(c.moveToFirst()) {
			String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
			return name;
		}

		// return the original number if no match was found
		return number;
	}

	class UsageAdapter extends CursorAdapter {

		UsageAdapter(Cursor c) {
			super(getActivity(), c);
		}

		@Override
		public void bindView(View row, Context ctxt, Cursor c) {
			UsageHolder holder = (UsageHolder) row.getTag();
			holder.populateFrom(c, helper);
		}

		@Override
		public View newView(Context ctxt, Cursor c, ViewGroup parent) {
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View listItem = inflater.inflate(R.layout.usage_list_item, parent, false);
			UsageHolder holder = new UsageHolder(listItem);
			listItem.setTag(holder);
			return listItem;
		}

	}

	static class UsageHolder {
		private ImageView				logo			= null;
		private TextView				title			= null, cost = null, date = null, duration = null;
		private static DecimalFormat	currencyFormat	= new DecimalFormat("#.##");
		private static SimpleDateFormat	formatTime		= new SimpleDateFormat("dd/MM/yyyy HH:mm");
		private static DecimalFormat	dataFormat		= new DecimalFormat("#.##");

		private static String formatCurrency(double amount) {
			return currencyFormat.format(amount) + "€";

		}

		private static String formatTime(long timestamp) {
			return formatTime.format(new Date(timestamp));
		}

		private static String formatBytes(Context c, long bytes) {
			if(bytes < 1048576) {
				return c.getString(R.string.kilobytes, dataFormat.format((double) bytes / 1024));
			}
			else {
				return c.getString(R.string.megabytes, dataFormat.format((double) bytes / 1048576));
			}
		}

		private static String formatDuration(long duration) {
			int hours = (int) (duration / 3600);
			int minutes = (int) ((duration / 60) - (hours * 60));
			int seconds = (int) (duration % 60);
			String result = "";
			if(hours != 0)
				result += String.format("%02d:", hours);
			result += String.format("%02d:%02d", minutes, seconds);
			return result;
		}

		UsageHolder(View listItem) {
			logo = (ImageView) listItem.findViewById(R.id.usage_logo);
			title = (TextView) listItem.findViewById(R.id.usage_title);
			cost = (TextView) listItem.findViewById(R.id.usage_cost);
			date = (TextView) listItem.findViewById(R.id.usage_date);
			duration = (TextView) listItem.findViewById(R.id.usage_duration);
		}

		void populateFrom(Cursor c, DatabaseHelper helper) {
			cost.setText(formatCurrency(helper.usage.getCost(c)));
			date.setText(formatTime(helper.usage.getTimestamp(c)));
			duration.setText("xx");

			switch(helper.usage.getType(c)) {
				case Usage.TYPE_DATA: {
					logo.setImageResource(R.drawable.credit_data);
					title.setText("Data");
					duration.setText(formatBytes(duration.getContext(), helper.usage.getduration(c)));
					break;
				}
				case Usage.TYPE_MMS: {
					logo.setImageResource(R.drawable.credit_sms);
					title.setText(getContactNameFromNumber(title.getContext(), helper.usage.getContact(c)));
					duration.setText("");
					break;
				}
				case Usage.TYPE_SMS: {
					if(helper.usage.isIncoming(c))
						logo.setImageResource(R.drawable.sms_incoming);
					else
						logo.setImageResource(R.drawable.sms_outgoing);
					title.setText(getContactNameFromNumber(title.getContext(), helper.usage.getContact(c)));
					duration.setText("");
					break;
				}
				case Usage.TYPE_VOICE: {
					if(helper.usage.isIncoming(c))
						logo.setImageResource(R.drawable.call_incoming);
					else
						logo.setImageResource(R.drawable.call_outgoing);
					title.setText(getContactNameFromNumber(title.getContext(), helper.usage.getContact(c)));
					duration.setText(formatDuration(helper.usage.getduration(c)));
					break;
				}
			}
		}
	}

}
