/**
 * Copyright (C) 2016-2018 DSpot Sp. z o.o
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
package com.dspot.declex.handler;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.holder.EComponentWithViewSupportHolder;

import org.androidannotations.annotations.export.Export;
import org.androidannotations.annotations.export.Exported;
import com.dspot.declex.annotation.OnEvent;
import com.dspot.declex.handler.base.BaseOnEventHandler;
import com.dspot.declex.holder.ViewsHolder;
import com.dspot.declex.util.DeclexConstant;
import org.androidannotations.internal.virtual.VirtualElement;
import com.helger.jcodemodel.AbstractJClass;

public class OnEventHandler extends BaseOnEventHandler {	
	
	public OnEventHandler(AndroidAnnotationsEnvironment environment) {
		super(OnEvent.class, environment);
	}

	@Override
	public void process(Element element, EComponentHolder holder)
			throws Exception {
		
		if (adiHelper.hasAnnotation(element, Exported.class)
			|| adiHelper.hasAnnotation(element, Export.class)) {
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
		
		AbstractJClass EventClass = eventsHelper.createEvent(classField, element);
		        
		final ViewsHolder viewsHolder;
		if (holder instanceof EComponentWithViewSupportHolder) {
			viewsHolder = holder.getPluginHolder(
				new ViewsHolder((EComponentWithViewSupportHolder) holder)
			);
		} else {
			viewsHolder = null;
		}
		
		ExecutableElement executableElement = (ExecutableElement) element;
		eventsHelper.addEventListener(EventClass.fullName(), executableElement, holder, viewsHolder);		
		
	}


}
