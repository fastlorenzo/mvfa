/*
	Copyright (C) 2010 Ben Van Daele (vandaeleben@gmail.com)

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

package be.bernardi.mvforandroid.beta.activities;

import be.bernardi.mvforandroid.beta.Log;
import be.bernardi.mvforandroid.beta.R;
import be.bernardi.mvforandroid.beta.data.MVDataService;
import be.bernardi.mvforandroid.beta.exception.BadLoginException;
import be.bernardi.mvforandroid.beta.mvapi.XAuthHelper;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AuthActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTitle(getString(R.string.login_title));
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_window);
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		lp.copyFrom(getWindow().getAttributes());
		lp.width = WindowManager.LayoutParams.FILL_PARENT;
		getWindow().setAttributes(lp);

		final EditText username = (EditText) findViewById(R.id.username);
		final EditText password = (EditText) findViewById(R.id.password);
		final Button auth_btn = (Button) findViewById(R.id.auth_btn);

		Intent i = getIntent();

		final String consumer_key = i.getStringExtra("consumer_key");
		final String consumer_secret = i.getStringExtra("consumer_secret");
		final String access_token_url = i.getStringExtra("access_token_url");

		auth_btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {

				XAuthHelper authHelper = new XAuthHelper(consumer_key, consumer_secret, access_token_url);
				try {
					if(authHelper.performAuth(username.getText().toString(), password.getText().toString())) {
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AuthActivity.this);
						Editor ed = prefs.edit();
						String token = authHelper.getToken();
						String token_secret = authHelper.getToken_secret();
						ed.putString("token", token);
						ed.putString("token_secret", token_secret);
						ed.commit();
						Intent i = new Intent(AuthActivity.this, MVDataService.class);
						i.setAction(MVDataService.UPDATE_MSISDNS);
						WakefulIntentService.sendWakefulWork(AuthActivity.this, i);
						i.setAction(MVDataService.SCHEDULE_SERVICE);
						WakefulIntentService.sendWakefulWork(AuthActivity.this, i);
						sendBroadcast(new Intent(MVDataService.CREDIT_UPDATED));
						finish();
					}
					else {
						throw new Exception("Error getting access token");
					}
				}
				catch(BadLoginException e) {
					Toast.makeText(AuthActivity.this, getString(R.string.exception_badlogin_message), Toast.LENGTH_LONG).show();
				}
				catch(Exception e) {
					Log.w("Exception in Auth dialog", e);
				}
			}

		});
	}

}
