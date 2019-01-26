package com.dspot.declex.architecture.handler;

import com.dspot.declex.architecture.annotation.Observable;
import com.dspot.declex.architecture.annotation.Observer;
import com.dspot.declex.architecture.holder.ObserversHolder;
import com.dspot.declex.architecture.holder.ViewModelHolder;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JMethod;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.handler.MethodInjectionHandler;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EComponentHolder;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

import static com.dspot.declex.api.util.FormatsUtils.fieldToGetter;
import static com.dspot.declex.architecture.ArchCanonicalNameConstants.LIFECYCLE_OWNER;
import static com.dspot.declex.architecture.ArchCanonicalNameConstants.VIEW_MODEL;
import static com.dspot.declex.architecture.handler.ObserverHandler.observerNameFor;
import static com.dspot.declex.util.TypeUtils.isSubtype;
import static com.helger.jcodemodel.JExpr.*;

public class BaseInjectionHandler<T extends EComponentHolder> extends BaseAnnotationHandler<T> {

    public BaseInjectionHandler(Class<?> targetClass, AndroidAnnotationsEnvironment environment) {
        super(targetClass, environment);
    }

    @Override
    protected void validate(Element element, ElementValidation validation) {
    }

    @Override
    public void process(Element element, EComponentHolder holder) {

        TypeMirror typeMirror = annotationHelper.extractAnnotationClassParameter(element);
        if (typeMirror == null) {
            typeMirror = element.asType();
            typeMirror = getProcessingEnvironment().getTypeUtils().erasure(typeMirror);
        }

        String typeQualifiedName = typeMirror.toString();
        AbstractJClass EnhancedArchitecturalComponent = getJClass(annotationHelper.generatedClassQualifiedNameFromQualifiedName(typeQualifiedName));


        //Check Observable fields
        final String generatedArchName = TypeUtils.getGeneratedClassName(element, getEnvironment());
        checkObserversFrom(generatedArchName.substring(0, generatedArchName.length() - 1), element, EnhancedArchitecturalComponent, holder);

    }

    protected void performObservation(String fieldName, String referencedClass, Element element, AbstractJClass enhancedClass, EComponentHolder holder) {

        ObserversHolder observersHolder = holder.getPluginHolder(new ObserversHolder(holder));

        //Look for matching observers
        for (Element elem : element.getEnclosingElement().getEnclosedElements()) {

            if (adiHelper.hasAnnotation(elem, Observer.class)) {

                final String observerMethodName = elem.getSimpleName().toString();

                //TODO ensure the parameters are the same type

                if (observerMethodName.equals(fieldName)) {

                    ExecutableElement executableElem = (ExecutableElement) elem;

                    //Register the observer
                    JBlock block;
                    if (this instanceof MethodInjectionHandler) {
                        block = ((MethodInjectionHandler) this).getInvocationBlock(holder);
                    } else {
                        block = holder.getInitBodyAfterInjectionBlock();
                    }

                    IJExpression archField;
                    if (element.asType().toString().endsWith(ModelConstants.generationSuffix())) {
                        archField = ref(element.getSimpleName().toString());
                    } else {
                        archField = cast(enhancedClass, ref(element.getSimpleName().toString()));
                    }

                    String observerGetterName = fieldToGetter(observerNameFor(executableElem, codeModelHelper));

                    if (isSubtype(holder.getAnnotatedElement(), LIFECYCLE_OWNER, getProcessingEnvironment())) {
                        block.add(
                                invoke(archField, fieldToGetter(fieldName)).invoke("observe")
                                        .arg(_this()).arg(invoke(observerGetterName))
                        );
                    } else if (isSubtype(holder.getAnnotatedElement(), VIEW_MODEL, getProcessingEnvironment())) {
                        block.add(
                                invoke(archField, fieldToGetter(fieldName)).invoke("observeForever").arg(invoke(observerGetterName))
                        );

                        //Remove the observer when the ViewModel is not needed anymore
                        ViewModelHolder viewModelHolder = holder.getPluginHolder(new ViewModelHolder(holder));
                        viewModelHolder.getOnClearedMethodBlock().add(
                                invoke(archField, fieldToGetter(fieldName)).invoke("removeObserver").arg(invoke(observerGetterName))
                        );

                    } else {

                        block.add(
                                invoke(archField, fieldToGetter(fieldName)).invoke("observeForever")
                                        .arg(invoke(fieldToGetter(observerNameFor(executableElem, codeModelHelper))))
                        );

                    }

                    //Register the observers which could be removed in the removeObserver method
                    if (!executableElem.getReturnType().toString().equals("void")) {
                        JMethod method = observersHolder.getRemoveObserverMethod();

                        JBlock removeObserverBlock = method.body()
                                ._if(observersHolder.getRemoveObserverMethodParam().eq(invoke(observerGetterName)))._then();

                        removeObserverBlock.add(
                                invoke(archField, fieldToGetter(fieldName)).invoke("removeObserver").arg(observersHolder.getRemoveObserverMethodParam())
                        );
                    }

                }

            }

        }

    }

    private void checkObserversFrom(String archName, Element element, AbstractJClass enhancedClass, EComponentHolder holder) {

        TypeElement archElement = getProcessingEnvironment().getElementUtils().getTypeElement(archName);
        for (Element elem : archElement.getEnclosedElements()) {

            if (adiHelper.hasAnnotation(elem, Observable.class)) {

                final String fieldName = elem.getSimpleName().toString();

                String referencedClass = elem.asType().toString();
                if (referencedClass.contains("<")) {
                    referencedClass = referencedClass.substring(referencedClass.indexOf("<") + 1, referencedClass.length() - 1);
                }

                performObservation(fieldName, referencedClass, element, enhancedClass, holder);

            }

        }

        //Look for observers in the super classes
        List<? extends TypeMirror> superTypes = getProcessingEnvironment().getTypeUtils().directSupertypes(archElement.asType());
        for (TypeMirror type : superTypes) {
            checkObserversFrom(type.toString(), element, enhancedClass, holder);
        }

    }

}
