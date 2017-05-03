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

import java.lang.reflect.Field;

import javax.lang.model.element.Element;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.helper.APTCodeModelHelper;
import org.androidannotations.holder.EActivityHolder;
import org.androidannotations.holder.HasIntentBuilder;
import org.androidannotations.internal.core.helper.IntentBuilder;

import com.dspot.declex.override.holder.ActivityActionHolder;
import com.dspot.declex.override.util.DeclexAPTCodeModelHelper;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.util.TypeUtils.ClassInformation;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class ExtraHandler extends org.androidannotations.internal.core.handler.ExtraHandler {

	public ExtraHandler(AndroidAnnotationsEnvironment environment) {
		super(environment);
		
		codeModelHelper = new DeclexAPTCodeModelHelper(getEnvironment());
	}

	@Override
	public void validate(Element element, ElementValidation valid) {
		validatorHelper.enclosingElementHasEActivity(element, valid);

		validatorHelper.isNotPrivate(element, valid);
	}
	
	@Override
	public void process(Element element, EActivityHolder holder) {
		try {
			
			if (holder instanceof HasIntentBuilder) {
				APTCodeModelHelper helper = new DeclexAPTCodeModelHelper(getEnvironment());
				helper.getActualType(element, holder);
				
			    IntentBuilder builder = ((HasIntentBuilder) holder).getIntentBuilder();
			    
			    Field helperField = IntentBuilder.class.getDeclaredField("codeModelHelper");
			    helperField.setAccessible(true);
			    helperField.set(builder, helper);
			}
			
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}		
				
		super.process(element, holder);
		
		ActivityActionHolder actionHolder = holder.getPluginHolder(new ActivityActionHolder(holder));
		JDefinedClass ActivityAction = actionHolder.getActivityAction();
		
		ClassInformation classInformation = TypeUtils.getClassInformation(element, getEnvironment());
		final String className = classInformation.originalClassName;
		final String fieldName = element.getSimpleName().toString();
		
		JMethod fieldMethod = ActivityAction.method(JMod.PUBLIC, ActivityAction, fieldName);
		JVar fieldMethodParam = fieldMethod.param(getJClass(className), fieldName);
		fieldMethod.body().invoke(ref("builder"), fieldName).arg(fieldMethodParam);
		fieldMethod.body()._return(_this());
	}
}
