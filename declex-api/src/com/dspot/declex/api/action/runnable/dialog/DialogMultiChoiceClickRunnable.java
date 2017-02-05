package com.dspot.declex.api.action.runnable.dialog;

import android.content.DialogInterface;

public abstract class DialogMultiChoiceClickRunnable 
	implements Runnable, DialogInterface.OnMultiChoiceClickListener {
	
	public DialogInterface dialog;
	public int position;
	public boolean checked;
	
	@Override
	public void onClick(DialogInterface dialog, int position, boolean checked) {
		this.dialog = dialog;
		this.position = position;
		this.checked = checked;
		run();
	}
}
