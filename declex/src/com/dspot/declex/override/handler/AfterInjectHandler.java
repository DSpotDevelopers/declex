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
package com.dspot.declex.override.handler;

import static com.helger.jcodemodel.JExpr.invoke;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.holder.EComponentHolder;

import com.dspot.declex.util.ParamUtils;
import com.helger.jcodemodel.JInvocation;

public class AfterInjectHandler extends org.androidannotations.internal.core.handler.AfterInjectHandler {

	public AfterInjectHandler(AndroidAnnotationsEnvironment environment) {
		super(environment);
	}
	
	@Override
	public void validate(Element element, ElementValidation valid) {
		validatorHelper.enclosingElementHasEnhancedViewSupportAnnotation(element, valid);

		ExecutableElement executableElement = (ExecutableElement) element;

		validatorHelper.returnTypeIsVoid(executableElement, valid);

		validatorHelper.isNotPrivate(element, valid);

		validatorHelper.doesntThrowException(executableElement, valid);
	}

	@Override
	public void process(Element element, EComponentHolder holder) {
		
		final String methodName = element.getSimpleName().toString();		
		JInvocation invoke = invoke(methodName);
		
		ExecutableElement exeElem = (ExecutableElement) element;
		for (VariableElement param : exeElem.getParameters()) {
			final String paramName = param.getSimpleName().toString();
			ParamUtils.injectParam(paramName, invoke);
		}
		
		holder.getInitBodyAfterInjectionBlock().add(invoke);
	}	
}
