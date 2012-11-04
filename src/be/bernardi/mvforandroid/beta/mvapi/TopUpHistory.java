package be.bernardi.mvforandroid.beta.mvapi;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import be.bernardi.mvforandroid.beta.Log;

public class TopUpHistory {

	private List<TopUp>	topups;

	public TopUpHistory(JSONArray jsonArr) throws JSONException {
		topups = new LinkedList<TopUp>();
		for(int i = 0; i < jsonArr.length(); i++) {
			topups.add(new TopUp(jsonArr.getJSONObject(i)));
		}
		Log.d(toString());
	}

	public List<TopUp> getTopups() {
		return topups;
	}

	@Override
	public String toString() {
		String out = "********************[TOPUP HISTORY]********************\n";
		for(TopUp item : topups) {
			out += item.toString();
		}
		return out;
	}

	public class TopUp {
		private double	amount;
		private double	amount_ex_vat;
		private Date	executed_on			= null;
		private String	method;
		private Date	payment_received_on	= null;
		private String	status;

		public TopUp(JSONObject jsonObj) {
			this.amount = jsonObj.optDouble("amount");
			this.amount_ex_vat = jsonObj.optDouble("amount_ex_vat");
			try {
				this.executed_on = MVAPIHelper.getDateFromAPI(jsonObj.optString("executed_on"));
			}
			catch(Exception e) {
			}
			this.method = jsonObj.optString("method");
			try {
				this.payment_received_on = MVAPIHelper.getDateFromAPI(jsonObj.optString("payment_received_on"));
			}
			catch(Exception e) {
			}
			this.status = jsonObj.optString("status");

		}

		public double getAmount() {
			return amount;
		}

		public double getAmount_ex_vat() {
			return amount_ex_vat;
		}

		public Date getExecuted_on() {
			return executed_on;
		}

		public String getMethod() {
			return method;
		}

		public Date getPayment_received_on() {
			return payment_received_on;
		}

		public String getStatus() {
			return status;
		}

		@Override
		public String toString() {
			String out = "";
			out += (executed_on != null) ? "** Executed on:\t\t" + executed_on.toLocaleString() + "\n" : "";
			out += "** Amount:\t\t\t" + amount + "€\n";
			out += "** Amount (eVAT):\t\t" + amount_ex_vat + "€\n";
			out += "** Method:\t\t\t" + method + "\n";
			out += "** Status:\t\t\t" + status + "\n";
			out += (payment_received_on != null) ? "** Payment received on:\t" + payment_received_on.toLocaleString() + "\n" : "";
			out += "*******************************************************\n";
			return out;
		}
	}

}
