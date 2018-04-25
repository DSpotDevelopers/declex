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

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.EBean;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.holder.EComponentWithViewSupportHolder;

import com.dspot.declex.action.Actions;
import com.dspot.declex.annotation.action.ActionFor;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.override.helper.OverrideAPTCodeModelHelper;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class ActionForHandler extends BaseAnnotationHandler<EComponentWithViewSupportHolder> {
	
	public ActionForHandler(AndroidAnnotationsEnvironment environment) {
		super(ActionFor.class, environment);
		
		codeModelHelper = new OverrideAPTCodeModelHelper(getEnvironment());
	}
	
	@Override
	public void getDependencies(Element element, Map<Element, Object> dependencies) {
		dependencies.put(element, EBean.class);
	}
		
	@Override
	protected void validate(Element element, ElementValidation valid) {

		boolean initFound = false;
		boolean buildFound = false;
		boolean executeFound = false;
		
		Element elementWithBuild = null;
		
		Element superElement = element;
		validation: while (superElement != null) {
			for (Element elem : superElement.getEnclosedElements()) {
				
				if (elem instanceof ExecutableElement) {
					
					ExecutableElement executableElement = (ExecutableElement) elem;
									
					if (executableElement.getSimpleName().toString().equals("init")) {
						if (!executableElement.getReturnType().toString().equals("void")) {
							valid.addError(elem, "\"init\" method of the Action Holder should not return a value");
						}
						
						if (executableElement.getModifiers().contains(Modifier.PUBLIC)) {
							valid.addError(elem, "\"init\" method of the Action Holder should not be \"public\"");
						}
						
						if (executableElement.getModifiers().contains(Modifier.PRIVATE)) {
							valid.addError(elem, "\"init\" method of the Action Holder should not be \"private\"");
						}
	
						initFound = true;
					}
					
					if (executableElement.getSimpleName().toString().equals("build")) {
						
						if (elem == elementWithBuild) {
							valid.addError(elem, "Only one \"build\" method is permitted inside Action Holders");
						}
						
						if (!executableElement.getReturnType().toString().equals("void")) {
							valid.addError(elem, "\"build\" method of the Action Holder should not return a value");
						}
						
						if (executableElement.getParameters().isEmpty()) {
							valid.addError(elem, "\"build\" method of the Action Holder should take at least one parameters");
						}
						
						for (VariableElement param : executableElement.getParameters()) {
						
							boolean isRunnable = false;
							
							if (!param.asType().getKind().isPrimitive()) {
								if (TypeUtils.isSubtype(param, Runnable.class.getCanonicalName(), getProcessingEnvironment())) {
									isRunnable = true;
								}						
							}
							
							if (!isRunnable) {
								valid.addError(
										param, 
										"Parameter \"" + param.getSimpleName() 
										+ "\" of the \"build\" method of the Action Holder should implemente Runnable interface"
									);
							}
							
						}
						
						if (executableElement.getModifiers().contains(Modifier.PUBLIC)) {
							valid.addError(elem, "\"build\" method of the Action Holder should not be \"public\"");
						}

						if (executableElement.getModifiers().contains(Modifier.PRIVATE)) {
							valid.addError(elem, "\"build\" method of the Action Holder should not be \"private\"");
						}

						buildFound = true;
						elementWithBuild = elem;
					}
					
					if (executableElement.getSimpleName().toString().equals("execute")) {
						if (!executableElement.getReturnType().toString().equals("void")) {
							valid.addError(elem, "\"execute\" method of the Action Holder should not return a value");
						}
						
						if (!executableElement.getParameters().isEmpty()) {
							valid.addError(elem, "\"execute\" method of the Action Holder should not take parameters");
						}
						
						if (executableElement.getModifiers().contains(Modifier.PUBLIC)) {
							valid.addError(elem, "\"execute\" method of the Action Holder should not be \"public\"");
						}
						
						if (executableElement.getModifiers().contains(Modifier.PRIVATE)) {
							valid.addError(elem, "\"execute\" method of the Action Holder should not be \"private\"");
						}
						
						executeFound = true;
					}
					
				}
			}
			
			List<? extends TypeMirror> superTypes = getProcessingEnvironment().getTypeUtils().directSupertypes(superElement.asType());
			for (TypeMirror type : superTypes) {
				superElement = getProcessingEnvironment().getElementUtils().getTypeElement(type.toString());
				if (superElement == null) continue;
				if (superElement.getKind().equals(ElementKind.INTERFACE)) continue;
				if (superElement.asType().toString().equals(Object.class.getCanonicalName())) continue;
				continue validation;
			}
			
			superElement = null;
		}

		
		if (!initFound) {
			valid.addError(element, "The Action Holder should implement a method \"init\". Ex. \"void init(){}\"");
		}
		
		if (!buildFound) {
			valid.addError(element, "The Action Holder should implement a method \"build\". Ex. \"void build(Runnable Started){}\"");
		}

		if (!executeFound) {
			valid.addError(element, "The Action Holder should implement a method \"execute\". Ex. \"void execute(){}\"");
		}
		
		if (valid.isValid()) {
			Actions.getInstance().addActionHolder(element.asType().toString());
		}
	}
	
	@Override
	public void process(Element element, EComponentWithViewSupportHolder holder) {
		
		overrideMethods(element, holder, null);
		
		ActionInfo actionInfo = Actions.getInstance().getActionInfos().get(element.asType().toString());
		if (holder.hasOnViewChanged()) {
			actionInfo.handleViewChanges = true;
		}
		
		final ActionFor actionFor = element.getAnnotation(ActionFor.class);
		final String clsName = element.asType().toString();		
		final String pkg = clsName.substring(0, clsName.lastIndexOf('.'));
		
		//Create Action Gates objects
		List<JDefinedClass> actionGates = new LinkedList<>();			
		for (String actionName : actionFor.value()) {
							
			final String actionGateClassName = pkg + "." + actionName + "Gate"; 
			final String javaDoc = getProcessingEnvironment().getElementUtils().getDocComment(element);
			
			try {
				JDefinedClass ActionGate = getCodeModel()._class(JMod.PUBLIC, actionGateClassName);
				ActionGate._extends(codeModelHelper.elementTypeToJClass(element));
				
				if (javaDoc != null) {
					ActionGate.javadoc().add(javaDoc);
				}
				
				actionGates.add(ActionGate);
			} catch (Exception e) {}
		}	
		
		createActionGateMethods(element, actionGates, null);
		
		//Add fire method to be used by Actions as Fields
		for (JDefinedClass ActionGate : actionGates) {
			ActionGate.method(JMod.PUBLIC, getCodeModel().VOID, "fire");
		}
		
	}

	public void createActionGateMethods(Element typeElement, List<JDefinedClass> actionGates, 
			 List<String> methodsHandled) {

		if (methodsHandled == null) {
			methodsHandled = new LinkedList<>();
		}
		
		for (Element elem : typeElement.getEnclosedElements()) {
			
			if (elem.getKind() == ElementKind.METHOD) {
				
				final ExecutableElement element = (ExecutableElement) elem;
				final String elementName = element.getSimpleName().toString();
				
				if (methodsHandled.contains(elem.toString())) continue;
				methodsHandled.add(element.toString());
				
				final List<String> specials = Arrays.asList("init", "build", "execute");
				final String holderClass = typeElement.asType().toString();
				final String resultClass = element.getReturnType().toString();
				final String javaDoc = getProcessingEnvironment().getElementUtils().getDocComment(element);	
				
				if (elementName.equals("build") && !methodsHandled.contains("build")) {
					methodsHandled.add("build");
					
					for (JDefinedClass ActionGate : actionGates) {
						//Create all the events for the action
						for (VariableElement param : element.getParameters()) {
							final String paramName = param.getSimpleName().toString();
							final AbstractJClass paramClass = codeModelHelper.elementTypeToJClass(param);
							
							JFieldVar field = ActionGate.field(
									JMod.PUBLIC | JMod.STATIC, 
									getCodeModel().BOOLEAN, 
									paramName,
									JExpr.TRUE
								);
							
							JFieldVar refField = ActionGate.field(
									JMod.PROTECTED | JMod.STATIC, 
									paramClass, 
									"$" + paramName
								);
							
							JMethod method = ActionGate.method(
									JMod.PUBLIC | JMod.STATIC, 
									paramClass, 
									paramName
								);
							method.body()._return(refField);
							
							if (javaDoc != null) {
								Matcher matcher = 
										Pattern.compile(
												"\\s+@param\\s+" + paramName + "\\s+((?:[^@]|(?<=\\{)@[^}]+\\})+)"
										).matcher(javaDoc);
								
								if (matcher.find()) {
									field.javadoc().add("<br><hr><br>\n" + matcher.group(1).trim());
								}
							}
						}							
					}
				}
				
				if (TypeUtils.isSubtype(holderClass, resultClass, getProcessingEnvironment()) 
						|| (resultClass.equals("void") && !specials.contains(elementName))) {
											
					for (JDefinedClass ActionGate : actionGates) {
						JMethod method;
						if (resultClass.equals("void")) {										
							method = ActionGate.method(JMod.PUBLIC, getCodeModel().VOID, elementName);
						} else {
							method = ActionGate.method(JMod.PUBLIC, ActionGate, elementName);
							method.body()._return(_this());
						}
						method.annotate(Override.class);
						
						for (VariableElement param : element.getParameters()) {
							
							List<Annotation> annotations = new LinkedList<>();
							for (Class<? extends Annotation> annotation : Actions.ACTION_ANNOTATION) {
								Annotation containedAnnotation = param.getAnnotation(annotation);
								if (containedAnnotation != null) {
									annotations.add(containedAnnotation);
								}
							}
							
							JVar paramVar = method.param(
								codeModelHelper.elementTypeToJClass(param), 
								param.getSimpleName().toString()
							);	
							
							for (AnnotationMirror annotationMirror : param.getAnnotationMirrors()) {
								TypeUtils.annotateVar(paramVar, annotationMirror, getEnvironment());
							} 
						}
											
						if (javaDoc != null) {
							method.javadoc().add("<br><hr><br>\n" + javaDoc.trim());
						}	
					}
										
				}
			}
		}
		
		List<? extends TypeMirror> superTypes = getProcessingEnvironment().getTypeUtils().directSupertypes(typeElement.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = getProcessingEnvironment().getElementUtils().getTypeElement(type.toString());
			if (superElement == null) continue;
			if (superElement.getKind().equals(ElementKind.INTERFACE)) continue;
			if (superElement.asType().toString().equals(Object.class.getCanonicalName())) continue;
			createActionGateMethods(superElement, actionGates, methodsHandled);
		}
		
	}
	
	private void overrideMethods(Element element, EComponentWithViewSupportHolder holder, 
								 List<String> overrideMethods) {
		
		if (overrideMethods == null) {
			overrideMethods = new LinkedList<>();
		}
		
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems) {
			if (elem.getKind() == ElementKind.METHOD) {
				if (overrideMethods.contains(elem.toString())) continue;
				if (elem.getModifiers().isEmpty() || elem.getModifiers().contains(Modifier.PROTECTED)) {
					codeModelHelper.overrideAnnotatedMethod((ExecutableElement) elem, holder);
					overrideMethods.add(elem.toString());
				}
			}
		}
		
		List<? extends TypeMirror> superTypes = getProcessingEnvironment().getTypeUtils().directSupertypes(element.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = getProcessingEnvironment().getElementUtils().getTypeElement(type.toString());
			if (superElement == null) continue;
			if (superElement.getKind().equals(ElementKind.INTERFACE)) continue;
			if (superElement.asType().toString().equals(Object.class.getCanonicalName())) continue;
			overrideMethods(superElement, holder, overrideMethods);
		}

	}
	
}
