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
package com.dspot.declex.handler.base;

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.Element;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.holder.EComponentHolder;

import com.dspot.declex.annotation.External;
import com.dspot.declex.helper.EventsHelper;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.wrapper.element.VirtualElement;

public class BaseOnEventHandler extends BaseAnnotationHandler<EComponentHolder> {
	
	private Map<Element, String> inlineEvents = new HashMap<>();
	
	protected EventsHelper eventsHelper;

	public BaseOnEventHandler(Class<?> targetClass,
			AndroidAnnotationsEnvironment environment) {
		super(targetClass, environment);
		eventsHelper = EventsHelper.getInstance(environment);
	}

	@Override
	public void validate(Element element, ElementValidation valid) {
		
		String classField = TypeUtils.getClassFieldValue(element, getTarget(), "value", getEnvironment());
		
		if (classField.trim().equals("")) {
			valid.addError("No event has been provided");
			return;
		}
		
		if (!classField.contains(".")) {
			classField = DeclexConstant.EVENT_PATH + classField;
		}
		
		inlineEvents.put(element, classField);
		eventsHelper.registerEvent(classField);
	}

	@Override
	public void process(Element element, EComponentHolder holder)
			throws Exception {
		
		if (adiHelper.hasAnnotation(element, External.class)) {
			if (element instanceof VirtualElement) {
				eventsHelper.registerAsEventListener(holder);
			}
		} else {
			eventsHelper.registerAsEventListener(holder);
		}
		
		String classField = TypeUtils.getClassFieldValue(element, getTarget(), "value", getEnvironment());
		if (!classField.contains(".")) {
			classField = DeclexConstant.EVENT_PATH + classField;
		}
		eventsHelper.createEvent(classField, element);
	}
	
}