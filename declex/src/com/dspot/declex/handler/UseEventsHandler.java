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

import static com.dspot.declex.api.util.FormatsUtils.fieldToGetter;
import static com.dspot.declex.api.util.FormatsUtils.fieldToSetter;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.EBean;
import org.androidannotations.holder.BaseGeneratedClassHolder;
import org.androidannotations.holder.EBeanHolder;

import com.dspot.declex.annotation.UseEvents;
import com.dspot.declex.handler.base.BaseTemplateHandler;
import com.dspot.declex.util.SharedRecords;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class UseEventsHandler extends BaseTemplateHandler<EBeanHolder> {

	public UseEventsHandler(AndroidAnnotationsEnvironment environment) {
		super(UseEvents.class, environment,
				 "com/dspot/declex/template/", "UseEvents.ftl.java");
	}
	
	@Override
	public void getDependencies(Element element, Map<Element, Object> dependencies) {
		dependencies.put(element, EBean.class);
	}

	@Override
	public void validate(Element element, ElementValidation valid) {
		validatorHelper.typeHasAnnotation(EBean.class, element, valid);
		
		if (valid.isValid()) {
			SharedRecords.addEventGeneratedClass(element.toString(), getEnvironment());
		}
	}

	@Override
	public void process(Element element, EBeanHolder holder) {
		super.process(element, holder);
		
		Map<String, Element> fields = new HashMap<>();
		Map<String, Element> methods = new HashMap<>();
		
		//Get all the fields and methods
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems) {
			final String elemName = elem.getSimpleName().toString();
			
			if (elem.getModifiers().contains(Modifier.STATIC)) continue;
			if (elem.getModifiers().contains(Modifier.PRIVATE)) continue;
			
			if (elem.getKind() == ElementKind.FIELD) {
				fields.put(elemName, elem);
			}
			
			if (elem.getKind() == ElementKind.METHOD) {
				methods.put(elemName, elem);
			}
		}
		
		generateGetterAndSetters(holder, fields, methods);
		
		if (fields.size() > 0) {
			JMethod create = holder.getGeneratedClass().method(JMod.PUBLIC | JMod.STATIC, holder.getGeneratedClass(), "create");
			JVar instance = create.body().decl(holder.getGeneratedClass(), "instance", JExpr.invoke("create"));
			
			for (String elemName : fields.keySet()) {
				AbstractJClass elemClass = codeModelHelper.elementTypeToJClass(fields.get(elemName));
				JVar param = create.param(elemClass, elemName);
				create.body().invoke(instance, fieldToSetter(elemName)).arg(param);
			}
			
			create.body()._return(instance);
		}
	}
	
	private void generateGetterAndSetters(BaseGeneratedClassHolder holder, Map<String, Element> fields, Map<String, Element> methods) {
		
		AbstractJClass EventClass = holder.getGeneratedClass();
		
		//Generate getter and setters for Column annotated elements
		for (String elemName : fields.keySet()) {
			AbstractJClass elemClass = codeModelHelper.elementTypeToJClass(fields.get(elemName));
			
			String getterName = fieldToGetter(elemName);
			
			//Generate Getter
			if (!methods.containsKey(getterName)) {
				JBlock getterBody = holder.getGeneratedClass().method(
						JMod.PUBLIC, 
						elemClass, 
						getterName
					).body();
				
				getterBody._return(_this().ref(elemName));
			}

			String setterName = fieldToSetter(elemName);

			//Generate Setter
			if (!methods.containsKey(setterName)) {
				JMethod setter = holder.getGeneratedClass().method(
						JMod.PUBLIC, 
						EventClass, 
						setterName
					);
				setter.param(elemClass, elemName);
				
				setter.body().assign(_this().ref(elemName), ref(elemName));
				setter.body()._return(_this());
			}
		}
	}

}
