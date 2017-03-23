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
package com.dspot.declex.runwith;

import static com.helger.jcodemodel.JExpr.ref;

import java.lang.annotation.Annotation;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.holder.EComponentHolder;

import com.dspot.declex.action.ActionHelper;
import com.dspot.declex.api.runwith.RunWith;
import com.dspot.declex.event.BaseEventHandler;
import com.dspot.declex.share.holder.ViewsHolder;
import com.dspot.declex.util.ParamUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JInvocation;

public class RunWithHandler<T extends EComponentHolder> extends BaseEventHandler<T> {
	
	protected Class<? extends Annotation> targetAnnotation;
	private ActionHelper actionHelper;
	
	public RunWithHandler(AndroidAnnotationsEnvironment environment) {
		this(RunWith.class, environment);
	}

	public RunWithHandler(Class<? extends Annotation> targetClass, AndroidAnnotationsEnvironment environment) {
		super(targetClass, environment);
		this.targetAnnotation = (Class<? extends Annotation>) targetClass;
		actionHelper = ActionHelper.getInstance(environment);
	}
	
	@Override
	public void validate(final Element element, final ElementValidation valid) {
		super.validate(element, valid);
				
		if (actionHelper.hasAction(element)) {
			return;
		} 
		
		if (element instanceof ExecutableElement) {
			return;
		} else {
			valid.addError("@" + getTarget() + " can be applied only to fields containing Actions");
		}
	}
	
	@Override
	protected IJStatement getStatement(AbstractJClass elementClass, Element element, ViewsHolder viewsHolder, T holder) {

		if (element instanceof ExecutableElement) {
			final String methodName = element.getSimpleName().toString();
			
			JInvocation invoke = JExpr.invoke(methodName);
			
			ExecutableElement exeElem = (ExecutableElement) element;
			for (VariableElement param : exeElem.getParameters()) {
				final String paramName = param.getSimpleName().toString();
				final String paramType = param.asType().toString();
				ParamUtils.injectParam(paramName, paramType, invoke, viewsHolder);
			}
			
			return invoke;
		}
		
		if (viewsHolder != null) {
			
			if (actionHelper.hasAction(element)) {
				return ref(element.getSimpleName().toString()).invoke("fire");
			};
			
		}
		
		return null;
	}

}
