package com.dspot.declex.override.holder;

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
import org.androidannotations.holder.EActivityHolder;
import org.androidannotations.plugin.PluginClassHolder;

import com.dspot.declex.action.Actions;
import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.api.action.annotation.StopOn;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.dspot.declex.util.JavaDocUtils;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.util.TypeUtils.ClassInformation;
import com.helger.jcodemodel.JAnnotationUse;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class ActivityActionHolder extends PluginClassHolder<EActivityHolder> {

	private final static String INIT_NAME = "init";
	private final static String BUILD_NAME = "build";
	private final static String EXECUTE_NAME = "execute";
	private final static String INTENT_NAME = "intent";
	private final static String WITH_RESULT_NAME = "withResult";
	
	public JDefinedClass ActivityAction;
	private JFieldVar contextField;
	private JFieldVar startedField;
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
		
		//This will avoid generation for parent classes, not used in the project
		actionInfo.generated = false; 
				
		actionInfo.setReferences(JavaDocUtils.referenceFromClassName(clsName));
		Actions.getInstance().addAction(activityName, actionName, actionInfo);
		
		actionInfo.addMethod(INIT_NAME, env.getCodeModel().VOID.fullName());
		
		actionInfo.addMethod(
				BUILD_NAME, 
				env.getCodeModel().VOID.fullName(),
				Arrays.asList(new ActionMethodParam("Started", env.getJClass(Runnable.class)))
			);
		
		actionInfo.addMethod(EXECUTE_NAME, env.getCodeModel().VOID.fullName());
		
		actionInfo.addMethod(
				INTENT_NAME, 
				actionName + ".IntentBuilder" + generationSuffix(),
				new ArrayList<ActionMethodParam>(0),
				Arrays.<Annotation>asList(createStopOn(env.getClass().getClassLoader()))
			);
		
		actionInfo.addMethod(WITH_RESULT_NAME, actionName);
		
		addFragmentArgFieldsInformation(actionInfo, element, env);
	}
	
	private static void addFragmentArgFieldsInformation(ActionInfo actionInfo, Element element, AndroidAnnotationsEnvironment env) {
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
			
			ActionInfo actionInfo = Actions.getInstance().getActionInfos().get(ActivityAction.fullName());
			actionInfo.generated = true;
			ActivityAction.javadoc().add(actionInfo.references);
			
			setFields();
			setInit();
			setWithResult();
			setBuild();
			setExecute();			
			setIntent();
			
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	private void setFields() {
		contextField = ActivityAction.field(JMod.NONE, environment().getClasses().CONTEXT, "context");
		contextField.annotate(RootContext.class);
		
		startedField = ActivityAction.field(JMod.PRIVATE, getJClass(Runnable.class), "Started");
		builderField = ActivityAction.field(JMod.PRIVATE, holder().getIntentBuilderClass(), "builder");
	}
	
	private void setInit() {
		JMethod initMethod = ActivityAction.method(JMod.NONE, getCodeModel().VOID, INIT_NAME);
		initMethod.body().assign(builderField, getGeneratedClass().staticInvoke("intent").arg(contextField));
	}
	
	private void setBuild() {
		JMethod buildMethod = ActivityAction.method(JMod.NONE, getCodeModel().VOID, BUILD_NAME);
		JVar startedParam = buildMethod.param(getJClass(Runnable.class), "Started");
		
		buildMethod.body().assign(_this().ref(startedField), startedParam);
	}
	
	private void setExecute() {
		JMethod executeMethod = ActivityAction.method(JMod.NONE, getCodeModel().VOID, EXECUTE_NAME);
		
		JConditional cond = executeMethod.body()._if(requestCodeField.eq(lit(0)));
		cond._then().invoke(builderField, "start");
		cond._else().invoke(builderField, "startForResult").arg(requestCodeField);
		
		executeMethod.body()._if(startedField.ne(_null()))._then().invoke(startedField, "run");
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
