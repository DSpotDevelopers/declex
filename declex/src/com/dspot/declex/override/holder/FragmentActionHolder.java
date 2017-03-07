package com.dspot.declex.override.holder;

import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.lit;
import static org.androidannotations.helper.ModelConstants.generationSuffix;

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
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.holder.EFragmentHolder;
import org.androidannotations.plugin.PluginClassHolder;
import org.androidannotations.rclass.IRClass.Res;

import com.dspot.declex.action.Actions;
import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.dspot.declex.util.JavaDocUtils;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.util.TypeUtils.ClassInformation;
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

	private final static String CONTAINER_NAME = "container";
	private final static String TRANSACTION_NAME = "transaction";
	private final static String REPLACE_NAME = "replace";
	private final static String ADD_NAME = "add";
	private final static String INIT_NAME = "init";
	private final static String BUILD_NAME = "build";
	private final static String EXECUTE_NAME = "execute";
	private final static String ADD_TO_BACK_STACK_NAME = "addToBackStack";
	private final static String BUILDER_NAME = "builder";
	
	public JDefinedClass FragmentAction;
	private JFieldVar contextField;
	private JFieldVar startedField;
	private JFieldVar builderField;
	private JMethod initMethod;
	private JFieldVar containerField;
	private JFieldVar transactionField;
	private JFieldVar transactionMethodField;
	
	public FragmentActionHolder(EFragmentHolder holder) {
		super(holder);
	}
	
	public static void createInformationForActionHolder(Element element,
			AndroidAnnotationsEnvironment env) {
		
		final String clsName = element.asType().toString();
		final int index = clsName.lastIndexOf('.');
		final String pkg = clsName.substring(0, index);
		final String fragmentName = clsName.substring(index + 1);
		final String actionName = pkg + "." + fragmentName + "ActionHolder";
		
		ActionInfo actionInfo = new ActionInfo(actionName);
		
		//This will avoid generation for parent classes, not used in the project
		actionInfo.generated = false; 
		
		actionInfo.setReferences(JavaDocUtils.referenceFromClassName(clsName));
		Actions.getInstance().addAction(fragmentName, actionName, actionInfo);
		
		actionInfo.addMethod(CONTAINER_NAME, actionName);
		
		//FragmentTransaction can change the package
		actionInfo.addMethod(TRANSACTION_NAME, "android.app.FragmentTransaction");
		
		actionInfo.addMethod(REPLACE_NAME, actionName);
		
		actionInfo.addMethod(ADD_NAME, actionName);
		
		actionInfo.addMethod(INIT_NAME, env.getCodeModel().VOID.fullName());
		
		actionInfo.addMethod(
				BUILD_NAME, 
				env.getCodeModel().VOID.fullName(),
				Arrays.asList(new ActionMethodParam("Started", env.getJClass(Runnable.class)))
			);
		
		actionInfo.addMethod(EXECUTE_NAME, env.getCodeModel().VOID.fullName()); 
		
		actionInfo.addMethod(ADD_TO_BACK_STACK_NAME, actionName);
		
		actionInfo.addMethod(BUILDER_NAME, actionName + ".FragmentBuilder" + generationSuffix());
		
		addFragmentArgFieldsInformation(actionInfo, element, env);
	}

	private static void addFragmentArgFieldsInformation(ActionInfo actionInfo, Element element, AndroidAnnotationsEnvironment env) {
		List<Element> fragmentArgFields = new LinkedList<>();
		findFragmentArgFields(element, fragmentArgFields, env.getProcessingEnvironment());
		
		for (Element fragmentArgField : fragmentArgFields) {
			addFieldInformationToActionHolder(actionInfo, fragmentArgField, env);
		}
	}
	
	private static void addFieldInformationToActionHolder(
			ActionInfo actionInfo, Element element,
			AndroidAnnotationsEnvironment env) {
		
		final String clsName = element.getEnclosingElement().asType().toString();
		final int index = clsName.lastIndexOf('.');
		final String pkg = clsName.substring(0, index);
		final String fragmentName = clsName.substring(index + 1);
		final String actionName = pkg + "." + fragmentName + "ActionHolder";

		ClassInformation classInformation = TypeUtils.getClassInformation(element, env);
		final String className = classInformation.originalClassName;
		final String fieldName = element.getSimpleName().toString();
		
		actionInfo.addMethod(
				fieldName, 
				actionName, 
				Arrays.asList(new ActionMethodParam(fieldName, env.getJClass(className)))
			);
	}
	
	private static void findFragmentArgFields(Element element, List<Element> fragmentArgFields, ProcessingEnvironment env) {
		
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems) {
			if (elem.getKind() == ElementKind.FIELD) {
				if (elem.getAnnotation(FragmentArg.class) != null) {
					fragmentArgFields.add(elem);
				}
			}
		}
		
		List<? extends TypeMirror> superTypes = env.getTypeUtils().directSupertypes(element.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = env.getElementUtils().getTypeElement(type.toString());
			
			findFragmentArgFields(superElement, fragmentArgFields, env);
		}

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

				FragmentAction = getCodeModel()._class(actionName);
				FragmentAction.annotate(EBean.class);
				JAnnotationUse actionFor = FragmentAction.annotate(ActionFor.class);
				actionFor.param("value", fragmentName);		
				
				ActionInfo actionInfo = Actions.getInstance().getActionInfos().get(FragmentAction.fullName());
				actionInfo.generated = true;
				FragmentAction.javadoc().add(actionInfo.references);
				
				setFields();
				
				setInit();
				setContainer();
				setTransaction();
				setTransactionMethods();
				
				setBuild();
				setExecute();
				
				setBuilder();
				setAddToBackStack();			
			}
		} catch (Exception e) {
			e.printStackTrace();
		}				

	}
	
	private void setContainer() {
		JFieldRef containerRes = environment().getRClass().get(Res.ID).getIdStaticRef(CONTAINER_NAME, environment());
		if (containerRes != null) {
			containerField = FragmentAction.field(JMod.PRIVATE, getCodeModel().INT, CONTAINER_NAME, containerRes);
		} else {
			containerField = FragmentAction.field(JMod.PRIVATE, getCodeModel().INT, CONTAINER_NAME);
		}	
		
		JMethod containerMethod = FragmentAction.method(JMod.PUBLIC, FragmentAction, CONTAINER_NAME);
		JVar containerParam = containerMethod.param(getCodeModel().INT, CONTAINER_NAME);
		containerMethod.body().assign(_this().ref(containerField), containerParam);
		containerMethod.body()._return(_this());
	}
	
	private void setTransaction() {
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
		    transactionField = FragmentAction.field(JMod.PRIVATE, FragmentTransaction, TRANSACTION_NAME);
			
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
			transactionField = FragmentAction.field(JMod.PRIVATE, FragmentTransaction, TRANSACTION_NAME);
					
			JConditional ifIsActivity = initMethod.body()._if(contextField._instanceof(ACTIVITY));
			ifIsActivity._then().assign(
					transactionField, 
					invoke(cast(ACTIVITY, contextField), getFragmentManager).invoke("beginTransaction")
				);
		}
		
		JMethod transactionMethodMethod = FragmentAction.method(JMod.PUBLIC, FragmentTransaction, TRANSACTION_NAME);
		transactionMethodMethod.body()._return(transactionField);
	}
	
	private void setTransactionMethods() {
		JMethod replaceMethod = FragmentAction.method(JMod.PUBLIC, FragmentAction, REPLACE_NAME);
		replaceMethod.body().assign(_this().ref(transactionMethodField), lit(0));
		replaceMethod.body()._return(_this());

		JMethod addMethod = FragmentAction.method(JMod.PUBLIC, FragmentAction, ADD_NAME);
		addMethod.body().assign(_this().ref(transactionMethodField), lit(1));
		addMethod.body()._return(_this());
	}

	private void setFields() {
		contextField = FragmentAction.field(JMod.NONE, environment().getClasses().CONTEXT, "context");
		contextField.annotate(RootContext.class);
		
		startedField = FragmentAction.field(JMod.PRIVATE, getJClass(Runnable.class), "Started");
		builderField = FragmentAction.field(JMod.PRIVATE, holder().getBuilderClass(), "builder");
		
		transactionMethodField = FragmentAction.field(JMod.PRIVATE, getCodeModel().INT, "transactionMethod", lit(0));
	}
	
	private void setInit() {
		initMethod = FragmentAction.method(JMod.NONE, getCodeModel().VOID, INIT_NAME);
		initMethod.body().assign(builderField, getGeneratedClass().staticInvoke("builder"));
	}
	
	private void setBuild() {
		JMethod buildMethod = FragmentAction.method(JMod.NONE, getCodeModel().VOID, BUILD_NAME);
		JVar startedParam = buildMethod.param(getJClass(Runnable.class), "Started");
		buildMethod.body().assign(_this().ref(startedField), startedParam);
	}
	
	private void setExecute() {
		JMethod executeMethod = FragmentAction.method(JMod.NONE, getCodeModel().VOID, EXECUTE_NAME);

		executeMethod.body()._if(transactionField.eq(_null()))._then()._return();
		
		JInvocation transactionReplaceInvoke = transactionField.invoke("replace").arg(containerField)
				.arg(builderField.invoke("build"))
				.invoke("commit"); 

		JInvocation transactionAddInvoke = transactionField.invoke("add").arg(containerField)
				.arg(builderField.invoke("build"))
				.invoke("commit"); 
		
		JConditional ifReplace = executeMethod.body()._if(transactionMethodField.eq(lit(0)));
		ifReplace._then().add(transactionReplaceInvoke);
		ifReplace._elseif(transactionMethodField.eq(lit(1)))._then().add(transactionAddInvoke);
		
		executeMethod.body()._if(startedField.ne(_null()))._then().invoke(startedField, "run");
	}
	
	private void setAddToBackStack() {
		JMethod addToBackStackMethod = FragmentAction.method(JMod.PUBLIC, FragmentAction, ADD_TO_BACK_STACK_NAME);
		addToBackStackMethod.body()._if(transactionField.neNull())._then()
		                           .invoke(transactionField, "addToBackStack").arg(_null());
		addToBackStackMethod.body()._return(_this());
	}
	
	private void setBuilder() {
		JMethod builderMethodMethod = FragmentAction.method(JMod.PUBLIC, holder().getBuilderClass(), BUILDER_NAME);
		builderMethodMethod.body()._return(builderField);
	}
	
}
