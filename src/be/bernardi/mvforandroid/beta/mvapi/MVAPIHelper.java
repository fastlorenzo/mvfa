package be.bernardi.mvforandroid.beta.mvapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import be.bernardi.mvforandroid.beta.Log;
import be.bernardi.mvforandroid.beta.data.MVDataService;
import be.bernardi.mvforandroid.beta.exception.BadLoginException;

/**
 * Helper class to get API result
 * 
 * @author zio
 * 
 */
public class MVAPIHelper {

	private static final String		CONSUMER_KEY			= "VhbdZYB3kC2C6phf4E";
	private static final String		CONSUMER_SECRET			= "wM98njrvyqRckSvTHadcRxUyUBDnuddx";
	private static final String		BASEURL					= "https://mobilevikings.com/api/2.0/oauth/";
	// private static final String ACCESS_TOKEN_URL = BASEURL + "access_token/";

	private static final String		MSISDN_URL				= BASEURL + "msisdn_list.json";
	private static final String		PRICE_PLAN_DETAILS_URL	= BASEURL + "price_plan_details.json";
	private static final String		SIM_BALANCE_URL			= BASEURL + "sim_balance.json";
	private static final String		TOPUP_HISTORY_URL		= BASEURL + "top_up_history.json";
	private static final String		USAGE_URL				= BASEURL + "usage.json";
	private static final String		VIKING_POINTS_STATS_URL	= BASEURL + "points/stats.json";

	private static SimpleDateFormat	apiFormat				= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private OAuthConsumer			consumer;
	private String					msisdn;

	public MVAPIHelper(String token, String token_secret) throws Exception {
		if(token.equals("") || token_secret.equals("")) {
			throw new Exception("Auth info not set");
		}
		consumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
		consumer.setTokenWithSecret(token, token_secret);
	}

	public MVAPIHelper(String token, String token_secret, String msisdn) throws Exception {
		this(token, token_secret);
		this.msisdn = msisdn;
	}

	/**
	 * Gets the MSISDN list for the account.
	 * 
	 * @param alias
	 *            Include SIM alias in response or not
	 * @return JSONArray
	 * @throws IOException
	 * @throws BadLoginException
	 * @throws JSONException
	 * @throws OAuthCommunicationException
	 * @throws OAuthExpectationFailedException
	 * @throws OAuthMessageSignerException
	 * @throws Exception
	 */
	public JSONArray getMsisdnList(boolean alias) throws IOException, OAuthMessageSignerException, OAuthExpectationFailedException,
			OAuthCommunicationException, JSONException, BadLoginException {
		return new JSONArray(alias ? getResponse(MSISDN_URL + "?alias=1") : getResponse(MSISDN_URL));
	}

	public JSONArray getMsisdnList() throws IOException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException,
			JSONException, BadLoginException {
		return this.getMsisdnList(false);
	}

	/**
	 * Gets the price plan details for the given msisdn
	 * 
	 * @param msisdn
	 * @return PricePlanDetails
	 * @throws JSONException
	 * @throws IOException
	 * @throws OAuthCommunicationException
	 * @throws OAuthExpectationFailedException
	 * @throws OAuthMessageSignerException
	 * @throws BadLoginException
	 * @throws Exception
	 */
	public PricePlanDetails getPricePlanDetails(String msisdn) throws JSONException, IOException, OAuthMessageSignerException, OAuthExpectationFailedException,
			OAuthCommunicationException, BadLoginException {
		JSONObject jsonobj = new JSONObject(getResponse(PRICE_PLAN_DETAILS_URL + "?msisdn=" + msisdn));
		PricePlanDetails ppd = new PricePlanDetails(jsonobj.getString("name"), jsonobj.getDouble("top_up_amount"));
		ppd.addBundles(jsonobj.getJSONArray("bundles"));
		ppd.addPrices(jsonobj.getJSONArray("prices"));
		Log.d(ppd.toString());
		return ppd;
	}

	public PricePlanDetails getPricePlanDetails() throws IOException, Exception {
		return getPricePlanDetails(msisdn);
	}

	/**
	 * Gets the sim balance of the given MSISDN
	 * 
	 * @param msisdn
	 * @return SimBalance
	 * @throws IOException
	 * @throws JSONException
	 * @throws OAuthCommunicationException
	 * @throws OAuthExpectationFailedException
	 * @throws OAuthMessageSignerException
	 * @throws BadLoginException
	 * @throws Exception
	 */
	public SimBalance getSimBalance(String msisdn) throws IOException, OAuthMessageSignerException, OAuthExpectationFailedException,
			OAuthCommunicationException, JSONException, BadLoginException {
		return new SimBalance(new JSONObject(getResponse(SIM_BALANCE_URL + "?msisdn=" + msisdn)), msisdn);
	}

	public SimBalance getSimBalance() throws IOException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException,
			JSONException, BadLoginException {
		return getSimBalance(msisdn);
	}

	public SimBalance getSimBalance(String msisdn, boolean addPricePlan) throws OAuthMessageSignerException, OAuthExpectationFailedException,
			OAuthCommunicationException, JSONException, IOException, BadLoginException {
		return addPricePlan ? new SimBalance(new JSONObject(getResponse(SIM_BALANCE_URL + "?msisdn=" + msisdn + "&" + "add_price_plan=1")), msisdn)
				: getSimBalance(msisdn);
	}

	public SimBalance getSimBalance(boolean addPricePlan) throws IOException, OAuthMessageSignerException, OAuthExpectationFailedException,
			OAuthCommunicationException, JSONException, BadLoginException {
		return getSimBalance(msisdn, addPricePlan);
	}

	/**
	 * Gets the topup history of the given MSISDN
	 * 
	 * @param msisdn
	 * @return TopUpHistory
	 * @throws JSONException
	 * @throws IOException
	 * @throws BadLoginException
	 * @throws OAuthCommunicationException
	 * @throws OAuthExpectationFailedException
	 * @throws OAuthMessageSignerException
	 * @throws Exception
	 */
	public TopUpHistory getTopUpHistory(String msisdn) throws JSONException, IOException, OAuthMessageSignerException, OAuthExpectationFailedException,
			OAuthCommunicationException, BadLoginException {
		return new TopUpHistory(new JSONArray(getResponse(TOPUP_HISTORY_URL + "?msisdn=" + msisdn)));
	}

	public TopUpHistory getTopUpHistory() throws JSONException, IOException, OAuthMessageSignerException, OAuthExpectationFailedException,
			OAuthCommunicationException, BadLoginException {
		return getTopUpHistory(msisdn);
	}

	/**
	 * Gets the usage for the given MSISDN
	 * 
	 * @param msisdn
	 * @param page_size
	 * @param page
	 * @param starttime
	 * @param endtime
	 * @return Usage
	 * @throws JSONException
	 * @throws IOException
	 * @throws BadLoginException
	 * @throws OAuthCommunicationException
	 * @throws OAuthExpectationFailedException
	 * @throws OAuthMessageSignerException
	 * @throws Exception
	 */
	public Usage getUsage(String msisdn, int page_size, int page, long starttime, long endtime) throws JSONException, IOException, OAuthMessageSignerException,
			OAuthExpectationFailedException, OAuthCommunicationException, BadLoginException {
		if((starttime != 0) && (endtime != 0)) {
			SimpleDateFormat formatTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			String start = formatTime.format(new Date(starttime));
			String end = formatTime.format(new Date(endtime));
			Log.d("[" + getClass().getSimpleName() + "] From:" + start + " to:" + end);
			return new Usage(new JSONArray(getResponse(USAGE_URL + "?msisdn=" + msisdn + "&page_size=" + page_size + "&from_date=" + start + "&until_date="
					+ end + "&page=" + page)), msisdn);
		}
		else {
			return new Usage(new JSONArray(getResponse(USAGE_URL + "?msisdn=" + msisdn + "&page_size=" + page_size)), msisdn);
		}
	}

	public Usage getUsage(int page_size, int page, long starttime, long endtime) throws JSONException, IOException, OAuthMessageSignerException,
			OAuthExpectationFailedException, OAuthCommunicationException, BadLoginException {
		return getUsage(msisdn, page_size, page, starttime, endtime);
	}

	public Usage getUsage(String msisdn, long starttime, long endtime) throws JSONException, IOException, OAuthMessageSignerException,
			OAuthExpectationFailedException, OAuthCommunicationException, BadLoginException {
		return getUsage(msisdn, 100, 1, starttime, endtime);
	}

	public Usage getUsage(long starttime, long endtime) throws JSONException, IOException, OAuthMessageSignerException, OAuthExpectationFailedException,
			OAuthCommunicationException, BadLoginException {
		return getUsage(msisdn, 100, 1, starttime, endtime);
	}

	public Usage getUsage(String msisdn) throws JSONException, IOException, OAuthMessageSignerException, OAuthExpectationFailedException,
			OAuthCommunicationException, BadLoginException {
		return getUsage(msisdn, 100, 1, 0, 0);
	}

	public Usage getUsage() throws JSONException, IOException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException,
			BadLoginException {
		return getUsage(msisdn, 100, 1, 0, 0);
	}

	public VikingPointsStats getVikingPointsStats() throws JSONException, IOException, Exception {
		return new VikingPointsStats(new JSONObject(getResponse(VIKING_POINTS_STATS_URL)));
	}

	public String getResponse(String url) throws IOException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException,
			BadLoginException {
		HttpClient client = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(url);
		consumer.sign(httpget);
		HttpResponse response = client.execute(httpget);

		// If login or password incorrect (HTTP code 401), throws a
		// BadLoginException
		if(response.getStatusLine().getStatusCode() == 401 || response.getStatusLine().getStatusCode() == 403) {
			throw new BadLoginException();
		}
		if(response.getEntity() != null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			StringBuilder sb = new StringBuilder();

			String line = null;
			while((line = reader.readLine()) != null) {
				sb.append(line);
			}
			Log.d("Request:" + url);
			Log.d("Response (" + response.getStatusLine() + "):" + sb.toString());
			return sb.toString();
		}
		return null;
	}

	public String getMsisdn() {
		return this.msisdn;
	}

	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}

	public static Date getDateFromAPI(String dateString) {
		try {
			return apiFormat.parse(dateString);
		}
		catch(ParseException e) {
			return null;
		}
	}

}
