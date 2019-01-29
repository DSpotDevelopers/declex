package com.dspot.declex.architecture.handler;

import com.dspot.declex.architecture.annotation.EViewPresenter;
import com.dspot.declex.architecture.annotation.PresenterMethod;
import com.dspot.declex.architecture.api.MethodCall;
import com.dspot.declex.architecture.holder.ViewPresenterHolder;
import com.helger.jcodemodel.*;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.holder.EBeanHolder;
import org.androidannotations.holder.EComponentHolder;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import java.util.Map;

import static com.dspot.declex.api.util.FormatsUtils.fieldToGetter;
import static com.dspot.declex.architecture.ArchCanonicalNameConstants.LIVE_DATA;
import static com.dspot.declex.architecture.ArchCanonicalNameConstants.OBSERVER;
import static com.helger.jcodemodel.JExpr.*;
import static com.helger.jcodemodel.JExpr._this;
import static com.helger.jcodemodel.JMod.PRIVATE;
import static com.helger.jcodemodel.JMod.PUBLIC;

public class EViewPresenterHandler extends BaseAnnotationHandler<EBeanHolder> {

    public EViewPresenterHandler(AndroidAnnotationsEnvironment environment) {
        super(EViewPresenter.class, environment);
    }

    @Override
    public void getDependencies(Element element, Map<Element, Object> dependencies) {

        //EViewPresenter works different when it is in a view or activity
        if (adiHelper.hasAnnotation(element, EFragment.class) || adiHelper.hasAnnotation(element, EActivity.class)) {
            return;
        }

        dependencies.put(element, EBean.class);

        EViewPresenter viewPresenter = element.getAnnotation(EViewPresenter.class);

        if (viewPresenter.markPresenterMethods()) {

            //Mark the public methods with @PresenterMethod
            for (Element elem : ((TypeElement) element).getEnclosedElements()) {

                if (elem.getKind() != ElementKind.METHOD) continue;
                if (elem.getModifiers().contains(Modifier.STATIC)) continue;
                if (((ExecutableElement)elem).getReturnType().getKind() != TypeKind.VOID) continue; //TODO notify to the user about this method not being "protected"?

                if (elem.getModifiers().contains(Modifier.PUBLIC)) {
                    dependencies.put(elem, PresenterMethod.class);
                }

            }

        }

    }

    @Override
    public void validate(Element element, ElementValidation valid) {}

    @Override
    public void process(Element element, EBeanHolder holder) {

        ViewPresenterHolder viewPresenterHolder = holder.getPluginHolder(new ViewPresenterHolder(holder));

        viewPresenterHolder.getConstructorMethod();

        viewPresenterHolder.getPresenterLiveDataField();

        viewPresenterHolder.getRebindMethod();

    }

}
