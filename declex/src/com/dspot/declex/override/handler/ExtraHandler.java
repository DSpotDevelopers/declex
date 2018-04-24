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
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.helper.APTCodeModelHelper;
import org.androidannotations.helper.InjectHelper;
import org.androidannotations.holder.EActivityHolder;
import org.androidannotations.holder.HasIntentBuilder;
import org.androidannotations.internal.core.helper.IntentBuilder;

import com.dspot.declex.action.Actions;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.override.helper.DeclexAPTCodeModelHelper;
import com.dspot.declex.override.holder.ActivityActionHolder;
import com.dspot.declex.override.holder.FragmentActionHolder;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;
import org.androidannotations.internal.model.AnnotationElements;

public class ExtraHandler extends org.androidannotations.internal.core.handler.ExtraHandler {

	public ExtraHandler(AndroidAnnotationsEnvironment environment) {
		super(environment);
		
		codeModelHelper = new DeclexAPTCodeModelHelper(getEnvironment());
	}
	
	@Override
	public void validate(Element element, ElementValidation validation) {
		
		super.validate(element, validation);
		if (!validation.isValid()) return;

		final AnnotationElements validatedElements = getEnvironment().getValidatedElements();

		if (validatedElements.isAncestor(element)) {
						
			Set<AnnotationElements.AnnotatedAndRootElements> subClasses = validatedElements.getAncestorSubClassesElements(element);
			for (AnnotationElements.AnnotatedAndRootElements subClass : subClasses) {

				if (getEnvironment().getValidatedElements().isAncestor(subClass.rootTypeElement)) continue;

				final String subClassName = subClass.rootTypeElement.asType().toString();
				final ActionInfo activityActionInfo = Actions.getInstance().getActionInfos().get(subClassName + "ActionHolder");
				
				if (element.getKind() == ElementKind.PARAMETER) {
					FragmentActionHolder.addFragmentArg(activityActionInfo, element.getEnclosingElement(), getEnvironment());
				} else {
					FragmentActionHolder.addFragmentArg(activityActionInfo, element, getEnvironment());
				}			

			}
			
		} else {

			final Element rootElement = TypeUtils.getRootElement(element);
			final String rootElementClass = rootElement.asType().toString();
			final ActionInfo activityActionInfo = Actions.getInstance().getActionInfos().get(rootElementClass + "ActionHolder");
			
			if (element.getKind() == ElementKind.PARAMETER) {
				FragmentActionHolder.addFragmentArg(activityActionInfo, element.getEnclosingElement(), getEnvironment());
			} else {
				FragmentActionHolder.addFragmentArg(activityActionInfo, element, getEnvironment());
			}			
		}
		
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
			    
			    helperField = InjectHelper.class.getDeclaredField("codeModelHelper");
			    helperField.setAccessible(true);
			    helperField.set(injectHelper, helper);
			}
			
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}		
				
		super.process(element, holder);
		
		if (element.getKind() != ElementKind.PARAMETER) {
			ActivityActionHolder actionHolder = holder.getPluginHolder(new ActivityActionHolder(holder));
			JDefinedClass ActivityAction = actionHolder.getActivityAction();
			
			final String fieldName = element.getSimpleName().toString();		
			
			final Element paramElement;
			if (element.getKind() == ElementKind.METHOD) {
				VariableElement param = ((ExecutableElement)element).getParameters().get(0);
				paramElement = param;
			} else {
				paramElement = element;
			}
			
			final AbstractJClass clazz = codeModelHelper.elementTypeToJClass(paramElement);
			
			JMethod fieldMethod = ActivityAction.method(JMod.PUBLIC, ActivityAction, fieldName);
			JVar fieldMethodParam = fieldMethod.param(clazz, fieldName);
			fieldMethod.body().invoke(ref("builder"), fieldName).arg(fieldMethodParam);
			fieldMethod.body()._return(_this());
		}
				
	}
}
