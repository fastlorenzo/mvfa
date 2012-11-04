package be.bernardi.mvforandroid.beta.mvapi;

import java.util.Date;
import org.json.JSONObject;

import be.bernardi.mvforandroid.beta.Log;

public class SimBalance {
	private long	data;
	private boolean	is_expired;
	private double	credits;
	private Date	valid_until;
	private int		sms;
	private int		sms_son;
	private String	price_plan;
	private String	msisdn;

	public SimBalance(JSONObject jsonobj, String msisdn) {
		super();
		this.data = jsonobj.optLong("data");
		this.is_expired = jsonobj.optBoolean("is_expired");
		this.credits = jsonobj.optDouble("credits");
		try {
			this.valid_until = MVAPIHelper.getDateFromAPI(jsonobj.optString("valid_until"));
		}
		catch(Exception e) {
		}
		this.sms = jsonobj.optInt("sms");
		this.sms_son = jsonobj.optInt("sms_super_on_net");
		this.price_plan = jsonobj.optString("price_plan");
		this.msisdn = msisdn;
		Log.d(this.toString());
	}

	public long getData() {
		return data;
	}

	public boolean isExpired() {
		return is_expired;
	}

	public double getCredits() {
		if(Double.isNaN(credits))
			return 0;
		return credits;
	}

	public Date getValidUntil() {
		return valid_until;
	}

	public int getSms() {
		return sms;
	}

	public int getSmsSon() {
		return sms_son;
	}

	public String getPricePlan() {
		return price_plan;
	}

	public String getMsisdn() {
		return msisdn;
	}

	public String toString() {
		String out = "*********************[SIM BALANCE]*********************\n";
		out += "** MSISDN:\t" + msisdn + "\n";
		out += "** Price plan:\t" + price_plan + "\n";
		out += "** Data:\t\t" + data + "\n";
		out += (valid_until != null) ? "** Valid until:\t" + valid_until.toLocaleString() + "\n" : "";
		out += "** Expired:\t\t" + is_expired + "\n";
		out += "** Credits:\t\t" + credits + "\n";
		out += "** Sms:\t\t" + sms + "\n";
		out += "** Sms SON:\t\t" + sms_son + "\n";
		out += "*******************************************************\n";
		return out;
	}

}
