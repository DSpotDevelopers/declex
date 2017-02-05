/**
 * Copyright (C) 2016 DSpot Sp. z o.o
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dspot.declex.api.action.builtin;

import android.os.Handler;
import android.os.Looper;

import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.api.action.annotation.Field;
import com.dspot.declex.api.action.annotation.FormattedExpression;
import com.dspot.declex.api.action.processor.GetModelActionProcessor;
import com.dspot.declex.api.action.runnable.OnFailedRunnable;

/**
 * An Action to load a {@link com.dspot.declex.api.model.Model @Model} 
 * annotated field. The Action will force a Populate if the field is also 
 * annotated with {@link com.dspot.declex.api.populator.Populator @Populator}.
 * 
 * <br><br>
 * The model will be loaded in background if it was specified {@code "async=true"},
 * otherwise, it is directly loaded in the calling thread where the action
 * is executed. 
 * 
 * <br><br>
 * By default, after the Action executes, it returns to the main
 * thread, so, it can be savely assigned values to View components (This applies to 
 * {@link com.dspot.declex.Action.$GetModel#AfterLoad AfterLoad} and 
 * {@link com.dspot.declex.Action.$GetModel#Failed Failed} Action selectors). 
 * To avoid this behavior use {@link keepCallingThread()}.
 * 
 * @see com.dspot.declex.Action.$PutModel $PutModel
 * @see com.dspot.declex.Action.$Populate $Populate
 * @see com.dspot.declex.Action.$Recollect $Recollect
 */

@ActionFor(value="GetModel", processors=GetModelActionProcessor.class)
public class GetModelActionHolder {

	private Runnable AfterLoad;
	private OnFailedRunnable Failed;
	
	private boolean keepCallingThread;
	
	private String query;
	private String orderBy;
	private String fields;
	
	/**
	 *@param field The field annotated with {@link com.dspot.declex.api.model.Model @Model}.
	 */
    void init(@Field Object field) {
    }
    
    /**
     * <i>"query"</i> that will be used as a query to load the 
     * {@link com.dspot.declex.api.model.Model @Model}  annotated field.
     * If it is not provided, the "query" parameter of the 
     * {@link com.dspot.declex.api.model.Model @Model}  annotation will be used
     */
    public GetModelActionHolder query(@FormattedExpression String query) {
    	this.query = query;
    	return this;
    }

    /**
     * "<i>orderBy</i>" that will be used to load the 
     * {@link com.dspot.declex.api.model.Model @Model} annotated field.
     * If it is not provided, the "orderBy" parameter of the 
     * {@link com.dspot.declex.api.model.Model @Model}  annotation will be used
     */
    public GetModelActionHolder orderBy(@FormattedExpression String orderBy) {
    	this.orderBy = orderBy;
    	return this;
    }

    /**
     * "<i>fields</i>" that will be loaded by @Model annotated field.
     * If it is not provided, the "fields" parameter of the
     * {@link com.dspot.declex.api.model.Model @Model}  annotation will be used
     */
    public GetModelActionHolder fields(@FormattedExpression String fields) {
    	this.fields = fields;
    	return this;
    }
    
    /**
     * @param AfterLoad <i><b>(default)</b></i> It will be executed after the 
     * {@link com.dspot.declex.api.model.Model @Model}  annotated
     * field is loaded
     * 
     * @param Failed It will be executed if the 
     * {@link com.dspot.declex.api.model.Model @Model}  annotated field fails loading.
     */
    void build(Runnable AfterLoad, OnFailedRunnable Failed) {
    	this.AfterLoad = AfterLoad;
    	this.Failed = Failed;
    }
    
    /**
     * Keeps the calling Thread for the Action. This avoids the automatic switch to
     * the UIThread after the Action finishes execution
     */
    public GetModelActionHolder keepCallingThread() {
    	keepCallingThread = true; //This will keep the Action in the thread that is executed, after finalization
    	return this;
    }
    
    String getQuery() {
    	return this.query;
    }
    
    String getOrderBy() {
    	return this.orderBy;
    }
    
    String getFields() {
    	return this.fields;
    }
    
    Runnable getAfterLoad() {    	
    	if (!keepCallingThread && this.AfterLoad != null) {
    		
    		//Return to the main thread    		
			return new Runnable() {
				
				@Override
				public void run() {
					if(Looper.myLooper() == Looper.getMainLooper()) {
						AfterLoad.run();;
					} else {
						Handler handler = new Handler(Looper.getMainLooper());
						handler.post(AfterLoad);	
					}					
				}
			};
			
    	}
    	
    	return this.AfterLoad;
    }
    
    OnFailedRunnable getFailed() {
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

    void execute() {
    }
    
}