/**
 * Copyright (C) 2016-2017 DSpot Sp. z o.o
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dspot.declex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.androidannotations.AndroidAnnotationsEnvironment;
import org.androidannotations.Option;
import org.androidannotations.handler.AnnotationHandler;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.internal.core.handler.AfterExtrasHandler;
import org.androidannotations.internal.core.handler.AfterPreferencesHandler;
import org.androidannotations.internal.core.handler.AfterTextChangeHandler;
import org.androidannotations.internal.core.handler.AnimationResHandler;
import org.androidannotations.internal.core.handler.AppHandler;
import org.androidannotations.internal.core.handler.BackgroundHandler;
import org.androidannotations.internal.core.handler.BeanHandler;
import org.androidannotations.internal.core.handler.BeforeTextChangeHandler;
import org.androidannotations.internal.core.handler.ColorResHandler;
import org.androidannotations.internal.core.handler.ColorStateListResHandler;
import org.androidannotations.internal.core.handler.CustomTitleHandler;
import org.androidannotations.internal.core.handler.DefaultResHandler;
import org.androidannotations.internal.core.handler.DrawableResHandler;
import org.androidannotations.internal.core.handler.EApplicationHandler;
import org.androidannotations.internal.core.handler.EBeanHandler;
import org.androidannotations.internal.core.handler.EIntentServiceHandler;
import org.androidannotations.internal.core.handler.EProviderHandler;
import org.androidannotations.internal.core.handler.EReceiverHandler;
import org.androidannotations.internal.core.handler.EServiceHandler;
import org.androidannotations.internal.core.handler.EViewGroupHandler;
import org.androidannotations.internal.core.handler.EViewHandler;
import org.androidannotations.internal.core.handler.FragmentByIdHandler;
import org.androidannotations.internal.core.handler.FragmentByTagHandler;
import org.androidannotations.internal.core.handler.FromHtmlHandler;
import org.androidannotations.internal.core.handler.FullscreenHandler;
import org.androidannotations.internal.core.handler.HierarchyViewerSupportHandler;
import org.androidannotations.internal.core.handler.HtmlResHandler;
import org.androidannotations.internal.core.handler.HttpsClientHandler;
import org.androidannotations.internal.core.handler.IgnoreWhenHandler;
import org.androidannotations.internal.core.handler.InjectMenuHandler;
import org.androidannotations.internal.core.handler.InstanceStateHandler;
import org.androidannotations.internal.core.handler.ItemSelectHandler;
import org.androidannotations.internal.core.handler.KeyDownHandler;
import org.androidannotations.internal.core.handler.KeyLongPressHandler;
import org.androidannotations.internal.core.handler.KeyMultipleHandler;
import org.androidannotations.internal.core.handler.KeyUpHandler;
import org.androidannotations.internal.core.handler.NonConfigurationInstanceHandler;
import org.androidannotations.internal.core.handler.OnActivityResultHandler;
import org.androidannotations.internal.core.handler.OptionsItemHandler;
import org.androidannotations.internal.core.handler.OptionsMenuHandler;
import org.androidannotations.internal.core.handler.OptionsMenuItemHandler;
import org.androidannotations.internal.core.handler.PageScrollStateChangedHandler;
import org.androidannotations.internal.core.handler.PageScrolledHandler;
import org.androidannotations.internal.core.handler.PageSelectedHandler;
import org.androidannotations.internal.core.handler.PrefHandler;
import org.androidannotations.internal.core.handler.PreferenceByKeyHandler;
import org.androidannotations.internal.core.handler.PreferenceChangeHandler;
import org.androidannotations.internal.core.handler.PreferenceClickHandler;
import org.androidannotations.internal.core.handler.PreferenceHeadersHandler;
import org.androidannotations.internal.core.handler.PreferenceScreenHandler;
import org.androidannotations.internal.core.handler.ReceiverActionHandler;
import org.androidannotations.internal.core.handler.ReceiverHandler;
import org.androidannotations.internal.core.handler.RootContextHandler;
import org.androidannotations.internal.core.handler.SeekBarProgressChangeHandler;
import org.androidannotations.internal.core.handler.SeekBarTouchStartHandler;
import org.androidannotations.internal.core.handler.SeekBarTouchStopHandler;
import org.androidannotations.internal.core.handler.ServiceActionHandler;
import org.androidannotations.internal.core.handler.SharedPrefHandler;
import org.androidannotations.internal.core.handler.SupposeBackgroundHandler;
import org.androidannotations.internal.core.handler.SupposeThreadHandler;
import org.androidannotations.internal.core.handler.SupposeUiThreadHandler;
import org.androidannotations.internal.core.handler.SystemServiceHandler;
import org.androidannotations.internal.core.handler.TextChangeHandler;
import org.androidannotations.internal.core.handler.TouchHandler;
import org.androidannotations.internal.core.handler.TraceHandler;
import org.androidannotations.internal.core.handler.TransactionalHandler;
import org.androidannotations.internal.core.handler.UiThreadHandler;
import org.androidannotations.internal.core.handler.ViewByIdHandler;
import org.androidannotations.internal.core.handler.ViewsByIdHandler;
import org.androidannotations.internal.core.handler.WakeLockHandler;
import org.androidannotations.internal.core.handler.WindowFeatureHandler;
import org.androidannotations.internal.core.model.AndroidRes;
import org.androidannotations.plugin.AndroidAnnotationsPlugin;

import com.dspot.declex.action.ActionForHandler;
import com.dspot.declex.eventbus.EventHandler;
import com.dspot.declex.eventbus.UseEventBusHandler;
import com.dspot.declex.eventbus.UseEventsHandler;
import com.dspot.declex.eventbus.oneventhandler.LoadOnEventHandler;
import com.dspot.declex.eventbus.oneventhandler.PutOnActionHandler;
import com.dspot.declex.eventbus.oneventhandler.PutOnEventHandler;
import com.dspot.declex.eventbus.oneventhandler.UpdateOnEventHandler;
import com.dspot.declex.json.JsonModelHandler;
import com.dspot.declex.localdb.LocalDBModelHandler;
import com.dspot.declex.localdb.LocalDBTransactionHandler;
import com.dspot.declex.localdb.UseLocalDBHandler;
import com.dspot.declex.model.AfterLoadHandler;
import com.dspot.declex.model.AfterPutHandler;
import com.dspot.declex.model.ModelHandler;
import com.dspot.declex.model.UseModelHandler;
import com.dspot.declex.override.handler.AfterInjectHandler;
import com.dspot.declex.override.handler.AfterViewsHandler;
import com.dspot.declex.override.handler.CheckedChangeHandler;
import com.dspot.declex.override.handler.ClickHandler;
import com.dspot.declex.override.handler.EActivityHandler;
import com.dspot.declex.override.handler.EFragmentHandler;
import com.dspot.declex.override.handler.EditorActionHandler;
import com.dspot.declex.override.handler.ExtraHandler;
import com.dspot.declex.override.handler.FocusChangeHandler;
import com.dspot.declex.override.handler.FragmentArgHandler;
import com.dspot.declex.override.handler.ItemClickHandler;
import com.dspot.declex.override.handler.ItemLongClickHandler;
import com.dspot.declex.override.handler.LongClickHandler;
import com.dspot.declex.plugin.JClassPlugin;
import com.dspot.declex.runwith.RunWithHandler;
import com.dspot.declex.server.ServerModelHandler;
import com.dspot.declex.util.SharedRecords;
import com.dspot.declex.viewsinjection.AdapterClassHandler;
import com.dspot.declex.viewsinjection.PopulateHandler;
import com.dspot.declex.viewsinjection.RecollectHandler;

public class DeclexCorePlugin extends AndroidAnnotationsPlugin {

	private final String DECLEX_ISSUES_URL = "https://github.com/smaugho/declex/issues";
	
	private static final String NAME = "DecleX";

	public DeclexCorePlugin() {
		SharedRecords.reset();
	}
	
	@Override
	public String getIssuesUrl() {
		return DECLEX_ISSUES_URL;
	}
	
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public List<Option> getSupportedOptions() {
		return Arrays.asList(TraceHandler.OPTION_TRACE, SupposeThreadHandler.OPTION_THREAD_CONTROL);
	}
	
	@Override
	public List<AnnotationHandler<?>> getHandlers(AndroidAnnotationsEnvironment androidAnnotationEnv) {
		
		List<AnnotationHandler<?>> annotationHandlers = new ArrayList<>();
		
		//Generating Annotations Handlers
		annotationHandlers.add(new EApplicationHandler(androidAnnotationEnv));
		annotationHandlers.add(new EActivityHandler(androidAnnotationEnv));
		annotationHandlers.add(new EProviderHandler(androidAnnotationEnv));
		annotationHandlers.add(new EReceiverHandler(androidAnnotationEnv));
		annotationHandlers.add(new EServiceHandler(androidAnnotationEnv));
		annotationHandlers.add(new EIntentServiceHandler(androidAnnotationEnv));
		annotationHandlers.add(new EFragmentHandler(androidAnnotationEnv));
		annotationHandlers.add(new EBeanHandler(androidAnnotationEnv));
		annotationHandlers.add(new EViewGroupHandler(androidAnnotationEnv));
		annotationHandlers.add(new EViewHandler(androidAnnotationEnv));
		annotationHandlers.add(new SharedPrefHandler(androidAnnotationEnv));

		annotationHandlers.add(new ActionForHandler(androidAnnotationEnv));

		//Events Handlers
		annotationHandlers.add(new EventHandler(androidAnnotationEnv));
		annotationHandlers.add(new UpdateOnEventHandler(androidAnnotationEnv));
		annotationHandlers.add(new LoadOnEventHandler(androidAnnotationEnv));
		annotationHandlers.add(new PutOnEventHandler(androidAnnotationEnv));
		annotationHandlers.add(new PutOnActionHandler(androidAnnotationEnv));
		annotationHandlers.add(new UseEventsHandler(androidAnnotationEnv));

		//Model Handlers
		annotationHandlers.add(new UseModelHandler(androidAnnotationEnv));
		annotationHandlers.add(new AfterLoadHandler(androidAnnotationEnv));
		annotationHandlers.add(new AfterPutHandler(androidAnnotationEnv));
		annotationHandlers.add(new JsonModelHandler(androidAnnotationEnv));
		annotationHandlers.add(new LocalDBModelHandler(androidAnnotationEnv));
		annotationHandlers.add(new ServerModelHandler(androidAnnotationEnv));
		annotationHandlers.add(new UseLocalDBHandler(androidAnnotationEnv));
		
		annotationHandlers.add(new LocalDBTransactionHandler(androidAnnotationEnv));
		annotationHandlers.add(new UseEventBusHandler(androidAnnotationEnv));

		//Main Injection Handlers
		annotationHandlers.add(new PrefHandler(androidAnnotationEnv));
		annotationHandlers.add(new ViewByIdHandler(androidAnnotationEnv));
		annotationHandlers.add(new ViewsByIdHandler(androidAnnotationEnv));
		annotationHandlers.add(new FragmentByIdHandler(androidAnnotationEnv));
		annotationHandlers.add(new FragmentByTagHandler(androidAnnotationEnv));
		annotationHandlers.add(new FromHtmlHandler(androidAnnotationEnv));
		
		//Parameters Injection Handler
		annotationHandlers.add(new FragmentArgHandler(androidAnnotationEnv));
		annotationHandlers.add(new ExtraHandler(androidAnnotationEnv));
		
		//Actions and its plugins
		annotationHandlers.add(new RunWithHandler<EComponentHolder>(androidAnnotationEnv));

		//Listeners Handlers
		annotationHandlers.add(new ClickHandler(androidAnnotationEnv));
		annotationHandlers.add(new LongClickHandler(androidAnnotationEnv));
		annotationHandlers.add(new TouchHandler(androidAnnotationEnv));
		annotationHandlers.add(new FocusChangeHandler(androidAnnotationEnv));
		annotationHandlers.add(new CheckedChangeHandler(androidAnnotationEnv));
		annotationHandlers.add(new ItemClickHandler(androidAnnotationEnv));
		annotationHandlers.add(new ItemSelectHandler(androidAnnotationEnv));
		annotationHandlers.add(new ItemLongClickHandler(androidAnnotationEnv));
		annotationHandlers.add(new EditorActionHandler(androidAnnotationEnv));
				
		for (AndroidRes androidRes : AndroidRes.values()) {
			if (androidRes == AndroidRes.ANIMATION) {
				annotationHandlers.add(new AnimationResHandler(androidAnnotationEnv));
			} else if (androidRes == AndroidRes.COLOR) {
				annotationHandlers.add(new ColorResHandler(androidAnnotationEnv));
			} else if (androidRes == AndroidRes.COLOR_STATE_LIST) {
				annotationHandlers.add(new ColorStateListResHandler(androidAnnotationEnv));
			} else if (androidRes == AndroidRes.DRAWABLE) {
				annotationHandlers.add(new DrawableResHandler(androidAnnotationEnv));
			} else if (androidRes == AndroidRes.HTML) {
				annotationHandlers.add(new HtmlResHandler(androidAnnotationEnv));
			} else {
				annotationHandlers.add(new DefaultResHandler(androidRes, androidAnnotationEnv));
			}
		}
		
		annotationHandlers.add(new TransactionalHandler(androidAnnotationEnv));		
		annotationHandlers.add(new SystemServiceHandler(androidAnnotationEnv));

		annotationHandlers.add(new NonConfigurationInstanceHandler(androidAnnotationEnv));
		annotationHandlers.add(new AppHandler(androidAnnotationEnv));
		annotationHandlers.add(new BeanHandler(androidAnnotationEnv));
		annotationHandlers.add(new InjectMenuHandler(androidAnnotationEnv));
		annotationHandlers.add(new OptionsMenuHandler(androidAnnotationEnv));
		annotationHandlers.add(new OptionsMenuItemHandler(androidAnnotationEnv));
		annotationHandlers.add(new OptionsItemHandler(androidAnnotationEnv));
		
		annotationHandlers.add(new CustomTitleHandler(androidAnnotationEnv));
		annotationHandlers.add(new FullscreenHandler(androidAnnotationEnv));
		annotationHandlers.add(new RootContextHandler(androidAnnotationEnv));
		
		annotationHandlers.add(new BeforeTextChangeHandler(androidAnnotationEnv));
		annotationHandlers.add(new TextChangeHandler(androidAnnotationEnv));
		annotationHandlers.add(new AfterTextChangeHandler(androidAnnotationEnv));
		annotationHandlers.add(new SeekBarProgressChangeHandler(androidAnnotationEnv));
		annotationHandlers.add(new SeekBarTouchStartHandler(androidAnnotationEnv));
		annotationHandlers.add(new SeekBarTouchStopHandler(androidAnnotationEnv));
		annotationHandlers.add(new KeyDownHandler(androidAnnotationEnv));
		annotationHandlers.add(new KeyLongPressHandler(androidAnnotationEnv));
		annotationHandlers.add(new KeyMultipleHandler(androidAnnotationEnv));
		annotationHandlers.add(new KeyUpHandler(androidAnnotationEnv));
		
		annotationHandlers.add(new ServiceActionHandler(androidAnnotationEnv));
		annotationHandlers.add(new InstanceStateHandler(androidAnnotationEnv));
		annotationHandlers.add(new HttpsClientHandler(androidAnnotationEnv));
		annotationHandlers.add(new HierarchyViewerSupportHandler(androidAnnotationEnv));
		annotationHandlers.add(new WindowFeatureHandler(androidAnnotationEnv));
		
		annotationHandlers.add(new ReceiverHandler(androidAnnotationEnv));
		annotationHandlers.add(new ReceiverActionHandler(androidAnnotationEnv));
		annotationHandlers.add(new OnActivityResultHandler(androidAnnotationEnv));
		
		annotationHandlers.add(new PageScrolledHandler(androidAnnotationEnv));
		annotationHandlers.add(new PageScrollStateChangedHandler(androidAnnotationEnv));
		annotationHandlers.add(new PageSelectedHandler(androidAnnotationEnv));
				
		annotationHandlers.add(new IgnoreWhenHandler(androidAnnotationEnv));

		//Populators, Recollectors and Injected Models
		annotationHandlers.add(new RecollectHandler(androidAnnotationEnv));
		annotationHandlers.add(new ModelHandler(androidAnnotationEnv));		
		
		List<JClassPlugin> adapterPlugins = new LinkedList<JClassPlugin>();
		
		//View Features Handler
		AdapterClassHandler adapterClassHandler = new AdapterClassHandler(androidAnnotationEnv);
		annotationHandlers.add(adapterClassHandler);
		adapterPlugins.add(adapterClassHandler);
		
		annotationHandlers.add(new PopulateHandler(androidAnnotationEnv, adapterPlugins));

		/* After injection methods must be after injections */
		annotationHandlers.add(new AfterInjectHandler(androidAnnotationEnv));
		annotationHandlers.add(new AfterExtrasHandler(androidAnnotationEnv));
		annotationHandlers.add(new AfterViewsHandler(androidAnnotationEnv));

		annotationHandlers.add(new PreferenceScreenHandler(androidAnnotationEnv));
		annotationHandlers.add(new PreferenceHeadersHandler(androidAnnotationEnv));
		annotationHandlers.add(new PreferenceByKeyHandler(androidAnnotationEnv));
		annotationHandlers.add(new PreferenceChangeHandler(androidAnnotationEnv));
		annotationHandlers.add(new PreferenceClickHandler(androidAnnotationEnv));
		annotationHandlers.add(new AfterPreferencesHandler(androidAnnotationEnv));

		annotationHandlers.add(new TraceHandler(androidAnnotationEnv));
		
		/*
		 * WakeLockHandler must be after TraceHandler but before UiThreadHandler
		 * and BackgroundHandler
		 */
		annotationHandlers.add(new WakeLockHandler(androidAnnotationEnv));

		/*
		 * UIThreadHandler and BackgroundHandler must be after TraceHandler and
		 * IgnoredWhenDetached
		 */
		annotationHandlers.add(new UiThreadHandler(androidAnnotationEnv));
		annotationHandlers.add(new BackgroundHandler(androidAnnotationEnv));

		/*
		 * SupposeUiThreadHandler and SupposeBackgroundHandler must be after all
		 * handlers that modifies generated method body
		 */
		annotationHandlers.add(new SupposeUiThreadHandler(androidAnnotationEnv));
		annotationHandlers.add(new SupposeBackgroundHandler(androidAnnotationEnv));

		return annotationHandlers;		
	}

}
