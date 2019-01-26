package com.dspot.declex.architecture.handler;

import com.dspot.declex.architecture.annotation.ArchInject;
import com.dspot.declex.architecture.annotation.EViewModel;
import com.dspot.declex.holder.ViewsHolder;
import com.dspot.declex.util.TypeUtils;
import com.helger.jcodemodel.*;
import javafx.scene.shape.Arc;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.Bean;
import org.androidannotations.handler.MethodInjectionHandler;
import org.androidannotations.helper.InjectHelper;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EActivityHolder;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.holder.EComponentWithViewSupportHolder;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static com.dspot.declex.api.util.FormatsUtils.fieldToGetter;
import static com.dspot.declex.architecture.ArchCanonicalNameConstants.*;
import static com.dspot.declex.util.TypeUtils.isSubtype;
import static com.helger.jcodemodel.JExpr.*;
import static org.androidannotations.helper.CanonicalNameConstants.BOOLEAN;
import static org.androidannotations.helper.CanonicalNameConstants.FRAGMENT_ACTIVITY;

public class ArchInjectHandler extends BaseInjectionHandler<EComponentWithViewSupportHolder> implements MethodInjectionHandler<EComponentWithViewSupportHolder> {

    private final InjectHelper<EComponentWithViewSupportHolder> injectHelper;

    private final Map<EActivityHolder, JBlock> blockAfterSuperCallPerHolder = new HashMap<>();

    public ArchInjectHandler(AndroidAnnotationsEnvironment environment) {
        super(ArchInject.class, environment);

        injectHelper = new InjectHelper<>(validatorHelper, this);

    }

    @Override
    public void validate(Element element, ElementValidation validation) {

        //Don't validate further if annotated with EBean
        if (adiHelper.hasAnnotation(element, Bean.class)) return;

        injectHelper.validate(ArchInject.class, element, validation);
        if (!validation.isValid()) {
            return;
        }

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

    @Override
    public JBlock getInvocationBlock(EComponentWithViewSupportHolder holder) {

        if (holder instanceof EActivityHolder) {
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
    public void assignValue(JBlock targetBlock, IJAssignmentTarget fieldRef, EComponentWithViewSupportHolder holder, Element element, Element param) {

        TypeMirror typeMirror = annotationHelper.extractAnnotationClassParameter(element);
        if (typeMirror == null) {
            typeMirror = param.asType();
            typeMirror = getProcessingEnvironment().getTypeUtils().erasure(typeMirror);
        }

        String typeQualifiedName = typeMirror.toString();
        AbstractJClass enhancedClass = getJClass(annotationHelper.generatedClassQualifiedNameFromQualifiedName(typeQualifiedName));

        //Inject the ViewModel
        ArchInject archInject = adiHelper.getAnnotation(element, ArchInject.class);

        JInvocation archInstance;
        switch (archInject.scope()) {
            case Activity:

                //Add check for Lifecycle Owner Content
                JBlock checkLifcycleBlock = holder.getInitBodyBeforeInjectionBlock()
                        ._if(holder.getContextRef()._instanceof(getJClass(FRAGMENT_ACTIVITY)).not())._then();

                checkLifcycleBlock._throw(_new(getJClass(IllegalStateException.class))
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

        IJExpression archField;
        if (element.asType().toString().endsWith(ModelConstants.generationSuffix())) {
            archField = fieldRef;
        } else {
            archField = cast(enhancedClass, fieldRef);
        }
        targetBlock.invoke(archField, "rebind").arg(holder.getContextRef());

    }

    @Override
    public void validateEnclosingElement(Element element, ElementValidation valid) {
        validatorHelper.enclosingElementHasEnhancedComponentAnnotation(element, valid);
    }

    @Override
    protected void performObservation(String fieldName, String referencedClass, Element element, AbstractJClass enhancedClass, EComponentHolder holder) {

        super.performObservation(fieldName, referencedClass, element, enhancedClass, holder);

        final ViewsHolder viewsHolder = holder.getPluginHolder(new ViewsHolder((EComponentWithViewSupportHolder) holder));

        //Determine if the observer can be bound to a visual component property
        ViewsHolder.ViewProperty viewProperty = viewsHolder.getPropertySetterForFieldName(fieldName, referencedClass);
        if (viewProperty != null) {

            bindObserver(viewProperty,  fieldName, referencedClass, element, enhancedClass, viewsHolder.holder());

        }

    }

    private void bindObserver(ViewsHolder.ViewProperty viewProperty, String fieldName, String referencedClass,
                              Element element, AbstractJClass enhancedClass, EComponentWithViewSupportHolder holder) {

        AbstractJClass Observer = getJClass(OBSERVER);
        AbstractJClass ReferencedClass = getJClass(referencedClass);

        JDefinedClass AnonymousObserver = getCodeModel().anonymousClass(Observer.narrow(ReferencedClass));
        JMethod anonymousOnChanged = AnonymousObserver.method(JMod.PUBLIC, getCodeModel().VOID, "onChanged");
        anonymousOnChanged.annotate(Override.class);
        JVar param = anonymousOnChanged.param(ReferencedClass, "value");

        assignProperty(viewProperty, referencedClass, param, anonymousOnChanged.body());

        JBlock block = holder.getOnViewChangedBodyAfterInjectionBlock().block();
        JVar observer = block.decl(Observer.narrow(ReferencedClass), "observer", _new(AnonymousObserver));

        IJExpression archField;
        if (element.asType().toString().endsWith(ModelConstants.generationSuffix())) {
            archField = ref(element.getSimpleName().toString());
        } else {
            archField = cast(enhancedClass, ref(element.getSimpleName().toString()));
        }


        block.add(invoke(archField, fieldToGetter(fieldName)).invoke("observe").arg(_this()).arg(observer));

    }

    private void assignProperty(ViewsHolder.ViewProperty viewProperty, String referencedClass, JVar param, JBlock block) {

        final String viewClass = viewProperty.viewClass.fullName();
        final JFieldRef view = viewProperty.view;

        if (viewProperty.property == null) {

            //CompoundButtons, if the param is boolean, it will set the checked state
            if (TypeUtils.isSubtype(viewClass, "android.widget.CompoundButton", getProcessingEnvironment())) {
                if (referencedClass.equals("boolean") || referencedClass.equals(BOOLEAN)) {
                    block.invoke(view, "setChecked").arg(param);

                    //This ensures not to check against TextView (since a CompoundButton is a TextView descendant)
                    return;
                }
            }

            if (isSubtype(viewClass, "android.widget.TextView", getProcessingEnvironment())) {
                if (isSubtype(referencedClass, "android.text.Spanned", getProcessingEnvironment())) {
                    block.invoke(view, "setText").arg(param);
                } else if (isSubtype(referencedClass, CharSequence.class.getCanonicalName(), getProcessingEnvironment())) {
                    block.invoke(view, "setText").arg(param);
                } else {
                    block.invoke(view, "setText").arg(getJClass(String.class).staticInvoke("valueOf").arg(param));
                }
            }

        } else {

            block.invoke(view, "set" + viewProperty.property).arg(param);

        }

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
