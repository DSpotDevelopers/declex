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
package com.dspot.declex.action;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.EBean;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.holder.BaseGeneratedClassHolder;

import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.override.util.DeclexAPTCodeModelHelper;

public class ActionForHandler extends BaseAnnotationHandler<BaseGeneratedClassHolder> {
	
	public ActionForHandler(AndroidAnnotationsEnvironment environment) {
		super(ActionFor.class, environment);
		
		codeModelHelper = new DeclexAPTCodeModelHelper(getEnvironment());
	}
	
	@Override
	public Set<Class<? extends Annotation>> getDependencies() {
		return new HashSet<>(Arrays.<Class<? extends Annotation>>asList(
					EBean.class
			   ));
	}
	
	@Override
	protected void validate(Element element, ElementValidation valid) {
		Actions.getInstance().addAction(element.asType().toString());
	}
	
	@Override
	public void process(Element element, BaseGeneratedClassHolder holder) {
		
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems) {
			if (elem.getKind() == ElementKind.METHOD) {
				if (elem.getModifiers().isEmpty() || elem.getModifiers().contains(Modifier.PROTECTED)) {
					codeModelHelper.overrideAnnotatedMethod((ExecutableElement) elem, holder);
				}
			}
		}		
	}

}
