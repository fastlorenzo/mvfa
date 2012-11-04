/*
	Copyright (C) 2011 Lorenzo Bernardi (fastlorenzo@gmail.com)

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

package be.bernardi.mvforandroid.exception;

import be.bernardi.mvforandroid.data.MVDataService;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class ExceptionReceiver extends BroadcastReceiver {

	private Activity act;
	
	public ExceptionReceiver(Activity act) {
		this.act = act;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.e(MVDataService.class.getSimpleName(), "Exception:"+intent.getStringExtra("message"));
		Toast.makeText(context, intent.getStringExtra("message"), Toast.LENGTH_LONG).show();
		act.getParent().setProgressBarIndeterminateVisibility(false);

	}

}
