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
import java.util.List;
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
import org.androidannotations.helper.InjectHelper.ParamHelper;
import org.androidannotations.holder.EFragmentHolder;

import com.dspot.declex.action.Actions;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.override.helper.DeclexAPTCodeModelHelper;
import com.dspot.declex.override.holder.FragmentActionHolder;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.wrapper.element.VirtualElement;
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
		
		final Element rootElement = TypeUtils.getRootElement(element);
		final String rootElementClass = rootElement.asType().toString();
		
		if (filesCacheHelper.isAncestor(rootElementClass)) {
			
			Set<String> subClasses = filesCacheHelper.getAncestorSubClasses(rootElementClass);
			for (String subClass : subClasses) {
				if (filesCacheHelper.isAncestor(subClass)) continue;

				ActionInfo fragmentActionInfo = Actions.getInstance().getActionInfos().get(subClass + "ActionHolder");
				
				if (element.getKind() == ElementKind.PARAMETER) {
					FragmentActionHolder.addFragmentArg(fragmentActionInfo, element.getEnclosingElement(), getEnvironment());
				} else {
					FragmentActionHolder.addFragmentArg(fragmentActionInfo, element, getEnvironment());
				}			

			}					
			
		} else {
			ActionInfo fragmentActionInfo = Actions.getInstance().getActionInfos().get(rootElementClass + "ActionHolder");
			
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
			final TypeMirror paramType;
			if (element.getKind() == ElementKind.METHOD) {
				VariableElement param = ((ExecutableElement)element).getParameters().get(0); 
				paramType = param.asType();
				paramName = param.getSimpleName().toString();
			} else {
				paramType = element.asType();
				paramName = fieldName;
			}
			
			final AbstractJClass clazz = codeModelHelper.typeMirrorToJClass(paramType);
			
			JMethod fieldMethod = FragmentAction.method(JMod.PUBLIC, FragmentAction, fieldName);
			JVar fieldMethodParam = fieldMethod.param(clazz, paramName);
			fieldMethod.body().invoke(ref("builder"), fieldName).arg(fieldMethodParam);
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
		
		JInvocation invocation = fieldMethod.body().invoke(ref("builder"), methodName);
		for (ParamHelper paramHelper : parameterList) {
			final AbstractJClass clazz = codeModelHelper.typeMirrorToJClass(paramHelper.getParameterElement().asType());
			JVar fieldMethodParam = fieldMethod.param(clazz, paramHelper.getParameterElement().getSimpleName().toString());
			invocation = invocation.arg(fieldMethodParam);
			
		}
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
