package com.dspot.declex.action.builder;

import com.dspot.declex.action.util.ExpressionsHelper;
import com.dspot.declex.override.helper.DeclexAPTCodeModelHelper;
import com.dspot.declex.util.JavaDocUtils;
import com.helger.jcodemodel.*;
import com.sun.source.tree.ReturnTree;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.holder.EComponentHolder;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr.cast;

public class ActionsMethodHolder {

    private JFieldRef delegatingMethodResultValueVar;
    private IJExpression delegatingMethodResultValueVarExpression;
    private JFieldRef delegatingMethodFinishedVar;

    private JMethod delegatingMethod;
    private JBlock delegatingMethodStart;
    private JBlock delegatingMethodEnd;
    private JBlock delegatingMethodBody = null;

    private EComponentHolder holder;
    private Element element;
    private AndroidAnnotationsEnvironment environment;

    private ActionsMethodBuilder methodBuilder;
    private DeclexAPTCodeModelHelper codeModelHelper;
    private ExpressionsHelper expressionsHelper;

    public ActionsMethodHolder(Element element, EComponentHolder holder, AndroidAnnotationsEnvironment environment) {
        this.element = element;
        this.holder = holder;
        this.environment = environment;
        this.codeModelHelper = new DeclexAPTCodeModelHelper(environment);
    }

    public void setMethodBuilder(ActionsMethodBuilder methodBuilder) {
        this.methodBuilder = methodBuilder;
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
            delegatingMethodStart = delegatingMethodBody.blockVirtual();

        } else {
            delegatingMethod.annotate(Override.class);
        }

        String javaDocRef = "<br><hr><br>\nAction Method " + JavaDocUtils.referenceFromElement(element);
        delegatingMethod.javadoc().add(javaDocRef);

    }

    public void addReturn(boolean insideAction, ReturnTree returnTree) {

        final String resultName = delegatingMethod.name() + "Result";

        if (delegatingMethodFinishedVar == null) {

            final TypeMirror returnType = ((ExecutableElement)element).getReturnType();
            final AbstractJClass ActionResult = environment.getJClass(com.dspot.declex.api.action.structure.ActionResult.class);
            final JVar result = delegatingMethodStart.decl(
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
            methodBuilder.addStatement(
                expressionsHelper.expressionToString(delegatingMethodResultValueVar) + " = " + returnTree.getExpression() + ";");
        }

        if (delegatingMethodFinishedVar != null) {
            methodBuilder.addStatement(
                resultName + "." + delegatingMethodFinishedVar.name() + " = true;");
        }

        if (insideAction || delegatingMethodResultValueVar == null || methodBuilder.hasSharedVariableHolder()) {
            methodBuilder.addStatement("return;");
        } else {
            methodBuilder.addStatement(
                "return " + expressionsHelper.expressionToString(delegatingMethodResultValueVarExpression) + ";");
        }

    }

    public void addBlockReturn(JBlock block, boolean shouldReturnResultValue) {

        JBlock finishBlock = block._if(delegatingMethodFinishedVar)._then();
        if (delegatingMethodResultValueVar != null && shouldReturnResultValue && !methodBuilder.hasSharedVariableHolder()) {
            finishBlock._return(delegatingMethodResultValueVarExpression);
        } else {
            finishBlock._return();
        }

    }

    public void completeActionMethod() {

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

    public boolean hasDelegatingMethodBody() {
        return delegatingMethodBody != null;
    }

    public void setDelegatingMethodBody(JBlock delegatingMethodBody) {
        this.delegatingMethodBody = delegatingMethodBody;
    }

    public JMethod getDelegatingMethod() {
        return delegatingMethod;
    }

}
