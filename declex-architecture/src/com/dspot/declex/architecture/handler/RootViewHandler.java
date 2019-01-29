package com.dspot.declex.architecture.handler;

import static com.helger.jcodemodel.JExpr.cast;
import static com.helger.jcodemodel.JExpr.lit;
import static org.androidannotations.helper.LogHelper.logTagForClassHolder;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

import com.dspot.declex.architecture.annotation.EViewModel;
import com.dspot.declex.architecture.annotation.EViewPresenter;
import com.dspot.declex.architecture.annotation.RootView;
import com.dspot.declex.architecture.holder.ViewModelHolder;
import com.dspot.declex.architecture.holder.ViewPresenterHolder;
import com.helger.jcodemodel.*;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.handler.MethodInjectionHandler;
import org.androidannotations.helper.CanonicalNameConstants;
import org.androidannotations.helper.InjectHelper;
import org.androidannotations.holder.EBeanHolder;

public class RootViewHandler extends BaseAnnotationHandler<EBeanHolder>implements MethodInjectionHandler<EBeanHolder> {

    private final InjectHelper<EBeanHolder> injectHelper;

    public RootViewHandler(AndroidAnnotationsEnvironment environment) {
        super(RootView.class, environment);
        injectHelper = new InjectHelper<>(validatorHelper, this);
    }

    @Override
    public void validate(Element element, ElementValidation validation) {
        injectHelper.validate(RootView.class, element, validation);
        if (!validation.isValid()) {
            return;
        }

        validatorHelper.isNotPrivate(element, validation);
    }

    @Override
    public void process(Element element, EBeanHolder holder) {
        injectHelper.process(element, holder);
    }

    @Override
    public JBlock getInvocationBlock(Element element, EBeanHolder holder) {
        return holder.getInitBodyInjectionBlock();
    }

    @Override
    public void assignValue(JBlock targetBlock, IJAssignmentTarget fieldRef, EBeanHolder holder, Element element, Element param) {
        TypeMirror elementType = param.asType();
        String typeQualifiedName = elementType.toString();

        //Could be @EViewModel or @EViewPresenter
        JFieldVar rootViewField = null;
        if (adiHelper.hasAnnotation(holder.getAnnotatedElement(), EViewModel.class)) {
            ViewModelHolder viewModelHolder = holder.getPluginHolder(new ViewModelHolder(holder));
            rootViewField = viewModelHolder.getRootViewField();

        } else if (adiHelper.hasAnnotation(holder.getAnnotatedElement(), EViewPresenter.class)) {
            ViewPresenterHolder viewPresenterHolder = holder.getPluginHolder(new ViewPresenterHolder(holder));
            rootViewField = viewPresenterHolder.getRootViewField();
        }

        if (rootViewField == null) return;

        if (CanonicalNameConstants.OBJECT.equals(typeQualifiedName)) {
            targetBlock.add(fieldRef.assign(rootViewField));
        } else {

            AbstractJClass extendingRootViewClass = getEnvironment().getJClass(typeQualifiedName);

            JConditional cond = getInvocationBlock(element, holder)._if(rootViewField._instanceof(extendingRootViewClass));
            cond._then().add(fieldRef.assign(cast(extendingRootViewClass, rootViewField)));

            JInvocation warningInvoke = getClasses().LOG.staticInvoke("w");
            warningInvoke.arg(logTagForClassHolder(holder));
            warningInvoke.arg(lit("Due to the class ").plus(holder.getContextRef().invoke("getClass").invoke("getSimpleName"))
                    .plus(lit(", the @RootView " + extendingRootViewClass.name() + " won't be populated")));
            cond._else().add(warningInvoke);
        }
    }

    @Override
    public void validateEnclosingElement(Element element, ElementValidation valid) {
        validatorHelper.enclosingElementHasEBeanAnnotation(element, valid);
    }
}

