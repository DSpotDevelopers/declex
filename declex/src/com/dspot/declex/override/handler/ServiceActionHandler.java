/**
 * Copyright (C) 2016-2019 DSpot Sp. z o.o
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
package com.dspot.declex.override.handler;

import java.lang.reflect.Field;

import javax.lang.model.element.Element;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.helper.APTCodeModelHelper;
import org.androidannotations.holder.EIntentServiceHolder;
import org.androidannotations.holder.HasIntentBuilder;
import org.androidannotations.internal.core.helper.IntentBuilder;

import com.dspot.declex.override.helper.DeclexAPTCodeModelHelper;

public class ServiceActionHandler extends org.androidannotations.internal.core.handler.ServiceActionHandler {

	public ServiceActionHandler(AndroidAnnotationsEnvironment environment) {
		super(environment);
		codeModelHelper = new DeclexAPTCodeModelHelper(getEnvironment());
	}
	
	@Override
	public void process(Element element, EIntentServiceHolder holder) throws Exception {
		try {
			
			if (holder instanceof HasIntentBuilder) {
				APTCodeModelHelper helper = new DeclexAPTCodeModelHelper(getEnvironment());
				helper.getActualType(element, holder);
				
			    IntentBuilder builder = ((HasIntentBuilder) holder).getIntentBuilder();
			    
			    Field helperField = IntentBuilder.class.getDeclaredField("codeModelHelper");
			    helperField.setAccessible(true);
			    helperField.set(builder, helper);
			}
			
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}		
				
		super.process(element, holder);
		
	}

	
}
