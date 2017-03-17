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
import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.lit;
import static com.helger.jcodemodel.JExpr.ref;
import static org.androidannotations.helper.ModelConstants.generationSuffix;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
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
import org.androidannotations.annotations.RootContext;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.holder.EActivityHolder;
import org.androidannotations.plugin.PluginClassHolder;

import com.dspot.declex.action.Actions;
import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.api.action.annotation.StopOn;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.dspot.declex.api.action.processor.ActivityActionProcessor;
import com.dspot.declex.api.action.runnable.OnActivityResultRunnable;
import com.dspot.declex.util.JavaDocUtils;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.util.TypeUtils.ClassInformation;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JAnnotationUse;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class ActivityActionHolder extends PluginClassHolder<EActivityHolder> {

	private static final int MIN_SDK_WITH_FRAGMENT_SUPPORT = 11;
	
	private final static String INIT_NAME = "init";
	private final static String BUILD_NAME = "build";
	private final static String EXECUTE_NAME = "execute";
	private final static String INTENT_NAME = "intent";
	private final static String WITH_RESULT_NAME = "withResult";
	private final static String SET_BUILDER = "setBuilder";
	private final static String GET_ON_RESULT = "getOnResult";
	private final static String GET_REQUEST_CODE = "getRequestCode";
	
	public JDefinedClass ActivityAction;
	private JFieldVar contextField;
	private JFieldVar startedField;
	private JFieldVar onResultField;
	private JFieldVar builderField;
	private JFieldVar requestCodeField;
	
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
						
		ActionInfo actionInfo = new ActionInfo(actionName);
		actionInfo.processors.add(new ActivityActionProcessor());
		actionInfo.isGlobal = true;
		
		//This will avoid generation for parent classes, not used in the project
		actionInfo.generated = false; 
				
		actionInfo.setReferences(JavaDocUtils.referenceFromClassName(clsName));
		Actions.getInstance().addAction(activityName, actionName, actionInfo);
		
		actionInfo.addMethod(INIT_NAME, env.getCodeModel().VOID.fullName());
		
		actionInfo.addMethod(
				BUILD_NAME, 
				env.getCodeModel().VOID.fullName(),
				Arrays.asList(
						new ActionMethodParam("Started", env.getJClass(Runnable.class)),
						new ActionMethodParam("OnResult", env.getJClass(OnActivityResultRunnable.class))
				)
			);
		
		actionInfo.addMethod(EXECUTE_NAME, env.getCodeModel().VOID.fullName());
		
		actionInfo.addMethod(
				INTENT_NAME, 
				actionName + ".IntentBuilder" + generationSuffix(),
				new ArrayList<ActionMethodParam>(0),
				Arrays.<Annotation>asList(createStopOn(env.getClass().getClassLoader()))
			);
		
		actionInfo.addMethod(WITH_RESULT_NAME, actionName);
		actionInfo.addMethod(WITH_RESULT_NAME, actionName, Arrays.asList(
					new ActionMethodParam("requestCode" , env.getCodeModel().INT)
				));
		
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
		
		actionInfo.addMethod(
				fieldName, 
				actionName, 
				Arrays.asList(new ActionMethodParam(fieldName, env.getJClass(className)))
			);
	}
	
	private static void findExtraFields(Element element, List<Element> fragmentArgFields, ProcessingEnvironment env) {
		
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems) {
			if (elem.getKind() == ElementKind.FIELD) {
				if (elem.getAnnotation(Extra.class) != null) {
					fragmentArgFields.add(elem);
				}
			}
		}
		
		List<? extends TypeMirror> superTypes = env.getTypeUtils().directSupertypes(element.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = env.getElementUtils().getTypeElement(type.toString());
			
			findExtraFields(superElement, fragmentArgFields, env);
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
			ActivityAction.annotate(EBean.class);
			JAnnotationUse actionFor = ActivityAction.annotate(ActionFor.class);
			actionFor.param("value", activityName);		
			actionFor.param("processors", ActivityActionProcessor.class);
			
			ActionInfo actionInfo = Actions.getInstance().getActionInfos().get(ActivityAction.fullName());
			actionInfo.generated = true;
			ActivityAction.javadoc().add(actionInfo.references);
			
			setFields();
			setInit();
			setWithResult();
			setBuild();
			setExecute();			
			setIntent();
			setBuilder();
			setOnResult();
			
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	private void setFields() {
		contextField = ActivityAction.field(JMod.NONE, environment().getClasses().CONTEXT, "context");
		contextField.annotate(RootContext.class);
		
		startedField = ActivityAction.field(JMod.PRIVATE, getJClass(Runnable.class), "Started");
		onResultField = ActivityAction.field(JMod.PRIVATE, getJClass(OnActivityResultRunnable.class), "OnResult");
		builderField = ActivityAction.field(JMod.PRIVATE, holder().getIntentBuilderClass(), "builder");
	}
	
	private void setInit() {
		JMethod initMethod = ActivityAction.method(JMod.NONE, getCodeModel().VOID, INIT_NAME);
	}
	
	private void setBuild() {
		JMethod buildMethod = ActivityAction.method(JMod.NONE, getCodeModel().VOID, BUILD_NAME);
		JVar startedParam = buildMethod.param(getJClass(Runnable.class), "Started");
		JVar onResultParam = buildMethod.param(getJClass(OnActivityResultRunnable.class), "OnResult");
		
		buildMethod.body().assign(_this().ref(startedField), startedParam);
		buildMethod.body().assign(_this().ref(onResultField), onResultParam);
	}
	
	private void setExecute() {
		JMethod executeMethod = ActivityAction.method(JMod.NONE, getCodeModel().VOID, EXECUTE_NAME);
		
		JConditional cond = executeMethod.body()._if(requestCodeField.eq(lit(0)));
		cond._then().invoke(builderField, "start");
		cond._else().invoke(builderField, "startForResult").arg(requestCodeField);
		
		executeMethod.body()._if(startedField.ne(_null()))._then().invoke(startedField, "run");
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
	
	private void setOnResult() {
		JMethod setBuilderMethod = ActivityAction.method(JMod.NONE, getJClass(OnActivityResultRunnable.class), GET_ON_RESULT);
		setBuilderMethod.body()._return(onResultField);
		
		setBuilderMethod = ActivityAction.method(JMod.NONE, getCodeModel().INT, GET_REQUEST_CODE);
		setBuilderMethod.body()._return(requestCodeField);
	}
	
	private void setIntent() {
		JMethod intentMethod = ActivityAction.method(JMod.PUBLIC, holder().getIntentBuilderClass(), INTENT_NAME);
		JAnnotationUse stopOn = intentMethod.annotate(StopOn.class);
		stopOn.param("value", "get");
		intentMethod.body()._return(builderField);
	}
	
	private void setWithResult() {
		JMethod withResultMethod = ActivityAction.method(JMod.PUBLIC, ActivityAction, WITH_RESULT_NAME);
		withResultMethod.param(getCodeModel().INT, "requestCode");
			
		
		requestCodeField = ActivityAction.field(JMod.PRIVATE, getCodeModel().INT, "requestCode");
		withResultMethod.body().assign(_this().ref("requestCode"), ref("requestCode"));
		withResultMethod.body()._return(_this());
		
		//Same method without param
		withResultMethod = ActivityAction.method(JMod.PUBLIC, ActivityAction, WITH_RESULT_NAME);
		withResultMethod.body()._return(_this());
	}
	
	private static StopOn createStopOn(ClassLoader classLoader) {
		return (StopOn) Proxy.newProxyInstance(
				classLoader,
				new Class[] { StopOn.class }, 
				new StopOnProxy()
			);
	}
	
	private static class StopOnProxy implements InvocationHandler {

		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			
			if (method.getName().equals("value")) return new String[]{"get"};
			
			return method.getDefaultValue();
		}
	}

}
