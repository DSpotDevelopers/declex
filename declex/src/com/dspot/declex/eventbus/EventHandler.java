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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.holder.EComponentHolder;

import com.dspot.declex.api.eventbus.Event;
import com.dspot.declex.api.eventbus.UseEventBus;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.EventUtils;
import com.dspot.declex.util.SharedRecords;
import com.helger.jcodemodel.JClassAlreadyExistsException;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JMod;

public class EventHandler extends BaseAnnotationHandler<EComponentHolder> {

	public final static Map<String, Map<String, String>> roundGeneratedEvents = new HashMap<>();
	
	public EventHandler(AndroidAnnotationsEnvironment environment) {
		super(Event.class, environment);
	}
	
	@Override
	protected void validate(Element element, ElementValidation valid) {
		if (element instanceof ExecutableElement) {
			ExecutableElement executableElement = (ExecutableElement) element;
			
			if (!executableElement.getReturnType().getKind().equals(TypeKind.VOID)) {
				valid.addError(element, "An event method should return \"void\"");					
			}
			
			String methodName = element.getSimpleName().toString();
			if (!methodName.startsWith("on")) {
				valid.addError("An event method name should start with \"on\"");	
			}

			if (methodName.toLowerCase().startsWith("onevent")) {
				valid.addError("An event method name cannot start with \"onEvent\", this is reserved for the system");	
			}
			
			if (methodName.length() <= 2) {
				valid.addError("You should provide a name in your event, Ex: \"onMyEvent\"");				
			}

			UseEventBus annotation = element.getEnclosingElement().getAnnotation(UseEventBus.class);
			if (annotation == null) {
				valid.addError("The enclosing class should be annotated with @UseEventBus");
			}

			if (!valid.isValid()) return;
			
			String className = methodName.substring(2);
			className = DeclexConstant.EVENT_PATH + className;
			
			//Add to the Special Events in validation, so other validators will find this Event
			SharedRecords.addEventGeneratedClass(className, getEnvironment());

			Map<String, String> fields = new HashMap<>();
			roundGeneratedEvents.put(className, fields);
			
			List<? extends VariableElement> parameters = executableElement.getParameters();				
			if (parameters.size() != 0) {
				for (VariableElement param : parameters) {
					final String paramName = param.getSimpleName().toString();
					String paramType = param.asType().toString();
					
					if (paramType.equals(className)) continue;
					if (!paramType.contains(".") && className.endsWith("." + paramType)) continue;
					
					fields.put(paramName, paramType);
				}	
			}
			return;
		}
		
		String className = element.asType().toString();
		if (className.contains(".")) className = className.substring(className.lastIndexOf('.')+1);
		
		if (className.toLowerCase().startsWith("event")) {
			valid.addError("An event class name cannot start with \"Event\" signature, this is reserved for the system");
			return;
		}
		
		className = DeclexConstant.EVENT_PATH + className;
		
		//Add to the Special Events in validation, so other validators will find this Event
		SharedRecords.addEventGeneratedClass(className, getEnvironment());
		
		Map<String, String> fields = new HashMap<>();
		roundGeneratedEvents.put(className, fields);
		
		Event eventAnnotation = element.getAnnotation(Event.class);
		for (String field : eventAnnotation.value()) {
			Matcher matcher = Pattern.compile("\\s*(\\w+)\\s*:\\s*([A-Za-z_][A-Za-z0-9_.]+)").matcher(field);
			if (matcher.find()) {
				String clazz = matcher.group(2); 
				
				if (clazz.indexOf('.')==-1 && Character.isUpperCase(clazz.charAt(0))) {
					clazz = "java.lang." + clazz;
				}
				
				fields.put(matcher.group(1), clazz);
			}
		}
	}

	@Override
	public void process(Element element, EComponentHolder holder)
			throws Exception {
		
		try {
			String className = element instanceof ExecutableElement ? 
					           element.getSimpleName().toString().substring(2) : 
				               element.asType().toString();
			if (className.contains(".")) className = className.substring(className.lastIndexOf('.')+1);
			
			
			JDefinedClass EventClass = EventUtils.createNewEvent(className, getEnvironment());
			Map<String, String> eventFields = roundGeneratedEvents.get(EventClass.fullName());
			
			for (Entry<String, String> field : eventFields.entrySet()) {
				EventClass.field(JMod.NONE, getJClass(field.getValue()), field.getKey());
			}
						
		} catch (JClassAlreadyExistsException e) {
		}
	}


}
