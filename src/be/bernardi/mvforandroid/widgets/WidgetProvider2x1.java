/*
	Copyright (C) 2011 Lorenzo Bernardi (fastlorenzo@gmail.com)
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

package be.bernardi.mvforandroid.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import be.bernardi.mvforandroid.MainActivity;
import be.bernardi.mvforandroid.R;
import be.bernardi.mvforandroid.SettingsActivity;
import be.bernardi.mvforandroid.data.DatabaseHelper;
import be.bernardi.mvforandroid.data.MVDataService;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class WidgetProvider2x1 extends AppWidgetProvider {

	public static final String	ON_WIDGET_CLICK	= "be.bernardi.mvforandroid.onWidgetClick";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		int amount = appWidgetIds.length;

		for(int i = 0; i < amount; i++) {
			int appWidgetId = appWidgetIds[i];

			appWidgetManager.updateAppWidget(appWidgetId, getViews(context));
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		if(intent.getAction().equals(ON_WIDGET_CLICK)) {
			String widgetAction = getWidgetActionPreference(context);
			if(widgetAction.equals(SettingsActivity.OPEN_APP))
				openApp(context);
			else if(widgetAction.equals(SettingsActivity.UPDATE_DATA))
				updateData(context);
		}
		else if(intent.getAction().equals(MVDataService.CREDIT_UPDATED)) {
			// Some magic to obtain a reference to the AppWidgetManager
			ComponentName thisWidget = new ComponentName(context, WidgetProvider2x1.class);
			AppWidgetManager manager = AppWidgetManager.getInstance(context);

			manager.updateAppWidget(thisWidget, getViews(context));

		}
	}

	private void openApp(Context context) {
		Intent intent = new Intent(context, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	private void updateData(Context context) {
		Intent i = new Intent(context, MVDataService.class);
		i.setAction(MVDataService.UPDATE_CREDIT);
		WakefulIntentService.sendWakefulWork(context, i);
	}

	private String getWidgetActionPreference(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString("widget_action", SettingsActivity.UPDATE_DATA);
	}

	private RemoteViews getViews(Context context) {
		RemoteViews views = null;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		int bg_color = Integer.decode(prefs.getString("widget_background", "0"));
		int layout = 0;
		switch(bg_color) {
			case SettingsActivity.WIDGET_BG_BLACK:
				layout = R.layout.widget2x1;
				break;
			case SettingsActivity.WIDGET_BG_TRANSPARENT:
				layout = R.layout.widget2x1_transparent;
				break;
			default:
				layout = R.layout.widget2x1;
		}
		views = new RemoteViews(context.getPackageName(), layout);

		Intent intent = new Intent(ON_WIDGET_CLICK);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
		views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

		updateViewContent(context, views);
		return views;
	}

	public void updateViewContent(Context context, RemoteViews views) {
		DatabaseHelper helper = new DatabaseHelper(context);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		views.setTextViewText(R.id.msisdn_text, prefs.getString("select_msisdn", "none"));
		views.setTextViewText(R.id.credit_text, helper.credit.getRemainingCredit() + "");
		views.setTextViewText(R.id.sms_text, helper.credit.getRemainingSms() + " SMS");
		views.setTextViewText(R.id.sms_mv_text, helper.credit.getRemainingSmsSuperOnNet() + " SMS");
		views.setTextViewText(R.id.data_text, (helper.credit.getRemainingData() / (1024 * 1024)) + " MB");

		helper.close();
	}
}
