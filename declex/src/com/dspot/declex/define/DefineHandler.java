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
package com.dspot.declex.define;

import javax.lang.model.element.Element;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.holder.EComponentWithViewSupportHolder;

import com.dspot.declex.api.define.Define;

public class DefineHandler extends BaseAnnotationHandler<EComponentWithViewSupportHolder> {
		
	public DefineHandler(AndroidAnnotationsEnvironment environment) {
		super(Define.class, environment);
	}
	
	@Override
	protected void validate(Element element, ElementValidation valid) {
		for (String def : element.getAnnotation(Define.class).value()) {
			if (!def.contains("=")) {
				valid.addError("\"" + def + "\" is not a valid definition");
			}
		}
	}
	
	@Override
	public void process(Element element, EComponentWithViewSupportHolder holder) {
		
		final DefineHolder defineHolder = holder.getPluginHolder(new DefineHolder(holder));
		
		for (String def : element.getAnnotation(Define.class).value()) {
			String key = def.substring(0, def.indexOf('='));
			String value = def.substring(def.indexOf('=') + 1);
			
			if (key.startsWith("$")) {
				defineHolder.getRegexDefine().put(key.substring(1), value);
				continue;
			} 
			
			defineHolder.getNormalDefine().put(key, value);
		}
	}

}
