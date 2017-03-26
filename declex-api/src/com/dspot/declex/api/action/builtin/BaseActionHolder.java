package com.dspot.declex.api.action.builtin;


public class BaseActionHolder {
	
	protected Runnable Done;
	
    void build(Runnable Done) {
    	this.Done = Done;
    }

    void execute() {
    }
    
    Runnable getDone() {
    	return this.Done;
    }
}
