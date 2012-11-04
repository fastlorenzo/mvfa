package be.bernardi.mvforandroid.beta.fragments;

import com.commonsware.cwac.sacklist.SackOfViewsAdapter;

import be.bernardi.mvforandroid.beta.Log;
import be.bernardi.mvforandroid.beta.R;
import be.bernardi.mvforandroid.beta.data.DatabaseHelper;
import be.bernardi.mvforandroid.beta.data.MVDataService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class PointsStatFragment extends ListFragment {

	private DatabaseHelper		helper;
	private PointsStatAdapter	adapter;
	// private Cursor model;

	private BroadcastReceiver	updatedReceiver	= new BroadcastReceiver() {
													@Override
													public void onReceive(Context context, Intent intent) {
														Log.d("PointsStatFragment", "Broadcast received");
														// model.requery();
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
		Log.d(getClass().getSimpleName(), "inflate");
		return inflater.inflate(R.layout.tab_pointsstat, container, false);
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
			adapter = new PointsStatAdapter();
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
			Log.w(e.getMessage(), e);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		try {
			getActivity().registerReceiver(updatedReceiver, new IntentFilter(MVDataService.POINTSSTAT_UPDATED));
		}
		catch(Exception e) {
			Log.w(e.getMessage(), e);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			getActivity().unregisterReceiver(updatedReceiver);
		}
		catch(Exception e) {
			Log.w(e.getMessage(),e);
		}
		helper.close();
	}

	class PointsStatAdapter extends SackOfViewsAdapter {

		private static final int	NUM_ROWS	= 1;
		private static final int	POINTS		= 0;

		// private static final int WAITING_POINTS = 1;
		// private static final int TOPUPS_USED = 2;

		public PointsStatAdapter() {
			super(NUM_ROWS);
		}

		@Override
		protected View newView(int position, ViewGroup parent) {
			switch(position) {
				case POINTS: {
					// int used_points = helper.pointsstat.getUsedPoints();
					int unused_points = helper.pointsstat.getUnusedPoints();
					int earned_points = helper.pointsstat.getEarnedPoints();
					View view = getActivity().getLayoutInflater().inflate(R.layout.pointstat_points, parent, false);
					TextView text = (TextView) view.findViewById(R.id.pointstat_points_text);
					text.setText(getString(R.string.remaining, unused_points + "/" + earned_points));

					float ratio = ((float) unused_points / earned_points);
					view.setBackgroundDrawable(getProgressBackground(ratio));
					return view;
				}

			}
			return null;
		}

		private BitmapDrawable getProgressBackground(double ratio) {
			// Setup bitmap and corresponding canvas
			int width = getActivity().getWindow().getWindowManager().getDefaultDisplay().getWidth();
			Bitmap result = Bitmap.createBitmap(width, 1, Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas();
			c.setBitmap(result);

			// Draw background
			c.drawColor(0xFFFFFFFF);

			// Draw progress rectangle
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setColor(0xFFF2EFE3);
			c.drawRect(0, 0, (float) (ratio * width), 1, paint);

			return new BitmapDrawable(result);
		}

	}

}