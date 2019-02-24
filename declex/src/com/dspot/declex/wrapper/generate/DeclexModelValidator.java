package com.dspot.declex.wrapper.generate;

import com.dspot.declex.annotation.ExportPopulate;
import com.dspot.declex.annotation.ExportRecollect;
import com.dspot.declex.annotation.Model;
import com.dspot.declex.annotation.Populate;
import com.dspot.declex.annotation.Recollect;
import com.dspot.declex.util.TypeUtils;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.export.Export;
import org.androidannotations.annotations.export.Exported;
import org.androidannotations.handler.AnnotationHandler;
import org.androidannotations.helper.ADIHelper;
import org.androidannotations.internal.model.AnnotationElements;
import org.androidannotations.internal.model.AnnotationElementsHolder;
import org.androidannotations.internal.process.ModelValidator;
import org.androidannotations.internal.virtual.VirtualElement;
import org.androidannotations.logger.Logger;
import org.androidannotations.logger.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

public class DeclexModelValidator  {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelValidator.class);
    private AndroidAnnotationsEnvironment environment;

    private Map<String, Map<Element, Element>> beanAndModelParents;
    private List<VirtualElement> paramsWaitingForItsVirtualMethod;

    public DeclexModelValidator(AndroidAnnotationsEnvironment environment) {
        this.environment = environment;
    }

    public AnnotationElements validate(AnnotationElements extractedModel, AnnotationElementsHolder validatingHolder) {

        LOGGER.info("Validating elements");

        /*
         * We currently do not validate the elements on the ancestors, assuming they've
         * already been validated. This also means some checks such as unique ids might
         * not be check all situations.
         */

        final ADIHelper adiHelper = new ADIHelper(environment);
        final Map<Element, Set<Element>> virtualElementsMap = new HashMap<>();

        // Beans and Models pair <parent, reference where the bean is used>
        beanAndModelParents = new HashMap<>();
        paramsWaitingForItsVirtualMethod = new LinkedList<>();

        Set<Element> beanAndModelAnnotatedElements = null;

        for (AnnotationHandler annotationHandler : environment.getHandlers()) {

            if (!annotationHandler.isEnabled()) {
                continue;
            }

            String validatorSimpleName = annotationHandler.getClass().getSimpleName();
            String annotationName = annotationHandler.getTarget();

            Set<? extends Element> annotatedElements = extractedModel.getRootAnnotatedElements(annotationName);

            Set<Element> validatedAnnotatedElements = new LinkedHashSet<>();

            validatingHolder.putRootAnnotatedElements(annotationName, validatedAnnotatedElements);

            if (!annotatedElements.isEmpty()) {
                LOGGER.debug("Validating with {}: {}", validatorSimpleName, annotatedElements);
            }

            for (Element realAnnotatedElement : annotatedElements) {
                try {

                    final boolean isExported = adiHelper.hasAnnotation(realAnnotatedElement, Export.class);
                    if (isExported && !(realAnnotatedElement instanceof VirtualElement)) {
                        if (!annotationHandler.getTarget().equals(Export.class.getCanonicalName()) && !realAnnotatedElement.getKind().isField()) {
                            continue;
                        }
                    }

                    Set<Element> allAnnotatedElements = new HashSet<>();
                    allAnnotatedElements.add(realAnnotatedElement);

                    final boolean hasExported = adiHelper.hasAnnotation(realAnnotatedElement, Exported.class);
                    final boolean hasExportPopulate = adiHelper.hasAnnotation(realAnnotatedElement, ExportPopulate.class);
                    final boolean hasExportRecollect = adiHelper.hasAnnotation(realAnnotatedElement, ExportRecollect.class);

                    if (annotationHandler.getTarget().equals(Exported.class.getCanonicalName()) && realAnnotatedElement.getKind().equals(ElementKind.CLASS)) {
                        // @Exported is not processed when it is over a CLASS
                        // this is intended to be used only for ADI purposes
                        continue;
                    }

                    // This kind of annotation will be processed only in the parents (containers of
                    // Beans and Models)
                    if ((hasExported || hasExportPopulate || hasExportRecollect) && !realAnnotatedElement.getKind().equals(ElementKind.CLASS)) {

                        if (beanAndModelAnnotatedElements == null) {
                            beanAndModelAnnotatedElements = new HashSet<>();

                            beanAndModelAnnotatedElements.addAll(extractedModel.getRootAnnotatedElements(Bean.class.getCanonicalName()));
                            beanAndModelAnnotatedElements.addAll(extractedModel.getRootAnnotatedElements(Model.class.getCanonicalName()));

                            for (Element element : beanAndModelAnnotatedElements) {
                                final Element rootElement = TypeUtils.getRootElement(element);
                                TypeUtils.ClassInformation info = TypeUtils.getClassInformation(element, environment);

                                Map<Element, Element> parents = beanAndModelParents.get(info.generatorClassName);
                                if (parents == null) {
                                    parents = new HashMap<>();
                                    beanAndModelParents.put(info.generatorClassName, parents);
                                }

                                parents.put(rootElement, element);
                            }
                        }

                        if (!virtualElementsMap.containsKey(realAnnotatedElement)) {
                            final Element rootElement = TypeUtils.getRootElement(realAnnotatedElement);
                            final String rootElementClass = rootElement.asType().toString();

                            final Set<Element> virtualElements = new HashSet<>();
                            virtualElementsMap.put(realAnnotatedElement, virtualElements);

                            // TODO check if it is ancestor, and create virtual elements for all the not
                            // ancestors
                            if (validatingHolder.isAncestor(rootElement)) {

                                Set<AnnotationElements.AnnotatedAndRootElements> subClassesElements = validatingHolder.getAncestorSubClassesElements(rootElement);
                                for (AnnotationElements.AnnotatedAndRootElements subClass : subClassesElements) {
                                    if (validatingHolder.isAncestor(subClass.rootTypeElement)) {
                                        continue;
                                    }
                                    createVirtualElements(subClass.rootTypeElement.asType().toString(), realAnnotatedElement, virtualElements);
                                }

                            } else {
                                createVirtualElements(rootElementClass, realAnnotatedElement, virtualElements);
                            }

                        }

                        // Use the virtual element
                        if (hasExported) {
                            allAnnotatedElements = virtualElementsMap.get(realAnnotatedElement);
                        }

                        if (hasExportPopulate) {

                            // @Populate will be applied in this case only to the virtual elements
                            if (annotationHandler.getTarget().equals(Populate.class.getCanonicalName())) {
                                allAnnotatedElements = virtualElementsMap.get(realAnnotatedElement);
                            }

                            // @ExportPopulate is applied to the real and virtual elements as well
                            if (annotationHandler.getTarget().equals(ExportPopulate.class.getCanonicalName())) {
                                allAnnotatedElements.addAll(virtualElementsMap.get(realAnnotatedElement));
                            }

                        }

                        if (hasExportRecollect) {
                            // @Recollect will be applied in this case only to the virtual elements
                            if (annotationHandler.getTarget().equals(Recollect.class.getCanonicalName())) {
                                allAnnotatedElements = virtualElementsMap.get(realAnnotatedElement);
                            }

                            // @ExportRecollect is applied to the real and virtual elements as well
                            if (annotationHandler.getTarget().equals(ExportRecollect.class.getCanonicalName())) {
                                allAnnotatedElements.addAll(virtualElementsMap.get(realAnnotatedElement));
                            }
                        }
                    }

                    for (Element annotatedElement : allAnnotatedElements) {
                        ElementValidation elementValidation = annotationHandler.validate(annotatedElement);

                        AnnotationMirror annotationMirror = elementValidation.getAnnotationMirror();
                        for (ElementValidation.Error error : elementValidation.getErrors()) {
                            LOGGER.error(error.getElement(), annotationMirror, error.getMessage());
                        }

                        for (String warning : elementValidation.getWarnings()) {
                            LOGGER.warn(elementValidation.getElement(), annotationMirror, warning);
                        }

                        if (elementValidation.isValid()) {
                            validatedAnnotatedElements.add(annotatedElement);
                        } else {
                            LOGGER.warn(annotatedElement, "Element {} invalidated by {}", annotatedElement, validatorSimpleName);
                        }
                    }

                } catch (Throwable e) {
                    LOGGER.error("Internal crash while validating element {} with annotation {}. \n" + "Please report this in " + annotationHandler.getAndroidAnnotationPlugin().getIssuesUrl(),
                            realAnnotatedElement, annotationName);
                    LOGGER.error("Crash Report: {}", e);
                }
            }
        }

        return validatingHolder;
    }

    private void createVirtualElements(String beanElementClass, Element realAnnotatedElement, Set<Element> virtualElements) {

        Set<Class<? extends Annotation>> adiForElement = environment.getADIOnElement(realAnnotatedElement);

        if (beanAndModelParents.containsKey(beanElementClass)) {
            for (Map.Entry<Element, Element> parentReference : beanAndModelParents.get(beanElementClass).entrySet()) {
                VirtualElement virtualElement = VirtualElement.from(realAnnotatedElement);

                if (realAnnotatedElement.getKind() == ElementKind.PARAMETER) {
                    VirtualElement virtualParent = null;

                    List<Element> rootElementVirtualEnclosed = VirtualElement.getVirtualEnclosedElements(parentReference.getKey());
                    for (Element elem : rootElementVirtualEnclosed) {
                        if (((VirtualElement) elem).getElement().equals(realAnnotatedElement.getEnclosingElement())) {
                            virtualParent = (VirtualElement) elem;
                            break;
                        }
                    }

                    if (virtualParent == null) {
                        paramsWaitingForItsVirtualMethod.add(virtualElement);

                        VirtualElement temporalParent = VirtualElement.from(realAnnotatedElement.getEnclosingElement());
                        temporalParent.setTemporal();
                        temporalParent.setEnclosingElement(parentReference.getKey());
                        temporalParent.setReference(parentReference.getValue());

                        virtualElement.setEnclosingElement(temporalParent);
                    } else {
                        virtualElement.setEnclosingElement(virtualParent);
                    }
                } else {

                    if (realAnnotatedElement.getKind() == ElementKind.METHOD) {
                        int i = 0;
                        while (i < paramsWaitingForItsVirtualMethod.size()) {
                            VirtualElement param = paramsWaitingForItsVirtualMethod.get(i);
                            if (param.getElement().getEnclosingElement().equals(realAnnotatedElement)) {
                                paramsWaitingForItsVirtualMethod.remove(i);
                                param.setEnclosingElement(virtualElement);
                                continue;
                            }
                            i++;
                        }
                    }

                    virtualElement.setEnclosingElement(parentReference.getKey());
                }

                virtualElement.setReference(parentReference.getValue());

                // Add ADI to virtual elements
                for (Class<? extends Annotation> annotation : adiForElement) {
                    environment.addAnnotationToADI(virtualElement, annotation);
                }

                virtualElements.add(virtualElement);

            }
        }
    }

}
