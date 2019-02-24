package com.dspot.declex.holder;

import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.ref;

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.Element;

import org.androidannotations.helper.ADIHelper;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.internal.process.ProcessHolder;
import org.androidannotations.plugin.PluginClassHolder;

import com.dspot.declex.annotation.ExportPopulate;
import com.dspot.declex.annotation.Model;
import com.dspot.declex.annotation.Populate;
import com.dspot.declex.api.action.runnable.OnFailedRunnable;
import com.dspot.declex.util.SharedRecords;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.util.TypeUtils.ClassInformation;
import org.androidannotations.internal.virtual.VirtualElement;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JCatchBlock;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JTryBlock;
import com.helger.jcodemodel.JVar;

public class PopulateHolder extends PluginClassHolder<EComponentWithViewSupportHolder> {

	private static int uniquePriorityCounter = 5000;
	
	private Map<Element, JMethod> populateMethods = new HashMap<>();
	private Map<Element, JBlock> populateMethodsBlock = new HashMap<>();
	private JMethod populateThisMethod;
	
	//PopulateListeners are shared through all the PopulateHolders
	private static Map<Element, JFieldRef> populateListeners = new HashMap<>();
	
	private ADIHelper adiHelper;
	
	public PopulateHolder(EComponentWithViewSupportHolder holder) {
		super(holder);
		
		this.adiHelper = new ADIHelper(environment());
		
	}

	public JMethod getPopulateMethod(Element element) {
		JMethod populateMethod = populateMethods.get(element);
		if (populateMethod == null) {
			setPopulateMethod(element);
			populateMethod = populateMethods.get(element);
		}
		
		return populateMethod;
	}
	
	public JBlock getPopulateMethodBlock(Element element) {
		JBlock populateMethodBlock = populateMethodsBlock.get(element);
		if (populateMethodBlock == null) {
			setPopulateMethod(element);
			populateMethodBlock = populateMethodsBlock.get(element);
		}
		
		return populateMethodBlock;		
	}
	
	public JMethod getPopulateThis() {
		if (populateThisMethod == null) {
			setPopulateThis();
		}
		return populateThisMethod;
	}
	
	public JFieldRef getPopulateListener(Element element) {
		final boolean hasExportPopulate = adiHelper.getAnnotation(element, ExportPopulate.class) != null;
		if (!hasExportPopulate) return null;
		
		JFieldRef populateListener;
		if (element instanceof VirtualElement) {		
			populateListener = populateListeners.get(((VirtualElement) element).getElement());
		} else {
			populateListener = populateListeners.get(element);
		}

		if (populateListener == null) {
			if (element instanceof VirtualElement) {
				setPopulateMethod(element);
				populateListener = populateListeners.get(((VirtualElement) element).getElement());
			} else {
				final String fieldName = element.getSimpleName().toString();
				final String populateListenerName = "populate" + fieldName.substring(0, 1).toUpperCase()
                        + fieldName.substring(1);
				populateListener = ref(populateListenerName);
			}
		}
		
		return populateListener;
	}
	
	private void setPopulateMethod(Element element) {
		
		final String fieldName = element.getSimpleName().toString();

		JMethod populateMethod = holder().getGeneratedClass().method(JMod.NONE, getCodeModel().VOID, "_populate_" + fieldName);
		JVar afterPopulate = populateMethod.param(JMod.FINAL, getJClass(Runnable.class), "afterPopulate");
		populateMethod.param(JMod.FINAL, getJClass(OnFailedRunnable.class), "onFailed");
		
		populateMethods.put(element, populateMethod);
		
		JMethod populateThisMethod = getPopulateThis();
		if (!adiHelper.getAnnotation(element, Populate.class).independent()) {
			populateThisMethod.body().add(invoke(populateMethod).arg(_null()).arg(ref("onFailed")));
		}
		
		JTryBlock tryBlock = populateMethod.body()._try();
		{//Catch block
			JCatchBlock catchBlock = tryBlock._catch(getClasses().THROWABLE);
			JVar caughtException = catchBlock.param("e");
						
			IJStatement uncaughtExceptionCall = getClasses().THREAD 
					.staticInvoke("getDefaultUncaughtExceptionHandler") 
					.invoke("uncaughtException") 
					.arg(getClasses().THREAD.staticInvoke("currentThread")) 
					.arg(caughtException);
			
			JFieldRef onFailed = ref("onFailed");
			JConditional ifOnFailedAssigned = catchBlock.body()._if(onFailed.ne(_null()));
			ifOnFailedAssigned._then().add(invoke(onFailed, "onFailed").arg(caughtException));
			ifOnFailedAssigned._else().add(uncaughtExceptionCall);
		}
		populateMethodsBlock.put(element, tryBlock.body().blockVirtual());
		
		tryBlock.body()._if(afterPopulate.ne(_null()))._then()
		               .invoke(afterPopulate, "run");
		
		callPopulateAfterModelLoaded(element, populateMethod);
	}

	private void callPopulateAfterModelLoaded(Element element, JMethod populateMethod) {
		
		final String fieldName = element.getSimpleName().toString();
		final boolean hasExportPopulate = adiHelper.getAnnotation(element, ExportPopulate.class) != null;
		
		Model model = adiHelper.getAnnotation(element, Model.class); 
		if (model != null) {			
			final ModelHolder modelHolder;
			
			//Support ExportPopulate
			if (hasExportPopulate) {
								
				final Element referenceElement = ((VirtualElement) element).getReference();
				ClassInformation classInformation = TypeUtils.getClassInformation(referenceElement, environment(), true);
				ProcessHolder processHolder = environment().getProcessHolder();
				EComponentHolder holder = (EComponentHolder) processHolder.getGeneratedClassHolder(classInformation.generatorElement);
				
				modelHolder = holder.getPluginHolder(new ModelHolder(holder));
				element = ((VirtualElement) element).getElement();
				
				if (populateListeners.containsKey(element)) {
					//This means the call was already created
					return;
				};
				
			} else {
				modelHolder = holder().getPluginHolder(new ModelHolder(holder()));				
			}
			
			JBlock methodBody = modelHolder.getAfterLoadModelBlock(element);
			
			if (model.async()) {
				JDefinedClass anonymousRunnable = getCodeModel().anonymousClass(Runnable.class);
				JMethod anonymousRunnableRun = anonymousRunnable.method(JMod.PUBLIC, getCodeModel().VOID, "run");
				anonymousRunnableRun.annotate(Override.class);
				
				JVar handler = methodBody.decl(getClasses().HANDLER, "handler", 
						_new(getClasses().HANDLER).arg(getClasses().LOOPER.staticInvoke("getMainLooper")));
				methodBody.add(invoke(handler, "post").arg(_new(anonymousRunnable)));
				
				methodBody = anonymousRunnableRun.body();
			} 
			
			JBlock ifPopulate = methodBody._if(
				ref("args").invoke("containsKey").arg("populate").not()
				.cor(cast(getJClass(Boolean.class), ref("args").invoke("get").arg("populate")))
			)._then();
			
			if (hasExportPopulate) {
				final String populateListenerName = "populate" + fieldName.substring(0, 1).toUpperCase()
                        + fieldName.substring(1);
				
				JFieldRef listenerField = ref(populateListenerName);				
  			    ifPopulate._if(listenerField.neNull())._then()
						  .add(invoke(listenerField, "populateModel").arg(_null()).arg(ref("onFailed")));
				
  			    populateListeners.put(element, listenerField);
  			    
			} else {
				ifPopulate.add(invoke(populateMethod).arg(_null()).arg(ref("onFailed")));
			}
			
		}
	}
	
	private void setPopulateThis() {
		populateThisMethod = holder().getGeneratedClass().method(JMod.NONE, getCodeModel().VOID, "_populate_this");
		JVar afterPopulate = populateThisMethod.param(JMod.FINAL, getJClass(Runnable.class), "afterPopulate");
		populateThisMethod.param(JMod.FINAL, getJClass(OnFailedRunnable.class), "onFailed");
		
		JBlock block = new JBlock();
		block._if(afterPopulate.neNull())._then()
		     .invoke(afterPopulate, "run");
		SharedRecords.priorityAdd(populateThisMethod.body(), block, uniquePriorityCounter);
	}
}
