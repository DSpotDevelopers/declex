package com.dspot.declex.architecture.helper;

import com.dspot.declex.DeclexProcessor;
import com.dspot.declex.architecture.annotation.listener.Listen;
import com.dspot.declex.holder.ViewsHolder;
import com.dspot.declex.parser.LayoutsParser;
import com.dspot.declex.util.ParamUtils;
import com.helger.jcodemodel.*;
import com.sun.tools.javac.code.Attribute;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.helper.APTCodeModelHelper;
import org.androidannotations.helper.ModelConstants;
import org.androidannotations.holder.EComponentWithViewSupportHolder;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;
import org.androidannotations.rclass.IRClass;
import org.androidannotations.rclass.IRInnerClass;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.*;

import static com.dspot.declex.util.ParamUtils.injectParam;
import static com.dspot.declex.util.TypeUtils.getGeneratedClassName;
import static com.helger.jcodemodel.JExpr.*;
import static com.helger.jcodemodel.JExpr.ref;
import static javax.lang.model.element.ElementKind.METHOD;
import static org.androidannotations.helper.ModelConstants.generationSuffix;

public class ViewsLinkingListenersHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeclexProcessor.class);

    private final AndroidAnnotationsEnvironment environment;

    private final APTCodeModelHelper codeModelHelper;

    public ViewsLinkingListenersHelper(AndroidAnnotationsEnvironment environment) {
        this.environment = environment;
        codeModelHelper = new APTCodeModelHelper(environment);
    }

    public void process(String generatorClassName, Element injectingElement, AbstractJClass injectingEnhancedClass,
                        EComponentWithViewSupportHolder injectingElementHolder) {

        TypeElement generatorTypeElement = getProcessingEnvironment().getElementUtils().getTypeElement(generatorClassName);
        for (Element elem : generatorTypeElement.getEnclosedElements()) {

            List<? extends AnnotationMirror> elemAnnotations = elem.getAnnotationMirrors();
            for (AnnotationMirror annotationMirror : elemAnnotations) {

                //Process any annotation marked with Listen
                DeclaredType annotationType = annotationMirror.getAnnotationType();
                if (annotationType.asElement().getAnnotation(Listen.class) != null) {

                    Listen listenAnnotation = annotationType.asElement().getAnnotation(Listen.class);
                    int[] value = null;
                    String[] resName = null;

                    Map<? extends ExecutableElement, ? extends AnnotationValue> parameters = annotationMirror.getElementValues();
                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> param : parameters.entrySet()) {

                        if (param.getKey().getSimpleName().toString().equals("value")
                            && param.getKey().getReturnType().toString().equals("int[]")) {
                            List<Attribute.Constant> valueList = (List<Attribute.Constant>) param.getValue().getValue();
                            value = new int[valueList.size()];

                            for (int i = 0; i < valueList.size(); i++) {
                                value[i] = (int)valueList.get(i).getValue();
                            }

                        }

                        if (param.getKey().getSimpleName().toString().equals("resName")
                                && param.getKey().getReturnType().toString().equals("java.lang.String[]")) {
                            List<Attribute.Constant> valueList = (List<Attribute.Constant>) param.getValue().getValue();
                            resName = new String[valueList.size()];

                            for (int i = 0; i < valueList.size(); i++) {
                                resName[i] = (String)valueList.get(i).getValue();
                            }
                        }

                    }


                    processListener(value, resName, listenAnnotation.listeners(), listenAnnotation.validEndings(),
                            (ExecutableElement) elem, injectingElement, injectingEnhancedClass, injectingElementHolder);

                }

                //Process Listen Annotation
                if (annotationType.toString().equals(Listen.class.getCanonicalName())) {
                    Listen annotation = elem.getAnnotation(Listen.class);
                    processListener(annotation.value(), annotation.resName(), annotation.listeners(), annotation.validEndings(),
                            (ExecutableElement) elem, injectingElement, injectingEnhancedClass, injectingElementHolder);
                }

            }

        }

        //Look for Listeners in the super classes
        List<? extends TypeMirror> superTypes = getProcessingEnvironment().getTypeUtils().directSupertypes(generatorTypeElement.asType());
        for (TypeMirror type : superTypes) {
            process(type.toString(), injectingElement, injectingEnhancedClass, injectingElementHolder);
        }

    }

    private void processListener(int[] values, String[] resNames, String[] listeners, String[] validEndings,
                                 ExecutableElement listenerElement, Element injectingElement, AbstractJClass injectingEnhancedClass,
                                 EComponentWithViewSupportHolder injectingElementHolder) {

        System.out.println("PP:: " + listenerElement + ": values=" + Arrays.toString(values) + ", resNames=" + Arrays.toString(resNames)
                + ", listeners=" + Arrays.toString(listeners) + ", validEndings=" + Arrays.toString(validEndings));

        //Get all the valid Ids
        final Set<String> validIds = new HashSet<>();
        final String listenerElementName = listenerElement.getSimpleName().toString();

        if (values != null) {
            for (int value : values) {
                IRInnerClass rInnerClass = environment.getRClass().get(IRClass.Res.ID);
                if (rInnerClass.containsIdValue(value)) {
                    String qualifiedName = rInnerClass.getIdQualifiedName(value);
                    validIds.add(qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1));
                }
            }
        }

        if (resNames != null) {
            validIds.addAll(Arrays.asList(resNames));
        }

        if (validEndings != null) {

            String shorterId = null;
            for (String ending : validEndings) {
                if (ending != null && !ending.isEmpty() && listenerElementName.endsWith(ending)) {

                    String shortedId = listenerElementName.substring(0, listenerElementName.length() - ending.length());

                    if (shorterId == null || (shortedId.length() < shorterId.length())) {
                        shorterId = shortedId;
                    }

                }
            }

            if (shorterId != null) {
                validIds.add(shorterId);
            }

        }

        validIds.add(listenerElementName);


        //Check which of those views exists
        final ViewsHolder viewsHolder = injectingElementHolder.getPluginHolder(new ViewsHolder(injectingElementHolder));
        for (String id : validIds) {
            if (viewsHolder.layoutContainsId(id)) {

                LayoutsParser.LayoutObject layoutObject = viewsHolder.getLayoutObject(id);
                placeListenerIn(layoutObject, listeners, listenerElement, injectingElement, injectingEnhancedClass, injectingElementHolder);

            }
        }

    }

    private void placeListenerIn(LayoutsParser.LayoutObject layoutObject, String[] listeners, ExecutableElement listenerElement,
                                 Element injectingElement, AbstractJClass injectingEnhancedClass, EComponentWithViewSupportHolder injectingElementHolder) {

        String className = layoutObject.className;
        if (className.endsWith(generationSuffix())) {
            className = className.substring(0, className.length() - 1);
        }

        TypeElement viewClassElement = getProcessingEnvironment().getElementUtils().getTypeElement(className);
        if (viewClassElement == null) {
            LOGGER.warn("This listener seems to point to a view which class cannot be found. View Class: " + className, listenerElement);
            return;
        }

        //Determine Valid Setters Name to look for
        final Map<String, String> validSettersNameWithOptions = new HashMap<>();
        for (String listener : listeners) {

            String options = null;

            if (listener.contains("::")) {
                options = listener.substring(listener.indexOf("::")).trim();
                listener = listener.substring(0, listener.indexOf("::"));
            }

            if (listener.contains("->")) {
                options = listener.substring(listener.indexOf("->")).trim();
                listener = listener.substring(0, listener.indexOf("->"));
            }

            listener = listener.trim();

            validSettersNameWithOptions.put("set" + listener, options);
            validSettersNameWithOptions.put("add" + listener, options);

            if (!listener.endsWith("Listener")) {
                validSettersNameWithOptions.put("set" + listener + "Listener", options);
                validSettersNameWithOptions.put("add" + listener + "Listener", options);
            }

        }

        System.out.println("DD::Looking for " + validSettersNameWithOptions);

        Map<ExecutableElement, String> listenersSettersWithOptions = new HashMap<>();
        findSettersForListeners(viewClassElement, validSettersNameWithOptions, listenersSettersWithOptions);
        if (listenersSettersWithOptions.isEmpty()) return;

        System.out.println("DD::Found " + listenersSettersWithOptions);

        instantiateListenersAndLink(listenersSettersWithOptions, layoutObject, listenerElement, injectingElement, injectingElementHolder);

    }

    private void instantiateListenersAndLink(Map<ExecutableElement, String> listenersSettersWithOptions, LayoutsParser.LayoutObject layoutObject,
                                             ExecutableElement listenerElement, Element injectingElement,
                                             EComponentWithViewSupportHolder injectingElementHolder) {

        final ViewsHolder viewsHolder = injectingElementHolder.getPluginHolder(new ViewsHolder(injectingElementHolder));
        final JFieldRef view = viewsHolder.createAndAssignView(layoutObject.id);

        final AbstractJClass viewClass = getJClass(layoutObject.className);
        final JFieldRef idRef = environment.getRClass().get(IRClass.Res.ID).getIdStaticRef(layoutObject.id, environment);

        final JBlock block = injectingElementHolder.getFoundViewHolder(idRef, viewClass).getIfNotNullBlock();

        for (ExecutableElement element : listenersSettersWithOptions.keySet()) {

            //Create anonymous class
            VariableElement paramElement = element.getParameters().get(0);
            TypeElement paramTypeElement = getProcessingEnvironment().getElementUtils().getTypeElement(paramElement.asType().toString());

            if (paramTypeElement == null) {
                LOGGER.warn("The listener parameters couldn't be initialized: " + element, listenerElement);
                continue;
            }

            IJExpression injectingField;
            if (injectingElement.asType().toString().endsWith(ModelConstants.generationSuffix())) {
                injectingField = ref(injectingElement.getSimpleName().toString());
            } else {
                injectingField = cast(getJClass(getGeneratedClassName(injectingElement, environment)), ref(injectingElement.getSimpleName().toString()));
            }

            JInvocation invoke = injectingField.invoke(listenerElement.getSimpleName().toString());
            for (VariableElement param : listenerElement.getParameters()) {
                final String paramName = param.getSimpleName().toString();
                final String paramType = param.asType().toString();
                invoke = injectParam(paramName, paramType, invoke, viewsHolder);
            }

            AbstractJClass anonymousListener = createAnonymousFrom(paramTypeElement, invoke, listenersSettersWithOptions.get(element));
            String setterName = element.getSimpleName().toString();

            block.invoke(view, setterName).arg(_new(anonymousListener));

        }


    }

    private AbstractJClass createAnonymousFrom(TypeElement element, IJStatement statement, String options) {

        //TODO it is assumed a single method, this is not generic

        final AbstractJClass ElementClass = codeModelHelper.elementTypeToJClass(element);
        final JDefinedClass AnonymousClass = getCodeModel().anonymousClass(ElementClass);

        //It is assumed all the methods should be override
        for (Element elem : element.getEnclosedElements()) {

            if (elem.getKind() == METHOD) {

                JMethod anonymousMethod = codeModelHelper.overrideMethod((ExecutableElement) elem, AnonymousClass, element);
                anonymousMethod.body().add(statement);

                //TODO process more options

                if (options != null && options.contains("->")) {
                    anonymousMethod.body().directStatement(options.substring(options.indexOf("->") + 2));
                }

            }

        }

        return AnonymousClass;

    }

    private void findSettersForListeners(TypeElement viewClassElement, Map<String, String> validSettersNames, Map<ExecutableElement, String> listenersSetters) {

        for (Element element : viewClassElement.getEnclosedElements()) {

            if (element.getModifiers().contains(Modifier.PUBLIC) && element.getKind() == METHOD) {

                if (validSettersNames.containsKey(element.getSimpleName().toString())) {
                    ExecutableElement executableElement = (ExecutableElement) element;
                    listenersSetters.put(executableElement, validSettersNames.get(element.getSimpleName().toString()));
                }

            }

        }

        List<? extends TypeMirror> superTypes = getProcessingEnvironment().getTypeUtils().directSupertypes(viewClassElement.asType());
        for (TypeMirror type : superTypes) {
            TypeElement superElement = getProcessingEnvironment().getElementUtils().getTypeElement(type.toString());
            if (superElement == null) continue;
            if (superElement.getKind().equals(ElementKind.INTERFACE)) continue;
            if (superElement.asType().toString().equals(Object.class.getCanonicalName())) break;

            findSettersForListeners(superElement, validSettersNames, listenersSetters);

        }

    }

    private ProcessingEnvironment getProcessingEnvironment() {
        return environment.getProcessingEnvironment();
    }

    private AbstractJClass getJClass(Class<?> clazz) {
        return environment.getJClass(clazz);
    }

    private AbstractJClass getJClass(String fullyQualifiedName) {
        return environment.getJClass(fullyQualifiedName);
    }

    private JCodeModel getCodeModel() {
        return environment.getCodeModel();
    }

}
