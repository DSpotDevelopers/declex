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
import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.assign;
import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.direct;
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.ref;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EBeanHolder;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.internal.helper.ViewNotifierHelper;
import org.androidannotations.internal.process.ProcessHolder.Classes;

import com.dspot.declex.api.action.annotation.Assignable;
import com.dspot.declex.api.action.annotation.Field;
import com.dspot.declex.api.action.annotation.FormattedExpression;
import com.dspot.declex.api.action.annotation.Literal;
import com.dspot.declex.api.action.annotation.StopOn;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethod;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.dspot.declex.api.action.structure.ActionResult;
import com.dspot.declex.api.util.FormatsUtils;
import com.dspot.declex.override.util.DeclexAPTCodeModelHelper;
import com.dspot.declex.share.holder.EnsureImportsHolder;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.JavaDocUtils;
import com.dspot.declex.util.TypeUtils;
import com.dspot.declex.util.element.VirtualElement;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JAnonymousClass;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JCatchBlock;
import com.helger.jcodemodel.JClassAlreadyExistsException;
import com.helger.jcodemodel.JCodeModel;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JFieldRef;
import com.helger.jcodemodel.JFormatter;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JSwitch;
import com.helger.jcodemodel.JTryBlock;
import com.helger.jcodemodel.JVar;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

class ActionsProcessor extends TreePathScanner<Boolean, Trees> {
	
	private String debugIndex = "";

	private int actionCount = 0;
	
	private List<MethodInvocationTree> subMethods = new LinkedList<>();
	
	private List<StatementTree> statements = new LinkedList<>();
	
	private JFieldRef delegatingMethodResultValueVar;
	private JFieldRef delegatingMethodFinishedVar;
	private JMethod delegatingMethod;
	private JBlock delegatingMethodStart;
	private JBlock delegatingMethodEnd;
	private JBlock delegatingMethodBody = null;
	private List<String> methodActionParamNames = new LinkedList<>();
	
	private List<JBlock> blocks = new LinkedList<>();
	private List<JBlock> originalBlocks = new LinkedList<>();
	
	private JBlock initialBlock = new JBlock();
	private JAnonymousClass sharedVariablesHolder = null;
	
	private LiteralTree literalDiscovered;
	
	private boolean processingTry;
	private boolean visitingTry;
	private TryTree currentTry;
	private boolean visitingCatch;
	
	private boolean omitParallel;
	
	private boolean visitingVariable;
	private String actionInFieldWithoutInitializer = null;
	
	private List<String> actionsTree = new LinkedList<>();
	private List<JBlock> actionsTreeAfterExecute = new LinkedList<>();
	
	private List<String> currentAction = new LinkedList<>();
	private List<String> currentActionSelectors = new LinkedList<>();
	private List<JInvocation> currentBuildInvocation = new LinkedList<>();
	private List<Map<String, ParamInfo>> currentBuildParams = new LinkedList<>(); 
	
	private Map<Integer, List<BlockDescription>> additionalBlocks = new HashMap<>();
	
	private AndroidAnnotationsEnvironment env;
	private EComponentHolder holder;
	private DeclexAPTCodeModelHelper codeModelHelper;
	private Element element;
	
	private List<? extends ImportTree> imports;
	
	private boolean ignoreActions;
	private ClassTree anonymousClassTree;
	private boolean processStarted = false;

	private AssignmentTree assignment;
	
	private ElementValidation valid;
	
	private Map<String, String> staticImports = new HashMap<>();
	
	//Cache to stored the elements that were already scanned for Actions
	private static Map<Element, Boolean> hasActionMap = new HashMap<>();
	private static List<Element> overrideAction = new LinkedList<>();
	
	public static boolean hasAction(final Element element, AndroidAnnotationsEnvironment env) {
		
		Boolean hasAction = hasActionMap.get(element);
		if (hasAction != null) return hasAction;
		
		final Trees trees = Trees.instance(env.getProcessingEnvironment());
    	final TreePath treePath = trees.getPath(
    			element instanceof VirtualElement? ((VirtualElement)element).getElement() : element
		);
    	
    	//Check if the Action Api was activated for this compilation unit
    	for (ImportTree importTree : treePath.getCompilationUnit().getImports()) {
    		
            if (importTree.getQualifiedIdentifier().toString().startsWith(DeclexConstant.ACTION + ".")) {

            	try {

	            	//Scan first to see if an action exists in the method
	            	TreePathScanner<Boolean, Trees> scanner = new TreePathScanner<Boolean, Trees>() {
	            		@Override
	            		public Boolean visitIdentifier(IdentifierTree id,
	            				Trees trees) {
	            			
	            			String name = id.getName().toString();
	            			
	            			if (Actions.getInstance().hasActionNamed(name)) {
	            				//Identifier detected
	            				throw new ActionDetectedException();
	            			}
	            			
	            			return super.visitIdentifier(id, trees);
	            		}
	            	};
	            	scanner.scan(treePath, trees);
                	        		
            	} catch (ActionDetectedException e) {  
            		//This means that an Action identifier was found
            		hasActionMap.put(element, true);
            		
                	return true;
            	}
            	
            	break;
            }
        }
    	
    	//Actions extended elements are marked in front with "$"
		if (element.getKind().equals(ElementKind.METHOD) 
			&& element.getSimpleName().toString().startsWith("$")) {
			
			hasActionMap.put(element, true);
			overrideAction.add(element);
        	return true;
		}
    	
    	hasActionMap.put(element, false);
    	return false;		
	}
	
	public static void validateActions(final Element element, ElementValidation valid, AndroidAnnotationsEnvironment env) {
		
		if (hasAction(element, env)) {
					
			//Validate overrideActions
			if (overrideAction.contains(element) ||
				(element.getKind().equals(ElementKind.METHOD) 
				&& element.getSimpleName().toString().startsWith("$"))) {
				
				//Check that the method exists in the parent
				if (!isSuperMethodInParents((TypeElement)element.getEnclosingElement(), (ExecutableElement) element, env.getProcessingEnvironment())) {
					String message = "This override actions is not valid, the method ";
					message =  message + element.toString().substring(1);
					valid.addError(message + " is not found in the parent tree");
				}
				
				return;
			}
			
    		final Trees trees = Trees.instance(env.getProcessingEnvironment());
        	final TreePath treePath = trees.getPath(element);
        	
			try {
        		ActionsProcessor scanner = new ActionsProcessor(element, null, valid, treePath, env);
            	scanner.scan(treePath, trees);
			} catch (ActionCallSuperException e) {
				throw e;
			} catch (ActionProcessingException e) {
				valid.addError(e.getMessage());
			} catch (IllegalStateException e) {
				valid.addError(e.getMessage());
			}
		}
	}
	
	private static boolean isSuperMethodInParents(TypeElement element, ExecutableElement executableElement, 
			ProcessingEnvironment env) {
		
		List<? extends Element> elems = element.getEnclosedElements();
		elements: for (Element elem : elems) {
			final String elemName = elem.getSimpleName().toString();
			
			if (elem.getModifiers().contains(Modifier.STATIC)
				|| elem.getModifiers().contains(Modifier.PRIVATE)) continue;
			
			if (elem instanceof ExecutableElement) {
								
				String executableElementName = executableElement.getSimpleName().toString();
				if (executableElementName.startsWith("$")) {
					executableElementName = executableElementName.substring(1);
				}
				
				if (elemName.equals(executableElementName)) {
					
					if (((ExecutableElement) elem).getParameters().size() != executableElement.getParameters().size())  {
						continue;
					}
					
					for (int i = 0; i < executableElement.getParameters().size(); i++) {
						
						String elemParameterType = ((ExecutableElement) elem).getParameters().get(i).asType().toString();
						String executableElementParameterType = executableElement.getParameters().get(i).asType().toString();
						
						if (!elemParameterType.equals(executableElementParameterType)) 
							continue elements;
					}
					
					return true;
				}
			}
		}
		
		//Apply to Extensions
		List<? extends TypeMirror> superTypes = env.getTypeUtils().directSupertypes(element.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = env.getElementUtils().getTypeElement(type.toString());
			
			if (isSuperMethodInParents(superElement, executableElement, env)) return true;			
		}
		
		return false;
	}
	
	public static void processActions(final Element element, EComponentHolder holder) {
    	
		if (!hasActionMap.containsKey(element)) {
			throw new RuntimeException("Action not validated: " + element + " in " + holder.getAnnotatedElement());
		}
		    		
		final Trees trees = Trees.instance(holder.getEnvironment().getProcessingEnvironment());
    	final TreePath treePath = trees.getPath(element);
    	
    	ActionsProcessor scanner = new ActionsProcessor(element, holder, null, treePath, holder.getEnvironment());
    	
    	if (!overrideAction.contains(element)) {
    		scanner.scan(treePath, trees);
    	}
    	
	}
	
	private ActionsProcessor(Element element, EComponentHolder holder, ElementValidation valid, TreePath treePath, AndroidAnnotationsEnvironment env) {
		
		this.element = element;
		this.holder = holder;
		this.env = env;
		this.valid = valid;
		
		this.codeModelHelper = new DeclexAPTCodeModelHelper(env);
		imports = treePath.getCompilationUnit().getImports();
		
		if (overrideAction.contains(element)) {

			createActionMethod(true);
			
			return;
		}
		
		//Reference and check method parameters
		if (element instanceof ExecutableElement) {	
			for (VariableElement param : ((ExecutableElement)element).getParameters()) {
				methodActionParamNames.add(param.getSimpleName().toString());
				if (param.getSimpleName().toString().startsWith("$")) {
					throw new ActionProcessingException(
							"Parameter names starting with \"$\" are not permitted for action methods"
						);
				}
			}
		}
		
		pushBlock(initialBlock, null);

		if (showDebugInfo()) System.out.println("PROCESSING: " + holder.getAnnotatedElement());
	}
	
	private boolean isValidating() {
		return valid != null;
	}
	
	private boolean showDebugInfo() {
		if (isValidating()) return false;
		
		return env.getOptionBooleanValue(Actions.OPTION_DEBUG_ACTIONS);
	}
	
	private String debugPrefix() {
		String prefix = "";
		return prefix + debugIndex;
	}
	
	private String parseForSpecials(String expression, boolean ignoreThis) {
		if (isValidating()) return expression;
		
		//Split by string literals (Specials should not be placed inside Strings)
		List<String> literalsSplit = Arrays.asList(expression.split("(?<!\\\\)\""));
		if (literalsSplit.size() <= 1) return parseStringForSpecial(expression, ignoreThis);		
		
		String newExpression = "";
		int i = 0;
		for (String part : literalsSplit) {
			if (i % 2 == 0) { //Not string literals
				newExpression = newExpression + parseStringForSpecial(part, ignoreThis);		
			} else {  //String literals
				newExpression = newExpression + "\"" + part + "\"";
			}
			i++;
		}
		
		return newExpression;
	}
	
	private String parseStringForSpecial(String expression, boolean ignoreThis) {
		
		String generatedClass = holder.getGeneratedClass().name();
		String annotatedClass = holder.getAnnotatedElement().getSimpleName().toString();
		
		if (!ignoreThis) {
			expression = expression.replaceAll("(?<![a-zA-Z_$.0-9])this(?![a-zA-Z_$0-9])", generatedClass + ".this");
			expression = expression.replaceAll("(?<![a-zA-Z_$.0-9])super(?![a-zA-Z_$0-9])", generatedClass + ".super");
		}
		
		expression = expression.replaceAll("(?<![a-zA-Z_$.])" + annotatedClass + ".this(?![a-zA-Z_$])", generatedClass + ".this");
		expression = expression.replaceAll("(?<![a-zA-Z_$.])" + annotatedClass + ".super(?![a-zA-Z_$])", generatedClass + ".super");
		
		if (currentActionSelectors.size() > 0) {
			expression = expression.replace(currentActionSelectors.get(0), "");
		}
		
		//Static imports
		for (Entry<String, String> staticImport : staticImports.entrySet()) {
			expression = expression.replaceAll(
					"(?<![a-zA-Z_$.0-9])" + staticImport.getKey() + "(?![a-zA-Z_$.0-9])", staticImport.getValue()
				);
		}
		
		return expression;
	}
	
	private void writeVariable(VariableTree variable, JBlock block, IJExpression initializer) {
		
		if (isValidating()) return;
		
		if (showDebugInfo()) System.out.println(debugPrefix() + "writeVariable: " + variable);

		//Inferred variables must start with $ sign
		final String name = variable.getName().toString();
		if (sharedVariablesHolder == null) {
			createSharedVariablesHolder();
		}
		
		String variableClass = variableClassFromImports(variable.getType().toString(), true);		
		
		int arrayCounter = 0;
		while (variableClass.endsWith("[]")) {
			arrayCounter++;
			variableClass = variableClass.substring(0, variableClass.length() - 2);
		}
		
		AbstractJClass VARIABLECLASS = getJClass(variableClass);
		for (int i = 0; i < arrayCounter; i++) {
			VARIABLECLASS = VARIABLECLASS.array();
		}
		
		if (!sharedVariablesHolder.containsField(name)) {
			if (initializer != null && !name.startsWith("$")) {

				sharedVariablesHolder.field(
						JMod.NONE, 
						VARIABLECLASS, 
						name
					);
				
				//Initializers for arrays
				if (arrayCounter > 0) {
					String initializerValue = expressionToString(initializer);
					while (initializerValue.startsWith("(") && initializerValue.endsWith(")")) {
						initializerValue = initializerValue.substring(1, initializerValue.length()-1);
					}
					
					if (initializerValue.startsWith("{")) {
						initializerValue = "new " + VARIABLECLASS.name() + initializerValue;
					}
					
					block.assign(ref(name), direct(initializerValue));
					
				} else {
					block.assign(ref(name), initializer);
				}
				
			} else {
				if (name.startsWith("$")) {
					sharedVariablesHolder.field(
							JMod.NONE, 
							VARIABLECLASS, 
							name
						);
					
					block.assign(ref(name), ref(name.substring(1)));
				} else {
					sharedVariablesHolder.field(
							JMod.NONE, 
							VARIABLECLASS, 
							name
						);												
				}
			}
		} else {
			if (initializer != null) {
				block.assign(ref(name), initializer);
			}
		}
	}
	
	private void createSharedVariablesHolder() {
		sharedVariablesHolder = getCodeModel().anonymousClass(Runnable.class);
		JMethod anonymousRunnableRun = sharedVariablesHolder.method(JMod.PUBLIC, getCodeModel().VOID, "run");
		anonymousRunnableRun.annotate(Override.class);
		
		//Add all the created code to the sharedVariablesHolder
		anonymousRunnableRun.body().add(initialBlock);
		
		initialBlock = new JBlock();
		JVar sharedVariablesHolderVar = initialBlock.decl(
				getJClass(Runnable.class.getCanonicalName()), 
				"sharedVariablesHolder", 
				_new(sharedVariablesHolder)
			);
		initialBlock.invoke(sharedVariablesHolderVar, "run");
	}
	
	private void writePreviousStatements() {
		
		if (!isValidating()) {
			JBlock block = blocks.get(0);
			
			//Write all the statements till this point
			for (StatementTree statement : statements) {
				if (statement instanceof ExpressionStatementTree) {
					if (showDebugInfo()) System.out.println(debugPrefix() + "writeStatement: " + statement);
					block.directStatement(parseForSpecials(
							statement.toString(), 
							statement instanceof StringExpressionStatement? 
									((StringExpressionStatement)statement).ignoreThis() : false
						));
				}
				
				if (statement instanceof VariableTree) {
					VariableTree variable = (VariableTree) statement;
					IJExpression initializer = variable.getInitializer() == null? 
			                   null : direct(parseForSpecials(variable.getInitializer().toString(), false));
				
					writeVariable(variable, block, initializer);
				}
				
			}			
		}
		
		statements.clear();
	}
	
	private void buildPreviousAction() {
		
		if (isValidating()) return;
		
		//Call the arguments for the last Action
		if (currentAction.get(0) != null) {
			
			if (showDebugInfo()) System.out.println(debugPrefix() + "buildingPreviousAction");
			
			for (String paramName : currentBuildParams.get(0).keySet()) {
				
				final ParamInfo paramInfo = currentBuildParams.get(0).get(paramName);
				
				//The first line of the runnableBlock, is the description line
				if (paramInfo.runnableBlock != null && paramInfo.runnableBlock.getContents().size()==1) {
					currentBuildInvocation.get(0).arg(_null());
				} else {
					currentBuildInvocation.get(0).arg(paramInfo.assignment);
				}
			}
			
		}
	}
	
	@Override
	public Boolean visitVariable(VariableTree variable, Trees trees) {
		//Field action
		if (!(element instanceof ExecutableElement)) {
			if (variable.getInitializer() == null) {
				actionInFieldWithoutInitializer = variable.getType().toString();
				return visitMethodInvocation(null, trees);
			}
		}
		
		if (ignoreActions) return super.visitVariable(variable, trees);
		
		//Ignore variables out of the method
		if (!processStarted) return super.visitVariable(variable, trees);
		
		if (visitingCatch) {
			visitingCatch = false;
			return super.visitVariable(variable, trees);
		}
		
		statements.add(variable);	
		
		visitingVariable = true;
		Boolean result = super.visitVariable(variable, trees);
		addAnonymousStatements(variable.toString());
		visitingVariable = false;
		
		return result;
	}
	
	@Override
	public Boolean visitAssignment(AssignmentTree assignment, Trees arg1) {
		if (!visitingVariable) this.assignment  = assignment;
		return super.visitAssignment(assignment, arg1);
	}
	
	@Override
	public Boolean visitExpressionStatement(
			ExpressionStatementTree expr, Trees trees) {
		
		if (expr.toString().startsWith("super.") && anonymousClassTree == null) {
			//Calling to a super method, it should be checked if it is an action
			Matcher matcher = Pattern.compile("super.([a-zA-Z_$][a-zA-Z_$0-9]*)\\(").matcher(expr.toString());
			if (matcher.find()) {
				String methodName = matcher.group(1);
				String parameters = expr.toString().replace(matcher.group(0), "");
				parameters = parameters.substring(0, parameters.lastIndexOf(')'));
				
				//Count parameters TODO: determine parameter types
				int deep = 0;
				int parametersCount = 0;
				for (int i = 0; i < parameters.length(); i++) {
					char ch = parameters.charAt(i);
					if (ch == '(') deep++;
					if (ch == ')') deep--;
					if (ch == ',' && deep == 0) parametersCount++;
				}
				if (!parameters.equals("")) parametersCount++;
				
				List<? extends Element> elems = element.getEnclosingElement().getEnclosedElements();
				for (Element elem : elems) {
					final String elemName = elem.getSimpleName().toString();
					
					if (elem.getModifiers().contains(Modifier.STATIC)
						|| elem.getModifiers().contains(Modifier.PRIVATE)) continue;
					
					if (elem instanceof ExecutableElement) {
						if (elemName.equals(methodName) ) {
							if (((ExecutableElement) elem).getParameters().size() == parametersCount) {
								if (hasAction(elem, env) && !overrideAction.contains(elem)) {
									throw new ActionCallSuperException(elem);
								}
							}
						}						
					}
				}
			}
		}

		if (ignoreActions) return super.visitExpressionStatement(expr, trees);
		
		statements.add(expr);
		
		assignment = null;
		Boolean result = super.visitExpressionStatement(expr, trees);
		addAnonymousStatements(expr.toString());
		
		return result;
	}
	
	@Override
	public Boolean visitReturn(ReturnTree returnTree, Trees trees) {
		if (ignoreActions) return super.visitReturn(returnTree, trees);
		
		boolean insideAction = false;
		for (String action : actionsTree) {
			
			if (action == null) continue;
			
			String actionClass = Actions.getInstance().getActionNames().get(action);
			ActionInfo actionInfo = Actions.getInstance().getActionInfos().get(actionClass);
			
			if (actionInfo.isTimeConsuming) {
				throw new ActionProcessingException(
						"The return statement of action method cannot be inside a time consumming action."
						+ " The action " + action + " will finish it's operation in a different thread. "
						+ "Error: \"" + returnTree + "\" is inside \"" + actionsTree + "\". "
						+ "If you want to simply break the action \"" + action + "\" execution, consider using instead: "
						+ "\"'{'" + returnTree + "'}'\""
				);
			} else {
				insideAction = true;
			}
											
		}
		
		if (!isValidating() && (insideAction || delegatingMethodFinishedVar != null)) {
			
			final String resultName = delegatingMethod.name() + "_result"; 
			
			if (delegatingMethodFinishedVar == null) {
				
				AbstractJClass ActionResult = env.getJClass(ActionResult.class);
				JVar result = delegatingMethodStart.decl(
					JMod.FINAL,
					ActionResult, 
					resultName,
					_new(ActionResult)
				);
				
				delegatingMethodFinishedVar = result.ref("finished");
				
				String resultRef;
				if (((ExecutableElement)element).getReturnType().getKind().isPrimitive()) {
					resultRef = ((ExecutableElement)element).getReturnType().toString() + "Val";
				} else {
					resultRef = "objectVal";
				}
				
				if (!((ExecutableElement)element).getReturnType().toString().equals("void")) {
					delegatingMethodResultValueVar = result.ref(resultRef);
					
					delegatingMethodEnd = new JBlock();
					delegatingMethodEnd._return(result.ref(resultRef));
				}
			}
			
			//Assign value and return
			if (delegatingMethodResultValueVar != null) {
				statements.add(
					new StringExpressionStatement(
						resultName + "." + delegatingMethodResultValueVar.name() + " = " + returnTree.getExpression() + ";"
					)
				);
			}
		
			if (delegatingMethodFinishedVar != null) {
				statements.add(
					new StringExpressionStatement(
						resultName + "." + delegatingMethodFinishedVar.name() + " = true;"
					)
				);
			}
			
			if (insideAction || delegatingMethodResultValueVar == null) {
				statements.add(
					new StringExpressionStatement("return;")
				);
			} else {
				statements.add(
					new StringExpressionStatement(
						"return " + resultName + "." + delegatingMethodResultValueVar.name() + ";"
					)
				);
			}
			
			for (int i = 0; i < actionsTreeAfterExecute.size(); i++) {
				JBlock block = actionsTreeAfterExecute.get(i);
				
				//This block contains only this condition
				if (!block.isEmpty()) continue;
				
				JBlock finishBlock = block._if(delegatingMethodFinishedVar)._then();
				if (delegatingMethodResultValueVar != null && i == 0) {
					finishBlock._return(delegatingMethodResultValueVar);
				} else {
					finishBlock._return();
				}	
			}
			
		} else {
			statements.add(new StringExpressionStatement(returnTree.toString()));	
		}		
		
		Boolean result = super.visitReturn(returnTree, trees);
		addAnonymousStatements(returnTree.toString());
		
		return result;
	}
	
	@Override
	public Boolean visitIdentifier(IdentifierTree id,
			Trees trees) {
		
		//This will happen with identifiers prior to the main block (ex. Annotations)
		if (!processStarted) return true;
				
		final String idName = id.toString();
		
		//If it is used one of the method parameters, then use sharedVariablesHolder
		if ((currentAction.get(0) != null || currentAction.size() > 1 || anonymousClassTree != null) 
			&& methodActionParamNames.contains(idName)) {
			
			if (sharedVariablesHolder == null) {
				createSharedVariablesHolder();
			}
		}
				
		//Accessing Action from within unsupported structure
		if (Actions.getInstance().hasActionNamed(idName) && ignoreActions) {
			throw new ActionProcessingException(
					"Action is not supported in this location: " + idName
				);
		}
		
		subMethods.clear();		
			
		//Ensure import of all the identifiers
		variableClassFromImports(idName, true);
		
		return super.visitIdentifier(id, trees);
	}

	@Override
	public Boolean visitEmptyStatement(EmptyStatementTree arg0, Trees trees) {
		if (ignoreActions) return super.visitEmptyStatement(arg0, trees);
		
		if (showDebugInfo()) System.out.println(debugPrefix() + "empty");
		return super.visitEmptyStatement(arg0, trees);
	}
	
	@Override
	public Boolean visitIf(IfTree ifTree, Trees trees) {
		if (ignoreActions) return super.visitIf(ifTree, trees);
		
		writePreviousStatements();
		
		String ifCondition = ifTree.getCondition().toString();
		
		//Remove unnecessary parenthesis
		while (ifCondition.startsWith("(") && ifCondition.endsWith(")")) {
			ifCondition = ifCondition.substring(1, ifCondition.length()-1);
		}
		
		scan(ifTree.getCondition(), trees);
		
		//Check for Action Selectors
		Matcher matcher = Pattern.compile("(\\$[a-zA-Z_$][a-zA-Z_$0-9]*)\\.([a-zA-Z_$][a-zA-Z_$0-9]*)").matcher(ifCondition);
		if (matcher.find()) {
			
			final String actionName = matcher.group(1);
			final String actionSelector = matcher.group(2);
			
			if (Actions.getInstance().hasActionNamed(actionName)) {
				
				if (!matcher.group(0).equals(ifCondition)) {
					throw new ActionProcessingException(
							"Malformed Action Selector: if(" + ifCondition + ")"
						);						
				}
				
				if (actionName.equals(currentAction.get(0))) {

					ParamInfo thenParam = null, elseParam = null;
					
					for (ParamInfo paramInfo : currentBuildParams.get(0).values()) {
						
						if (thenParam != null && elseParam != null) break;
												
						if (paramInfo.param.name.equals(actionSelector)) {
							thenParam = paramInfo;
						} else {
							if (elseParam == null) {
								elseParam = paramInfo;
							}
						}
						
					}

					if (thenParam != null) {
						currentActionSelectors.add(0, actionName + "." + actionSelector + "().");
						addActionSelector(actionName, actionSelector, thenParam);
						scanForActionsOmitingBlock(ifTree.getThenStatement(), trees);
						finishBlock();
						currentActionSelectors.remove(0);
					} else {
						throw new ActionProcessingException(
								"Malformed Action Selector: if(" + ifCondition + ")"
							);							
					}
					
					if (ifTree.getElseStatement() != null) {
						
						if (elseParam == null) {
							throw new ActionProcessingException(
									"The action " + actionName + " doesn't support \"else\" statements in if(" + ifCondition + ")"
								);
						}
						
						currentActionSelectors.add(0, actionName + "." + actionSelector + "().");
						addActionSelector(actionName, elseParam.param.name, elseParam);
						scanForActionsOmitingBlock(ifTree.getElseStatement(), trees);
						finishBlock();
						currentActionSelectors.remove(0);
					}						
					
				} else {
					throw new ActionProcessingException(
							"Action Selector out of context: " + matcher.group(1) 
							+ " in if(" + ifCondition + "). The current action is " + currentAction.get(0) + "." 
						);
				}
			
				return true;
				
			}				
		}
		
		JConditional _if = blocks.get(0)._if(direct(parseForSpecials(ifCondition, false)));
		JBlock thenBlock = _if._then();
		
		pushCompleteBlock(thenBlock, "newIf: " + ifCondition);
		scanForActionsOmitingBlock(ifTree.getThenStatement(), trees);
		finishBlock();
		
		if (ifTree.getElseStatement() != null) {
			JBlock elseBlock = _if._else();
			pushCompleteBlock(elseBlock, "elseBlock:");
			scanForActionsOmitingBlock(ifTree.getElseStatement(), trees);
			finishBlock();
		}

		return true;
	}
	
	private void addActionSelector(String actionName, String actionSelector, ParamInfo paramInfo) {
		final ActionMethodParam param = paramInfo.param;

		//If the block already exists, do not create a new Runnable 
		if (paramInfo.runnableBlock != null) {
			pushCompleteBlock(paramInfo.runnableBlock, null);
		} else {
			
			JDefinedClass anonymousRunnable = getCodeModel().anonymousClass((AbstractJClass) param.clazz);
			JMethod anonymousRunnableRun = anonymousRunnable.method(JMod.PUBLIC, getCodeModel().VOID, "run");
			anonymousRunnableRun.annotate(Override.class);
			anonymousRunnableRun.body().directStatement("//ACTION EVENT: " + param.name);
			
			ParamInfo newParamInfo = new ParamInfo(
					paramInfo.param, 
					anonymousRunnableRun.body(), 
					_new(anonymousRunnable)
				);
			currentBuildParams.get(0).put(param.name, newParamInfo);
			
			JBlock anonymousBlock = checkTryForBlock(anonymousRunnableRun.body(), false);
			pushCompleteBlock(anonymousBlock, null);
		}
		
		if (showDebugInfo()) {
			System.out.println(debugPrefix() + "newSelector: " + actionName + "." + actionSelector);
			debugIndex = debugIndex + "    ";
		}

	}
	
	//Method should be called only after a pushCompleteBlock
	private void scanForActionsOmitingBlock(Tree expression, Trees trees) {
		if (expression.getKind().equals(Kind.BLOCK)) {
			omitParallel = true;
		}
		scan(expression, trees);
	}
	
	@Override
	public Boolean visitMethodInvocation(MethodInvocationTree invoke,
			Trees trees) {
		if (ignoreActions) {
			return super.visitMethodInvocation(invoke, trees);
		}

		String methodSelect = invoke != null? invoke.getMethodSelect().toString() 
				                            : actionInFieldWithoutInitializer;
	
		if (methodSelect.contains(".")) {
			subMethods.add(invoke);
		} else {
			
			if (Actions.getInstance().hasActionNamed(methodSelect)) {
				
				String actionClass = Actions.getInstance().getActionNames().get(methodSelect);
				ActionInfo actionInfo = Actions.getInstance().getActionInfos().get(actionClass);

				if (delegatingMethodBody == null) {
					if (isValidating()) {
						//This block is not going to be used
						delegatingMethodBody = new JBlock();
						
						if (!(element instanceof ExecutableElement)) { 
							
							///Current Action Information for the new Block
							currentAction.add(0, null);
							currentBuildInvocation.add(0, null);
							currentBuildParams.add(0, new LinkedHashMap<String, ParamInfo>());
							processStarted = true;
							
						}
					} else {
						if (element instanceof ExecutableElement) {
							
							//Create the Action method
							createActionMethod(false);
							
						} else {
							//Fields
							JDefinedClass anonymous = getCodeModel().anonymousClass(
									getJClass(DeclexConstant.ACTION + "." + methodSelect)
								);
								
							JMethod fire = anonymous.method(JMod.PUBLIC, getCodeModel().VOID, "fire");
							fire.annotate(Override.class);
							delegatingMethodBody = fire.body();
							
							holder.getInitBody().assign(ref(element.getSimpleName().toString()), _new(anonymous));
							
							///Current Action Information for the new Block
							currentAction.add(0, null);
							currentBuildInvocation.add(0, null);
							currentBuildParams.add(0, new LinkedHashMap<String, ParamInfo>());
							processStarted = true;
							
							if (showDebugInfo()) {
								System.out.println(debugPrefix() + "FieldStart:" + subMethods);
								debugIndex = debugIndex + "    ";				
							}
						}
					}
				}

				String actionName = methodSelect.substring(0, 1).toLowerCase() 
		                  + methodSelect.substring(1) + actionCount;
				if (actionInfo.isGlobal) {
					actionName = actionName + "$" + element.getSimpleName();
				}
				
				JBlock block = blocks.get(0);
				
				//This is important to detect empty blocks
				block.directStatement("//===========ACTION: " + actionName + "===========");
				
				buildPreviousAction();
				actionsTree.add(methodSelect);
				
				VariableTree variable = null;
				if (visitingVariable) {
					variable = (VariableTree) statements.get(statements.size()-1);
				}
				
				//Remove last statement (represents this Action)
				if (statements.size() > 0) {
					statements.remove(statements.size()-1);
				}
				
				writePreviousStatements();
				            					
				currentAction.set(0, methodSelect);
				actionCount++;
				
				IJExpression context = holder == null? ref("none") : holder.getContextRef();
				if (context == _this()) {
					context = holder.getGeneratedClass().staticRef("this");
				}
				
				AbstractJClass injectedClass = getJClass(actionClass + ModelConstants.generationSuffix());				
				JBlock preInstantiate = block.blockVirtual();
				
				final JVar action;
				if (actionInfo.isGlobal && !isValidating()) {
					action = holder.getGeneratedClass().field(JMod.PRIVATE, injectedClass, actionName);
					block.assign(action, injectedClass.staticInvoke(EBeanHolder.GET_INSTANCE_METHOD_NAME).arg(context));
				} else {
					action = block.decl(
							JMod.FINAL,
							injectedClass, 
							actionName,
							injectedClass.staticInvoke(EBeanHolder.GET_INSTANCE_METHOD_NAME).arg(context)
					);
				}
				

				if (holder instanceof EComponentWithViewSupportHolder) {
					
					if (actionInfo.handleViewChanges) {
						ViewNotifierHelper viewNotifierHelper = ((EComponentWithViewSupportHolder) holder).getViewNotifierHelper();
						
						JVar previousNotifier = viewNotifierHelper.replacePreviousNotifier(preInstantiate);
						viewNotifierHelper.resetPreviousNotifier(block, previousNotifier);
						viewNotifierHelper.ifWasCalledNotifier(block)
						                  .invoke(action, "onViewChanged")
						                  .arg(holder.getGeneratedClass().staticRef("this"));							
					}
					
				}
				
				actionInfo.clearMetaData();
				
				JBlock preInit = block.blockVirtual();
				JInvocation initInvocation = block.invoke(action, "init");
				if (invoke != null) {
					
					for (String arg : processArguments("init", invoke, action, actionInfo)) {
						initInvocation.arg(direct(arg));
					}
				} 
				JBlock postInit = block.blockVirtual();
				
				Collections.reverse(subMethods);
				JInvocation externalInvoke = null;
				
				String[] stopOn = null;
				boolean buildAndExecute = true;
				
				for (MethodInvocationTree invocation : subMethods) {
					String name = invocation.getMethodSelect().toString();
					int index = name.lastIndexOf('.');
					name = name.substring(index+1);
					
					if (externalInvoke == null) {
						List<ActionMethod> methods = actionInfo.methods.get(name);
						if (methods != null && methods.size() > 0) {
							String holderClass = actionInfo.holderClass;
							String resultClass = methods.get(0).resultClass;
							
							if (holderClass.startsWith(Actions.BUILTIN_DIRECT_PKG)) {
								holderClass = holderClass.replace(Actions.BUILTIN_DIRECT_PKG, Actions.BUILTIN_PKG);
							}
							if (resultClass.startsWith(Actions.BUILTIN_DIRECT_PKG)) {
								resultClass = resultClass.replace(Actions.BUILTIN_DIRECT_PKG, Actions.BUILTIN_PKG);
							}
							
							if (!TypeUtils.isSubtype(holderClass, resultClass, env.getProcessingEnvironment())) {
								
								if (actionInfo.superHolderClass == null 
									|| !TypeUtils.isSubtype(actionInfo.superHolderClass, resultClass, env.getProcessingEnvironment())) {

									externalInvoke = invoke(action, name);					
									
									if (methods.get(0).annotations != null) {
										for (Annotation annotation : methods.get(0).annotations) {	
											if (annotation instanceof StopOn) {
												stopOn = ((StopOn) annotation).value();
											}
										}
									}
								}								
							}
						}

						JInvocation subMethodInvocation = externalInvoke==null? block.invoke(action, name) : externalInvoke;
						for (String arg : processArguments(name, invocation, action, actionInfo)) {
							subMethodInvocation = subMethodInvocation.arg(direct(arg));
	    				}
					} else {
						externalInvoke = externalInvoke.invoke(name);
						for (ExpressionTree arg : invocation.getArguments()) {
							externalInvoke = externalInvoke.arg(direct(parseForSpecials(arg.toString(), false)));
	    				}
						
						if (stopOn != null) {
							for (String stopOnMethodName : stopOn) {
								if (stopOnMethodName.equals(name)) {
									buildAndExecute = false;
									break;
								}
							}
						}
					}
					
				}
					
				List<ActionMethod> buildMethods = actionInfo.methods.get("build");
				if (buildMethods != null && buildMethods.size() > 0) {
					
					try {
						actionInfo.metaData.put("action", action);						
						
						if (isValidating()) {
							actionInfo.validateProcessors();
						} else {
							actionInfo.metaData.put("holder", holder);
							actionInfo.callProcessors();
						}						
						
					} catch (IllegalStateException e) {
						throw new ActionProcessingException(
									e.getMessage()
								);
					}
					
					@SuppressWarnings("unchecked")
					List<IJStatement> preInitBlocks = (List<IJStatement>) actionInfo.metaData.get("preInitBlocks");
					if (preInitBlocks != null) {
						for (IJStatement preInitBlock : preInitBlocks) {
							preInit.add(preInitBlock);
						}
					}
					
					@SuppressWarnings("unchecked")
					List<IJStatement> postInitBlocks = (List<IJStatement>) actionInfo.metaData.get("postInitBlocks");
					if (postInitBlocks != null) {
						for (IJStatement postBuildBlock : postInitBlocks) {
							postInit.add(postBuildBlock);
						}
					}
					
					if (buildAndExecute) {
						
						@SuppressWarnings("unchecked")
						List<IJStatement> preBuildBlocks = (List<IJStatement>) actionInfo.metaData.get("preBuildBlocks");
						if (preBuildBlocks != null) {
							for (IJStatement preBuildBlock : preBuildBlocks) {
								block.add(preBuildBlock);
							}
						}
						
						currentBuildInvocation.set(0, block.invoke(action, "build"));
						currentBuildParams.get(0).clear();
						
						@SuppressWarnings("unchecked")
						List<IJStatement> postBuildBlocks = (List<IJStatement>) actionInfo.metaData.get("postBuildBlocks");
						if (postBuildBlocks != null) {
							for (IJStatement postBuildBlock : postBuildBlocks) {
								block.add(postBuildBlock);
							}
						}
						
						if (externalInvoke != null) {
							externalInvokeInBlock(block, externalInvoke, variable);
						}
						
						ActionMethod buildMethod = buildMethods.get(0);
						boolean firstParam = true;
						for (ActionMethodParam param : buildMethod.params) {
							ParamInfo paramInfo;
							if (firstParam) {
								
								JDefinedClass anonymousRunnable = getCodeModel().anonymousClass((AbstractJClass) param.clazz);
								JMethod anonymousRunnableRun = anonymousRunnable.method(JMod.PUBLIC, getCodeModel().VOID, "run");
								anonymousRunnableRun.annotate(Override.class);
								anonymousRunnableRun.body().directStatement("//ACTION EVENT: " + param.name);
								
								paramInfo = new ParamInfo(param, anonymousRunnableRun.body(), _new(anonymousRunnable));
								
								JBlock anonymousBlock = checkTryForBlock(anonymousRunnableRun.body(), false);
								blocks.set(0, anonymousBlock);
								
								if (showDebugInfo()) {
									if (invoke != null) {
										System.out.println(debugPrefix() + "writeAction: " + invoke);
									} else {
										System.out.println(debugPrefix() + "writeAction: " + methodSelect);
									}
									
								}
								
								firstParam = false;
								
							} else {
								paramInfo = new ParamInfo(param, null, _null());
							}
							
							currentBuildParams.get(0).put(param.name, paramInfo);
						}
					
						block.invoke(action, "execute");
						
					} else {
						currentBuildInvocation.set(0, null);
						currentBuildParams.get(0).clear();
						
						if (externalInvoke != null) {
							externalInvokeInBlock(block, externalInvoke, variable);
						}
					}
				}
			
				actionsTreeAfterExecute.add(block.blockVirtual());
				block.directStatement("//============================================");
				
				if (!(element instanceof ExecutableElement)) {
					finishBlock();
					return true;
				}
			}
			
			subMethods.clear();
		}
		
		return super.visitMethodInvocation(invoke, trees);
	}
	
	private void createActionMethod(boolean isOverrideAction) {
		delegatingMethod = codeModelHelper.overrideAnnotatedMethod((ExecutableElement) element, holder, true);
		
		if (!isOverrideAction) {
			codeModelHelper.removeBody(delegatingMethod);
			
			delegatingMethodBody = delegatingMethod.body();							
			delegatingMethodStart = delegatingMethodBody.blockVirtual();			
		} else {
			delegatingMethod.annotate(Override.class);
		}

		String javaDocRef = "<br><hr><br>\nAction Method " + JavaDocUtils.referenceFromElement(element);
		delegatingMethod.javadoc().add(javaDocRef);
		
		JMethod overrideMethod = codeModelHelper.findAlreadyGeneratedMethod((ExecutableElement) element, holder, false);
		if (overrideMethod != null) {
			
			JBlock newBody = replaceSuperCallInBlock(overrideMethod.body(), (ExecutableElement)element);
			if (newBody != null) {
				codeModelHelper.removeBody(overrideMethod);
				overrideMethod.body().add(newBody);
			}
			
		} else {
			overrideMethod = codeModelHelper.overrideAnnotatedMethod((ExecutableElement) element, holder);
			codeModelHelper.removeBody(overrideMethod);
			
			if (isOverrideAction) {
				javaDocRef = "<br><hr><br>\nOverride Action Method " + JavaDocUtils.referenceFromElement(element);
				overrideMethod.javadoc().add(javaDocRef);
			}
			
			JInvocation actionInvoke = invoke(delegatingMethod);								
			for (JVar param : overrideMethod.params()) {
				actionInvoke.arg(ref(param.name()));
			}
			
			if (((ExecutableElement)element).getReturnType().toString().equals("void")) {
				overrideMethod.body().add(actionInvoke);
			} else {
				overrideMethod.body()._return(actionInvoke);
			}
			
		}
	}

	private JBlock replaceSuperCallInBlock(JBlock block, ExecutableElement executableElement) {
				
		boolean replace = false;
		JBlock newBody = new JBlock();
		
		//TODO Replace calls to super, if any
		for (Object content : block.getContents()) {
			
			if (content instanceof IJStatement) {
				boolean contentReplaced = false;
				StringWriter writer = new StringWriter();
				JFormatter formatter = new JFormatter(writer);
				IJStatement statement = (IJStatement) content;
				statement.state(formatter);
				String statementString = writer.getBuffer().toString();
				
				Matcher matcher = Pattern.compile(
						"((?:(?:[a-zA-Z_$][a-zA-Z_$0-9]*\\.)*" + holder.getGeneratedClass().name() + "\\.)*super.)"
						+ "([a-zA-Z_$][a-zA-Z_$0-9]*)\\(([^;]*);"
					).matcher(statementString);
				
				while (matcher.find()) {
					String methodName = matcher.group(2);
					String parameters = matcher.group(3);
					parameters = parameters.substring(0, parameters.lastIndexOf(')'));
					
					//Count parameters TODO: determine parameter types
					int deep = 0;
					int parametersCount = 0;
					for (int i = 0; i < parameters.length(); i++) {
						char ch = parameters.charAt(i);
						if (ch == '(') deep++;
						if (ch == ')') deep--;
						if (ch == ',' && deep == 0) parametersCount++;
					}
					if (!parameters.equals("")) parametersCount++;
					
					String executableElementName = executableElement.getSimpleName().toString();
					if (executableElementName.startsWith("$")) {
						executableElementName = executableElementName.substring(1);
					}
					
					if (parametersCount == executableElement.getParameters().size()
						&& methodName.equals(executableElementName)) {
						
						statementString = statementString.replace(matcher.group(1), "$");
						replace = true;
						contentReplaced = true;
					}
				}
				
				if (contentReplaced) {
					newBody.directStatement("//Action Method \"" + executableElement + "\" was injected");
					String[] lines = statementString.split(System.lineSeparator());
					for (String line : lines) {
						newBody.directStatement(line);
					}
					continue;
				}
			}
			
			if (content instanceof JVar) {
				JVar var = (JVar) content;
				try {
					java.lang.reflect.Field varInitField = JVar.class.getDeclaredField("m_aInitExpr");
					varInitField.setAccessible(true);
					IJExpression varInit = (IJExpression) varInitField.get(var);

					newBody.decl(var.type(), var.name(), varInit);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} else {
				newBody.add((IJStatement) content);
			}
		}
		
		if (replace) return newBody;
		
		return null;
		
	}

	private void externalInvokeInBlock(JBlock block, JInvocation invocation, VariableTree variable) {
		if (visitingVariable) {							
			writeVariable(variable, block, invocation);
		} else {
			
			if (assignment != null) {
				block.assign(ref(assignment.getVariable().toString()), invocation);
				assignment = null;
			} else {
				block.add(invocation);
			}
			
		}
	}
	
	@Override
	public Boolean visitBlock(BlockTree blockTree, Trees tree) {
			
		if (ignoreActions) return super.visitBlock(blockTree, tree);
	
		if (blockTree.getStatements().size() == 1 && actionsTree.size() > 0
			&& blockTree.getStatements().get(0).toString().startsWith("return")) {
			
			if (blockTree.getStatements().get(0).toString().equals("return;")) {
				JBlock block = blocks.get(0);
				block._return();
				return true;
			} else {
				if (blockTree.getStatements().get(0).toString().equals("return null;")
					|| blockTree.getStatements().get(0).toString().equals("return 0;")
					|| blockTree.getStatements().get(0).toString().equals("return false;")
					|| blockTree.getStatements().get(0).toString().equals("return 0.0")) {
					
					JBlock block = blocks.get(0);
					block._return(direct(blockTree.getStatements().get(0).toString()));
					return true;
					
				} else {
					throw new ActionProcessingException(
							"A return block for an action can only contain a default value or none for \"void\" methods."
							+ "Error in: " + blockTree.getStatements().get(0)
					);
				}
			}
		}
		
		boolean isParallelBlock = true;
		
		if (processingTry) {
			JBlock block = blocks.get(0);
			
			//Create the runnables for the Try
			BlockTree finallyBlock = currentTry.getFinallyBlock();
			if (finallyBlock != null) {
				JDefinedClass anonymousRunnable = getCodeModel().anonymousClass(Runnable.class);
				JMethod anonymousRunnableRun = anonymousRunnable.method(JMod.PUBLIC, getCodeModel().VOID, "run");
				anonymousRunnableRun.annotate(Override.class);
				JBlock runnableBlock = anonymousRunnableRun.body();
				
				block.decl(
						JMod.FINAL, 
						getJClass(Runnable.class.getCanonicalName()), 
						"tryFinallyRunnable",
						_new(anonymousRunnable)
					);
				addAdditionalBlock(runnableBlock, "finallyBlock:");
			}
			
			List<CatchTree> catches = new ArrayList<>(currentTry.getCatches());
			Collections.reverse(catches);
			
			for (CatchTree catchTree : catches) {
				String className = catchTree.getParameter().getType().toString();
				if (className.contains(".")) {
					className = className.substring(className.indexOf('.') + 1);
				}
				
				String variableClass = variableClassFromImports(catchTree.getParameter().getType().toString());				
				final AbstractJClass catchClass = getJClass(variableClass);
				final String catchName = catchTree.getParameter().getName().toString();
				
				if (!isValidating()) {
					try {
						JDefinedClass onErrorClass = holder.getGeneratedClass()._class(JMod.ABSTRACT | JMod.STATIC, className + "OnError");					
						JMethod onErrorMethod = onErrorClass.method(JMod.PUBLIC | JMod.ABSTRACT, getCodeModel().VOID, "onError");
						onErrorMethod.param(catchClass, catchName);
					} catch (JClassAlreadyExistsException e) {}					
				}
				
				AbstractJClass onErrorClass = getJClass(className + "OnError");
				
				JDefinedClass anonymousOnError = getCodeModel().anonymousClass(onErrorClass);
				JMethod anonymousOnErrorMethod = anonymousOnError.method(JMod.PUBLIC, getCodeModel().VOID, "onError");
				anonymousOnErrorMethod.annotate(Override.class);
				anonymousOnErrorMethod.param(JMod.FINAL, catchClass, catchName);
				JBlock runnableBlock = anonymousOnErrorMethod.body();
				
				block.decl(
						JMod.FINAL, 
						onErrorClass, 
						"catch" + className + "OnError",
						_new(anonymousOnError)
					);
				addAdditionalBlock(runnableBlock, "catchBlock: " + catchTree.getParameter());
			}
			
			JBlock tryBlock = checkTryForBlock(block, true);
			pushBlock(tryBlock, "newTry: ");
			
			isParallelBlock = false;
		}
	
		if (!processStarted) {
			if (showDebugInfo()) {
				System.out.println(debugPrefix() + "MethodStart: " + element);
				debugIndex = debugIndex + "    ";				
			} 
			isParallelBlock = false;
		}
		
		boolean finishCurrentBlock = true;
		if (omitParallel) {
			omitParallel = false;
			finishCurrentBlock = false;
			isParallelBlock = false;
		} else {
			//Current Action Information for the new Block
			currentAction.add(0, null);
			currentBuildInvocation.add(0, null);
			currentBuildParams.add(0, new LinkedHashMap<String, ParamInfo>());			
		}

		if (hasAdditionalBlock()) {
			pushAdditionalBlock();
			isParallelBlock = false;
		}
				
		//Used for execution of actions in parallel
		if (isParallelBlock) {			
			writePreviousStatements();
			
			JBlock block = blocks.get(0);			
			pushBlock(block.block(), "newBlock: ");
		}
		
		processStarted = true;
		
		Boolean result = super.visitBlock(blockTree, tree); 
		
		if (finishCurrentBlock) {
			finishBlock();
		}
		
		return result;
	}
	
	private boolean hasAdditionalBlock() {
		List<BlockDescription> descriptions = additionalBlocks.get(blocks.size());
		return descriptions != null;
	}
	
	private void addAdditionalBlock(JBlock block, String blockName) {
		BlockDescription blockDescription = new BlockDescription(block, blockName);
		
		List<BlockDescription> descriptions = additionalBlocks.get(blocks.size());
		if (descriptions == null) {
			descriptions = new LinkedList<>();
			additionalBlocks.put(blocks.size(), descriptions);
		}
		
		descriptions.add(blockDescription);
	}
	
	private void removeAdditionalBlock() {
		List<BlockDescription> descriptions = additionalBlocks.get(blocks.size());
		if (descriptions != null) {
			descriptions.remove(descriptions.size()-1);
			
			if (descriptions.size() == 0) {
				additionalBlocks.remove(blocks.size());
			}
		}
	}
	
	private void pushAdditionalBlock() {
		List<BlockDescription> descriptions = additionalBlocks.get(blocks.size());
		if (descriptions != null) {
			BlockDescription blockDescription = descriptions.get(descriptions.size()-1);
			removeAdditionalBlock();
			pushBlock(blockDescription.block, blockDescription.description);
		}
		
	}
	
	private void pushCompleteBlock(JBlock block, String blockName) {
		pushBlock(block, blockName);
		
		//Current Action Information for the new Block
		currentAction.add(0, null);
		currentBuildInvocation.add(0, null);
		currentBuildParams.add(0, new LinkedHashMap<String, ParamInfo>());
	}
	
	private void pushBlock(JBlock block, String blockName) {
		blocks.add(0, block);
		originalBlocks.add(0, block);
		
		if (showDebugInfo() && blockName != null) {
			System.out.println(debugPrefix() + blockName);
			debugIndex = debugIndex + "    ";	
		}
	}
	
	private void popBlock() {
		if (showDebugInfo()) {
			debugIndex = debugIndex.substring(0, debugIndex.length()-4);
			System.out.println(debugPrefix() + "end");
		}		
		
		blocks.remove(0);
		originalBlocks.remove(0);
	}
	
	private void finishBlock() {
		writePreviousStatements();						
		buildPreviousAction();
		
		if (actionsTree.size() > 0) {
			actionsTree.remove(actionsTree.size()-1);
			actionsTreeAfterExecute.remove(actionsTreeAfterExecute.size()-1);
		}
		
		currentAction.remove(0);
		currentBuildInvocation.remove(0);
		currentBuildParams.remove(0);
		
		popBlock();
		
		processingTry = false;
		
		//If the method concluded, populate it
		if (currentAction.size() == 0) {
			
			//Crate the parameter variables
			if (delegatingMethodStart != null) {
				
				for (JVar param : delegatingMethod.listParams()) {
					
					String paramName = param.name();
					if (paramName.startsWith("$")) {
						paramName = paramName.substring(1);
					}
					
					if (sharedVariablesHolder != null) {
						sharedVariablesHolder.field(JMod.NONE, param.type(), paramName, param);
					} else {
						delegatingMethodStart.decl(param.type(), paramName, param);
					}
				}
				
			} 
			
			delegatingMethodBody.add(initialBlock);
			if (delegatingMethodEnd != null) {
				delegatingMethodBody.add(delegatingMethodEnd);
			}
		}	
	}

	private List<ExpressionStatementTree> statementsFromCode(String code) {
		List<ExpressionStatementTree> statements = new LinkedList<>();
		
		//Write line by line to format better the text
		try {
			BufferedReader bufReader = new BufferedReader(new StringReader(code));
			
			String line = null;
			while((line=bufReader.readLine()) != null) {
				statements.add(new StringExpressionStatement(line));
			}
		} catch (Exception e) {}
		
		return statements;
	}
	
	private void writeDirectStructure(String code) {
		statements.addAll(statementsFromCode(code));		
	}
	
	@Override
	public Boolean visitTry(TryTree tryTree, Trees trees) {
		
		if (ignoreActions) return super.visitTry(tryTree, trees);
			
		writeDirectStructure(tryTree.toString());
		
		ignoreActions = true;
		Boolean result = super.visitTry(tryTree, trees);
		ignoreActions = false;
		
		return result;
		
//		if (visitingTry) {
//			throw new ActionProcessingException(
//					"Nested try are not supported: " + tryTree
//				);
//		}
//		
//		writePreviousStatements();
//		
//		currentTry = tryTree;
//		processingTry = true;
//		
//		visitingTry = true;		
//		Boolean result = super.visitTry(tryTree, trees);
//		visitingTry = false;
//		
//		return result;
	}
	
	@Override
	public Boolean visitCatch(CatchTree catchTree, Trees trees) {
		visitingCatch = true;
		Boolean result = super.visitCatch(catchTree, trees);
		visitingCatch = false;
		return result;
	}
	
	private JBlock checkTryForBlock(JBlock block, boolean firstTry) {
		
		if (processingTry) {
			JTryBlock tryBlock = block._try();
			
			BlockTree finallyBlock = currentTry.getFinallyBlock();
			if (finallyBlock != null && firstTry) {
				tryBlock._finally().invoke(ref("tryFinallyRunnable"), "run");
			}
			
			if (currentTry.getCatches() != null && currentTry.getCatches().size() > 0) {
				JCatchBlock catchBlock = tryBlock._catch(getClasses().THROWABLE);
				JVar e = catchBlock.param("e");
				
				JBlock conditions = catchBlock.body();
				for (CatchTree catchTree : currentTry.getCatches()) {
					String className = catchTree.getParameter().getType().toString();
					if (className.contains(".")) {
						className = className.substring(className.indexOf('.') + 1);
					}
					
					String variableClass = variableClassFromImports(catchTree.getParameter().getType().toString());

					//It is used conditionals here to catch the errors, cause' the try-catch
					//can be placed in a block from where a specific exception is not thrown,
					//and java will not compile in that situation (this is why conditionals and not
					//catch statements are used).
					JConditional ifInstance = conditions._if(e._instanceof(getJClass(variableClass)));
					ifInstance._then().invoke(ref("catch" + className + "OnError"), "onError")
					                  .arg(cast(getJClass(variableClass), e));
					
					conditions = ifInstance._else();
				}
				
				//TODO if the error is not handled, it should be thrown someway
				conditions.directStatement("//TODO Throw the exception");
				conditions.directStatement("//throw e;");
				//conditions._throw(e);
			}
			
			return tryBlock.body();
		}
		
		return block;
	}
	
	@Override
	public Boolean visitSwitch(SwitchTree switchTree, Trees trees) {
		if (ignoreActions) return super.visitSwitch(switchTree, trees);
		
		writePreviousStatements();
		
		String switchExpression = switchTree.getExpression().toString();
		scan(switchTree.getExpression(), trees);
		
		if (showDebugInfo()) {
			System.out.println(debugPrefix() + "newSwitch: " + switchExpression);
			debugIndex = debugIndex + "    ";
		}
		
		JBlock block = blocks.get(0);
		JSwitch _switch = block._switch(direct(parseForSpecials(switchExpression, false)));
		
		for (CaseTree caseTree : switchTree.getCases()) {
			//Default statement
			if (caseTree.getExpression() == null) {
				JBlock defaultBlock = _switch._default().body();
				
				pushCompleteBlock(defaultBlock, "defaultBlock:");
				scan(caseTree.getStatements(), trees);
				finishBlock();
				
				continue;
			}
			
			String caseExpression = caseTree.getExpression().toString();
			
			literalDiscovered = null;
			scan(caseTree.getExpression(), trees);
			
			IJExpression expr;
			if (literalDiscovered != null || caseExpression.contains(".")) {
				expr = direct(caseExpression);
			} else {
				expr = ref(caseExpression);
			}
			
			JBlock caseBlock = _switch._case(expr).body();
			
			pushCompleteBlock(caseBlock, "caseBlock: " + caseExpression);
			scan(caseTree.getStatements(), trees);
			finishBlock();
		}
		
		return true;
	}
	
	@Override
	public Boolean visitLiteral(LiteralTree literalTree, Trees trees) {
		literalDiscovered = literalTree;
		return super.visitLiteral(literalTree, trees);
	}
	
	@Override
	public Boolean visitBreak(BreakTree breakTree, Trees trees) {
		if (ignoreActions) return super.visitBreak(breakTree, trees);
		
		writePreviousStatements();
		if (currentAction.get(0) != null) {
			buildPreviousAction();
			currentAction.set(0, null);
			blocks.set(0, originalBlocks.get(0));
		} 
		
		JBlock block = blocks.get(0);
		block._break();
		
		return super.visitBreak(breakTree, trees);
	}
	
	@Override
	public Boolean visitLabeledStatement(LabeledStatementTree labeled, Trees trees) {
		statements.add(new StringExpressionStatement(labeled.getLabel().toString() + ":"));
		return super.visitLabeledStatement(labeled, trees);
	}
		
	@Override
	public Boolean visitForLoop(ForLoopTree forLoop, Trees trees) {
		if (ignoreActions) return super.visitForLoop(forLoop, trees);
				
		writeDirectStructure(forLoop.toString());
		
		ignoreActions = true;
		Boolean result = super.visitForLoop(forLoop, trees);
		ignoreActions = false;
		
		return result;
	}
	
	@Override
	public Boolean visitEnhancedForLoop(EnhancedForLoopTree forLoop, Trees trees) {
		if (ignoreActions) return super.visitEnhancedForLoop(forLoop, trees);
				
		writeDirectStructure(forLoop.toString());
		
		ignoreActions = true;
		Boolean result = super.visitEnhancedForLoop(forLoop, trees);
		ignoreActions = false;
		
		return result;
	}
	
	@Override
	public Boolean visitWhileLoop(WhileLoopTree whileLoop, Trees trees) {
		if (ignoreActions) return super.visitWhileLoop(whileLoop, trees);
				
		writeDirectStructure(whileLoop.toString());
		
		ignoreActions = true;
		Boolean result = super.visitWhileLoop(whileLoop, trees);
		ignoreActions = false;
		
		return result;
	}
	
	@Override
	public Boolean visitDoWhileLoop(DoWhileLoopTree doWhileLoop, Trees trees) {
		if (ignoreActions) return super.visitDoWhileLoop(doWhileLoop, trees);
				
		writeDirectStructure(doWhileLoop.toString());
		
		ignoreActions = true;
		Boolean result = super.visitDoWhileLoop(doWhileLoop, trees);
		ignoreActions = false;
		
		return result;
	}
	
	@Override
	public Boolean visitMethod(MethodTree arg0, Trees arg1) {
		// TODO Auto-generated method stub
		return super.visitMethod(arg0, arg1);
	}
	
	@Override
	public Boolean visitClass(ClassTree cls, Trees trees) {
		if (ignoreActions) return super.visitClass(cls, trees); 
		
		ignoreActions = true;
		anonymousClassTree = cls;
		Boolean result = super.visitClass(cls, trees);
		ignoreActions = false;
		
		return result;
	}
	
	private void addAnonymousStatements(final String expression) {
		
		if (anonymousClassTree != null) {
			StatementTree lastStatement = statements.get(statements.size()-1);
			statements.remove(statements.size()-1);
						
			int anonymousStart = expression.indexOf("new " + anonymousClassTree.getSimpleName());
			String start = expression.substring(0, anonymousStart);
			
			if (lastStatement instanceof VariableTree) {
				statements.add(new VariableExpressionWithoutInitializer((VariableTree) lastStatement));
				
				Matcher matcher = Pattern.compile(((VariableTree) lastStatement).getName() + "\\s*=").matcher(expression);
				if (matcher.find()) {
					start = start.substring(matcher.start());
				}
			}
			
			int anonymousEnd = 0;
			String end = null;
			
			int curlyBracketCount = 0;
			for (int i = anonymousStart; i < expression.length(); i++) {
				if (expression.charAt(i) == '{') curlyBracketCount++;
				
				if (expression.charAt(i) == '}') {
					curlyBracketCount--;
					
					if (curlyBracketCount == 0) {
						anonymousEnd = i+1;
						end = expression.substring(anonymousEnd);
						break;
					}
				}				
			}
			
			if (!start.equals("")) {
				statements.add(new StringExpressionStatement(start));
			}
			
			List<ExpressionStatementTree> anonymouseStatements = 
					statementsFromCode(expression.substring(anonymousStart, anonymousEnd));
			for (ExpressionStatementTree statement : anonymouseStatements) {
				((StringExpressionStatement)statement).setIgnoreThis();
			}
			
			statements.addAll(anonymouseStatements);
			
			if (end.equals("") && !expression.endsWith(";")) {
				end = ";";
			}
			
			if (!end.equals("")) {
				statements.add(new StringExpressionStatement(end));
			}
			
			anonymousClassTree = null;
		}
	}
	
	@Override
	public Boolean visitSynchronized(SynchronizedTree sync, Trees trees) {
		if (ignoreActions) return super.visitSynchronized(sync, trees);
				
		writeDirectStructure(sync.toString());
		
		ignoreActions = true;
		Boolean result = super.visitSynchronized(sync, trees);
		ignoreActions = false;
		
		return result;
	}
	
	private List<String> processArguments(String methodName, MethodInvocationTree invocation,  JVar action, ActionInfo actionInfo) {
		Pattern patternForStringLiterals = Pattern.compile("\"((?:\\\\\"|[^\"])*?)\"");
		
		List<String> arguments = new LinkedList<>();
		boolean matchFound = false;
		
		List<ActionMethod> methods = actionInfo.methods.get(methodName);
		if (methods == null) {
			throw new ActionProcessingException(
					"Method \"" + methodName + "\" not found for action " + invocation
				);
		}		
		
		for (ActionMethod method : methods) {
			
			List<ActionMethodParam> params = method.params;
			
			if (invocation.getArguments().size() == params.size()) {
				
				method.metaData = new HashMap<>();
				
				for (int i = 0; i < params.size(); i++) {
					final ActionMethodParam param = params.get(i);
					
					arguments.clear();
					param.metaData = new HashMap<>();

					String currentParam = invocation.getArguments().get(i).toString();
					param.metaData.put("value", currentParam);
					
					boolean useArguments = false;
					for (Annotation annotation : param.annotations) {					
						
						//Literal Expressions
						if (annotation instanceof Literal) {
							boolean finded = false;			
							Matcher matcher = patternForStringLiterals.matcher(currentParam);
							
							while (matcher.find()) {
								finded = true;
								
								String matched = matcher.group(0);
								if (!matched.equals(currentParam)) {
									throw new ActionProcessingException("You should provide a literal value for \"fields\" in action " + invocation);
								}
								
								String literalStringValue = matcher.group(1);
								
								IJExpression exp = FormatsUtils.expressionFromString(literalStringValue);
								param.metaData.put("literalExpression", exp);
								param.metaData.put("literal", literalStringValue);
							}

							if (!finded) {
								throw new ActionProcessingException("You should provide a literal value for \"fields in action invocation");
							}
						}
						
						//Formatted Expressions
						if (annotation instanceof FormattedExpression) {

							boolean finded = false;			
							Matcher matcher = patternForStringLiterals.matcher(currentParam);
							
							while (matcher.find()) {
								finded = true;
								
								String matched = matcher.group(0);
								String literalStringValue = matcher.group(1);
								
								IJExpression exp = FormatsUtils.expressionFromString(literalStringValue);
								
								currentParam = currentParam.replace(matched, expressionToString(exp));						
							}

							if (finded) {
								arguments.add(parseForSpecials(currentParam, false));
								param.metaData.put("value", currentParam);								
								currentParam = null;
								useArguments = true;								
							}

						} 
						
						else if (annotation instanceof Assignable) {
							@SuppressWarnings("unchecked")
							List<IJStatement> postBuildBlocks = (List<IJStatement>) actionInfo.metaData.get("postBuildBlocks");
							if (postBuildBlocks == null) {
								postBuildBlocks = new LinkedList<>();
								actionInfo.metaData.put("postBuildBlocks", postBuildBlocks);
							}
							
							postBuildBlocks.add(assign(ref(currentParam), action.invoke(((Assignable)annotation).value())));
						}
						
						else if (annotation instanceof Field) {						
							
							Element fieldElement = findField(element.getEnclosingElement(), currentParam);
							
							if (fieldElement != null) {
								param.metaData.put("field", fieldElement);
								param.metaData.put("fieldName", fieldElement.getSimpleName().toString());
								
								String fieldClass = TypeUtils.typeFromTypeString(fieldElement.asType().toString(), env);
								param.metaData.put("fieldClass", fieldClass);
								param.metaData.put("fieldJClass", getJClass(fieldClass));
							} else {
								throw new ActionProcessingException(
										"There's no an accesible field named: " + currentParam + " in " + invocation
									);
							}
						}
						
						if (currentParam != null) {
							arguments.add(parseForSpecials(currentParam, false));
						}
					}
					
					if (useArguments) return arguments;
				}
			} 
		}
		
		if (!matchFound) {
			arguments.clear();
			for (ExpressionTree arg : invocation.getArguments()) {
				arguments.add(parseForSpecials(arg.toString(), false));
			}
		}
		
		return arguments;
	}
	
	private Element findField(Element element, String fieldName) {
		
		List<? extends Element> elems = element.getEnclosedElements();
		for (Element elem : elems) {
			if (elem.getKind() == ElementKind.FIELD) {
				if (elem.getModifiers().contains(Modifier.PRIVATE)) continue;
				
				if (elem.getSimpleName().toString().equals(fieldName)) {
					return elem;
				}
			}
		}
		
		final ProcessingEnvironment env = this.env.getProcessingEnvironment();
		
		//Apply to Extensions
		List<? extends TypeMirror> superTypes = env.getTypeUtils().directSupertypes(element.asType());
		for (TypeMirror type : superTypes) {
			TypeElement superElement = env.getElementUtils().getTypeElement(type.toString());
			
			Element elem = findField(superElement, fieldName);
			if (elem != null) return elem;
		}
		
		return null;

	}
	
	private String expressionToString(IJExpression expression) {
	    if (expression == null) {
	        throw new IllegalArgumentException("Generable must not be null.");
	    }
	    final StringWriter stringWriter = new StringWriter();
	    final JFormatter formatter = new JFormatter(stringWriter);
	    expression.generate(formatter);
	    
	    return stringWriter.toString();
	}
	
	private String variableClassFromImports(final String variableClass) {
		return this.variableClassFromImports(variableClass, false);
	}
	
	private String variableClassFromImports(final String variableClass, boolean ensureImport) {
		
		for (ImportTree importTree : imports) {
			String lastElementImport = importTree.getQualifiedIdentifier().toString();
			String firstElementName = variableClass;
			String currentVariableClass = "";
			
			int pointIndex = lastElementImport.lastIndexOf('.');
			if (pointIndex != -1) {
				lastElementImport = lastElementImport.substring(pointIndex + 1);
			}
			
			pointIndex = firstElementName.indexOf('.');
			if (pointIndex != -1) {
				firstElementName = firstElementName.substring(0, pointIndex);
				currentVariableClass = variableClass.substring(pointIndex);
			}
			
			while (firstElementName.endsWith("[]")) {
				firstElementName = firstElementName.substring(0, firstElementName.length()-2);
				if (currentVariableClass.isEmpty()) currentVariableClass = currentVariableClass + "[]";
			}
			
			if (lastElementImport.equals(firstElementName)) {
				
				if (!isValidating() && ensureImport) {
					EnsureImportsHolder importsHolder = holder.getPluginHolder(new EnsureImportsHolder(holder));
					if (!importTree.isStatic()) {						
						importsHolder.ensureImport(importTree.getQualifiedIdentifier().toString());							
					} else {
						staticImports.put(lastElementImport, importTree.getQualifiedIdentifier().toString());
					}
				}

				return importTree.getQualifiedIdentifier() + currentVariableClass;
			}
		}
		
		return variableClass;
	}
		
	private AbstractJClass getJClass(String clazz) {
		return env.getJClass(clazz);
	}
		
	private JCodeModel getCodeModel() {
		return env.getCodeModel();
	}
	
	private Classes getClasses() {
		return env.getClasses();
	}

	private class VariableExpressionWithoutInitializer implements VariableTree {

		VariableTree variableTree;
		
		public VariableExpressionWithoutInitializer(VariableTree variableTree) {
			this.variableTree = variableTree;
		}
		
		@Override
		public <R, D> R accept(TreeVisitor<R, D> arg0, D arg1) {
			return variableTree.accept(arg0, arg1);
		}

		@Override
		public Kind getKind() {
			return variableTree.getKind();
		}

		@Override
		public ExpressionTree getInitializer() {
			return null;
		}

		@Override
		public ModifiersTree getModifiers() {
			return variableTree.getModifiers();
		}

		@Override
		public Name getName() {
			return variableTree.getName();
		}

		@Override
		public Tree getType() {
			return variableTree.getType();
		}
		
	}
	
	private class StringExpressionStatement implements ExpressionStatementTree {

		String statement;
		boolean ignoreThis;
		
		public StringExpressionStatement(String statement) {
			this.statement = statement;
		}
		
		public void setIgnoreThis() {
			this.ignoreThis = true;
		}
		
		public boolean ignoreThis() {
			return this.ignoreThis;
		}
		
		@Override
		public Kind getKind() {
			return null;
		}
		
		@Override
		public <R, D> R accept(TreeVisitor<R, D> arg0, D arg1) {
			return null;
		}
		
		@Override
		public ExpressionTree getExpression() {
			return null;
		}
		
		@Override
		public String toString() {
			return statement;
		}
		
	}
	
	private class ParamInfo {
		ActionMethodParam param;
		IJExpression assignment;
		JBlock runnableBlock;
		
		public ParamInfo(ActionMethodParam param, JBlock runnableBlock, IJExpression assignment) {
			super();
			this.param = param;
			this.runnableBlock = runnableBlock;
			this.assignment = assignment;
		}		
	}
	
	private class BlockDescription {
		JBlock block;
		String description;
		
		public BlockDescription(JBlock block, String description) {
			super();
			this.block = block;
			this.description = description;
		}
		
	}
	
	private static class ActionDetectedException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
	
	private static class ActionProcessingException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public ActionProcessingException(String message) {
			super(message);
		}
		
	}
	
	public static class ActionCallSuperException extends ActionProcessingException {

		private static final long serialVersionUID = 1L;
		private Element element;
		
		public ActionCallSuperException(Element element) {
			super("");
			this.element = element;
		}
		
		public Element getElement() {
			return element;
		}
		
	}

}
