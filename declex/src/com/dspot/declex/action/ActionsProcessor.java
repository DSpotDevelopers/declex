/**
 * Copyright (C) 2016-2018 DSpot Sp. z o.o
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

import com.dspot.declex.action.builder.ActionsBuilder;
import com.dspot.declex.action.exception.ActionCallSuperException;
import com.dspot.declex.action.exception.ActionDetectedException;
import com.dspot.declex.action.exception.ActionProcessingException;
import com.dspot.declex.action.util.ActionsLogger;
import com.dspot.declex.action.builder.ActionsMethodBuilder;
import com.dspot.declex.action.util.ExpressionsHelper;
import com.helger.jcodemodel.*;
import com.sun.source.tree.*;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.internal.process.ProcessHolder.Classes;
import org.androidannotations.internal.virtual.VirtualElement;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.helger.jcodemodel.JExpr.*;

public class ActionsProcessor extends TreePathScanner<Boolean, Trees> {

    private ActionsBuilder actionsBuilder;

	private ActionsMethodBuilder methodBuilder;

	private ActionsLogger logger;

    private ExpressionsHelper expressionsHelper;

	private List<String> methodActionParamNames = new LinkedList<>();
	
	private LiteralTree literalDiscovered;
	
	private boolean processingTry;
	private boolean visitingTry;
	private TryTree currentTry;
	private boolean visitingCatch;
	
	private boolean omitParallel;
	
	private boolean isVisitingVariable;
	
	private AndroidAnnotationsEnvironment env;
	private EComponentHolder holder;
	private Element element;

	private final CompilationUnitTree compilationUnit;
	
	private boolean ignoreActions;
	private ClassTree anonymousClassTree;
	
	private ElementValidation valid;
	
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
    	
    	//This means the element is Virtual and was already processed (ex. Imported Method)
    	if (treePath == null) return false;
    	
    	//Check if the Action Api was activated for this compilation unit
    	for (ImportTree importTree : treePath.getCompilationUnit().getImports()) {
    		
            if (Actions.getInstance().isAction(importTree.getQualifiedIdentifier().toString())) {

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
			if (superElement == null) continue;
			
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

        this.logger = new ActionsLogger(isValidating(), env);
        this.actionsBuilder = new ActionsBuilder(isValidating(), element, holder, logger, env);
        this.expressionsHelper = new ExpressionsHelper(isValidating(), actionsBuilder, holder, treePath);
		this.methodBuilder = new ActionsMethodBuilder(isValidating(), logger, env);

		methodBuilder.setExpressionsHelper(expressionsHelper);
		actionsBuilder.setExpressionsHelper(expressionsHelper);
		actionsBuilder.setMethodBuilder(methodBuilder);

		compilationUnit = treePath.getCompilationUnit();

		if (overrideAction.contains(element)) {
			actionsBuilder.buildActionMethod(true);
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
		
		methodBuilder.pushInitialBlock();

		if (!isValidating()) {
            logger.info("PROCESSING: " + holder.getAnnotatedElement());
        }
	}
	
	private boolean isValidating() {
		return valid != null;
	}
	
	@Override
	public Boolean visitVariable(VariableTree variable, Trees trees) {

        final int sourcePosition = (int) trees.getSourcePositions().getStartPosition(compilationUnit, variable);
        if (actionsBuilder.searchAction(variable, sourcePosition)) {
            return visitMethodInvocation(null, trees);
        }
		
		if (ignoreActions) return super.visitVariable(variable, trees);
		
		//Ignore variables out of the method
		if (!actionsBuilder.doesProcessStarted()) return super.visitVariable(variable, trees);
		
		if (visitingCatch) {
			visitingCatch = false;
			return super.visitVariable(variable, trees);
		}

		methodBuilder.addStatement(variable);

		isVisitingVariable = true;
		Boolean result = super.visitVariable(variable, trees);
		addAnonymousStatements(variable.toString());
		isVisitingVariable = false;
		
		return result;
	}
	
	@Override
	public Boolean visitAssignment(AssignmentTree assignment, Trees trees) {
		if (!isVisitingVariable) actionsBuilder.registerAssignment(assignment);
		return super.visitAssignment(assignment, trees);
	}
	
	@Override
	public Boolean visitExpressionStatement(ExpressionStatementTree expr, Trees trees) {
		
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

		methodBuilder.addStatement(expr);
        actionsBuilder.clearAssignment();

		Boolean result = super.visitExpressionStatement(expr, trees);
		addAnonymousStatements(expr.toString());
		
		return result;
	}
	
	@Override
	public Boolean visitReturn(ReturnTree returnTree, Trees trees) {

	    if (ignoreActions) return super.visitReturn(returnTree, trees);

        if (actionsBuilder.hasActionFinished(returnTree)) {
		    methodBuilder.addStatement(returnTree.toString());
		}
		
		Boolean result = super.visitReturn(returnTree, trees);
		addAnonymousStatements(returnTree.toString());
		
		return result;
	}
	
	@Override
	public Boolean visitIdentifier(IdentifierTree id, Trees trees) {
		
		//This will happen with identifiers prior to the main block (ex. Annotations)
		if (!actionsBuilder.doesProcessStarted()) return true;
				
		final String idName = id.toString();
		
		//If it is used one of the method parameters, then use sharedVariablesHolder
		if ((actionsBuilder.hasPendingAction() || anonymousClassTree != null) && methodActionParamNames.contains(idName)) {

		    methodBuilder.needsSharedVariablesHolder();
		}
				
		//Accessing Action from within unsupported structure
		if (Actions.getInstance().hasActionNamed(idName) && ignoreActions) {
			throw new ActionProcessingException(
					"Action is not supported in this location: " + idName
				);
		}

		actionsBuilder.clearActionSearch();
			
		//Ensure import of all the identifiers
		expressionsHelper.variableClassFromImports(idName, true);
		
		return super.visitIdentifier(id, trees);
	}

	@Override
	public Boolean visitEmptyStatement(EmptyStatementTree arg0, Trees trees) {
		if (ignoreActions) return super.visitEmptyStatement(arg0, trees);

		logger.info("empty");

		return super.visitEmptyStatement(arg0, trees);
	}
	
	@Override
	public Boolean visitIf(IfTree ifTree, Trees trees) {
		if (ignoreActions) return super.visitIf(ifTree, trees);
		
		methodBuilder.buildStatements();
		
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
				
				if (actionName.equals(actionsBuilder.getCurrentAction())) {

					ActionsBuilder.ParamInfo thenParam = null, elseParam = null;
					
					for (ActionsBuilder.ParamInfo paramInfo : actionsBuilder.getCurrentBuildParameters()) {
						
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
					    actionsBuilder.pushSelector(actionName + "." + actionSelector + "().", actionName, actionSelector, thenParam);
						scanForActionsOmittingBlock(ifTree.getThenStatement(), trees);
						finishBlock();
						actionsBuilder.popSelector();
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

						actionsBuilder.pushSelector(actionName + "." + actionSelector + "().", actionName, elseParam.param.name, elseParam);
						scanForActionsOmittingBlock(ifTree.getElseStatement(), trees);
						finishBlock();
                        actionsBuilder.popSelector();
					}						
					
				} else {
					throw new ActionProcessingException(
							"Action Selector out of context: " + matcher.group(1) 
							+ " in if(" + ifCondition + "). The current action is " + actionsBuilder.getCurrentAction() + "."
						);
				}
			
				return true;
				
			}				
		}
		
		JConditional _if = methodBuilder.getCurrentBlock()._if(expressionsHelper.stringToExpression(ifCondition));
		JBlock thenBlock = _if._then();
		
		pushCompleteBlock(thenBlock, "newIf: " + ifCondition);
		scanForActionsOmittingBlock(ifTree.getThenStatement(), trees);
		finishBlock();
		
		if (ifTree.getElseStatement() != null) {
			JBlock elseBlock = _if._else();
			pushCompleteBlock(elseBlock, "elseBlock:");
			scanForActionsOmittingBlock(ifTree.getElseStatement(), trees);
			finishBlock();
		}

		return true;
	}
	
	//Method should be called only after a pushCompleteBlock
	private void scanForActionsOmittingBlock(Tree expression, Trees trees) {
		if (expression.getKind().equals(Kind.BLOCK)) {
			omitParallel = true;
		}
		scan(expression, trees);
	}
	
	@Override
	public Boolean visitMethodInvocation(MethodInvocationTree invoke, Trees trees) {
		
		if (ignoreActions) {
			return super.visitMethodInvocation(invoke, trees);
		}

		final int sourcePosition = (int) trees.getSourcePositions().getStartPosition(compilationUnit, invoke);
		if (actionsBuilder.searchAction(invoke, sourcePosition, isVisitingVariable)) {
            finishBlock();
		    return true;
        }
		
		return super.visitMethodInvocation(invoke, trees);
	}
	
	@Override
	public Boolean visitBlock(BlockTree blockTree, Trees tree) {
			
		if (ignoreActions) return super.visitBlock(blockTree, tree);

		if (actionsBuilder.hasActionFinished(blockTree)) {
		    return true;
        }
		
		boolean isParallelBlock = true;
		
		if (processingTry) {
			JBlock block = methodBuilder.getCurrentBlock();
			
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
				methodBuilder.addAdditionalBlock(runnableBlock, "finallyBlock:");
			}
			
			List<CatchTree> catches = new ArrayList<>(currentTry.getCatches());
			Collections.reverse(catches);
			
			for (CatchTree catchTree : catches) {
				String className = catchTree.getParameter().getType().toString();
				if (className.contains(".")) {
					className = className.substring(className.indexOf('.') + 1);
				}
				
				String variableClass = expressionsHelper.variableClassFromImports(catchTree.getParameter().getType().toString());
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
                methodBuilder.addAdditionalBlock(runnableBlock, "catchBlock: " + catchTree.getParameter());
			}
			
			JBlock tryBlock = checkTryForBlock(block, true);
            methodBuilder.pushBlock(tryBlock, "newTry: ");
			
			isParallelBlock = false;
		}
	
		if (!actionsBuilder.doesProcessStarted()) {

		    logger.info("MethodStart: " + element);
		    logger.increaseIndex();

			isParallelBlock = false;

		}
		
		boolean finishCurrentBlock = true;
		if (omitParallel) {
			omitParallel = false;
			finishCurrentBlock = false;
			isParallelBlock = false;
		} else {
			//Current Action Information for the new Block
            actionsBuilder.pushAction();
		}

		if (methodBuilder.hasAdditionalBlock()) {
            methodBuilder.pushAdditionalBlock();
			isParallelBlock = false;
		}
				
		//Used for execution of actions in parallel
		if (isParallelBlock) {			
			methodBuilder.buildStatements();
			
			JBlock block = methodBuilder.getCurrentBlock();
            methodBuilder.pushBlock(block.block(), "newBlock: ");
		}

		actionsBuilder.startProcess();
		
		Boolean result = super.visitBlock(blockTree, tree); 
		
		if (finishCurrentBlock) {
			finishBlock();
		}
		
		return result;
	}

    private void pushCompleteBlock(JBlock block, String blockName) {
        methodBuilder.pushBlock(block, blockName);
        actionsBuilder.pushAction();
    }
	
	private void finishBlock() {

		methodBuilder.buildStatements();
		actionsBuilder.buildAction();

        methodBuilder.popBlock();
        actionsBuilder.popAction();

		processingTry = false;

	}
	
	@Override
	public Boolean visitTry(TryTree tryTree, Trees trees) {
		
		if (ignoreActions) return super.visitTry(tryTree, trees);
			
		methodBuilder.addStatementBlock(tryTree.toString());
		
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
//		buildStatements();
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
					
					String variableClass = expressionsHelper.variableClassFromImports(catchTree.getParameter().getType().toString());

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
		
		methodBuilder.buildStatements();
		
		String switchExpression = switchTree.getExpression().toString();
		scan(switchTree.getExpression(), trees);

		logger.info("newSwitch: " + switchExpression);
		logger.increaseIndex();

		JBlock block = methodBuilder.getCurrentBlock();
		JSwitch _switch = block._switch(expressionsHelper.stringToExpression(switchExpression));
		
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
		
		methodBuilder.buildStatements();
		if (actionsBuilder.getCurrentAction() != null) {
			actionsBuilder.buildAction();
			actionsBuilder.setCurrentAction(null);
			methodBuilder.setCurrentBlock(methodBuilder.getCurrentOriginalBlock());
		}
		
		JBlock block = methodBuilder.getCurrentBlock();
		block._break();
		
		return super.visitBreak(breakTree, trees);
	}
	
	@Override
	public Boolean visitLabeledStatement(LabeledStatementTree labeled, Trees trees) {
	    methodBuilder.addStatement(labeled.getLabel().toString() + ":");
		return super.visitLabeledStatement(labeled, trees);
	}
		
	@Override
	public Boolean visitForLoop(ForLoopTree forLoop, Trees trees) {
		if (ignoreActions) return super.visitForLoop(forLoop, trees);
				
		methodBuilder.addStatementBlock(forLoop.toString());
		
		ignoreActions = true;
		Boolean result = super.visitForLoop(forLoop, trees);
		ignoreActions = false;
		
		return result;
	}
	
	@Override
	public Boolean visitEnhancedForLoop(EnhancedForLoopTree forLoop, Trees trees) {
		if (ignoreActions) return super.visitEnhancedForLoop(forLoop, trees);
				
		methodBuilder.addStatementBlock(forLoop.toString());
		
		ignoreActions = true;
		Boolean result = super.visitEnhancedForLoop(forLoop, trees);
		ignoreActions = false;
		
		return result;
	}
	
	@Override
	public Boolean visitWhileLoop(WhileLoopTree whileLoop, Trees trees) {
		if (ignoreActions) return super.visitWhileLoop(whileLoop, trees);
				
		methodBuilder.addStatementBlock(whileLoop.toString());
		
		ignoreActions = true;
		Boolean result = super.visitWhileLoop(whileLoop, trees);
		ignoreActions = false;
		
		return result;
	}
	
	@Override
	public Boolean visitDoWhileLoop(DoWhileLoopTree doWhileLoop, Trees trees) {
		if (ignoreActions) return super.visitDoWhileLoop(doWhileLoop, trees);
				
		methodBuilder.addStatementBlock(doWhileLoop.toString());
		
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
			StatementTree lastStatement = methodBuilder.getLastStatement();
			methodBuilder.removeLastStatement();
						
			int anonymousStart = expression.indexOf("new " + anonymousClassTree.getSimpleName());
			String start = expression.substring(0, anonymousStart);
			
			if (lastStatement instanceof VariableTree) {
			    methodBuilder.addStatementVariableWithoutInitializer((VariableTree) lastStatement);
				
				Matcher matcher = Pattern.compile(((VariableTree) lastStatement).getName() + "\\s*=").matcher(expression);
				if (matcher.find()) {
					start = start.substring(matcher.start());
				}
			}
			
			int anonymousEnd = 0;
			String end = "";
			
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
			
			if (!start.isEmpty()) {
			    methodBuilder.addStatement(start);
			}

			methodBuilder.addStatementBlock(expression.substring(anonymousStart, anonymousEnd), true);
			
			if (end.isEmpty() && !expression.endsWith(";")) {
				end = ";";
			}
			
			if (!end.equals("")) {
			    methodBuilder.addStatement(end);
			}
			
			anonymousClassTree = null;
		}
	}
	
	@Override
	public Boolean visitSynchronized(SynchronizedTree sync, Trees trees) {
		if (ignoreActions) return super.visitSynchronized(sync, trees);
				
		methodBuilder.addStatementBlock(sync.toString());
		
		ignoreActions = true;
		Boolean result = super.visitSynchronized(sync, trees);
		ignoreActions = false;
		
		return result;
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

}
