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
package com.dspot.declex.eventbus;

import javax.lang.model.element.Element;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.holder.EComponentHolder;

import com.dspot.declex.api.eventbus.UseEventBus;
import com.dspot.declex.handler.BaseTemplateHandler;
import com.dspot.declex.util.SharedRecords;

public class UseEventBusHandler extends BaseTemplateHandler<EComponentHolder> {
	
	public UseEventBusHandler(AndroidAnnotationsEnvironment environment) {
		super(UseEventBus.class, environment,
				 "com/dspot/declex/eventbus/", "UseEventBus.ftl.java");
	}

	@Override
	public void validate(Element element, ElementValidation valid) {
		
		UseEventBus useEventBus = adiHelper.getAnnotation(element, UseEventBus.class);
		if (useEventBus.debug())
			valid.addWarning("Available Events: " + SharedRecords.getEventGeneratedClasses(getEnvironment()));
	}
	
}
