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
package com.dspot.declex.handler;

import static com.helger.jcodemodel.JExpr._new;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.holder.EComponentHolder;

import com.dspot.declex.annotation.JsonModel;
import com.dspot.declex.annotation.Model;
import com.dspot.declex.annotation.SerializeCondition;
import com.dspot.declex.annotation.UseModel;
import com.dspot.declex.handler.base.BaseTemplateHandler;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.util.TypeUtils.ClassInformation;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class JsonModelHandler extends BaseTemplateHandler<EComponentHolder> {
	
	public JsonModelHandler(AndroidAnnotationsEnvironment environment) {
		super(JsonModel.class, environment, 
				"com/dspot/declex/template/", "JsonModel.ftl.java");
	}
	
	@Override
	public void getDependencies(Element element, Map<Element, Object> dependencies) {
		if (element.getKind().equals(ElementKind.CLASS)) {
			dependencies.put(element, UseModel.class);
		} else {
			dependencies.put(element, Model.class);
		}
	}
	
	@Override
	protected void setTemplateDataModel(Map<String, Object> rootDataModel,
			Element element, EComponentHolder holder) {
		super.setTemplateDataModel(rootDataModel, element, holder);
		
		List<String> fieldsName = new LinkedList<>();
		getFieldsName((TypeElement) element, fieldsName);
		
		Map<String, String> serializeConditions = new HashMap<>();
		Map<String, ClassInformation> jsonSerializedModels = new HashMap<>();
		
		getRequiredMaps((TypeElement) element, fieldsName, serializeConditions, jsonSerializedModels);
		
				
		rootDataModel.put("serializeConditions", serializeConditions);
		rootDataModel.put("jsonSerializedModels", jsonSerializedModels);
	}	

	private void getFieldsName(TypeElement element, List<String> fieldsName) {
		
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems) {
			final String elemName = elem.getSimpleName().toString();
			
			if (elem.getModifiers().contains(Modifier.STATIC)) continue;
			
			if (fieldsName != null) {
				if (elem.getKind() == ElementKind.FIELD) {
					if (elem.getModifiers().contains(Modifier.PRIVATE)) continue;
					
					fieldsName.add(elemName);
				}					
			}
			
		}
		
		//Apply to Extensions
		List<? extends TypeMirror> superTypes = getProcessingEnvironment().getTypeUtils().directSupertypes(element.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = getProcessingEnvironment().getElementUtils().getTypeElement(type.toString());
			if (superElement == null) continue;
			
			if (adiHelper.hasAnnotation(superElement, UseModel.class)) {
				getFieldsName(superElement, fieldsName);
			}
			
			break;
		}
	}

	private void getRequiredMaps(
			TypeElement element, 
			List<String> fieldsName, 
			Map<String, String> serializeConditions,
			Map<String, ClassInformation> jsonSerializedModels) {
		
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems) {
			final String elemName = elem.getSimpleName().toString();
			
			if (elem.getModifiers().contains(Modifier.STATIC)) continue;
			
			if (elem.getKind() == ElementKind.FIELD) {
				if (elem.getModifiers().contains(Modifier.PRIVATE)) continue;
				
				SerializeCondition serializeCondition = elem.getAnnotation(SerializeCondition.class);
				if (serializeCondition != null) {
					String cond = "!(" + serializeCondition.value() + ")";
					for (String clsField : fieldsName) {
						String prevCond = new String(cond);
						cond = cond.replaceAll("(?<!\\w)(this\\.)*"+clsField+"(?!\\w)", "inst." + clsField);
						
						if (!cond.equals(prevCond) && !cond.startsWith("inst != null")) {
							cond = "inst != null && " + cond;
						}
					}
					
					serializeConditions.put(elemName, cond);
				}
				
				ClassInformation classInformation = TypeUtils.getClassInformation(elem, getEnvironment(), true);				
				if (classInformation.generatorElement != null) {
					if (adiHelper.hasAnnotation(classInformation.generatorElement, JsonModel.class)) {
						jsonSerializedModels.put(elemName, classInformation);
					}
				}
			}			
		}
		
		//Apply to Extensions
		List<? extends TypeMirror> superTypes = getProcessingEnvironment().getTypeUtils().directSupertypes(element.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = getProcessingEnvironment().getElementUtils().getTypeElement(type.toString());
			if (superElement == null) continue;
			
			if (adiHelper.hasAnnotation(superElement, UseModel.class)) {
				getRequiredMaps(superElement, fieldsName, serializeConditions, jsonSerializedModels);
			}
			
			break;
		}
	}

	@Override
	protected void validate(Element element, ElementValidation validation) {
	}
	
	@Override
	public void process(Element element, EComponentHolder holder) {
		
		if (element instanceof ExecutableElement) return;
		
		super.process(element, holder);
		
		boolean callSuperCreateGetGsonBuilderMethod = false;
		boolean createGetGsonBuilderMethod = true;
		
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems) {
			if (elem.getAnnotation(JsonModel.class) == null) continue;
			
			if (elem.getKind() == ElementKind.METHOD) {
				ExecutableElement executableElement = (ExecutableElement) elem;
				
				//TODO validate it should not be private, and it should be static
				
				if (elem.getSimpleName().toString().equals("getGsonBuilder")) {
					
					List<? extends VariableElement> params = executableElement.getParameters();
					if (params.isEmpty()) {
						//This means the builder is returned by this method (TODO validate this)
						createGetGsonBuilderMethod = false;
					} else {
						//This means the current builder should be passed as parameter (TODO validate this)
						callSuperCreateGetGsonBuilderMethod = true;
					}
				}
			}
		}
		
		createGetGsonBuilderMethod(createGetGsonBuilderMethod, callSuperCreateGetGsonBuilderMethod, holder);
		
	}

	private void createGetGsonBuilderMethod(boolean createGetGsonBuilderMethod, 
			boolean callSuperCreateGetGsonBuilderMethod, EComponentHolder holder) {
		
		AbstractJClass GsonBuilder = getJClass("com.google.gson.GsonBuilder");
		
		JMethod getGsonBuilderMethod = 
				holder.getGeneratedClass().method(JMod.PRIVATE | JMod.STATIC, GsonBuilder, "getGsonBuilder");
		JVar inst = getGsonBuilderMethod.param(holder.getGeneratedClass(), "inst");
		JVar fields = getGsonBuilderMethod.param(getClasses().STRING, "fields");
		
		JBlock body = getGsonBuilderMethod.body();
		
		AbstractJClass Model = holder.getGeneratedClass();
		if (createGetGsonBuilderMethod){
			IJExpression createBuilder = _new(GsonBuilder)
					.invoke("addSerializationExclusionStrategy")
						.arg(_new(getJClass("ModelExclusionStrategy")).arg(inst).arg(fields))
					.invoke("addDeserializationExclusionStrategy")
						.arg(_new(getJClass("ModelExclusionStrategy")).arg(inst).arg(fields))
					.invoke("excludeFieldsWithModifiers")
						.arg(getJClass(java.lang.reflect.Modifier.class).staticRef("FINAL"))
						.arg(getJClass(java.lang.reflect.Modifier.class).staticRef("STATIC"))
						.arg(getJClass(java.lang.reflect.Modifier.class).staticRef("TRANSIENT"))
					.invoke("serializeNulls");
			
			if (callSuperCreateGetGsonBuilderMethod) {
				createBuilder = body.decl(GsonBuilder, "builder", createBuilder);
				body.add(Model.staticInvoke("getGsonBuilder").arg(createBuilder));
			} 		
			
			body._return(createBuilder);
		} else {
			body._return(Model.staticInvoke("getGsonBuilder"));
		}
		
			
	}
	
}
