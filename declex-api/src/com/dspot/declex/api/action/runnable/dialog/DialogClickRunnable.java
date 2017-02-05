package com.dspot.declex.api.action.runnable.dialog;

import android.content.DialogInterface;

public abstract class DialogClickRunnable implements Runnable, DialogInterface.OnClickListener {
	
	public DialogInterface dialog;
	public int position;
	
	@Override
	public void onClick(DialogInterface dialogInterface, int position) {
		this.dialog = dialogInterface;
		this.position = position;
		run();
	}
}
