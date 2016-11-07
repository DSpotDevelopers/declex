package com.dspot.declex.override.holder;

import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr._this;

import java.util.Arrays;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.holder.EActivityHolder;
import org.androidannotations.plugin.PluginClassHolder;

import com.dspot.declex.action.ActionForHandler;
import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.helger.jcodemodel.JAnnotationUse;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class ActivityActionHolder extends PluginClassHolder<EActivityHolder> {

	public JDefinedClass ActivityAction;
	private JFieldVar contextField;
	private JFieldVar startedField;
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
	
	private void setActivityAction() {
		try {
			String clsName = holder().getAnnotatedElement().asType().toString();
			
			int index = clsName.lastIndexOf('.');
			String pkg = clsName.substring(0, index);
			String activityName = clsName.substring(index + 1);
			String actionName = pkg + "." + activityName + "ActionHolder";
							
			//This will make that the Action class be generated in the next round
			ActionForHandler.GENERATE_IN_ROUND = false;
			ActionInfo actionInfo = new ActionInfo(actionName);
			ActionForHandler.addAction(activityName, actionName, actionInfo);
			
			ActivityAction = getCodeModel()._class(actionName);
			ActivityAction.annotate(EBean.class);
			JAnnotationUse actionFor = ActivityAction.annotate(ActionFor.class);
			actionFor.param("value", activityName);			
			
			setFields();
			setInit(actionInfo);
			setBuild(actionInfo);
			setExecute(actionInfo);			
			setBuilder(actionInfo);
			
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
	
	private void setInit(ActionInfo actionInfo) {
		final String initName = "init";
		JMethod initMethod = ActivityAction.method(JMod.NONE, getCodeModel().VOID, initName);
		initMethod.body().assign(builderField, getGeneratedClass().staticInvoke("intent").arg(contextField));
		actionInfo.addMethod(initName, getCodeModel().VOID.fullName());
	}
	
	private void setBuild(ActionInfo actionInfo) {
		final String buildName = "build";
		
		JMethod buildMethod = ActivityAction.method(JMod.NONE, getCodeModel().VOID, buildName);
		JVar startedParam = buildMethod.param(getJClass(Runnable.class), "Started");
		
		buildMethod.body().assign(_this().ref(startedField), startedParam);
		
		actionInfo.addMethod(
				buildName, 
				getCodeModel().VOID.fullName(),
				Arrays.asList(new ActionMethodParam("Started", getJClass(Runnable.class)))
			);
	}
	
	private void setExecute(ActionInfo actionInfo) {
		final  String executeName = "execute";
		
		JMethod executeMethod = ActivityAction.method(JMod.NONE, getCodeModel().VOID, executeName);
		
		executeMethod.body().invoke(builderField, "start");		
		executeMethod.body()._if(startedField.ne(_null()))._then().invoke(startedField, "run");
		
		actionInfo.addMethod(executeName, getCodeModel().VOID.fullName());
	}
	
	private void setBuilder(ActionInfo actionInfo) {
		final String builderName = "builder";
		JMethod builderMethodMethod = ActivityAction.method(JMod.PUBLIC, holder().getIntentBuilderClass(), builderName);
		builderMethodMethod.body()._return(builderField);
		actionInfo.addMethod(builderName, holder().getIntentBuilderClass().fullName());
	}
}
