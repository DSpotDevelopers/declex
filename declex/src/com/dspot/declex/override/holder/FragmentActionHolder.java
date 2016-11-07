package com.dspot.declex.override.holder;

import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.invoke;

import java.util.Arrays;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.holder.EFragmentHolder;
import org.androidannotations.plugin.PluginClassHolder;
import org.androidannotations.rclass.IRClass.Res;

import com.dspot.declex.action.ActionForHandler;
import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JAnnotationUse;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public class FragmentActionHolder extends PluginClassHolder<EFragmentHolder> {

	public JDefinedClass FragmentAction;
	private JFieldVar contextField;
	private JFieldVar startedField;
	private JFieldVar builderField;
	private JMethod initMethod;
	private JFieldVar containerField;
	private JFieldVar transactionField;
	
	public FragmentActionHolder(EFragmentHolder holder) {
		super(holder);
	}
	
	public JDefinedClass getFragmentAction() {
		if (FragmentAction == null) {
			setFragmentAction();
		}
		return FragmentAction;
	}
	
	private void setFragmentAction() {
		try {
			String clsName = holder().getAnnotatedElement().asType().toString();
			
			int index = clsName.lastIndexOf('.');
			String pkg = clsName.substring(0, index);
			String fragmentName = clsName.substring(index + 1);
			String actionName = pkg + "." + fragmentName + "ActionHolder";
			
			FragmentAction = getCodeModel()._getClass(actionName);
			if (FragmentAction == null) {
				
				//This will make that the Action class be generated in the next round
				ActionForHandler.GENERATE_IN_ROUND = false;
				ActionInfo actionInfo = new ActionInfo(actionName);
				ActionForHandler.addAction(fragmentName, actionName, actionInfo);
				
				FragmentAction = getCodeModel()._class(actionName);
				FragmentAction.annotate(EBean.class);
				JAnnotationUse actionFor = FragmentAction.annotate(ActionFor.class);
				actionFor.param("value", fragmentName);		
				
				setFields();
				
				setInit(actionInfo);
				setContainer(actionInfo);
				setTransaction(actionInfo);
				
				setBuild(actionInfo);
				setExecute(actionInfo);
				
				setBuilder(actionInfo);
				setAddToBackStack(actionInfo);			
			}
		} catch (Exception e) {
			e.printStackTrace();
		}				

	}
	
	private void setContainer(ActionInfo actionInfo) {
		final String containerName = "container";
		
		JFieldRef containerRes = environment().getRClass().get(Res.ID).getIdStaticRef(containerName, environment());
		if (containerRes != null) {
			containerField = FragmentAction.field(JMod.PRIVATE, getCodeModel().INT, containerName, containerRes);
		} else {
			containerField = FragmentAction.field(JMod.PRIVATE, getCodeModel().INT, containerName);
		}	
		
		JMethod containerMethod = FragmentAction.method(JMod.PUBLIC, FragmentAction, containerName);
		JVar containerParam = containerMethod.param(getCodeModel().INT, containerName);
		containerMethod.body().assign(_this().ref(containerField), containerParam);
		containerMethod.body()._return(_this());
		actionInfo.addMethod(containerName, getCodeModel().INT.fullName());
	}
	
	private void setTransaction(ActionInfo actionInfo) {
		AbstractJClass ACTIVITY = environment().getClasses().ACTIVITY;
		AbstractJClass FragmentTransaction =  getJClass("android.app.FragmentTransaction");
		AbstractJClass AppCompatActivity = getJClass("android.support.v7.app.AppCompatActivity");
		AbstractJClass ActionBarActivity = getJClass("android.support.v7.app.ActionBarActivity");
		
		String getFragmentManager = "getFragmentManager";
		String elementSuperClass = getGeneratedClass().fullName();
		elementSuperClass = elementSuperClass.substring(0, elementSuperClass.length()-1);
		
		if (TypeUtils.isSubtype(elementSuperClass, CanonicalNameConstants.SUPPORT_V4_FRAGMENT, environment().getProcessingEnvironment())) {
			getFragmentManager = "getSupportFragmentManager";
			FragmentTransaction = getJClass("android.support.v4.app.FragmentTransaction");
		    transactionField = FragmentAction.field(JMod.PRIVATE, FragmentTransaction, "transaction");
			
			JConditional ifIsActivity = initMethod.body()._if(contextField._instanceof(AppCompatActivity));
			ifIsActivity._then().assign(
					transactionField, 
					invoke(cast(AppCompatActivity, contextField), getFragmentManager).invoke("beginTransaction")
				);
			ifIsActivity._elseif(contextField._instanceof(ActionBarActivity))._then().assign(
					transactionField, 
					invoke(cast(ActionBarActivity, contextField), getFragmentManager).invoke("beginTransaction")
				);
		} else {
			transactionField = FragmentAction.field(JMod.PRIVATE, FragmentTransaction, "transaction");
					
			JConditional ifIsActivity = initMethod.body()._if(contextField._instanceof(ACTIVITY));
			ifIsActivity._then().assign(
					transactionField, 
					invoke(cast(ACTIVITY, contextField), getFragmentManager).invoke("beginTransaction")
				);
		}
		
		JMethod transactionMethodMethod = FragmentAction.method(JMod.PUBLIC, FragmentTransaction, "transaction");
		transactionMethodMethod.body()._return(transactionField);
		actionInfo.addMethod("transaction", holder().getBuilderClass().fullName());
	}

	private void setFields() {
		contextField = FragmentAction.field(JMod.NONE, environment().getClasses().CONTEXT, "context");
		contextField.annotate(RootContext.class);
		
		startedField = FragmentAction.field(JMod.PRIVATE, getJClass(Runnable.class), "Started");
		builderField = FragmentAction.field(JMod.PRIVATE, holder().getBuilderClass(), "builder");
	}
	
	private void setInit(ActionInfo actionInfo) {
		final String initName = "init";
		initMethod = FragmentAction.method(JMod.NONE, getCodeModel().VOID, initName);
		initMethod.body().assign(builderField, getGeneratedClass().staticInvoke("builder"));
		actionInfo.addMethod(initName, getCodeModel().VOID.fullName());
	}
	
	private void setBuild(ActionInfo actionInfo) {
		final String buildName = "build";
		
		JMethod buildMethod = FragmentAction.method(JMod.NONE, getCodeModel().VOID, buildName);
		JVar startedParam = buildMethod.param(getJClass(Runnable.class), "Started");
		buildMethod.body().assign(_this().ref(startedField), startedParam);
		
		actionInfo.addMethod(
				buildName, 
				getCodeModel().VOID.fullName(),
				Arrays.asList(new ActionMethodParam("Started", getJClass(Runnable.class)))
			);
	}
	
	private void setExecute(ActionInfo actionInfo) {
		final String executeName = "execute";
		JMethod executeMethod = FragmentAction.method(JMod.NONE, getCodeModel().VOID, executeName);
						
		JInvocation transactionInvoke = transactionField.invoke("replace").arg(containerField)
											.arg(builderField.invoke("build"))
											.invoke("commit"); 
		executeMethod.body().add(transactionInvoke);
		
		executeMethod.body()._if(startedField.ne(_null()))._then().invoke(startedField, "run");
		
		actionInfo.addMethod(executeName, getCodeModel().VOID.fullName()); 	
	}
	
	private void setAddToBackStack(ActionInfo actionInfo) {
		final String addToBackStack = "addToBackStack";
		JMethod addToBackStackMethod = FragmentAction.method(JMod.PUBLIC, getCodeModel().VOID, addToBackStack);
		addToBackStackMethod.body().invoke(transactionField, "addToBackStack").arg(_null());
		actionInfo.addMethod(addToBackStack, getCodeModel().VOID.fullName());
	}
	
	private void setBuilder(ActionInfo actionInfo) {
		final String builderName = "builder";
		JMethod builderMethodMethod = FragmentAction.method(JMod.PUBLIC, holder().getBuilderClass(), builderName);
		builderMethodMethod.body()._return(builderField);
		actionInfo.addMethod(builderName, holder().getBuilderClass().fullName());
	}
	
}
