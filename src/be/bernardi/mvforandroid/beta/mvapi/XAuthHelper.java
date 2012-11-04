package be.bernardi.mvforandroid.beta.mvapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import be.bernardi.mvforandroid.beta.Log;
import be.bernardi.mvforandroid.beta.exception.BadLoginException;
import android.text.TextUtils;

public class XAuthHelper {

	private String consumer_key;
	private String consumer_secret;
	private String access_token_url;
	private String token;
	private String token_secret;
	private String userid;

	public XAuthHelper(String consumer_key, String consumer_secret, String access_token_url) {
		this.consumer_key = consumer_key;
		this.consumer_secret = consumer_secret;
		this.access_token_url = access_token_url;
	}

	public boolean performAuth(String username, String password) throws Exception {
		OAuthConsumer consumer = new CommonsHttpOAuthConsumer(consumer_key, consumer_secret);
		HttpClient client = new DefaultHttpClient();
		HttpPost request = new HttpPost(access_token_url);

		List<BasicNameValuePair> params = Arrays.asList(new BasicNameValuePair("x_auth_username", username),
				new BasicNameValuePair("x_auth_password", password), new BasicNameValuePair("x_auth_mode", "client_auth"));
		UrlEncodedFormEntity entity = null;
		try {
			entity = new UrlEncodedFormEntity(params, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("wtf");
		}
		request.setEntity(entity);
		try {
			consumer.sign(request);
		} catch (OAuthMessageSignerException e) {
			Log.e(getClass().getSimpleName()+": Exception: " + e.getMessage());
			return false;
		} catch (OAuthExpectationFailedException e) {
			Log.e(getClass().getSimpleName()+": Exception: " + e.getMessage());
			return false;
		} catch (OAuthCommunicationException e) {
			Log.e(getClass().getSimpleName()+": Exception: " + e.getMessage());
			return false;
		}
		HttpResponse response = null;
		InputStream data = null;
		try {
			Log.d(getClass().getSimpleName()+": Executing request: " + request.getURI());
			response = client.execute(request);
			Log.d(getClass().getSimpleName()+": StatusLine: " + response.getStatusLine());
			data = response.getEntity().getContent();
		} catch (ClientProtocolException e) {
			Log.e(getClass().getSimpleName()+": Exception: " + e.getMessage());
			return false;
		} catch (IOException e) {
			Log.e(getClass().getSimpleName()+": Exception: " + e.getMessage());
			return false;
		} catch (Exception e) {
			Log.e(getClass().getSimpleName()+": Exception: " + e.getMessage());
			return false;
		}
		
		if (response.getStatusLine().getStatusCode() == 403)
			throw new BadLoginException();
		String responseString = "";
		try {
			final char[] buffer = new char[0x10000];
			StringBuilder out = new StringBuilder();
			if (data != null) {
				Reader in = new InputStreamReader(data, HTTP.UTF_8);
				int read;
				do {
					read = in.read(buffer, 0, buffer.length);
					if (read > 0) {
						out.append(buffer, 0, read);
					}
				} while (read >= 0);
				in.close();
				responseString = out.toString();
			}
		} catch (IOException e) {
			Log.e(getClass().getSimpleName()+": Exception: " + e.getMessage());
			return false;
		} catch (Exception e) {
			Log.e(getClass().getSimpleName()+": Exception: " + e.getMessage());
			return false;
		}

		Log.d(getClass().getSimpleName()+": Response: " + responseString);
		String[] result = null;
		try {
			result = TextUtils.split(responseString, "&");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		if(result.length>3){
			token_secret = TextUtils.split(result[0], "=")[1];
			userid = TextUtils.split(result[1], "=")[1];
			token = TextUtils.split(result[2], "=")[1];
			return true;
		}
		return false;
	}
	

	public String getToken() {
		return token;
	}

	public String getToken_secret() {
		return token_secret;
	}

	public String getUserid() {
		return userid;
	}
}
