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

import com.dspot.declex.annotation.action.*;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethod;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.dspot.declex.api.action.process.ActionProcessor;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import org.androidannotations.Option;
import org.androidannotations.api.view.HasViews;
import org.androidannotations.helper.APTCodeModelHelper;
import org.androidannotations.helper.CompilationTreeHelper;
import org.androidannotations.helper.IdAnnotationHelper;
import org.androidannotations.internal.InternalAndroidAnnotationsEnvironment;
import org.androidannotations.internal.process.ProcessHolder;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.Map.Entry;

import static com.helger.jcodemodel.JExpr._new;

public class Actions {

	public static final Option OPTION_DEBUG_ACTIONS = new Option("debugActions", "false");
	
	protected static final Logger LOGGER = LoggerFactory.getLogger(Actions.class);
	
	private static Actions instance;

	private final Map<String, ClassLoader> classLoaderForProcessor = new HashMap<>();
	
	//<Action Holder Class, Is Exported (Declared in a library)
	private final Map<String, Boolean> ACTION_HOLDERS = new HashMap<>();
	private final Map<String, TypeElement> ACTION_HOLDER_ELEMENT_FOR_ACTION = new HashMap<>();
	
	public final static Set<Class<? extends Annotation>> ACTION_ANNOTATION = new HashSet<>();
	private final Set<String> EXTERNAL_ACTIONS = new HashSet<>();
	
	private final Map<String, String> ACTION_NAMES = new HashMap<>();
	private final Map<String, ActionInfo> ACTION_INFOS = new HashMap<>();
	
	private InternalAndroidAnnotationsEnvironment env;
	
	private boolean generateInRound = true;
	
	private final IdAnnotationHelper annotationHelper;
	private final APTCodeModelHelper codeModelHelper;

	public static Actions getInstance() {
		return instance;
	}

	public Actions(InternalAndroidAnnotationsEnvironment env) {
		
		this.env = env;
		
		ACTION_ANNOTATION.add(Assignable.class);
		ACTION_ANNOTATION.add(Field.class);
		ACTION_ANNOTATION.add(FormattedExpression.class);
		ACTION_ANNOTATION.add(Literal.class);
		ACTION_ANNOTATION.add(StopOn.class);
		
		annotationHelper = new IdAnnotationHelper(env, ActionFor.class.getCanonicalName());
		codeModelHelper = new APTCodeModelHelper(env);

        Actions.instance = this;
	}

    public boolean isAction(String name) {

        String actionToCheck = name.substring(0, name.lastIndexOf('.'));
        if (actionToCheck.isEmpty() || !actionToCheck.endsWith(".Action")) return false;

        if (actionToCheck.equals(DeclexConstant.ACTION)) return true;

        TypeElement element = env.getProcessingEnvironment().getElementUtils().getTypeElement(actionToCheck);
        if (element == null) return false;

        boolean isAction = element.getAnnotation(com.dspot.declex.annotation.action.Actions.class) != null;
        if (isAction) {
            //If it is an Actions container object, add it as an export Action
            addActions(actionToCheck);
        }

        return isAction;

    }

    private void addActions(String actions) {
		
		if (!EXTERNAL_ACTIONS.contains(actions)) {
			TypeElement typeElement = env.getProcessingEnvironment().getElementUtils().getTypeElement(actions);
			
			CLASSES: for (Element element : typeElement.getEnclosedElements()) {
				
				if (element.getKind().isClass()) {
					
					List<? extends TypeMirror> superTypesForGate = env.getProcessingEnvironment().getTypeUtils().directSupertypes(element.asType());
					for (TypeMirror gate : superTypesForGate) {
						TypeElement superElementForGate = env.getProcessingEnvironment().getElementUtils().getTypeElement(gate.toString());
						if (superElementForGate == null) continue;
						if (superElementForGate.getKind().equals(ElementKind.INTERFACE)) continue;
						if (superElementForGate.asType().toString().equals(Object.class.getCanonicalName())) continue;
						
						//This is the Gate element, its parent it is the Holder
						List<? extends TypeMirror> superTypesForHolder = env.getProcessingEnvironment().getTypeUtils().directSupertypes(superElementForGate.asType());
						for (TypeMirror holder : superTypesForHolder) {
							TypeElement superElementForHolder = env.getProcessingEnvironment().getElementUtils().getTypeElement(holder.toString());
							if (superElementForHolder == null) continue;
							if (superElementForHolder.getKind().equals(ElementKind.INTERFACE)) continue;
							if (superElementForHolder.asType().toString().equals(Object.class.getCanonicalName())) continue;
							
							addActionHolder(superElementForHolder.asType().toString(), true);
							
							//This is the Holder element
							continue CLASSES;
						}
					}
					
				}
				
			}
			
			EXTERNAL_ACTIONS.add(actions);
		}
		
	}

	public void addActionHolder(String action) {
		this.addActionHolder(action, false);
	}
	
	private void addActionHolder(String actionHolder, boolean isExternal) {
		ACTION_HOLDERS.put(actionHolder, isExternal);
		createInformationForAction(actionHolder, isExternal);
	}
	
	public TypeElement getActionHolderForAction(String action) {
		return ACTION_HOLDER_ELEMENT_FOR_ACTION.get(action);		
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
		
		ActionInfo prevInfo = ACTION_INFOS.get(clazz);
		if (prevInfo != null) info.setReferences(prevInfo.references);
		
		ACTION_INFOS.put(clazz, info);
	}
	
	private void createInformationForAction(String actionHolder, boolean isExternal) {
		
		TypeElement typeElement = env.getProcessingEnvironment().getElementUtils().getTypeElement(actionHolder);
		TypeElement generatedHolder = env.getProcessingEnvironment().getElementUtils().getTypeElement(
				TypeUtils.getGeneratedClassName(typeElement, env)
			);

		ActionFor actionForAnnotation = null;
		try{
		    actionForAnnotation = typeElement.getAnnotation(ActionFor.class);
        } catch (Exception e) {
		    LOGGER.error("An error occurred processing the @ActionFor annotation", e);
        }

        if (actionForAnnotation != null) {

            for (String name : actionForAnnotation.value()) {

                ACTION_HOLDER_ELEMENT_FOR_ACTION.put("$" + name, typeElement);

                //Get model info
                final ActionInfo actionInfo = new ActionInfo(actionHolder);
                actionInfo.isGlobal = actionForAnnotation.global();
                actionInfo.isTimeConsuming = actionForAnnotation.timeConsuming();

                if (isExternal) {
                    actionInfo.generated = false;
                }

                //This will work only for cached classes
                if (generatedHolder != null) {
                    for (Element elem : generatedHolder.getEnclosedElements()) {
                        if (elem instanceof ExecutableElement) {
                            final String elemName = elem.getSimpleName().toString();
                            final List<? extends VariableElement> params = ((ExecutableElement) elem).getParameters();

                            if (elemName.equals("onViewChanged") && params.size() == 1
                                    && params.get(0).asType().toString().equals(HasViews.class.getCanonicalName())) {
                                actionInfo.handleViewChanges = true;
                                break;
                            }
                        }
                    }
                }

                addAction(name, actionHolder, actionInfo, false);

                String javaDoc = env.getProcessingEnvironment().getElementUtils().getDocComment(typeElement);
                actionInfo.setReferences(javaDoc);

                List<DeclaredType> processors = annotationHelper.extractAnnotationClassArrayParameter(
                        typeElement, ActionFor.class.getCanonicalName(), "processors"
                );

                //Load processors
                if (processors != null) {
                    for (DeclaredType processor : processors) {

                        Class<ActionProcessor> processorClass = null;

                        try {

                            ClassLoader loader = classLoaderForProcessor.get(processor.toString());
                            if (loader != null) {
                                processorClass = (Class<ActionProcessor>) Class.forName(processor.toString(), true, loader);
                            } else {
                                processorClass = (Class<ActionProcessor>) Class.forName(processor.toString());
                            }

                        } catch (ClassNotFoundException e) {

                            Element element =  env.getProcessingEnvironment().getElementUtils().getTypeElement(processor.toString());
							if (element == null) {
								LOGGER.error("Processor \"" + processor.toString() + "\" couldn't be loaded", typeElement);
							} else {

							    try {
							        //Get the file from which the class was loaded
                                    java.lang.reflect.Field field = element.getClass().getField("classfile");
                                    field.setAccessible(true);
                                    JavaFileObject classfile = (JavaFileObject) field.get(element);

                                    String jarUrl = classfile.toUri().toURL().toString();
                                    jarUrl = jarUrl.substring(0, jarUrl.lastIndexOf('!') + 2);

                                    //Create or use a previous created class loader for the given file
                                    ClassLoader loader;
                                    if (classLoaderForProcessor.containsKey(jarUrl)) {
                                        loader = classLoaderForProcessor.get(jarUrl);
                                    } else {
                                        loader = new URLClassLoader(new URL[]{new URL(jarUrl)}, Actions.class.getClassLoader());
                                        classLoaderForProcessor.put(processor.toString(), loader);
                                        classLoaderForProcessor.put(jarUrl, loader);
                                    }

                                    processorClass = (Class<ActionProcessor>) Class.forName(processor.toString(), true, loader);

                                } catch (Throwable e1) {
                                    LOGGER.error("Processor \"" + processor.toString() + "\" couldn't be loaded: " + e1.getMessage(), typeElement);
                                }

							}

                        } catch (ClassCastException e) {
                            LOGGER.error("Processor \"" + processor.toString() + "\" is not an Action Processor", typeElement);
                        }

                        if (processorClass != null) {
                            try {
                                actionInfo.processors.add(processorClass.newInstance());
                            } catch (Throwable e) {
                                LOGGER.info("Processor \"" + processor.toString() + "\" couldn't be instantiated", typeElement);
                            }
                        }

                    }
                }

                createInformationForMethods(typeElement, actionInfo);
            }

        }

	}
	
	public void createInformationForMethods(Element typeElement, ActionInfo actionInfo) {
		this.createInformationForMethods(typeElement, actionInfo, null);
	}
	
	private void createInformationForMethods(Element typeElement, ActionInfo actionInfo, List<String> methodsHandled) {

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
					
					final AbstractJClass paramType = codeModelHelper.elementTypeToJClass(param);
					
					ActionMethodParam actionMethodParam = 
							new ActionMethodParam(
									param.getSimpleName().toString(), 
									paramType,
									annotations
								);
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
				
				final String clazz = element.getReturnType().toString();
				
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
	
	public void getActionsInformation() {
				
		//This will ensure a correct working-flow for Actions Processing	
		if (env.getProcessHolder() == null) {
			ProcessHolder processHolder = new ProcessHolder(env.getProcessingEnvironment());
			env.setProcessHolder(processHolder);
		}
		
		for (Entry<String, Boolean> holder: ACTION_HOLDERS.entrySet()) {
			createInformationForAction(holder.getKey(), holder.getValue());
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
				Action.annotate(com.dspot.declex.annotation.action.Actions.class);
				
				for (String name : ACTION_NAMES.keySet()) {
					
					final String action = ACTION_NAMES.get(name);
					final ActionInfo actionInfo = ACTION_INFOS.get(action);					
										
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
