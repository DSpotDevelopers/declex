package com.dspot.declex.override.handler;

import com.dspot.declex.helper.ActionHelper;
import com.dspot.declex.holder.EventHolder;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.holder.EProviderHolder;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

public class EProviderHandler extends org.androidannotations.internal.core.handler.EProviderHandler {

    public EProviderHandler(AndroidAnnotationsEnvironment environment) {
        super(environment);
    }

    @Override
    public void validate(Element element, ElementValidation valid) {
        if (element.getKind().equals(ElementKind.CLASS)) {
            ActionHelper.getInstance(getEnvironment()).validate(element, this);
        }

        super.validate(element, valid);
    }

    @Override
    public void process(Element element, EProviderHolder holder) {
        super.process(element, holder);

        EventHolder eventHolder = holder.getPluginHolder(new EventHolder(holder));
        eventHolder.setEventRegisteringBlock(holder.getInitBodyBeforeInjectionBlock());
    }

}
