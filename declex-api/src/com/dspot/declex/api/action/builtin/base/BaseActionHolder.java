package com.dspot.declex.api.action.builtin.base;


public class BaseActionHolder {
	
	protected Runnable Done;
	
    protected void build(Runnable Done) {
    	this.Done = Done;
    }

    protected void execute() {
    }
    
    protected Runnable getDone() {
    	return this.Done;
    }
}
