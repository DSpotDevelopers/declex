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
package com.dspot.declex.override.handler;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.holder.EComponentWithViewSupportHolder;

import com.dspot.declex.action.ActionsProcessor;
import com.dspot.declex.share.holder.ViewsHolder;
import com.dspot.declex.util.ParamUtils;
import com.dspot.declex.util.SharedRecords;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JInvocation;

public class AfterViewsHandler extends org.androidannotations.internal.core.handler.AfterViewsHandler {

	private static int uniquePriorityCounter = 100;
	
	public AfterViewsHandler(AndroidAnnotationsEnvironment environment) {
		super(environment);
	}
	
	@Override
	public void validate(Element element, ElementValidation valid) {
		validatorHelper.enclosingElementHasEnhancedViewSupportAnnotation(element, valid);

		ExecutableElement executableElement = (ExecutableElement) element;

		validatorHelper.returnTypeIsVoid(executableElement, valid);

		validatorHelper.isNotPrivate(element, valid);

		validatorHelper.doesntThrowException(executableElement, valid);
		
		ActionsProcessor.validateActions(element, valid, getEnvironment());
	}

	@Override
	public void process(Element element, EComponentWithViewSupportHolder holder) throws Exception {
		
		ActionsProcessor.processActions(element, holder);
		
		uniquePriorityCounter++;
		
		final ViewsHolder viewsHolder = holder.getPluginHolder(new ViewsHolder(holder, annotationHelper));
		final String methodName = element.getSimpleName().toString();
		
		JBlock block = new JBlock();
		JInvocation invoke = block.invoke(methodName);
		
		ExecutableElement exeElem = (ExecutableElement) element;
		for (VariableElement param : exeElem.getParameters()) {
			final String paramName = param.getSimpleName().toString();
			ParamUtils.injectParam(paramName, invoke, viewsHolder);
		}
		
		SharedRecords.priorityAdd(holder.getOnViewChangedBody(), block, uniquePriorityCounter);
	}	
}
