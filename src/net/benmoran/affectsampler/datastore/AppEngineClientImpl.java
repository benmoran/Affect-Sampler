/*
 * 
 *  Copyright 2009 (C) Ben Moran
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License. 
 *  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0 
 *     
 *  Unless required by applicable law or agreed to in writing, 
 *  software distributed under the License is distributed on an 
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *  either express or implied. See the License for the specific 
 *  language governing permissions and limitations under the License. 
 */
package net.benmoran.affectsampler.datastore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import net.benmoran.affectsampler.SyncException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import android.util.Log;

public class AppEngineClientImpl implements AppEngineClient {
	
	private static final String APP_NAME = "affectsampler-sync-v1";
	private static final String GOOGLE_CLIENT_LOGIN = "https://www.google.com/accounts/ClientLogin";
	private static final String SYNC_URI = "https://affectsampler.appspot.com";
	private static final String TAG = "AppEngineClient";

	private URI mUri;
	private String mUsername;
	private String mPassword;
	private HttpClient mClient;
	private boolean mLoggedIn;

	public AppEngineClientImpl(String user, String pass, String uri)
			throws SyncException {
		mUsername = user;
		mPassword = pass;
		if (uri == null) {
			uri = SYNC_URI;
		}
		try {
			mUri = new URI(uri);
		} catch (URISyntaxException e) {
			throw new SyncException(e);
		}
		final HttpParams params = new BasicHttpParams();
		HttpClientParams.setRedirecting(params, false);

		mClient = new DefaultHttpClient(params);
	}

	private URI getFullURI(String fragment) throws URISyntaxException {
		return new URI(mUri.toString() + fragment);
	}

	/* (non-Javadoc)
	 * @see net.benmoran.affectsampler.datastore.AppEngineClient#postJSON(java.net.URI, java.lang.String)
	 */
	public HttpResponse postJSON(URI uri, String body) throws SyncException {
		if (!isLoggedIn()) {
			login();
		}
		HttpPost request;
		try {
			request = new HttpPost(getFullURI(uri.toString()));
		} catch (URISyntaxException e) {
			throw new SyncException(e);
		}
		StringEntity entity;
		try {
			entity = new StringEntity(body);
		} catch (UnsupportedEncodingException e) {
			throw new SyncException(e);
		}
		entity.setContentType("application/json");

		request.setEntity(entity);

		Log.i(TAG, "Sending to uri " + request.getURI().toString() + " " + body);
		try {
			return mClient.execute(request);
		} catch (ClientProtocolException e) {
			throw new SyncException(e);
		} catch (IOException e) {
			throw new SyncException(e);
		}
	}
	
	private URI getLoginUri(String authKey, String continueUrl) {
		return URI.create(mUri.toString() + "/_ah/login?auth=" + authKey
				+ "&continue=" + continueUrl);
	}

	private void login() throws SyncException {

		// Setup Google.com login request parameters
		List<BasicNameValuePair> nvps = new ArrayList<BasicNameValuePair>();
		nvps.add(new BasicNameValuePair("Email", mUsername));
		nvps.add(new BasicNameValuePair("Passwd", mPassword));
		nvps.add(new BasicNameValuePair("service", "ah"));
		nvps.add(new BasicNameValuePair("source", APP_NAME)); 
		nvps.add(new BasicNameValuePair("accountType", "GOOGLE"));
		HttpPost httpost = new HttpPost(GOOGLE_CLIENT_LOGIN);
		try {
			httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new SyncException(e);
		}
		HttpResponse response;
		try {
			response = mClient.execute(httpost);
		} catch (ClientProtocolException e) {
			throw new SyncException(e);
		} catch (IOException e) {
			throw new SyncException(e);
		}
		Log.i(TAG, "Google.com Login Response: " + response.getStatusLine());

		// Find authkey in response body to pass to Appspot.com
		ByteArrayOutputStream ostream = new ByteArrayOutputStream();
		try {
			response.getEntity().writeTo(ostream);
		} catch (IOException e) {
			throw new SyncException(e);
		}
		String strResponse = ostream.toString();
		Log.v(TAG, strResponse);
		StringTokenizer st = new StringTokenizer(strResponse, "\n\r=");
		String authKey = null;
		while (st.hasMoreTokens()) {
			if (st.nextToken().equalsIgnoreCase("auth")) {
				authKey = st.nextToken();
				Log.d(TAG, "AUTH = " + authKey);
				break;
			}
		}

		URI uri = getLoginUri(authKey, "/"); // Continue URL just /
		HttpGet httpget = new HttpGet(uri);

		try {
			response = mClient.execute(httpget);

		} catch (ClientProtocolException e) {

			throw new SyncException(e);
		} catch (IOException e) {
			throw new SyncException(e);
		}

		int status = response.getStatusLine().getStatusCode();
		try {
			response.getEntity().consumeContent();
		} catch (IOException e) {
			// Not interested in response, but want to silence warnings:
//			11-20 22:34:02.461: WARN/SingleClientConnManager(1295): Invalid use of SingleClientConnManager: connection still allocated.
//			11-20 22:34:02.461: WARN/SingleClientConnManager(1295): Make sure to release the connection before allocating another one.

		}
		if (status == 302 || status == 200) {
			mLoggedIn = true;
		} else {
			throw new SyncException("Unexpected response to sync: " + response.getStatusLine().toString());
		}
	}

	/* (non-Javadoc)
	 * @see net.benmoran.affectsampler.datastore.AppEngineClient#isLoggedIn()
	 */
	public boolean isLoggedIn() {
		return mLoggedIn;
	}

}
