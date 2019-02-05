package com.dspot.declex.override.handler;

import com.dspot.declex.holder.EventHolder;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.holder.EViewGroupHolder;

import javax.lang.model.element.Element;

public class EViewGroupHandler extends org.androidannotations.internal.core.handler.EViewGroupHandler {

    public EViewGroupHandler(AndroidAnnotationsEnvironment environment) {
        super(environment);
    }

    @Override
    public void process(Element element, EViewGroupHolder holder) {
        super.process(element, holder);

        EventHolder eventHolder = holder.getPluginHolder(new EventHolder(holder));
        eventHolder.setEventRegisteringBlock(holder.getStartLifecycleAfterSuperBlock());
    }

}
