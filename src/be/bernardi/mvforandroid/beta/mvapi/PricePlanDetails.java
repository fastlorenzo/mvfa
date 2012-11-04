package be.bernardi.mvforandroid.beta.mvapi;

import java.util.HashMap;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PricePlanDetails {
	private static final int		VOICE_CALL	= 1;
	private static final int		DATA		= 2;
	private static final int		SMS			= 5;
	private static final int		MMS			= 7;
	private static final int		SMS_SON		= 15;

	private float					topUpAmount;
	private String					name;
	private HashMap<Integer, Type>	bundles;
	private HashMap<Integer, Type>	prices;

	public PricePlanDetails(String name) {
		this.name = name;
		this.bundles = new HashMap<Integer, Type>();
		this.prices = new HashMap<Integer, Type>();
	}

	public PricePlanDetails(String name, double topUpAmount) {
		this(name);
		this.topUpAmount = (float) topUpAmount;
	}

	public String getName() {
		return this.name;
	}

	public void setTopUpAmount(float topUpAmount) {
		this.topUpAmount = topUpAmount;
	}

	public float getTopUpAmount() {
		return this.topUpAmount;
	}

	public void addBundle(int typeId, String typeName, double amount) {
		if(!Double.isNaN(amount))
			bundles.put(typeId, new Type(typeId, typeName, amount));
	}

	public void addBundle(JSONObject jsonobj) throws JSONException {
		addBundle(jsonobj.getInt("type_id"), jsonobj.getString("type"), jsonobj.optDouble("amount"));
	}

	public void addBundles(JSONArray jsonarr) throws JSONException {
		for(int i = 0; i < jsonarr.length(); i++) {
			addBundle(jsonarr.getJSONObject(i));
		}
	}

	public void addPrices(int typeId, String typeName, double amount) {
		if(!Double.isNaN(amount))
			prices.put(typeId, new Type(typeId, typeName, amount));
	}

	public void addPrices(JSONObject jsonobj) throws JSONException {
		addPrices(jsonobj.getInt("type_id"), jsonobj.getString("type"), jsonobj.optDouble("amount"));
	}

	public void addPrices(JSONArray jsonarr) throws JSONException {
		for(int i = 0; i < jsonarr.length(); i++) {
			addPrices(jsonarr.getJSONObject(i));
		}
	}

	// Getters for bundles

	public Type getVoiceCallBundle() {
		return bundles.get(VOICE_CALL);
	}

	public Type getDataBundle() {
		return bundles.get(DATA);
	}

	public Type getSmsBundle() {
		return bundles.get(SMS);
	}

	public Type getMmsBundle() {
		return bundles.get(MMS);
	}

	public Type getSmsSonBundle() {
		return bundles.get(SMS_SON);
	}

	// Getters for prices

	public Type getVoiceCallPrice() {
		return prices.get(VOICE_CALL);
	}

	public Type getDataPrice() {
		return prices.get(DATA);
	}

	public Type getSmsPrice() {
		return prices.get(SMS);
	}

	public Type getMmsPrice() {
		return prices.get(MMS);
	}

	public Type getSmsSonPrice() {
		return prices.get(SMS_SON);
	}

	@Override
	public String toString() {
		String out = "******************[PRICE PLAN DETAIL]******************\n";
		out += "** Price plan: " + name + "\n";
		out += "** Topup amount:" + topUpAmount + "\n";
		out += "** [Bundles]:\n";
		Iterator<Integer> i = bundles.keySet().iterator();
		while(i.hasNext()) {
			out += "***\t" + bundles.get(i.next()).toString() + "\n";
		}
		out += "** [Prices]:\n";
		i = prices.keySet().iterator();
		while(i.hasNext()) {
			out += "***\t" + prices.get(i.next()).toString() + "\n";
		}
		out += "*******************************************************\n";
		return out;
	}

	public class Type {
		private int		type_id;
		private String	type;
		private Double	amount;

		public Type(int type_id, String type, Double amount) {
			super();
			this.type_id = type_id;
			this.type = type;
			this.amount = amount;
		}

		public int getType_id() {
			return type_id;
		}

		public String getType() {
			return type;
		}

		public Double getAmount() {
			return amount;
		}

		@Override
		public String toString() {
			return "[" + type_id + "] " + type + ": " + amount;
		}
	}
}
