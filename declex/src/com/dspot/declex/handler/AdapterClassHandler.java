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
package com.dspot.declex.handler;

import static com.helger.jcodemodel.JExpr._super;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;

import com.dspot.declex.adapter.plugin.JClassPlugin;
import com.dspot.declex.annotation.AdapterClass;
import com.dspot.declex.annotation.ExportPopulate;
import com.dspot.declex.holder.ViewsHolder;
import com.dspot.declex.override.helper.DeclexAPTCodeModelHelper;
import com.dspot.declex.util.ParamUtils;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.AbstractJType;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class AdapterClassHandler extends BaseAnnotationHandler<EComponentHolder> implements JClassPlugin {
	
	protected static final Logger LOGGER = LoggerFactory.getLogger(AdapterClassHandler.class);
	
	public AdapterClassHandler(AndroidAnnotationsEnvironment environment) {
		super(AdapterClass.class, environment);
		codeModelHelper = new DeclexAPTCodeModelHelper(environment);
	}

	@Override
	public void validate(Element element, ElementValidation valid) {
		String classField = TypeUtils.getClassFieldValue(element, getTarget(), "value", getEnvironment());
		if (classField == null) {
			valid.addError("The provided class isn't a valid class");
		}
		
		AbstractJClass baseClass = getBaseAdapter(element);
		if (baseClass != null && adiHelper.getAnnotation(element, ExportPopulate.class) != null) {
			Element baseClassElement = getProcessingEnvironment().getElementUtils().getTypeElement(baseClass.fullName());
			if (baseClassElement != null) {
				if (!baseClassElement.getEnclosingElement().getKind().equals(ElementKind.PACKAGE)) {
					if (!baseClassElement.getModifiers().contains(Modifier.STATIC)) {
						valid.addError("For @ExportPopulate fields, the provided inner class in @AdapterClass should be static");
					}
				}
			}
		}
	}

	@Override
	public void process(Element element, EComponentHolder holder)
			throws Exception { 		
	}

	@Override
	public void process(Element element, EComponentHolder holder, JDefinedClass AdapterClass) {
		
		if (element.getAnnotation(AdapterClass.class) == null) return;
		
		String classField = TypeUtils.getClassFieldValue(element, getTarget(), "value", getEnvironment());
		TypeElement typeElement = getProcessingEnvironment().getElementUtils().getTypeElement(classField);
		
		if (typeElement == null) {
			LOGGER.error("Error getting the TypeElement", element, element.getAnnotation(AdapterClass.class));
			return;
		}
		
		boolean isViewAdapter = false;
		JMethod inflaterMethod = AdapterClass.getMethod("inflate", new AbstractJType[]{getCodeModel().INT, getClasses().VIEW_GROUP, getClasses().LAYOUT_INFLATER});
		if (inflaterMethod == null) {
			inflaterMethod = AdapterClass.getMethod("inflate", new AbstractJType[]{getCodeModel().INT, getClasses().VIEW, getClasses().VIEW_GROUP, getClasses().LAYOUT_INFLATER});
			isViewAdapter = true;
		}		
		
		//Get all the fields and methods
		List<? extends Element> elems = typeElement.getEnclosedElements();
		for (Element elem : elems) {
			final String elemName = elem.getSimpleName().toString();
			
			if (elem.getKind() == ElementKind.METHOD) {
				final ExecutableElement executableElement = (ExecutableElement) elem;
				final List<? extends VariableElement> params = executableElement.getParameters();
				final TypeMirror result = executableElement.getReturnType();
				
				if (elemName.equals("getModels") && params.size() == 0 && elem.getModifiers().contains(Modifier.ABSTRACT)) {

					if (TypeUtils.isSubtype(executableElement.getReturnType(), CanonicalNameConstants.LIST, getProcessingEnvironment())) {
						
						JMethod getModelsMethod = AdapterClass.method(
								JMod.PUBLIC, 
								codeModelHelper.elementTypeToJClass(executableElement),
								"getModels"
							);
						getModelsMethod.annotate(Override.class);
						getModelsMethod.body()._return(ref("models"));
					} else {
						//TODO validate getModels class
					}
				}
				
				if (elemName.equals("getItemViewType")) {
					
					//TODO validate getItemViewType parameter
					
					JMethod getModelsMethod = AdapterClass.method(
							JMod.PUBLIC, 
							getCodeModel().INT,
							"getItemViewType"
						);
					getModelsMethod.annotate(Override.class);
					JVar position = getModelsMethod.param(getCodeModel().INT, "position");
					
					JInvocation invoke = _super().invoke("getItemViewType");
					
					for (VariableElement param : params) {

						if (param.getSimpleName().toString().equals("model")) {
							invoke = invoke.arg(ref("models").invoke("get").arg(position));
						} else {
							
							final ViewsHolder viewsHolder = holder.getPluginHolder(new ViewsHolder((EComponentWithViewSupportHolder) holder));
							
							if (viewsHolder != null) {
								invoke = ParamUtils.injectParam(
										param.getSimpleName().toString(), 
										codeModelHelper.elementTypeToJClass(param).fullName(), 
										invoke, viewsHolder);
							}
						}
					}
					
					getModelsMethod.body()._return(invoke);
				}
				
				if (isViewAdapter) {
					
					if (elemName.equals("getCount") && params.size() == 0 && result.toString().equals("int")) {
						JMethod getCountMethod = AdapterClass.getMethod("getCount", new AbstractJType[]{});
						codeModelHelper.removeBody(getCountMethod);
						getCountMethod.body()._return(_super().invoke(getCountMethod));
					}
					
					if (elemName.equals("getItem") && params.size() == 1 && params.get(0).asType().toString().equals("int")
						&& result.toString().equals(Object.class.getCanonicalName())) {
						JMethod getItemMethod = AdapterClass.getMethod("getItem", new AbstractJType[]{getCodeModel().INT});
						codeModelHelper.removeBody(getItemMethod);
						getItemMethod.body()._return(_super().invoke(getItemMethod).arg(ref("position")));
					}
					
					if (elemName.equals("getItemId") && params.size() == 1 && params.get(0).asType().toString().equals("int")
							&& result.toString().equals("long")) {
						JMethod getItemIdMethod = AdapterClass.getMethod("getItemId", new AbstractJType[]{getCodeModel().INT});
						codeModelHelper.removeBody(getItemIdMethod);
						getItemIdMethod.body()._return(_super().invoke(getItemIdMethod).arg(ref("position")));
					}
					
				}
								
				if (elemName.equals("inflate")) {
						
					//Remove previous method body
					codeModelHelper.removeBody(inflaterMethod);
					
					JFieldRef position = ref("position");
					JFieldRef viewType = ref("viewType");
					JFieldRef parent = ref("parent");
					JFieldRef convertView = ref("convertView");
					JFieldRef inflater = ref("inflater");

					JInvocation invoke = JExpr._super().invoke(inflaterMethod);
					for (VariableElement param : params) {
						if (param.asType().toString().equals("int")) {
							if (isViewAdapter) invoke = invoke.arg(position);
							else invoke = invoke.arg(viewType);
						} else
						
						if (isViewAdapter && param.asType().toString().equals(getClasses().VIEW.fullName())) {
							invoke = invoke.arg(convertView);
						} else
							
						if (param.asType().toString().equals(getClasses().VIEW_GROUP.fullName())) {
							invoke = invoke.arg(parent);
						} else
						
						if (param.asType().toString().equals(getClasses().LAYOUT_INFLATER.fullName())) {
							invoke = invoke.arg(inflater);
						} else					
							
						{
							
							final ViewsHolder viewsHolder = holder.getPluginHolder(new ViewsHolder((EComponentWithViewSupportHolder) holder));
							
							if (viewsHolder != null) {
								invoke = ParamUtils.injectParam(
										param.getSimpleName().toString(), 
										codeModelHelper.elementTypeToJClass(param).fullName(), 
										invoke, viewsHolder);
							}
						}
					}
					
					inflaterMethod.body()._return(invoke);
					
				}				
			}
		}
		
	}

	@Override
	public AbstractJClass getBaseAdapter(Element element) {
		if (element.getAnnotation(AdapterClass.class) == null) return null;
		
		String classField = TypeUtils.getClassFieldValue(element, getTarget(), "value", getEnvironment());
		return getJClass(classField);
	}

}