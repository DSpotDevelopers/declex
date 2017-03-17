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
package com.dspot.declex.action.sequence;

import java.util.List;

import org.androidannotations.holder.EComponentWithViewSupportHolder;

import com.dspot.declex.action.ActionPlugin;
import com.dspot.declex.api.action.sequence.P1;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JBlock;

public class ParallelActionHandler extends BaseListActionHandler {

	public ParallelActionHandler(List<ActionPlugin> plugins) {
		super(plugins);
	}

	@Override
	protected Class<?> getBaseClass() {
		return P1.class;
	}

	@Override
	protected boolean passNextRunnableParameter() {
		return false;
	}

	@Override
	protected JBlock createSequenceCaller(String name,
			EComponentWithViewSupportHolder holder) {
		return (JBlock) this.statement;
	}

	@Override
	protected void addCallStatements(List<IJStatement> callStatements) {
	}

	@Override
	protected boolean processLocalInst(String inst) {
		return false;
	}

}
