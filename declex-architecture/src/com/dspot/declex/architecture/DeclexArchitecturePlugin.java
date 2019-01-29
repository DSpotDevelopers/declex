package com.dspot.declex.architecture;

import com.dspot.declex.architecture.handler.*;
import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.handler.AnnotationHandler;
import org.androidannotations.plugin.AndroidAnnotationsPlugin;

import java.util.LinkedList;
import java.util.List;

public class DeclexArchitecturePlugin extends AndroidAnnotationsPlugin {

    @Override
    public String getName() {
        return "declex-architecture";
    }

    @Override
    public List<AnnotationHandler<?>> getHandlers(AndroidAnnotationsEnvironment androidAnnotationEnv) {

        List<AnnotationHandler<?>> handlers = new LinkedList<>();

        handlers.add(new EViewModelHandler(androidAnnotationEnv));

        handlers.add(new ObservableHandler(androidAnnotationEnv));
        handlers.add(new ObserverHandler(androidAnnotationEnv));

        handlers.add(new EViewPresenterHandler(androidAnnotationEnv));
        handlers.add(new PresenterMethodHandler(androidAnnotationEnv));

        handlers.add(new ArchInjectHandler(androidAnnotationEnv));
        handlers.add(new RootViewHandler(androidAnnotationEnv));

        return handlers;
    }

}
