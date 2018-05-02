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
package com.dspot.declex.action.builder;

import com.dspot.declex.action.Actions;
import com.dspot.declex.action.exception.ActionProcessingException;
import com.dspot.declex.action.util.ActionsLogger;
import com.dspot.declex.action.util.ExpressionsHelper;
import com.dspot.declex.annotation.action.*;
import com.dspot.declex.api.action.process.ActionInfo;
import com.dspot.declex.api.action.process.ActionMethod;
import com.dspot.declex.api.action.process.ActionMethodParam;
import com.dspot.declex.api.action.structure.ActionResult;
import com.dspot.declex.api.util.FormatsUtils;
import com.dspot.declex.override.helper.DeclexAPTCodeModelHelper;
import com.dspot.declex.util.DeclexConstant;
import com.dspot.declex.util.JavaDocUtils;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.*;
import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.helper.ADIHelper;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EBeanHolder;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.internal.helper.ViewNotifierHelper;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.helger.jcodemodel.JExpr.*;
import static com.helger.jcodemodel.JExpr.invoke;

/**
 * This class creates all the actions structure.
 * It should translate the action syntax into actual code which will be executed.
 */
public class ActionsBuilder {

    private List<String> currentAction = new LinkedList<>();
    private List<String> currentActionSelectors = new LinkedList<>();
    private List<JInvocation> currentBuildInvocation = new LinkedList<>();
    private List<Map<String, ParamInfo>> currentBuildParams = new LinkedList<>();

    private List<String> actionsTree = new LinkedList<>();
    private List<JBlock> actionsTreeAfterExecute = new LinkedList<>();

    private JFieldRef delegatingMethodResultValueVar;
    private JFieldRef delegatingMethodFinishedVar;
    private JMethod delegatingMethod;
    private JBlock delegatingMethodStart;
    private JBlock delegatingMethodEnd;
    private JBlock delegatingMethodBody = null;

    private String actionInFieldWithoutInitializer = null;
    private int actionInFieldWithoutInitializerPosition;

    private List<MethodInvocationTree> subMethods = new LinkedList<>();

    private boolean isValidating;

    private ActionsLogger logger;

    private EComponentHolder holder;

    private Element element;

    private AndroidAnnotationsEnvironment environment;

    private ActionsMethodBuilder methodBuilder;

    private DeclexAPTCodeModelHelper codeModelHelper;

    private ExpressionsHelper expressionsHelper;

    private ADIHelper adiHelper;

    public ActionsBuilder(boolean isValidating, Element element, EComponentHolder holder, ActionsLogger logger,
                          TreePath treePath, AndroidAnnotationsEnvironment environment) {

        this.isValidating = isValidating;
        this.element = element;
        this.holder = holder;
        this.logger = logger;
        this.environment = environment;

        this.codeModelHelper = new DeclexAPTCodeModelHelper(environment);
        this.adiHelper = new ADIHelper(environment);
        this.expressionsHelper = new ExpressionsHelper(isValidating, this, holder, treePath);

    }

    public void setMethodBuilder(ActionsMethodBuilder methodBuilder) {
        this.methodBuilder = methodBuilder;
    }

    public boolean hasPendingAction() {
        return currentAction.get(0) != null || currentAction.size() > 1;
    }

    public void buildAction() {

        if (isValidating) return;

        //Call the arguments for the last Action
        if (currentAction.get(0) != null) {

            logger.info("buildingPreviousAction");

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

    public void buildActionMethod(boolean isOverrideAction) {

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

    public boolean searchAction(VariableTree variable, int sourcePosition) {

        //Field action
        if (!(element instanceof ExecutableElement)) {
            if (variable.getInitializer() == null) {
                actionInFieldWithoutInitializer = variable.getType().toString();
                actionInFieldWithoutInitializerPosition = sourcePosition;
                return true;
            }
        }

        return false;

    }

    public boolean searchAction(MethodInvocationTree invoke, int sourcePosition) {

        String methodSelect = invoke != null? invoke.getMethodSelect().toString() : actionInFieldWithoutInitializer;

        if (methodSelect.contains(".")) {
            subMethods.add(invoke);
        } else {

            if (Actions.getInstance().hasActionNamed(methodSelect)) {

                String actionClass = Actions.getInstance().getActionNames().get(methodSelect);
                ActionInfo actionInfo = Actions.getInstance().getActionInfos().get(actionClass);

                if (delegatingMethodBody == null) {
                    if (isValidating) {
                        //This block is not going to be used
                        delegatingMethodBody = new JBlock();

                        if (!(element instanceof ExecutableElement)) {
                            pushAction();
                            processStarted = true;
                        }

                    } else {
                        if (element instanceof ExecutableElement) {

                            //Create the Action method
                            buildActionMethod(false);

                        } else {
                            //Fields
                            JDefinedClass anonymous = getCodeModel().anonymousClass(
                                getJClass(DeclexConstant.ACTION + "." + methodSelect)
                            );

                            JMethod fire = anonymous.method(JMod.PUBLIC, getCodeModel().VOID, "fire");
                            fire.annotate(Override.class);
                            delegatingMethodBody = fire.body();

                            holder.getInitBody().assign(ref(element.getSimpleName().toString()), _new(anonymous));

                            pushAction();
                            processStarted = true;

                            logger.info("FieldStart:" + subMethods);
                            logger.increaseIndex();
                        }
                    }
                }

                final int position = invoke == null ? actionInFieldWithoutInitializerPosition : sourcePosition;
                final String actionName = methodSelect.substring(0, 1).toLowerCase() + methodSelect.substring(1) + position;

                JBlock block = methodBuilder.getCurrentBlock();

                //This is important to detect empty blocks
                block.directStatement("//===========ACTION: " + actionName + "===========");

                buildAction();
                actionsTree.add(methodSelect);

                //Remove last statement (represents this Action)
                methodBuilder.removeLastStatement();

                methodBuilder.buildStatements();

                setCurrentAction(methodSelect);

                IJExpression context = holder == null? ref("none") : holder.getContextRef();
                if (context == _this()) {
                    context = holder.getGeneratedClass().staticRef("this");
                }

                AbstractJClass injectedClass = getJClass(actionClass + ModelConstants.generationSuffix());
                JBlock preInstantiate = block.blockVirtual();

                final JVar action;
                if (actionInfo.isGlobal && !isValidating) {
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
                    for (IJExpression arg : processArguments("init", invoke, action, actionInfo)) {
                        initInvocation.arg(arg);
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

                            if (!TypeUtils.isSubtype(holderClass, resultClass, environment.getProcessingEnvironment())) {

                                if (actionInfo.superHolderClass == null
                                    || !TypeUtils.isSubtype(actionInfo.superHolderClass, resultClass, environment.getProcessingEnvironment())) {

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
                        for (IJExpression arg : processArguments(name, invocation, action, actionInfo)) {
                            subMethodInvocation = subMethodInvocation.arg(arg);
                        }
                    } else {
                        externalInvoke = externalInvoke.invoke(name);
                        for (ExpressionTree arg : invocation.getArguments()) {
                            externalInvoke = externalInvoke.arg(expressionsHelper.stringToExpression(arg.toString()));
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
                        actionInfo.metaData.put("element", element);
                        actionInfo.metaData.put("adi", adiHelper);

                        if (isValidating) {
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
                        for (IJStatement postInitBlock : postInitBlocks) {
                            postInit.add(postInitBlock);
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

                        setCurrentBuildInvocation(block.invoke(action, "build"));

                        @SuppressWarnings("unchecked")
                        List<IJStatement> postBuildBlocks = (List<IJStatement>) actionInfo.metaData.get("postBuildBlocks");
                        if (postBuildBlocks != null) {
                            for (IJStatement postBuildBlock : postBuildBlocks) {
                                block.add(postBuildBlock);
                            }
                        }

                        if (externalInvoke != null) {
                            externalInvokeInBlock(block, externalInvoke);
                        }

                        ActionMethod buildMethod = buildMethods.get(0);
                        boolean firstParam = true;
                        for (ActionMethodParam param : buildMethod.params) {
                            ActionsBuilder.ParamInfo paramInfo;
                            if (firstParam) {

                                JDefinedClass anonymousRunnable = getCodeModel().anonymousClass((AbstractJClass) param.clazz);
                                JMethod anonymousRunnableRun = anonymousRunnable.method(JMod.PUBLIC, getCodeModel().VOID, "run");
                                anonymousRunnableRun.annotate(Override.class);
                                anonymousRunnableRun.body().directStatement("//ACTION EVENT: " + param.name);

                                paramInfo = new ActionsBuilder.ParamInfo(param, anonymousRunnableRun.body(), _new(anonymousRunnable));

                                methodBuilder.setCurrentBlock(anonymousRunnableRun.body());

                                if (invoke != null) {
                                    logger.info("writeAction: " + invoke);
                                } else {
                                    logger.info("writeAction: " + methodSelect);
                                }

                                firstParam = false;

                            } else {
                                paramInfo = new ActionsBuilder.ParamInfo(param, null, _null());
                            }

                            setCurrentBuildParameters(param.name, paramInfo);
                        }

                        block.invoke(action, "execute");

                    } else {
                        setCurrentBuildInvocation(null);

                        if (externalInvoke != null) {
                            externalInvokeInBlock(block, externalInvoke);
                        }
                    }
                }

                actionsTreeAfterExecute.add(block.blockVirtual());
                block.directStatement("//============================================");

                if (!(element instanceof ExecutableElement)) {
                    return true;
                }

            }

            subMethods.clear();
        }

        return false;

    }

    public void stopActionSearch() {
        subMethods.clear();
    }

    public boolean hasActionFinished(BlockTree blockTree) {

        if (blockTree.getStatements().size() == 1 && actionsTree.size() > 0
            && blockTree.getStatements().get(0).toString().startsWith("return")) {

            if (blockTree.getStatements().get(0).toString().equals("return;")) {
                JBlock block = methodBuilder.getCurrentBlock();
                block._return();
                return true;
            } else {
                if (blockTree.getStatements().get(0).toString().equals("return null;")
                    || blockTree.getStatements().get(0).toString().equals("return 0;")
                    || blockTree.getStatements().get(0).toString().equals("return false;")
                    || blockTree.getStatements().get(0).toString().equals("return 0.0")) {

                    JBlock block = methodBuilder.getCurrentBlock();
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

        return false;

    }

    public boolean hasActionFinished(ReturnTree returnTree) {

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

        if (!isValidating && (insideAction || delegatingMethodFinishedVar != null)) {

            final String resultName = delegatingMethod.name() + "_result";

            if (delegatingMethodFinishedVar == null) {

                AbstractJClass ActionResult = environment.getJClass(ActionResult.class);
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
                methodBuilder.addStatement(
                    resultName + "." + delegatingMethodResultValueVar.name() + " = " + returnTree.getExpression() + ";");
            }

            if (delegatingMethodFinishedVar != null) {
                methodBuilder.addStatement(
                    resultName + "." + delegatingMethodFinishedVar.name() + " = true;");
            }

            if (insideAction || delegatingMethodResultValueVar == null || methodBuilder.hasSharedVariableHolder()) {
                methodBuilder.addStatement("return;");
            } else {
                methodBuilder.addStatement(
                    "return " + resultName + "." + delegatingMethodResultValueVar.name() + ";");
            }

            for (int i = 0; i < actionsTreeAfterExecute.size(); i++) {
                JBlock block = actionsTreeAfterExecute.get(i);

                //This block contains only this condition
                if (!block.isEmpty()) continue;

                JBlock finishBlock = block._if(delegatingMethodFinishedVar)._then();
                if (delegatingMethodResultValueVar != null && i == 0 && !methodBuilder.hasSharedVariableHolder()) {
                    finishBlock._return(delegatingMethodResultValueVar);
                } else {
                    finishBlock._return();
                }
            }

            return true;

        }

        return false;

    }

    public Collection<ParamInfo> getCurrentBuildParameters() {
        return currentBuildParams.get(0).values();
    }

    public void setCurrentBuildInvocation(JInvocation invocation) {
        currentBuildInvocation.set(0, invocation);
        currentBuildParams.get(0).clear();
    }

    public void setCurrentBuildParameters(String name, ParamInfo param) {
        currentBuildParams.get(0).put(name, param);
    }

    public String getCurrentAction() {
        return currentAction.get(0);
    }

    public void setCurrentAction(String actionName) {
        currentAction.set(0, actionName);
    }

    public void pushAction() {
        currentAction.add(0, null);
        currentBuildInvocation.add(0, null);
        currentBuildParams.add(0, new LinkedHashMap<String, ParamInfo>());
    }

    public void popAction() {

        if (actionsTree.size() > 0) {
            actionsTree.remove(actionsTree.size()-1);
            actionsTreeAfterExecute.remove(actionsTreeAfterExecute.size()-1);
        }

        currentAction.remove(0);
        currentBuildInvocation.remove(0);
        currentBuildParams.remove(0);

        if (currentAction.size() == 0) {

            //Crate the parameter variables
            if (delegatingMethodStart != null) {

                for (JVar param : delegatingMethod.listParams()) {

                    String paramName = param.name();
                    if (paramName.startsWith("$")) {
                        paramName = paramName.substring(1);
                    }

                    if (methodBuilder.hasSharedVariableHolder()) {
                        methodBuilder.getSharedVariablesHolder().field(JMod.NONE, param.type(), paramName, param);
                    } else {
                        delegatingMethodStart.decl(param.type(), paramName, param);
                    }
                }

            }

            delegatingMethodBody.add(methodBuilder.getInitialBlock());
            if (delegatingMethodEnd != null) {
                delegatingMethodBody.add(delegatingMethodEnd);
            }
        }

    }

    public boolean hasSelector() {
        return currentActionSelectors.size() > 0;
    }

    public String getCurrentSelector() {
        return currentActionSelectors.get(0);
    }

    public void pushSelector(String selectorId, String actionName, String selectorName, ParamInfo param) {
        currentActionSelectors.add(0, selectorId);
        addActionSelector(actionName, selectorName, param);
    }

    public void popSelector() {
        currentActionSelectors.remove(0);
    }

    private void addActionSelector(String actionName, String actionSelector, ParamInfo paramInfo) {
        final ActionMethodParam param = paramInfo.param;

        //If the block already exists, do not create a new Runnable
        if (paramInfo.runnableBlock != null) {
            methodBuilder.pushBlock(paramInfo.runnableBlock, null);
            pushAction();
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

            methodBuilder.pushBlock(anonymousRunnableRun.body(), null);
            pushAction();
        }

        logger.info("newSelector: " + actionName + "." + actionSelector);
        logger.increaseIndex();

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

    private void externalInvokeInBlock(JBlock block, JInvocation invocation) {

        if (visitingVariable) {

            VariableTree variable = (VariableTree) methodBuilder.getLastStatement();
            methodBuilder.addVariable(variable, block, invocation);

        } else {

            if (assignment != null) {
                block.assign(ref(assignment.getVariable().toString()), invocation);
                assignment = null;
            } else {
                block.add(invocation);
            }

        }

    }

    private List<IJExpression> processArguments(String methodName, MethodInvocationTree invocation,  JVar action, ActionInfo actionInfo) {

        Pattern patternForStringLiterals = Pattern.compile("\"((?:\\\\\"|[^\"])*?)\"");

        List<IJExpression> arguments = new LinkedList<>();

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
                            boolean found = false;
                            Matcher matcher = patternForStringLiterals.matcher(currentParam);

                            while (matcher.find()) {
                                found = true;

                                String matched = matcher.group(0);
                                if (!matched.equals(currentParam)) {
                                    throw new ActionProcessingException("You should provide a literal value for \"fields\" in action " + invocation);
                                }

                                String literalStringValue = matcher.group(1);

                                IJExpression exp = FormatsUtils.expressionFromString(literalStringValue);
                                param.metaData.put("literalExpression", exp);
                                param.metaData.put("literal", literalStringValue);
                            }

                            if (!found) {
                                throw new ActionProcessingException("You should provide a literal value for \"fields in action invocation");
                            }
                        }

                        //Formatted Expressions
                        if (annotation instanceof FormattedExpression) {

                            boolean found = false;
                            Matcher matcher = patternForStringLiterals.matcher(currentParam);

                            while (matcher.find()) {
                                found = true;

                                String matched = matcher.group(0);
                                String literalStringValue = matcher.group(1);

                                IJExpression exp = FormatsUtils.expressionFromString(literalStringValue);

                                currentParam = currentParam.replace(matched, expressionsHelper.expressionToString(exp));
                            }

                            if (found) {
                                arguments.add(expressionsHelper.stringToExpression(currentParam));
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

                                AbstractJClass fieldJClass = codeModelHelper.elementTypeToJClass(fieldElement, true);
                                param.metaData.put("fieldClass", fieldJClass.fullName());
                                param.metaData.put("fieldJClass", fieldJClass);
                            } else {

                                if (!currentParam.equals(((Field) annotation).ignoreExpression())) {
                                    throw new ActionProcessingException(
                                        "There's no an accesible field named: " + currentParam + " in " + invocation
                                    );
                                }

                            }
                        }

                        if (currentParam != null) {
                            arguments.add(expressionsHelper.stringToExpression(currentParam));
                        }
                    }

                    if (useArguments) return arguments;
                }
            }
        }

        arguments.clear();
        for (ExpressionTree arg : invocation.getArguments()) {
            arguments.add(expressionsHelper.stringToExpression(arg.toString()));
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

        final ProcessingEnvironment env = environment.getProcessingEnvironment();

        //Apply to Extensions
        List<? extends TypeMirror> superTypes = env.getTypeUtils().directSupertypes(element.asType());
        for (TypeMirror type : superTypes) {
            TypeElement superElement = env.getElementUtils().getTypeElement(type.toString());
            if (superElement == null) continue;

            Element elem = findField(superElement, fieldName);
            if (elem != null) return elem;
        }

        return null;

    }

    private AbstractJClass getJClass(String clazz) {
        return environment.getJClass(clazz);
    }

    private JCodeModel getCodeModel() {
        return environment.getCodeModel();
    }

    public static class ParamInfo {

        public ActionMethodParam param;
        public IJExpression assignment;
        public JBlock runnableBlock;

        public ParamInfo(ActionMethodParam param, JBlock runnableBlock, IJExpression assignment) {
            super();
            this.param = param;
            this.runnableBlock = runnableBlock;
            this.assignment = assignment;
        }
    }

}
