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

import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.api.action.annotation.Field;
import com.dspot.declex.api.action.annotation.FormattedExpression;
import com.dspot.declex.api.action.processor.GetModelActionProcessor;

@EBean
@ActionFor(value="GetModel", processors=GetModelActionProcessor.class)
public class GetModelActionHolder {

	private Runnable AfterLoad;
	
    void init(@Field Object object) {
    }
    
    public GetModelActionHolder query(@FormattedExpression String query) {
    	return this;
    }

    public GetModelActionHolder orderBy(@FormattedExpression String query) {
    	return this;
    }

    void build(Runnable AfterLoad) {
    	this.AfterLoad = AfterLoad;
    }
    
    Runnable getAfterLoad() {
    	return this.AfterLoad;
    }

    void execute() {
    }
}