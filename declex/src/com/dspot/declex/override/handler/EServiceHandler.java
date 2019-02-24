package com.dspot.declex.override.handler;

import com.dspot.declex.helper.ActionHelper;
import com.dspot.declex.holder.EventHolder;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.ElementValidation;
import org.androidannotations.holder.EServiceHolder;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

public class EServiceHandler extends org.androidannotations.internal.core.handler.EServiceHandler {

    public EServiceHandler(AndroidAnnotationsEnvironment environment) {
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
    public void process(Element element, EServiceHolder holder) {
        super.process(element, holder);

        EventHolder eventHolder = holder.getPluginHolder(new EventHolder(holder));
        holder.getInit(); //This avoids a crash due to the method not being created
        eventHolder.setEventRegisteringBlock(holder.getStartLifecycleAfterSuperBlock());
    }
}
