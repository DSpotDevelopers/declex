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
package com.dspot.declex.api.action.processor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethod;
import com.dspot.declex.api.action.process.ActionProcessor;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JCodeModel;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JVar;

public abstract class BaseActionProcessor implements ActionProcessor {

	private ActionInfo actionInfo;
	
	private JVar action;
	private Object holder;
	private Object env;
	
	private void reset() {
		action = null;
		holder = null;
		env = null;		
	}
	
	@Override
	public void validate(ActionInfo actionInfo) {
		this.actionInfo = actionInfo;
		reset();
	}
	
	@Override
	public void process(ActionInfo actionInfo) {
		this.actionInfo = actionInfo;
		reset();
	}
	
	protected ActionMethod getActionMethod(String methodName) {
		return actionInfo.methods.get(methodName).get(0);
	}
	
	protected void addPreBuildBlock(IJStatement statement) {
		if (actionInfo.metaData == null) {
			actionInfo.metaData = new HashMap<>();
		}
		
		@SuppressWarnings("unchecked")
		List<IJStatement> preBuildBlocks = (List<IJStatement>) actionInfo.metaData.get("preBuildBlocks");
		if (preBuildBlocks == null) {
			preBuildBlocks = new LinkedList<>();
			actionInfo.metaData.put("preBuildBlocks", preBuildBlocks);
		}
		
		preBuildBlocks.add(statement);
	}
	
	protected void addPostBuildBlock(IJStatement statement) {
		if (actionInfo.metaData == null) {
			actionInfo.metaData = new HashMap<>();
		}
		
		@SuppressWarnings("unchecked")
		List<IJStatement> postBuildBlocks = (List<IJStatement>) actionInfo.metaData.get("postBuildBlocks");
		if (postBuildBlocks == null) {
			postBuildBlocks = new LinkedList<>();
			actionInfo.metaData.put("postBuildBlocks", postBuildBlocks);
		}
		
		postBuildBlocks.add(statement);
	}
	
	protected void addPreInitBlock(IJStatement statement) {
		if (actionInfo.metaData == null) {
			actionInfo.metaData = new HashMap<>();
		}
		
		@SuppressWarnings("unchecked")
		List<IJStatement> preInitBlocks = (List<IJStatement>) actionInfo.metaData.get("preInitBlocks");
		if (preInitBlocks == null) {
			preInitBlocks = new LinkedList<>();
			actionInfo.metaData.put("preInitBlocks", preInitBlocks);
		}
		
		preInitBlocks.add(statement);
	}
	
	protected void addPostInitBlock(IJStatement statement) {
		if (actionInfo.metaData == null) {
			actionInfo.metaData = new HashMap<>();
		}
		
		@SuppressWarnings("unchecked")
		List<IJStatement> postInitBlocks = (List<IJStatement>) actionInfo.metaData.get("postInitBlocks");
		if (postInitBlocks == null) {
			postInitBlocks = new LinkedList<>();
			actionInfo.metaData.put("postInitBlocks", postInitBlocks);
		}
		
		postInitBlocks.add(statement);
	}
	
	protected JVar getAction() {
		if (action == null) {
			action = (JVar) actionInfo.metaData.get("action"); 
		}
		return action;
	}
	
	protected Object getHolder() {
		if (holder == null) {
			holder = actionInfo.metaData.get("holder");
		}
		return holder;
	}
	
	
	protected JDefinedClass getGeneratedClass() {
		return getMethodInHolder("getGeneratedClass");
	}

	protected TypeElement getAnnotatedElement() {
		return getMethodInHolder("getAnnotatedElement");
	}

	protected ProcessingEnvironment processingEnv() {
		Object env = getEnvironment();
		try {
			Method method = env.getClass().getMethod("getProcessingEnvironment");
			return (ProcessingEnvironment) method.invoke(env);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException 
				 | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	protected AbstractJClass getJClass(String fullyQualifiedClassName) {
		Object env = getEnvironment();
		try {
			Method method = env.getClass().getMethod("getJClass", String.class);
			return (AbstractJClass) method.invoke(env, fullyQualifiedClassName);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException 
				 | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	protected JCodeModel getCodeModel() {
		Object env = getEnvironment();
		try {
			Method method = env.getClass().getMethod("getCodeModel");
			return (JCodeModel) method.invoke(env);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException 
				 | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		
		return null;
	}
		
	
	public <T> T getMethodInHolder(String methodName) {
		return ActionProcessorUtil.getMethodInHolder(methodName, getHolder());
	}
	
	public <T> T getMethodInHolder(String methodName, String subHolder, Object ... params) {
		return ActionProcessorUtil.getMethodInHolder(methodName, getHolder(), subHolder, params);
	}
	
	private Object getEnvironment() {
		if (env == null) {
			env = ActionProcessorUtil.getMethodInHolder("getEnvironment", getHolder());
		}
		return env;
	}
}
