package com.dspot.declex.model;

import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.dotclass;
import static com.helger.jcodemodel.JExpr.lit;
import static com.helger.jcodemodel.JExpr.ref;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

import org.androidannotations.api.BackgroundExecutor;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.holder.BaseGeneratedClassHolder;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.internal.process.ProcessHolder;
import org.androidannotations.plugin.PluginClassHolder;

import com.dspot.declex.api.model.Model;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.util.TypeUtils.ClassInformation;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJAssignmentTarget;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JAnonymousClass;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JCatchBlock;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JPrimitiveType;
import com.helger.jcodemodel.JTryBlock;
import com.helger.jcodemodel.JVar;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

public class ModelHolder extends PluginClassHolder<EComponentHolder> {
	
	private Map<Element, PutModelRecord> putModelMethods = new HashMap<>();
	private Map<Element, GetModelRecord> getModelMethods = new HashMap<>();
	
	final AbstractJClass STRING;
	final AbstractJClass LIST;
	final AbstractJClass CONTEXT;
	final AbstractJClass ARRAYS;
	final AbstractJClass THROWABLE;
	final AbstractJClass THREAD;
	final JPrimitiveType VOID;
	
	public ModelHolder(EComponentHolder holder) {
		super(holder);
		
		STRING = environment().getClasses().STRING;
		LIST = environment().getClasses().LIST;
		CONTEXT = environment().getClasses().CONTEXT;
		ARRAYS = environment().getClasses().ARRAYS;
		THROWABLE = environment().getClasses().THROWABLE;
		THREAD = environment().getClasses().THREAD;
		VOID = getCodeModel().VOID;
	}
	
	private UseModelHolder useModelHolderForElement(Element element) {
		ClassInformation classInformation = TypeUtils.getClassInformation(element, environment(), true);
		ProcessHolder processHolder = environment().getProcessHolder();
		BaseGeneratedClassHolder useModelComponentHolder = 
				(BaseGeneratedClassHolder) processHolder.getGeneratedClassHolder(classInformation.generatorElement);
		
		return useModelComponentHolder.getPluginHolder(new UseModelHolder(useModelComponentHolder));
	}
	
	public JMethod getGetModelMethod(Element element) {
		GetModelRecord getModelRecord = getModelMethods.get(element);
		if (getModelRecord == null) {
			getModelRecord = setGetModelMethod(element);
		}
			
		return getModelRecord.getModelMethod;
	}
	
	public JBlock getAfterGetModelBlock(Element element) {
		GetModelRecord getModelRecord = getModelMethods.get(element);
		if (getModelRecord == null) {
			getModelRecord = setGetModelMethod(element);
		}
			
		return getModelRecord.afterGetModelBlock;
	}
	
	private GetModelRecord setGetModelMethod(Element element) {

		final UseModelHolder useModelHolder = useModelHolderForElement(element);
		
		final String fieldName = element.getSimpleName().toString();
		final Model annotation = element.getAnnotation(Model.class);
		final boolean isStatic = element.getModifiers().contains(Modifier.STATIC);
		
		IJAssignmentTarget beanField = ref(fieldName);
		String className = TypeUtils.typeFromTypeString(element.asType().toString(), environment());
		
		JMethod getModelMethod = getGeneratedClass().method(JMod.NONE | (isStatic? JMod.STATIC : 0), getCodeModel().VOID, "_get_" + fieldName);
		JVar context = getModelMethod.param(JMod.FINAL, CONTEXT, "context");
		JVar query = getModelMethod.param(JMod.FINAL, STRING, "query");
		JVar orderBy = getModelMethod.param(JMod.FINAL, STRING, "orderBy");
		JVar onFinished = getModelMethod.param(JMod.FINAL, getJClass(Runnable.class), "onFinished");
		
		JBlock block = getModelMethod.body();
		
		
		//Check if this is a list @Model
		boolean isList = TypeUtils.isSubtype(element, CanonicalNameConstants.LIST, environment().getProcessingEnvironment());		
		if (isList) {
			Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(className);
			if (matcher.find()) {
				className = matcher.group(1);
			}
			
			beanField = ref(fieldName + "Local");
			
			getModelMethod.annotate(SuppressWarnings.class).paramArray("value").param("unchecked").param("rawtypes");
		} 
			
		String converted = null;
		if (!className.endsWith("_")) {
			converted = className;
			className = className + "_";
		}
		
		AbstractJClass ModelClass = getJClass(className);
		
		
		//======CODE GENERATION========
	
		final List<? extends AnnotationMirror> annotations = element.getAnnotationMirrors();
		final JInvocation annotations_invocation = ARRAYS
				                                      .staticInvoke("asList")
				                                      .arg(dotclass(getJClass(Annotation.class)));
		final Map<Integer, AbstractJClass> annotationClasses = new TreeMap<>();
		
		//Insert the annotations in the order of appearance 
		//TreePathScanner is used to determine the order in the code and ensure
		//that it doens't depends on the compiler implementation
		final Trees trees = Trees.instance(environment().getProcessingEnvironment());
    	final TreePath treePath = trees.getPath(element);
    	TreePathScanner<Object, Trees> scanner = new TreePathScanner<Object, Trees>() {
    		
    		@Override
    		public Object visitAnnotation(AnnotationTree annotationTree, Trees trees) {
    			String annotationName = annotationTree.getAnnotationType().toString();
    			
    			for (AnnotationMirror annotation : annotations) {
    				final String annotationCanonicalName = annotation.getAnnotationType().toString();
    				if (annotationCanonicalName.equals(annotationName)
    	    				|| annotationCanonicalName.endsWith("." + annotationName)) {
    					
    					int position = (int) trees.getSourcePositions()
								  .getStartPosition(treePath.getCompilationUnit(), annotationTree);
    					
    					annotationClasses.put(position, getJClass(annotationCanonicalName));
    				}
    			}
    			
    			return super.visitAnnotation(annotationTree, trees);
    		}
    		
    	};
    	scanner.scan(treePath, trees);  
    	
		for (Entry<Integer, AbstractJClass> annotationEntry : annotationClasses.entrySet()) {
			annotations_invocation.arg(dotclass(annotationEntry.getValue())); 
		}
		
		JMethod getModelInjectionMethod = isList ? useModelHolder.getGetModelListMethod() 
				                                 : useModelHolder.getGetModelMethod();
		JInvocation getModel = ModelClass.staticInvoke(getModelInjectionMethod)
				  .arg(context)
				  .arg(query)
				  .arg(orderBy)
				  .arg(annotations_invocation);
		
		JBlock assign;
		
		//Use the BackgroundExecutor for asynchronous calls
		if (annotation.async()) {
			JDefinedClass anonymousTaskClass = getCodeModel().anonymousClass(BackgroundExecutor.Task.class);

			JMethod executeMethod = anonymousTaskClass.method(JMod.PUBLIC, getCodeModel().VOID, "execute");
			executeMethod.annotate(Override.class);

			// Catch exception in user code
			JTryBlock tryBlock = executeMethod.body()._try();
			
			if (isList) tryBlock.body().decl(LIST.narrow(ModelClass), fieldName + "Local");
			
			assign = tryBlock.body().assign(beanField, getModel);
			JCatchBlock catchBlock = tryBlock._catch(THROWABLE);
			JVar caughtException = catchBlock.param("e");
			IJStatement uncaughtExceptionCall = THREAD 
					.staticInvoke("getDefaultUncaughtExceptionHandler") 
					.invoke("uncaughtException") 
					.arg(THREAD.staticInvoke("currentThread")) 
					.arg(caughtException);
			catchBlock.body().add(uncaughtExceptionCall);

			AbstractJClass backgroundExecutorClass = getJClass(BackgroundExecutor.class);
			JInvocation newTask = _new(anonymousTaskClass).arg(lit("")).arg(lit(0)).arg(lit(""));
			JInvocation executeCall = backgroundExecutorClass.staticInvoke("execute").arg(newTask);
			
			block.add(executeCall);
		} else {
			if (isList) block.decl(LIST.narrow(ModelClass), fieldName + "Local");
			assign = block.assign(beanField, getModel);
		}
		
		if (isList) {
			IJExpression assignField = beanField;
			
			if (converted != null) {
				assignField = cast(LIST.narrow(getJClass(converted)), cast(LIST, beanField));
			}
			
			JFieldRef view = ref(fieldName);
			JConditional ifCond = assign._if(view.eq(_null()));
			ifCond._then().assign(view, assignField);
			ifCond._else().invoke(view, "clear");
			ifCond._else().invoke(view, "addAll").arg(assignField);
			
			JBlock forEachBody = assign.forEach(getJClass(converted == null ? className : converted), "model", beanField).body();
			forEachBody.invoke(converted == null ? ref("model") : cast(ModelClass, ref("model")), useModelHolder.getModelInitMethod())
			  .arg(query)
			  .arg(orderBy);	
		} else {
			assign.invoke(converted == null ? beanField : cast(ModelClass, beanField), useModelHolder.getModelInitMethod())
			  .arg(query)
			  .arg(orderBy);	
		}
		
		JBlock afterGetModelBlock = assign.block();
		
		assign._if(onFinished.ne(_null()))._then()
			  .invoke(onFinished, "run");

		GetModelRecord getModelRecord = new GetModelRecord(getModelMethod, afterGetModelBlock);
		getModelMethods.put(element, getModelRecord);
		return getModelRecord;
	}
	
	public JMethod getPutModelMethod(Element element) {
		PutModelRecord putModelRecord = putModelMethods.get(element);
		if (putModelRecord == null) {
			putModelRecord = setPutModelMethod(element);
		}
			
		return putModelRecord.putModelMethod;
	}
	
	public JBlock getPutModelMethodBlock(Element element) {
		PutModelRecord putModelRecord = putModelMethods.get(element);
		if (putModelRecord == null) {
			putModelRecord = setPutModelMethod(element);
		}
			
		return putModelRecord.putModelMethodBlock;
	}
	
	private PutModelRecord setPutModelMethod(Element element) {
		
		final UseModelHolder useModelHolder = useModelHolderForElement(element);
		
		final String fieldName = element.getSimpleName().toString();

		JFieldRef beanField = ref(fieldName);
		String className = TypeUtils.typeFromTypeString(element.asType().toString(), environment());
		
		JMethod putModelMethod = getGeneratedClass().method(JMod.NONE, getCodeModel().VOID, "_put_" + fieldName);
		JVar query = putModelMethod.param(JMod.FINAL, STRING, "query");
		JVar orderBy = putModelMethod.param(JMod.FINAL, STRING, "orderBy");
		JVar onFinished = putModelMethod.param(JMod.FINAL, getJClass(Runnable.class), "onFinished");
				
		JAnonymousClass PutModelRunnable = getCodeModel().anonymousClass(getJClass(Runnable.class));
		JMethod run = PutModelRunnable.method(JMod.PUBLIC, VOID, "run");
		PutModelRunnable.annotate(Override.class);
		
		//Populate the _put_ method
		JBlock block = putModelMethod.body();
		JVar putModelRunnable = block.decl(PutModelRunnable, "putModelRunnable", _new(PutModelRunnable));
		JBlock putModelMethodBlock = block.block();
		putModelMethodBlock.invoke(putModelRunnable, "run");
	
		block = run.body();
		
		//Check if this is a list @Model
		boolean isList = TypeUtils.isSubtype(element, CanonicalNameConstants.LIST, environment().getProcessingEnvironment());		
		if (isList) {
			Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(className);
			if (matcher.find()) {
				className = matcher.group(1);
			}
			
			beanField = ref(fieldName + "Local");
		} 
				
		String converted = null;
		if (!className.endsWith("_")) {
			converted = className;
			className = className + "_";
		}
		
		AbstractJClass ModelClass = getJClass(className);
		
		//======CODE GENERATION========
		
		JBlock putModel = new JBlock();
		putModel._if(
				(converted == null ? beanField : cast(ModelClass, beanField))
				  	.invoke(useModelHolder.getPutModelMethod())
				  	.arg(query)
				  	.arg(orderBy)
				  	.eq(_null())
			)
			._then()
			._return();
		
		
		//Use the BackgroundExecutor for asynchronous calls
		final Model annotation = element.getAnnotation(Model.class);
		if (annotation != null && annotation.asyncPut()) {
			JDefinedClass anonymousTaskClass = getCodeModel().anonymousClass(BackgroundExecutor.Task.class);

			JMethod executeMethod = anonymousTaskClass.method(JMod.PUBLIC, getCodeModel().VOID, "execute");
			executeMethod.annotate(Override.class);

			// Catch exception in user code
			JTryBlock tryBlock = executeMethod.body()._try();
			
			if (isList) {
				JBlock forEachBlock = tryBlock.body().forEach((converted == null ? ModelClass : getJClass(converted)), fieldName + "Local", ref(fieldName)).body();				
				forEachBlock.add(putModel);
			} else {
				tryBlock.body().add(putModel);
			}
			
			tryBlock.body()._if(onFinished.ne(_null()))._then()
						   .invoke(onFinished, "run");
			
			JCatchBlock catchBlock = tryBlock._catch(THROWABLE);
			JVar caughtException = catchBlock.param("e");
			IJStatement uncaughtExceptionCall = THREAD 
					.staticInvoke("getDefaultUncaughtExceptionHandler") 
					.invoke("uncaughtException") 
					.arg(THREAD.staticInvoke("currentThread")) 
					.arg(caughtException);
			catchBlock.body().add(uncaughtExceptionCall);

			AbstractJClass backgroundExecutorClass = getJClass(BackgroundExecutor.class);
			JInvocation newTask = _new(anonymousTaskClass).arg(lit("")).arg(lit(0)).arg(lit(""));
			JInvocation executeCall = backgroundExecutorClass.staticInvoke("execute").arg(newTask);
			
			block.add(executeCall);
		} else {
			if (isList) {
				JBlock forEachBlock = block.forEach((converted == null ? ModelClass : getJClass(converted)), fieldName + "Local", ref(fieldName)).body();				
				forEachBlock.add(putModel);
			} else {
				block.add(putModel);
			}
			
			block._if(onFinished.ne(_null()))._then()
			     .invoke(onFinished, "run");
		}
		
		PutModelRecord putModelRecord = new PutModelRecord(putModelMethod, putModelMethodBlock);
		putModelMethods.put(element, putModelRecord);
		return putModelRecord;
	}

	public IJExpression getContextRef() {
		return holder().getContextRef();
	}	
	
	private class GetModelRecord {
		JMethod getModelMethod;
		JBlock afterGetModelBlock;
		
		public GetModelRecord(JMethod getModelMethod, JBlock afterGetModelBlock) {
			this.getModelMethod = getModelMethod;
			this.afterGetModelBlock = afterGetModelBlock;
		}
	}
	
	private class PutModelRecord {
		JMethod putModelMethod;
		JBlock putModelMethodBlock;
		
		public PutModelRecord(JMethod putModelMethod, JBlock putModelMethodBlock) {
			this.putModelMethod = putModelMethod;
			this.putModelMethodBlock = putModelMethodBlock;
		}
	}

}
