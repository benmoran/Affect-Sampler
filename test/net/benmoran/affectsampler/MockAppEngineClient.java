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

import java.net.URI;

import net.benmoran.affectsampler.datastore.AppEngineClientImpl;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

public class MockAppEngineClient extends AppEngineClientImpl {

	public MockAppEngineClient() throws SyncException {
		super(null, null, null);
	}

	static class MockHttpResponse extends BasicHttpResponse {

		public static MockHttpResponse newResponse(int status) {
			ProtocolVersion version = new ProtocolVersion("HTTP", 1, 1);
			String reason = "OK";
			return new MockHttpResponse(new BasicStatusLine(version, status,
					reason));
		}

		public MockHttpResponse(StatusLine statusline) {
			super(statusline);
		}

	}

	public HttpResponse postJSON(URI uri, String body) throws SyncException {
		return MockHttpResponse.newResponse(201);
	};
}
