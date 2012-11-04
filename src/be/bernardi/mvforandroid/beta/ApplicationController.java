package be.bernardi.mvforandroid.beta;

import org.acra.annotation.ReportsCrashes;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import android.app.Application;

@ReportsCrashes(formKey = "dF9tRjVCczlJeHhGbkRHS3pVNE9FVlE6MQ", mode = ReportingInteractionMode.SILENT, logcatArguments = { "-t", "100", "-v", "time",
		"ActivityManager:I", "MVFA:D", "*:W" }
// sharedPreferencesName = HoneybuzzApplication.PREFERENCES_NAME, // Name of the
// SharedPreferences that will host the acra.enable or acra.disable preference.
// sharedPreferencesMode = HoneybuzzApplication.PREFERENCES_MODE // The mode
// that you need for the SharedPreference file creation: Context.MODE_PRIVATE,
// Context.MODE_WORLD_READABLE or Context.MODE_WORLD_WRITEABLE.
)
public class ApplicationController extends Application {

	private boolean	DEBUG	= false;
	private String	TAG		= "MVFA";

	@Override
	public void onCreate() {
		ACRA.init(this); // error reporting
		Log.init(Log.DEBUG, true);
		Log.setTag(TAG);
		super.onCreate();
	}

	public void setDebug(boolean debug) {
		this.DEBUG = debug;
	}

	public boolean getDebug() {
		return DEBUG;
	}
}
