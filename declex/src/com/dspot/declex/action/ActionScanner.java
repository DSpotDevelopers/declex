package com.dspot.declex.action;

import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._null;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JExpr.assign;
import static com.helger.jcodemodel.JExpr.direct;
import static com.helger.jcodemodel.JExpr.invoke;
import static com.helger.jcodemodel.JExpr.ref;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

import org.androidannotations.helper.APTCodeModelHelper;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EBeanHolder;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;

import com.dspot.declex.api.action.annotation.Assignable;
import com.dspot.declex.api.action.annotation.Field;
import com.dspot.declex.api.action.annotation.FormattedExpression;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethod;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.dspot.declex.api.util.FormatsUtils;
import com.dspot.declex.override.util.DeclexAPTCodeModelHelper;
import com.dspot.declex.share.holder.EnsureImportsHolder;
import com.dspot.declex.util.DeclexConstant;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.IJStatement;
import com.helger.jcodemodel.JAnonymousClass;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JCodeModel;
import com.helger.jcodemodel.JConditional;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JFormatter;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;
import com.sun.source.tree.BlockTree;
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
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

public class ActionScanner extends TreePathScanner<Boolean, Trees> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ActionScanner.class);
	
	private static final boolean DEBUG = true;
	private static String debugIndex = "";

	private int actionCount = 0;
	
	private List<MethodInvocationTree> subMethods = new LinkedList<>();
	
	private List<ExpressionStatementTree> statements = new LinkedList<>();
	private List<VariableTree> variables = new LinkedList<>();
	
	private JBlock delegatingMethodBody = null;
	private List<JBlock> blocks = new LinkedList<>();
	private List<JBlock> parallelBlock = new LinkedList<>();
	
	private JBlock initialBlock = new JBlock();
	private JAnonymousClass sharedVariablesHolder = null;
	
	private boolean visitingIfCondition;
	private String currentIfCondition;
	private StatementTree elseIfCondition;
	
	private boolean visitingVariable;
	private String actionInFieldWithoutInitializer = null;
	
	private String lastMemberIdentifier;
	
	private List<String> currentAction = new LinkedList<>();
	private List<JInvocation> currentBuildInvocation = new LinkedList<>();
	private List<Map<String, ParamInfo>> currentBuildParams = new LinkedList<>(); 
	
	private Map<Integer, JBlock> additionalBlocks = new HashMap<>();
	
	private EComponentHolder holder;
	private APTCodeModelHelper codeModelHelper;
	private Element element;
	
	private List<? extends ImportTree> imports;
	
	private boolean ignoreActions;
	private ClassTree anonymousClassTree;
	private boolean processStarted = false;
	
	public static boolean processActions(Element element, EComponentHolder holder) {
		
		ActionForHandler.buildActionObject(holder.getEnvironment());
		
		final Trees tree = Trees.instance(holder.getEnvironment().getProcessingEnvironment());
    	final TreePath treePath = tree.getPath(element);
    	
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
	            			if (ActionForHandler.getActionNames().containsKey(name)) {
	            				//Identifier detected
	            				throw new IllegalStateException();
	            			}
	            			
	            			return super.visitIdentifier(id, trees);
	            		}
	            	};
	            	scanner.scan(treePath, tree);
            	
                	        		
            	} catch (IllegalStateException e) { 
            		
        			try {
                		ActionScanner scanner = new ActionScanner(element, holder, treePath);
                    	scanner.scan(treePath, tree);
        			} catch (IllegalStateException e1) {}
            		
                	return true;
            	}
            	
            	break;
            }
        }
    	
    	return false;
		
	}
	
	private ActionScanner(Element element, EComponentHolder holder, TreePath treePath) {
		
		this.element = element;
		this.holder = holder;
		this.codeModelHelper = new DeclexAPTCodeModelHelper(holder.getEnvironment());
		
		pushBlock(initialBlock, null);
		
		imports = treePath.getCompilationUnit().getImports();

	}
	
	private String debugPrefix() {
		String prefix = "";
		return prefix + debugIndex;
	}
	
	private String parseForSpecials(String expression, boolean ignoreThis) {
		String generatedClass = holder.getGeneratedClass().name();
		String annotatedClass = holder.getAnnotatedElement().getSimpleName().toString();
		
		if (!ignoreThis) {
			expression = expression.replaceAll("(?<![a-zA-Z_$.])this(?![a-zA-Z_$])", generatedClass + ".this");
		}
		
		expression = expression.replaceAll("(?<![a-zA-Z_$.])" + annotatedClass + ".this(?![a-zA-Z_$])", generatedClass + ".this");
		return expression;
	}
	
	private void writeVariable(VariableTree variable, JBlock block, IJExpression initializer) {
		if (DEBUG) System.out.println(debugPrefix() + "writeVariable: " + variable);

		//Inferred variables must start with $ sign
		final String name = variable.getName().toString();
		if (sharedVariablesHolder == null) {
			sharedVariablesHolder = getCodeModel().anonymousClass(Runnable.class);
			JMethod annonimousRunnableRun = sharedVariablesHolder.method(JMod.PUBLIC, getCodeModel().VOID, "run");
			annonimousRunnableRun.annotate(Override.class);
			
			//Add all the created code to the sharedVariablesHolder
			annonimousRunnableRun.body().add(initialBlock);
			
			initialBlock = new JBlock();
			JVar sharedVariablesHolderVar = initialBlock.decl(
					getJClass(Runnable.class.getCanonicalName()), 
					"sharedVariablesHolder", 
					_new(sharedVariablesHolder)
				);
			initialBlock.invoke(sharedVariablesHolderVar, "run");
		}
		
		String variableClass = variable.getType().toString();
		for (ImportTree importTree : imports) {
			if (importTree.getQualifiedIdentifier().toString().endsWith(variableClass)) {
				variableClass = importTree.getQualifiedIdentifier().toString();
				break;
			}
		}
						
		if (!sharedVariablesHolder.containsField(name)) {
			if (initializer != null && !name.startsWith("$")) {
				sharedVariablesHolder.field(
						JMod.NONE, 
						getJClass(variableClass), 
						name
					);
				block.assign(ref(name), initializer);
			} else {
				if (name.startsWith("$")) {
					sharedVariablesHolder.field(
							JMod.NONE, 
							getJClass(variableClass), 
							name
						);
					
					block.assign(ref(name), ref(name.substring(1)));
				} else {
					sharedVariablesHolder.field(
							JMod.NONE, 
							getJClass(variableClass), 
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
	
	private void writePreviousStatements() {
		JBlock block = blocks.get(0);
		
		//Write all the variables till this point
		for (VariableTree variable : variables) {
			IJExpression initializer = variable.getInitializer() == null? 
					                   null : direct(parseForSpecials(variable.getInitializer().toString(), false));
			writeVariable(variable, block, initializer);
		}
		variables.clear();
		
		//Write all the statements till this point
		for (ExpressionStatementTree statement : statements) {
			if (DEBUG) System.out.println(debugPrefix() + "writeStatement: " + statement);
			block.directStatement(parseForSpecials(
					statement.toString(), 
					statement instanceof StringExpressionStatement? 
							((StringExpressionStatement)statement).ignoreThis() : false
				));
		}
		statements.clear();
	}
	
	private void buildPreviousAction() {
		
		//Call the arguments for the last Action
		if (currentAction.get(0) != null) {
			
			if (DEBUG) System.out.println(debugPrefix() + "buildingPreviousAction");
			
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
		
		variables.add(variable);		
		
		visitingVariable = true;
		Boolean result = super.visitVariable(variable, trees);
		visitingVariable = false;
		
		return result;
	}
	
	@Override
	public Boolean visitExpressionStatement(
			ExpressionStatementTree expr, Trees trees) {
		if (ignoreActions) return super.visitExpressionStatement(expr, trees);
		
		statements.add(expr);
		
		Boolean result = super.visitExpressionStatement(expr, trees);
		
		if (anonymousClassTree != null) {
			statements.remove(statements.size()-1);
			
			final String exprString = expr.toString(); 
			
			System.out.println(anonymousClassTree.getSimpleName());
			int anonymousStart = exprString.indexOf("new " + anonymousClassTree.getSimpleName());
			String start = exprString.substring(0, anonymousStart);
			
			int anonymousEnd = 0;
			String end = null;
			
			int curlyBracketCount = 0;
			for (int i = anonymousStart; i < exprString.length(); i++) {
				if (exprString.charAt(i) == '{') curlyBracketCount++;
				
				if (exprString.charAt(i) == '}') {
					curlyBracketCount--;
					
					if (curlyBracketCount == 0) {
						anonymousEnd = i+1;
						end = exprString.substring(anonymousEnd);
						break;
					}
				}				
			}
			
			if (!start.equals("")) {
				statements.add(new StringExpressionStatement(start));
			}
			
			List<ExpressionStatementTree> anonymouseStatements = 
					statementsFromCode(exprString.substring(anonymousStart, anonymousEnd));
			for (ExpressionStatementTree statement : anonymouseStatements) {
				((StringExpressionStatement)statement).setIgnoreThis();
			}
			
			statements.addAll(anonymouseStatements);
			if (!end.equals("")) {
				statements.add(new StringExpressionStatement(end));
			}
			
			anonymousClassTree = null;
		}
		
		return result;
	}
	
	@Override
	public Boolean visitReturn(ReturnTree returnTree, Trees trees) {
		if (ignoreActions) return super.visitReturn(returnTree, trees);
		
		statements.add(new StringExpressionStatement(returnTree.toString()));
		
		Boolean result = super.visitReturn(returnTree, trees);
		
		if (anonymousClassTree != null) {
			statements.remove(statements.size()-1);
			
			final String exprString = returnTree.toString(); 
			
			System.out.println(anonymousClassTree.getSimpleName());
			int anonymousStart = exprString.indexOf("new " + anonymousClassTree.getSimpleName());
			String start = exprString.substring(0, anonymousStart);
			
			int anonymousEnd = 0;
			String end = null;
			
			int curlyBracketCount = 0;
			for (int i = anonymousStart; i < exprString.length(); i++) {
				if (exprString.charAt(i) == '{') curlyBracketCount++;
				
				if (exprString.charAt(i) == '}') {
					curlyBracketCount--;
					
					if (curlyBracketCount == 0) {
						anonymousEnd = i+1;
						end = exprString.substring(anonymousEnd);
						break;
					}
				}				
			}
			
			if (!start.equals("")) {
				statements.add(new StringExpressionStatement(start));
			}
			
			List<ExpressionStatementTree> anonymouseStatements = 
					statementsFromCode(exprString.substring(anonymousStart, anonymousEnd));
			for (ExpressionStatementTree statement : anonymouseStatements) {
				((StringExpressionStatement)statement).setIgnoreThis();
			}
			
			statements.addAll(anonymouseStatements);
			if (!end.equals("")) {
				statements.add(new StringExpressionStatement(end));
			}
			
			anonymousClassTree = null;
		}
		
		return result;
	}
	
	@Override
	public Boolean visitIdentifier(IdentifierTree id,
			Trees trees) {
		
		//This will happen with identifiers prior to the main block (ex. Annotations)
		if (!processStarted) return true;
				
		final String idName = id.toString();
		if (visitingIfCondition) {
			if (idName.equals(currentAction.get(0))) {
						
				if (!currentIfCondition.equals("(" + idName + "." + lastMemberIdentifier + ")")) {
					LOGGER.error(
							"Malformed Action Event Selector: if{}", 
							element, currentIfCondition
						);
					throw new IllegalStateException();
				}
				
				if (elseIfCondition != null) {
					LOGGER.error(
							"Else Block not supported for Action Events: if{} ...\nelse {}", 
							element, currentIfCondition, elseIfCondition
						);
					throw new IllegalStateException();
				}
	
				for (ParamInfo paramInfo : currentBuildParams.get(0).values()) {
					final ActionMethodParam param = paramInfo.param;
					
					if (param.name.equals(lastMemberIdentifier)) {
						//If the block already exists, do not create a new Runnable 
						if (paramInfo.runnableBlock != null) {
							pushBlock(paramInfo.runnableBlock, null);
						} else {
							
							JDefinedClass annonimousRunnable = getCodeModel().anonymousClass(param.clazz);
							JMethod annonimousRunnableRun = annonimousRunnable.method(JMod.PUBLIC, getCodeModel().VOID, "run");
							annonimousRunnableRun.annotate(Override.class);
							annonimousRunnableRun.body().directStatement("//ACTION EVENT: " + param.name);
				
							ParamInfo newParamInfo = new ParamInfo(
									paramInfo.param, 
									annonimousRunnableRun.body(), 
									_new(annonimousRunnable)
								);
							currentBuildParams.get(0).put(param.name, newParamInfo);
							
							pushBlock(annonimousRunnableRun.body(), null);
						}
						
						break;            						
					}
				}

				if (DEBUG) {
					System.out.println(debugPrefix() + "newEvent: " + currentIfCondition);
					debugIndex = debugIndex + "    ";
				}
				
				currentIfCondition = null;
			} else {
				if (ActionForHandler.getActionNames().containsKey(idName)) {
					LOGGER.error(
							"Action Out of Context: {} in if{}", 
							element, idName, currentIfCondition
						);
					throw new IllegalStateException();
				}
			}
		} 
		
		//Accessing Action from within unsupported structure
		if (ActionForHandler.getActionNames().containsKey(idName) && ignoreActions) {
			LOGGER.error(
					"Action is not supported in this location: {}", 
					element, idName
				);
			throw new IllegalStateException();
		}
		
		subMethods.clear();		
			
		//Ensure import of all the identifiers
		for (ImportTree importTree : imports) {
			if (importTree.getQualifiedIdentifier().toString().endsWith("." + idName)) {
				EnsureImportsHolder importsHolder = holder.getPluginHolder(new EnsureImportsHolder(holder));
				importsHolder.ensureImport(importTree.getQualifiedIdentifier().toString());		    	
				break;
			}
		}
		
		return super.visitIdentifier(id, trees);
	}
	
	@Override
	public Boolean visitEmptyStatement(EmptyStatementTree arg0, Trees trees) {
		if (ignoreActions) return super.visitEmptyStatement(arg0, trees);
		
		if (DEBUG) System.out.println(debugPrefix() + "empty");
		return super.visitEmptyStatement(arg0, trees);
	}
	
	@Override
	public Boolean visitMemberSelect(MemberSelectTree member,
			Trees trees) { 
		if (ignoreActions) return super.visitMemberSelect(member, trees);
		
		lastMemberIdentifier = member.getIdentifier().toString();
		return super.visitMemberSelect(member, trees);
	}
	
	@Override
	public Boolean visitIf(IfTree ifTree, Trees trees) {
		if (ignoreActions) return super.visitIf(ifTree, trees);
		
		if (!ifTree.getThenStatement().getKind().equals(Kind.BLOCK)
				|| (elseIfCondition != null && !elseIfCondition.getKind().equals(Kind.BLOCK))) {
			//Run this kind of Ifs without Action processing
			writeDirectStructure(ifTree.toString());
			
			ignoreActions = true;
			Boolean result = super.visitIf(ifTree, trees);
			ignoreActions = false;
			
			return result;
		}
		
		visitingIfCondition = true;
		currentIfCondition = ifTree.getCondition().toString();
		elseIfCondition = ifTree.getElseStatement();
		
		writePreviousStatements();    					
		
		return super.visitIf(ifTree, trees);
	}
			
	@Override
	public Boolean visitMethodInvocation(MethodInvocationTree invoke,
			Trees trees) {
		if (ignoreActions) return super.visitMethodInvocation(invoke, trees);
		if (visitingIfCondition) return super.visitMethodInvocation(invoke, trees);

		String methodSelect = invoke != null? invoke.getMethodSelect().toString() 
				                            : actionInFieldWithoutInitializer;
		if (methodSelect.contains(".")) {
			subMethods.add(invoke);
		} else {
			
			if (ActionForHandler.getActionNames().containsKey(methodSelect)) {
				
				String actionClass = ActionForHandler.getActionNames().get(methodSelect);
				ActionInfo actionInfo = ActionForHandler.getActionInfos().get(actionClass);

				if (delegatingMethodBody == null) {
					if (element instanceof ExecutableElement) {
						//Methods
						JMethod delegatingMethod = codeModelHelper.overrideAnnotatedMethod((ExecutableElement) element, holder);
						codeModelHelper.removeBody(delegatingMethod);
						delegatingMethodBody = delegatingMethod.body();
					} else {
						//Fields
						JDefinedClass anonymous = holder.getEnvironment().getCodeModel().anonymousClass(
								holder.getEnvironment().getJClass(DeclexConstant.ACTION + "." + methodSelect)
							);
							
						JMethod fire = anonymous.method(JMod.PUBLIC, holder.getEnvironment().getCodeModel().VOID, "fire");
						fire.annotate(Override.class);
						delegatingMethodBody = fire.body();
						
						holder.getInitBody().assign(ref(element.getSimpleName().toString()), _new(anonymous));
						
						///Current Action Information for the new Block
						currentAction.add(0, null);
						currentBuildInvocation.add(0, null);
						currentBuildParams.add(0, new LinkedHashMap<String, ParamInfo>());
						processStarted = true;
						
						if (DEBUG) {
							System.out.println(debugPrefix() + "FieldStart:");
							debugIndex = debugIndex + "    ";				
						}
					}
				}

				final String actionName = methodSelect.substring(0, 1).toLowerCase() 
		                  + methodSelect.substring(1) + actionCount;
				
				JBlock block = blocks.get(0);
				//This is important to detect empty blocks
				block.directStatement("//===========ACTION: " + actionName + "===========");
				
				buildPreviousAction();
				
				VariableTree variable = null;
				if (visitingVariable) {
					variable = variables.get(variables.size()-1);
					variables.remove(variables.size()-1);
				} else {
					//Remove last statement (represents this Action)
					if (statements.size() > 0) {
						statements.remove(statements.size()-1);
					}
				}
				writePreviousStatements();
				            					
				currentAction.set(0, methodSelect);
				actionCount++;
				
				IJExpression context = holder.getContextRef();
				if (context == _this()) {
					context = holder.getGeneratedClass().staticRef("this");
				}
				
				AbstractJClass injectedClass = getJClass(actionClass + ModelConstants.generationSuffix());
				
				JVar action = block.decl(
						injectedClass, 
						actionName,
						injectedClass.staticInvoke(EBeanHolder.GET_INSTANCE_METHOD_NAME).arg(context)
				);
								
				actionInfo.metaData = new HashMap<>();

				JInvocation initInvocation = block.invoke(action, "init");
				if (invoke != null) {
					for (String arg : processArguments("init", invoke, action, actionInfo)) {
						initInvocation.arg(direct(arg));
					}
				} 
				
				Collections.reverse(subMethods);
				JInvocation externalInvoke = null;
				
				for (MethodInvocationTree invocation : subMethods) {
					String name = invocation.getMethodSelect().toString();
					int index = name.lastIndexOf('.');
					name = name.substring(index+1);
					
					if (externalInvoke == null) {
						List<ActionMethod> methods = actionInfo.methods.get(name);
						if (methods != null && methods.size() > 0) {
							if (!methods.get(0).resultClass.equals(actionInfo.holderClass)) {
								externalInvoke = invoke(action, name);							
							}
						}

						JInvocation subMethodInvocation = externalInvoke==null? block.invoke(action, name) : externalInvoke;
						for (String arg : processArguments(name, invocation, action, actionInfo)) {
							subMethodInvocation = subMethodInvocation.arg(direct(arg));
	    				}
					} else {
						externalInvoke = externalInvoke.invoke(name);
						for (ExpressionTree arg : invocation.getArguments()) {
							externalInvoke = externalInvoke.arg(direct(arg.toString()));
	    				}
					}
					
				}
				
				List<ActionMethod> buildMethods = actionInfo.methods.get("build");
				if (buildMethods != null && buildMethods.size() > 0) {
					
					try {
						actionInfo.metaData.put("action", action);
						actionInfo.metaData.put("holder", holder);
						
						actionInfo.callProcessors();
						
					} catch (IllegalStateException e) {
						LOGGER.error(
								e.getMessage(), 
								element
							);
						throw new IllegalStateException();
					}
					
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
						if (visitingVariable) {							
							writeVariable(variable, block, externalInvoke);
						} else {
							block.add(externalInvoke);
						}
					}
					
					ActionMethod buildMethod = buildMethods.get(0);
					boolean firstParam = true;
					for (ActionMethodParam param : buildMethod.params) {
						ParamInfo paramInfo;
						if (firstParam) {
							
							JDefinedClass annonimousRunnable = getCodeModel().anonymousClass(param.clazz);
							JMethod annonimousRunnableRun = annonimousRunnable.method(JMod.PUBLIC, getCodeModel().VOID, "run");
							annonimousRunnableRun.annotate(Override.class);
							annonimousRunnableRun.body().directStatement("//ACTION EVENT: " + param.name);
							
							paramInfo = new ParamInfo(param, annonimousRunnableRun.body(), _new(annonimousRunnable));
							
							blocks.set(0, annonimousRunnableRun.body());
							
							if (DEBUG) {
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
				}
				
				block.invoke(action, "execute");
				block.directStatement("//============================================");
				
				if (invoke == null) {
					finishBlock();
					return true;
				}
			}
			
			subMethods.clear();
		}
		
		return super.visitMethodInvocation(invoke, trees);
	}
	    		
	@Override
	public Boolean visitBlock(BlockTree blockTree, Trees tree) {
		if (ignoreActions) return super.visitBlock(blockTree, tree);
		
		boolean isParallelBlock = true;
		
		if (visitingIfCondition) {

			if (currentIfCondition != null) {

				JBlock block = blocks.get(0);
				JConditional cond = block._if(direct(currentIfCondition));
				
				if (elseIfCondition != null) {
					//Push Else Block
					additionalBlocks.put(blocks.size(), cond._else());
				}				
				
				pushBlock(cond._then(), "newIf: " + currentIfCondition);
			}

			visitingIfCondition = false;
			isParallelBlock = false;
		} 
	
		if (!processStarted) {
			if (DEBUG) {
				System.out.println(debugPrefix() + "MethodStart:");
				debugIndex = debugIndex + "    ";				
			} 
			isParallelBlock = false;
		}
		
		//Current Action Information for the new Block
		currentAction.add(0, null);
		currentBuildInvocation.add(0, null);
		currentBuildParams.add(0, new LinkedHashMap<String, ParamInfo>());

		JBlock additionalBlock = additionalBlocks.get(blocks.size());
		if (additionalBlock != null) {
			additionalBlocks.remove(blocks.size());
			pushBlock(additionalBlock, "elseBlock:");
			isParallelBlock = false;
		} 
		
		//Used for execution of actions in parallel
		if (isParallelBlock) {			
			writePreviousStatements();
			
			JBlock block = parallelBlock.get(0);			
			pushBlock(block.block(), "newBlock: ");
		}
		
		processStarted = true;
		
		Boolean result = super.visitBlock(blockTree, tree); 
		finishBlock();
		return result;
	}
	
	private void pushBlock(JBlock block, String blockName) {
		blocks.add(0, block);
		parallelBlock.add(0, block);
		
		if (DEBUG && blockName != null) {
			System.out.println(debugPrefix() + blockName);
			debugIndex = debugIndex + "    ";	
		}
	}
	
	private void popBlock() {
		if (DEBUG) {
			debugIndex = debugIndex.substring(0, debugIndex.length()-4);
			System.out.println(debugPrefix() + "end");
		}
		
		blocks.remove(0);
		parallelBlock.remove(0);
	}
	
	private void finishBlock() {
		writePreviousStatements();						
		buildPreviousAction();
		
		currentAction.remove(0);
		currentBuildInvocation.remove(0);
		currentBuildParams.remove(0);
		
		popBlock();
		
		//If the method concluded, populate it
		if (currentAction.size() == 0) {
			delegatingMethodBody.add(initialBlock);
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
	}
	
	@Override
	public Boolean visitSwitch(SwitchTree switchTree, Trees trees) {
		if (ignoreActions) return super.visitSwitch(switchTree, trees);
				
		writeDirectStructure(switchTree.toString());
		
		ignoreActions = true;
		Boolean result = super.visitSwitch(switchTree, trees);
		ignoreActions = false;
		
		return result;
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
	public Boolean visitClass(ClassTree cls, Trees trees) {
		if (ignoreActions) return super.visitClass(cls, trees); 
				
		ignoreActions = true;
		anonymousClassTree = cls;
		Boolean result = super.visitClass(cls, trees);
		ignoreActions = false;
		
		return result;
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
		for (ActionMethod method : methods) {
			
			List<ActionMethodParam> params = method.params;
			
			if (invocation.getArguments().size() == params.size()) {
				
				method.metaData = new HashMap<>();
				
				for (int i = 0; i<params.size(); i++) {
					final ActionMethodParam param = params.get(i);
					
					arguments.clear();
					param.metaData = new HashMap<>();

					String currentParam = invocation.getArguments().get(i).toString();
					param.metaData.put("value", currentParam);
					
					boolean useArguments = false;
					for (Annotation annotation : param.annotations) {					
						
						//Formatted Expressions
						if (annotation instanceof FormattedExpression) {

							Matcher matcher = patternForStringLiterals.matcher(currentParam);
							while (matcher.find()) {
								String matched = matcher.group(0);
								String literalStringValue = matcher.group(1);
								
								IJExpression exp = FormatsUtils.expressionFromString(literalStringValue);
								
								currentParam = currentParam.replace(matched, expressionToString(exp)); 
								arguments.add(currentParam);
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
							
							boolean fieldFound = false;
							
							List<? extends Element> elems = holder.getAnnotatedElement().getEnclosedElements();
							for (Element elem : elems) {
								if (elem.getKind() == ElementKind.FIELD) {
									if (elem.getModifiers().contains(Modifier.PRIVATE)) continue;
									
									if (elem.getSimpleName().toString().equals(currentParam)) {
										fieldFound = true;
										
										param.metaData.put("field", elem);
									}
								}
							}
							
							if (!fieldFound) {
								LOGGER.error(
										"There's no an accesible field named: {} in {}", 
										element, currentParam, invocation
									);
								throw new IllegalStateException();
							}
						}
						
						if (currentParam != null) arguments.add(currentParam);
					}
					
					if (useArguments) return arguments;
				}
			} 
		}
		
		if (!matchFound) {
			arguments.clear();
			for (ExpressionTree arg : invocation.getArguments()) {
				arguments.add(arg.toString());
			}
		}
		
		return arguments;
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
	
	private AbstractJClass getJClass(String clazz) {
		return holder.getEnvironment().getJClass(clazz);
	}
		
	private JCodeModel getCodeModel() {
		return holder.getEnvironment().getCodeModel();
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
}
