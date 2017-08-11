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
package com.dspot.declex.override.holder;

import static com.helger.jcodemodel.JExpr._new;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.Extra;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.holder.EActivityHolder;
import org.androidannotations.plugin.PluginClassHolder;

import com.dspot.declex.action.Actions;
import com.dspot.declex.annotation.action.ActionFor;
import com.dspot.declex.annotation.action.StopOn;
import com.dspot.declex.api.action.base.BaseActivityActionHolder;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.dspot.declex.api.action.processor.ActivityActionProcessor;
import com.dspot.declex.helper.FilesCacheHelper;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.JavaDocUtils;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.util.TypeUtils.ClassInformation;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JAnnotationUse;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class ActivityActionHolder extends PluginClassHolder<EActivityHolder> {

	private static final int MIN_SDK_WITH_FRAGMENT_SUPPORT = 11;
	
	private final static String INTENT_NAME = "intent";
	private final static String SET_BUILDER = "setBuilder";
	
	public JDefinedClass ActivityAction;
	private JFieldVar builderField;
	
	public ActivityActionHolder(EActivityHolder holder) {
		super(holder);
	}
	
	public JDefinedClass getActivityAction() {
		if (ActivityAction == null) {
			setActivityAction();
		}
		return ActivityAction;
	}
	
	public static void createInformationForActionHolder(Element element, AndroidAnnotationsEnvironment env) {
		
		final String clsName = element.asType().toString();		
		final int index = clsName.lastIndexOf('.');
		final String pkg = clsName.substring(0, index);
		final String activityName = clsName.substring(index + 1);
		final String actionName = pkg + "." + activityName + "ActionHolder";
		
		FilesCacheHelper.getInstance().addGeneratedClass(DeclexConstant.ACTION, element, true);
		FilesCacheHelper.getInstance().addGeneratedClass(actionName, element);
		FilesCacheHelper.getInstance().addGeneratedClass(
				TypeUtils.getGeneratedClassName(actionName, env, false), 
				null
			);
		
		ActionInfo actionInfo = new ActionInfo(actionName);
		actionInfo.processors.add(new ActivityActionProcessor());
		actionInfo.isGlobal = true;
		actionInfo.isTimeConsuming = false;
		actionInfo.superHolderClass = BaseActivityActionHolder.class.getCanonicalName();
		
		//This will avoid generation for parent classes, not used in the project
		actionInfo.generated = false;
				
		actionInfo.setReferences(JavaDocUtils.referenceFromClassName(clsName));
		Actions.getInstance().addAction(activityName, actionName, actionInfo);
		
		TypeElement superElement = env.getProcessingEnvironment()
				                      .getElementUtils()
				                      .getTypeElement(actionInfo.superHolderClass);
		Actions.getInstance().createInformationForMethods(superElement, actionInfo);
				
		addActivityExtraInformation(actionInfo, element, env);
	}
	
	private static void addActivityExtraInformation(ActionInfo actionInfo, Element element, AndroidAnnotationsEnvironment env) {
		List<Element> extraFields = new LinkedList<>();
		findExtraFields(element, extraFields, env.getProcessingEnvironment());
		
		for (Element fragmentArgField : extraFields) {
			addFieldInformationToActionHolder(actionInfo, fragmentArgField, env);
		}
	}
	
	private static void addFieldInformationToActionHolder(
			ActionInfo actionInfo, Element element,
			AndroidAnnotationsEnvironment env) {
		
		final String clsName = element.getEnclosingElement().asType().toString();		
		final int index = clsName.lastIndexOf('.');
		final String pkg = clsName.substring(0, index);
		final String activityName = clsName.substring(index + 1);
		final String actionName = pkg + "." + activityName + "ActionHolder";
		
		ClassInformation classInformation = TypeUtils.getClassInformation(element, env);
		final String className = classInformation.originalClassName;
		final String fieldName = element.getSimpleName().toString();
		
		AbstractJClass clazz = env.getJClass(className);
		if (classInformation.isList) {
			clazz = env.getClasses().LIST.narrow(clazz);
		}
		
		actionInfo.addMethod(
				fieldName, 
				actionName, 
				Arrays.asList(new ActionMethodParam(fieldName, clazz))
			);
	}
	
	private static void findExtraFields(Element element, List<Element> fields, ProcessingEnvironment env) {
		
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems) {
			if (elem.getKind() == ElementKind.FIELD) {
				if (elem.getAnnotation(Extra.class) != null) {
					fields.add(elem);
				}
			}
		}
		
		List<? extends TypeMirror> superTypes = env.getTypeUtils().directSupertypes(element.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = env.getElementUtils().getTypeElement(type.toString());
			if (superElement == null) continue;
			
			findExtraFields(superElement, fields, env);
		}

	}
	
	private void setActivityAction() {
		try {
			final String clsName = holder().getAnnotatedElement().asType().toString();		
			final int index = clsName.lastIndexOf('.');
			final String pkg = clsName.substring(0, index);
			final String activityName = clsName.substring(index + 1);
			final String actionName = pkg + "." + activityName + "ActionHolder";
			
			ActivityAction = getCodeModel()._class(actionName);
			ActivityAction._extends(BaseActivityActionHolder.class);
			ActivityAction.annotate(EBean.class);
			
			JAnnotationUse actionFor = ActivityAction.annotate(ActionFor.class);
			actionFor.param("global", true);
			actionFor.param("value", activityName);
			actionFor.param("timeConsuming", false);	
			actionFor.param("processors", ActivityActionProcessor.class);
			
			ActionInfo actionInfo = Actions.getInstance().getActionInfos().get(ActivityAction.fullName());
			actionInfo.generated = true;
			ActivityAction.javadoc().add(actionInfo.references);
			
			setFields();			
			setIntent();
			setBuilder();
			
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	private void setFields() {
		builderField = ActivityAction.field(JMod.PRIVATE, holder().getIntentBuilderClass(), "builder");
	}
		
	private void setBuilder() {
		AbstractJClass Context = getClasses().CONTEXT;
		AbstractJClass Fragment = getClasses().FRAGMENT;
		AbstractJClass SupportFragment = getClasses().SUPPORT_V4_FRAGMENT;
		
		JMethod setBuilderMethod = ActivityAction.method(JMod.NONE, holder().getIntentBuilderClass(), SET_BUILDER);
		JVar param = setBuilderMethod.param(Context, "context");
		setBuilderMethod.body().assign(builderField, _new(holder().getIntentBuilderClass()).arg(param));
		setBuilderMethod.body()._return(builderField);
		
		if (hasFragmentInClasspath()) {
			setBuilderMethod = ActivityAction.method(JMod.NONE, holder().getIntentBuilderClass(), SET_BUILDER);
			param = setBuilderMethod.param(Fragment, "fragment");
			setBuilderMethod.body().assign(builderField, _new(holder().getIntentBuilderClass()).arg(param));
			setBuilderMethod.body()._return(builderField);
		}
		
		if (hasFragmentSupportInClasspath()) {
			setBuilderMethod = ActivityAction.method(JMod.NONE, holder().getIntentBuilderClass(), SET_BUILDER);
			param = setBuilderMethod.param(SupportFragment, "supportFragment");
			setBuilderMethod.body().assign(builderField, _new(holder().getIntentBuilderClass()).arg(param));
			setBuilderMethod.body()._return(builderField);
		}
	}
	
	protected boolean hasFragmentInClasspath() {
		boolean fragmentExistsInSdk = environment().getAndroidManifest().getMinSdkVersion() >= MIN_SDK_WITH_FRAGMENT_SUPPORT;
		return fragmentExistsInSdk && processingEnv().getElementUtils().getTypeElement(CanonicalNameConstants.FRAGMENT) != null;
	}
	
	private boolean hasFragmentSupportInClasspath() {
		return processingEnv().getElementUtils().getTypeElement(CanonicalNameConstants.SUPPORT_V4_FRAGMENT) != null;
	}
		
	private void setIntent() {
		JMethod intentMethod = ActivityAction.method(JMod.PUBLIC, holder().getIntentBuilderClass(), INTENT_NAME);
		intentMethod.annotate(Override.class);
		JAnnotationUse stopOn = intentMethod.annotate(StopOn.class);
		stopOn.param("value", "get");
		intentMethod.body()._return(builderField);
	}
		
}
