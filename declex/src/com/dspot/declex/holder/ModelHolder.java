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
package com.dspot.declex.holder;

import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.cond;
import static com.helger.jcodemodel.JExpr.dotclass;
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.lit;
import static com.helger.jcodemodel.JExpr.ref;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedList;
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
import org.androidannotations.helper.ADIHelper;
import org.androidannotations.helper.APTCodeModelHelper;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.BaseGeneratedClassHolder;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.internal.process.ProcessHolder;
import org.androidannotations.plugin.PluginClassHolder;

import com.dspot.declex.annotation.External;
import com.dspot.declex.annotation.ExternalPopulate;
import com.dspot.declex.annotation.ExternalRecollect;
import com.dspot.declex.annotation.Model;
import com.dspot.declex.api.action.runnable.OnFailedRunnable;
import com.dspot.declex.api.util.FormatsUtils;
import com.dspot.declex.helper.FilesCacheHelper;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.util.TypeUtils.ClassInformation;
import com.dspot.declex.wrapper.element.VirtualElement;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.AbstractJType;
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
import com.helger.jcodemodel.JSynchronizedBlock;
import com.helger.jcodemodel.JTryBlock;
import com.helger.jcodemodel.JVar;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

public class ModelHolder extends PluginClassHolder<EComponentHolder> {
	
	private Map<Element, ModelMethod> putModelMethods = new HashMap<>();
	private Map<Element, ModelMethod> loadModelMethods = new HashMap<>();
	
	private Map<Element, ModelMethod> getterModelMethods = new HashMap<>();
	private Map<Element, ModelMethod> setterModelMethods = new HashMap<>();
	
	final AbstractJClass STRING;
	final AbstractJClass LIST;
	final AbstractJClass MAP;
	final AbstractJClass CONTEXT;
	final AbstractJClass ARRAYS;
	final AbstractJClass THROWABLE;
	final AbstractJClass THREAD;
	final JPrimitiveType VOID;
	
	private APTCodeModelHelper codeModelHelper;
	private ADIHelper adiHelper;
		
	public ModelHolder(EComponentHolder holder) {
		super(holder);
		
		STRING = environment().getClasses().STRING;
		LIST = environment().getClasses().LIST;
		MAP = environment().getClasses().MAP.narrow(String.class, Object.class);
		CONTEXT = environment().getClasses().CONTEXT;
		ARRAYS = environment().getClasses().ARRAYS;
		THROWABLE = environment().getClasses().THROWABLE;
		THREAD = environment().getClasses().THREAD;
		VOID = getCodeModel().VOID;
		
		codeModelHelper = new APTCodeModelHelper(environment());
		adiHelper = new ADIHelper(environment());
	}
	
	public JMethod getGetterMethod(Element element) {
		ModelMethod getter = getterModelMethods.get(element);
		if (getter == null) {
			getter = setGetterMethod(element);
		}
			
		return getter.method;
	}
	
	public JBlock getGetterBody(Element element) {
		ModelMethod getter = getterModelMethods.get(element);
		if (getter == null) {
			getter = setGetterMethod(element);
		}
			
		return getter.methodBlock;
	}
	
	public JMethod getSetterMethod(Element element) {
		ModelMethod setter = setterModelMethods.get(element);
		if (setter == null) {
			setter = setSetterMethod(element);
		}
			
		return setter.method;
	}
	
	public JBlock getSetterBody(Element element) {
		ModelMethod setter = setterModelMethods.get(element);
		if (setter == null) {
			setter = setSetterMethod(element);
		}
			
		return setter.methodBlock;
	}
	
	private ModelMethod setSetterMethod(Element element) {
		
		final boolean isStatic = element.getModifiers().contains(Modifier.STATIC);
		final String elementName = element.getSimpleName().toString();

		ModelMethod modelMethod = null;
		
		BaseGeneratedClassHolder holder = holder();
		if (element instanceof VirtualElement && (adiHelper.getAnnotation(element, External.class) != null 
			|| adiHelper.getAnnotation(element, ExternalPopulate.class) != null
			|| adiHelper.getAnnotation(element, ExternalRecollect.class) != null)) {
		
			//It will never be static here
			
			JMethod setter = holder.getGeneratedClass().method(
					JMod.PUBLIC,
					getCodeModel().VOID,
					FormatsUtils.fieldToSetter(elementName)
				);
			JVar param = setter.param(elementModelClass(element), elementName);
					
			final Element referenceElement = ((VirtualElement) element).getReference();
			final String referenceElementName = referenceElement.getSimpleName().toString();
			String referenceElementClass = referenceElement.asType().toString();
			
			boolean converted = false;
			if (!referenceElementClass.endsWith(ModelConstants.generationSuffix())) {
				converted = true;
				referenceElementClass = TypeUtils.getGeneratedClassName(referenceElementClass, environment());
			}
			
			setter.body()._if(ref(referenceElementName).eqNull())._then()._return();
			if (converted) {
				setter.body().add(
					cast(getJClass(referenceElementClass), ref(referenceElementName)).invoke(setter).arg(param)
				);
			} else {
				setter.body().add(
					ref(referenceElementName).invoke(setter).arg(param)
				);				
			}
			
			modelMethod = new ModelMethod(setter, setter.body());
			
			if (adiHelper.getAnnotation(element, ExternalPopulate.class) != null) {
				setterModelMethods.put(element, modelMethod);
				return modelMethod;
			}
			
			ClassInformation classInformation = TypeUtils.getClassInformation(referenceElement, environment(), true);
			ProcessHolder processHolder = environment().getProcessHolder();
			holder = (BaseGeneratedClassHolder) processHolder.getGeneratedClassHolder(classInformation.generatorElement);
			
		}
				
		JMethod setter = holder.getGeneratedClass().getMethod(
				FormatsUtils.fieldToSetter(elementName),
				new AbstractJType[]{elementModelClass(element)}
			);
		if (setter == null) {		
			
			int mods = JMod.PUBLIC;
			if (isStatic) {
				mods |= JMod.STATIC;
			}
			
			setter = holder.getGeneratedClass().method(
					mods,
					getCodeModel().VOID,
					FormatsUtils.fieldToSetter(elementName)
				);
			setter.param(elementModelClass(element), elementName);
		}
		
		//Remove previous method body
		codeModelHelper.removeBody(setter);
		
		if (isStatic) {
			setter.body().assign(holder.getGeneratedClass().staticRef(elementName), ref(elementName));
		} else {
			setter.body().assign(_this().ref(elementName), ref(elementName));			
		}
		
		if (modelMethod != null) {
			modelMethod.methodBlock = setter.body();
			setterModelMethods.put(element, modelMethod);
			return modelMethod;
		}
		
		modelMethod = new ModelMethod(setter, setter.body());
		setterModelMethods.put(element, modelMethod);
		
		return modelMethod;
	}
	
	private ModelMethod setGetterMethod(Element element) {
		
		final String elementName = element.getSimpleName().toString();
		final boolean isStatic = element.getModifiers().contains(Modifier.STATIC);
		final boolean isLazy = adiHelper.getAnnotation(element, Model.class).lazy();
		
		ModelMethod modelMethod = null;
		
		BaseGeneratedClassHolder holder = holder();
		if (element instanceof VirtualElement && (adiHelper.getAnnotation(element, External.class) != null 
			|| adiHelper.getAnnotation(element, ExternalPopulate.class) != null
			|| adiHelper.getAnnotation(element, ExternalRecollect.class) != null)) {

			//It will never be static here
			
			JMethod getter = holder.getGeneratedClass().method(
					JMod.PUBLIC,
					elementModelClass(element),
					FormatsUtils.fieldToGetter(elementName)
				);
					
			final Element referenceElement = ((VirtualElement) element).getReference();
			final String referenceElementName = referenceElement.getSimpleName().toString();
			String referenceElementClass = referenceElement.asType().toString();
			
			boolean converted = false;
			if (!referenceElementClass.endsWith(ModelConstants.generationSuffix())) {
				converted = true;
				referenceElementClass = TypeUtils.getGeneratedClassName(referenceElementClass, environment());
			}
			
			JBlock getterBody = getter.body().blockVirtual();
			getter.body()._if(ref(referenceElementName).eqNull())._then()._return(_null());
			if (converted) {
				getter.body()._return(
					cast(getJClass(referenceElementClass), ref(referenceElementName)).invoke(getter)
				);
			} else {
				getter.body()._return(
					ref(referenceElementName).invoke(getter)
				);				
			}
			
			modelMethod = new ModelMethod(getter, getterBody);
			
			if (adiHelper.getAnnotation(element, ExternalPopulate.class) != null) {
				getterModelMethods.put(element, modelMethod);
				return modelMethod;
			}
			
			ClassInformation classInformation = TypeUtils.getClassInformation(referenceElement, environment(), true);
			ProcessHolder processHolder = environment().getProcessHolder();
			holder = (BaseGeneratedClassHolder) processHolder.getGeneratedClassHolder(classInformation.generatorElement);
		}
				
		AbstractJType[] params = isStatic && isLazy ? new AbstractJType[]{getClasses().CONTEXT} : new AbstractJType[]{};
		JMethod getter = holder.getGeneratedClass().getMethod(
				FormatsUtils.fieldToGetter(elementName),
				params
			);
		if (getter == null) {
			
			int mods = JMod.PUBLIC;
			if (isStatic) {
				mods |= JMod.STATIC;
			}
			
			getter = holder.getGeneratedClass().method(
					mods,
					elementModelClass(element),
					FormatsUtils.fieldToGetter(elementName)
				);
			
			if (isStatic && isLazy) {
				getter.param(getClasses().CONTEXT, "context");
			}
		}
		
		//Remove previous method body
		codeModelHelper.removeBody(getter);
		
		JFieldRef field = ref(element.getSimpleName().toString());
		JBlock getterBody = getter.body().blockVirtual();
		getter.body()._return(field);
		
		if (modelMethod != null) {
			modelMethod.methodBlock = getterBody;
			getterModelMethods.put(element, modelMethod);
			return modelMethod;
		}
		
		modelMethod = new ModelMethod(getter, getterBody);
		getterModelMethods.put(element, modelMethod);
		
		return modelMethod;
	}
	
	private AbstractJClass elementModelClass(Element element) {
		String elemType = TypeUtils.typeFromTypeString(element.asType().toString(), environment());
		AbstractJClass elemClass = getJClass(elemType);
		
		Matcher matcher = Pattern.compile("<([A-Za-z_][A-Za-z0-9_.]+)>$").matcher(elemType);
		if (matcher.find()) {
			String innerElem = matcher.group(1);
			
			elemClass = getJClass(elemType.substring(0, elemType.length() - matcher.group(0).length()));
			elemClass = elemClass.narrow(getJClass(innerElem));
		}
		
		return elemClass;
	}
	
	private UseModelHolder useModelHolderForElement(Element element) {
		
		//If CacheFiles is enabled, crossed references cannot be handled
		if (FilesCacheHelper.isCacheFilesEnabled()) return null;
		
		ClassInformation classInformation = TypeUtils.getClassInformation(element, environment(), true);
		ProcessHolder processHolder = environment().getProcessHolder();
		BaseGeneratedClassHolder useModelComponentHolder = 
				(BaseGeneratedClassHolder) processHolder.getGeneratedClassHolder(classInformation.generatorElement);
		
		return useModelComponentHolder.getPluginHolder(new UseModelHolder(useModelComponentHolder));
	}
	
	private String useModelGetModelListMethod(UseModelHolder useModelHolder) {
		if (useModelHolder == null) return UseModelHolder.getModelListName();
		return useModelHolder.getGetModelListMethod().name();
	}
	
	private String useModelLoadModelMethod(UseModelHolder useModelHolder) {
		if (useModelHolder == null) return UseModelHolder.getModelName();
		return useModelHolder.getLoadModelMethod().name();
	}
	
	private String useModelModelInitMethod(UseModelHolder useModelHolder) {
		if (useModelHolder == null) return UseModelHolder.modelInitName();
		return useModelHolder.getModelInitMethod().name();
	}
	
	private String useModelPutModelMethod(UseModelHolder useModelHolder) {
		if (useModelHolder == null) return UseModelHolder.putModelName();
		return useModelHolder.getPutModelMethod().name();
	}
	
	public JMethod getLoadModelMethod(Element element) {
		ModelMethod loadModelRecord = loadModelMethods.get(element);
		if (loadModelRecord == null) {
			loadModelRecord = setLoadModelMethod(element);
		}
			
		return loadModelRecord.method;
	}
	
	public JBlock getAfterLoadModelBlock(Element element) {
		ModelMethod loadModelRecord = loadModelMethods.get(element);
		if (loadModelRecord == null) {
			loadModelRecord = setLoadModelMethod(element);
		}
			
		return loadModelRecord.methodBlock;
	}
	
	private ModelMethod setLoadModelMethod(Element element) {		
		
		final UseModelHolder useModelHolder = useModelHolderForElement(element);
		
		final String fieldName = element.getSimpleName().toString();
		final Model modelAnnotation = adiHelper.getAnnotation(element, Model.class);
		final boolean isStatic = element.getModifiers().contains(Modifier.STATIC);
		final boolean isLazy = modelAnnotation.lazy();
		
		IJAssignmentTarget beanField = null;
		String className = TypeUtils.typeFromTypeString(element.asType().toString(), environment());
		
		IJExpression context = isStatic? ref("context") : holder().getContextRef();
		if (context == _this()) {
			context = holder().getGeneratedClass().staticRef("this");
		}
		
		JMethod loadModelMethod = getGeneratedClass().method(JMod.NONE | (isStatic? JMod.STATIC : 0), getCodeModel().VOID, "_load_" + fieldName);
		if (isStatic) {
			context = loadModelMethod.param(JMod.FINAL, CONTEXT, "context");
		}
		JVar args = loadModelMethod.param(JMod.FINAL, MAP, "args");
		JVar onDone = loadModelMethod.param(JMod.FINAL, getJClass(Runnable.class), "onDone");
		
		JInvocation getter = invoke(getGetterMethod(element));
		if (isStatic && isLazy) {
			getter = getter.arg(context);
		}		
		JInvocation setter = invoke(getSetterMethod(element));
		
		JBlock block = loadModelMethod.body();

		JVar onFailed;
		if (modelAnnotation.handleExceptions()) {
			
			JAnonymousClass anonymousFailed = getCodeModel().anonymousClass(getJClass(OnFailedRunnable.class));
			JMethod handleExceptionMethod = anonymousFailed.method(JMod.PUBLIC, getCodeModel().VOID, "run");
			handleExceptionMethod.annotate(Override.class);
			
			JVar _onFailed = loadModelMethod.param(JMod.FINAL, getJClass(OnFailedRunnable.class), "_onFailed");
			onFailed = block.decl(
				JMod.FINAL, getJClass(OnFailedRunnable.class), "onFailed",
				cond(_onFailed.neNull(), _onFailed, _new(anonymousFailed))
			);
		} else {
			onFailed = loadModelMethod.param(JMod.FINAL, getJClass(OnFailedRunnable.class), "onFailed");
		} 
				
		//Check if this is a list @Model
		boolean isList = TypeUtils.isSubtype(element, CanonicalNameConstants.LIST, environment().getProcessingEnvironment());		
		if (isList) {
			Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9.]+<([a-zA-Z_][a-zA-Z_0-9.]+)>").matcher(className);
			if (matcher.find()) {
				className = matcher.group(1);
			}
			
			beanField = ref(fieldName + "Local");
			
			loadModelMethod.annotate(SuppressWarnings.class).paramArray("value").param("unchecked").param("rawtypes");
		} 
			
		String converted = null;
		if (!className.endsWith(ModelConstants.generationSuffix())) {
			converted = className;
			className = TypeUtils.getGeneratedClassName(className, environment());
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
		final TreePath treePath = trees.getPath(
    			element instanceof VirtualElement? ((VirtualElement)element).getElement() : element
		);
		
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
		
		String getModelInjectionMethod = isList ? useModelGetModelListMethod(useModelHolder)
				                                 : useModelLoadModelMethod(useModelHolder);
		JInvocation getModel = ModelClass.staticInvoke(getModelInjectionMethod)
				  .arg(context)
				  .arg(args)
				  .arg(annotations_invocation);
		
		JBlock assign;
		
		JTryBlock tryBlock;
		if (modelAnnotation.async()) {
			//Use the BackgroundExecutor for asynchronous calls
			JDefinedClass anonymousTaskClass = getCodeModel().anonymousClass(BackgroundExecutor.Task.class);

			JMethod executeMethod = anonymousTaskClass.method(JMod.PUBLIC, getCodeModel().VOID, "execute");
			executeMethod.annotate(Override.class);

			AbstractJClass backgroundExecutorClass = getJClass(BackgroundExecutor.class);
			JInvocation newTask = _new(anonymousTaskClass).arg(lit("")).arg(lit(0)).arg(lit(""));
			JInvocation executeCall = backgroundExecutorClass.staticInvoke("execute").arg(newTask);
			block.add(executeCall);

			tryBlock = executeMethod.body()._try();
		} else {
			tryBlock = block._try();
		}
		
		if (isList) tryBlock.body().decl(LIST.narrow(ModelClass), fieldName + "Local");
		
		assign = tryBlock.body();		
		if (beanField != null) {
			assign.assign(beanField, getModel);
		} else {
			assign.add(setter.arg(getModel));
		}
		
		JCatchBlock catchBlock = tryBlock._catch(THROWABLE);
		JVar caughtException = catchBlock.param("e");
		IJStatement uncaughtExceptionCall = THREAD 
				.staticInvoke("getDefaultUncaughtExceptionHandler") 
				.invoke("uncaughtException") 
				.arg(THREAD.staticInvoke("currentThread")) 
				.arg(caughtException);
		
		JConditional ifOnFailedAssigned = catchBlock.body()._if(onFailed.ne(_null()));
		ifOnFailedAssigned._then().invoke(onFailed, "onFailed").arg(caughtException);
		ifOnFailedAssigned._else().add(uncaughtExceptionCall);
		
		IJExpression assignField = beanField == null? getter : beanField;
		if (isList) {
			
			JBlock forEachBody = tryBlock.body().forEach(getJClass(converted == null ? className : converted), "model", assignField).body();
			forEachBody.invoke(converted == null ? ref("model") 
					                               : cast(ModelClass, ref("model")), useModelModelInitMethod(useModelHolder)).arg(args);			
			
			if (converted != null) {
				assignField = cast(LIST.narrow(getJClass(converted)), cast(LIST, assignField));
			}
			
			JConditional ifCond = assign._if(getter.eq(_null()));
			ifCond._then().add(setter.arg(_new(getJClass(LinkedList.class))));
			
			JSynchronizedBlock syncBlock = tryBlock.body().synchronizedBlock(getter);
			syncBlock.body().add(getter.invoke("clear"));
			syncBlock.body().add(getter.invoke("addAll").arg(assignField));
						
		} else {
			assign.invoke(converted == null ? assignField 
					                          : cast(ModelClass, assignField), useModelModelInitMethod(useModelHolder)).arg(args);
		}
		
		JBlock afterGetModelBlock = assign.blockVirtual();
		
		assign._if(onDone.ne(_null()))._then()
			  .invoke(onDone, "run");

		ModelMethod getModelRecord = new ModelMethod(loadModelMethod, afterGetModelBlock);
		loadModelMethods.put(element, getModelRecord);
		return getModelRecord;
	}
	
	public JMethod getPutModelMethod(Element element) {
		ModelMethod putModelRecord = putModelMethods.get(element);
		if (putModelRecord == null) {
			putModelRecord = setPutModelMethod(element);
		}
			
		return putModelRecord.method;
	}
	
	public JBlock getPutModelMethodBlock(Element element) {
		ModelMethod putModelRecord = putModelMethods.get(element);
		if (putModelRecord == null) {
			putModelRecord = setPutModelMethod(element);
		}
			
		return putModelRecord.methodBlock;
	}
	
	private ModelMethod setPutModelMethod(Element element) {
		
		final UseModelHolder useModelHolder = useModelHolderForElement(element);
		
		final Model modelAnnotation = adiHelper.getAnnotation(element, Model.class);		
		final String fieldName = element.getSimpleName().toString();

		final boolean isStatic = element.getModifiers().contains(Modifier.STATIC);
		final boolean isLazy = modelAnnotation.lazy();
		JInvocation getter = invoke(getGetterMethod(element));
		if (isStatic && isLazy) {
			IJExpression context = holder().getContextRef();
			if (context == _this()) {
				context = holder().getGeneratedClass().staticRef("this");
			}
			getter = getter.arg(context);
		}
		
		IJExpression beanField = getter;
		String className = TypeUtils.typeFromTypeString(element.asType().toString(), environment());
		
		JMethod putModelMethod = getGeneratedClass().method(JMod.NONE, getCodeModel().VOID, "_put_" + fieldName);
		JVar args = putModelMethod.param(JMod.FINAL, MAP, "args");
		JVar onDone = putModelMethod.param(JMod.FINAL, getJClass(Runnable.class), "onDone");
		
		JBlock block = putModelMethod.body();

		JVar onFailed;
		if (modelAnnotation.handleExceptions()) {
			
			JAnonymousClass anonymousFailed = getCodeModel().anonymousClass(getJClass(OnFailedRunnable.class));
			JMethod handleExceptionMethod = anonymousFailed.method(JMod.PUBLIC, getCodeModel().VOID, "run");
			handleExceptionMethod.annotate(Override.class);
			
			JVar _onFailed = putModelMethod.param(JMod.FINAL, getJClass(OnFailedRunnable.class), "_onFailed");
			onFailed = block.decl(
				JMod.FINAL, getJClass(OnFailedRunnable.class), "onFailed",
				cond(_onFailed.neNull(), _onFailed, _new(anonymousFailed))
			);
		} else {
			onFailed = putModelMethod.param(JMod.FINAL, getJClass(OnFailedRunnable.class), "onFailed");
		} 
				
		JAnonymousClass PutModelRunnable = getCodeModel().anonymousClass(getJClass(Runnable.class));
		JMethod run = PutModelRunnable.method(JMod.PUBLIC, VOID, "run");
		PutModelRunnable.annotate(Override.class);
		
		//Populate the _put_ method
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
		if (!className.endsWith(ModelConstants.generationSuffix())) {
			converted = className;
			className = TypeUtils.getGeneratedClassName(className, environment());
		}
		
		AbstractJClass ModelClass = getJClass(className);
		
		//======CODE GENERATION========
		
		JBlock putModel = new JBlock();
		JBlock ifNotPut = putModel._if(
				(converted == null ? beanField : cast(ModelClass, beanField))
				  	.invoke(useModelPutModelMethod(useModelHolder))
				  	.arg(args)
				  	.eq(_null())
			)
			._then();
		
		//Call onFailed if assigned
		IJExpression validationException = 
				_new(getJClass(RuntimeException.class)).arg("Put operation over field \"" + fieldName + "\" failed");
		ifNotPut._if(onFailed.ne(_null()))._then()
		   						 .invoke(onFailed, "onFailed").arg(validationException);
		
		ifNotPut._return();
		
		//Use the BackgroundExecutor for asynchronous calls
		if (modelAnnotation != null) {
			JTryBlock tryBlock;
			if (modelAnnotation.asyncPut()) {
				JDefinedClass anonymousTaskClass = getCodeModel().anonymousClass(BackgroundExecutor.Task.class);

				JMethod executeMethod = anonymousTaskClass.method(JMod.PUBLIC, getCodeModel().VOID, "execute");
				executeMethod.annotate(Override.class);
				
				tryBlock = executeMethod.body()._try();
				
				AbstractJClass backgroundExecutorClass = getJClass(BackgroundExecutor.class);
				JInvocation newTask = _new(anonymousTaskClass).arg(lit("")).arg(lit(0)).arg(lit(""));
				JInvocation executeCall = backgroundExecutorClass.staticInvoke("execute").arg(newTask);
				block.add(executeCall);
			} else {
				tryBlock = block._try();
			}
			
			if (isList) {
				JSynchronizedBlock syncBlock = tryBlock.body().synchronizedBlock(getter);
				JBlock forEachBlock = syncBlock.body().forEach((converted == null ? ModelClass : getJClass(converted)), fieldName + "Local", getter).body();				
				forEachBlock.add(putModel);
			} else {
				tryBlock.body().add(putModel);
			}
			
			tryBlock.body()._if(onDone.ne(_null()))._then()
						   .invoke(onDone, "run");
			
			JCatchBlock catchBlock = tryBlock._catch(THROWABLE);
			JVar caughtException = catchBlock.param("e");
						
			IJStatement uncaughtExceptionCall = THREAD 
					.staticInvoke("getDefaultUncaughtExceptionHandler") 
					.invoke("uncaughtException") 
					.arg(THREAD.staticInvoke("currentThread")) 
					.arg(caughtException);
			
			JConditional ifOnFailedAssigned = catchBlock.body()._if(onFailed.ne(_null()));
			ifOnFailedAssigned._then().invoke(onFailed, "onFailed").arg(caughtException);
			ifOnFailedAssigned._else().add(uncaughtExceptionCall);
		}
		
		ModelMethod putModelRecord = new ModelMethod(putModelMethod, putModelMethodBlock);
		putModelMethods.put(element, putModelRecord);
		return putModelRecord;
	}

	public IJExpression getContextRef() {
		return holder().getContextRef();
	}	
	
	private class ModelMethod {
		JMethod method;
		JBlock methodBlock;
		
		ModelMethod(JMethod method, JBlock methodBlock) {
			this.method = method;
			this.methodBlock = methodBlock;
		}
	}
	
}
