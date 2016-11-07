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
package com.dspot.declex.api.action.processor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethod;
import com.dspot.declex.api.action.process.ActionProcessor;
import com.helger.jcodemodel.IJStatement;

public abstract class BaseActionProcessor implements ActionProcessor {

	private ActionInfo actionInfo;
	
	@Override
	public void process(ActionInfo actionInfo) {
		this.actionInfo = actionInfo;		
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
}
