/**
 * Copyright (C) 2016-2019 DSpot Sp. z o.o
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
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.ref;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.helper.APTCodeModelHelper;
import org.androidannotations.helper.InjectHelper;
import org.androidannotations.helper.InjectHelper.ParamHelper;
import org.androidannotations.holder.EFragmentHolder;

import com.dspot.declex.action.Actions;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.override.helper.DeclexAPTCodeModelHelper;
import com.dspot.declex.override.holder.FragmentActionHolder;
import com.dspot.declex.util.TypeUtils;
import org.androidannotations.internal.model.AnnotationElements;
import org.androidannotations.internal.virtual.VirtualElement;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class FragmentArgHandler extends org.androidannotations.internal.core.handler.FragmentArgHandler {

	public FragmentArgHandler(AndroidAnnotationsEnvironment environment) {
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

				if (validatedElements.isAncestor(subClass.rootTypeElement)) continue;

				final String subClassName = subClass.rootTypeElement.asType().toString();
				final ActionInfo fragmentActionInfo = Actions.getInstance().getActionInfos().get(subClassName + "ActionHolder");

				if (element.getKind() == ElementKind.PARAMETER) {
					FragmentActionHolder.addFragmentArg(fragmentActionInfo, element.getEnclosingElement(), getEnvironment());
				} else {
					FragmentActionHolder.addFragmentArg(fragmentActionInfo, element, getEnvironment());
				}			

			}					
			
		} else {

			final Element rootElement = TypeUtils.getRootElement(element);
			final String rootElementClass = rootElement.asType().toString();
			final ActionInfo fragmentActionInfo = Actions.getInstance().getActionInfos().get(rootElementClass + "ActionHolder");
			
			if (element.getKind() == ElementKind.PARAMETER) {
				FragmentActionHolder.addFragmentArg(fragmentActionInfo, element.getEnclosingElement(), getEnvironment());
			} else {
				FragmentActionHolder.addFragmentArg(fragmentActionInfo, element, getEnvironment());
			}			
		}
		
		
	}
	
	@Override
	public void process(Element element, EFragmentHolder holder) {
		
		try {
			
			APTCodeModelHelper helper = new DeclexAPTCodeModelHelper(getEnvironment());
		    Field helperField = InjectHelper.class.getDeclaredField("codeModelHelper");
		    helperField.setAccessible(true);
		    helperField.set(injectHelper, helper);
			
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}

		super.process(element, holder);
		
		if (element.getKind() != ElementKind.PARAMETER) {
			FragmentActionHolder actionHolder = holder.getPluginHolder(new FragmentActionHolder(holder));
			JDefinedClass FragmentAction = actionHolder.getFragmentAction();	
			
			final String fieldName = element.getSimpleName().toString();
			
			final String paramName;
			final Element paramElement;
			if (element.getKind() == ElementKind.METHOD) {
				VariableElement param = ((ExecutableElement)element).getParameters().get(0); 
				paramElement = param;
				paramName = param.getSimpleName().toString();
			} else {
				paramElement = element;
				paramName = fieldName;
			}
			
			final AbstractJClass clazz = codeModelHelper.elementTypeToJClass(paramElement);
			
			JMethod fieldMethod = FragmentAction.method(JMod.PUBLIC, FragmentAction, fieldName);
			JVar fieldMethodParam = fieldMethod.param(clazz, paramName);
			fieldMethod.body().add(invoke(ref("builder"), fieldName).arg(fieldMethodParam));
			fieldMethod.body()._return(_this());			
		}
	}
	
	@Override
	public void afterAllParametersInjected(EFragmentHolder holder,
			ExecutableElement method, List<ParamHelper> parameterList) {
		
		FragmentActionHolder actionHolder = holder.getPluginHolder(new FragmentActionHolder(holder));
		JDefinedClass FragmentAction = actionHolder.getFragmentAction();	
		
		final String methodName = method.getSimpleName().toString();	
		JMethod fieldMethod = FragmentAction.method(JMod.PUBLIC, FragmentAction, methodName);
		
		JInvocation invocation = invoke(ref("builder"), methodName);
		for (ParamHelper paramHelper : parameterList) {
			final AbstractJClass clazz = codeModelHelper.elementTypeToJClass(paramHelper.getParameterElement());
			JVar fieldMethodParam = fieldMethod.param(clazz, paramHelper.getParameterElement().getSimpleName().toString());
			invocation = invocation.arg(fieldMethodParam);
			
		}

		fieldMethod.body().add(invocation);
		fieldMethod.body()._return(_this());
		
		super.afterAllParametersInjected(holder, method, parameterList);
	}
	
	@Override
	public void createBuilderInjectMethod(EFragmentHolder holder,
			Element element, List<ArgHelper> argHelpers) {
		
		if (element instanceof VirtualElement) {
			super.createBuilderInjectMethod(holder, ((VirtualElement) element).getElement(), argHelpers);	
		} else {
			super.createBuilderInjectMethod(holder, element, argHelpers);
		}
	
	}
}
