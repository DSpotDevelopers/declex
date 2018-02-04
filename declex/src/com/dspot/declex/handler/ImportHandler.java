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

import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EComponentHolder;

import org.androidannotations.annotations.Import;
import com.dspot.declex.annotation.action.ActionFor;
import com.dspot.declex.override.helper.DeclexAPTCodeModelHelper;
import com.dspot.declex.util.TypeUtils;
import org.androidannotations.internal.virtual.VirtualElement;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JAnonymousClass;
import com.helger.jcodemodel.JClassAlreadyExistsException;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class ImportHandler extends BaseAnnotationHandler<EComponentHolder> {
	
	public ImportHandler(AndroidAnnotationsEnvironment environment) {
		super(Import.class, environment);
		
		codeModelHelper = new DeclexAPTCodeModelHelper(environment);
	}
	
	@Override
	public void getDependencies(Element element, Map<Element, Object> dependencies) {
		//TODO: Import ADI mechanism should be different, cause' it cannot be used normally inside a library
	}
		
	@Override
	public void validate(final Element element, final ElementValidation valid) {
		
		if (!(element instanceof VirtualElement)) {
			
			if (!adiHelper.hasAnnotation(element.getEnclosingElement(), EBean.class)) {
				valid.addError("Import can be used only in @EBeans");
				return;
				
			}
				
			ActionFor actionForAnnotation = element.getEnclosingElement().getAnnotation(ActionFor.class); 
			if (actionForAnnotation != null) {
				if (!actionForAnnotation.global()) {
					valid.addError("You cannot use @Import in a not Global Action Holder. You should set \"global\" parameter of the ActionFor annotation to \"true\"");
					return;					
				}
			} 
			
			if (element.getAnnotation(AfterInject.class) != null) {
				valid.addError("You cannot use @Import in an @AfterInject method");
				return;
			}
			
			if (element.getModifiers().contains(Modifier.STATIC)) {
				valid.addError("You cannot use @Import in a static element");
				return;
			}
			
			if (element.getModifiers().contains(Modifier.PRIVATE)) {
				valid.addError("You cannot use @Import in private elements");
				return;
			}
			
			if (element instanceof ExecutableElement) {
				if (((ExecutableElement) element).getReturnType().getKind() != TypeKind.VOID) {
					valid.addError("You cannot use @Import in method with a result, only VOID methods are supported");
					return;
				}
			}
		}
	}
	
	@Override
	public void process(Element element, EComponentHolder holder) {
		
		if (element instanceof VirtualElement) {
			
			final ExecutableElement executableElement = (ExecutableElement) element;
			
			//Check if the method exists in the super class, if not, import should do nothing
			boolean methodExistsInSuper = false;
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
						
						methodExistsInSuper = true;
						break;
						
					}
				}
			}
			
			if (methodExistsInSuper) {
				createInvokeListenerStructure((VirtualElement) element, holder);
			}
		} else {
			createListenerStructure(element, holder);
		}
		
	}
	
	private void createInvokeListenerStructure(VirtualElement element, EComponentHolder holder) {
		
		final String elementName = element.getSimpleName().toString();
		final String mainName = elementName.substring(0, 1).toUpperCase() + elementName.substring(1);
		
		Element rootElement = element.getContainerElement();
		AbstractJClass ListenerClass = getJClass(
				TypeUtils.getGeneratedClassName(element.getReference(), getEnvironment()) 
				+ "." + mainName + "Listener" + ModelConstants.generationSuffix());
		
		JAnonymousClass anonymousListener = getCodeModel().anonymousClass(ListenerClass);
		JMethod anonymousListenerCall = anonymousListener.method(JMod.PUBLIC, getCodeModel().VOID, "call");
		anonymousListenerCall.annotate(Override.class);

		JInvocation invocation = JExpr.invoke(elementName);
		
		ExecutableElement executableElement = (ExecutableElement) element;
		for (VariableElement param : executableElement.getParameters()) {
			anonymousListenerCall.param(codeModelHelper.elementTypeToJClass(param), param.getSimpleName().toString());
			invocation.arg(ref(param.getSimpleName().toString()));
		}
		
		anonymousListenerCall.body().add(invocation);

		final IJExpression referenceExpression = element.getReferenceExpression();
		JMethod listenerSetter = holder.getGeneratedClass().method(
				JMod.PUBLIC, getCodeModel().VOID, 
				"set" + rootElement.getSimpleName().toString() + mainName + "Listener" + ModelConstants.generationSuffix());
		
		listenerSetter.body().invoke(referenceExpression, "set" + mainName + "Listener" + ModelConstants.generationSuffix())
					         .arg(_new(anonymousListener));
		
		if (rootElement.getAnnotation(ActionFor.class) == null) {
			holder.getInitBodyAfterInjectionBlock().invoke(listenerSetter);
		} else {
			//TODO, the listener should be established any time the action is created
		}
		
	}
	
	private void createListenerStructure(Element element, EComponentHolder holder) {
		
		final String elementName = element.getSimpleName().toString();	
		final String mainName = elementName.substring(0, 1).toUpperCase() + elementName.substring(1);
		final ExecutableElement executableElement = (ExecutableElement) element;
		
		JDefinedClass ListenerClass;
		try {
			
			ListenerClass = holder.getGeneratedClass()
					._interface(JMod.PUBLIC, mainName + "Listener" + ModelConstants.generationSuffix());
			
			JMethod callMethod = ListenerClass.method(JMod.PUBLIC, getCodeModel().VOID, "call");
			
			for (VariableElement param : executableElement.getParameters()) {
				callMethod.param(codeModelHelper.elementTypeToJClass(param), param.getSimpleName().toString());
			}
			
		} catch (JClassAlreadyExistsException e) {
			//TODO, this should no happen
			return;
		}
		
		JFieldVar listenerField = holder.getGeneratedClass()
				.field(JMod.PRIVATE, ListenerClass, elementName + "Listener" + ModelConstants.generationSuffix());		
		
		JMethod setter = holder.getGeneratedClass().method(
			JMod.PUBLIC, getCodeModel().VOID, 
			"set" + mainName + "Listener" + ModelConstants.generationSuffix());
		
		JVar listener = setter.param(ListenerClass, "listener");
		setter.body().assign(listenerField, listener);
		
		//Override and call the listener in the override method
		JMethod overrideMethod = codeModelHelper.overrideAnnotatedMethod(executableElement, holder);
		JInvocation invocation = overrideMethod.body()._if(listenerField.neNull())._then()
							 				   .invoke(listenerField, "call");
		
		for (VariableElement param : executableElement.getParameters()) {
			invocation = invocation.arg(ref(param.getSimpleName().toString()));
		}
	}

}
