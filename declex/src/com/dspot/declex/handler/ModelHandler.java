/**
 * Copyright (C) 2016-2018 DSpot Sp. z o.o
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
import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.ref;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

import com.dspot.declex.annotation.*;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.helper.CompilationTreeHelper;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;
import org.androidannotations.rclass.IRClass.Res;

import com.dspot.declex.annotation.ExportPopulate;
import com.dspot.declex.api.util.FormatsUtils;
import com.dspot.declex.helper.EventsHelper;
import com.dspot.declex.holder.ModelHolder;
import com.dspot.declex.holder.UseModelHolder;
import com.dspot.declex.holder.ViewsHolder;
import com.dspot.declex.holder.ViewsHolder.WriteInBlockWithResult;
import com.dspot.declex.holder.view_listener.ClickHolder;
import com.dspot.declex.util.SharedRecords;
import com.dspot.declex.util.TypeUtils;
import org.androidannotations.internal.virtual.VirtualElement;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JVar;

public class ModelHandler extends BaseAnnotationHandler<EComponentHolder> {
		
	private static final Logger LOGGER = LoggerFactory.getLogger(ModelHandler.class);
	
	private EventsHelper eventsHelper;
	private CompilationTreeHelper compilationTreeHelper;
	
	public ModelHandler(AndroidAnnotationsEnvironment environment) {
		super(Model.class, environment);		
		eventsHelper = EventsHelper.getInstance(environment);
		compilationTreeHelper = new CompilationTreeHelper(environment);
	}

	@Override
	public void validate(Element element, ElementValidation valid) {
		
		validatorHelper.enclosingElementHasEnhancedComponentAnnotation(element, valid);

		validatorHelper.isNotPrivate(element, valid);
		
		String className = element.asType().toString();
		if (className.endsWith(ModelConstants.generationSuffix())) className = className.substring(0, className.length()-1);
		
		String enclosingClassName = element.getEnclosingElement().asType().toString(); 
		if (enclosingClassName.equals(className) || enclosingClassName.endsWith("." + className)) {
			
			if (!adiHelper.getAnnotation(element, Model.class).lazy()) {
				valid.addError( 
						"You cannot inject a @Model from inside itself, this can cause an infinite loop. "
						+ "It can be done only with lazy loads (lazy = true)"
					);
			}
			
		}
		
		if (element.getModifiers().contains(Modifier.STATIC)) {

			if (!adiHelper.hasAnnotation(element.getEnclosingElement(), UseModel.class)) {
				 valid.addError("Static @Models only permitted inside @UseModel annotated elements");
			 }
			 
			 if (adiHelper.getAnnotation(element, Model.class).async()) {
				 valid.addError("Asynchronous model injection not permitted in static @Models");
			 }
		}
		
		PutOnEvent putOnEvent = element.getAnnotation(PutOnEvent.class);
		PutOnAction putOnAction = element.getAnnotation(PutOnAction.class);
		if (putOnEvent != null || putOnAction != null) {
			if (putOnAction != null) {
				validatorHelper.enclosingElementHasEnhancedViewSupportAnnotation(element, valid);
			}
		}
				
	}

	@Override
	public void process(Element element, EComponentHolder holder) {
		
		JBlock block;
		boolean checkNull = false;
		
		final boolean hasExport = adiHelper.getAnnotation(element, ExportPopulate.class) != null
                                    || adiHelper.getAnnotation(element, ExportRecollect.class) != null;

		final ModelHolder modelHolder = holder.getPluginHolder(new ModelHolder(holder));	
		final UseModelHolder useModelHolder = holder.getPluginHolder(new UseModelHolder(holder));
		
		final ViewsHolder viewsHolder;
		if (holder instanceof EComponentWithViewSupportHolder) {
			viewsHolder = holder.getPluginHolder(
					new ViewsHolder((EComponentWithViewSupportHolder) holder)
				);
		} else {
			viewsHolder = null;
		}
		
		//If there's a LoadOnEvent annotation, then instantiate the object only on that event
		LoadOnEvent loadOnEvent = element.getAnnotation(LoadOnEvent.class);
		if (loadOnEvent != null) {
			String classField = annotationHelper.extractAnnotationClassNameParameter(element, LoadOnEvent.class.getCanonicalName(), "value");
			String eventClass = SharedRecords.getEvent(classField, getEnvironment());
			
			if (classField == null || eventClass == null) {
				LOGGER.error("@LoadOnEvent failed in " + classField, element, loadOnEvent);
				return;
			}
			
			block = eventsHelper.addEventListener(eventClass, element.getEnclosingElement(), viewsHolder);
		} else {
			block = holder.getInitBodyInjectionBlock();
			
			//If this is a lazy loading model, create or change the getter method
			if (adiHelper.getAnnotation(element, Model.class).lazy()) {
				
				//In the init(), set the field to null
				if (adiHelper.hasAnnotation(element.getEnclosingElement(), UseModel.class)) { 
					block._if(useModelHolder.getFullInitVar())._then()
					     .invoke(modelHolder.getSetterMethod(element)).arg(_null());
				} else {
					if (element.getAnnotation(Extra.class)==null && element.getAnnotation(FragmentArg.class)==null) {
						block.invoke(modelHolder.getSetterMethod(element)).arg(_null());
					}	
				}
								
				if (!hasExport) {
					block = modelHolder.getGetterBody(element)
							           ._if(ref(element.getSimpleName().toString()).eq(_null()))._then();
				} else {
					block = null;
				}
				
			} else {
				checkNull = true;
			}
		}
		
		//If the element is static, then the declaration block is inside the getModel(s) of enclosing element
		if (element.getModifiers().contains(Modifier.STATIC)) {
			if (!adiHelper.getAnnotation(element, Model.class).lazy()) {
				block = useModelHolder.getGetModelInitBlock();
				generateGetModelCallInBlock(block, false, element, modelHolder, useModelHolder, true);
				
				block = useModelHolder.getGetModelListInitBlock();
				generateGetModelCallInBlock(block, false, element, modelHolder, useModelHolder, true);				
			} else {			
				//It is important to avoid this behavior for Exported fields, because this
				//causes infinite loops
				if (!hasExport) {
					generateGetModelCallInBlock(block, false, element, modelHolder, useModelHolder, true);
				}
			}
			
		} else {
			if (block != null) {
				generateGetModelCallInBlock(block, checkNull, element, modelHolder);
			}
		}
		
		PutOnEvent putOnEvent = element.getAnnotation(PutOnEvent.class);
		if (putOnEvent != null) {
			String classField = annotationHelper.extractAnnotationClassNameParameter(element, PutOnEvent.class.getCanonicalName(), "value");
			String eventClass = SharedRecords.getEvent(classField, getEnvironment());
			
			if (classField == null || eventClass == null) {
				LOGGER.error("@PutOnEvent failed in " + classField, element, putOnEvent);
				return;
			}
			
			block = eventsHelper.addEventListener(eventClass, element.getEnclosingElement(), viewsHolder);
			generatePutModelCallInBlock(block, element, modelHolder, true);
		}
		
		if (viewsHolder != null) {
			PutOnAction putOnAction = element.getAnnotation(PutOnAction.class);
			if (putOnAction != null) {
				
				final ClickHolder listenerHolder = holder.getPluginHolder(new ClickHolder(viewsHolder.holder()));
				
				String referecedId = getEnvironment().getRClass().get(Res.ID).getIdQualifiedName(putOnAction.value()[0]);
				referecedId = referecedId.substring(referecedId.lastIndexOf('.') + 1);
				
				if (viewsHolder.layoutContainsId(referecedId)) {
					WriteInBlockWithResult<JBlock> writeInBlockWithResult = new WriteInBlockWithResult<JBlock>() {

						@Override
						public void writeInBlock(String viewName, AbstractJClass viewClass, JFieldRef view, JBlock block) {
							result = listenerHolder.createListener(viewName, block);
							
						}
					};
					viewsHolder.createAndAssignView(referecedId, writeInBlockWithResult);
					
					JBlock putOnBlock = writeInBlockWithResult.getResult();
					generatePutModelCallInBlock(putOnBlock, element, modelHolder, false);
				}
			}	
		}
		
		//If an UpdateOnEvent is present, add the call to the provided Event
		UpdateOnEvent updateOnEvent = element.getAnnotation(UpdateOnEvent.class);
		if (updateOnEvent != null) {
			String classField = annotationHelper.extractAnnotationClassNameParameter(element, UpdateOnEvent.class.getCanonicalName(), "value");
			String eventClass = SharedRecords.getEvent(classField, getEnvironment());
			
			if (classField == null || eventClass == null) {
				LOGGER.error("@UpdateOnEvent failed in " + classField, element, updateOnEvent);
				return;
			}
			
			block = eventsHelper.addEventListener(eventClass, element.getEnclosingElement(), viewsHolder);
			generateGetModelCallInBlock(block, false, element, modelHolder);				
		}
	}
	
	private void generateGetModelCallInBlock(JBlock block, boolean checkNull, Element element, ModelHolder holder) {
		this.generateGetModelCallInBlock(block, checkNull, element, holder, null, false);
	}
	
	private void generateGetModelCallInBlock(final JBlock block, final boolean checkNull, final Element element, 
			final ModelHolder modelHolder, final UseModelHolder useModelHolder, final boolean isStatic) {

		compilationTreeHelper.visitElementTree(element, new TreePathScanner<Boolean, Trees>() {

			CompilationUnitTree compilationUnit = compilationTreeHelper.getCompilationUnitImportFromElement(element);

			@Override
			public Boolean visitAnnotation(AnnotationTree annotationTree, Trees trees) {
				String annotationName = annotationTree.getAnnotationType().toString();
				if (annotationName.equals(Model.class.getSimpleName())
						|| annotationName.equals(Model.class.getCanonicalName())) {

					int position = (int) trees.getSourcePositions().getStartPosition(compilationUnit, annotationTree);

					Model annotation = adiHelper.getAnnotation(element, Model.class);

					//Get the internal calling method
					JMethod getModelMethod = modelHolder.getLoadModelMethod(element);

					JBlock callBlock = new JBlock();
					JVar args = callBlock.decl(
							getClasses().MAP.narrow(String.class, Object.class),
							"loadModelArgs",
							_new(getClasses().HASH_MAP)
					);

					{
						final IJExpression queryExpr = FormatsUtils.expressionFromString(annotation.query());
						final IJExpression orderByExpr = FormatsUtils.expressionFromString(annotation.orderBy());
						final IJExpression fieldsExpr = FormatsUtils.expressionFromString(annotation.fields());

						if (!annotation.query().isEmpty()) callBlock.add(args.invoke("put").arg("query").arg(queryExpr));
						if (!annotation.orderBy().isEmpty()) callBlock.add(args.invoke("put").arg("orderBy").arg(orderByExpr));
						if (!annotation.fields().isEmpty()) callBlock.add(args.invoke("put").arg("fields").arg(fieldsExpr));
					}

					IJExpression onFailed = _null();
					if (isStatic && useModelHolder != null && !annotation.lazy())
						onFailed = useModelHolder.getGetModelInitBlockOnFailed();

					JInvocation invocation = invoke(getModelMethod);
					if (isStatic) {
						invocation = invocation.arg(ref("context"));
					}
					invocation = invocation.arg(args).arg(_null()).arg(onFailed);

					if (checkNull) {
						callBlock._if(invoke(modelHolder.getGetterMethod(element)).eq(_null()))
								._then().add(invocation);
					} else {
						callBlock.add(invocation);
					}

					SharedRecords.priorityAdd(block, callBlock, position);
				}

				return super.visitAnnotation(annotationTree, trees);
			}

		});
	}

	private void generatePutModelCallInBlock(JBlock block, Element element, ModelHolder holder, boolean hasEvent) {
		
		final Model annotation = adiHelper.getAnnotation(element, Model.class);
		final JMethod putModelMethod = holder.getPutModelMethod(element);

		JBlock callBlock = new JBlock();
		JVar args = callBlock.decl(
			getClasses().MAP.narrow(String.class, Object.class),
			"loadModelArgs",
			_new(getClasses().HASH_MAP)
		);
		
		{
			final IJExpression queryExpr = FormatsUtils.expressionFromString(annotation.query());
			final IJExpression orderByExpr = FormatsUtils.expressionFromString(annotation.orderBy());
			final IJExpression fieldsExpr = FormatsUtils.expressionFromString(annotation.fields());
			if (!annotation.query().isEmpty()) callBlock.add(args.invoke("put").arg("query").arg(queryExpr));
			if (!annotation.orderBy().isEmpty()) callBlock.add(args.invoke("put").arg("orderBy").arg(orderByExpr));
			if (!annotation.fields().isEmpty()) callBlock.add(args.invoke("put").arg("fields").arg(fieldsExpr));
		}
		
		if (hasEvent) {
			
			callBlock.invoke(putModelMethod).arg(args)
                    .arg(ref("event").invoke("getNextListener"))
                    .arg(ref("event").invoke("getFailedListener"));
			
		} else {
			callBlock.invoke(putModelMethod).arg(args)
							            .arg(_null())
							            .arg(_null());
		}
		
		block.add(callBlock);
	}

}
