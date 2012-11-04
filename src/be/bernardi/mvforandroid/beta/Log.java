package be.bernardi.mvforandroid.beta;

public class Log {

	private static int		logLevel;
	private static boolean	enabled;
	private static String	defaultTag	= "notset";

	public static int		ASSERT		= android.util.Log.ASSERT;
	public static int		DEBUG		= android.util.Log.DEBUG;
	public static int		INFO		= android.util.Log.INFO;
	public static int		VERBOSE		= android.util.Log.VERBOSE;
	public static int		WARN		= android.util.Log.WARN;
	public static int		ERROR		= android.util.Log.ERROR;

	public static void init(int level) {
		logLevel = level;
	}

	public static void init(int level, boolean enabled) {
		Log.enabled = enabled;
		Log.init(level);
	}

	public static void setEnabled(boolean enabled) {
		Log.enabled = enabled;
	}

	public static boolean isEnabled() {
		return Log.enabled;
	}

	public static void setTag(String tag) {
		Log.defaultTag = tag;
	}

	public static String getTag() {
		return Log.defaultTag;
	}

	// DEBUG
	public static int d(String msg) {
		if(isLoggable(Log.defaultTag, Log.DEBUG)) {
			return android.util.Log.d(Log.defaultTag, msg);
		}
		return 0;
	}

	public static int d(String tag, String msg) {
		if(isLoggable(tag, Log.DEBUG)) {
			return android.util.Log.d(tag, msg);
		}
		return 0;
	}

	public static int d(String msg, Throwable tr) {
		if(isLoggable(Log.defaultTag, Log.DEBUG)) {
			return android.util.Log.d(Log.defaultTag, msg, tr);
		}
		return 0;
	}

	public static int d(String tag, String msg, Throwable tr) {
		if(isLoggable(tag, Log.DEBUG)) {
			return android.util.Log.d(tag, msg, tr);
		}
		return 0;
	}

	// INFO
	public static int i(String msg) {
		if(isLoggable(Log.defaultTag, Log.INFO)) {
			return android.util.Log.i(Log.defaultTag, msg);
		}
		return 0;
	}

	public static int i(String tag, String msg) {
		if(isLoggable(tag, Log.INFO)) {
			return android.util.Log.i(tag, msg);
		}
		return 0;
	}

	public static int i(String msg, Throwable tr) {
		if(isLoggable(Log.defaultTag, Log.INFO)) {
			return android.util.Log.i(Log.defaultTag, msg, tr);
		}
		return 0;
	}

	public static int i(String tag, String msg, Throwable tr) {
		if(isLoggable(tag, Log.INFO)) {
			return android.util.Log.i(tag, msg, tr);
		}
		return 0;
	}

	// VERBOSE
	public static int v(String msg) {
		if(isLoggable(Log.defaultTag, Log.VERBOSE)) {
			return android.util.Log.v(Log.defaultTag, msg);
		}
		return 0;
	}

	public static int v(String tag, String msg) {
		if(isLoggable(tag, Log.VERBOSE)) {
			return android.util.Log.v(tag, msg);
		}
		return 0;
	}

	public static int v(String msg, Throwable tr) {
		if(isLoggable(Log.defaultTag, Log.VERBOSE)) {
			return android.util.Log.v(Log.defaultTag, msg, tr);
		}
		return 0;
	}

	public static int v(String tag, String msg, Throwable tr) {
		if(isLoggable(tag, Log.VERBOSE)) {
			return android.util.Log.v(tag, msg, tr);
		}
		return 0;
	}

	// WARN
	public static int w(String msg) {
		if(isLoggable(Log.defaultTag, Log.WARN)) {
			return android.util.Log.w(Log.defaultTag, msg);
		}
		return 0;
	}

	public static int w(String tag, String msg) {
		if(isLoggable(tag, Log.WARN)) {
			return android.util.Log.w(tag, msg);
		}
		return 0;
	}

	public static int w(String msg, Throwable tr) {
		if(isLoggable(Log.defaultTag, Log.WARN)) {
			return android.util.Log.w(Log.defaultTag, msg, tr);
		}
		return 0;
	}

	public static int w(String tag, String msg, Throwable tr) {
		if(isLoggable(tag, Log.WARN)) {
			return android.util.Log.w(tag, msg, tr);
		}
		return 0;
	}

	// ERROR
	public static int e(String msg) {
		if(isLoggable(Log.defaultTag, Log.ERROR)) {
			return android.util.Log.e(Log.defaultTag, msg);
		}
		return 0;
	}

	public static int e(String tag, String msg) {
		if(isLoggable(tag, Log.ERROR)) {
			return android.util.Log.e(tag, msg);
		}
		return 0;
	}

	public static int e(String msg, Throwable tr) {
		if(isLoggable(Log.defaultTag, Log.ERROR)) {
			return android.util.Log.e(Log.defaultTag, msg, tr);
		}
		return 0;
	}

	public static int e(String tag, String msg, Throwable tr) {
		if(isLoggable(tag, Log.ERROR)) {
			return android.util.Log.e(tag, msg, tr);
		}
		return 0;
	}

	public static boolean isLoggable(String tag, int level) {
		if(!enabled && level != Log.ASSERT) {
			return false;
		}
		return level >= logLevel;
	}

	public static String getStackTraceString(Throwable tr) {
		return android.util.Log.getStackTraceString(tr);
	}

}
