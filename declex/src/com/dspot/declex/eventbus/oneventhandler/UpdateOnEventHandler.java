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
package com.dspot.declex.eventbus.oneventhandler;

import javax.lang.model.element.Element;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;

import com.dspot.declex.api.eventbus.UpdateOnEvent;
import com.dspot.declex.util.TypeUtils;

public class UpdateOnEventHandler extends BaseOnEventHandler {

	public UpdateOnEventHandler(AndroidAnnotationsEnvironment environment) {
		super(UpdateOnEvent.class, environment);
	}

	@Override
	public void validate(Element element, ElementValidation valid) {
		super.validate(element, valid);
		
		if (element.getAnnotation(UpdateOnEvent.class).debug()) {
			String classField = TypeUtils.getClassFieldValue(element, getTarget(), "value", getEnvironment());
			valid.addWarning("Event class: " + classField);
		}
		
		
	}

}
