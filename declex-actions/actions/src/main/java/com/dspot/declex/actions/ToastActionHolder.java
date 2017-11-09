/**
 * Copyright (C) 2016-2017 DSpot Sp. z o.o
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
package com.dspot.declex.actions;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.dspot.declex.annotation.action.ActionFor;
import com.dspot.declex.annotation.action.FormattedExpression;

@ActionFor(value="Toast", timeConsuming = false)
public class ToastActionHolder {

    String text;
    int res;

    @RootContext
    Context context;

    Runnable Shown;
    
    int duration = Toast.LENGTH_LONG;

    void init(@FormattedExpression String text) {
        this.text = text;
    }

    void init(int res) {
        this.res = res;
    }

    void build(Runnable Shown) {
        this.Shown = Shown;
    }

    void execute() {
    	if (context == null) return;
    	
    	Runnable callToast = new Runnable() {
			
			@Override
			public void run() {
				if (text != null) {
		            Toast.makeText(context, text, duration).show();
		        } else if (res != 0) {
		            Toast.makeText(context, res, duration).show();
		        } else {
		        	Toast.makeText(context, "Toast Error: Toast message was not provided", duration).show();
		        }
		        if (Shown != null) Shown.run();
			}
		};
    	
    	//Thread save
    	if(Looper.myLooper() == Looper.getMainLooper()) {
		   callToast.run();
		} else {
			Handler handler = new Handler(Looper.getMainLooper());
			handler.post(callToast);
		}
        
    }
    
    public ToastActionHolder shortLength() {
    	duration = Toast.LENGTH_SHORT;
    	return this;
    }
    
    public ToastActionHolder longLength() {
    	duration = Toast.LENGTH_LONG;
    	return this;
    }

}