/**
 * Copyright (C) 2016-2018 DSpot Sp. z o.o
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dspot.declex.api.exception;

import okhttp3.Response;

public class ServerJsonParseException extends ServerResponseException {

	private static final long serialVersionUID = 7943256678652691298L;

	private String json;
	
	public ServerJsonParseException(Response response) {
		super(response);
	}
	public ServerJsonParseException(String json, Throwable cause) {
		super(cause);
		this.json = json;
	}
	
	@Override
	public String getMessage() {
		if (response != null) return super.getMessage(); 
		return "Error processing json: " + json;
	}
	
	@Override
	public String getLocalizedMessage() {
		return "Error processing json";
	}
}
