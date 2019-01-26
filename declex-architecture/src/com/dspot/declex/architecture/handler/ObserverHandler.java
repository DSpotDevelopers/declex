package com.dspot.declex.architecture.handler;

import com.dspot.declex.architecture.annotation.Observer;
import com.dspot.declex.architecture.holder.ObserversHolder;
import com.helger.jcodemodel.*;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.helper.APTCodeModelHelper;
import org.androidannotations.holder.EComponentHolder;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.List;

import static com.dspot.declex.api.util.FormatsUtils.fieldToGetter;
import static com.dspot.declex.architecture.ArchCanonicalNameConstants.*;
import static com.dspot.declex.util.TypeUtils.getRootElement;
import static com.dspot.declex.util.TypeUtils.isSubtype;
import static com.helger.jcodemodel.JExpr.*;
import static com.sun.codemodel.internal.JMod.PRIVATE;

public class ObserverHandler extends BaseAnnotationHandler<EComponentHolder> {

    public ObserverHandler(AndroidAnnotationsEnvironment environment) {
        super(Observer.class, environment);
    }

    @Override
    public void validate(Element element, ElementValidation validation) {
        validatorHelper.param.anyType().validate((ExecutableElement) element, validation);

        Observer observer = adiHelper.getAnnotation(element, Observer.class);

        if (!observer.observeForever()) {
            TypeElement rootElement = getRootElement(element);
            if (!isSubtype(rootElement, LIFECYCLE_OWNER, getProcessingEnvironment())
                    && !isSubtype(rootElement, VIEW_MODEL, getProcessingEnvironment())) {
                validation.addError("@Observer can be placed only inside classes which implement LifeCycleOwner or ViewModel"
                                    + "\nIf you are aware of the consequences of observing another class forever, you can manually set \"observeForever = true\"");
            }
        }

        validatorHelper.returnTypeIsVoidOrBoolean((ExecutableElement) element, validation);

    }

    @Override
    public void process(Element element, EComponentHolder holder) {

        ObserversHolder observersHolder = holder.getPluginHolder(new ObserversHolder(holder));

        final String fieldName = element.getSimpleName().toString();
        final ExecutableElement executableElement = (ExecutableElement) element;
        final String observerName = observerNameFor(executableElement, codeModelHelper);

        final List<? extends VariableElement> params = (executableElement).getParameters();

        //Create get observer method
        {
            AbstractJClass Observer = getJClass(OBSERVER);
            AbstractJClass ReferencedClass = codeModelHelper.elementTypeToJClass(params.get(0));

            //Create Observer field
            JFieldVar field = holder.getGeneratedClass().field(PRIVATE, Observer.narrow(ReferencedClass), observerName);

            //Create Getter for this observer
            JMethod getterMethod = holder.getGeneratedClass().method(
                    JMod.PUBLIC,
                    Observer.narrow(ReferencedClass),
                    fieldToGetter(observerName)
            );
            JBlock creationBlock = getterMethod.body()._if(_this().ref(observerName).eqNull())._then();

            JDefinedClass AnonymousObserver = getCodeModel().anonymousClass(Observer.narrow(ReferencedClass));
            JMethod anonymousOnChanged = AnonymousObserver.method(JMod.PUBLIC, getCodeModel().VOID, "onChanged");
            anonymousOnChanged.annotate(Override.class);
            JVar param = anonymousOnChanged.param(ReferencedClass, "value");

            //Call this method
            if (executableElement.getReturnType().toString().equals("void")) {
                anonymousOnChanged.body().invoke(fieldName).arg(param);
            } else {
                //Check the boolean, its value determines if it is needed to unregister the observer
                JVar removeObserver = anonymousOnChanged.body().decl(getCodeModel().BOOLEAN, "removeObserver", invoke(fieldName).arg(param));
                JBlock removeObserverBlock = anonymousOnChanged.body()._if(removeObserver)._then();
                removeObserverBlock.invoke(observersHolder.getRemoveObserverMethod()).arg(_this());
            }

            creationBlock.assign(field, _new(AnonymousObserver));
            getterMethod.body()._return(_this().ref(observerName));
        }



    }

    public static String observerNameFor(ExecutableElement element, APTCodeModelHelper codeModelHelper) {

        final String fieldName = element.getSimpleName().toString();
        final List<? extends VariableElement> params = ((ExecutableElement) element).getParameters();
        AbstractJClass ReferencedClass = codeModelHelper.elementTypeToJClass(params.get(0));

        return "observerFor" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1) + "$" + ReferencedClass.name();
    }

}
