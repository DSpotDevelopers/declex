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
package com.dspot.declex.api.action.builtin;

import org.androidannotations.annotations.RootContext;

import android.content.Context;
import android.support.annotation.AnimRes;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.dspot.declex.api.action.annotation.ActionFor;


/**
 * Animation Action that can be applied to Views, Surfaces, or
 * other objects. See the {@link android.view.animation animation package
 * description file}.
 */
@ActionFor("Animate")
public class AnimateActionHolder {

    @RootContext
    Context context;

    private Animation animation;
    private View view;

    /**
     * @param view The View to which the animation will be applied
     *
     * @param anim The resource id of the animation to load
     */
    void init(View view, @AnimRes int anim) {
        this.view = view;
        
        if (context != null) {
        	this.animation = AnimationUtils.loadAnimation(context, anim);
        }
    }

    /**
     * @param Ended <i><b>(default)</b></i> Notifies the end of the animation. This callback is not invoked
     * for animations with repeat count set to INFINITE.
     * 
     * @param Started Notifies the start of the animation.
     *
     * @param Repeated Notifies the repetition of the animation.
     */
    void build(
        final Runnable Ended,
        final Runnable Started,
        final Runnable Repeated
    ) {
    	
    	if (animation == null) return;

        this.animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (Started != null) Started.run();
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (Ended != null) Ended.run();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                if (Repeated != null) Repeated.run();
            }
        });
    }

    void execute() {
    	if (animation != null) {
    		view.startAnimation(animation);
    	}        
    }
    
    /**
     * @return Animation object that was loaded by the Action
     */
    public Animation animation() {
    	return this.animation;
    }
}
