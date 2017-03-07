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
import com.dspot.declex.api.action.processor.PutModelActionProcessor;
import com.dspot.declex.api.action.runnable.OnFailedRunnable;

/**
 * An Action to put a {@link com.dspot.declex.api.model.Model @Model} 
 * annotated field. The Action will force a Recollect if the field is also 
 * annotated with {@link com.dspot.declex.api.viewsinjection.Recollect @Recollect}.
 * 
 * <br><br>
 * The model will be put in background by default. If it was specified 
 * {@code "asyncPut=false"} then  it is directly loaded in the calling thread 
 * where the action is executed. 
 * 
 * <br><br>
 * By default, after the Action executes, it returns 
 * to the main thread, so, it can be savely assigned values to View components.
 * This applies to {@link com.dspot.declex.Action.$PutModel#AfterPut AfterPut} and 
 * {@link com.dspot.declex.Action.$PutModel#Failed Failed} Action selectors). 
 * To avoid this behavior use {@link keepCallingThread()}.
 * 
 * @see com.dspot.declex.Action.$LoadModel $LoadModel
 * @see com.dspot.declex.Action.$Populate $Populate
 * @see com.dspot.declex.Action.$Recollect $Recollect
 */

@ActionFor(value="PutModel", processors=PutModelActionProcessor.class)
public class PutModelActionHolder {

	private Runnable AfterPut;
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
     * <i>"query"</i> that will be used as a query to put the 
     * {@link com.dspot.declex.api.model.Model @Model}  annotated field.
     * If it is not provided, the "query" parameter of the 
     * {@link com.dspot.declex.api.model.Model @Model}  annotation will be used
     */
    public PutModelActionHolder query(@FormattedExpression String query) {
    	this.query = query;
    	return this;
    }

    /**
     * "<i>orderBy</i>" that will be used to put the 
     * {@link com.dspot.declex.api.model.Model @Model} annotated field.
     * If it is not provided, the "orderBy" parameter of the 
     * {@link com.dspot.declex.api.model.Model @Model}  annotation will be used
     */
    public PutModelActionHolder orderBy(@FormattedExpression String orderBy) {
    	this.orderBy = orderBy;
    	return this;
    }
    
    /**
     * "<i>fields</i>" that will be put by @Model annotated field.
     * If it is not provided, the "fields" parameter of the
     * {@link com.dspot.declex.api.model.Model @Model}  annotation will be used
     */
    public PutModelActionHolder fields(@FormattedExpression String fields) {
    	this.fields = fields;
    	return this;
    }

    /**
     * @param AfterPut <i><b>(default)</b></i> It will be executed after the 
     * {@link com.dspot.declex.api.model.Model @Model}  annotated
     * field is put
     * 
     * @param Failed It will be executed if the 
     * {@link com.dspot.declex.api.model.Model @Model}  annotated field fails loading.
     */
    void build(Runnable AfterPut, OnFailedRunnable Failed) {
    	this.AfterPut = AfterPut;
    	this.Failed = Failed;
    }
    
    /**
     * Keeps the calling Thread for the Action. This avoids the automatic switch to
     * the UIThread after the Action finishes execution
     */
    public PutModelActionHolder keepCallingThread() {
    	keepCallingThread = true; //This will keep the Action in the thread that is executed, after finalization
    	return this;
    }
    
    /**
     * No recollect the Model after it is loaded
     */
    public PutModelActionHolder noRecollect() {
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
    
    Runnable getAfterPut() {
    	if (!keepCallingThread && this.AfterPut != null) {
    		
    		//Return to the main thread    		
			return new Runnable() {
				
				@Override
				public void run() {
					if(Looper.myLooper() == Looper.getMainLooper()) {
						AfterPut.run();;
					} else {
						Handler handler = new Handler(Looper.getMainLooper());
						handler.post(AfterPut);	
					}					
				}
			};
			
    	}

    	return this.AfterPut;
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