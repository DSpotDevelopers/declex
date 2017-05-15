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
package com.dspot.declex.viewsinjection;

import static com.dspot.declex.api.util.FormatsUtils.fieldToGetter;
import static com.dspot.declex.api.util.FormatsUtils.fieldToSetter;
import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.lit;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.internal.process.ProcessHolder;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;
import org.androidannotations.rclass.IRClass.Res;

import com.dspot.declex.api.action.error.ValidationException;
import com.dspot.declex.api.action.runnable.OnFailedRunnable;
import com.dspot.declex.api.external.ExternalRecollect;
import com.dspot.declex.api.model.Model;
import com.dspot.declex.api.model.UseModel;
import com.dspot.declex.api.viewsinjection.Recollect;
import com.dspot.declex.helper.ViewsHelper;
import com.dspot.declex.model.ModelHolder;
import com.dspot.declex.share.holder.ViewsHolder;
import com.dspot.declex.share.holder.ViewsHolder.IdInfoHolder;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.LayoutsParser.LayoutObject;
import com.dspot.declex.util.SharedRecords;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.util.TypeUtils.ClassInformation;
import com.dspot.declex.util.element.VirtualElement;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.AbstractJType;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JAnonymousClass;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JCatchBlock;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JTryBlock;
import com.helger.jcodemodel.JVar;
import com.mobsandgeeks.saripaar.annotation.Order;
import com.mobsandgeeks.saripaar.annotation.ValidateUsing;

public class RecollectHandler extends BaseAnnotationHandler<EComponentWithViewSupportHolder> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RecollectHandler.class);
	
	public RecollectHandler(AndroidAnnotationsEnvironment environment) {
		super(Recollect.class, environment);
	}
	
	@Override
	public void validate(Element element, ElementValidation valid) {
		
		final String elementName = element.getSimpleName().toString();
		
		validatorHelper.enclosingElementHasEnhancedComponentAnnotation(element, valid);
		validatorHelper.isNotPrivate(element, valid);
		
		ViewsHelper viewsHelper = new ViewsHelper(element.getEnclosingElement(), annotationHelper, getEnvironment());
		
		//Validate special methods
		boolean specialAssignField = false;
		List<? extends Element> elems = element.getEnclosingElement().getEnclosedElements();
		for (Element elem : elems) {
			if (elem.getKind() == ElementKind.METHOD) {
				ExecutableElement executableElement = (ExecutableElement) elem;

				if (executableElement.getSimpleName().toString().equals("readField")) {
					List<? extends VariableElement> parameters = executableElement.getParameters();

					TypeMirror returnType = executableElement.getReturnType();
					if (!returnType.toString().equals(String.class.getCanonicalName())) {
						valid.addError(executableElement, "%s should return a String");
					}
					
					if (parameters.size() != 1) {
						valid.addError(executableElement, "%s can only be used on a method with  1 parameter, instead of " + parameters.size());
					} else {
						VariableElement firstParameter = parameters.get(0);

						if (!TypeUtils.isSubtype(firstParameter, CanonicalNameConstants.VIEW, getProcessingEnvironment())) {
							valid.addError(executableElement, "The first parameter should be an instance of View");									
						}
					}
					
					specialAssignField = true;
					
					break;
				}
				
			}
		}
		
		//Check if the field is a primitive or a String
		if (element.asType().getKind().isPrimitive() || 
			element.asType().toString().equals(String.class.getCanonicalName())) {
			
			if (!viewsHelper.getLayoutObjects().containsKey(elementName)) {
				valid.addError("The element with Id \"" + elementName + "\" cannot be found in the Layout ");
			} else if (!specialAssignField) {
				LayoutObject layoutObject = viewsHelper.getLayoutObjects().get(elementName);
				String className = layoutObject.className;
				
				if (!specialAssignField) {
					if (!TypeUtils.isSubtype(className, "android.widget.TextView", getProcessingEnvironment()) &&
						!TypeUtils.isSubtype(className, "android.widget.ImageView", getProcessingEnvironment())) {
						valid.addError("You should provide an assignField method for the class \"" + className + 
								"\" used on the field " + elementName);
					}
				}
			}
		}
				
		boolean isList = TypeUtils.isSubtype(element, "java.util.Collection", getProcessingEnvironment());		
		if (isList) {
			valid.addError("@Recollect cannot be used over a Collection of models");
		} 		
		
	}

	@Override
	public void process(Element element, final EComponentWithViewSupportHolder holder) {
		
		final ViewsHolder viewsHolder = holder.getPluginHolder(new ViewsHolder(holder, annotationHelper));
		final String fieldName = element.getSimpleName().toString();
		
		final boolean hasExternalRecollect = adiHelper.getAnnotation(element, ExternalRecollect.class) != null;
		
		final ClassInformation classInformation = TypeUtils.getClassInformation(element, getEnvironment());
		final String className = classInformation.generatorClassName;
				
		JMethod recollectModelMethod = holder.getGeneratedClass().method(JMod.PRIVATE, getCodeModel().VOID, "_recollect_" + fieldName);
		JVar afterRecollect = recollectModelMethod.param(JMod.FINAL, getJClass(Runnable.class), "afterRecollect");
		JVar onFailed = recollectModelMethod.param(JMod.FINAL, getJClass(OnFailedRunnable.class), "onFailed");
		
		//Create the "populate this" method
		JMethod recollectThisMethod = viewsHolder.getGeneratedClass().getMethod(
				"_recollect_this",
				new AbstractJType[] {getJClass(Runnable.class), getJClass(OnFailedRunnable.class)}
			);
		if (recollectThisMethod == null) {
			recollectThisMethod = viewsHolder.getGeneratedClass().method(JMod.NONE, getCodeModel().VOID, "_recollect_this");
			JVar afterRecollectParam = recollectThisMethod.param(JMod.FINAL, getJClass(Runnable.class), "afterRecollect");
			recollectThisMethod.param(JMod.FINAL, getJClass(OnFailedRunnable.class), "onFailed");
			
			JBlock block = new JBlock();
			block._if(afterRecollectParam.neNull())._then()
			     .invoke(afterRecollectParam, "run");
			SharedRecords.priorityAdd(recollectThisMethod.body(), block, 10);
		}
		recollectThisMethod.body().invoke(recollectModelMethod).arg(_null()).arg(ref("onFailed"));
		
		final Model modelAnnotation = element.getAnnotation(Model.class);
		if (modelAnnotation != null) {
			
			EComponentHolder beanHolder = holder;
			if (hasExternalRecollect) {
				final Element referenceElement = ((VirtualElement) element).getReference();
				ClassInformation info = TypeUtils.getClassInformation(referenceElement, getEnvironment(), true);
				ProcessHolder processHolder = getEnvironment().getProcessHolder();
				beanHolder = (EComponentHolder) processHolder.getGeneratedClassHolder(info.generatorElement);
			}

			final ModelHolder modelHolder = beanHolder.getPluginHolder(new ModelHolder(beanHolder));
			JBlock putModelMethodBlock = modelHolder.getPutModelMethodBlock(
				hasExternalRecollect? ((VirtualElement)element).getElement() : element
			);
			putModelMethodBlock.removeAll();						
			
			JFieldRef args = ref("args");

			JBlock ifRecollect = putModelMethodBlock._if(args.invoke("containsKey").arg("recollect").not()
					                .cor(cast(getJClass(Boolean.class), args.invoke("get").arg("recollect")))
					            )._then();
			
			if (hasExternalRecollect) {
				JFieldRef listenerField = ref(
					"recollect" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1)
				);
				
				JBlock block = new JBlock();
				JConditional ifNeNull = block._if(listenerField.neNull());
				ifNeNull._then().invoke(listenerField, "recollectModel")
				           .arg(ref("putModelRunnable"))
				           .arg(ref("onFailed"));
				
				ifRecollect.add(block);
			} else {
				ifRecollect.invoke(recollectModelMethod).arg(ref("putModelRunnable")).arg(ref("onFailed"));
			}

		} 
		
		JBlock recollectBlock;
		JAnonymousClass ValidatorListenerClass = null;
		Recollect recollector = element.getAnnotation(Recollect.class);
		if (recollector.validate()) {
			AbstractJClass Validator = getEnvironment().getJClass("com.mobsandgeeks.saripaar.Validator");
			AbstractJClass ValidatorListener = getEnvironment().getJClass("com.mobsandgeeks.saripaar.Validator.ValidationListener");
			AbstractJClass ValidatorError = getEnvironment().getJClass("com.mobsandgeeks.saripaar.ValidationError");
			
			ValidatorListenerClass = getCodeModel().anonymousClass(ValidatorListener);
			
			JMethod onValidationFailed = ValidatorListenerClass.method(JMod.PUBLIC, getCodeModel().VOID, "onValidationFailed");
			JVar errors = onValidationFailed.param(getClasses().LIST.narrow(ValidatorError), "errors");
			onValidationFailed.annotate(Override.class);
						
			IJExpression context = holder.getContextRef();
			if (context == _this()) {
				context = holder.getGeneratedClass().staticRef("this");
			}

			JVar messages = onValidationFailed.body().decl(getClasses().STRING, "messages", lit(""));
			JBlock forEach = onValidationFailed.body().forEach(ValidatorError, "error", errors).body();
			JFieldRef error = ref("error");
			JVar message = forEach.decl(getClasses().STRING, "message", error.invoke("getCollatedErrorMessage").arg(context));
			forEach.assign(messages, message.plus(message).plus(" "));
			
			if (recollector.validateAutoMessage()) {
				JVar view = forEach.decl(getClasses().VIEW, "view", error.invoke("getView"));
				
				AbstractJClass EditText = getJClass("android.widget.EditText");
				AbstractJClass Toast = getJClass("android.widget.Toast");
				JConditional conditional = forEach._if(view._instanceof(EditText));
				conditional._then().invoke(cast(EditText, view), "setError").arg(message);
				conditional._else().add(
						Toast.staticInvoke("makeText").arg(context)
						     .arg(message)
						     .arg(Toast.staticRef("LENGTH_SHORT"))
						     .invoke("show")
				     );
			}
			
			//Call onFailed if assigned
			IJExpression validationException = _new(getJClass(ValidationException.class))
					 								.arg(messages);
			onValidationFailed.body()._if(onFailed.ne(_null()))._then()
			   						 .invoke(onFailed, "onFailed").arg(validationException);
			                         
			
			JMethod onValidationSucceeded = ValidatorListenerClass.method(JMod.PUBLIC, getCodeModel().VOID, "onValidationSucceeded");
			onValidationSucceeded.annotate(Override.class);

			JBlock block = recollectModelMethod.body();			
			JVar validatorHolder = block.decl(ValidatorListenerClass, fieldName + "$validatorHolder", _new(ValidatorListenerClass));
			JVar validator = block.decl(Validator, fieldName + "$validator", _new(Validator).arg(validatorHolder));
			block.invoke(validator, "setValidationListener").arg(validatorHolder);
			block.invoke(validator, "validate");
			
			recollectBlock = onValidationSucceeded.body();
		} else {
			recollectBlock = recollectModelMethod.body();
		}
		
		JTryBlock tryBlock = recollectBlock._try();
		{//Catch block
			JCatchBlock catchBlock = tryBlock._catch(getClasses().THROWABLE);
			JVar caughtException = catchBlock.param("e");
						
			IJStatement uncaughtExceptionCall = getClasses().THREAD 
					.staticInvoke("getDefaultUncaughtExceptionHandler") 
					.invoke("uncaughtException") 
					.arg(getClasses().THREAD.staticInvoke("currentThread")) 
					.arg(caughtException);
			
			JConditional ifOnFailedAssigned = catchBlock.body()._if(onFailed.ne(_null()));
			ifOnFailedAssigned._then().invoke(onFailed, "onFailed").arg(caughtException);
			ifOnFailedAssigned._else().add(uncaughtExceptionCall);
		}
		
		recollectBlock = tryBlock.body();
		
		//Check if the field is a primitive or a String
		if (element.asType().getKind().isPrimitive() || 
			element.asType().toString().equals(String.class.getCanonicalName())) {
			
			if (annotationHelper.containsField(fieldName, Res.ID)) {
				if (element.getAnnotation(Recollect.class).debug())
					LOGGER.warn("\nField: " + fieldName, element, element.getAnnotation(Recollect.class));
								
				JFieldRef view = viewsHolder.createAndAssignView(fieldName);
				assignValueToField(ref(fieldName), element.asType(), view, recollectBlock);
				
				checkValidatorClass(element, element, fieldName, view, ValidatorListenerClass, viewsHolder);
			}
			
			recollectBlock.invoke(afterRecollect, "run");
			
			return;
		} 
		
		//Find all the fields and methods that are presented in the layouts
		Map<String, IdInfoHolder> fields = new HashMap<String, IdInfoHolder>();
		Map<String, IdInfoHolder> methods = new HashMap<String, IdInfoHolder>();
		viewsHolder.findFieldsAndMethods(className, fieldName, element, fields, methods, false);
		
		if (element.getAnnotation(Recollect.class).debug())
			LOGGER.warn("\nFields: " + fields + "\nMethods: " + methods, element, element.getAnnotation(Recollect.class));
		
		
		for (String field : fields.keySet()) {
			String composedField = "";
			String[] fieldSplit = field.split("\\.");
			for (int i = 0; i < fieldSplit.length-1; i++) {
				composedField = composedField + "." + fieldToGetter(fieldSplit[i]);
			}
			composedField = composedField + "." + fieldToSetter(fieldSplit[fieldSplit.length-1]);
			
			injectAndAssignField(
					recollectBlock, 
					fieldName, 
					fields.get(field), 
					composedField, 
					element, 
					viewsHolder,
					ValidatorListenerClass
				);
		}
		
		for (String method : methods.keySet()) {
			String composedField = "";
			String[] methodSplit = method.split("\\.");
			for (int i = 0; i < methodSplit.length-1; i++) {
				composedField = composedField + "." + fieldToGetter(methodSplit[i]);
			}
			composedField = composedField + "." + methodSplit[methodSplit.length-1];
			
			injectAndAssignField(
					recollectBlock, 
					fieldName, 
					methods.get(method), 
					composedField, 
					element, 
					viewsHolder, 
					ValidatorListenerClass
				);
		}
		
		recollectBlock.invoke(afterRecollect, "run");
	}
	
	private void assignValueToField(JFieldRef field, TypeMirror typeMirror, JFieldRef view, JBlock body) {
		body = body._if(view.ne(_null()))._then();
		
		IJExpression readField = invoke("readField_").arg(view);
		
		if (typeMirror.getKind().isPrimitive()) {
			
			switch (typeMirror.getKind()) {
			case BOOLEAN:
				body.assign(field, getJClass("Boolean").staticInvoke("valueOf").arg(readField));
				break;
				
			case INT:
				body.assign(field, getJClass("Integer").staticInvoke("valueOf").arg(readField));
				break;
				
			case SHORT:
				body.assign(field, getJClass("Short").staticInvoke("valueOf").arg(readField));
				break;
				
			case DOUBLE:
				body.assign(field, getJClass("Double").staticInvoke("valueOf").arg(readField));
				break;
				
			case FLOAT:
				body.assign(field, getJClass("Float").staticInvoke("valueOf").arg(readField));
				break;
				
			case BYTE:
				body.assign(field, getJClass("Byte").staticInvoke("valueOf").arg(readField));
				break;
				
			case LONG:
				body.assign(field, getJClass("Long").staticInvoke("valueOf").arg(readField));
				break;
				
			default:
				break;
			}
			
		} else if (typeMirror.toString().equals(String.class.getCanonicalName())) {
			body.assign(field, readField);
		}
			
	
	}
	
	private void injectAndAssignField(JBlock body, String fieldName, IdInfoHolder info, String methodName, 
			Element element, ViewsHolder holder, JAnonymousClass ValidatorListenerClass) {
		
		boolean castNeeded = false;
		String className = element.asType().toString();
		if (!className.endsWith(ModelConstants.generationSuffix())) {
			if (TypeUtils.isClassAnnotatedWith(className, UseModel.class, getEnvironment())) {
				className = TypeUtils.getGeneratedClassName(className, getEnvironment());
				castNeeded = true;
			}
		}
		
		IJExpression fieldRef = ref(fieldName);
		if (element.getAnnotation(Model.class) != null) {
			ModelHolder modelHolder = holder.holder().getPluginHolder(new ModelHolder(holder.holder()));
			fieldRef = invoke(modelHolder.getGetterMethod(element));
		}
		
		final IJExpression assignRef = castNeeded ? cast(getJClass(className), fieldRef) : fieldRef;
		
		IJExpression methodsCall = assignRef;
		
		JBlock checkForNull = new JBlock();
		JBlock changedBlock = checkForNull;
		
		String[] methodSplit = methodName.split("\\.");
		for (int i = 0; i < methodSplit.length; i++) {
			String methodPart = methodSplit[i];
			if (!methodPart.equals("")) {
				methodsCall = methodsCall.invoke(methodPart);		
				
				boolean theresMoreAfter = false;
				for (int j = i+1; j < methodSplit.length; j++) {
					if (!methodSplit[j].equals("")) {
						theresMoreAfter = true;
						break;
					}
				}
				
				if (theresMoreAfter) changedBlock = changedBlock._if(methodsCall.ne(_null()))._then();
			}			
		}
		
		JFieldRef view = holder.createAndAssignView(info.idName);
		
		body = body._if(view.ne(_null()))._then();
		
		IJExpression readField;
		if (info.getterOrSetter != null) {
			readField = view.invoke("get" + info.getterOrSetter);
		} else {
			if (TypeUtils.isSubtype(info.viewClass, "android.widget.TextView", getProcessingEnvironment())) {
				readField = cast(getClasses().TEXT_VIEW, view);
				readField = readField.invoke("getText").invoke("toString");
			} else {
				readField = invoke("readField").arg(view);
			}

		}

		JInvocation setInvocation = null;
		if (info.getterOrSetter != null) {
			setInvocation = ((JInvocation)methodsCall).arg(readField);
		} else {
			if (info.type.getKind().isPrimitive()) {
				
				switch (info.type.getKind()) {
				case BOOLEAN:
					setInvocation =((JInvocation)methodsCall).arg(getJClass("Boolean").staticInvoke("valueOf").arg(readField));
					break;
					
				case INT:
					setInvocation = ((JInvocation)methodsCall).arg(getJClass("Integer").staticInvoke("valueOf").arg(readField));
					break;
					
				case SHORT:
					setInvocation = ((JInvocation)methodsCall).arg(getJClass("Short").staticInvoke("valueOf").arg(readField));
					break;
					
				case DOUBLE:
					setInvocation = ((JInvocation)methodsCall).arg(getJClass("Double").staticInvoke("valueOf").arg(readField));
					break;
					
				case FLOAT:
					setInvocation = ((JInvocation)methodsCall).arg(getJClass("Float").staticInvoke("valueOf").arg(readField));
					break;
					
				case BYTE:
					setInvocation = ((JInvocation)methodsCall).arg(getJClass("Byte").staticInvoke("valueOf").arg(readField));
					break;
					
				case LONG:
					setInvocation = ((JInvocation)methodsCall).arg(getJClass("Long").staticInvoke("valueOf").arg(readField));
					break;
					
				default:
					break;
				}
				
			} else if (info.type.toString().equals(String.class.getCanonicalName())) {
				setInvocation = ((JInvocation)methodsCall).arg(readField);
			}			
		}
		
		for (String param : info.extraParams) {
			setInvocation = ((JInvocation)setInvocation).arg(ref(param));
		}
		
		changedBlock.add((JInvocation)methodsCall);
		body.add(checkForNull);

		checkValidatorClass(element, info.element, info.idName, view, ValidatorListenerClass, holder);
	}
	
	private void checkValidatorClass(Element element, Element infoElement, String fieldName, 
			JFieldRef view, JAnonymousClass ValidatorListenerClass, ViewsHolder viewsHolder) {
		
		if (ValidatorListenerClass != null) {
			
			JFieldVar field = null;
			
			final List<? extends AnnotationMirror> annotations = infoElement.getAnnotationMirrors();
			for (AnnotationMirror annotation : annotations) {
				ValidateUsing validateUsingAnnotation =  annotation.getAnnotationType()
						                                           .asElement()
						                                           .getAnnotation(ValidateUsing.class);
				if (validateUsingAnnotation != null
					|| annotation.getAnnotationType().toString().equals(Order.class.getCanonicalName())) {

					if (field == null) {
						AbstractJClass ViewClass = getJClass(viewsHolder.getClassNameFromId(fieldName));
						field = ValidatorListenerClass.field(
								JMod.PRIVATE, 
								ViewClass, 
								"$" + fieldName + DeclexConstant.VIEW,
								view
							);						
					}
				
					TypeUtils.annotateVar(field, annotation, getEnvironment());
				}
			}
		}
		
	}
	
}
