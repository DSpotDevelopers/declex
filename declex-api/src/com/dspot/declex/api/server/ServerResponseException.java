package com.dspot.declex.api.server;

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
