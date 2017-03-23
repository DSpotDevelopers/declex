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
package com.dspot.declex.model;

import static com.dspot.declex.api.util.FormatsUtils.fieldToGetter;
import static com.dspot.declex.api.util.FormatsUtils.fieldToSetter;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.ref;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.EBean;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.holder.BaseGeneratedClassHolder;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;

import com.dspot.declex.api.extension.Extension;
import com.dspot.declex.api.localdb.LocalDBModel;
import com.dspot.declex.api.model.Model;
import com.dspot.declex.api.model.UseModel;
import com.dspot.declex.api.runwith.RunWith;
import com.dspot.declex.api.server.ServerModel;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;

public class UseModelHandler extends BaseAnnotationHandler<BaseGeneratedClassHolder> {

	protected static final Logger LOGGER = LoggerFactory.getLogger(UseModelHandler.class);
	
	public UseModelHandler(AndroidAnnotationsEnvironment environment) {
		super(UseModel.class, environment);
	}
	
	@Override
	public Set<Class<? extends Annotation>> getDependencies() {
		return new HashSet<>(Arrays.<Class<? extends Annotation>>asList(
					EBean.class
			   ));
	}
	
	@Override
	public void validate(Element element, ElementValidation valid) {		
		if (element.getKind().isField()) {
			Model annotated = element.getAnnotation(Model.class);
			if (annotated == null) {
				valid.addError("You can only apply this annotation in a field annotated with @Model");
			}
			
			return;
		}
		
		//Validate special methods
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems)
			if (elem.getKind() == ElementKind.METHOD) {
				ExecutableElement executableElement = (ExecutableElement) elem;
				
				if (executableElement.getSimpleName().toString().equals("afterLoad") || 
					executableElement.getSimpleName().toString().equals("afterPut")) {
					
					validatorHelper.returnTypeIsVoid(executableElement, valid);					
				}
				
			}
	}

	private void getFieldsAndMethods(Map<String, String> fields, Map<String, String> methods, TypeElement element) {
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems) {
			final String elemName = elem.getSimpleName().toString();
			final String elemType = elem.asType().toString();
			
			if (elem.getModifiers().contains(Modifier.STATIC)) continue;

			//Omit specials and private fields
			if (elem.getAnnotation(RunWith.class) != null) continue;
			if (elem.getAnnotation(ServerModel.class) != null) continue;
			if (elem.getAnnotation(LocalDBModel.class) != null) continue;
			if (elem.getAnnotation(UseModel.class) != null) continue;
			
			if (elem.getKind() == ElementKind.FIELD) {
				if (elem.getModifiers().contains(Modifier.PRIVATE)) continue;
				
				fields.put(elemName, TypeUtils.typeFromTypeString(elemType, getEnvironment()));
			}
			
			if (elem.getKind() == ElementKind.METHOD) {
				methods.put(elemName, elemType);
			}
		}
		
		//Apply to Extensions
		List<? extends TypeMirror> superTypes = getProcessingEnvironment().getTypeUtils().directSupertypes(element.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = getProcessingEnvironment().getElementUtils().getTypeElement(type.toString());
			
			if (superElement.getAnnotation(Extension.class) != null) {
				getFieldsAndMethods(fields, methods, superElement);
			}
			
			break;
		}
	}
	
	@Override
	public void process(Element element, BaseGeneratedClassHolder holder) {
		if (element.getKind().isField()) return;
		
		Map<String, String> fields = new HashMap<String, String>();
		Map<String, String> methods = new HashMap<String, String>();		
		getFieldsAndMethods(fields, methods, (TypeElement)element);
		
		UseModel useModel = adiHelper.getAnnotation(element, UseModel.class);
		if (useModel.debug())
			LOGGER.warn("\nFields: " + fields + "\nMethods: " + methods, element, useModel);
		
		generateGetterAndSetters(holder, fields, methods);
		
		UseModelHolder useModelHolder = holder.getPluginHolder(new UseModelHolder(holder));
		useModelHolder.getConstructorMethod();
		
		useModelHolder.getWriteObjectMethod();
		useModelHolder.getReadObjectMethod();
	}
	
	private void generateGetterAndSetters(BaseGeneratedClassHolder holder, Map<String, String> fields, Map<String, String> methods) {
		//Generate getter and setters 
		for (String elemName : fields.keySet()) {
			String elemType = fields.get(elemName);
			AbstractJClass elemClass = TypeUtils.classFromTypeString(elemType, getEnvironment());
			
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
			
			if (elemType.equals("boolean")) {
				getterName = "is" + getterName.substring(3);
				if (!methods.containsKey(getterName)) {
					JBlock getterBody = holder.getGeneratedClass().method(
							JMod.PUBLIC, 
							elemClass, 
							getterName
						).body();
					
					getterBody._return(_this().ref(elemName));
				}
			}

			String setterName = fieldToSetter(elemName);

			//Generate Setter
			if (!methods.containsKey(setterName)) {
				JMethod setter = holder.getGeneratedClass().method(
						JMod.PUBLIC, 
						getCodeModel().VOID, 
						setterName
					);
				setter.param(elemClass, elemName);
				
				setter.body().assign(_this().ref(elemName), ref(elemName));
			}
		}
	}

}
