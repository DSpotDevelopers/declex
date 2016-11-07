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
package com.dspot.declex.action.sequence;

import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr.direct;

import java.util.List;

import javax.lang.model.element.Element;

import org.androidannotations.holder.EComponentWithViewSupportHolder;

import com.dspot.declex.action.ActionPlugin;
import com.dspot.declex.api.action.sequence.S1;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;

public class SequenceActionHandler extends BaseListActionHandler {

	private JBlock elseBlockForRunnableRef = null;
	private IJStatement savedStatement;
	
	public SequenceActionHandler(List<ActionPlugin> plugins) {
		super(plugins);
	}

	@Override
	protected Class<?> getBaseClass() {
		return S1.class;
	}
	
	@Override
	public void process(String[] parameters, Element element,
			EComponentWithViewSupportHolder holder) {
		elseBlockForRunnableRef = null;
		savedStatement = null;
		
		super.process(parameters, element, holder);
		
		if (savedStatement != null) this.statement = savedStatement;
	}

	@Override
	protected boolean passNextRunnableParameter() {
		return true;
	}

	@Override
	protected JBlock createSequenceCaller(String name,
			EComponentWithViewSupportHolder holder) {		
		JDefinedClass annonimousRunnable = environment.getCodeModel().anonymousClass(Runnable.class);
		JMethod annonimousRunnableRun = annonimousRunnable.method(JMod.PUBLIC, environment.getCodeModel().VOID, "run");
		annonimousRunnableRun.annotate(Override.class);
		
		holder.getGeneratedClass().field(JMod.PRIVATE, Runnable.class, name, _new(annonimousRunnable));
		
		return annonimousRunnableRun.body();
	}

	@Override
	protected void addCallStatements(List<IJStatement> callStatements) {
		if (elseBlockForRunnableRef != null && callStatements.size() >= 2) {
			elseBlockForRunnableRef.add(callStatements.get(1));
		}
		
		JBlock block = (JBlock) this.statement;
		block.add(callStatements.get(0));
	}

	@Override
	protected boolean processLocalInst(String inst) {
		JBlock block = (JBlock) this.statement;
		
		if (inst.endsWith("?")) {
			savedStatement = this.statement;
			
			boolean useElseBlock = false;
			
			String ifCond = inst.substring(0, inst.length()-1);
			if (ifCond.endsWith("!")) {
				ifCond = ifCond.substring(0, ifCond.length()-1);
				useElseBlock = true;
			}
			
			if (ifCond.endsWith("?")) {
				ifCond = ifCond.substring(0, ifCond.length()-1);
				useElseBlock = true;
			}
			
			JConditional conditional = block._if(direct(ifCond)); 
			this.statement = conditional._then();
			
			if (useElseBlock) {
				elseBlockForRunnableRef = conditional._else(); 
			}
			
			return true;
		}
		
		return false;
	}

}
