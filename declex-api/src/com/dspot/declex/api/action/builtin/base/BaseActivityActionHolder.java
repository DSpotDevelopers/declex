package com.dspot.declex.api.action.builtin.base;

import org.androidannotations.api.builder.ActivityIntentBuilder;

import com.dspot.declex.annotation.action.StopOn;
import com.dspot.declex.api.action.runnable.OnActivityResultRunnable;

public abstract class BaseActivityActionHolder extends BaseActionHolder {
	
	protected OnActivityResultRunnable OnResult;
	protected int requestCode;
	
	protected void init() {
    }
	    
    protected void build(Runnable Done, OnActivityResultRunnable OnResult) {
        super.build(Done);
        this.OnResult = OnResult;
    }

    protected void execute() {
        if (requestCode == 0) {
            intent().start();
        } else {
        	intent().startForResult(requestCode);
        }
        if (Done != null) {
            Done.run();
        }
    }
    
	public BaseActivityActionHolder withResult(int requestCode) {
        this.requestCode = requestCode;
        return this;
    }

    public BaseActivityActionHolder withResult() {
        return this;
    }
    
    @StopOn("get")
    public abstract ActivityIntentBuilder<?> intent();
    
    protected OnActivityResultRunnable getOnResult() {
        return OnResult;
    }

    protected int getRequestCode() {
        return requestCode;
    }

}
