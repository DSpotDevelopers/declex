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

import static com.helger.jcodemodel.JExpr.ref;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.holder.EComponentWithViewSupportHolder;

import com.dspot.declex.action.sequence.BaseListActionHandler;
import com.dspot.declex.api.action.Action;
import com.dspot.declex.define.DefineHolder;
import com.dspot.declex.event.BaseEventHandler;
import com.dspot.declex.share.holder.EnsureImportsHolder;
import com.dspot.declex.share.holder.ViewsHolder;
import com.dspot.declex.util.ParamUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JInvocation;

public class ActionHandler<T extends EComponentHolder> extends BaseEventHandler<T> {
	
	private static List<ActionPlugin> plugins = new ArrayList<ActionPlugin>();
	
	protected Class<? extends Annotation> targetAnnotation;
	
	public ActionHandler(AndroidAnnotationsEnvironment environment) {
		super(Action.class, environment);
		targetAnnotation = Action.class;
	}

	public ActionHandler(Class<? extends Annotation> targetClass, AndroidAnnotationsEnvironment environment) {
		super(targetClass, environment);
		this.targetAnnotation = (Class<? extends Annotation>) targetClass;
	}

	public static void clearPlugins() {
		plugins.clear();;
	}
	
	public static void addPlugin(ActionPlugin plugin) {
		plugins.add(plugin);
	}
	
	public static List<ActionPlugin> getPlugins() {
		return plugins;
	}
	
	@Override
	public void validate(final Element element, final ElementValidation valid) {
		super.validate(element, valid);
				
		if (ActionsProcessor.hasAction(element, getEnvironment())) {
			ActionsProcessor.validateActions(element, valid, getEnvironment());
			return;
		} 
		
		if (element instanceof ExecutableElement) return;
			
		Action actionAnnotation = element.getAnnotation(Action.class);
		String[] params;
		if (actionAnnotation == null) {
			try {
				Method customMethod = targetAnnotation.getMethod("action", new Class[] {});
				params = (String[]) customMethod.invoke(element.getAnnotation(targetAnnotation), new Object[] {});
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | 
					 IllegalArgumentException | InvocationTargetException e1) {
				valid.addError("Annotation implementation does not includes a \"action\" method");
				params = new String[]{};
			} 
		} else {
			params = actionAnnotation.value();
		}

    	if (params.length == 1) {
          	
    		boolean apply = true;
    		
    		//Don't apply division to List Actions
    		for (ActionPlugin plugin : plugins) 
    			if (plugin.canProcessElement(element, getEnvironment())) {
    				if (plugin instanceof BaseListActionHandler) {
    					apply = false;
    				}
    				
    				break;
    			}
    		
    		if (apply) params = params[0].split("(?:(\\s+\\$(?!\\s))|(\\s*->\\s*))");
    	}
    	
		ActionPlugin pluginFound = null;
		for (ActionPlugin plugin : plugins) {
			if (plugin.canProcessElement(element, getEnvironment())) {
				plugin.validate(params, element, valid);
				pluginFound = plugin;
				break;
			}
		}
		
		if (pluginFound == null) {
			valid.addError("There's not a plugin for this element");
		}
		
		if (actionAnnotation != null && actionAnnotation.debug() && pluginFound!=null) {
			valid.addWarning("Using Plugin " + pluginFound.getClass().getSimpleName());
		}	
	}
	
	@Override
	protected IJStatement getStatement(AbstractJClass elementClass, Element element, ViewsHolder viewsHolder, T holder) {

		if (element instanceof ExecutableElement) {
			final String methodName = element.getSimpleName().toString();
			
			JInvocation invoke = JExpr.invoke(methodName);
			
			ExecutableElement exeElem = (ExecutableElement) element;
			for (VariableElement param : exeElem.getParameters()) {
				final String paramName = param.getSimpleName().toString();
				ParamUtils.injectParam(paramName, invoke, viewsHolder);
			}
			
			return invoke;
		}
		
		if (viewsHolder != null) {
			
			if (ActionsProcessor.hasAction(element, getEnvironment())) {
				return ref(element.getSimpleName().toString()).invoke("fire");
			};
			
			for (ActionPlugin plugin : plugins) 
				if (plugin.canProcessElement(element, getEnvironment())) {
					return plugin.getStatement();
				}
		}
		
		return null;
	}

	@Override
	public void process(Element element, T holder) {
		
		if (ActionsProcessor.hasAction(element, getEnvironment())) {
			ActionsProcessor.processActions(element, holder);
			super.process(element, holder);
			return;
		}
		
		//Action methods that were not process as DecleX Actions
		if (element instanceof ExecutableElement) {
			super.process(element, holder);
			return;
		}
		
		//Ensure R import for actions
		if (holder instanceof EComponentWithViewSupportHolder) {
			EnsureImportsHolder importsHolder = holder.getPluginHolder(new EnsureImportsHolder(holder));
			String importClass = getEnvironment().getAndroidManifest().getApplicationPackage() + ".R";
			importsHolder.ensureImport(importClass);	
	    	
	    	Action actionAnnotation = element.getAnnotation(Action.class);
			String[] params;
			if (actionAnnotation == null) {
				try {
					Method customMethod = targetAnnotation.getMethod("action", new Class[] {});
					params = (String[]) customMethod.invoke(element.getAnnotation(targetAnnotation), new Object[] {});
				} catch (NoSuchMethodException | SecurityException | IllegalAccessException | 
						 IllegalArgumentException | InvocationTargetException e1) {
					//This should not happen
					params = new String[]{};
				} 
			} else {
				params = actionAnnotation.value();
			}
			
	    	if (params.length == 1) {    		
	    		boolean apply = true;
	    		
	    		//Don't apply division to List Actions
	    		for (ActionPlugin plugin : plugins) 
	    			if (plugin.canProcessElement(element, getEnvironment())) {
	    				if (plugin instanceof BaseListActionHandler) {
	    					apply = false;
	    				}
	    				
	    				break;
	    			}
	    		
	    		if (apply) params = params[0].split("(?:(\\s+\\$(?!\\s))|(\\s*->\\s*))");
	    	} 
	    	    	
	    	final DefineHolder defineHolder = holder.getPluginHolder(
	    			new DefineHolder((EComponentWithViewSupportHolder) holder)
    			);
	    	for (int i = 0; i < params.length; i++) {
	    		
	    		Map<String, String> normalDefine = defineHolder.getNormalDefine(); 
	    		for (String define : normalDefine.keySet()) {
	    			params[i] = params[i].replace(define, normalDefine.get(define));
	    		}
	    		
	    		Map<String, String> regularDefine = defineHolder.getRegexDefine(); 
	    		for (String define : regularDefine.keySet()) {
	    			params[i] = params[i].replaceAll(define, regularDefine.get(define));
	    		}
	    	}
	    	
			for (ActionPlugin plugin : plugins) 
				if (plugin.canProcessElement(element, getEnvironment())) {
					plugin.process(
							params,
							element, (EComponentWithViewSupportHolder) holder
						);
					break;
				}	
		}
		
		super.process(element, holder);
	}

}
