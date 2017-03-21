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
package com.dspot.declex.action;

import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._this;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.Filer;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.androidannotations.helper.APTCodeModelHelper;
import org.androidannotations.helper.IdAnnotationHelper;
import org.androidannotations.internal.InternalAndroidAnnotationsEnvironment;
import org.androidannotations.internal.process.ProcessHolder;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;

import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.api.action.annotation.Assignable;
import com.dspot.declex.api.action.annotation.Field;
import com.dspot.declex.api.action.annotation.FormattedExpression;
import com.dspot.declex.api.action.annotation.Literal;
import com.dspot.declex.api.action.annotation.StopOn;
import com.dspot.declex.api.action.builtin.AlertDialogActionHolder;
import com.dspot.declex.api.action.builtin.AnimateActionHolder;
import com.dspot.declex.api.action.builtin.BackgroundThreadActionHolder;
import com.dspot.declex.api.action.builtin.CallActionHolder;
import com.dspot.declex.api.action.builtin.DateDialogActionHolder;
import com.dspot.declex.api.action.builtin.LoadModelActionHolder;
import com.dspot.declex.api.action.builtin.NotificationActionHolder;
import com.dspot.declex.api.action.builtin.PopulateActionHolder;
import com.dspot.declex.api.action.builtin.ProgressDialogActionHolder;
import com.dspot.declex.api.action.builtin.PutModelActionHolder;
import com.dspot.declex.api.action.builtin.RecollectActionHolder;
import com.dspot.declex.api.action.builtin.TimeDialogActionHolder;
import com.dspot.declex.api.action.builtin.ToastActionHolder;
import com.dspot.declex.api.action.builtin.UIThreadActionHolder;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethod;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.dspot.declex.api.action.process.ActionProcessor;
import com.dspot.declex.override.util.DeclexAPTCodeModelHelper;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

public class Actions {

	protected static final Logger LOGGER = LoggerFactory.getLogger(Actions.class);
	
	private static final String BUILTIN_DIRECT_PKG = "com.dspot.declex.action.builtin.";
	private static final String BUILTIN_PKG = "com.dspot.declex.api.action.builtin.";
	private static final String BUILTIN_PATH = "com/dspot/declex/api/action/builtin/";

	private static Actions instance;
	
	private final Set<String> ACTIONS = new HashSet<>(); 
	private final Set<Class<? extends Annotation>> ACTION_ANNOTATION = new HashSet<>();
	
	private final Map<String, String> ACTION_NAMES = new HashMap<>();
	private final Map<String, ActionInfo> ACTION_INFOS = new HashMap<>();
	
	
	private final List<String> BUILTIN_CLASSES = Arrays.asList(
			AlertDialogActionHolder.class.getSimpleName(),
			ProgressDialogActionHolder.class.getSimpleName(),
			TimeDialogActionHolder.class.getSimpleName(),
			DateDialogActionHolder.class.getSimpleName(),

			AnimateActionHolder.class.getSimpleName(),
			NotificationActionHolder.class.getSimpleName(),
			ToastActionHolder.class.getSimpleName(),

			PutModelActionHolder.class.getSimpleName(),
			LoadModelActionHolder.class.getSimpleName(),
			PopulateActionHolder.class.getSimpleName(),
			RecollectActionHolder.class.getSimpleName(),

			UIThreadActionHolder.class.getSimpleName(),
			BackgroundThreadActionHolder.class.getSimpleName(), 
			
			CallActionHolder.class.getSimpleName()
	);
	
	private InternalAndroidAnnotationsEnvironment env;
	
	private boolean generateInRound = false; 	//Never generate Actions in first round
	
	final IdAnnotationHelper annotationHelper;
	final APTCodeModelHelper codeModelHelper;
	
	public static Actions getInstance() {
		return instance;
	}
	
	public Actions(InternalAndroidAnnotationsEnvironment env) {
		
		this.env = env;
				
		//TODO read builtin classes from within the package and test
		//if adding more classes to this package in external jar, ensures
		//that it is found by the annotation processor, if not, try to create
		//some method (like static code execution in Jars) to ensure that
		//the classes are added to the BUILTIN_CLASSES
		
		//Copy all the built-in Actions
		for (String builtin : BUILTIN_CLASSES) {
			ACTIONS.add(BUILTIN_DIRECT_PKG + builtin);
		}
		
		ACTION_ANNOTATION.add(Assignable.class);
		ACTION_ANNOTATION.add(Field.class);
		ACTION_ANNOTATION.add(FormattedExpression.class);
		ACTION_ANNOTATION.add(Literal.class);
		ACTION_ANNOTATION.add(Assignable.class);
		ACTION_ANNOTATION.add(StopOn.class);
		
		annotationHelper = new IdAnnotationHelper(env, ActionFor.class.getCanonicalName());
		codeModelHelper = new DeclexAPTCodeModelHelper(env);
		
		copyBuiltinActions();
		
		Actions.instance = this;
	}
	
	private void copyBuiltinActions() {
		
		for (String builtin : BUILTIN_CLASSES) {
			try {
				String builtInFileName = BUILTIN_PATH + builtin + ".java";
				
				//Get the file from the package
				URL url = env.getClass().getClassLoader().getResource(builtInFileName);
				if (url == null) {
					throw new IllegalStateException(builtInFileName + " not found, execute ant on the project to generate it");
				}

				final Filer filer = env.getProcessingEnvironment().getFiler();
				JavaFileObject sourceFile = filer.createSourceFile(BUILTIN_DIRECT_PKG + builtin);

				OutputStream out = sourceFile.openOutputStream();
				InputStream in = url.openStream();
				
				int pkgIndex = 0;
				String pkgInit = "package com.dspot.declex.";
				
				int b = in.read();
				while (b != -1) {
					
					if (pkgIndex != -1) {
						if (pkgInit.charAt(pkgIndex) == b) {
							pkgIndex++;
							
							if (pkgIndex >= pkgInit.length()) {
								//Omit 4 chars ("api.")
								in.read();
								in.read();
								in.read();
								in.read();
								
								//Stop the search for the package
								pkgIndex = -1;
							}
						} else {
							pkgIndex = 0;
						}						
					}
					
					out.write(b);						
					b = in.read();
				}
				out.close();
				in.close();
									
			} catch(FileNotFoundException e) {
			} catch (Throwable e) {
			}	
		}
	}
	
	public void addAction(Class<?> action) {
		addAction(action.getCanonicalName());
	}

	public void addAction(String action) {
		ACTIONS.add(action);
		createInformationForAction(action);
	}
		
	public boolean hasActionNamed(String actionName) {
		return ACTION_NAMES.containsKey(actionName);
	}
	
	public Map<String, String> getActionNames() {
		return Collections.unmodifiableMap(ACTION_NAMES);
	} 
	
	public Map<String, ActionInfo> getActionInfos() {
		return Collections.unmodifiableMap(ACTION_INFOS);
	}
	
	public void addAction(String name, String clazz, ActionInfo info) {
		addAction(name, clazz, info, true);
	}
	
	private void addAction(String name, String clazz, ActionInfo info, boolean stopGeneration) {
		if (stopGeneration) {
			this.generateInRound = false;
		}
		
		ACTION_NAMES.put("$" + name, clazz);
		//ACTION_NAMES.put(name, clazz);
		
		ActionInfo prevInfo = ACTION_INFOS.get(clazz);
		if (prevInfo != null) info.setReferences(prevInfo.references);
		
		ACTION_INFOS.put(clazz, info);
	}
	
	private void createInformationForAction(String action) {
		
		TypeElement typeElement = env.getProcessingEnvironment().getElementUtils().getTypeElement(action);
		if (typeElement == null && action.startsWith(BUILTIN_DIRECT_PKG)) {
			typeElement = env.getProcessingEnvironment().getElementUtils().getTypeElement(
					action.replace(BUILTIN_DIRECT_PKG, BUILTIN_PKG)
				);
		}
		
		final ActionFor actionForAnnotation = typeElement.getAnnotation(ActionFor.class);
		
		for (String name : actionForAnnotation.value()) {

			//Get model info
			final ActionInfo actionInfo = new ActionInfo(action);
			actionInfo.isGlobal = actionForAnnotation.global();
			addAction(name, action, actionInfo, false);
			
			String javaDoc = env.getProcessingEnvironment().getElementUtils().getDocComment(typeElement);
			actionInfo.setReferences(javaDoc);
			
			List<DeclaredType> processors = annotationHelper.extractAnnotationClassArrayParameter(
					typeElement, ActionFor.class.getCanonicalName(), "processors"
				);
			
			if (processors != null) {
				for (DeclaredType processor : processors) {
					try {
						actionInfo.processors.add(
								(ActionProcessor) Class.forName(processor.toString()).newInstance()
							);
					} catch (Exception e) {
						TypeElement element = env.getProcessingEnvironment().getElementUtils().getTypeElement(processor.toString());
						if (element == null) {
							LOGGER.info("Processor \"" + processor.toString() + "\" coudn't be loaded, it is not in the building path", typeElement);							
						} else {
							try {
								ActionProcessor processorInstance = (ActionProcessor) compileAndLoadClass(element);
								if (processorInstance == null) {
									LOGGER.info("Processor \"" + processor.toString() + "\" coudn't be loaded", typeElement);
								}
							} catch (ClassNotFoundException | MalformedURLException | InstantiationException | IllegalAccessException e1) {
								LOGGER.info("Processor \"" + processor.toString() + "\" coudn't be loaded by the ClassLoader", typeElement);							
							} catch (IOException e1) {
								LOGGER.info("Processor \"" + processor.toString() + "\" coudn't be loaded, IOException", typeElement);
							}
							
						}
						
					}
				}
			}
									
			for (Element elem : typeElement.getEnclosedElements()) {
				
				if (elem.getKind() == ElementKind.METHOD) {
					final ExecutableElement element = (ExecutableElement) elem;
					
					List<ActionMethodParam> params = new LinkedList<>();
					for (VariableElement param : element.getParameters()) {
						
						List<Annotation> annotations = new LinkedList<>();
						for (Class<? extends Annotation> annotation : ACTION_ANNOTATION) {
							Annotation containedAnnotation = param.getAnnotation(annotation);
							if (containedAnnotation != null) {
								annotations.add(containedAnnotation);
							}
						}
						
						//Use direct package for builtin classes
						AbstractJClass paramType = codeModelHelper.typeMirrorToJClass(param.asType());
						String clazz = param.asType().toString();
						if (clazz.startsWith(BUILTIN_PKG)) {
							clazz = clazz.replace(BUILTIN_PKG, BUILTIN_DIRECT_PKG);
							paramType = env.getJClass(clazz);
						}
						
						ActionMethodParam actionMethodParam = 
								new ActionMethodParam(
										param.getSimpleName().toString(), 
										paramType,
										annotations
									);
						actionMethodParam.internal = param;
						params.add(actionMethodParam);
					}
					
					List<Annotation> annotations = new LinkedList<>();
					for (Class<? extends Annotation> annotation : ACTION_ANNOTATION) {
						Annotation containedAnnotation = element.getAnnotation(annotation);
						if (containedAnnotation != null) {
							annotations.add(containedAnnotation);
						}
					}
					
					javaDoc = env.getProcessingEnvironment().getElementUtils().getDocComment(element);	
					
					//Use direct package for builtin classes
					String clazz = element.getReturnType().toString();
					if (clazz.startsWith(BUILTIN_PKG)) {
						clazz = clazz.replace(BUILTIN_PKG, BUILTIN_DIRECT_PKG);
					}
					
					actionInfo.addMethod(
							element.getSimpleName().toString(),
							clazz, 								 
							javaDoc,
							params, 
							annotations
						);
				}
			}
		}		
	}
	
	private Object compileAndLoadClass(TypeElement element) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
		final Trees trees = Trees.instance(env.getProcessingEnvironment());
    	final TreePath treePath = trees.getPath(element);    	
		
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        
        List<String> optionList = new ArrayList<String>();
        optionList.add("-classpath");
        optionList.add(System.getProperty("java.class.path"));
        
        System.out.println("DD: " + System.getProperty("java.class.path"));
        
        JavaCompiler.CompilationTask task = compiler.getTask(
            null, 
            fileManager, 
            diagnostics, 
            optionList, 
            null, 
            Arrays.asList(treePath.getCompilationUnit().getSourceFile()));

        if (task.call()) {
            /** Load *************************************************************************************************/
            System.out.println("Yipe");
            // Create a new custom class loader, pointing to the directory that contains the compiled
            // classes, this should point to the top of the package structure!
            URLClassLoader classLoader = new URLClassLoader(new URL[]{new File("./").toURI().toURL()});
            // Load the class from the classloader by name....
            Class<?> loadedClass = classLoader.loadClass(element.asType().toString());
            // Create a new instance...
            return loadedClass.newInstance();
            /************************************************************************************************* Load and execute **/
        } else {
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            	LOGGER.info("Error compiling processor file {}\n{} ", element, 
		        			String.format("Error on line %d in %s%n",
		                    diagnostic.getLineNumber(),
		                    diagnostic.getSource().toUri())
                        );
            	
            	System.out.format("Error on line %d in %s%n",
                        diagnostic.getLineNumber(),
                        diagnostic.getSource().toUri());
            }
        }
        
        fileManager.close();

        return null;
	}

	public void getActionsInformation() {
		
		//This will ensure a correct working-flow for Actions Processing	
		if (env.getProcessHolder() == null) {
			ProcessHolder processHolder = new ProcessHolder(env.getProcessingEnvironment());
			env.setProcessHolder(processHolder);
		}
		
		for (String action : ACTIONS) {
			createInformationForAction(action);
		}
	}
	
	public void buildActionsObject() {
		
		if (!generateInRound) {
			generateInRound = true;
			return;
		}
		
		try {
			//It is important to update the Action information again when generating
			getActionsInformation();
			
			JDefinedClass Action = env.getCodeModel()._getClass(DeclexConstant.ACTION);
			if (Action == null) {
				Action = env.getCodeModel()._class(DeclexConstant.ACTION);
				
				for (String name : ACTION_NAMES.keySet()) {
					
					final String action = ACTION_NAMES.get(name);
					final ActionInfo actionInfo = ACTION_INFOS.get(action);
					
					//This will avoid generation for parent classes, not used in the project
					if (!actionInfo.generated) continue;
					
					List<ActionMethod> builds = actionInfo.methods.get("build");
					if (builds != null && builds.size() > 0) {
						ActionMethod build = builds.get(0);
						
						JDefinedClass ActionGate = Action._class(JMod.PUBLIC | JMod.STATIC, name);
						ActionGate._extends(env.getJClass(actionInfo.holderClass));
						
						if (actionInfo.references != null) {
							ActionGate.javadoc().add(actionInfo.references);
						}
						
						//Create all the events for the action
						for (ActionMethodParam param : build.params) {
							JFieldVar field = ActionGate.field(
									JMod.PUBLIC | JMod.STATIC, 
									env.getCodeModel().BOOLEAN, 
									param.name,
									JExpr.TRUE
								);
							
							JFieldVar refField = ActionGate.field(
									JMod.PROTECTED | JMod.STATIC, 
									param.clazz, 
									"$" + param.name
								);
							
							JMethod method = ActionGate.method(
									JMod.PUBLIC | JMod.STATIC, 
									param.clazz, param.name
								);
							method.body()._return(refField);
							
							if (build.javaDoc != null) {
								Matcher matcher = 
										Pattern.compile(
												"\\s+@param\\s+" + param.name + "\\s+((?:[^@]|(?<=\\{)@[^}]+\\})+)"
										).matcher(build.javaDoc);
								
								if (matcher.find()) {
									field.javadoc().add("<br><hr><br>\n" + matcher.group(1).trim());
								}
							}
						}	
						
						//Create the init methods for the action
						List<ActionMethod> inits = actionInfo.methods.get("init");
						if (inits != null) {
							for (ActionMethod actionMethod : inits) {
								JMethod method = Action.method(
										JMod.STATIC | JMod.PUBLIC, ActionGate, name
									);
								
								if (actionInfo.references != null) {
									method.javadoc().add("<br><hr><br>\n" + actionInfo.references.trim());
								}
								
								if (actionMethod.javaDoc != null) {
									method.javadoc().add("\n" + actionMethod.javaDoc.trim());
								}
								
								for (ActionMethodParam param : actionMethod.params) {
									method.param(param.clazz, param.name);
								}

								method.body()._return(_new(ActionGate));
							
							}
						}
						
						//All the methods of the Action Holder that returns the Holder itself,  
						//are inserted in order to use this ActionGate from the fields
						for (Entry<String, List<ActionMethod>> entry : actionInfo.methods.entrySet()) {
							for (ActionMethod actionMethod : entry.getValue()) {
								
								List<String> specials = Arrays.asList("init", "build", "execute");
								
								if (actionMethod.resultClass.equals(actionInfo.holderClass) 
									|| (actionMethod.resultClass.equals("void") && !specials.contains(actionMethod.name))) {
																		
									JMethod method;
									if (actionMethod.resultClass.equals("void")) {
										
										method = ActionGate.method(JMod.PUBLIC, env.getCodeModel().VOID, actionMethod.name);
									} else {
										method = ActionGate.method(JMod.PUBLIC, ActionGate, actionMethod.name);
										method.body()._return(_this());
									}
									method.annotate(Override.class);
									
									for (ActionMethodParam param : actionMethod.params) {
										JVar paramVar = method.param(param.clazz, param.name);	
										
										if (param.internal instanceof VariableElement) {
											VariableElement element = (VariableElement) param.internal;
											
											for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
												TypeUtils.annotateVar(paramVar, annotationMirror, env);
											} 
										}
									}
									
									if (actionMethod.javaDoc != null) {
										method.javadoc().add("<br><hr><br>\n" + actionMethod.javaDoc.trim());
									}
									
								}
							}
						}
						
						ActionGate.method(JMod.PUBLIC, env.getCodeModel().VOID, "fire");
					}
					
				}
			}			
			
		} catch (Exception e) {
			e.printStackTrace();
		}				
	}
}
