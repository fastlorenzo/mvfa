package be.bernardi.mvforandroid.beta.mvapi;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Usage {

	public static final int	TYPE_DATA	= 0;
	public static final int	TYPE_SMS	= 1;
	public static final int	TYPE_VOICE	= 2;
	public static final int	TYPE_MMS	= 3;

	private List<UsageItem>	usages;
	private String			msisdn;

	public Usage(JSONArray jsonArr, String msisdn) throws JSONException {
		usages = new LinkedList<UsageItem>();
		this.msisdn = msisdn;
		for(int i = 0; i < jsonArr.length(); i++) {
			usages.add(new UsageItem(jsonArr.getJSONObject(i)));
		}
	}

	public void addPage(JSONArray jsonArr) throws JSONException {
		for(int i = 0; i < jsonArr.length(); i++) {
			usages.add(new UsageItem(jsonArr.getJSONObject(i)));
		}
	}

	public String getMsisdn() {
		return msisdn;
	}

	public List<UsageItem> getUsages() {
		return this.usages;
	}

	@Override
	public String toString() {
		String out = "************************[USAGE]************************\n";
		out += "** MSISDN: " + msisdn + "\n";
		int i = 0;
		int more = 0;
		for(UsageItem item : usages) {
			if(i < 5)
				out += item.toString();
			else
				more++;
			i++;
		}
		if(more > 0)
			out += "5 first showed, " + more + " more\n";
		return out;
	}

	public class UsageItem {

		private String	from;
		private String	to;
		private boolean	is_incoming;
		private boolean	is_sms;
		private boolean	is_mms;
		private boolean	is_voice;
		private boolean	is_data;
		private double	balance;
		private double	price;
		private int		duration_call;
		private String	duration_human;
		private int		duration_connection;
		private Date	start_timestamp;
		private Date	end_timestamp;
		private int		type;

		public UsageItem(JSONObject jsonObj) {
			this.from = jsonObj.optString("from");
			this.to = jsonObj.optString("to");
			this.is_incoming = jsonObj.optBoolean("is_incoming");
			this.is_sms = jsonObj.optBoolean("is_sms");
			this.is_mms = jsonObj.optBoolean("is_mms");
			this.is_voice = jsonObj.optBoolean("is_voice");
			this.is_data = jsonObj.optBoolean("is_data");
			if(is_sms)
				this.type = TYPE_SMS;
			else if(is_mms)
				this.type = TYPE_MMS;
			else if(is_voice)
				this.type = TYPE_VOICE;
			else if(is_data)
				this.type = TYPE_DATA;
			this.balance = jsonObj.optDouble("balance");
			this.price = jsonObj.optDouble("price");
			this.duration_call = jsonObj.optInt("duration_call");
			this.duration_human = jsonObj.optString("duration_human");
			this.duration_connection = jsonObj.optInt("duration_connection");
			try {
				this.start_timestamp = MVAPIHelper.getDateFromAPI(jsonObj.optString("start_timestamp"));
			}
			catch(Exception e) {
			}
			try {
				this.end_timestamp = MVAPIHelper.getDateFromAPI(jsonObj.optString("end_timestamp"));
			}
			catch(Exception e) {
			}
		}

		public String getFrom() {
			return from;
		}

		public String getTo() {
			return to;
		}

		public Date getEnd_timestamp() {
			return end_timestamp;
		}

		public boolean isIncoming() {
			return is_incoming;
		}

		public boolean isSms() {
			return is_sms;
		}

		public boolean isMms() {
			return is_mms;
		}

		public boolean isVoice() {
			return is_voice;
		}

		public boolean isData() {
			return is_data;
		}

		public double getBalance() {
			return balance;
		}

		public double getPrice() {
			return price;
		}

		public int getDuration_call() {
			return duration_call;
		}

		public String getDuration_human() {
			return duration_human;
		}

		public double getDuration_connection() {
			return duration_connection;
		}

		public Date getStart_timestamp() {
			return start_timestamp;
		}

		public int getType() {
			return type;
		}

		private String typeToString(int type) {
			switch(type) {
				case TYPE_SMS:
					return "SMS";
				case TYPE_MMS:
					return "MMS";
				case TYPE_VOICE:
					return "VOICE";
				case TYPE_DATA:
					return "DATA";
				default:
					return "unknown";
			}
		}

		@Override
		public String toString() {
			String out = "";
			out += "** Type:\t\t\t" + typeToString(type) + "\n";
			out += "** From:\t\t\t" + from + "\n";
			out += "** To:\t\t\t" + to + "\n";
			out += "** Way :\t\t\t" + (is_incoming ? "incoming" : "outgoing") + "\n";
			out += "** Balance:\t\t\t" + balance + "\n";
			out += "** Price:\t\t\t" + price + "\n";
			out += "** Duration call:\t\t" + duration_call + "(human: " + duration_human + ")\n";
			out += "** Duration connection:\t" + duration_connection + "\n";
			out += (start_timestamp != null) ? "** Start:\t\t\t" + start_timestamp.toLocaleString() + "\n" : "";
			out += (end_timestamp != null) ? "** End:\t\t\t" + end_timestamp.toLocaleString() + "\n" : "";
			out += "*******************************************************\n";
			return out;
		}
	}
}
