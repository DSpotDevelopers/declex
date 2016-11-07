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
import org.androidannotations.annotations.RootContext;
import org.androidannotations.api.BackgroundExecutor;

import android.content.Context;

import com.dspot.declex.api.action.annotation.ActionFor;

@EBean
@ActionFor("Background")
public class BackgroundThreadActionHolder {

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
        	BackgroundExecutor.execute(Run);
        }
    }
}