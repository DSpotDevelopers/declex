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
package com.dspot.declex.util;

import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.lit;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.annotations.EBean;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.BaseGeneratedClassHolder;

import com.dspot.declex.action.Actions;
import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.dspot.declex.api.eventbus.UseEvents;
import com.dspot.declex.api.util.FormatsUtils;
import com.dspot.declex.helper.FilesCacheHelper;
import com.dspot.declex.share.holder.ViewsHolder;
import com.dspot.declex.util.element.VirtualElement;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.AbstractJType;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JAnnotationUse;
import com.helger.jcodemodel.JAnonymousClass;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JClassAlreadyExistsException;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class EventUtils {
	
	private final static String INIT_NAME = "init";
	private final static String BUILD_NAME = "build";
	private final static String EXECUTE_NAME = "execute";
	
	public final static Map<String, Map<String, String>> eventsFields = new HashMap<>();
	
	public static JMethod getEventMethod(String eventClass, Element parentElement, ViewsHolder viewsHolder, AndroidAnnotationsEnvironment environment) {
		return getEventMethod(eventClass, parentElement, viewsHolder.holder(), viewsHolder, environment);
	}
	
	public static JMethod getEventMethod(String eventClass, Element parentElement, BaseGeneratedClassHolder holder, AndroidAnnotationsEnvironment environment) {
		return getEventMethod(eventClass, parentElement, holder, null, environment);
	}
	
	public static JMethod getEventMethod(String eventClass, Element element, BaseGeneratedClassHolder holder, 
			ViewsHolder viewsHolder, AndroidAnnotationsEnvironment environment) {
		
		eventClass = TypeUtils.getGeneratedClassName(eventClass, environment);
		
		String eventSimpleName = eventClass;
		Matcher matcher = Pattern.compile("\\.([A-Za-z_][A-Za-z0-9_]+)$").matcher(eventClass);
		if (matcher.find()) {
			eventSimpleName = matcher.group(1);
		}
		
		AbstractJClass EventClass = environment.getJClass(eventClass);
		JMethod eventMethod = holder.getGeneratedClass().getMethod(
				"on" + eventSimpleName, 
				new AbstractJType[] {EventClass}
			);

		if (eventMethod == null) {
			AbstractJClass Runnable = environment.getJClass(Runnable.class);
			
			eventMethod = holder.getGeneratedClass().method(JMod.PUBLIC, environment.getCodeModel().VOID, "on" + eventSimpleName);
			JVar event = eventMethod.param(JMod.FINAL, EventClass, "event");

			//--------------------------------------------
			//Create holder method
			//--------------------------------------------
			JMethod eventMethodHolder = holder.getGeneratedClass().method(JMod.PUBLIC, environment.getCodeModel().VOID, "onEventMainThread");
			JAnnotationUse subscribe = eventMethodHolder.annotate(environment.getJClass("org.greenrobot.eventbus.Subscribe"));
			subscribe.param("threadMode", environment.getJClass("org.greenrobot.eventbus.ThreadMode").staticRef("MAIN"));
			eventMethodHolder.param(EventClass, "event");
			JBlock evtBody = eventMethodHolder.body(); 
			
			evtBody.invoke(eventMethod).arg(event);
			
			//If the method is !provided, then the parent element was provided
			if (!(element instanceof ExecutableElement)) {
				
				List<String> acceptedNames = new LinkedList<String>();
				acceptedNames.add(eventSimpleName);
				acceptedNames.add(eventSimpleName.substring(0, eventSimpleName.length()-1));
				acceptedNames.add("on" + eventSimpleName);
				acceptedNames.add("on" + eventSimpleName.substring(0, eventSimpleName.length()-1));
				
				//Search methods in parent referencing this event
				List<? extends Element> elems = element.getEnclosedElements();
				List<Element> allElems = new LinkedList<>(elems);
				allElems.addAll(VirtualElement.getVirtualEnclosedElements(element));
				
				for (Element elem : allElems) {
					if (elem.getKind() == ElementKind.METHOD) {
						ExecutableElement executableElement = (ExecutableElement) elem;
						
						if (!executableElement.getReturnType().getKind().equals(TypeKind.VOID)) continue;						
						
						if (acceptedNames.contains(executableElement.getSimpleName().toString())) {
							element = elem;
						}
						
						break;
					}
				}
			}
			
			if (element instanceof ExecutableElement) {
				ExecutableElement executableElement = (ExecutableElement) element;								
				List<? extends VariableElement> parameters = executableElement.getParameters();
				
				JInvocation invocation = evtBody.invoke(executableElement.getSimpleName().toString());
				if (parameters.size() != 0) {
					String originalEventClass = SharedRecords.getEvent(eventClass, environment);
					TypeElement OriginalEventClass = environment.getProcessingEnvironment().getElementUtils().getTypeElement(originalEventClass);
					
					Map<String, String> eventFields = new TreeMap<String, String>();
					if (OriginalEventClass != null && OriginalEventClass.toString().contains(".")) {
						List<? extends Element> els = OriginalEventClass.getEnclosedElements();
						for (Element el : els) {
							if (el.getKind() == ElementKind.FIELD) {
								final String paramName = el.getSimpleName().toString();
								String paramType = el.asType().toString();
								
								if (!paramType.equals(TypeUtils.getGeneratedClassName(originalEventClass, environment)) 
									&& !paramType.equals(originalEventClass)) {
									eventFields.put(paramName, paramType);
								}
							}
						}
					} else {
						eventFields = eventsFields.get(originalEventClass);						
						if (eventFields == null) {
							eventFields = new TreeMap<String, String>();
							System.out.println("NO EVENT FOR " + originalEventClass + " in " + eventsFields);
						}
					}
					
					for (VariableElement param : parameters) {
						final String paramName = param.getSimpleName().toString();
						String paramType = param.asType().toString();
						
						String type = eventFields.get(paramName);
						if (type != null && type.equals(paramType)) {
							invocation = invocation.arg(ref("event").invoke(FormatsUtils.fieldToGetter(param.getSimpleName().toString())));
						} else {
							paramType = SharedRecords.getEvent(paramType, environment);
							if (paramType != null 
								&& (paramType.equals(TypeUtils.getGeneratedClassName(originalEventClass, environment))  
									|| paramType.equals(originalEventClass))) {
								
								invocation = invocation.arg(ref("event"));
							} else {
								ParamUtils.injectParam(paramName, paramType, invocation, viewsHolder);
							}
						}
					}
				}
				
			}
			
			//If there's a nextRunnable, then run it
			JBlock ifBlock = evtBody._if(event.invoke("getValues").ref("length").gt(lit(0)))._then();
			IJExpression value = event.invoke("getValues").component(lit(0));
			ifBlock = ifBlock._if(value._instanceof(Runnable))._then();
			ifBlock.invoke(cast(Runnable, value), "run");
			
		}
			
		return eventMethod;			
	}
	
	private static void createEventInfo(String className, AndroidAnnotationsEnvironment env) {
		if (!className.contains(".")) {
			className = DeclexConstant.EVENT_PATH + className;
		}
		final int index = className.lastIndexOf('.');
		final String eventName = className.substring(index + 1);
				
		if (!FilesCacheHelper.getInstance().hasCachedFile(className)) {
			FilesCacheHelper.getInstance().addGeneratedClass(className, null);
			FilesCacheHelper.getInstance().addGeneratedClass(
				TypeUtils.getGeneratedClassName(className, env, false), 
				null
			);
		}
		
		ActionInfo actionInfo = new ActionInfo(className);
		Actions.getInstance().addAction(eventName, className, actionInfo);
		
		actionInfo.addMethod(INIT_NAME, env.getCodeModel().VOID.fullName());
		
		actionInfo.addMethod(
				BUILD_NAME, 
				env.getCodeModel().VOID.fullName(),
				Arrays.asList(
					new ActionMethodParam("Finished", env.getJClass(className + ".EventFinishedRunnable")),
					new ActionMethodParam("Failed", env.getJClass(Runnable.class))
				)
			);
		
		actionInfo.addMethod(EXECUTE_NAME, env.getCodeModel().VOID.fullName());
	}
	
	public static void registerEvent(String className, AndroidAnnotationsEnvironment env) {
		registerEvent(className, new HashMap<String, String>(0), env);
	}
	
	public static void registerEvent(String className, Map<String, String> fields, AndroidAnnotationsEnvironment env) {
		
		//Add to the Special Events in validation, so other validators will find this Event
		SharedRecords.addEventGeneratedClass(className, env);
		
		EventUtils.eventsFields.put(className, fields);
		
		EventUtils.createEventInfo(className, env);
	}
	
	public static AbstractJClass createNewEvent(String className, Element fromElement, AndroidAnnotationsEnvironment env) {
		if (!className.contains(".")) {
			className = DeclexConstant.EVENT_PATH + className;
		}
		final int index = className.lastIndexOf('.');
		final String eventName = className.substring(index + 1);

		final ActionInfo actionInfo = Actions.getInstance().getActionInfos().get(className);

		final String reference = JavaDocUtils.referenceFromElement(fromElement);
		
		JDefinedClass EventClass;
		try {
			
			if (FilesCacheHelper.getInstance().hasCachedFile(className)) {
				throw new JClassAlreadyExistsException(null);
			}
			
			EventClass = env.getCodeModel()._class(className);
			
			actionInfo.setReferences(reference);
			EventClass.javadoc().add(actionInfo.references);
			
			Element rootElement = TypeUtils.getRootElement(fromElement);
			FilesCacheHelper.getInstance().addGeneratedClass(className, rootElement);
			
		} catch (JClassAlreadyExistsException e) {
			
			if (actionInfo.references == null) actionInfo.setReferences(reference);
			else {
				//TODO this is not working
				actionInfo.setReferences(
						actionInfo.references + "\n<br>\n" + reference.replace("Created by ", "Referenced by ")
					);
			}
			
			return env.getJClass(className);
		}
		
		
		EventClass.annotate(EBean.class);
		EventClass.annotate(UseEvents.class);

		JAnnotationUse actionFor = EventClass.annotate(ActionFor.class);
		actionFor.param("value", eventName);				
		
		JFieldVar finished = EventClass.field(JMod.PRIVATE, env.getJClass("EventFinishedRunnable"), "Finished");
		JFieldVar failed = EventClass.field(JMod.PRIVATE, env.getJClass(Runnable.class), "Failed");
		
		AbstractJClass EventClass_ = env.getJClass(className + ModelConstants.generationSuffix());
		JFieldVar event = EventClass.field(JMod.PRIVATE, EventClass_, "event");
		
		JMethod initMethod = EventClass.method(JMod.NONE, env.getCodeModel().VOID, INIT_NAME);
		initMethod.body().assign(event, EventClass_.staticInvoke("create"));
				
		JMethod buildMethod = EventClass.method(JMod.NONE, env.getCodeModel().VOID, BUILD_NAME);
		JVar finishedParam = buildMethod.param(env.getJClass("EventFinishedRunnable"), "Finished");
		buildMethod.body().assign(_this().ref(finished), finishedParam);
		
		AbstractJClass EventBus = env.getJClass("org.greenrobot.eventbus.EventBus");
		JVar failedParam = buildMethod.param(env.getJClass(Runnable.class), "Failed");
		buildMethod.body().assign(_this().ref(failed), failedParam);
				
		JMethod executeMethod = EventClass.method(JMod.NONE, env.getCodeModel().VOID, EXECUTE_NAME);
		
		JBlock ifFailed = executeMethod.body()
				._if(EventBus.staticInvoke("getDefault").invoke("hasSubscriberForEvent").arg(EventClass_.dotclass()).not())
				._then();
		ifFailed._if(failed.neNull())._then().invoke(failed, "run");
		ifFailed._return();
		
		JAnonymousClass anonymous = env.getCodeModel().anonymousClass(Runnable.class);
		JMethod anonymousRunnableRun = anonymous.method(JMod.PUBLIC, env.getCodeModel().VOID, "run");
		anonymousRunnableRun.annotate(Override.class);
		anonymousRunnableRun.body().invoke(finished, "onEventFinished").arg(event);
		JConditional ifFinished = executeMethod.body()._if(finished.ne(_null()));
		ifFinished._then().add(event.invoke("setValues").arg(_new(anonymous)).invoke("postEvent"));
		ifFinished._else().add(event.invoke("postEvent"));	
		
		try {
			JDefinedClass EventFinishedRunnable = EventClass._class(JMod.PUBLIC | JMod.ABSTRACT | JMod.STATIC, "EventFinishedRunnable");
			EventFinishedRunnable._implements(Runnable.class);
			
			JFieldVar eventField = EventFinishedRunnable.field(JMod.PUBLIC, EventClass_, "event");
			JMethod onEventFinished = EventFinishedRunnable.method(JMod.PUBLIC, env.getCodeModel().VOID, "onEventFinished");
			JVar eventParam = onEventFinished.param(EventClass_, "event");
			onEventFinished.body().assign(_this().ref(eventField), eventParam);
			onEventFinished.body().invoke("run");
		} catch (JClassAlreadyExistsException e) {
		}

		return EventClass;
	}
	
}
