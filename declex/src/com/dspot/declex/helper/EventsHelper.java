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
package com.dspot.declex.helper;

import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr._this;
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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.annotations.EBean;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.*;

import com.dspot.declex.action.Actions;
import com.dspot.declex.annotation.UseEvents;
import com.dspot.declex.annotation.action.ActionFor;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.dspot.declex.api.action.runnable.OnFailedRunnable;
import com.dspot.declex.api.util.FormatsUtils;
import com.dspot.declex.holder.EventHolder;
import com.dspot.declex.holder.ViewsHolder;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.JavaDocUtils;
import com.dspot.declex.util.ParamUtils;
import com.dspot.declex.util.SharedRecords;
import com.dspot.declex.util.TypeUtils;
import org.androidannotations.internal.virtual.VirtualElement;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.AbstractJType;
import com.helger.jcodemodel.JAnnotationUse;
import com.helger.jcodemodel.JAnonymousClass;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JClassAlreadyExistsException;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JTryBlock;
import com.helger.jcodemodel.JVar;

public class EventsHelper {
	
	private final static String INIT_NAME = "init";
	private final static String BUILD_NAME = "build";
	private final static String EXECUTE_NAME = "execute";
	
	private AndroidAnnotationsEnvironment environment;
	
	private final Map<String, Map<String, String>> eventsFields = new HashMap<>();
	
	private static EventsHelper instance;
	
	public static EventsHelper getInstance(AndroidAnnotationsEnvironment environment) {
		if (instance == null) {
			instance = new EventsHelper(environment);
		}
		
		return instance;
	}
	
	private EventsHelper(AndroidAnnotationsEnvironment environment) {
		this.environment = environment;
	}
	
	private boolean isEvent(String name) {
		
		String eventToCheck = name.substring(0, name.lastIndexOf('.'));
		if (eventToCheck.isEmpty() || !eventToCheck.endsWith(".event")) return false;
		
		if (eventToCheck.equals(DeclexConstant.EVENT_PATH)) return true;

		Element element = environment.getProcessingEnvironment().getElementUtils().getTypeElement(name);
		if (element == null) return false;

		boolean isEvent = false;

		if (element.getAnnotation(UseEvents.class) != null) isEvent = true;

		if (!isEvent && element.getSimpleName().toString().endsWith(ModelConstants.generationSuffix())) {

			//Check in the super class
			List<? extends TypeMirror> superTypes = environment.getProcessingEnvironment().getTypeUtils().directSupertypes(element.asType());
			for (TypeMirror type : superTypes) {
				TypeElement superElement = environment.getProcessingEnvironment().getElementUtils().getTypeElement(type.toString());
				if (superElement == null) continue;

				if (superElement.getAnnotation(UseEvents.class) != null) isEvent = true;
			}

		}

		if (isEvent) {
			registerEvent(element);
		}

		return isEvent;

	}
		
	private void registerEvent(Element element) {
		
		final Map<String, String> fields = new HashMap<>();
		
		for (Element elem : element.getEnclosedElements()) {
			
			if (elem.getKind().isField()) {
				if (elem.getModifiers().isEmpty()) {
					fields.put(elem.getSimpleName().toString(), elem.asType().toString());
				}
			}
			
		}
		
		registerEvent(element.asType().toString(), fields);
		
	}

	private static void createEventInfo(String className, AndroidAnnotationsEnvironment env) {
		if (!className.contains(".")) {
			className = DeclexConstant.EVENT_PATH + className;
		}
		final int index = className.lastIndexOf('.');
		final String eventName = className.substring(index + 1);
		
		ActionInfo actionInfo = new ActionInfo(className);
		Actions.getInstance().addAction(eventName, className, actionInfo);
		
		actionInfo.addMethod(INIT_NAME, env.getCodeModel().VOID.fullName());
		
		actionInfo.addMethod(
				BUILD_NAME, 
				env.getCodeModel().VOID.fullName(),
				Arrays.asList(
					new ActionMethodParam("Finished", env.getJClass(className + ".EventFinishedRunnable")),
					new ActionMethodParam("Failed", env.getJClass(OnFailedRunnable.class))
				)
			);
		
		actionInfo.addMethod(EXECUTE_NAME, env.getCodeModel().VOID.fullName());
	}
		
	public void registerEvent(String className) {
		registerEvent(className, new HashMap<String, String>(0));
		
		//Ensure to register this event (ex. if it is a library event)
		isEvent(className);
	}
	
	public void registerEvent(String className, Map<String, String> fields) {
		
		//Add to the Special Events in validation, so other validators will find this Event
		SharedRecords.addEventGeneratedClass(className, environment);
		
		Map<String, String> storedFields = eventsFields.get(className);
		if (storedFields == null) {
			eventsFields.put(className, fields);
		} else {
			storedFields.putAll(fields);
		}
		
		EventsHelper.createEventInfo(className, environment);
	}
	
    public void removeParameterReference(String clazz, String paramName) {
		
		if (!eventsFields.containsKey(clazz)) return;
		
		Map<String, String>  fields = eventsFields.get(clazz);
		fields.remove(paramName);
	}

	public void registerAsEventListener(EComponentHolder holder) {
		AbstractJClass EventBus = environment.getJClass("org.greenrobot.eventbus.EventBus");
		
		JMethod registerMethod = holder.getGeneratedClass().getMethod("registerWithEventBus_", new AbstractJType[]{});
		if (registerMethod == null) {
			registerMethod = holder.getGeneratedClass().method(JMod.PRIVATE, environment.getCodeModel().VOID, "registerWithEventBus_");
			JTryBlock tryBlock = registerMethod.body()._try();
			tryBlock.body().add(EventBus.staticInvoke("getDefault").invoke("register").arg(_this()));
			tryBlock._catch(environment.getClasses().THROWABLE);
			
			if (holder instanceof EActivityHolder) {
        		((EActivityHolder) holder).getOnResumeAfterSuperBlock().invoke(registerMethod);
        	} else if (holder instanceof EFragmentHolder) {
        		((EFragmentHolder) holder).getOnResumeAfterSuperBlock().invoke(registerMethod);
        	} else if (holder instanceof EBeanHolder) {
        		holder.getInitBody().invoke(registerMethod);
        	} else if (holder instanceof EApplicationHolder) {
        		holder.getInitBody().invoke(registerMethod);
        	} else if (holder instanceof EServiceHolder) {
				holder.getInit();
        		((EServiceHolder) holder).getStartLifecycleAfterSuperBlock().invoke(registerMethod);
        	} else if (holder instanceof EProviderHolder) {
        		((EProviderHolder) holder).getOnCreateBody().invoke(registerMethod);
        	} else if (holder instanceof EReceiverHolder) {
        		//Not supported
        	} else if (holder instanceof EViewHolder) {
        		((EViewHolder) holder).getStartLifecycleAfterSuperBlock().invoke(registerMethod);
        	} 
			
		}
		
		JMethod unregisterMethod = holder.getGeneratedClass().getMethod("unregisterWithEventBus_", new AbstractJType[]{});
		if (unregisterMethod == null) {
			unregisterMethod = holder.getGeneratedClass().method(JMod.PRIVATE, environment.getCodeModel().VOID, "unregisterWithEventBus_");
			JTryBlock tryBlock = unregisterMethod.body()._try();
			tryBlock.body().add(EventBus.staticInvoke("getDefault").invoke("unregister").arg(_this()));
			tryBlock._catch(environment.getClasses().THROWABLE);
			
			if (holder instanceof EActivityHolder) {
        		((EActivityHolder) holder).getOnPauseBeforeSuperBlock().invoke(unregisterMethod);
        	} else if (holder instanceof EFragmentHolder) {
        		((EFragmentHolder) holder).getOnPauseBeforeSuperBlock().invoke(unregisterMethod);
        	} else if (holder instanceof EBeanHolder) {
        		//Not supported
        	} else if (holder instanceof EApplicationHolder) {
        		//Not supported
        	} else if (holder instanceof EServiceHolder) {
        		((EServiceHolder) holder).getEndLifecycleBeforeSuperBlock().invoke(unregisterMethod);
        	} else if (holder instanceof EProviderHolder) {
        		//Not supported
        	} else if (holder instanceof EReceiverHolder) {
        		//Not supported
        	} else if (holder instanceof EViewHolder) {
        		((EViewHolder) holder).getEndLifecycleBeforeSuperBlock().invoke(unregisterMethod);
        	} 
		}
		
	}
    
	public AbstractJClass createEvent(String className, Element fromElement) {
		return createEvent(className, fromElement, false);
	}
	
	public AbstractJClass createEvent(String className, Element fromElement, boolean parametersContainer) {
		
		if (!className.contains(".")) {
			className = DeclexConstant.EVENT_PATH + className;
		}

		//If the class was already created, do nothing (ex. events in libraries)
        Element existingElement = environment.getProcessingEnvironment().getElementUtils().getTypeElement(className);
        if (existingElement != null) {
            return environment.getJClass(existingElement.asType().toString());
        }

		final int index = className.lastIndexOf('.');
		final String eventName = className.substring(index + 1);

		final ActionInfo actionInfo = Actions.getInstance().getActionInfos().get(className);

		final String reference = JavaDocUtils.referenceFromElement(fromElement);
		
		JDefinedClass EventClass;
		try {
			
			if (!parametersContainer && eventRegisteredAndHasParameters(className)) {
				//This means an @Event with parameters registered and it will create the class
				throw new RuntimeException();
			}
			
			EventClass = environment.getCodeModel()._class(className);
			
			actionInfo.setReferences(reference);
			EventClass.javadoc().add(actionInfo.references);
			
			Element rootElement = TypeUtils.getRootElement(fromElement);
			
		} catch (JClassAlreadyExistsException e) {
			
			if (actionInfo.references == null) actionInfo.setReferences(reference);
			else {
				//TODO this is not working
				actionInfo.setReferences(
						actionInfo.references + "\n<br>\n" + reference.replace("Created by ", "Referenced by ")
					);
			}
			
			if (parametersContainer) {
				return environment.getCodeModel()._getClass(className);
			}
			
			return environment.getJClass(className);
			
		} catch (RuntimeException e) {
			if (actionInfo.references == null) actionInfo.setReferences(reference);
			else {
				//TODO this is not working
				actionInfo.setReferences(
						actionInfo.references + "\n<br>\n" + reference.replace("Created by ", "Referenced by ")
					);
			}
			
			return environment.getJClass(className);
		}
		
		
		EventClass.annotate(EBean.class);
		EventClass.annotate(UseEvents.class);

		JAnnotationUse actionFor = EventClass.annotate(ActionFor.class);
		actionFor.param("value", eventName);				
		
		JFieldVar finished = EventClass.field(JMod.PRIVATE, environment.getJClass("EventFinishedRunnable"), "Finished");
		JFieldVar failed = EventClass.field(JMod.PRIVATE, environment.getJClass(OnFailedRunnable.class), "Failed");
		
		AbstractJClass EventClass_ = environment.getJClass(className + ModelConstants.generationSuffix());
		JFieldVar event = EventClass.field(JMod.PRIVATE, EventClass_, "event");
		
		JMethod initMethod = EventClass.method(JMod.NONE, environment.getCodeModel().VOID, INIT_NAME);
		initMethod.body().assign(event, EventClass_.staticInvoke("create"));
				
		JMethod buildMethod = EventClass.method(JMod.NONE, environment.getCodeModel().VOID, BUILD_NAME);
		JVar finishedParam = buildMethod.param(environment.getJClass("EventFinishedRunnable"), "Finished");
		buildMethod.body().assign(_this().ref(finished), finishedParam);
		
		AbstractJClass EventBus = environment.getJClass("org.greenrobot.eventbus.EventBus");
		JVar failedParam = buildMethod.param(environment.getJClass(OnFailedRunnable.class), "Failed");
		buildMethod.body().assign(_this().ref(failed), failedParam);
				
		JMethod executeMethod = EventClass.method(JMod.NONE, environment.getCodeModel().VOID, EXECUTE_NAME);
		
		JBlock ifNoSubscriber = executeMethod.body()
				._if(EventBus.staticInvoke("getDefault").invoke("hasSubscriberForEvent").arg(EventClass_.dotclass()).not())
				._then();
		ifNoSubscriber._if(failed.neNull())._then().invoke(failed, "onFailed").arg(_null());
		ifNoSubscriber._return();
		
		{//Next Listener
			JAnonymousClass nextListenerRunnable = environment.getCodeModel().anonymousClass(Runnable.class);
			JFieldVar called = nextListenerRunnable.field(JMod.NONE, environment.getCodeModel().BOOLEAN, "called");
			JMethod anonymousRunnableRun = nextListenerRunnable.method(JMod.PUBLIC, environment.getCodeModel().VOID, "run");
			anonymousRunnableRun.annotate(Override.class);
	
			JBlock ifNotCalled = anonymousRunnableRun.body()._if(called.not())._then();
			ifNotCalled.assign(called, JExpr.TRUE);
			ifNotCalled.invoke(finished, "onEventFinished").arg(event);
			
			JConditional ifFinished = executeMethod.body()._if(finished.ne(_null()));
			ifFinished._then().add(event.invoke("setNextListener").arg(_new(nextListenerRunnable)));
		}
		
		{//Failed Listener
			JAnonymousClass failedListenerRunnable = environment.getCodeModel().anonymousClass(OnFailedRunnable.class);
			JFieldVar called = failedListenerRunnable.field(JMod.NONE, environment.getCodeModel().BOOLEAN, "called");
			JMethod anonymousRunnableRun = failedListenerRunnable.method(JMod.PUBLIC, environment.getCodeModel().VOID, "run");
			anonymousRunnableRun.annotate(Override.class);
	
			JBlock ifNotCalled = anonymousRunnableRun.body()._if(called.not())._then();
			ifNotCalled.assign(called, JExpr.TRUE);
			ifNotCalled.invoke(failed, "onFailed").arg(ref("e"));
			
			JConditional ifFailed = executeMethod.body()._if(failed.ne(_null()));
			ifFailed._then().add(event.invoke("setFailedListener").arg(_new(failedListenerRunnable)));
		}
		
		executeMethod.body().add(event.invoke("postEvent"));
		
		try {
			JDefinedClass EventFinishedRunnable = EventClass._class(JMod.PUBLIC | JMod.ABSTRACT | JMod.STATIC, "EventFinishedRunnable");
			EventFinishedRunnable._implements(Runnable.class);
			
			JFieldVar eventField = EventFinishedRunnable.field(JMod.PUBLIC, EventClass_, "event");
			JMethod onEventFinished = EventFinishedRunnable.method(JMod.PUBLIC, environment.getCodeModel().VOID, "onEventFinished");
			JVar eventParam = onEventFinished.param(EventClass_, "event");
			onEventFinished.body().assign(_this().ref(eventField), eventParam);
			onEventFinished.body().invoke("run");
		} catch (JClassAlreadyExistsException e) {
		}

		return EventClass;
	}
	
	private boolean eventRegisteredAndHasParameters(String clazz) {
		return eventsFields.containsKey(clazz)
		       && eventsFields.get(clazz) != null 
		       && !eventsFields.get(clazz).isEmpty();
	}
	
	public JBlock addEventListener(String eventClass, Element parentElement, ViewsHolder viewsHolder) {
		return addEventListener(eventClass, parentElement, viewsHolder.holder(), viewsHolder);
	}
	
	public JBlock addEventListener(String eventClass, Element parentElement, BaseGeneratedClassHolder holder) {
		return addEventListener(eventClass, parentElement, holder, null);
	}
	
	public JBlock addEventListener(String eventClass, Element element, BaseGeneratedClassHolder holder, ViewsHolder viewsHolder) {
		
		eventClass = TypeUtils.getGeneratedClassName(eventClass, environment);
		String eventSimpleName = eventClass;
		Matcher matcher = Pattern.compile("\\.([A-Za-z_][A-Za-z0-9_]+)$").matcher(eventClass);
		if (matcher.find()) {
			eventSimpleName = matcher.group(1);
		}
				
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
		
		final String originalEventClassName = SharedRecords.getEvent(eventClass, environment);
		final EventHolder eventHolder = holder.getPluginHolder(new EventHolder(holder));
		final JBlock eventBody = eventHolder.getEventBlock(eventClass);
		
		if (element instanceof ExecutableElement) {
			
			ExecutableElement executableElement = (ExecutableElement) element;								
			List<? extends VariableElement> parameters = executableElement.getParameters();
			
			JInvocation invocation = eventBody.invoke(executableElement.getSimpleName().toString());
			if (parameters.size() != 0) {				
				TypeElement OriginalEventClass = environment.getProcessingEnvironment().getElementUtils().getTypeElement(originalEventClassName);
				
				Map<String, String> eventFields = new TreeMap<String, String>();
				if (OriginalEventClass != null && OriginalEventClass.toString().contains(".")) {
					List<? extends Element> els = OriginalEventClass.getEnclosedElements();
					for (Element el : els) {
						if (el.getKind() == ElementKind.FIELD) {
							final String paramName = el.getSimpleName().toString();
							String paramType = el.asType().toString();
							
							if (el.getModifiers().contains(Modifier.STATIC)) continue;
							if (el.getModifiers().contains(Modifier.PRIVATE)) continue;
							
							if (!paramType.equals(TypeUtils.getGeneratedClassName(originalEventClassName, environment)) 
								&& !paramType.equals(originalEventClassName)) {
								eventFields.put(paramName, paramType);
							}
						}
					}
				} else {
					eventFields = eventsFields.get(originalEventClassName);						
					if (eventFields == null) {
						eventFields = new TreeMap<String, String>();
						System.out.println("NO EVENT FOR " + originalEventClassName + " in " + eventsFields);
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
							&& (paramType.equals(TypeUtils.getGeneratedClassName(originalEventClassName, environment))  
								|| paramType.equals(originalEventClassName))) {
							
							invocation = invocation.arg(ref("event"));
						} else {
							ParamUtils.injectParam(paramName, param.asType().toString(), invocation, viewsHolder);
						}
					}
				}
			}
		}
		
		return eventBody;
	}

}
