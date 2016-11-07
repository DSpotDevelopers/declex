/**
 * Copyright (C) 2016 DSpot Sp. z o.o
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
package com.dspot.declex.action.sequence;

import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr.direct;
import static com.helger.jcodemodel.JExpr.ref;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.helper.TargetAnnotationHelper;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.internal.model.AnnotationElements;

import com.dspot.declex.action.ActionPlugin;
import com.dspot.declex.api.action.Action;
import com.dspot.declex.util.SharedRecords;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;

public abstract class BaseListActionHandler implements ActionPlugin {
	
	protected IJStatement statement;
	
	private TargetAnnotationHelper annotationHelper;
	
	protected AndroidAnnotationsEnvironment environment;
	protected AnnotationElements validatedModel;
	protected ProcessingEnvironment processingEnv;
	
	private List<ActionPlugin> plugins;
	
	private List<IJStatement> statements = new LinkedList<>(); 
	private int innerClassCounter = 0;

	public BaseListActionHandler(List<ActionPlugin> plugins) {
		this.plugins = plugins;
	}
	
	protected abstract Class<?> getBaseClass();
	
	@Override
	public boolean canProcessElement(Element element, AndroidAnnotationsEnvironment environment) {
		this.validatedModel = environment.getValidatedElements();
		this.processingEnv = environment.getProcessingEnvironment();
		this.environment = environment;
		
		if (this.annotationHelper == null) {
			annotationHelper = new TargetAnnotationHelper(environment, Action.class.getCanonicalName());
		}
		
		String elementClass = element.asType().toString();
		
		if (elementClass.endsWith("<>")) return false;
		
		try {
			Class<?> clazz = getClass().getClassLoader().loadClass(
					elementClass.substring(0, elementClass.indexOf('<'))
				);
			clazz.asSubclass(getBaseClass());
			
			return true;
		} catch (Exception e) {
		};
		
		return false;
	}

	private List<String> getElementClasses(String elementClass) {
		return this.divideByComma(elementClass, '<', '>');
	}
	
	private List<String> divideByComma(String value, char div1, char div2) {
		List<String> classesList = new LinkedList<>();
		value = value.substring(value.indexOf(div1)+1, value.length()-1);
		
		int insideDiv = 0;
		int prevComma = 0;
		for (int i = 0; i < value.length(); i++) {
			if (value.charAt(i) == div1) insideDiv++;
			if (value.charAt(i) == div2) insideDiv--;
			
			if (value.charAt(i) == ',' && insideDiv==0) {
				classesList.add(value.substring(prevComma, i).trim());
				prevComma = i+1;
			}
		}
		
		classesList.add(value.substring(prevComma).trim());
		
		return classesList;
	}
	
	private String[] getParameters(String paramStr) {
		
		List<String> parameters = new LinkedList<>();
		
		boolean insideDiv = false;
		String value = "";
		String[] split = paramStr.split("(?:(\\s+\\$(?!\\s))|(\\s*->\\s*))");
		
		for (int i = 0; i < split.length; i++) {
			
			if (split[i].startsWith("(")) insideDiv = true;
			
			if (insideDiv) {
				if (value.equals("")) value = split[i];
				else value = value + "->" + split[i];

				
				if (split[i].endsWith(")")) {
					
					//Count the division opening and closes, if it's totally closed, then finish the division
					int countDivider = 0;
					for (int j = 0; j < value.length(); j++) {
						if (value.charAt(j) == '(') countDivider++;
						if (value.charAt(j) == ')') countDivider--;
					}
					
					if (countDivider == 0) {
						parameters.add(value);
						value = "";
						insideDiv = false;
					}
				}
			} else {
				parameters.add(split[i]);
			}
		}

		return parameters.toArray(new String[parameters.size()]);
	}
	
	@Override
	public void validate(String[] parameters, Element element, ElementValidation valid) {
		String elementClass = element.asType().toString();
		List<String> classesList = getElementClasses(elementClass);
		
		//Make a list of instructions for this sequence and instructions for all the Sequenced Parameters
		List<String> instSequencedParameters = new LinkedList<String>(Arrays.asList(parameters));
		int i = 0;
		while (i < instSequencedParameters.size()) {
			final String paramValue = instSequencedParameters.get(i);  
			if (paramValue.startsWith("#")) {
				instSequencedParameters.remove(i);
				continue;
			}
			
			//This is used to indicated a subsequence inside a sequence
			if (paramValue.equals("@")) {
				instSequencedParameters.remove(i);
				continue;
			}

			i++;
		}
				
		int index = 0;
		for (String clazz : classesList) {
			String elemClass = clazz;
			if (clazz.contains("<")) {
				elemClass = clazz.substring(0, clazz.indexOf('<'));
			}
			
			String[] params = instSequencedParameters.size() > index ? getParameters(instSequencedParameters.get(index)) : new String[]{""};
			
			if (params[0].startsWith("ui:")) {
				params[0] = params[0].substring(3).trim();
			}
			
			Element elem = processingEnv.getElementUtils().getTypeElement(elemClass);
			if (elem == null) {
				if (SharedRecords.getEvent(clazz, environment) != null) {
					elem = new LocalElement(element, SharedRecords.getEvent(clazz, environment));
					index++;
					continue;
				}
				
				valid.addError("The element " + clazz + " couldn't be loaded");
				
				break;
			} else {
				elem = new LocalElement(element, clazz);
			}
			
			ActionPlugin pluginFound = null;
			
			for (ActionPlugin plugin : plugins) {
				if (plugin.canProcessElement(elem, environment)) {
					if (plugin instanceof BaseListActionHandler) {
						for (String value : params) {
							if (value.startsWith("(") && value.endsWith(")")) {
								List<String> newParams = divideByComma(value, '(', ')');
								newParams.add("@");
								plugin.validate(newParams.toArray(new String[newParams.size()]), elem, valid);
								break;
							}
						}
						
					} else {
						plugin.validate(params,	elem, valid);
					}
					
					pluginFound = plugin;
					
					break;
				}
			}
			
			if (pluginFound == null) {
				valid.addError("There's not a plugin for class \"" + clazz + "\"");
				break;
			}
			
			index++;
		}
	}
	
	protected abstract boolean passNextRunnableParameter();

	@Override
	public void process(String[] parameters, Element element,
			EComponentWithViewSupportHolder holder) {
				
		final String elementName = element.getSimpleName().toString();
		final String elementClass = element.asType().toString();
		List<String> classesList = getElementClasses(elementClass);
		
		boolean canFire = true;
		
		String runnableAfterSequence = null;
		
		//Make a list of instructions for this sequence and instructions for all the Sequenced Parameters
		List<String> inst = new LinkedList<String>();
		List<String> instSequencedParameters = new LinkedList<String>(Arrays.asList(parameters));
		int i = 0;
		while (i < instSequencedParameters.size()) {
			final String paramValue = instSequencedParameters.get(i);  
			if (paramValue.startsWith("#")) {
				//Fire cannot be used if there's this kind of processing
				canFire = false;
				
				inst.add(paramValue.substring(1));
				instSequencedParameters.remove(i);
				continue;
			}
			
			//This is used to indicated a subsequence inside a sequence
			if (paramValue.startsWith("@")) {
				
				if (paramValue.length() > 1) {
					runnableAfterSequence = paramValue.substring(1);
				}
				
				canFire = false;
				instSequencedParameters.remove(i);
				statements.add(0, this.statement);
				continue;
			}
			
			i++;
		}
		
		if (canFire) {
			//Firable call		
			JDefinedClass anonymous = environment.getCodeModel().anonymousClass(
				environment.getJClass(elementClass.substring(0, elementClass.indexOf('<')))
			);
			
			JMethod fire = anonymous.method(JMod.PUBLIC, environment.getCodeModel().VOID, "fire");
			fire.annotate(Override.class);
			this.statement = fire.body();
			
			holder.getInitBody().assign(ref(elementName), _new(anonymous));
		} else {
			this.statement = new JBlock();
		}
		
		List<IJStatement> callStatements = new LinkedList<>();
		int index = 0;
		for (String clazz : classesList) {
			innerClassCounter = index;
			
			String elemClass = clazz;
			if (clazz.contains("<")) {
				elemClass = clazz.substring(0, clazz.indexOf('<'));
			}
			
			Element elem = processingEnv.getElementUtils().getTypeElement(elemClass);
			
			//Create Sequence Caller and Params
			String[] params = instSequencedParameters.size() > index ? getParameters(instSequencedParameters.get(index)) : new String[]{""};
			
			boolean runInUI = false;
			if (params[0].toLowerCase().startsWith("ui:")) {
				params[0] = params[0].substring(3).trim();
				runInUI = true;
			}
				
			if (passNextRunnableParameter()) {
				if (index < classesList.size()-1) {
					params = Arrays.copyOf(params, params.length+1);
					params[params.length-1] = elementName + "Action" + (index+1) + "!";
				} else if (runnableAfterSequence != null) {
					params = Arrays.copyOf(params, params.length+1);
					params[params.length-1] = runnableAfterSequence + "!";
				}
			}
			
			String fieldName = elementName + "Action" + index;
			callStatements.add(ref(fieldName).invoke("run"));
			
			JBlock sequenceCaller = createSequenceCaller(fieldName, holder);
			
			if (runInUI) {
				JDefinedClass annonimousRunnable = environment.getCodeModel().anonymousClass(Runnable.class);
				JMethod annonimousRunnableRun = annonimousRunnable.method(JMod.PUBLIC, environment.getCodeModel().VOID, "run");
				annonimousRunnableRun.annotate(Override.class);
				
				JVar handler = sequenceCaller.decl(environment.getClasses().HANDLER, "handler", 
						_new(environment.getClasses().HANDLER).arg(environment.getClasses().LOOPER.staticInvoke("getMainLooper")));
				sequenceCaller.invoke(handler, "post").arg(_new(annonimousRunnable));
				
				sequenceCaller = annonimousRunnableRun.body();
			}
			
			//This is for the case of a special event
			//I generate a pseudo Element
			if (elem==null) {
				elem = new LocalElement(element, SharedRecords.getEvent(clazz, environment));
			} else {
				elem = new LocalElement(element, clazz);
			}
			
			for (ActionPlugin plugin : plugins) {
				if (plugin.canProcessElement(elem, environment)) {
					if (plugin instanceof BaseListActionHandler) {
						
						JBlock generatedBlock = new JBlock(); 
						JBlock block = generatedBlock;
						
						String runnableRef = null;
						JBlock elseBlockForRunnableRef = null;
						boolean passRunnableRef = true;
						
						List<String> newParams = null;
						for (String value : params) {
							if (value.startsWith("#")) continue;
							
							//This value is used to indicate a Runnable instance that should be call 
							//when this method finishes normal or asynchronously
							if (value.endsWith("!")) {
								runnableRef = value.substring(0, value.length()-1);
								continue;
							}
							
							if (value.startsWith(";")) {
								block.directStatement(value.substring(1));
							}
							
							if (value.endsWith(";")) {
								continue;
							}
							
							if (value.endsWith("?")) {
								boolean useElseBlock = false;
								
								String ifCond = value.substring(0, value.length()-1);
								if (ifCond.endsWith("!")) {
									ifCond = ifCond.substring(0, ifCond.length()-1);
									useElseBlock = true;
								}
								
								if (ifCond.endsWith("?")) {
									ifCond = ifCond.substring(0, ifCond.length()-1);
									useElseBlock = true;
									passRunnableRef = false;
								}
								
								JConditional conditional = block._if(direct(ifCond)); 
								block = conditional._then();
								
								if (useElseBlock) {
									elseBlockForRunnableRef = conditional._else(); 
								}
								
								continue;
							}
							
							if (value.startsWith("(") && value.endsWith(")")) {
								newParams = divideByComma(value, '(', ')');
								continue;
							}
						}
						
						if (newParams != null) {
							newParams.add("@" + (runnableRef != null && passRunnableRef ? runnableRef : ""));
							plugin.process(newParams.toArray(new String[newParams.size()]), elem, holder);
						}
						
						if (elseBlockForRunnableRef != null && runnableRef != null) {
							elseBlockForRunnableRef.invoke(ref(runnableRef), "run");
							
							if (!passRunnableRef) {
								runnableRef = null;
							}
						}
						
						block.add(plugin.getStatement());
						
						for (String value : params) {
							if (value.endsWith(";") && !value.startsWith(";")) {
								block.directStatement(value);
							}
						}
						
						sequenceCaller.add(generatedBlock);
						
						break;
						
					} 
					
					plugin.process(params, elem, holder);
					sequenceCaller.add(plugin.getStatement());
					
					break;
				}
			}
			
			index++;
		}
		
		for (String value : inst) {
			if (value.startsWith(";")) {
				((JBlock) this.statement).directStatement(value.substring(1));
			}
			
			if (value.endsWith(";")) {
				continue;
			}
			
			if (processLocalInst(value)) continue;
		}
		
		addCallStatements(callStatements);
		
		for (String value : inst) {
			if (value.endsWith(";") && !value.startsWith(";")) {
				((JBlock) this.statement).directStatement(value);
			}
		}
		
		if (canFire) {
			this.statement = ref(elementName).invoke("fire");	
		}
	}
	
	
	protected abstract JBlock createSequenceCaller(String name, EComponentWithViewSupportHolder holder);
	protected abstract void addCallStatements(List<IJStatement> callStatements);
	protected abstract boolean processLocalInst(String inst);
	
	@Override
	public IJStatement getStatement() {
		IJStatement newStatement = this.statement;
		
		if (this.statements.size() > 0) {
			this.statement = this.statements.get(0);
			this.statements.remove(0);
		}
		
		return newStatement;
	}

	public interface ElementContainer {
		public Element getElement();
	}
	
	private class LocalElement implements Element, ElementContainer {
		
		private Element element;
		private String className;
		
		public LocalElement(Element element, String className) {
			super();
			this.element = element;
			this.className = className;
		}
		
		@Override
		public Element getElement() {
			return element;
		}

		@Override
		public TypeMirror asType() {
			return new TypeMirror() {
				
				@Override
				public TypeKind getKind() {
					return TypeKind.DECLARED;
				}
				
				@Override
				public <R, P> R accept(TypeVisitor<R, P> v, P p) {
					return null;
				}
				
				@Override
				public String toString() {
					return className;
				}

				@Override
				public <A extends Annotation> A getAnnotation(Class<A> arg0) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public List<? extends AnnotationMirror> getAnnotationMirrors() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public <A extends Annotation> A[] getAnnotationsByType(
						Class<A> arg0) {
					// TODO Auto-generated method stub
					return null;
				}
			};
		}

		@Override
		public ElementKind getKind() {
			return element.getKind();
		}

		@Override
		public List<? extends AnnotationMirror> getAnnotationMirrors() {
			return element.getAnnotationMirrors();
		}

		@Override
		public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
			return element.getAnnotation(annotationType);
		}

		@Override
		public Set<Modifier> getModifiers() {
			return element.getModifiers();
		}

		@Override
		public Name getSimpleName() {
			return new Name() {
				@Override
				public CharSequence subSequence(int start, int end) {
					return element.getSimpleName().subSequence(start, end);
				}
				
				@Override
				public int length() {
					return element.getSimpleName().length();
				}
				
				@Override
				public char charAt(int index) {
					return element.getSimpleName().charAt(index);
				}
				
				@Override
				public boolean contentEquals(CharSequence cs) {
					return element.getSimpleName().contentEquals(cs);
				}
				
				@Override
				public String toString() {
					return element.getSimpleName().toString() + "$" + innerClassCounter + "$" + (statements.size() + 1) ;
				}
			};
		}

		@Override
		public Element getEnclosingElement() {
			return element.getEnclosingElement();
		}

		@Override
		public List<? extends Element> getEnclosedElements() {
			return element.getEnclosedElements();
		}

		@Override
		public <R, P> R accept(ElementVisitor<R, P> v, P p) {
			return element.accept(v, p);
		}
		
		@Override
		public String toString() {
			return element.toString() + "$" + innerClassCounter + "$" + (statements.size() + 1);
		}

		@Override
		public <A extends Annotation> A[] getAnnotationsByType(Class<A> arg0) {
			// TODO Auto-generated method stub
			return null;
		}

	}
	
}
