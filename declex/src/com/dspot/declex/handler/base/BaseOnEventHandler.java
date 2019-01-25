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
package com.dspot.declex.handler.base;

import javax.lang.model.element.Element;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EComponentHolder;

import org.androidannotations.annotations.export.Exported;
import com.dspot.declex.helper.EventsHelper;
import com.dspot.declex.util.DeclexConstant;
import org.androidannotations.internal.virtual.VirtualElement;

public class BaseOnEventHandler extends BaseAnnotationHandler<EComponentHolder> {
	
	protected EventsHelper eventsHelper;

	public BaseOnEventHandler(Class<?> targetClass,
			AndroidAnnotationsEnvironment environment) {
		super(targetClass, environment);
		eventsHelper = EventsHelper.getInstance(environment);
	}

	@Override
	public void validate(Element element, ElementValidation valid) {
		
		String classField = annotationHelper.extractAnnotationClassNameParameter(element, getTarget(), "value");
		
		if (classField.trim().equals("")) {
			valid.addError("No event has been provided");
			return;
		}
		
		if (!classField.contains(".")) {
			classField = DeclexConstant.EVENT_PATH + classField;
		}
		if (classField.endsWith(ModelConstants.generationSuffix())) {
			classField = classField.substring(0, classField.length()-1);
		}
		
		eventsHelper.registerEvent(classField);
	}

	@Override
	public void process(Element element, EComponentHolder holder)
			throws Exception {
		
		if (adiHelper.hasAnnotation(element, Exported.class)) {
			if (element instanceof VirtualElement) {
				eventsHelper.registerAsEventListener(holder);
			}
		} else {
			eventsHelper.registerAsEventListener(holder);
		}
		
		String classField = annotationHelper.extractAnnotationClassNameParameter(element, getTarget(), "value");
		if (!classField.contains(".")) {
			classField = DeclexConstant.EVENT_PATH + classField;
		}
		if (classField.endsWith(ModelConstants.generationSuffix())) {
			classField = classField.substring(0, classField.length()-1);
		}
		
		eventsHelper.createEvent(classField, element);
	}
	
}