/**
 * Copyright (C) 2016 DSpot Sp. z o.o
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
package com.dspot.declex.action;

import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._this;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.helper.APTCodeModelHelper;
import org.androidannotations.helper.IdAnnotationHelper;
import org.androidannotations.holder.BaseGeneratedClassHolder;

import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.api.action.annotation.Assignable;
import com.dspot.declex.api.action.annotation.Field;
import com.dspot.declex.api.action.annotation.FormattedExpression;
import com.dspot.declex.api.action.builtin.AlertDialogActionHolder;
import com.dspot.declex.api.action.builtin.BackgroundThreadActionHolder;
import com.dspot.declex.api.action.builtin.DateDialogActionHolder;
import com.dspot.declex.api.action.builtin.GetModelActionHolder;
import com.dspot.declex.api.action.builtin.PopulateActionHolder;
import com.dspot.declex.api.action.builtin.ProgressDialogActionHolder;
import com.dspot.declex.api.action.builtin.PutModelActionHolder;
import com.dspot.declex.api.action.builtin.RecollectActionHolder;
import com.dspot.declex.api.action.builtin.TimeDialogActionHolder;
import com.dspot.declex.api.action.builtin.ToastActionHolder;
import com.dspot.declex.api.action.builtin.UIThreadActionHolder;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethod;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.dspot.declex.api.action.process.ActionProcessor;
import com.dspot.declex.override.util.DeclexAPTCodeModelHelper;
import com.dspot.declex.util.DeclexConstant;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;

public class ActionForHandler extends BaseAnnotationHandler<BaseGeneratedClassHolder> {
	
	public static boolean GENERATE_IN_ROUND = true;
	
	private static final Set<String> ACTIONS = new HashSet<>(); 
	private static final Set<Class<? extends Annotation>> ACTION_ANNOTATION = new HashSet<>();
	
	private static final Map<String, String> ACTION_NAMES = new HashMap<>();
	private static final Map<String, ActionInfo> ACTION_INFOS = new HashMap<>();
	
	public ActionForHandler(AndroidAnnotationsEnvironment environment) {
		super(ActionFor.class, environment);
		
		//Copy all the built-in Actions
		ACTIONS.add(AlertDialogActionHolder.class.getCanonicalName());
		ACTIONS.add(ProgressDialogActionHolder.class.getCanonicalName());
		ACTIONS.add(TimeDialogActionHolder.class.getCanonicalName());
		ACTIONS.add(DateDialogActionHolder.class.getCanonicalName());

		
		ACTIONS.add(ToastActionHolder.class.getCanonicalName());
		
		ACTIONS.add(PutModelActionHolder.class.getCanonicalName());
		ACTIONS.add(GetModelActionHolder.class.getCanonicalName());
		ACTIONS.add(PopulateActionHolder.class.getCanonicalName());
		ACTIONS.add(RecollectActionHolder.class.getCanonicalName());
		
		ACTIONS.add(UIThreadActionHolder.class.getCanonicalName());
		ACTIONS.add(BackgroundThreadActionHolder.class.getCanonicalName());
		
		ACTION_ANNOTATION.add(Assignable.class);
		ACTION_ANNOTATION.add(Field.class);
		ACTION_ANNOTATION.add(FormattedExpression.class);
		ACTION_ANNOTATION.add(Assignable.class);
		
		codeModelHelper = new DeclexAPTCodeModelHelper(getEnvironment());
	}
	
	public static Map<String, String> getActionNames() {
		return Collections.unmodifiableMap(ACTION_NAMES);
	}
	
	public static Map<String, ActionInfo> getActionInfos() {
		return Collections.unmodifiableMap(ACTION_INFOS);
	}
	
	public static void addAction(String name, String clazz, ActionInfo info) {
		ACTION_NAMES.put("$" + name, clazz);
		//ACTION_NAMES.put(name, clazz);
		ACTION_INFOS.put(clazz, info);
	}
	
	private static void getActionsInformation(AndroidAnnotationsEnvironment env) {
		final IdAnnotationHelper annotationHelper = new IdAnnotationHelper(env, ActionFor.class.getCanonicalName());
		final APTCodeModelHelper codeModelHelper = new DeclexAPTCodeModelHelper(env);
		
		for (String action : ACTIONS) {
			final TypeElement typeElement = env.getProcessingEnvironment().getElementUtils().getTypeElement(action);
			final ActionFor actionForAnnotation = typeElement.getAnnotation(ActionFor.class);
			
			for (String name : actionForAnnotation.value()) {

				//Get model info
				ActionInfo actionInfo = new ActionInfo(action);
				addAction(name, action, actionInfo);
				
				List<DeclaredType> processors = annotationHelper.extractAnnotationClassArrayParameter(
						typeElement, ActionFor.class.getCanonicalName(), "processors"
					);
				
				if (processors != null) {
					for (DeclaredType processor : processors) {
						try {
							actionInfo.processors.add(
									(ActionProcessor) Class.forName(processor.toString()).newInstance()
								);
						} catch (Exception e) {}
					}
				}
										
				for (Element elem : typeElement.getEnclosedElements()) {
					final String elemName = elem.getSimpleName().toString();
					
					if (elem.getKind() == ElementKind.METHOD) {
						
						List<ActionMethodParam> params = new LinkedList<>();
						for (VariableElement param : ((ExecutableElement) elem).getParameters()) {
							
							List<Annotation> annotations = new LinkedList<>();
							for (Class<? extends Annotation> annotation : ACTION_ANNOTATION) {
								Annotation containedAnnotation = param.getAnnotation(annotation);
								if (containedAnnotation != null) {
									annotations.add(containedAnnotation);
								}
							}
							
							ActionMethodParam actionMethodParam = 
									new ActionMethodParam(
											param.getSimpleName().toString(), 
											codeModelHelper.typeMirrorToJClass(param.asType()),
											annotations
										);
							params.add(actionMethodParam);
						}
						
						List<Annotation> annotations = new LinkedList<>();
						for (Class<? extends Annotation> annotation : ACTION_ANNOTATION) {
							Annotation containedAnnotation = elem.getAnnotation(annotation);
							if (containedAnnotation != null) {
								annotations.add(containedAnnotation);
							}
						}
						
						actionInfo.addMethod(
								elemName, 
								((ExecutableElement) elem).getReturnType().toString(), 
								params, 
								annotations
							);
					}
				}
			}
		}
	}
	
	public static void buildActionObject(AndroidAnnotationsEnvironment env) {
		if (env.getProcessingEnvironment().getOptions().containsKey("internal")) return;
		
		if (!GENERATE_IN_ROUND) {
			getActionsInformation(env);
			
			return;
		}
		
		try {
			JDefinedClass Action = env.getCodeModel()._getClass(DeclexConstant.ACTION);
			if (Action == null) {
				Action = env.getCodeModel()._class(DeclexConstant.ACTION);
				
				getActionsInformation(env);
				
				for (String name : ACTION_NAMES.keySet()) {
					
					final String action = ACTION_NAMES.get(name);
					final ActionInfo actionInfo = ACTION_INFOS.get(action);
					
					List<ActionMethod> builds = actionInfo.methods.get("build");
					if (builds != null && builds.size() > 0) {
						ActionMethod build = builds.get(0);
						
						JDefinedClass ActionGate = Action._class(JMod.PUBLIC | JMod.STATIC, name);
						ActionGate._extends(env.getJClass(actionInfo.holderClass));
						
						//Create all the events for the action
						for (ActionMethodParam param : build.params) {
							ActionGate.field(
									JMod.PUBLIC | JMod.STATIC, 
									env.getCodeModel().BOOLEAN, 
									param.name,
									JExpr.TRUE
								);
						}	
						
						//Create the init methods for the action
						List<ActionMethod> inits = actionInfo.methods.get("init");
						if (inits != null) {
							for (ActionMethod actionMethod : inits) {
								JMethod method = Action.method(
										JMod.STATIC | JMod.PUBLIC, ActionGate, name
									);
								
								for (ActionMethodParam param : actionMethod.params) {
									method.param(param.clazz, param.name);
								}

								method.body()._return(_new(ActionGate));
							}
						}
						
						//All the methods of the Action Holder that returns the Holder itself,  
						//are inserted in order to use this ActionGate from the fields
						for (Entry<String, List<ActionMethod>> entry : actionInfo.methods.entrySet()) {
							for (ActionMethod actionMethod : entry.getValue()) {
								
								List<String> specials = Arrays.asList("init", "build", "execute");
								
								if (actionMethod.resultClass.equals(actionInfo.holderClass) 
									|| (actionMethod.resultClass.equals("void") && !specials.contains(actionMethod.name))) {
									
									JMethod method;
									if (actionMethod.resultClass.equals("void")) {
										method = ActionGate.method(JMod.PUBLIC, env.getCodeModel().VOID, actionMethod.name);
									} else {
										method = ActionGate.method(JMod.PUBLIC, ActionGate, actionMethod.name);
										method.body()._return(_this());
									}
									method.annotate(Override.class);
									for (ActionMethodParam param : actionMethod.params) {
										method.param(param.clazz, param.name);											
									}									
								}
							}
						}
						
						ActionGate.method(JMod.PUBLIC, env.getCodeModel().VOID, "fire");
					}
					
				}
			}			
			
		} catch (Exception e) {
			e.printStackTrace();
		}				
	}
	
	@Override
	protected void validate(Element element, ElementValidation valid) {
		ACTIONS.add(element.asType().toString());
	}
	
	@Override
	public void process(Element element, BaseGeneratedClassHolder holder) {
		
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems) {
			if (elem.getKind() == ElementKind.METHOD) {
				if (elem.getModifiers().isEmpty() || elem.getModifiers().contains(Modifier.PROTECTED)) {
					codeModelHelper.overrideAnnotatedMethod((ExecutableElement) elem, holder);
				}
			}
		}		
	}

}
