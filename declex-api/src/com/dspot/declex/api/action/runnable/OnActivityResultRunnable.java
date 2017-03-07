package com.dspot.declex.api.action.runnable;

import android.content.Intent;

public abstract class OnActivityResultRunnable implements Runnable {
	public int requestCode;
	public int resultCode;
	public Intent data;
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		this.requestCode = requestCode;
		this.resultCode = resultCode;
		this.data = data;
		run();
	}
}
