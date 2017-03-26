package com.dspot.declex.api.action.builtin;

import com.dspot.declex.api.action.annotation.Field;
import com.dspot.declex.api.action.runnable.OnFailedRunnable;

public class BaseFieldActionHolder extends BaseActionHolder {
	
	protected OnFailedRunnable Failed;
	
    void init(@Field Object field) {
    }

    void build(Runnable Done, OnFailedRunnable Failed) {
    	super.build(Done);
    	this.Failed = Failed;
    }
    
    OnFailedRunnable getFailed() {
    	return this.Failed;
    }
}
