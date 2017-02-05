package com.dspot.declex.api.action.error;

public class ValidationException extends RuntimeException {

	private static final long serialVersionUID = 3947581447815972637L;

	public ValidationException() {
		super();
	}

	public ValidationException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ValidationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ValidationException(String message) {
		super(message);
	}

	public ValidationException(Throwable cause) {
		super(cause);
	}

	
	
}
