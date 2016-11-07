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

import static com.helger.jcodemodel.JExpr.direct;
import static com.helger.jcodemodel.JExpr.ref;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.helper.TargetAnnotationHelper;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.internal.model.AnnotationElements;

import com.dspot.declex.api.action.Action;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JFieldRef;

public abstract class BaseActionHandler implements ActionPlugin {

	private IJStatement statement;
	
	protected AndroidAnnotationsEnvironment environment;
	protected AnnotationElements validatedModel;
	protected ProcessingEnvironment processingEnv;
	
	protected TargetAnnotationHelper annotationHelper;
	
	@Override
	public boolean canProcessElement(Element element,
			AndroidAnnotationsEnvironment environment) {
		this.validatedModel = environment.getValidatedElements();
		this.processingEnv = environment.getProcessingEnvironment();
		this.environment = environment;
		
		if (this.annotationHelper == null) {
			annotationHelper = new TargetAnnotationHelper(environment, Action.class.getCanonicalName());
		}
		
		return false;
	}
	
	@Override
	public void validate(String[] parameters, Element element,
			ElementValidation valid) {
		
	}

	@Override
	public void process(String[] parameters, Element element,
			EComponentWithViewSupportHolder holder) {
		
		String elementClass = element.asType().toString();
		if (!elementClass.endsWith("_") && useEnhancedClass()) {
			elementClass = elementClass + "_";
			elementClass = TypeUtils.typeFromTypeString(elementClass, environment);
		}
		
		this.statement = new JBlock();
		JBlock block = (JBlock) this.statement;
		
		JFieldRef runnableRef = null;
		JBlock elseBlockForRunnableRef = null;
		boolean passRunnableRef = true;
		
		for (String value : parameters) {
			if (value.startsWith("#")) continue;
			
			//This value is used to indicate a Runnable instance that should be call 
			//when this method finishes normal or asynchronously
			if (value.endsWith("!")) {
				runnableRef = ref(value.substring(0, value.length()-1));
				continue;
			}
			
			if (value.startsWith(";")) {
				block.directStatement(value.substring(1));
			}
			
			if (value.endsWith(";")) {
				continue;
			}
			
			if (value.endsWith("?")) {
				boolean useElseBlock = false;
				
				String ifCond = value.substring(0, value.length()-1);
				if (ifCond.endsWith("!")) {
					ifCond = ifCond.substring(0, ifCond.length()-1);
					useElseBlock = true;
				}
				
				if (ifCond.endsWith("?")) {
					ifCond = ifCond.substring(0, ifCond.length()-1);
					useElseBlock = true;
					passRunnableRef = false;
				}
				
				JConditional conditional = block._if(direct(ifCond)); 
				block = conditional._then();
				
				if (useElseBlock) {
					elseBlockForRunnableRef = conditional._else(); 
				}
				
				continue;
			}
		}
		
		if (elseBlockForRunnableRef != null && runnableRef != null) {
			elseBlockForRunnableRef.invoke(runnableRef, "run");
			
			if (!passRunnableRef) {
				runnableRef = null;
			}
		}
		
		block.add(getStatement(environment.getJClass(elementClass), runnableRef, parameters, element, holder));
		
		for (String value : parameters) {
			if (value.endsWith(";") && !value.startsWith(";")) {
				block.directStatement(value);
			}
		}
		
	}
	
	protected boolean useEnhancedClass() {
		return true;
	}
	
	protected abstract IJStatement getStatement(AbstractJClass elementClass, JFieldRef runnableRef, String[] parameters, Element element, EComponentWithViewSupportHolder holder);

	@Override
	public IJStatement getStatement() {
		return this.statement;
	}

}
