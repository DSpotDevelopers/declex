package com.dspot.declex.api.action.runnable;

public abstract class OnFailedRunnable implements Runnable {
	public Throwable e;
	
	public void onFailed(Throwable e) {
		this.e = e;
		run();
	}
}
