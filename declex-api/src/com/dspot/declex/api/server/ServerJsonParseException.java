package com.dspot.declex.api.server;

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
