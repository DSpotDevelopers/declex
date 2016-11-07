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
package com.dspot.declex.util;

import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr._super;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.lit;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.Arrays;
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

import com.dspot.declex.action.ActionForHandler;
import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.dspot.declex.api.eventbus.UseEvents;
import com.dspot.declex.api.util.FormatsUtils;
import com.dspot.declex.eventbus.EventHandler;
import com.dspot.declex.share.holder.ViewsHolder;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.AbstractJType;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JAnnotationUse;
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

	public static JMethod getEventMethod(String eventClass, Element parentElement, ViewsHolder viewsHolder, AndroidAnnotationsEnvironment environment) {
		return getEventMethod(eventClass, parentElement, viewsHolder.holder(), viewsHolder, environment);
	}

	
	public static JMethod getEventMethod(String eventClass, Element parentElement, BaseGeneratedClassHolder holder, AndroidAnnotationsEnvironment environment) {
		return getEventMethod(eventClass, parentElement, holder, null, environment);
	}
	
	public static JMethod getEventMethod(String eventClass, Element parentElement, BaseGeneratedClassHolder holder, 
			ViewsHolder viewsHolder, AndroidAnnotationsEnvironment environment) {
		
		if (!eventClass.endsWith("_")) eventClass = eventClass + "_";
		eventClass = TypeUtils.typeFromTypeString(eventClass, environment);
		
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

			List<String> acceptedNames = new LinkedList<String>();
			acceptedNames.add(eventSimpleName);
			acceptedNames.add(eventSimpleName.substring(0, eventSimpleName.length()-1));
			acceptedNames.add("on" + eventSimpleName);
			acceptedNames.add("on" + eventSimpleName.substring(0, eventSimpleName.length()-1));
			
			//Search methods in parent referencing this event
			List<? extends Element> elems = parentElement.getEnclosedElements();
			for (Element elem : elems)
				if (elem.getKind() == ElementKind.METHOD) {
					ExecutableElement executableElement = (ExecutableElement) elem;
					
					if (!executableElement.getReturnType().getKind().equals(TypeKind.VOID)) continue;
					
					if (acceptedNames.contains(executableElement.getSimpleName().toString())) {
						List<? extends VariableElement> parameters = executableElement.getParameters();
						
						JInvocation invocation = evtBody.invoke(_super(), executableElement.getSimpleName().toString());
						if (parameters.size() == 0) continue;
						
						String originalEventClass = SharedRecords.getEvent(eventClass, environment);
						TypeElement OriginalEventClass = environment.getProcessingEnvironment().getElementUtils().getTypeElement(originalEventClass);
						
						Map<String, String> eventFields = new TreeMap<String, String>();
						if (OriginalEventClass != null && OriginalEventClass.toString().contains(".")) {
							List<? extends Element> els = OriginalEventClass.getEnclosedElements();
							for (Element el : els)
								if (el.getKind() == ElementKind.FIELD) {
									eventFields.put(el.getSimpleName().toString(), el.asType().toString());
								}
						} else {
							eventFields = EventHandler.roundGeneratedEvents.get(originalEventClass);
							if (eventFields == null) {
								eventFields = new TreeMap<String, String>();
								System.out.println("NO EVENT FOR " + originalEventClass + " in " + EventHandler.roundGeneratedEvents);
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
								if (paramType != null && (paramType.equals(originalEventClass+"_") || 
									paramType.equals(originalEventClass))) {
									
									invocation = invocation.arg(ref("event"));
								} else {
									ParamUtils.injectParam(paramName, invocation, viewsHolder);
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
	
	public static JDefinedClass createNewEvent(String className, AndroidAnnotationsEnvironment env) throws JClassAlreadyExistsException {
		if (!className.contains(".")) {
			className = DeclexConstant.EVENT_PATH + className;
		}
		
		int index = className.lastIndexOf('.');
		String eventName = className.substring(index + 1);
		
		JDefinedClass EventClass = env.getCodeModel()._class(className);
		EventClass.annotate(EBean.class);
		EventClass.annotate(UseEvents.class);
		
		//This will make that the Action class be generated in the next round
		ActionForHandler.GENERATE_IN_ROUND = false;
		ActionInfo actionInfo = new ActionInfo(className);
		ActionForHandler.addAction(eventName, className, actionInfo);
		
		JAnnotationUse actionFor = EventClass.annotate(ActionFor.class);
		actionFor.param("value", eventName);				
		
		JFieldVar finished = EventClass.field(JMod.PRIVATE, env.getJClass(Runnable.class), "Finished");
		
		AbstractJClass EventClass_ = env.getJClass(className + ModelConstants.generationSuffix());
		JFieldVar event = EventClass.field(JMod.PRIVATE, EventClass_, "event");
		
		String initName = "init";
		JMethod initMethod = EventClass.method(JMod.NONE, env.getCodeModel().VOID, initName);
		initMethod.body().assign(event, EventClass_.staticInvoke("create"));
		actionInfo.addMethod(initName, env.getCodeModel().VOID.fullName());

		String buildName = "build";
		JMethod buildMethod = EventClass.method(JMod.NONE, env.getCodeModel().VOID, buildName);
		JVar finishedParam = buildMethod.param(env.getJClass(Runnable.class), "Finished");
		buildMethod.body().assign(_this().ref(finished), finishedParam);
		actionInfo.addMethod(
				buildName, 
				env.getCodeModel().VOID.fullName(),
				Arrays.asList(new ActionMethodParam("Finished", env.getJClass(Runnable.class)))
			);
		
		String executeName = "execute";
		JMethod executeMethod = EventClass.method(JMod.NONE, env.getCodeModel().VOID, executeName);
		
		JConditional ifFinished = executeMethod.body()._if(finished.ne(_null()));
		ifFinished._then().add(event.invoke("setValues").arg(finished).invoke("postEvent"));
		ifFinished._else().add(event.invoke("postEvent"));
		
		actionInfo.addMethod(executeName, env.getCodeModel().VOID.fullName());

		return EventClass;
	}
	
}
