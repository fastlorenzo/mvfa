package be.bernardi.mvforandroid.beta.fragments;

import java.text.SimpleDateFormat;
import java.util.Date;

import be.bernardi.mvforandroid.beta.Log;
import be.bernardi.mvforandroid.beta.R;
import be.bernardi.mvforandroid.beta.data.DatabaseHelper;
import be.bernardi.mvforandroid.beta.data.MVDataService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class TopupsFragment extends ListFragment {

	private DatabaseHelper		helper;
	private TopupsAdapter		adapter;
	private Cursor				model;
	public static final String	TOPUP_PHONE_NUMBER	= "8989";

	private BroadcastReceiver	updatedReceiver		= new BroadcastReceiver() {
														@Override
														public void onReceive(Context context, Intent intent) {
															Log.d("TopupsFragment: Broadcast received");
															model.requery();
														}
													};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		helper = new DatabaseHelper(getActivity());
		this.setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.tab_topups, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		registerForContextMenu(getListView());
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if(adapter == null) {
			model = helper.topups.getAll();
			adapter = new TopupsAdapter(model);
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
			getActivity().registerReceiver(updatedReceiver, new IntentFilter(MVDataService.TOPUPS_UPDATED));
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
//
//	@Override
//	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//		getActivity().getMenuInflater().inflate(R.menu.topups, menu);
//	}

	class TopupsAdapter extends CursorAdapter {

		public TopupsAdapter(Cursor c) {
			super(getActivity(), c);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TopupsHolder holder = (TopupsHolder) view.getTag();
			holder.populateFrom(cursor, helper);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View listItem = inflater.inflate(R.layout.topups_list_item, parent, false);
			TopupsHolder holder = new TopupsHolder(listItem);
			listItem.setTag(holder);
			return listItem;
		}

		@Override
		public boolean isEnabled(int position) {
			return false;
		}

	}

	static class TopupsHolder {
		private TextView				amount		= null, method = null, date = null;
		private static SimpleDateFormat	formatDate	= new SimpleDateFormat("dd/MM/yyyy");

		TopupsHolder(View listItem) {
			amount = (TextView) listItem.findViewById(R.id.topup_amount);
			method = (TextView) listItem.findViewById(R.id.topup_method);
			date = (TextView) listItem.findViewById(R.id.topup_date);
		}

		public void populateFrom(Cursor c, DatabaseHelper helper) {
			amount.setText((int) helper.topups.getAmount(c) + "€");
			method.setText(helper.topups.getMethod(c));
			date.setText(formatDate.format(new Date(helper.topups.getExecutedOn(c))));
		}
	}

}