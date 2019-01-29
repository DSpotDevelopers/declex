package com.dspot.declex.architecture.handler;

import com.dspot.declex.architecture.annotation.ArchInject;
import com.dspot.declex.architecture.annotation.EViewModel;
import com.dspot.declex.architecture.annotation.EViewPresenter;
import com.dspot.declex.architecture.helper.ViewsLinkingListenersHelper;
import com.dspot.declex.architecture.helper.ViewsLinkingObservablesHelper;
import com.dspot.declex.architecture.holder.ViewModelHolder;
import com.dspot.declex.architecture.holder.ViewPresenterHolder;
import com.helger.jcodemodel.*;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.Bean;
import org.androidannotations.handler.MethodInjectionHandler;
import org.androidannotations.helper.InjectHelper;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.*;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static com.dspot.declex.api.util.FormatsUtils.fieldToGetter;
import static com.dspot.declex.architecture.ArchCanonicalNameConstants.VIEW_MODEL_PROVIDERS;
import static com.dspot.declex.util.TypeUtils.getGeneratedClassName;
import static com.helger.jcodemodel.JExpr.*;
import static org.androidannotations.helper.CanonicalNameConstants.FRAGMENT_ACTIVITY;

public class ArchInjectHandler extends ObserversLinkingHandler<EComponentWithViewSupportHolder> implements MethodInjectionHandler<EComponentWithViewSupportHolder> {

    private final InjectHelper<EComponentWithViewSupportHolder> injectHelper;

    private final ViewsLinkingObservablesHelper viewsLinkingObservablesHelper;

    private final ViewsLinkingListenersHelper viewsLinkingListenersHelper;

    private final Map<EActivityHolder, JBlock> blockAfterSuperCallPerHolder = new HashMap<>();

    public ArchInjectHandler(AndroidAnnotationsEnvironment environment) {
        super(ArchInject.class, environment);

        injectHelper = new InjectHelper<>(validatorHelper, this);
        viewsLinkingObservablesHelper = new ViewsLinkingObservablesHelper(environment);
        viewsLinkingListenersHelper = new ViewsLinkingListenersHelper(environment);

    }

    @Override
    public void validate(Element element, ElementValidation validation) {

        //Don't validate further if annotated with EBean
        if (adiHelper.hasAnnotation(element, Bean.class)) return;

        injectHelper.validate(ArchInject.class, element, validation);
        if (!validation.isValid()) {
            return;
        }

        if (isViewPresenter(element)) {
            return;
        }

        //Validator for View Models
        {
            validatorHelper.typeOrTargetValueHasAnnotation(EViewModel.class, element, validation);

            validatorHelper.isNotPrivate(element, validation);

            ArchInject archInject = adiHelper.getAnnotation(element, ArchInject.class);
            if (archInject.scope() == ArchInject.Scope.Default) {
                boolean wasValid = validation.isValid();
                validatorHelper.enclosingElementHasEActivityOrEFragmentOrEViewOrEViewGroup(element, validation);

                if (wasValid && !validation.isValid()) {
                    validation.addWarning("If you intend to inject it from within an activity context, use implicitly \"scope = Activity\"");
                }
            }
        }

    }

    @Override
    public JBlock getInvocationBlock(Element element, EComponentWithViewSupportHolder holder) {

        if (holder instanceof EActivityHolder && !isViewPresenter(element)) {
            return getBlockAfterSuperCall((EActivityHolder) holder);
        }

        return holder.getInitBodyInjectionBlock();
    }

    @Override
    public void process(Element element, EComponentWithViewSupportHolder holder) {

        //Don't inject if annotated with Bean
        if (!adiHelper.hasAnnotation(element, Bean.class)) {
            injectHelper.process(element, holder);
        }

        super.process(element, holder);

    }

    @Override
    protected void process(String generatorClassName, Element injectingElement, AbstractJClass injectingEnhancedClass,
                           EComponentHolder injectingElementHolder) {

        super.process(generatorClassName, injectingElement, injectingEnhancedClass, injectingElementHolder);

        viewsLinkingListenersHelper.process(generatorClassName, injectingElement, injectingEnhancedClass,
                (EComponentWithViewSupportHolder) injectingElementHolder);
    }

    @Override
    public void assignValue(JBlock targetBlock, IJAssignmentTarget fieldRef, EComponentWithViewSupportHolder holder, Element element, Element param) {

        TypeMirror typeMirror = annotationHelper.extractAnnotationClassParameter(element);
        if (typeMirror == null) {
            typeMirror = param.asType();
            typeMirror = getProcessingEnvironment().getTypeUtils().erasure(typeMirror);
        }

        String typeQualifiedName = typeMirror.toString();
        AbstractJClass enhancedClass = getJClass(annotationHelper.generatedClassQualifiedNameFromQualifiedName(typeQualifiedName));

        IJExpression injectingField;
        if (element.asType().toString().endsWith(ModelConstants.generationSuffix())) {
            injectingField = fieldRef;
        } else {
            injectingField = cast(enhancedClass, fieldRef);
        }

        if (isViewPresenter(element)) {
            IJStatement assignment = fieldRef.assign(_new(enhancedClass));
            targetBlock.add(assignment);
        } else {

            //Inject the ViewModel
            ArchInject archInject = adiHelper.getAnnotation(element, ArchInject.class);

            JInvocation archInstance;
            switch (archInject.scope()) {
                case Activity:

                    //Add check for Lifecycle Owner Content
                    JBlock checkLifecycleBlock = holder.getInitBodyBeforeInjectionBlock()
                            ._if(holder.getContextRef()._instanceof(getJClass(FRAGMENT_ACTIVITY)).not())._then();

                    checkLifecycleBlock._throw(_new(getJClass(IllegalStateException.class))
                            .arg("This Bean can only be injected in the context of a FragmentActivity"));

                    archInstance = getJClass(VIEW_MODEL_PROVIDERS)
                            .staticInvoke("of").arg(cast(getJClass(FRAGMENT_ACTIVITY), holder.getContextRef()))
                            .invoke("get").arg(enhancedClass.dotclass());
                    break;

                default:
                    archInstance = getJClass(VIEW_MODEL_PROVIDERS)
                            .staticInvoke("of").arg(_this())
                            .invoke("get").arg(enhancedClass.dotclass());
            }

            IJStatement assignment = fieldRef.assign(archInstance);
            targetBlock.add(assignment);

        }

        //Call the rebind method
        JInvocation rebindInvoke = targetBlock.invoke(injectingField, ViewModelHolder.REBIND_NAME).arg(holder.getContextRef());
        if (holder instanceof EActivityHolder || holder instanceof EFragmentHolder) {
            rebindInvoke.arg(_this());
        } else {

            //Could be @EViewModel or @EViewPresenter
            if (adiHelper.hasAnnotation(holder.getAnnotatedElement(), EViewModel.class)) {
                ViewModelHolder viewModelHolder = holder.getPluginHolder(new ViewModelHolder(holder));
                rebindInvoke.arg(viewModelHolder.getRootViewField());
            } else if (adiHelper.hasAnnotation(holder.getAnnotatedElement(), EViewPresenter.class)) {
                ViewPresenterHolder viewPresenterHolder = holder.getPluginHolder(new ViewPresenterHolder(holder));
                rebindInvoke.arg(viewPresenterHolder.getRootViewField());
            }

        }

    }

    @Override
    public void validateEnclosingElement(Element element, ElementValidation valid) {
        validatorHelper.enclosingElementHasEnhancedComponentAnnotation(element, valid);
    }

    @Override
    protected void performObservation(String observableFieldName, String observableTypeName, Element injectingElement,
                                      IJExpression injectingField, AbstractJClass injectingEnhancedClass, EComponentHolder injectingElementHolder) {

        super.performObservation(observableFieldName, observableTypeName, injectingElement, injectingField, injectingEnhancedClass, injectingElementHolder);

        viewsLinkingObservablesHelper.linkViewToObservable(observableFieldName, observableTypeName, injectingElement, injectingEnhancedClass, injectingElementHolder);

    }


    private JBlock getBlockAfterSuperCall(EActivityHolder holder) {

        if (blockAfterSuperCallPerHolder.containsKey(holder)) {
            return blockAfterSuperCallPerHolder.get(holder);
        }

        JMethod onCreateMethod = holder.getOnCreate();
        JBlock previousBody = codeModelHelper.removeBody(onCreateMethod);
        JBlock newBody = onCreateMethod.body();
        JBlock blockAfterSuper = new JBlock();

        //TODO Replace calls to super, if any
        for (Object content : previousBody.getContents()) {

            if (content instanceof IJStatement) {

                StringWriter writer = new StringWriter();
                JFormatter formatter = new JFormatter(writer);
                IJStatement statement = (IJStatement) content;
                statement.state(formatter);
                String statementString = writer.getBuffer().toString();

                if (statementString.trim().startsWith("super.")) {
                    newBody.add((IJStatement) content);
                    newBody.add(blockAfterSuper);
                    continue;
                }

            }

            if (content instanceof JVar) {
                JVar var = (JVar) content;
                try {
                    java.lang.reflect.Field varInitField = JVar.class.getDeclaredField("m_aInitExpr");
                    varInitField.setAccessible(true);
                    IJExpression varInit = (IJExpression) varInitField.get(var);

                    newBody.decl(var.type(), var.name(), varInit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                newBody.add((IJStatement) content);
            }

        }

        blockAfterSuperCallPerHolder.put(holder, blockAfterSuper);

        return blockAfterSuper;

    }

}
