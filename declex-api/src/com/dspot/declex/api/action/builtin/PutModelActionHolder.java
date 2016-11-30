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

import org.androidannotations.annotations.EBean;

import android.os.Handler;
import android.os.Looper;

import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.api.action.annotation.Field;
import com.dspot.declex.api.action.annotation.FormattedExpression;
import com.dspot.declex.api.action.processor.PutModelActionProcessor;

@EBean
@ActionFor(value="PutModel", processors=PutModelActionProcessor.class)
public class PutModelActionHolder {

	private Runnable AfterPut;
	private boolean keepAsync;
	
    void init(@Field Object object) {
    }
    
    public PutModelActionHolder query(@FormattedExpression String query) {
    	return this;
    }

    public PutModelActionHolder orderBy(@FormattedExpression String query) {
    	return this;
    }

    void build(Runnable AfterPut) {
    	this.AfterPut = AfterPut;
    }
    
    public PutModelActionHolder keepAsync() {
    	keepAsync = true; //This will keep the Action in the thread that is executed, after finalization
    	return this;
    }
    
    Runnable getAfterPut() {
    	if (!keepAsync && this.AfterPut != null) {
    		
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

    void execute() {
    }
}