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

import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr.cond;
import static com.helger.jcodemodel.JExpr.lit;
import static com.helger.jcodemodel.JExpr.ref;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.holder.EBeanHolder;

import com.dspot.declex.annotation.AfterPut;
import com.dspot.declex.annotation.JsonModel;
import com.dspot.declex.annotation.LocalDBModel;
import com.dspot.declex.annotation.ServerModel;
import com.dspot.declex.annotation.UseModel;
import com.dspot.declex.annotation.modifier.ModelParams;
import com.dspot.declex.holder.UseModelHolder;
import com.dspot.declex.util.ParamUtils;
import com.dspot.declex.util.SharedRecords;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;

public class AfterPutHandler extends BaseAnnotationHandler<EBeanHolder> {

	public AfterPutHandler(AndroidAnnotationsEnvironment environment) {
		super(AfterPut.class, environment);
	}

	@Override
	public void getDependencies(Element element,
			Map<Element, Object> dependencies) {
		dependencies.put(element.getEnclosingElement(), UseModel.class);
	}
	
	@Override
	public void validate(Element element, ElementValidation valid) {
		
		ExecutableElement executableElement = (ExecutableElement) element;

		validatorHelper.returnTypeIsVoid(executableElement, valid);

		validatorHelper.isNotPrivate(element, valid);

		validatorHelper.doesntThrowException(executableElement, valid);		
	}

	@Override
	public void process(Element element, EBeanHolder holder) {
		final ExecutableElement afterPutMethod = (ExecutableElement) element;
		final UseModelHolder useModelHolder = holder.getPluginHolder(new UseModelHolder(holder));
		useModelHolder.setAfterPutMethod(afterPutMethod);
		
		List<Class<? extends Annotation>> annotations = Arrays.asList(UseModel.class, JsonModel.class, LocalDBModel.class, ServerModel.class);
		for (Class<? extends Annotation> annotation : annotations) {
			if (adiHelper.hasAnnotation(element, annotation)) return;
		}
		
		Set<String> modelParams = new HashSet<>();
		for (Class<? extends Annotation> annotation : annotations) {
			ModelParams modelParamsAnnotation = annotation.getDeclaredAnnotation(ModelParams.class);
			for (String value : modelParamsAnnotation.value()) {
				modelParams.add(value);
			}
		}
		
		List<? extends VariableElement> parameters = afterPutMethod.getParameters();
		
		JBlock putModel = new JBlock();
		JInvocation invocation = putModel._if(ref("result").ne(_null()))._then()
				                         .invoke(afterPutMethod.getSimpleName().toString());
		
		JFieldRef args = ref("args");
		for (VariableElement param : parameters) {
			final String paramName = param.getSimpleName().toString();
			final String paramType = param.asType().toString();	
			
			if (modelParams.contains(paramName) && String.class.getCanonicalName().equals(paramType)) {
				invocation.arg(cond(
					args.eqNull().cor(args.invoke("containsKey").arg(paramName).not()), 
					lit(""), args.invoke("get").arg(paramName)
				));
				continue;
			}
			
			ParamUtils.injectParam(paramName, paramType, invocation);
		}
		
		SharedRecords.priorityAdd(
				useModelHolder.getPutModelInitBlock(), 
				putModel, 
				10000
			);
	}
}
