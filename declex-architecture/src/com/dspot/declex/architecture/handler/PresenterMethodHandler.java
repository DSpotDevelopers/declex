package com.dspot.declex.architecture.handler;

import com.dspot.declex.action.ActionsProcessor;
import com.dspot.declex.api.action.process.ActionProcessor;
import com.dspot.declex.architecture.annotation.PresenterMethod;
import com.dspot.declex.architecture.api.MethodCall;
import com.dspot.declex.architecture.holder.ViewPresenterHolder;
import com.helger.jcodemodel.*;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.helper.APTCodeModelHelper;
import org.androidannotations.holder.EComponentHolder;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.List;

import static com.dspot.declex.action.ActionsProcessor.hasAction;
import static com.helger.jcodemodel.JExpr.*;
import static com.helger.jcodemodel.JMod.NONE;
import static com.helger.jcodemodel.JMod.PRIVATE;

public class PresenterMethodHandler extends BaseAnnotationHandler<EComponentHolder> {

    public PresenterMethodHandler(AndroidAnnotationsEnvironment environment) {
        super(PresenterMethod.class, environment);
    }

    @Override
    public void validate(Element element, ElementValidation validation) {
        validatorHelper.returnTypeIsVoid((ExecutableElement) element, validation);
    }

    @Override
    public void process(Element element, EComponentHolder holder) throws JClassAlreadyExistsException {

        final ViewPresenterHolder viewPresenterHolder = holder.getPluginHolder(new ViewPresenterHolder(holder));
        final ExecutableElement executableElement = (ExecutableElement) element;

        //Create the MethodCall class for current method
        JDefinedClass methodCallClass = holder.getGeneratedClass()._class(PRIVATE, methodCallClassNameFor(executableElement));
        JMethod constructor = methodCallClass.constructor(NONE);
        methodCallClass._implements(MethodCall.class);

        //Check the method call
        JBlock methodCallBlock = viewPresenterHolder.getPerformMethodCallBlock();
        methodCallBlock = methodCallBlock._if(viewPresenterHolder.getPerformMethodCallField()._instanceof(methodCallClass))._then();

        JVar methodCallCasted = methodCallBlock.decl(methodCallClass, "methodCallCasted",
                cast(methodCallClass, viewPresenterHolder.getPerformMethodCallField()));

        //If has action syntax, call the action method instead of the super
        JInvocation methodCallToSuper;
        if (hasAction(executableElement, getEnvironment())) {
            methodCallToSuper = methodCallBlock.invoke("$" + executableElement.getSimpleName().toString());
        } else {
            methodCallToSuper = methodCallBlock.invoke(ref("super"), executableElement.getSimpleName().toString());
        }


        //Add fields and params to methods
        JInvocation newInvocation = _new(methodCallClass);

        final List<? extends VariableElement> params = ((ExecutableElement) element).getParameters();
        for (VariableElement param : params) {
            String paramName = param.getSimpleName().toString();
            AbstractJClass paramType = codeModelHelper.elementTypeToJClass(param).erasure();

            JFieldVar field = methodCallClass.field(NONE, paramType, paramName);
            JVar methodParam = constructor.param(paramType, paramName);
            constructor.body().assign(_this().ref(field), methodParam);

            newInvocation = newInvocation.arg(ref(paramName));
            methodCallToSuper = methodCallToSuper.arg(methodCallCasted.ref(paramName));
        }

        //Override the method
        JMethod method = codeModelHelper.overrideAnnotatedMethod(executableElement, holder, false, false);
        JVar methodCallInstance = method.body().decl(methodCallClass, "methodCall", newInvocation);
        method.body().invoke(viewPresenterHolder.getPresenterLiveDataField(), "setValue").arg(methodCallInstance);


    }

    public static String methodCallClassNameFor(ExecutableElement element) {

        final String fieldName = element.getSimpleName().toString();
        final List<? extends VariableElement> params = ((ExecutableElement) element).getParameters();

        String className = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        for (VariableElement param : params) {
            String paramName = param.asType().toString();

            while (paramName.endsWith("[]")) paramName = paramName.substring(0, paramName.length() - 2) + "$";

            if (paramName.contains("<")) paramName = paramName.substring(0, paramName.indexOf('<'));

            className = className + "$" + paramName.substring(paramName.lastIndexOf('.') + 1);
        }

        className = className + "MethodCall";

        return  className;
    }

}
