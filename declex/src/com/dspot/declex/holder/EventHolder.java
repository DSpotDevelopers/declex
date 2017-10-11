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
package com.dspot.declex.holder;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.holder.BaseGeneratedClassHolder;
import org.androidannotations.plugin.PluginClassHolder;

import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JAnnotationUse;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JCatchBlock;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JTryBlock;
import com.helger.jcodemodel.JVar;

public class EventHolder extends PluginClassHolder<BaseGeneratedClassHolder> {
	
	private Map<String, JMethod> eventsMethod = new HashMap<>();
	private Map<String, JBlock> eventsBlock = new HashMap<>();
	
	public EventHolder(BaseGeneratedClassHolder holder) {
		super(holder);
	}
	
	public JMethod getEventMethod(String clazz) {
		
		JMethod eventMethod = eventsMethod.get(clazz);
		if (eventMethod == null) {
			setEventMethod(clazz);
		}

		return eventsMethod.get(clazz);
	}
	
	public JBlock getEventBlock(String clazz) {
		
		JBlock eventBlock = eventsBlock.get(clazz);
		if (eventBlock == null) {
			setEventMethod(clazz);
		}

		return eventsBlock.get(clazz);
	}

	
	private void setEventMethod(String clazz) {
		final AndroidAnnotationsEnvironment environment = environment();
		
		String eventSimpleName = clazz;
		Matcher matcher = Pattern.compile("\\.([A-Za-z_][A-Za-z0-9_]+)$").matcher(clazz);
		if (matcher.find()) {
			eventSimpleName = matcher.group(1);
		}
		
		AbstractJClass EventClass = environment.getJClass(clazz);

		//--------------------------------------------
		//Create holder method
		//--------------------------------------------
		final JMethod eventMethod = getGeneratedClass().method(JMod.PUBLIC, environment.getCodeModel().VOID, "on" + eventSimpleName);
		JAnnotationUse subscribe = eventMethod.annotate(environment.getJClass("org.greenrobot.eventbus.Subscribe"));
		subscribe.param("threadMode", environment.getJClass("org.greenrobot.eventbus.ThreadMode").staticRef("MAIN"));
		JVar event = eventMethod.param(EventClass, "event");
		
		JTryBlock tryBlock = eventMethod.body()._try();
		JBlock eventBody = tryBlock.body().blockVirtual(); 
		
		//If there's a nextRunnable, then run it
		JBlock ifBlock = tryBlock.body()._if(event.invoke("getNextListener").neNull())._then();
		ifBlock.invoke(event.invoke("getNextListener"), "run");		

		JCatchBlock catchBlock = tryBlock._catch(getClasses().THROWABLE);
		JVar e = catchBlock.param("e");				
		ifBlock = catchBlock.body()._if(event.invoke("getFailedListener").neNull())._then();
		ifBlock.invoke(event.invoke("getFailedListener"), "onFailed").arg(e);
		
		eventsMethod.put(clazz, eventMethod);
		eventsBlock.put(clazz, eventBody);
	}
		
}
