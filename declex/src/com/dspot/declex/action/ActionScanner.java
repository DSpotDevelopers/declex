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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

	private List<Integer> blockStatementsCount = new LinkedList<>();
	private boolean decrementWithIdentifier = false;
	
	private Map<Integer, JBlock> additionalBlocks = new HashMap<>();
	private Set<Integer> decrementPreviousBlock = new HashSet<>();
	
	private EComponentHolder holder;
	private APTCodeModelHelper codeModelHelper;
	private Element element;
	
	private List<? extends ImportTree> imports;
	
	private boolean ignoreActions;
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
	            				Trees arg1) {
	            			
	            			String name = id.getName().toString();	            			
	            			if (ActionForHandler.getActionNames().containsKey(name)) {
	            				//Identifier detected
	            				throw new IllegalStateException();
	            			}
	            			
	            			return super.visitIdentifier(id, arg1);
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
		this.codeModelHelper = new DeclexAPTCodeModelHelper(holder.getEnvironment());;
		
		blocks.add(initialBlock);
		parallelBlock.add(blocks.get(0));
		
		imports = treePath.getCompilationUnit().getImports();

	}
	
	private String debugPrefix() {
		String prefix = "(" + blocks.size() + ", " + (blockStatementsCount.size()==0?0:blockStatementsCount.get(0)) + "): ";
		
		return prefix + debugIndex;
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
					                   null : direct(variable.getInitializer().toString());
			writeVariable(variable, block, initializer);
		}
		variables.clear();
		
		//Write all the statements till this point
		for (ExpressionStatementTree statement : statements) {
			if (DEBUG) System.out.println(debugPrefix() + "writeStatement: " + statement);
			block.directStatement(statement.toString());
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
	
	private void decrementBlockStatementsCount() {
		//This will happen with identifiers prior to the main block (ex. Annotations)
		if (!processStarted) return;
		
		int statementsCount = blockStatementsCount.get(0);
		statementsCount--;
		blockStatementsCount.set(0, statementsCount);
		if (statementsCount == 0) {

			writePreviousStatements();			
			
			blockStatementsCount.remove(0);

			buildPreviousAction();
			
			currentAction.remove(0);
			currentBuildInvocation.remove(0);
			currentBuildParams.remove(0);
			
			if (DEBUG) {
				debugIndex = debugIndex.substring(0, debugIndex.length()-4);
				System.out.println(debugPrefix() + "end"  + " :" + blocks.get(0).hashCode());
			}
			
			blocks.remove(0);
			parallelBlock.remove(0);
			
			//If the method concluded, populate it
			if (currentAction.size() == 0) {
				delegatingMethodBody.add(initialBlock);
			}
			
			if (decrementPreviousBlock.contains(blocks.size())) {
				//If there's no extra blocks
				if (!additionalBlocks.containsKey(blocks.size())) {
					decrementPreviousBlock.remove(blocks.size());
					decrementBlockStatementsCount();
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
		if (currentAction.size() == 0) return super.visitVariable(variable, trees);
		
		variables.add(variable);		
		decrementBlockStatementsCount();
		
		visitingVariable = true;
		Boolean result = super.visitVariable(variable, trees);
		visitingVariable = false;
		
		return result;
	}
	
	@Override
	public Boolean visitExpressionStatement(
			ExpressionStatementTree expr, Trees arg1) {
		if (ignoreActions) return super.visitExpressionStatement(expr, arg1);
		
		statements.add(expr);
		decrementWithIdentifier = true;
		
		return super.visitExpressionStatement(expr, arg1);
	}
	            		
	@Override
	public Boolean visitIdentifier(IdentifierTree id,
			Trees arg1) {
		
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
							blocks.add(0, paramInfo.runnableBlock);
							parallelBlock.add(0, blocks.get(0));
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
							
							blocks.add(0, annonimousRunnableRun.body());
							parallelBlock.add(0, blocks.get(0));
						}
						
						break;            						
					}
				}

				if (DEBUG) {
					System.out.println(debugPrefix() + "newEvent: " + currentIfCondition + " :" + blocks.get(0).hashCode());
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
		
		if (decrementWithIdentifier) {
			decrementBlockStatementsCount();	
			decrementWithIdentifier = false;
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
		
		return super.visitIdentifier(id, arg1);
	}
	
	@Override
	public Boolean visitEmptyStatement(EmptyStatementTree arg0, Trees arg1) {
		if (ignoreActions) return super.visitEmptyStatement(arg0, arg1);
		
		if (DEBUG) System.out.println(debugPrefix() + "empty");
		decrementBlockStatementsCount();
		return super.visitEmptyStatement(arg0, arg1);
	}
	
	@Override
	public Boolean visitMemberSelect(MemberSelectTree member,
			Trees arg1) { 
		if (ignoreActions) return super.visitMemberSelect(member, arg1);
		
		lastMemberIdentifier = member.getIdentifier().toString();
		return super.visitMemberSelect(member, arg1);
	}
	
	@Override
	public Boolean visitIf(IfTree ifTree, Trees arg1) {
		if (ignoreActions) return super.visitIf(ifTree, arg1);
		
		visitingIfCondition = true;
		currentIfCondition = ifTree.getCondition().toString();
		elseIfCondition = ifTree.getElseStatement();
		
		if (!ifTree.getThenStatement().getKind().equals(Kind.BLOCK)
			|| (elseIfCondition != null && !elseIfCondition.getKind().equals(Kind.BLOCK))) {
			LOGGER.error(
					"Action Ifs should be composed of blocks: if{}", 
					element, currentIfCondition
				);
			throw new IllegalStateException();
		}
		
		decrementPreviousBlock.add(blocks.size());
		writePreviousStatements();    					
		
		return super.visitIf(ifTree, arg1);
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
						blockStatementsCount.add(0, 1);
						decrementWithIdentifier = true;
						processStarted = true;
						
						if (DEBUG) {
							System.out.println(debugPrefix() + "FieldStart:" + " :" + blocks.get(0).hashCode());
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
									System.out.println(debugPrefix() + "writeAction: " + invoke + " :" + blocks.get(0).hashCode());
								} else {
									System.out.println(debugPrefix() + "writeAction: " + methodSelect + " :" + blocks.get(0).hashCode());
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
			}
			
			subMethods.clear();
		}
		        		
		if (invoke == null) {
			if (decrementWithIdentifier) {
				decrementBlockStatementsCount();	
				decrementWithIdentifier = false;
			}
			return true;
		}
		
		return super.visitMethodInvocation(invoke, trees);
	}
	    		
	@Override
	public Boolean visitBlock(BlockTree blockTree, Trees tree) {
		if (ignoreActions) return super.visitBlock(blockTree, tree);
		
		boolean newBlock = true;
		
		if (visitingIfCondition) {

			if (currentIfCondition != null) {

				JBlock block = blocks.get(0);
				JConditional cond = block._if(direct(currentIfCondition));
				
				if (elseIfCondition != null) {
					//Push Else Block
					additionalBlocks.put(blocks.size(), cond._else());
				}				
				
				blocks.add(0, cond._then());
				parallelBlock.add(0, blocks.get(0));

				if (DEBUG) {
					System.out.println(debugPrefix() + "newIf: " + currentIfCondition + " :" + blocks.get(0).hashCode());
					debugIndex = debugIndex + "    ";
				}
			}

			visitingIfCondition = false;
			newBlock = false;
		} 
	
		if (currentAction.size() == 0) {
			if (DEBUG) {
				System.out.println(debugPrefix() + "MethodStart:" + " :" + blocks.get(0).hashCode());
				debugIndex = debugIndex + "    ";				
			} 
			newBlock = false;
		}
		
		//Current Action Information for the new Block
		currentAction.add(0, null);
		currentBuildInvocation.add(0, null);
		currentBuildParams.add(0, new LinkedHashMap<String, ParamInfo>());

		JBlock additionalBlock = additionalBlocks.get(blocks.size());
		if (additionalBlock != null) {
			
			additionalBlocks.remove(blocks.size());
			blocks.add(0, additionalBlock);
			parallelBlock.add(0, blocks.get(0));
			
			if (DEBUG) {
				System.out.println(debugPrefix() + "elseBlock" + " :" + blocks.get(0).hashCode());
				debugIndex = debugIndex + "    ";
			}
			
			newBlock = false;
		} 
		
		//The newBlocks are used for parallel execution of actions
		if (newBlock) {			
			decrementPreviousBlock.add(blocks.size());
			writePreviousStatements();
			
			JBlock block = parallelBlock.get(0);			
			blocks.add(0, block.block());
			parallelBlock.add(0, blocks.get(0));
			
			if (DEBUG) {
				System.out.println(debugPrefix() + "newBlock:" + " :" + blocks.get(0).hashCode());
				debugIndex = debugIndex + "    ";	
			}
		}
		
		blockStatementsCount.add(0, blockTree.getStatements().size());
		processStarted = true;
		
		return super.visitBlock(blockTree, tree);
	}

	private void writeDirectStructure(String code) {
		
		//Write line by line to format better the text
		try {
			BufferedReader bufReader = new BufferedReader(new StringReader(code));
			
			String line = null;
			while((line=bufReader.readLine()) != null)
			{
				final String statementLine = line; 
				statements.add(new ExpressionStatementTree() {
					String line = statementLine;
					
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
						return line;
					}
				});
			}
		} catch (Exception e) {}
		
		decrementBlockStatementsCount();
	}
	
	@Override
	public Boolean visitTry(TryTree tryTree, Trees arg1) {		
		writeDirectStructure(tryTree.toString());
		
		ignoreActions = true;
		Boolean result = super.visitTry(tryTree, arg1);
		ignoreActions = false;
		
		return result;
	}
	
	@Override
	public Boolean visitSwitch(SwitchTree switchTree, Trees arg1) {
		writeDirectStructure(switchTree.toString());
		
		ignoreActions = true;
		Boolean result = super.visitSwitch(switchTree, arg1);
		ignoreActions = false;
		
		return result;
	}
	
	@Override
	public Boolean visitForLoop(ForLoopTree forLoop, Trees arg1) {
		writeDirectStructure(forLoop.toString());
		
		ignoreActions = true;
		Boolean result = super.visitForLoop(forLoop, arg1);
		ignoreActions = false;
		
		return result;
	}
	
	@Override
	public Boolean visitEnhancedForLoop(EnhancedForLoopTree forLoop, Trees arg1) {
		writeDirectStructure(forLoop.toString());
		
		ignoreActions = true;
		Boolean result = super.visitEnhancedForLoop(forLoop, arg1);
		ignoreActions = false;
		
		return result;
	}
	
	@Override
	public Boolean visitWhileLoop(WhileLoopTree whileLoop, Trees arg1) {
		writeDirectStructure(whileLoop.toString());
		
		ignoreActions = true;
		Boolean result = super.visitWhileLoop(whileLoop, arg1);
		ignoreActions = false;
		
		return result;
	}
	
	@Override
	public Boolean visitDoWhileLoop(DoWhileLoopTree doWhileLoop, Trees arg1) {
		writeDirectStructure(doWhileLoop.toString());
		
		ignoreActions = true;
		Boolean result = super.visitDoWhileLoop(doWhileLoop, arg1);
		ignoreActions = false;
		
		return result;
	}
	
	@Override
	public Boolean visitClass(ClassTree cls, Trees arg1) {
		LOGGER.error(
				"Anonymouse classes are not supported yet in  Actions: {}", 
				element, cls
			);
		throw new IllegalStateException();
	}
	
	@Override
	public Boolean visitSynchronized(SynchronizedTree sync, Trees arg1) {
		writeDirectStructure(sync.toString());
		
		ignoreActions = true;
		Boolean result = super.visitSynchronized(sync, arg1);
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
