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
package com.dspot.declex.model;

import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.lit;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;
import org.androidannotations.rclass.IRClass.Res;
import org.apache.commons.lang3.StringUtils;

import com.dspot.declex.api.action.PutOnAction;
import com.dspot.declex.api.eventbus.LoadOnEvent;
import com.dspot.declex.api.eventbus.PutOnEvent;
import com.dspot.declex.api.eventbus.UpdateOnEvent;
import com.dspot.declex.api.model.Model;
import com.dspot.declex.api.model.UseModel;
import com.dspot.declex.api.util.FormatsUtils;
import com.dspot.declex.event.holder.ClickHolder;
import com.dspot.declex.share.holder.ViewsHolder;
import com.dspot.declex.share.holder.ViewsHolder.WriteInBlockWithResult;
import com.dspot.declex.util.EventUtils;
import com.dspot.declex.util.SharedRecords;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.AbstractJType;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

public class ModelHandler extends BaseAnnotationHandler<EComponentHolder> {
		
	private static final Logger LOGGER = LoggerFactory.getLogger(ModelHandler.class);
	
	public ModelHandler(AndroidAnnotationsEnvironment environment) {
		super(Model.class, environment);		
	}

	@Override
	public void validate(Element element, ElementValidation valid) {
		
		validatorHelper.enclosingElementHasEnhancedComponentAnnotation(element, valid);

		validatorHelper.isNotPrivate(element, valid);
		
		String className = element.asType().toString();
		if (className.endsWith(ModelConstants.generationSuffix())) className = className.substring(0, className.length()-1);
		
		String enclosingClassName = element.getEnclosingElement().asType().toString(); 
		if (enclosingClassName.equals(className) || enclosingClassName.endsWith("." + className)) {
			
			if (!element.getAnnotation(Model.class).lazy()) {
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
			 
			 if (element.getAnnotation(Model.class).async()) {
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
		
		String[] fields = element.getAnnotation(Model.class).fields();
		if (fields.length > 0) {
			for (String field : fields) {
				//TODO validate fields
			}
		}
	}

	@Override
	public void process(Element element, EComponentHolder holder) {
		
		JBlock block;
		final ModelHolder modelHolder = holder.getPluginHolder(new ModelHolder(holder));	
		final UseModelHolder useModelHolder = holder.getPluginHolder(new UseModelHolder(holder));
		
		final ViewsHolder viewsHolder;
		if (holder instanceof EComponentWithViewSupportHolder) {
			viewsHolder = holder.getPluginHolder(
					new ViewsHolder((EComponentWithViewSupportHolder) holder, annotationHelper)
				);
		} else {
			viewsHolder = null;
		}
		
		//If there's a LoadOnEvent annotation, then instantiate the object only on that event
		LoadOnEvent loadOnEvent = element.getAnnotation(LoadOnEvent.class);
		if (loadOnEvent != null) {
			String classField = TypeUtils.getClassFieldValue(element, LoadOnEvent.class.getCanonicalName(), "value", getEnvironment());
			String eventClass = SharedRecords.getEvent(classField, getEnvironment());
			
			if (classField == null || eventClass == null) {
				LOGGER.error("@LoadOnEvent failed in " + classField, element, loadOnEvent);
				return;
			}
			
			block = EventUtils.getEventMethod(eventClass, element.getEnclosingElement(), viewsHolder, getEnvironment()).body();
		} else {
			block = holder.getInitBodyInjectionBlock();
			
			//If this is a lazy loading model, create or change the getter method
			if (element.getAnnotation(Model.class).lazy()) {
				
				boolean isStatic = element.getModifiers().contains(Modifier.STATIC);
				
				AbstractJType[] params = isStatic ? new AbstractJType[]{getClasses().CONTEXT} : new AbstractJType[]{};
				JMethod getter = holder.getGeneratedClass().getMethod(
						FormatsUtils.fieldToGetter(element.getSimpleName().toString()),
						params
					);
				if (getter == null) {
					String elemType = TypeUtils.typeFromTypeString(element.asType().toString(), getEnvironment());
					AbstractJClass elemClass = getJClass(elemType);
					
					Matcher matcher = Pattern.compile("<([A-Za-z_][A-Za-z0-9_.]+)>$").matcher(elemType);
					if (matcher.find()) {
						String innerElem = matcher.group(1);
						
						elemClass = getJClass(elemType.substring(0, elemType.length() - matcher.group(0).length()));
						elemClass = elemClass.narrow(getJClass(innerElem));
					}
					
					int mods = JMod.PUBLIC;
					if (isStatic) {
						mods |= JMod.STATIC;
					}
					
					getter = holder.getGeneratedClass().method(
							mods,
							elemClass,
							FormatsUtils.fieldToGetter(element.getSimpleName().toString())
						);
					
					if (isStatic) {
						getter.param(getClasses().CONTEXT, "context");
					}
				}
				
				//Remove previous method body
				codeModelHelper.removeBody(getter);
				
				JFieldRef field = ref(element.getSimpleName().toString());
				
				//In the init(), set the field to null
				if (element.getEnclosingElement().getAnnotation(UseModel.class) != null) { 
					block._if(useModelHolder.getFullInitVar())._then().assign(field, _null());
				} else {
					if (element.getAnnotation(Extra.class)==null && element.getAnnotation(FragmentArg.class)==null) {
						block.assign(field, _null());
					}	
				}
								
				block = getter.body()._if(field.eq(_null()))._then();
				
				getter.body()._return(field);
			} else {
				JFieldRef field = ref(element.getSimpleName().toString());
				block = block._if(field.eq(_null()))._then();
			}
		}
		
		//If the element is static, then the declaration block is inside the getModel(s) of enclosing element
		if (element.getModifiers().contains(Modifier.STATIC)) {
			if (!element.getAnnotation(Model.class).lazy()) {
				block = useModelHolder.getGetModelInitBlock();
				generateGetModelCallInBlock(block, element, modelHolder, useModelHolder, true);
				
				block = useModelHolder.getGetModelListInitBlock();
				generateGetModelCallInBlock(block, element, modelHolder, useModelHolder, true);				
			} else {
				generateGetModelCallInBlock(block, element, modelHolder, useModelHolder, true);	
			}
			
		} else {
			 generateGetModelCallInBlock(block, element, modelHolder);
		}
		
		PutOnEvent putOnEvent = element.getAnnotation(PutOnEvent.class);
		if (putOnEvent != null) {
			String classField = TypeUtils.getClassFieldValue(element, PutOnEvent.class.getCanonicalName(), "value", getEnvironment());
			String eventClass = SharedRecords.getEvent(classField, getEnvironment());
			
			if (classField == null || eventClass == null) {
				LOGGER.error("@PutOnEvent failed in " + classField, element, putOnEvent);
				return;
			}
			
			JMethod putOnUpdateMethod = EventUtils.getEventMethod(eventClass, element.getEnclosingElement(), viewsHolder, getEnvironment());
			block = putOnUpdateMethod.body();
			
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
			String classField = TypeUtils.getClassFieldValue(element, UpdateOnEvent.class.getCanonicalName(), "value", getEnvironment());	
			String eventClass = SharedRecords.getEvent(classField, getEnvironment());
			
			if (classField == null || eventClass == null) {
				LOGGER.error("@UpdateOnEvent failed in " + classField, element, updateOnEvent);
				return;
			}
			
			JMethod eventOnUpdateMethod = EventUtils.getEventMethod(eventClass, element.getEnclosingElement(), viewsHolder, getEnvironment());
			block = eventOnUpdateMethod.body();
			
			generateGetModelCallInBlock(block, element, modelHolder);				
		}
	}
	
	private void generateGetModelCallInBlock(JBlock block, Element element, ModelHolder holder) {
		this.generateGetModelCallInBlock(block, element, holder, null, false);
	}
	
	private void generateGetModelCallInBlock(final JBlock block, final Element element, 
			final ModelHolder holder, final UseModelHolder useModelHolder, final boolean isStatic) {
		
		//This ensures the priority of the @Model
		final Trees trees = Trees.instance(getProcessingEnvironment());
    	final TreePath treePath = trees.getPath(element);
    	
    	TreePathScanner<Object, Trees> scanner = new TreePathScanner<Object, Trees>() {
    		
    		@Override
    		public Object visitAnnotation(AnnotationTree annotationTree, Trees trees) {
    			String annotationName = annotationTree.getAnnotationType().toString();
    			if (annotationName.equals(Model.class.getSimpleName()) 
    				|| annotationName.equals(Model.class.getCanonicalName())) {
    				
    				int position = (int) trees.getSourcePositions()
    										  .getStartPosition(treePath.getCompilationUnit(), annotationTree);
    				
    				Model annotation = element.getAnnotation(Model.class);

    				//Get the internal calling method
    				JMethod getModelMethod = holder.getLoadModelMethod(element);
    				
    				final IJExpression queryExpr = FormatsUtils.expressionFromString(annotation.query());
    				final IJExpression orderByExpr = FormatsUtils.expressionFromString(annotation.orderBy());
					final IJExpression fieldsExpr = FormatsUtils.expressionFromString(StringUtils.join(annotation.fields(), ", "));
    				
    				IJExpression contextExpr = holder.getContextRef();
    				if (contextExpr == _this()) {
    					contextExpr = holder.getGeneratedClass().staticRef("this");
    				}
    				if (isStatic) contextExpr = ref("context");
    				
    				IJExpression onFailed = _null();
    				if (isStatic && useModelHolder != null && !annotation.lazy()) 
    					onFailed = useModelHolder.getGetModelInitBlockOnFailed();
    				
    				JInvocation invocation = invoke(getModelMethod).arg(contextExpr)
    						                                       .arg(queryExpr)
    						                                       .arg(orderByExpr)
    						                                       .arg(fieldsExpr)
    						                                       .arg(_null())
    															   .arg(onFailed);
    				SharedRecords.priorityAdd(block, invocation, position);
    			}
    			
    			return super.visitAnnotation(annotationTree, trees);
    		}
    		
    	};
    	scanner.scan(treePath, trees);  
	}

	private void generatePutModelCallInBlock(JBlock block, Element element, ModelHolder holder, boolean hasEvent) {
		
		final Model annotation = element.getAnnotation(Model.class);
		final JMethod putModelMethod = holder.getPutModelMethod(element);
		final IJExpression queryExpr = FormatsUtils.expressionFromString(annotation.query());
		final IJExpression orderByExpr = FormatsUtils.expressionFromString(annotation.orderBy());
		final IJExpression fieldsExpr = FormatsUtils.expressionFromString(StringUtils.join(annotation.fields(), ", "));

		if (hasEvent) {
			IJExpression instanceOfExpression = ref("event").invoke("getValues").component(lit(0));
			
			JConditional condition = block._if(
						ref("event").invoke("getValues").ref("length").gt(lit(0))
						.cand(instanceOfExpression._instanceof(getJClass(Runnable.class)))
					);
			
			JBlock ifBlock = condition._then();
			ifBlock.invoke(putModelMethod)
				   .arg(queryExpr)
				   .arg(orderByExpr)
				   .arg(fieldsExpr)
			       .arg(JExpr.cast(getJClass(Runnable.class), instanceOfExpression))
			       .arg(_null());
			ifBlock.invoke(ref("event"), "setValues").arg(JExpr.newArray(getJClass(Object.class)));
			
			condition._else().invoke(putModelMethod).arg(queryExpr)
			                                        .arg(orderByExpr)
			                                        .arg(fieldsExpr)
			                                        .arg(_null())
			                                        .arg(_null());
		} else {
			block.invoke(putModelMethod).arg(queryExpr)
										.arg(orderByExpr)
							            .arg(fieldsExpr)
							            .arg(_null())
							            .arg(_null());
		}
		
	}

}
