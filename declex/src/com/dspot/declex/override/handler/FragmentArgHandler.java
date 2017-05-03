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

import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.ref;

import javax.lang.model.element.Element;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.holder.EFragmentHolder;

import com.dspot.declex.override.holder.FragmentActionHolder;
import com.dspot.declex.override.util.DeclexAPTCodeModelHelper;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.util.TypeUtils.ClassInformation;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class FragmentArgHandler extends org.androidannotations.internal.core.handler.FragmentArgHandler {

	public FragmentArgHandler(AndroidAnnotationsEnvironment environment) {
		super(environment);
		codeModelHelper = new DeclexAPTCodeModelHelper(getEnvironment());
	}

	@Override
	public void validate(Element element, ElementValidation valid) {
		validatorHelper.enclosingElementHasEFragment(element, valid);

		validatorHelper.isNotPrivate(element, valid);
	}
	
	@Override
	public void process(Element element, EFragmentHolder holder) {
		super.process(element, holder);
		
		FragmentActionHolder actionHolder = holder.getPluginHolder(new FragmentActionHolder(holder));
		JDefinedClass FragmentAction = actionHolder.getFragmentAction();
		
		ClassInformation classInformation = TypeUtils.getClassInformation(element, getEnvironment());
		final String className = classInformation.originalClassName;
		final String fieldName = element.getSimpleName().toString();
		
		JMethod fieldMethod = FragmentAction.method(JMod.PUBLIC, FragmentAction, fieldName);
		JVar fieldMethodParam = fieldMethod.param(getJClass(className), fieldName);
		fieldMethod.body().invoke(ref("builder"), fieldName).arg(fieldMethodParam);
		fieldMethod.body()._return(_this());
	}
}
