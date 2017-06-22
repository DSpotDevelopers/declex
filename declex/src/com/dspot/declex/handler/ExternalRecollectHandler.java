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
package com.dspot.declex.handler;

import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EComponentHolder;

import com.dspot.declex.annotation.ExternalRecollect;
import com.dspot.declex.annotation.Recollect;
import com.dspot.declex.api.action.runnable.OnFailedRunnable;
import com.dspot.declex.api.external.RecollectModelListener;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.wrapper.element.VirtualElement;
import com.helger.jcodemodel.JAnonymousClass;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class ExternalRecollectHandler extends ExternalHandler {
	
	private Set<Element> referencedElementsLinked = new HashSet<>();
	
	public ExternalRecollectHandler(AndroidAnnotationsEnvironment environment) {
		super(ExternalRecollect.class, environment);
	}
	
	@Override
	public void getDependencies(Element element, Map<Element, Object> dependencies) {
		dependencies.put(element, Recollect.class);
	}
	
	@Override
	public void validate(final Element element, final ElementValidation valid) {
		if (element instanceof VirtualElement) {
			super.validate(element, valid);
		}
	}
	
	@Override
	public void process(Element element, EComponentHolder holder) {
		
		final String elementName = element.getSimpleName().toString();
		
		if (element instanceof VirtualElement) {
			createInvokeListenerStructure(elementName, element, holder);
			
			if (element instanceof ExecutableElement) {
				super.process(element, holder);
			}
			
			if (!referencedElementsLinked.contains(((VirtualElement) element).getReference())) {
				createInvokeListenerStructure("this", element, holder);
				referencedElementsLinked.add(((VirtualElement) element).getReference());
			}
			                                       		
		} else {
			
			createListenerStructure(elementName, holder);
			
			//Create "recollect this" listener structure
			if (!holder.getGeneratedClass().containsField("recollectThis")) {
				createListenerStructure("this", holder);
			}
		}
		
		
	}
	
	private void createInvokeListenerStructure(String elementName, Element element, EComponentHolder holder) {	
		final String recollectListenerName = "setRecollect" + elementName.substring(0, 1).toUpperCase()
                + elementName.substring(1);
		
		JAnonymousClass listener = getCodeModel().anonymousClass(RecollectModelListener.class);
		JMethod anonymousRunnableRun = listener.method(JMod.PUBLIC, getCodeModel().VOID, "recollectModel");
		anonymousRunnableRun.annotate(Override.class);
		JVar afterRecollect = anonymousRunnableRun.param(Runnable.class, "afterRecollect");
		JVar onFailed = anonymousRunnableRun.param(OnFailedRunnable.class, "onFailed");
		
		anonymousRunnableRun.body().invoke("_recollect_" + elementName)
		                           .arg(afterRecollect).arg(onFailed);
		
		
		final Element referenceElement = ((VirtualElement) element).getReference();
		final String referenceElementName = referenceElement.getSimpleName().toString();
		String referenceElementClass = referenceElement.asType().toString();
		
		boolean converted = false;
		if (!referenceElementClass.endsWith(ModelConstants.generationSuffix())) {
			converted = true;
			referenceElementClass = TypeUtils.getGeneratedClassName(referenceElementClass, getEnvironment());
		}
		
		JBlock ifBlock = holder.getInitBodyAfterInjectionBlock()._if(ref(referenceElementName).neNull())._then();
		if (converted) {
			ifBlock.invoke(
				cast(getJClass(referenceElementClass), ref(referenceElementName)), recollectListenerName
			).arg(_new(listener));
		} else {
			ifBlock.invoke(
				ref(referenceElementName), recollectListenerName
			).arg(_new(listener));
		}
	}

	private void createListenerStructure(String elementName, EComponentHolder holder) {
		//Create the listeners structure
		final String recollectListenerName = "recollect" + elementName.substring(0, 1).toUpperCase()
				                               + elementName.substring(1);
		
		JFieldVar listenerField = holder.getGeneratedClass().field(JMod.PRIVATE, getJClass(RecollectModelListener.class), recollectListenerName);
		
		JMethod setter = holder.getGeneratedClass().method(
			JMod.PUBLIC, getCodeModel().VOID, 
			"set" + recollectListenerName.substring(0, 1).toUpperCase() + recollectListenerName.substring(1)
		);
		JVar listener = setter.param(RecollectModelListener.class, "listener");
		
		setter.body().assign(listenerField, listener);
	}

}
