package be.bernardi.mvforandroid.beta.mvapi;

import java.io.IOException;

import oauth.signpost.AbstractOAuthProvider;
import oauth.signpost.commonshttp.HttpRequestAdapter;
import oauth.signpost.commonshttp.HttpResponseAdapter;
import oauth.signpost.http.HttpRequest;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

public class ProviderHelper extends AbstractOAuthProvider {

	private static final long serialVersionUID = 1L;

	private transient HttpClient httpClient;

	public ProviderHelper(String requestTokenEndpointUrl, String accessTokenEndpointUrl, String authorizationWebsiteUrl) {
		super(requestTokenEndpointUrl, accessTokenEndpointUrl, authorizationWebsiteUrl);
		this.httpClient = new DefaultHttpClient();
	}

	public ProviderHelper(String requestTokenEndpointUrl, String accessTokenEndpointUrl, String authorizationWebsiteUrl, HttpClient httpClient) {
		super(requestTokenEndpointUrl, accessTokenEndpointUrl, authorizationWebsiteUrl);
		this.httpClient = httpClient;
	}

	public void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	@Override
	protected HttpRequest createRequest(String endpointUrl) throws Exception {
		HttpPost request = new HttpPost(endpointUrl);
		return new HttpRequestAdapter(request);
	}

	@Override
	protected oauth.signpost.http.HttpResponse sendRequest(HttpRequest request) throws Exception {
		HttpResponse response = httpClient.execute((HttpUriRequest) request.unwrap());
		return new HttpResponseAdapter(response);
	}

	@Override
	protected void closeConnection(HttpRequest request, oauth.signpost.http.HttpResponse response) throws Exception {
		if (response != null) {
			HttpEntity entity = ((HttpResponse) response.unwrap()).getEntity();
			if (entity != null) {
				try {
					// free the connection
					entity.consumeContent();
				} catch (IOException e) {
					// this means HTTP keep-alive is not possible
					e.printStackTrace();
				}
			}
		}
	}
}