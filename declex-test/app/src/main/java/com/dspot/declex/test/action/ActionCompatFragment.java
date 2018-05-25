/**
 * Copyright (C) 2016-2018 DSpot Sp. z o.o
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

import com.dspot.declex.test.R;

import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;

@EFragment(R.layout.fragment_main)
public class ActionCompatFragment extends Fragment {

    @FragmentArg
    String arg1;

    @FragmentArg
    void arg2(int arg2){}

    @FragmentArg
    void arg3(int arg3) {

    }

    void multiple(@FragmentArg int arg1, @FragmentArg String arg2) {

    }

}