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
package com.dspot.declex.action;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.EBean;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.holder.EComponentWithViewSupportHolder;

import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.override.util.OverrideAPTCodeModelHelper;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.TypeUtils;

public class ActionForHandler extends BaseAnnotationHandler<EComponentWithViewSupportHolder> {
	
	public ActionForHandler(AndroidAnnotationsEnvironment environment) {
		super(ActionFor.class, environment);
		
		codeModelHelper = new OverrideAPTCodeModelHelper(getEnvironment());
	}
	
	@Override
	public Set<Class<? extends Annotation>> getDependencies() {
		return new HashSet<>(Arrays.<Class<? extends Annotation>>asList(
					EBean.class
			   ));
	}
	
	@Override
	protected void validate(Element element, ElementValidation valid) {
		
		//Actions depends on Action Holders
		filesCacheHelper.addGeneratedClass(DeclexConstant.ACTION, element);
		
		try {
			//Mark the Cache of this file as action, to add action objects after generation
			filesCacheHelper.getFileDetails(element.asType().toString()).isAction = true;
		} catch (Throwable e){}
		
		boolean initFound = false;
		boolean buildFound = false;
		boolean executeFound = false;
		
		Element elementWithBuild = null;
		
		Element superElement = element;
		validation: while (superElement != null) {
			for (Element elem : superElement.getEnclosedElements()) {
				
				if (elem instanceof ExecutableElement) {
					
					ExecutableElement executableElement = (ExecutableElement) elem;
									
					if (executableElement.getSimpleName().toString().equals("init")) {
						if (!executableElement.getReturnType().toString().equals("void")) {
							valid.addError(elem, "\"init\" method of the Action Holder should not return a value");
						}
						
						if (executableElement.getModifiers().contains(Modifier.PUBLIC)) {
							valid.addError(elem, "\"init\" method of the Action Holder should not be \"public\"");
						}
						
						if (executableElement.getModifiers().contains(Modifier.PRIVATE)) {
							valid.addError(elem, "\"init\" method of the Action Holder should not be \"private\"");
						}
	
						initFound = true;
					}
					
					if (executableElement.getSimpleName().toString().equals("build")) {
						
						if (elem == elementWithBuild) {
							valid.addError(elem, "Only one \"build\" method is permitted inside Action Holders");
						}
						
						if (!executableElement.getReturnType().toString().equals("void")) {
							valid.addError(elem, "\"build\" method of the Action Holder should not return a value");
						}
						
						if (executableElement.getParameters().isEmpty()) {
							valid.addError(elem, "\"build\" method of the Action Holder should take at least one parameters");
						}
						
						for (VariableElement param : executableElement.getParameters()) {
						
							boolean isRunnable = false;
							
							if (!param.asType().getKind().isPrimitive()) {
								if (TypeUtils.isSubtype(param, Runnable.class.getCanonicalName(), getProcessingEnvironment())) {
									isRunnable = true;
								}						
							}
							
							if (!isRunnable) {
								valid.addError(
										param, 
										"Parameter \"" + param.getSimpleName() 
										+ "\" of the \"build\" method of the Action Holder should implemente Runnable interface"
									);
							}
							
						}
						
						if (executableElement.getModifiers().contains(Modifier.PUBLIC)) {
							valid.addError(elem, "\"build\" method of the Action Holder should not be \"public\"");
						}

						if (executableElement.getModifiers().contains(Modifier.PRIVATE)) {
							valid.addError(elem, "\"build\" method of the Action Holder should not be \"private\"");
						}

						buildFound = true;
						elementWithBuild = elem;
					}
					
					if (executableElement.getSimpleName().toString().equals("execute")) {
						if (!executableElement.getReturnType().toString().equals("void")) {
							valid.addError(elem, "\"execute\" method of the Action Holder should not return a value");
						}
						
						if (!executableElement.getParameters().isEmpty()) {
							valid.addError(elem, "\"execute\" method of the Action Holder should not take parameters");
						}
						
						if (executableElement.getModifiers().contains(Modifier.PUBLIC)) {
							valid.addError(elem, "\"execute\" method of the Action Holder should not be \"public\"");
						}
						
						if (executableElement.getModifiers().contains(Modifier.PRIVATE)) {
							valid.addError(elem, "\"execute\" method of the Action Holder should not be \"private\"");
						}
						
						executeFound = true;
					}
					
				}
			}
			
			List<? extends TypeMirror> superTypes = getProcessingEnvironment().getTypeUtils().directSupertypes(superElement.asType());
			for (TypeMirror type : superTypes) {
				superElement = getProcessingEnvironment().getElementUtils().getTypeElement(type.toString());
				if (superElement.getKind().equals(ElementKind.INTERFACE)) continue;
				if (superElement.asType().toString().equals(Object.class.getCanonicalName())) continue;
				continue validation;
			}
			
			superElement = null;
		}

		
		if (!initFound) {
			valid.addError(element, "The Action Holder should implement a method \"init\". Ex. \"void init(){}\"");
		}
		
		if (!buildFound) {
			valid.addError(element, "The Action Holder should implement a method \"build\". Ex. \"void build(Runnable Started){}\"");
		}

		if (!executeFound) {
			valid.addError(element, "The Action Holder should implement a method \"execute\". Ex. \"void execute(){}\"");
		}
		
		if (valid.isValid()) {
			Actions.getInstance().addActionHolder(element.asType().toString());
		}
	}
	
	@Override
	public void process(Element element, EComponentWithViewSupportHolder holder) {
		
		overrideMethods(element, holder, null);
		
		Actions.getInstance().getActionInfos().get(element.asType().toString()).actionForHolder = holder;
		
	}

	private void overrideMethods(Element element, EComponentWithViewSupportHolder holder, 
								 List<String> overrideMethods) {
		
		if (overrideMethods == null) {
			overrideMethods = new LinkedList<>();
		}
		
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems) {
			if (elem.getKind() == ElementKind.METHOD) {
				if (overrideMethods.contains(elem.toString())) continue;
				if (elem.getModifiers().isEmpty() || elem.getModifiers().contains(Modifier.PROTECTED)) {
					codeModelHelper.overrideAnnotatedMethod((ExecutableElement) elem, holder);
					overrideMethods.add(elem.toString());
				}
			}
		}
		
		List<? extends TypeMirror> superTypes = getProcessingEnvironment().getTypeUtils().directSupertypes(element.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = getProcessingEnvironment().getElementUtils().getTypeElement(type.toString());
			if (superElement.getKind().equals(ElementKind.INTERFACE)) continue;
			if (superElement.asType().toString().equals(Object.class.getCanonicalName())) continue;
			overrideMethods(superElement, holder, overrideMethods);
		}

	}
	
}
