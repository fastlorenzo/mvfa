package be.bernardi.mvforandroid.beta.fragments;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.commonsware.cwac.sacklist.SackOfViewsAdapter;

import be.bernardi.mvforandroid.beta.Log;
import be.bernardi.mvforandroid.beta.R;
import be.bernardi.mvforandroid.beta.data.DatabaseHelper;
import be.bernardi.mvforandroid.beta.data.MVDataService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

public class CreditFragment extends Fragment {

	private DatabaseHelper		helper;
	private SharedPreferences	prefs;

	private BroadcastReceiver	updatedReceiver	= new BroadcastReceiver() {
													@Override
													public void onReceive(Context context, Intent intent) {
														Log.d("CreditFragment: Broadcast received");
														updateView();
													}
												};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Load last state
		super.onCreate(savedInstanceState);
		helper = new DatabaseHelper(getActivity());
		prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.tab_credit, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		updateView();
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onPause() {
		// Save state
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
			getActivity().registerReceiver(updatedReceiver, new IntentFilter(MVDataService.CREDIT_UPDATED));
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

	private void updateView() {
		final double RATIO_THRESHOLD = 0.10;
		double ratio;
		int warning_color = 0xffa51d1d;

		// Credit
		double credit_max = prefs.getFloat(MVDataService.PRICE_PLAN_TOPUP_AMOUNT, 15);
		double credit_remain = helper.credit.getRemainingCredit();
		TextView credit_text = (TextView) getActivity().findViewById(R.id.newcredit_credit_text);
		credit_text.setText(getString(R.string.remaining, formatCurrency(credit_remain)));
		ratio = (credit_remain / credit_max);
		credit_text.measure(0, 0);
		credit_text.setBackgroundDrawable(getProgressBackground(ratio, credit_text.getMeasuredWidth(), credit_text.getMeasuredHeight()));
		if(ratio < RATIO_THRESHOLD)
			credit_text.setTextColor(warning_color);

		// SMS
		int sms_remain = helper.credit.getRemainingSms();
		int sms_max = prefs.getInt(MVDataService.PRICE_PLAN_SMS_AMOUNT, 1000);
		TextView sms_text = (TextView) getActivity().findViewById(R.id.newcredit_sms_text);
		sms_text.setText(getString(R.string.sms_remaining, sms_remain));
		ratio = ((float) sms_remain / sms_max);
		sms_text.measure(0, 0);
		sms_text.setBackgroundDrawable(getProgressBackground(ratio, sms_text.getMeasuredWidth(), sms_text.getMeasuredHeight()));
		if(ratio < RATIO_THRESHOLD)
			sms_text.setTextColor(warning_color);

		// SMS SON
		int smsson_remain = helper.credit.getRemainingSmsSuperOnNet();
		int smsson_max = prefs.getInt(MVDataService.PRICE_PLAN_SMS_AMOUNT, 1000);
		TextView smsson_text = (TextView) getActivity().findViewById(R.id.newcredit_smsson_text);
		smsson_text.setText(getString(R.string.sms_son_remaining, smsson_remain));
		ratio = ((float) smsson_remain / smsson_max);
		smsson_text.measure(0, 0);
		smsson_text.setBackgroundDrawable(getProgressBackground(ratio, smsson_text.getMeasuredWidth(), smsson_text.getMeasuredHeight()));
		if(ratio < RATIO_THRESHOLD)
			smsson_text.setTextColor(warning_color);

		// DATA
		long data_remain = helper.credit.getRemainingData();
		long data_max = prefs.getInt(MVDataService.PRICE_PLAN_DATA_AMOUNT, 2048);
		TextView data_text = (TextView) getActivity().findViewById(R.id.newcredit_data_text);
		data_text.setText(getString(R.string.megabytes_remaining, (data_remain / 1048576)));
		ratio = ((double) (data_remain / 1048576) / data_max);
		data_text.measure(0, 0);
		data_text.setBackgroundDrawable(getProgressBackground(ratio, data_text.getMeasuredWidth(), data_text.getMeasuredHeight()));
		if(ratio < RATIO_THRESHOLD)
			data_text.setTextColor(warning_color);

		// EXPIRATION
		TextView expiration_text = (TextView) getActivity().findViewById(R.id.newcredit_expiration_text);
		String text = getString(R.string.valid_until, formatValidUntilDate(helper.credit.getValidUntil()));

		long remainingTime = (helper.credit.getValidUntil() / 1000) - (System.currentTimeMillis() / 1000);
		long oneMonthInMillis = 30 * 24 * 3600;
		ratio = (double) remainingTime / oneMonthInMillis;

		if(ratio < RATIO_THRESHOLD)
			expiration_text.setTextColor(warning_color);

		String planName = prefs.getString(MVDataService.PRICE_PLAN_NAME, null);

		if(planName != null)
			text += "\n" + getString(R.string.price_plan, planName);

		expiration_text.setText(text);

		// planText.setText(getString(R.string.price_plan, planName));

		
	}

	public String formatValidUntilDate(long validUntil) {
		SimpleDateFormat validUntilFormat = new SimpleDateFormat("dd-MM-yyyy '" + getString(R.string.at_hour) + "' HH:mm");
		return validUntilFormat.format(new Date(validUntil));
	}

	private String formatCurrency(double credit_remain) {
		DecimalFormat currencyFormat = new DecimalFormat("#.##");
		return currencyFormat.format(credit_remain) + "€";

	}

	private BitmapDrawable getProgressBackground(double ratio, int width, int height) {
		Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(result);

		final Paint paint = new Paint();
		final RectF rectF = new RectF(0, 0, width, height);
		final float roundPx = 5;
		paint.setAntiAlias(true);
		// c.drawARGB(0, 0, 0, 0);
		paint.setColor(0xFFCCCCCC);
		c.drawRoundRect(rectF, roundPx, roundPx, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		paint.setColor(0xFFF2EFE3);
		c.drawRect(new RectF(0, 0, (float) (ratio * width), height), paint);
		return new BitmapDrawable(result);
	}

}
