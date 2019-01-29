package com.dspot.declex.architecture.handler;

import com.dspot.declex.architecture.annotation.ArchInject;
import com.dspot.declex.architecture.annotation.EViewPresenter;
import com.dspot.declex.architecture.annotation.Observable;
import com.dspot.declex.architecture.annotation.Observer;
import com.dspot.declex.architecture.api.MethodCall;
import com.dspot.declex.architecture.holder.ObserversHolder;
import com.dspot.declex.architecture.holder.ViewModelHolder;
import com.dspot.declex.architecture.holder.ViewPresenterHolder;
import com.helger.jcodemodel.*;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.handler.MethodInjectionHandler;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EComponentHolder;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

import static com.dspot.declex.api.util.FormatsUtils.fieldToGetter;
import static com.dspot.declex.architecture.ArchCanonicalNameConstants.*;
import static com.dspot.declex.architecture.handler.ObserverHandler.observerNameFor;
import static com.dspot.declex.util.TypeUtils.*;
import static com.helger.jcodemodel.JExpr.*;
import static com.sun.codemodel.internal.JMod.PRIVATE;

public class ObserversLinkingHandler<T extends EComponentHolder> extends BaseAnnotationHandler<T> {

    public ObserversLinkingHandler(Class<?> targetClass, AndroidAnnotationsEnvironment environment) {
        super(targetClass, environment);
    }

    @Override
    protected void validate(Element element, ElementValidation validation) {
    }

    @Override
    public void process(Element element, EComponentHolder holder) {

        //Determine the enhancedClass for the field
        TypeMirror typeMirror = annotationHelper.extractAnnotationClassParameter(element);
        if (typeMirror == null) {
            typeMirror = element.asType();
            typeMirror = getProcessingEnvironment().getTypeUtils().erasure(typeMirror);
        }

        String typeQualifiedName = typeMirror.toString();
        AbstractJClass enhancedClass = getJClass(annotationHelper.generatedClassQualifiedNameFromQualifiedName(typeQualifiedName));

        //Check Observable fields
        final String generatedClassName = getGeneratedClassName(element, getEnvironment());
        process(generatedClassName.substring(0, generatedClassName.length() - 1), element, enhancedClass, holder);

    }

    protected boolean isViewPresenter(Element element) {

        //Check if the class is a View Presenter
        TypeMirror typeMirror = annotationHelper.extractAnnotationClassParameter(element);
        if (typeMirror == null) {
            typeMirror = element.asType();
            typeMirror = getProcessingEnvironment().getTypeUtils().erasure(typeMirror);
        }

        String typeQualifiedName = typeMirror.toString();
        String generatedClass = annotationHelper.generatedClassQualifiedNameFromQualifiedName(typeQualifiedName);
        TypeElement generatorTypeElement = getProcessingEnvironment().getElementUtils().getTypeElement(
                generatedClass.substring(0, generatedClass.length() - 1)
        );

        return adiHelper.hasAnnotation(generatorTypeElement, EViewPresenter.class);

    }

    protected void process(String generatorClassName, Element injectingElement, AbstractJClass injectingEnhancedClass, EComponentHolder injectingElementHolder) {
        final IJExpression injectingField;
        if (injectingElement.asType().toString().endsWith(ModelConstants.generationSuffix())) {
            injectingField = ref(injectingElement.getSimpleName().toString());
        } else {
            injectingField = cast(injectingEnhancedClass, ref(injectingElement.getSimpleName().toString()));
        }

        linkObserversFrom(generatorClassName, injectingElement, injectingField, injectingEnhancedClass, injectingElementHolder);
    }

    private void linkObserversFrom(String generatorClassName, Element injectingElement, IJExpression injectingField,
                                   AbstractJClass injectingEnhancedClass, EComponentHolder injectingElementHolder) {

        TypeElement generatorTypeElement = getProcessingEnvironment().getElementUtils().getTypeElement(generatorClassName);
        if (generatorTypeElement == null) return;
        if (generatorTypeElement.getKind().equals(ElementKind.INTERFACE)) return;
        if (generatorTypeElement.asType().toString().equals(Object.class.getCanonicalName())) return;

        for (Element elem : generatorTypeElement.getEnclosedElements()) {

            final String observableFieldName = elem.getSimpleName().toString();
            String observableTypeName = elem.asType().toString();
            if (observableTypeName.contains("<")) {
                observableTypeName = observableTypeName.substring(observableTypeName.indexOf("<") + 1, observableTypeName.length() - 1);
            }

            if (adiHelper.hasAnnotation(elem, Observable.class)) {
                performObservation(observableFieldName, observableTypeName, injectingElement, injectingField, injectingEnhancedClass, injectingElementHolder);
            }

        }

        //Look for observers in the super classes
        List<? extends TypeMirror> superTypes = getProcessingEnvironment().getTypeUtils().directSupertypes(generatorTypeElement.asType());
        for (TypeMirror type : superTypes) {
            type = getProcessingEnvironment().getTypeUtils().erasure(type);
            linkObserversFrom(type.toString(), injectingElement, injectingField, injectingEnhancedClass, injectingElementHolder);
        }

    }

    protected void performObservation(String observableFieldName, String observableTypeName, Element injectingElement,
                                      IJExpression injectingField, AbstractJClass injectingEnhancedClass, EComponentHolder injectingElementHolder) {

        final ObserversHolder observersHolder = injectingElementHolder.getPluginHolder(new ObserversHolder(injectingElementHolder));
        final TypeElement rootElement = getRootElement(injectingElement);

        final JInvocation observableGetterInvoke = invoke(injectingField, fieldToGetter(observableFieldName));

        //Look for matching observer
        for (Element elem : rootElement.getEnclosedElements()) {

            if (adiHelper.hasAnnotation(elem, Observer.class)) {

                final String observerMethodName = elem.getSimpleName().toString();

                if (observerMethodName.equals(observableFieldName)) {

                    //TODO ensure the parameters are the same type
                    final ExecutableElement executableElem = (ExecutableElement) elem;
                    final String observerGetterName = fieldToGetter(observerNameFor(executableElem, codeModelHelper));

                    registerObserver(observableGetterInvoke, invoke(observerGetterName), injectingElement, injectingElementHolder);

                    //Register the observers which could be removed in the removeObserver method
                    if (!executableElem.getReturnType().toString().equals("void")) {
                        JMethod method = observersHolder.getRemoveObserverMethod();

                        JBlock removeObserverBlock = method.body()
                                ._if(observersHolder.getRemoveObserverMethodParam().eq(invoke(observerGetterName)))._then();

                        removeObserverBlock.add(observableGetterInvoke.invoke("removeObserver").arg(observersHolder.getRemoveObserverMethodParam()));
                    }
                }

            }

        }

    }

    private void registerObserver(IJExpression observableGetterReference, IJExpression observerGetterInvoke,
                                  Element injectingElement, EComponentHolder injectingElementHolder) {

        //Register the observer
        JBlock block;
        if (this instanceof MethodInjectionHandler) {
            block = ((MethodInjectionHandler) this).getInvocationBlock(injectingElement, injectingElementHolder);
        } else {
            block = injectingElementHolder.getInitBodyAfterInjectionBlock();
        }

        //Different behaviors depending on the injected element holder class type
        if (isSubtype(injectingElementHolder.getAnnotatedElement(), LIFECYCLE_OWNER, getProcessingEnvironment())) {
            block.add(observableGetterReference.invoke("observe").arg(_this()).arg(observerGetterInvoke));
        } else if (isSubtype(injectingElementHolder.getAnnotatedElement(), VIEW_MODEL, getProcessingEnvironment())) {
            block.add(observableGetterReference.invoke("observeForever").arg(observerGetterInvoke));

            //Remove the observer when the ViewModel is not needed anymore
            ViewModelHolder viewModelHolder = injectingElementHolder.getPluginHolder(new ViewModelHolder(injectingElementHolder));
            viewModelHolder.getOnClearedMethodBlock().add(observableGetterReference.invoke("removeObserver").arg(observerGetterInvoke)
            );

        } else {

            //It is assumed that a validation was done to ensure the user is aware of a "forever" observer
            //This validation should have been done in the ObserverHandler
            block.add(observableGetterReference.invoke("observeForever").arg(observerGetterInvoke));

        }


    }

}
