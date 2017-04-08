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

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.holder.EComponentHolder;

import com.dspot.declex.api.eventbus.UseEventBus;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.EventUtils;
import com.dspot.declex.util.TypeUtils;

public class BaseOnEventHandler extends BaseAnnotationHandler<EComponentHolder> {
	
	private Map<Element, String> inlineEvents = new HashMap<>();

	public BaseOnEventHandler(Class<?> targetClass,
			AndroidAnnotationsEnvironment environment) {
		super(targetClass, environment);
	}
	
	@Override
	public Set<Class<? extends Annotation>> getDependencies() {
		return new HashSet<>(Arrays.<Class<? extends Annotation>>asList(
					UseEventBus.class
			   ));
	}
	
	@Override
	public Element dependentElement(Element element,
			Class<? extends Annotation> dependency) {
		return element.getEnclosingElement();
	}

	@Override
	public void validate(Element element, ElementValidation valid) {
		
		UseEventBus annotation = adiHelper.getAnnotation(element.getEnclosingElement(), UseEventBus.class);
		if (annotation == null) {
			valid.addError("The enclosing element should include the @UseEventBus annotation");
		}
		
		String classField = TypeUtils.getClassFieldValue(element, getTarget(), "value", getEnvironment());
		
		if (classField.trim().equals("")) {
			valid.addError("No event has been provided");
			return;
		}
		
		if (!classField.contains(".")) {
			classField = DeclexConstant.EVENT_PATH + classField;
		}
		
		inlineEvents.put(element, classField);
		EventUtils.registerEvent(classField, getEnvironment());
	}

	@Override
	public void process(Element element, EComponentHolder holder)
			throws Exception {
		String classField = TypeUtils.getClassFieldValue(element, getTarget(), "value", getEnvironment());
		if (!classField.contains(".")) {
			classField = DeclexConstant.EVENT_PATH + classField;
		}
		EventUtils.createNewEvent(classField, element, getEnvironment());
	}
	
}