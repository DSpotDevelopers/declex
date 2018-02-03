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

import static com.helger.jcodemodel.JExpr.FALSE;
import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.lit;
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
import org.androidannotations.holder.EComponentWithViewSupportHolder;

import com.dspot.declex.annotation.ExternalPopulate;
import com.dspot.declex.annotation.Model;
import com.dspot.declex.annotation.Populate;
import com.dspot.declex.api.action.runnable.OnFailedRunnable;
import com.dspot.declex.api.external.PopulateModelListener;
import com.dspot.declex.api.util.FormatsUtils;
import com.dspot.declex.holder.PopulateHolder;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.wrapper.element.VirtualElement;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JAnonymousClass;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class ExternalPopulateHandler extends ExternalHandler {
	
	private Set<Element> referencedElementsLinked = new HashSet<>();
	
	public ExternalPopulateHandler(AndroidAnnotationsEnvironment environment) {
		super(ExternalPopulate.class, environment);
	}
	
	@Override
	public void getDependencies(Element element, Map<Element, Object> dependencies) {
		dependencies.put(element, Populate.class);
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
			} else {

				if (!adiHelper.hasAnnotation(element, Model.class)) {
					final String fieldGetter = FormatsUtils.fieldToGetter(elementName);
						
					final String referenceName = ((VirtualElement) element).getReference().getSimpleName().toString();
					IJExpression invocation = invoke(ref(referenceName), fieldGetter);
					
					String referenceElementClass = ((VirtualElement) element).getReference().asType().toString();
					if (!referenceElementClass.endsWith(ModelConstants.generationSuffix())) {
						referenceElementClass = TypeUtils.getGeneratedClassName(referenceElementClass, getEnvironment());
						invocation = invoke(cast(getJClass(referenceElementClass), ref(referenceName)), fieldGetter);
					}
					
					JMethod getter = holder.getGeneratedClass().method(
							JMod.PUBLIC, 
							codeModelHelper.elementTypeToJClass(element), 
							fieldGetter
						);
					
					getter.body()._if(ref(referenceName).neNull())._then()._return(invocation);
					
					if (getter.type().fullName().equals("boolean")) getter.body()._return(FALSE);
					else if (getter.type().fullName().contains(".") 
							|| getter.type().fullName().endsWith(ModelConstants.generationSuffix())) 
					        {getter.body()._return(_null());} 
					else getter.body()._return(lit(0));
				}
			}
			
			if (!referencedElementsLinked.contains(((VirtualElement) element).getReference())) {
				createInvokeListenerStructure("this", element, holder);
				referencedElementsLinked.add(((VirtualElement) element).getReference());
			}
			                                       		
		} else {
			
			createListenerStructure(elementName, holder);
			
			if (!adiHelper.hasAnnotation(element, Model.class)) {
				if (!(element instanceof ExecutableElement)) {
					createGetter(elementName, element, holder);
				}
			}
			
			//Create "populate this" listener structure
			if (!holder.getGeneratedClass().containsField("populateThis")) {
				createListenerStructure("this", holder);
			}
		}
			
	}
	
	private void createInvokeListenerStructure(String elementName, Element element, EComponentHolder holder) {	
		final String populateListenerName = "setPopulate" + elementName.substring(0, 1).toUpperCase()
                + elementName.substring(1);
		
		JAnonymousClass listener = getCodeModel().anonymousClass(PopulateModelListener.class);
		JMethod anonymousRunnableRun = listener.method(JMod.PUBLIC, getCodeModel().VOID, "populateModel");
		anonymousRunnableRun.annotate(Override.class);
		JVar afterPopulate = anonymousRunnableRun.param(Runnable.class, "afterPopulate");
		JVar onFailed = anonymousRunnableRun.param(OnFailedRunnable.class, "onFailed");
		
		PopulateHolder populateHolder = holder.getPluginHolder(new PopulateHolder((EComponentWithViewSupportHolder)holder));
		JMethod populateMethod = elementName.equals("this")? populateHolder.getPopulateThis() : populateHolder.getPopulateMethod(element);
		
		anonymousRunnableRun.body().invoke(populateMethod).arg(afterPopulate).arg(onFailed);
		
		
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
				cast(getJClass(referenceElementClass), ref(referenceElementName)), populateListenerName
			).arg(_new(listener));
		} else {
			ifBlock.invoke(
				ref(referenceElementName), populateListenerName
			).arg(_new(listener));
		}
	}
	
	private void createGetter(String elementName, Element element, EComponentHolder holder) {
		final String fieldGetter = FormatsUtils.fieldToGetter(elementName);
		JMethod getter = holder.getGeneratedClass().method(
				JMod.PUBLIC, 
				codeModelHelper.elementTypeToJClass(element), 
				fieldGetter
			);
		getter.body()._return(ref(elementName));
	}

	private void createListenerStructure(String elementName, EComponentHolder holder) {
		//Create the listeners structure
		final String populateListenerName = "populate" + elementName.substring(0, 1).toUpperCase()
				                               + elementName.substring(1);
		
		JFieldVar listenerField = holder.getGeneratedClass().field(JMod.PRIVATE, getJClass(PopulateModelListener.class), populateListenerName);
		
		JMethod setter = holder.getGeneratedClass().method(
			JMod.PUBLIC, getCodeModel().VOID, 
			"set" + populateListenerName.substring(0, 1).toUpperCase() + populateListenerName.substring(1)
		);
		JVar listener = setter.param(PopulateModelListener.class, "listener");
		
		setter.body().assign(listenerField, listener);
	}

}
