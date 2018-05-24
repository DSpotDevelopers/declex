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

import com.dspot.declex.action.util.ActionsLogger;
import com.dspot.declex.action.util.ExpressionsHelper;
import com.dspot.declex.override.helper.DeclexAPTCodeModelHelper;
import com.dspot.declex.util.JavaDocUtils;
import com.helger.jcodemodel.*;
import com.sun.source.tree.*;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.holder.EComponentHolder;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.helger.jcodemodel.JExpr.*;

/**
 * This class builds the Actions Method. It contains the method object, with its variables, blocks and statements.
 */
public class ActionsMethodBuilder {

    private JBlock initialBlock = new JBlock();
    public JBlock actionMethodStartBlock = new JBlock();

    private List<JBlock> blocks = new LinkedList<>();
    private List<JBlock> originalBlocks = new LinkedList<>();
    private Map<Integer, List<BlockDescription>> additionalBlocks = new HashMap<>();

    private List<StatementTree> statements = new LinkedList<>();

    private JAnonymousClass sharedVariablesHolder = null;

    private JMethod delegatingMethod;
    private JBlock delegatingMethodEnd;
    private JBlock delegatingMethodBody = null;

    private JFieldRef delegatingMethodResultValueVar;
    private IJExpression delegatingMethodResultValueVarExpression;
    private JFieldRef delegatingMethodFinishedVar;

    private boolean shouldBuildCreateParametersVariables;

    private boolean isValidating;

    private ActionsLogger logger;

    private EComponentHolder holder;

    private Element element;

    private ExpressionsHelper expressionsHelper;

    private AndroidAnnotationsEnvironment environment;

    private DeclexAPTCodeModelHelper codeModelHelper;

    public ActionsMethodBuilder(Element element, EComponentHolder holder, boolean isValidating,
                                ActionsLogger logger, AndroidAnnotationsEnvironment environment) {
        this.isValidating = isValidating;
        this.logger = logger;
        this.element = element;
        this.holder = holder;
        this.environment = environment;

        this.codeModelHelper = new DeclexAPTCodeModelHelper(environment);
    }

    public void setExpressionsHelper(ExpressionsHelper expressionsHelper) {
        this.expressionsHelper = expressionsHelper;
    }

    public boolean didInitializedResults() {
        return  delegatingMethodFinishedVar != null;
    }

    public void buildDelegatingMethod(boolean isOverrideAction) {

        delegatingMethod = codeModelHelper.overrideAnnotatedMethod((ExecutableElement) element, holder, true);

        if (!isOverrideAction) {

            codeModelHelper.removeBody(delegatingMethod);

            delegatingMethodBody = delegatingMethod.body();
            actionMethodStartBlock = delegatingMethodBody.blockVirtual().add(actionMethodStartBlock);

            shouldBuildCreateParametersVariables = true;

        } else {
            delegatingMethod.annotate(Override.class);
        }

        String javaDocRef = "<br><hr><br>\nAction Method " + JavaDocUtils.referenceFromElement(element);
        delegatingMethod.javadoc().add(javaDocRef);

    }

    public void addReturn(boolean insideAction, ReturnTree returnTree) {

        if (isValidating) return;

        final String resultName = element.getSimpleName().toString() + "Result";

        if (delegatingMethodFinishedVar == null) {

            final TypeMirror returnType = ((ExecutableElement)element).getReturnType();
            final AbstractJClass ActionResult = environment.getJClass(com.dspot.declex.api.action.structure.ActionResult.class);
            final JVar result = actionMethodStartBlock.decl(
                JMod.FINAL,
                ActionResult,
                resultName,
                _new(ActionResult)
            );

            delegatingMethodFinishedVar = result.ref("finished");

            IJExpression resultReference;
            JFieldRef resultReferenceAsVariable;
            if (returnType.getKind().isPrimitive()) {
                final String primitiveValueHolder = ((ExecutableElement)element).getReturnType().toString() + "Val";
                resultReferenceAsVariable = result.ref(primitiveValueHolder);
                resultReference = resultReferenceAsVariable;
            } else {
                resultReferenceAsVariable =result.ref("objectVal");
                resultReference = cast(codeModelHelper.typeMirrorToJClass(returnType), resultReferenceAsVariable);
            }

            if (returnType.getKind() != TypeKind.VOID) {
                delegatingMethodResultValueVar = resultReferenceAsVariable;
                delegatingMethodResultValueVarExpression = resultReference;

                delegatingMethodEnd = new JBlock();
                delegatingMethodEnd.virtual(true);
                delegatingMethodEnd._return(resultReference);
            }
        }

        //Assign value and return
        if (delegatingMethodResultValueVar != null) {
            addStatement(expressionsHelper.expressionToString(delegatingMethodResultValueVar) + " = " + returnTree.getExpression() + ";");
        }

        if (delegatingMethodFinishedVar != null) {
            addStatement(resultName + "." + delegatingMethodFinishedVar.name() + " = true;");
        }

        if (insideAction || delegatingMethodResultValueVar == null || hasSharedVariableHolder()) {
            addStatement("return;");
        } else {
            addStatement("return " + expressionsHelper.expressionToString(delegatingMethodResultValueVarExpression) + ";");
        }

    }

    public void addBlockReturn(JBlock block, boolean shouldReturnResultValue) {

        JBlock finishBlock = block._if(delegatingMethodFinishedVar)._then();
        if (delegatingMethodResultValueVar != null && shouldReturnResultValue && !hasSharedVariableHolder()) {
            finishBlock._return(delegatingMethodResultValueVarExpression);
        } else {
            finishBlock._return();
        }

    }

    public void finalizeActionMethod() {

        //Crate the parameter variables
        if (shouldBuildCreateParametersVariables) {

            for (JVar param : delegatingMethod.listParams()) {

                String paramName = param.name();
                if (paramName.startsWith("$")) {
                    paramName = paramName.substring(1);
                }

                if (hasSharedVariableHolder()) {
                    getSharedVariablesHolder().field(JMod.NONE, param.type(), paramName, param);
                } else {
                    actionMethodStartBlock.decl(param.type(), paramName, param);
                }
            }

        }

        if (delegatingMethod != null) {

            delegatingMethodBody.add(initialBlock);
            if (delegatingMethodEnd != null) {
                delegatingMethodBody.add(delegatingMethodEnd);
            }

        }

    }

    public boolean hasDelegatingMethodBody() {
        return delegatingMethodBody != null;
    }

    public void setDelegatingMethodBody(JBlock delegatingMethodBody) {
        this.delegatingMethodBody = delegatingMethodBody;
    }

    public JMethod getDelegatingMethod() {
        return delegatingMethod;
    }

    public void buildStatements() {

        if (!isValidating) {
            JBlock block = blocks.get(0);

            //Write all the statements till this point
            for (StatementTree statement : statements) {
                if (statement instanceof ExpressionStatementTree) {
                    logger.info("writeStatement: " + statement);
                    block.directStatement(expressionsHelper.parseForSpecials(
                            statement.toString(),
                            statement instanceof StringExpressionStatement && ((StringExpressionStatement) statement).ignoreThis()
                    ));
                }

                if (statement instanceof VariableTree) {
                    VariableTree variable = (VariableTree) statement;
                    IJExpression initializer = variable.getInitializer() == null?
                            null : expressionsHelper.stringToExpression(variable.getInitializer().toString());

                    addVariable(variable, block, initializer);
                }

            }
        }

        statements.clear();
    }

    public void addVariable(VariableTree variable, JBlock block, IJExpression initializer) {

        if (isValidating) return;

        logger.info("addVariable: " + variable);

        //Inferred variables must start with $ sign
        final String name = variable.getName().toString();
        needsSharedVariablesHolder();

        String variableClassName = expressionsHelper.variableClassFromImports(variable.getType().toString(), true);

        int arrayCounter = 0;
        while (variableClassName.endsWith("[]")) {
            arrayCounter++;
            variableClassName = variableClassName.substring(0, variableClassName.length() - 2);
        }

        AbstractJClass VARIABLE_CLASS = getJClass(variableClassName);
        for (int i = 0; i < arrayCounter; i++) {
            VARIABLE_CLASS = VARIABLE_CLASS.array();
        }

        if (!sharedVariablesHolder.containsField(name)) {

            if (initializer != null) {

                String initializerValue = expressionsHelper.expressionToString(initializer);
                while (initializerValue.startsWith("(") && initializerValue.endsWith(")")) {
                    initializerValue = initializerValue.substring(1, initializerValue.length()-1);
                }

                if (!initializerValue.isEmpty()) {

                    sharedVariablesHolder.field(
                        JMod.NONE,
                        VARIABLE_CLASS,
                        name
                    );

                    //Initializers for arrays
                    if (arrayCounter > 0) {
                        if (initializerValue.startsWith("{")) {
                            initializerValue = "new " + VARIABLE_CLASS.name() + initializerValue;
                        }

                        block.assign(ref(name), direct(initializerValue));

                    } else {
                        block.assign(ref(name), initializer);
                    }

                } //else Nothing should be done, the variable will be injected directly from the context

            } else {
                sharedVariablesHolder.field(
                    JMod.NONE,
                    VARIABLE_CLASS,
                    name
                );
            }
        } else {
            if (initializer != null) {

                String initializerValue = expressionsHelper.expressionToString(initializer);
                while (initializerValue.startsWith("(") && initializerValue.endsWith(")")) {
                    initializerValue = initializerValue.substring(1, initializerValue.length()-1);
                }

                if (!initializerValue.isEmpty()) {
                    block.assign(ref(name), initializer);
                } //else Nothing should be done, the variable will be injected directly from the context

            }
        }
    }

    public void needsSharedVariablesHolder() {

        if (sharedVariablesHolder == null) {

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

    }

    public boolean hasSharedVariableHolder() {
        return sharedVariablesHolder != null;
    }

    public JAnonymousClass getSharedVariablesHolder() {
        return sharedVariablesHolder;
    }

    public boolean hasAdditionalBlock() {
        List<BlockDescription> descriptions = additionalBlocks.get(blocks.size());
        return descriptions != null;
    }

    public void addAdditionalBlock(JBlock block, String blockName) {
        BlockDescription blockDescription = new BlockDescription(block, blockName);

        List<BlockDescription> descriptions = additionalBlocks.get(blocks.size());
        if (descriptions == null) {
            descriptions = new LinkedList<>();
            additionalBlocks.put(blocks.size(), descriptions);
        }

        descriptions.add(blockDescription);
    }

    public void removeAdditionalBlock() {
        List<BlockDescription> descriptions = additionalBlocks.get(blocks.size());
        if (descriptions != null) {
            descriptions.remove(descriptions.size()-1);

            if (descriptions.size() == 0) {
                additionalBlocks.remove(blocks.size());
            }
        }
    }

    public void pushAdditionalBlock() {
        List<BlockDescription> descriptions = additionalBlocks.get(blocks.size());
        if (descriptions != null) {
            BlockDescription blockDescription = descriptions.get(descriptions.size()-1);
            removeAdditionalBlock();
            pushBlock(blockDescription.block, blockDescription.description);
        }

    }

    public void pushInitialBlock() {
        pushBlock(initialBlock, null);
        actionMethodStartBlock = initialBlock.blockVirtual();
    }

    public void pushBlock(JBlock block, String blockName) {
        blocks.add(0, block);
        originalBlocks.add(0, block);

        if (blockName != null) {
            logger.info(blockName);
            logger.increaseIndex();
        }
    }

    public void popBlock() {

        logger.decreaseIndex();
        logger.info("end");

        blocks.remove(0);
        originalBlocks.remove(0);

        //Check if the method ended
        if (blocks.size() == 0 && originalBlocks.size() == 0) {
            finalizeActionMethod();
        }

    }

    public JBlock getCurrentBlock() {
        return blocks.get(0);
    }

    public void setCurrentBlock(JBlock block) {
        blocks.set(0, block);
    }

    public JBlock getCurrentOriginalBlock() {
        return originalBlocks.get(0);
    }

    public void addStatement(StatementTree statement) {
        this.statements.add(statement);
    }

    public void addStatement(String statement) {
        statements.add(new StringExpressionStatement(statement));
    }

    public void addReturnStatement(ReturnTree returnTree) {

        if (hasSharedVariableHolder()) {
            addReturn(false, returnTree);
        } else {
            addStatement(returnTree.toString());
        }

    }

    public void addStatementVariableWithoutInitializer(VariableTree variableTree) {
        statements.add(new VariableExpressionWithoutInitializer(variableTree));
    }

    public void addStatementBlock(String code) {
        this.addStatementBlock(code, false);
    }

    public void addStatementBlock(String code, boolean ignoreThisReferences) {

        List<StringExpressionStatement> blockStatements = statementsFromCode(code);

        if (ignoreThisReferences) {
            for (StringExpressionStatement statement : blockStatements) {
                statement.setIgnoreThis();
            }
        }

        statements.addAll(blockStatements);

    }

    public StatementTree getLastStatement() {
        return statements.get(statements.size()-1);
    }

    public void removeLastStatement() {
        if (statements.size() > 0) {
            statements.remove(statements.size()-1);
        }
    }

    private List<StringExpressionStatement> statementsFromCode(String code) {

        List<StringExpressionStatement> statements = new LinkedList<>();

        //Write line by line to format better the text
        try {

            BufferedReader bufReader = new BufferedReader(new StringReader(code));

            String line = null;
            while((line=bufReader.readLine()) != null) {
                statements.add(new StringExpressionStatement(line));
            }

        } catch (Throwable ignored) {}

        return statements;

    }

    private AbstractJClass getJClass(String clazz) {
        return environment.getJClass(clazz);
    }

    private JCodeModel getCodeModel() {
        return environment.getCodeModel();
    }

    private static class BlockDescription {
        JBlock block;
        String description;

        public BlockDescription(JBlock block, String description) {
            super();
            this.block = block;
            this.description = description;
        }

    }

    private static class VariableExpressionWithoutInitializer implements VariableTree {

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

        @Override
        public ExpressionTree getNameExpression() {
            return variableTree.getNameExpression();
        }

    }

    private static class StringExpressionStatement implements ExpressionStatementTree {

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

}
