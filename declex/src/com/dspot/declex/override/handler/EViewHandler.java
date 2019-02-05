package com.dspot.declex.override.handler;

import com.dspot.declex.holder.EventHolder;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.holder.EViewHolder;

import javax.lang.model.element.Element;

public class EViewHandler extends org.androidannotations.internal.core.handler.EViewHandler {

    public EViewHandler(AndroidAnnotationsEnvironment environment) {
        super(environment);
    }

    @Override
    public void process(Element element, EViewHolder holder) {
        super.process(element, holder);

        EventHolder eventHolder = holder.getPluginHolder(new EventHolder(holder));
        eventHolder.setEventRegisteringBlock(holder.getStartLifecycleAfterSuperBlock());
    }
}
