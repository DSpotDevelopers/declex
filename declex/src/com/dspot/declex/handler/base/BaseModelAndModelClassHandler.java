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
package com.dspot.declex.handler.base;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.DeclaredType;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.holder.BaseGeneratedClassHolder;

import com.dspot.declex.annotation.Model;
import com.dspot.declex.annotation.UseModel;
import com.dspot.declex.util.TypeUtils;

public abstract class BaseModelAndModelClassHandler<T extends BaseGeneratedClassHolder> extends BaseTemplateHandler<T> {
	
	public BaseModelAndModelClassHandler(Class<?> targetClass,
			AndroidAnnotationsEnvironment environment, String templatePath,
			String templateName) {
		super(targetClass, environment, templatePath, templateName);
	}
	
	protected abstract Class<?> getDefaultModelClass();
	
	@Override
	protected void setTemplateDataModel(Map<String, Object> rootDataModel,
			Element element, T holder) {
		super.setTemplateDataModel(rootDataModel, element, holder);
		
		final DeclaredType modelClassType = annotationHelper.extractAnnotationClassParameter(element, getTarget(), "modelClass");
		
		String modelClass = modelClassType == null || modelClassType.toString().equals(getDefaultModelClass().getCanonicalName()) ? 
				             	"" : modelClassType.toString();
		modelClass = TypeUtils.getGeneratedClassName(modelClass, getEnvironment());		
		rootDataModel.put("modelClass", modelClass);
		
		try {
			Method modelMethod = targetAnnotation.getMethod("model", new Class[] {});
			String model = (String) modelMethod.invoke(adiHelper.getAnnotation(element, targetAnnotation), new Object[] {});
			
			rootDataModel.put("model", model);
			
			List<? extends Element> elems = element.getEnclosedElements();
			for (Element elem : elems)
				if (elem.getKind() == ElementKind.FIELD) {
					if (elem.getSimpleName().toString().equals(model)) {
						String elemClassName = TypeUtils.getGeneratedClassName(elem, getEnvironment());						
						rootDataModel.put("modelType", TypeUtils.typeFromTypeString(elemClassName, getEnvironment()));
						
						Model annotation = adiHelper.getAnnotation(elem, Model.class);
						rootDataModel.put("modelQuery", annotation.query());
						rootDataModel.put("modelOrderBy", annotation.orderBy());
						
						break;
					}
					
				}
		} catch (Exception e) {
		} 
	}

	@Override
	protected void validate(Element element, ElementValidation valid) {
		
		final DeclaredType modelClassType = annotationHelper.extractAnnotationClassParameter(element, getTarget(), "modelClass");
		if (modelClassType != null) {
			UseModel useModel = adiHelper.getAnnotation(modelClassType.asElement(), UseModel.class);
			if (useModel == null) {
				valid.addError("The provided model \"" + modelClassType + "\" is not annotated with @UseModel");
			}
		}
		
		try {
			Method modelMethod = targetAnnotation.getMethod("model", new Class[] {});
			String model = (String) modelMethod.invoke(adiHelper.getAnnotation(element, targetAnnotation), new Object[] {});
			
			//Search coinciding fields
			if (!model.equals("") && !model.equals("this")) {
				boolean modelFound = false;
				List<? extends Element> elems = element.getEnclosedElements();
				for (Element elem : elems)
					if (elem.getKind() == ElementKind.FIELD) {
						if (elem.getSimpleName().toString().equals(model)) {
							if (adiHelper.getAnnotation(elem, Model.class) == null) {
								valid.addError("The provided model field \"" + model + "\" is not annotated with @Model");
							}
							
							modelFound = true;
							break;
						}
						
					}
				
				if (!modelFound) {
					valid.addError("The provided model field \"" + model + "\" cannot be found");
				}
			}
		} catch (Exception e) {
			valid.addError("Annotation implementation does not includes a \"model\" method");
		} 
	}

}
