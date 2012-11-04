/*
	Copyright (C) 2011 Lorenzo Bernardi (fastlorenzo@gmail.com)
	2010 Ben Van Daele (vandaeleben@gmail.com)

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

package be.bernardi.mvforandroid.beta.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import be.bernardi.mvforandroid.beta.exception.BadLoginException;

import android.util.Log;

public class MVDataHelper {



	/**
	 * Returns the GET response of the given url.
	 * 
	 * @throws IOException
	 * @return The response of the given URL. If no response was found, null is
	 *         returned.
	 * @throws BadLoginException 
	 */
	public static String getResponse(String username, String password, String url) throws IOException, BadLoginException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		Credentials creds = new UsernamePasswordCredentials(username, password);
		httpclient.getCredentialsProvider().setCredentials(new AuthScope(null, -1), creds);

		String auth = Base64.encodeToString((username + ":" + password).getBytes("UTF-8"), Base64.NO_WRAP);

		HttpGet httpget = new HttpGet(url);
		httpget.addHeader("Authorization", "Basic " + auth);
		HttpResponse response = httpclient.execute(httpget);

		// If login or password incorrect (HTTP code 401), throws a BadLoginException
		if(response.getStatusLine().getStatusCode()==401) {
			throw new BadLoginException();
		}
		if(response.getEntity() != null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			StringBuilder sb = new StringBuilder();

			String line = null;
			while((line = reader.readLine()) != null) {
				sb.append(line);
			}
			if(true) {
				Log.d(MVDataHelper.class.getSimpleName(), "Request:" + url);
				Log.d(MVDataHelper.class.getSimpleName(), "Response ("+response.getStatusLine()+"):" + sb.toString());
			}
			return sb.toString();
		}

		return null;
	}

}
