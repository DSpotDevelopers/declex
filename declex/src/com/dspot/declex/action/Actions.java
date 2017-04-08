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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
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
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethod;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.dspot.declex.api.action.process.ActionProcessor;
import com.dspot.declex.helper.FilesCacheHelper;
import com.dspot.declex.helper.FilesCacheHelper.FileDetails;
import com.dspot.declex.override.util.DeclexAPTCodeModelHelper;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.FileUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

public class Actions {

	protected static final Logger LOGGER = LoggerFactory.getLogger(Actions.class);
	
	public static final String BUILTIN_DIRECT_PKG = "com.dspot.declex.action.builtin.";
	public static final String BUILTIN_PKG = "com.dspot.declex.api.action.builtin.";
	private static final String BUILTIN_PATH = "com/dspot/declex/api/action/builtin/";
	private boolean builtin_copied = false;

	private static Actions instance;
	
	private final Set<String> ACTION_HOLDERS = new HashSet<>(); 
	final static Set<Class<? extends Annotation>> ACTION_ANNOTATION = new HashSet<>();
	
	private final Map<String, String> ACTION_NAMES = new HashMap<>();
	private final Map<String, ActionInfo> ACTION_INFOS = new HashMap<>();
	
	private InternalAndroidAnnotationsEnvironment env;
	
	private boolean generateInRound = false; 	//Never generate Actions in first round
	
	final IdAnnotationHelper annotationHelper;
	final APTCodeModelHelper codeModelHelper;
	
	private static Long lastBuiltInLibModified;
	
	public static Actions getInstance() {
		return instance;
	}
	
	public Actions(InternalAndroidAnnotationsEnvironment env) {
		
		this.env = env;
		
		ACTION_ANNOTATION.add(Assignable.class);
		ACTION_ANNOTATION.add(Field.class);
		ACTION_ANNOTATION.add(FormattedExpression.class);
		ACTION_ANNOTATION.add(Literal.class);
		ACTION_ANNOTATION.add(Assignable.class);
		ACTION_ANNOTATION.add(StopOn.class);
		
		annotationHelper = new IdAnnotationHelper(env, ActionFor.class.getCanonicalName());
		codeModelHelper = new DeclexAPTCodeModelHelper(env);		
		
		Actions.instance = this;
	}
	
	public static Set<String> getClassNamesFromBuiltInPackage() {
		
		Set<String> names = new HashSet<>();
		
		try {
		    ClassLoader classLoader = Actions.class.getClassLoader();
		    URL packageURL = classLoader.getResource(BUILTIN_PATH);		    

		    if(packageURL.getProtocol().equals("jar")){

		    	// build jar file name, then loop through zipped entries
		        String jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
		        jarFileName = jarFileName.substring(5,jarFileName.indexOf("!"));
		        JarFile jf = new JarFile(jarFileName);
		        Enumeration<JarEntry> jarEntries = jf.entries();
		        
		        File jarFile = new File(jarFileName);
		        lastBuiltInLibModified = jarFile.lastModified();		        
		        
		        while(jarEntries.hasMoreElements()){
		        	String entryName = jarEntries.nextElement().getName();
		            if(entryName.startsWith(BUILTIN_PATH) && entryName.length() > BUILTIN_PATH.length() + 5){
		                entryName = entryName.substring(BUILTIN_PATH.length(), entryName.lastIndexOf('.'));
		                if (!entryName.contains("$") && !entryName.contains("/")) { 
		                	names.add(entryName);
		                }
		            }
		        }
		        
		        jf.close();
		    
		    } else {
			    URI uri = new URI(packageURL.toString());
			    File folder = new File(uri.getPath());
			    
		        // won't work with path which contains blank (%20)
		        // File folder = new File(packageURL.getFile()); 
		        File[] contenuti = folder.listFiles();
		        String entryName;
		        for(File actual: contenuti){
		            entryName = actual.getName();
		            entryName = entryName.substring(0, entryName.lastIndexOf('.'));
		            if (!entryName.contains("$") && !entryName.contains("/")) {
		            	names.add(entryName);
		            }
		        }
		    }	
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	    return names;
	}
	
	private void copyBuiltinActions(Set<String> builtinClasses) {
		
		for (String builtin : builtinClasses) {
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
						
				if (FilesCacheHelper.isCacheFilesEnabled()) {
					copyFileToCache(BUILTIN_DIRECT_PKG + builtin, sourceFile);
				}
				
			} catch(FileNotFoundException e) {
			} catch (Throwable e) {
			}	
		}
	}
	
	private void copyFileToCache(String className, FileObject sourceFile) {
		// Cache the closed file
		URI fileUri = sourceFile.toUri();
		
		//Get unique name for cached file
		File externalCacheFolder = FileUtils.getPersistenceConfigFile("cache");
		
		final String pkg = className.substring(0, className.lastIndexOf('.'));
		final String java = className.substring(className.lastIndexOf('.') + 1) + ".java";
		File cachedFolder = new File(
				externalCacheFolder.getAbsolutePath() + File.separator
				+ "classes" + File.separator + pkg.replace('.', File.separatorChar)
			);
		cachedFolder.mkdirs();
			
		File externalCachedFile = new File(
			cachedFolder.getAbsolutePath() + File.separator + java
		);
		
		FileUtils.copyCompletely(fileUri, externalCachedFile);
		
		FilesCacheHelper.getInstance().addGeneratedClass(className, null);
		FileDetails details = FilesCacheHelper.getInstance().getFileDetails(className);
		details.setGeneratedJavaCache(externalCachedFile.getAbsolutePath(), Paths.get(fileUri).toString());
		details.metaData.put("lastBuiltInLibModified", lastBuiltInLibModified);
	}

	public void addActionHolder(String action) {
		ACTION_HOLDERS.add(action);
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
			actionInfo.isTimeConsuming = actionForAnnotation.timeConsuming();
			addAction(name, action, actionInfo, false);
			
			String javaDoc = env.getProcessingEnvironment().getElementUtils().getDocComment(typeElement);
			actionInfo.setReferences(javaDoc);
			
			List<DeclaredType> processors = annotationHelper.extractAnnotationClassArrayParameter(
					typeElement, ActionFor.class.getCanonicalName(), "processors"
				);
			
			//Load processors
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
									
			createInformationForMethods(typeElement, actionInfo);
		}		
	}
	
	public void createInformationForMethods(Element typeElement, ActionInfo actionInfo) {
		this.createInformationForMethods(typeElement, actionInfo, null);
	}
	
	public void createInformationForMethods(Element typeElement, ActionInfo actionInfo, 
			 List<String> methodsHandled) {

		if (methodsHandled == null) {
			methodsHandled = new LinkedList<>();
		}
		
		for (Element elem : typeElement.getEnclosedElements()) {
			
			if (elem.getKind() == ElementKind.METHOD) {
				if (methodsHandled.contains(elem.toString())) continue;
				
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
				
				String javaDoc = env.getProcessingEnvironment().getElementUtils().getDocComment(element);	
				
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
				
				methodsHandled.add(element.toString());
			}
		}
		
		List<? extends TypeMirror> superTypes = env.getProcessingEnvironment().getTypeUtils().directSupertypes(typeElement.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = env.getProcessingEnvironment().getElementUtils().getTypeElement(type.toString());
			if (superElement == null) continue;
			if (superElement.getKind().equals(ElementKind.INTERFACE)) continue;
			if (superElement.asType().toString().equals(Object.class.getCanonicalName())) continue;
			createInformationForMethods(superElement, actionInfo, methodsHandled);
		}
		
	}
	
	private Object compileAndLoadClass(TypeElement element) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
		final Trees trees = Trees.instance(env.getProcessingEnvironment());
    	final TreePath treePath = trees.getPath(element);    	
		
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        
        //TODO this doesn't work, the classpath return is the one of gradle, and not 
        //the one used for DecleX annotation processor
        List<String> optionList = new ArrayList<String>();
        optionList.add("-classpath");
        optionList.add(System.getProperty("java.class.path"));
        
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
		
		if (!builtin_copied) {
			//TODO read builtin classes from within the package and test
			//if adding more classes to this package in external jar, ensures
			//that it is found by the annotation processor, if not, try to create
			//some method (like static code execution in Jars) to ensure that
			//the classes are added to the BUILTIN_CLASSES
			
			Set<String> builtinClasses = getClassNamesFromBuiltInPackage();
			Set<String> builtinClassesNotCached = new HashSet<>();
			
			LOGGER.debug("Bultin Actions Package: " + builtinClasses);
			
			//Remove builtin that are cached
			for (String builtin : builtinClasses) {
				if (!FilesCacheHelper.getInstance().hasCachedFile(BUILTIN_DIRECT_PKG + builtin)) {
					builtinClassesNotCached.add(builtin);
				} else {
					FileDetails details = FilesCacheHelper.getInstance().getFileDetails(BUILTIN_DIRECT_PKG + builtin);
					Long cachedLastBuiltinModified = (Long) details.metaData.get("lastBuiltInLibModified");
					if (!cachedLastBuiltinModified.equals(lastBuiltInLibModified)) {
						LOGGER.debug("Removing Cached Action: " + builtin);
						details.invalidate();
						builtinClassesNotCached.add(builtin);
					} else {					
						LOGGER.debug("Cached bultin Action: " + builtin);
					}
				}
			}
			
			copyBuiltinActions(builtinClassesNotCached);
			
			//Copy all the built-in Actions
			for (String builtin : builtinClassesNotCached) {
				TypeElement typeElement = env.getProcessingEnvironment().getElementUtils().getTypeElement(BUILTIN_PKG + builtin);
				if (typeElement != null && typeElement.getAnnotation(ActionFor.class) != null) {
					LOGGER.debug("Adding bultin Action: " + builtin);
					ACTION_HOLDERS.add(BUILTIN_DIRECT_PKG + builtin);
				}
			}
			
			builtin_copied = true;
		}
		
		//This will ensure a correct working-flow for Actions Processing	
		if (env.getProcessHolder() == null) {
			ProcessHolder processHolder = new ProcessHolder(env.getProcessingEnvironment());
			env.setProcessHolder(processHolder);
		}
		
		for (String action : ACTION_HOLDERS) {
			createInformationForAction(action);
		}
	}
	
	public boolean buildActionsObject() {
		
		if (!generateInRound) {
			generateInRound = true;
			return false;
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
						
						final String pkg = actionInfo.holderClass.substring(0, actionInfo.holderClass.lastIndexOf('.'));
						JDefinedClass ActionGate = Action._class(JMod.PUBLIC | JMod.STATIC, name);	
						ActionGate._extends(env.getJClass(pkg + "." + name.substring(1) + "Gate"));
						
						if (actionInfo.references != null) {
							ActionGate.javadoc().add(actionInfo.references);
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
					}
					
				}
			}			
			
			return true;
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}				
	}
}
