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
package com.dspot.declex.test.action;

import android.support.v4.app.Fragment;
import android.view.animation.Animation;
import android.widget.EditText;

import com.dspot.declex.test.R;

import static com.dspot.declex.actions.Action.*;

import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;


@EFragment(R.layout.fragment_main)
public class ActionAnimate extends Fragment {

    protected  int animRes = android.R.anim.fade_in;

    @ViewById(R.id.edit)
    EditText editText;

    public void animateControls() {
        Animation animation = $Animate(editText, animRes).animation();

        if($Animate.Ended) {
            animation.start();
            System.out.print("Is Ok Start");
        }

        if($Animate.Started) {
            animation.reset();
            System.out.print("Is Ok End");
        }
    }

    public void animateOtherProperties() {
        Animation animation = $Animate(editText, animRes).animation();

        if(animation.hasEnded()) {
            animation.setDuration(300);
            animation.setStartTime(23);
            animation.setRepeatCount(1);
            animation.setRepeatMode(Animation.RESTART);
            animation.start();
        }

        if($Animate.Started) {
            animation.cancel();
            System.out.print("Is Ok Start");
        }
    }
}
