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

import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.ref;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.holder.EComponentHolder;

import org.androidannotations.annotations.export.Export;
import com.dspot.declex.annotation.action.ActionFor;
import com.dspot.declex.api.util.FormatsUtils;
import com.dspot.declex.override.helper.DeclexAPTCodeModelHelper;
import org.androidannotations.internal.virtual.VirtualElement;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class ExportHandler extends BaseAnnotationHandler<EComponentHolder> {
	
	public ExportHandler(AndroidAnnotationsEnvironment environment) {
		this(Export.class, environment);
		
		codeModelHelper = new DeclexAPTCodeModelHelper(environment);
	}

	public ExportHandler(Class<? extends Annotation> targetClass, AndroidAnnotationsEnvironment environment) {
		super(targetClass, environment);
	}
	
	@Override
	public void getDependencies(Element element, Map<Element, Object> dependencies) {
		//TODO: Export ADI mechanism should be different, cause' it cannot be used normally inside a library
	}
		
	@Override
	public void validate(final Element element, final ElementValidation valid) {
		
		if (!(element instanceof VirtualElement)) {
			
			if (!adiHelper.hasAnnotation(element.getEnclosingElement(), EBean.class)) {
				valid.addError("Export can be used only in @EBeans");
				return;
				
			}
				
			ActionFor actionForAnnotation = element.getEnclosingElement().getAnnotation(ActionFor.class); 
			if (actionForAnnotation != null) {
				if (!actionForAnnotation.global()) {
					valid.addError("You cannot use @Export in a not Global Action Holder. You should set \"global\" parameter of the ActionFor annotation to \"true\"");
					return;					
				}
			} else {
				
				if (!element.getKind().isField() && !element.getModifiers().contains(Modifier.PUBLIC)) {
					valid.addError("Inside a normal @EBean, you should use export only in PUBLIC elements");
					return;
				}
				
			}
			
			if (element.getAnnotation(AfterInject.class) != null) {
				valid.addError("You cannot use @Export in an @AfterInject method");
				return;
			}
			
			if (element.getModifiers().contains(Modifier.STATIC)) {
				valid.addError("You cannot use @Export in a static element");
				return;
			}
			
			if (element.getModifiers().contains(Modifier.PRIVATE)) {
				valid.addError("You cannot use @Export in private elements");
				return;
			}
		}
	}
	
	@Override
	public void process(Element element, EComponentHolder holder) {
		
		if (element.getKind().isField() && !(element instanceof VirtualElement)) {
			
			final String fieldName = element.getSimpleName().toString();
			
			//Create getter
			JMethod getterMethod = holder.getGeneratedClass().method(
					JMod.PUBLIC, 
					codeModelHelper.elementTypeToJClass(element), 
					FormatsUtils.fieldToGetter(fieldName)
				);
			getterMethod.body()._return(_this().ref(fieldName));
		}
		
		if (element.getKind() == ElementKind.METHOD && element instanceof VirtualElement) {
			
			final IJExpression referenceExpression = ((VirtualElement) element).getReferenceExpression();
			final ExecutableElement executableElement = (ExecutableElement) element;
			
			//Check if the method exists in the super class, in this case super should be called
			boolean placeOverrideAndCallSuper = false;
			List<? extends Element> elems = element.getEnclosingElement().getEnclosedElements();
			ELEMENTS: for (Element elem : elems) {
				if (elem instanceof ExecutableElement) {
					ExecutableElement executableElem = (ExecutableElement) elem;
					
					if (elem.getSimpleName().toString().equals(element.getSimpleName().toString())
						&& executableElem.getParameters().size() == executableElement.getParameters().size()) {
						
						for (int i = 0; i < executableElem.getParameters().size(); i++) {
							VariableElement paramElem = executableElem.getParameters().get(i);
							VariableElement paramElement = executableElement.getParameters().get(i);
							
							if (!paramElem.asType().toString().equals(paramElement.asType().toString())) {
								continue ELEMENTS;
							}
						}
						
						placeOverrideAndCallSuper = true;
						break;
						
					}
				}
			}
			
			final JMethod method = codeModelHelper.overrideAnnotatedMethod(executableElement, holder, false, placeOverrideAndCallSuper);								
			final JInvocation invocation = invoke(referenceExpression, method);
					
			if (method.type().fullName().equals("void")) {
				method.body()._if(referenceExpression.neNull())._then().add(invocation);
			} else {
				method.body()._if(referenceExpression.neNull())._then()._return(invocation);					
				method.body()._return(getDefault(method.type().fullName()));
			}
					
					                              ;
			for (JVar param : method.params()) {
				invocation.arg(ref(param.name()));
			}
				
		}		
		
	}
	
	private IJExpression getDefault(String type) {
		switch (type) {
		case "int":
			return JExpr.lit(0);
		case "float":
			return JExpr.lit(0f);
		case "double":
			return JExpr.lit(0d);
		case "long":
			return JExpr.lit(0L);
		case "short":
			return JExpr.lit((short) 0);
		case "char":
			return JExpr.lit((char) 0);
		case "byte":
			return JExpr.lit((byte) 0);
		case "boolean":
			return JExpr.lit(false);

		default:
			return JExpr._null();
		}
	}

}
