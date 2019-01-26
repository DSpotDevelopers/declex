package com.dspot.declex.architecture.handler;

import com.dspot.declex.architecture.annotation.Observable;
import com.helger.jcodemodel.*;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.handler.BaseAnnotationHandler;
import org.androidannotations.holder.EComponentHolder;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import static com.dspot.declex.api.util.FormatsUtils.fieldToGetter;
import static com.helger.jcodemodel.JExpr._new;
import static com.helger.jcodemodel.JExpr._this;

public class ObservableHandler extends BaseAnnotationHandler<EComponentHolder> {

    public ObservableHandler(AndroidAnnotationsEnvironment environment) {
        super(Observable.class, environment);
    }

    @Override
    public void validate(Element element, ElementValidation validation) {

    }

    @Override
    public void process(Element element, EComponentHolder holder) {

        final String fieldName = element.getSimpleName().toString();
        JFieldRef fieldRef = _this().ref(element.getSimpleName().toString());

        //Create the class if it wasn't initialized
        Observable observable = adiHelper.getAnnotation(element, Observable.class);

        //Inject the class if normal constructor found
        if (observable.initialize() && !observable.lazyLoad()) {
            initializeInBlock(element, fieldRef, holder.getInitBodyInjectionBlock()._if(fieldRef.eqNull())._then());
        }

        //Create Getter
        JMethod getterMethod = holder.getGeneratedClass().method(
                JMod.PUBLIC,
                codeModelHelper.elementTypeToJClass(element),
                fieldToGetter(fieldName)
        );

        if (observable.lazyLoad()) {
            initializeInBlock(element, fieldRef, getterMethod.body()._if(fieldRef.eqNull())._then());
        }

        getterMethod.body()._return(_this().ref(fieldName));

    }

    private void initializeInBlock(Element element, JFieldRef fieldRef, JBlock block) {

        String liveDataClassName = element.asType().toString();
        if (liveDataClassName.contains("<")) {
            liveDataClassName = liveDataClassName.substring(0, liveDataClassName.indexOf("<"));
        }

        TypeElement typeElement = getProcessingEnvironment().getElementUtils().getTypeElement(liveDataClassName);

        if (canBeInitialized(typeElement)) {
            AbstractJClass LiveData = codeModelHelper.elementTypeToJClass(element);
            block.assign(fieldRef, _new(LiveData));
        }
    }

    private boolean canBeInitialized(TypeElement typeElement) {
        if (typeElement.getModifiers().contains(Modifier.ABSTRACT)) return false;
        return true;
    }

}
