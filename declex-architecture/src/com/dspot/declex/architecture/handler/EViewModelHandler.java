package com.dspot.declex.architecture.handler;

import com.dspot.declex.architecture.annotation.ArchInject;
import com.dspot.declex.architecture.annotation.EViewModel;
import com.dspot.declex.architecture.holder.ViewModelHolder;
import com.dspot.declex.helper.ActionHelper;
import com.helger.jcodemodel.JMethod;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.holder.EBeanHolder;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.Map;

import static com.dspot.declex.api.util.FormatsUtils.fieldToGetter;
import static com.dspot.declex.architecture.ArchCanonicalNameConstants.ANDROID_VIEW_MODEL;
import static com.dspot.declex.architecture.ArchCanonicalNameConstants.VIEW_MODEL;
import static com.dspot.declex.util.TypeUtils.isSubtype;
import static com.helger.jcodemodel.JExpr.*;
import static com.helger.jcodemodel.JMod.PUBLIC;

public class EViewModelHandler extends BaseAnnotationHandler<EBeanHolder> {

    public EViewModelHandler(AndroidAnnotationsEnvironment environment) {
        super(EViewModel.class, environment);
    }

    @Override
    public void getDependencies(Element element, Map<Element, Object> dependencies) {
        dependencies.put(element, EBean.class);
    }

    @Override
    public void validate(Element element, ElementValidation valid) {
        if (!isSubtype(element, VIEW_MODEL, getProcessingEnvironment())) {
            valid.addError("The class " + element + " should be a subclass of ViewModel");
        }
        
        //Ensure the ViewModel doesn't have action objects (since actions have a reference to the Context)
        ActionHelper actionHelper = ActionHelper.getInstance(getEnvironment());

        for (Element elem : element.getEnclosedElements()) {
            if (actionHelper.hasAction(elem)) {
                valid.addError(elem, "Actions are not permitted inside ViewModels, you should declare them in support classes");
            }

            if (adiHelper.hasAnnotation(elem, RootContext.class)) {
                valid.addError(elem, "You should never inject the Root Context in a ViewModel");
            }

        }

    }

    @Override
    public void process(Element element, EBeanHolder holder) {

        ViewModelHolder viewModelHolder = holder.getPluginHolder(new ViewModelHolder(holder));

        viewModelHolder.getConstructorMethod();

        viewModelHolder.getRebindMethod();

        //Clear the context variable after the injections, to avoid that the class hold references to the Context
        holder.getInitBodyAfterInjectionBlock().assign(holder.getContextField(), _null());

        //Clear the rootView variable after the injection
        holder.getInitBodyAfterInjectionBlock().assign(viewModelHolder.getRootViewField(), _null());

        //Search for all the injections, and set them to null in the "onCleared", so no reference is kept
        //after the ViewModel was marked as not needed
        for (Element elem : element.getEnclosedElements()) {

            boolean markedToRemove = false;

            if (adiHelper.hasAnnotation(elem, Bean.class)) {
                markedToRemove = true;
            }

            if (adiHelper.hasAnnotation(elem, ArchInject.class)) {
                markedToRemove = true;
            }

            if (markedToRemove) {
                viewModelHolder.getOnClearedMethodFinalBlock().assign(_this().ref(elem.getSimpleName().toString()), _null());
            }

        }

    }

}
