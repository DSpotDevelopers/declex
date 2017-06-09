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
package com.dspot.declex.handler;

import static com.helger.jcodemodel.JExpr.ref;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.holder.EComponentWithViewSupportHolder;

import com.dspot.declex.annotation.Event;
import com.dspot.declex.annotation.External;
import com.dspot.declex.annotation.UseEventBus;
import com.dspot.declex.api.util.FormatsUtils;
import com.dspot.declex.holder.ViewsHolder;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.EventUtils;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;

public class EventHandler extends BaseAnnotationHandler<EComponentHolder> {	
	
	public EventHandler(AndroidAnnotationsEnvironment environment) {
		super(Event.class, environment);
	}
	
	@Override
	public void getDependencies(Element element, Map<Element, Object> dependencies) {
		if (element instanceof ExecutableElement && !adiHelper.hasAnnotation(element, External.class)) {
			dependencies.put(element.getEnclosingElement(), UseEventBus.class);
		}
	}
	
	@Override
	protected void validate(Element element, ElementValidation valid) {
		
	    String className = element instanceof ExecutableElement ? 
		           element.getSimpleName().toString().substring(2) : 
	               element.asType().toString();
        if (className.contains(".")) className = className.substring(className.lastIndexOf('.')+1);	        
     
		if (element instanceof ExecutableElement) {
			ExecutableElement executableElement = (ExecutableElement) element;
			
			if (!executableElement.getReturnType().getKind().equals(TypeKind.VOID)) {
				valid.addError(element, "An event method should return \"void\"");					
			}
			
			String methodName = element.getSimpleName().toString();
			if (!methodName.startsWith("on")) {
				valid.addError("An event method name should start with \"on\"");	
			}
			
			if (methodName.length() <= 2) {
				valid.addError("You should provide a name in your event, Ex: \"onMyEvent\"");				
			}

			UseEventBus annotation = adiHelper.getAnnotation(element.getEnclosingElement(), UseEventBus.class);
			if (annotation == null) {
				valid.addError("The enclosing class should be annotated with @UseEventBus");
			}

			if (!valid.isValid()) return;
			
			className = DeclexConstant.EVENT_PATH + className;			
			final Map<String, String> fields = new LinkedHashMap<>();
			
			List<? extends VariableElement> parameters = executableElement.getParameters();				
			if (parameters.size() != 0) {
				for (VariableElement param : parameters) {
					final String paramName = param.getSimpleName().toString();
					final String paramType = param.asType().toString();
					
					if (paramType.equals(className) 
						|| paramType.equals(TypeUtils.getGeneratedClassName(className, getEnvironment()))) continue;
					if (!paramType.contains(".") && className.endsWith("." + paramType)) continue;
					
					fields.put(paramName, paramType);
				}	
			}			
			
			EventUtils.registerEvent(className, fields, getEnvironment());
			
			return;
		}
		
		className = DeclexConstant.EVENT_PATH + className;
		Event eventAnnotation = element.getAnnotation(Event.class);
		final Map<String, String> fields = new HashMap<>();
		
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
		
		EventUtils.registerEvent(className, fields, getEnvironment());
	}

	@Override
	public void process(Element element, EComponentHolder holder)
			throws Exception {
		
		String className = element instanceof ExecutableElement ? 
		           element.getSimpleName().toString().substring(2) : 
	               element.asType().toString();
        if (className.contains(".")) className = className.substring(className.lastIndexOf('.')+1);	
        
        AbstractJClass EventClass = EventUtils.createNewEvent(className, element, getEnvironment());
		Map<String, String> eventFields = EventUtils.eventsFields.get(EventClass.fullName());
		
		if (element instanceof ExecutableElement) {
			final ViewsHolder viewsHolder;
			if (holder instanceof EComponentWithViewSupportHolder) {
				viewsHolder = holder.getPluginHolder(
					new ViewsHolder((EComponentWithViewSupportHolder) holder, annotationHelper)
				);
			} else {
				viewsHolder = null;
			}
			
			//Purge parameters
			List<? extends VariableElement> parameters = ((ExecutableElement) element).getParameters();				
			if (parameters.size() != 0) {
				for (VariableElement param : parameters) {
					final String paramName = param.getSimpleName().toString();
					String paramType = param.asType().toString();
					
					//Remove references to the same event
					paramType = TypeUtils.typeFromTypeString(paramType, getEnvironment());
					if (paramType.equals(EventClass.fullName() + ModelConstants.generationSuffix()) 
						|| paramType.equals(EventClass.fullName())) {
						eventFields.remove(paramName);
					}
					
					if (!TypeUtils.isSubtype(paramType, CanonicalNameConstants.VIEW, getProcessingEnvironment())) {
						continue;
					}
					
					if (viewsHolder != null && viewsHolder.layoutContainsId(paramName)) {
						eventFields.remove(paramName);
					}
				}	
			}	
			
			ExecutableElement executableElement = (ExecutableElement) element;
			EventUtils.getEventMethod(EventClass.fullName(), executableElement, holder, viewsHolder, getEnvironment());		
		}
				
		//Create the fields for the event, if it is created here
		if (EventClass instanceof JDefinedClass) {
			
			JMethod initMethod = null;
			for (Entry<String, String> field : eventFields.entrySet()) {
				
				if (initMethod == null) {
					initMethod = ((JDefinedClass) EventClass).method(
							JMod.NONE, getEnvironment().getCodeModel().VOID, "init"
						);
				}
				
				final String fieldName = field.getKey();
				final AbstractJClass fieldClass = TypeUtils.classFromTypeString(field.getValue(), getEnvironment());
				
				((JDefinedClass) EventClass).field(JMod.NONE, fieldClass, fieldName);
				initMethod.param(fieldClass, fieldName);
				
				initMethod.body().invoke("init");
				initMethod.body().invoke(ref("event"), FormatsUtils.fieldToSetter(fieldName)).arg(ref(fieldName));
			}			
		}
		
	}


}
