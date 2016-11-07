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

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.holder.BaseGeneratedClassHolder;
import org.androidannotations.holder.EComponentWithViewSupportHolder;

import com.dspot.declex.api.eventbus.UseEventBus;
import com.dspot.declex.handler.BaseTemplateHandler;
import com.dspot.declex.share.holder.ViewsHolder;
import com.dspot.declex.util.EventUtils;
import com.dspot.declex.util.SharedRecords;

public class UseEventBusHandler extends BaseTemplateHandler {

	public UseEventBusHandler(AndroidAnnotationsEnvironment environment) {
		super(UseEventBus.class, environment,
				 "com/dspot/declex/eventbus/", "UseEventBus.ftl.java");
	}

	@Override
	public void validate(Element element, ElementValidation valid) {
		
		if (element.getAnnotation(UseEventBus.class).debug())
			valid.addWarning("Available Events: " + SharedRecords.getEventGeneratedClasses(getEnvironment()));
	}
	
	@Override
	public void process(Element element, BaseGeneratedClassHolder holder) {
		super.process(element, holder);

		final ViewsHolder viewsHolder;
		if (holder instanceof EComponentWithViewSupportHolder) {
			viewsHolder = holder.getPluginHolder(
				new ViewsHolder((EComponentWithViewSupportHolder) holder, annotationHelper)
			);
		} else {
			viewsHolder = null;
		}
				
		
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems)
			if (elem.getKind() == ElementKind.METHOD) {
				ExecutableElement executableElement = (ExecutableElement) elem;
				if (!executableElement.getReturnType().getKind().equals(TypeKind.VOID)) continue;
				
				String elemName = executableElement.getSimpleName().toString();
				if (elemName.startsWith("on")) elemName = elemName.substring(2);
				
				String eventClassName = SharedRecords.getEvent(elemName, getEnvironment()); 
				if (eventClassName != null) {
					EventUtils.getEventMethod(eventClassName, element, holder, viewsHolder, getEnvironment());
				}
			}
	}
	
}
