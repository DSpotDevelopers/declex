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

import java.io.IOException;

import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;

public class ServerResponseException extends RuntimeException {

	private static final long serialVersionUID = -4844141003970037920L;

	protected Response response;
	private String sentBody;
	private String recvBody;
	
	public ServerResponseException(Response response) {
		super();
		this.response = response;
		
		this.sentBody = bodyToString(response.request());
		try {
			this.recvBody = response.body().string();
		} catch (IOException e) {
			this.recvBody = "NULL";
		}
	}
	
	public ServerResponseException(Throwable cause) {
		super(cause);
	}
	
	public Response getResponse() {
		return response;
	}
	
	private static String bodyToString(final Request request){

	    try {
	        final Request copy = request.newBuilder().build();
	        final Buffer buffer = new Buffer();
	        copy.body().writeTo(buffer);
	        return buffer.readUtf8();
	    } catch (Exception e) {
	        return "";
	    }
	}
	
	public String getSentData() {
		return bodyToString(response.request());
	}
	
	@Override
	public String getMessage() {
		
		if (response == null) return super.getMessage();
		
		StringBuilder builder = new StringBuilder();
		
		builder.append("Request: ");
		builder.append("\n    " + response.request());
		builder.append("\n    " + response.request().headers());
		
		builder.append("\nSent Data: ");
		builder.append("\n    " + sentBody);
		builder.append("\n\n");
		
		builder.append("\nResponse:");
		builder.append("\n    " + response);
		
		builder.append("\nReceived Data: ");
		builder.append("\n    " + recvBody);

		return builder.toString();
	}
	
	@Override
	public String getLocalizedMessage() {
		if (response != null) {
			return "The server response was no successful.";
		} else {
			return "The server response was no successful. Error: " +  response.code();
		}
	}
		
}
