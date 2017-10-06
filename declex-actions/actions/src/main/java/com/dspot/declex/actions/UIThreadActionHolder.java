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

import com.dspot.declex.annotation.action.ActionFor;

@ActionFor("UIThread")
public class UIThreadActionHolder {

    @RootContext
    Context context;

    Runnable Run;

    void init() {
    }
    
    void build(Runnable Run) {
    	this.Run = Run;
    }

    void execute() {
        if(Run!=null) {
        	Handler mainHandler = new Handler(context.getMainLooper());
        	mainHandler.post(Run);
        }
    }
}