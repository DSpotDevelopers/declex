package com.dspot.declex.api.action.builtin.base;

import android.os.Handler;
import android.os.Looper;

import com.dspot.declex.api.action.annotation.Field;
import com.dspot.declex.api.action.annotation.FormattedExpression;
import com.dspot.declex.api.action.runnable.OnFailedRunnable;


public class BaseModelActionHolder extends BaseFieldActionHolder {
	
	private boolean keepCallingThread;
	
	private String query;
	private String orderBy;
	private String fields;
	
	/**
	 *@param field The field annotated with {@link com.dspot.declex.api.model.Model @Model}.
	 */
	@Override
    protected void init(@Field Object field) {
    	super.init(field);
    }  
	
	/**
     * @param Done <i><b>(default)</b></i> It will be executed after the 
     * {@link com.dspot.declex.api.model.Model @Model}  annotated
     * field is loaded
     * 
     * @param Failed It will be executed if the 
     * {@link com.dspot.declex.api.model.Model @Model}  annotated field fails loading.
     */
	@Override
    protected void build(Runnable Done, OnFailedRunnable Failed) {
    	super.build(Done, Failed);
    }
    
    /**
     * <i>"query"</i> that will be used as a query to load the 
     * {@link com.dspot.declex.api.model.Model @Model}  annotated field.
     * If it is not provided, the "query" parameter of the 
     * {@link com.dspot.declex.api.model.Model @Model}  annotation will be used
     */
    public BaseModelActionHolder query(@FormattedExpression String query) {
    	this.query = query;
    	return this;
    }

    /**
     * "<i>orderBy</i>" that will be used to load the 
     * {@link com.dspot.declex.api.model.Model @Model} annotated field.
     * If it is not provided, the "orderBy" parameter of the 
     * {@link com.dspot.declex.api.model.Model @Model}  annotation will be used
     */
    public BaseModelActionHolder orderBy(@FormattedExpression String orderBy) {
    	this.orderBy = orderBy;
    	return this;
    }

    /**
     * "<i>fields</i>" that will be loaded by @Model annotated field.
     * If it is not provided, the "fields" parameter of the
     * {@link com.dspot.declex.api.model.Model @Model}  annotation will be used
     */
    public BaseModelActionHolder fields(@FormattedExpression String fields) {
    	this.fields = fields;
    	return this;
    }

    /**
     * Keeps the calling Thread for the Action. This avoids the automatic switch to
     * the UIThread after the Action finishes execution
     */
    public BaseModelActionHolder keepCallingThread() {
    	keepCallingThread = true; //This will keep the Action in the thread that is executed, after finalization
    	return this;
    }
    
    protected String getQuery() {
    	return this.query;
    }
    
    protected String getOrderBy() {
    	return this.orderBy;
    }
    
    protected String getFields() {
    	return this.fields;
    }
    
    @Override
    protected Runnable getDone() {    	
    	if (!keepCallingThread && this.Done != null) {
    		
    		//Return to the main thread    		
			return new Runnable() {
				
				@Override
				public void run() {
					if(Looper.myLooper() == Looper.getMainLooper()) {
						Done.run();;
					} else {
						Handler handler = new Handler(Looper.getMainLooper());
						handler.post(Done);	
					}					
				}
			};
			
    	}
    	
    	return this.Done;
    }
    
    @Override
    protected OnFailedRunnable getFailed() {
    	if (!keepCallingThread && this.Failed != null) {
    		
    		//Return to the main thread    		
			return new OnFailedRunnable() {
				
				@Override
				public void run() {
					Failed.e = this.e;
					
					if(Looper.myLooper() == Looper.getMainLooper()) {
						Failed.run();;
					} else {
						Handler handler = new Handler(Looper.getMainLooper());
						handler.post(Failed);	
					}					
				}
			};
			
    	}

    	return this.Failed;
    }
    
}
