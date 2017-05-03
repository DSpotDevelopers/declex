package com.dspot.declex.api.action.builtin.base;

import com.dspot.declex.api.action.annotation.Field;
import com.dspot.declex.api.action.runnable.OnFailedRunnable;

public class BaseFieldActionHolder extends BaseActionHolder {
	
	protected OnFailedRunnable Failed;
	
    protected void init(@Field Object field) {
    }

    protected void build(Runnable Done, OnFailedRunnable Failed) {
    	super.build(Done);
    	this.Failed = Failed;
    }
    
    protected OnFailedRunnable getFailed() {
    	return this.Failed;
    }
}
