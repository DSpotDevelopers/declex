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

import java.util.Map;

import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethod;
import com.dspot.declex.api.action.process.ActionMethodParam;

public class CallActionProcessor extends BaseActionProcessor {

	@Override
	public void validate(ActionInfo actionInfo) {
		super.validate(actionInfo);
		
		ActionMethod init = getActionMethod("init");
				
		if (init.metaData != null) {
		}
	}
	
	@Override
	public void process(ActionInfo actionInfo) {
		super.process(actionInfo);
		
		ActionMethod init = getActionMethod("init");
				
		try {
			ActionMethodParam initParam = init.params.get(0);
            String literal = (String) initParam.metaData.get("literal");
			
			Map<Class<?>, Object> listenerHolders = getMethodInHolder("getPluginHolders");
			for (Object listenerHolderObject : listenerHolders.values()) {
				Class<?> ViewListenerHolder = getClass().getClassLoader()
						                                .loadClass("com.dspot.declex.event.holder.ViewListenerHolder");
				
				if (!ViewListenerHolder.isInstance(listenerHolderObject)) continue;
				
				//TODO 
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
