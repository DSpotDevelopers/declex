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
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.holder.EBeanHolder;

import com.dspot.declex.annotation.LocalDBTransaction;
import com.dspot.declex.override.helper.DeclexAPTCodeModelHelper;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JTryBlock;

public class LocalDBTransactionHandler extends BaseAnnotationHandler<EBeanHolder> {

	public LocalDBTransactionHandler(AndroidAnnotationsEnvironment environment) {
		super(LocalDBTransaction.class, environment);
		codeModelHelper = new DeclexAPTCodeModelHelper(environment);
	}

	@Override
	public void validate(Element element, ElementValidation valid) {		
		validatorHelper.isNotPrivate(element, valid);
	}

	@Override
	public void process(Element element, EBeanHolder holder) {
		ExecutableElement executableElement = (ExecutableElement) element;

		JMethod delegatingMethod = codeModelHelper.overrideAnnotatedMethod(executableElement, holder);

		JBlock previousMethodBody = codeModelHelper.removeBody(delegatingMethod);

		AbstractJClass ActiveAndroid = getJClass("com.activeandroid.ActiveAndroid");
		
		delegatingMethod.body().staticInvoke(ActiveAndroid, "beginTransaction");
		
		JTryBlock tryBlock = delegatingMethod.body()._try();
		tryBlock.body().add(previousMethodBody);
		tryBlock.body().staticInvoke(ActiveAndroid, "setTransactionSuccessful");
		
		JBlock finallyBlock = tryBlock._finally();
		finallyBlock.staticInvoke(ActiveAndroid, "endTransaction");
	}
}
