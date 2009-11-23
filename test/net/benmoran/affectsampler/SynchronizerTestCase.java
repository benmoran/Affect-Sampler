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
package net.benmoran.affectsampler;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.net.URI;

import net.benmoran.affectsampler.datastore.AppEngineClient;
import net.benmoran.affectsampler.datastore.AppEngineClientImpl;

import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.json.JSONArray;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;

public class SynchronizerTestCase extends InstrumentationTestCase {
	private JSONArray mJSONArray;

	protected Context getContext() {
		return getInstrumentation().getContext();
	}

	protected void setUp() throws Exception {
		super.setUp();
		TestDataBuilder tdBuilder = new TestDataBuilder(getContext()
				.getContentResolver());
		mJSONArray = tdBuilder.getJSONArray();
	}

	@MediumTest
	public void testSynchronizerLive() throws SyncException {
		String username = getContext().getString(R.string.testusername);
		String password = getContext().getString(R.string.testpassword);
		String host = getContext().getString(R.string.testhost);
		host = null; // live test

		AppEngineClient client = new AppEngineClientImpl(username, password,
				host);
		Synchronizer sync = new Synchronizer(client);
		int syncCount = sync.sync(mJSONArray);
		assertEquals(true, client.isLoggedIn());
		assertEquals(2, syncCount);
	}

	static class StubHttpResponse extends BasicHttpResponse {

		public static StubHttpResponse newResponse(int status) {
			ProtocolVersion version = new ProtocolVersion("HTTP", 1, 1);
			String reason = "OK";
			return new StubHttpResponse(new BasicStatusLine(version, status,
					reason));
		}

		public StubHttpResponse(StatusLine statusline) {
			super(statusline);
		}

	}

	@MediumTest
	public void testSynchronizerMock() throws SyncException {

		AppEngineClient mockClient;
		mockClient = createMock(AppEngineClient.class);
		expect(mockClient.isLoggedIn()).andReturn(false);
		expect(mockClient.postJSON((URI) anyObject(), (String) anyObject()))
				.andReturn(StubHttpResponse.newResponse(201));
		expect(mockClient.isLoggedIn()).andReturn(true);
		expect(mockClient.postJSON((URI) anyObject(), (String) anyObject()))
				.andReturn(StubHttpResponse.newResponse(201));
		expect(mockClient.isLoggedIn()).andReturn(true);
		replay(mockClient);
		Synchronizer sync = new Synchronizer(mockClient);
		int syncCount = sync.sync(mJSONArray);
		assertEquals(2, syncCount);
	}

}
