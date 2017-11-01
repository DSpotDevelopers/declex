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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.dspot.declex.annotation.Model;
import com.dspot.declex.test.R;
import com.dspot.declex.test.model.servermodel.model.ServerModelEntity_;
import com.dspot.declex.test.util.CalcHelper;

import static com.dspot.declex.Action.*;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EFragment;

import java.util.List;

@EFragment(R.layout.fragment_main)
public class ActionMainFragment extends Fragment {

    protected int result = 0;

    @Model(async = true, orderBy = "list")
    List<ServerModelEntity_> listPosts;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void $onResume() {
        super.onResume();
        calcBasic();
    }

    public int first() {
        return 5;
    }

    public int second() {
        return 4;
    }

    public int getResult() { return result; }

    public void calcBasic() {
        $CalcBasic(result).operation(CalcHelper.SUM).numberFirst(first()).numberSecond(second());
        if($CalcBasic.Done) {
            result = 9;
        }
    }

    @AfterInject
    void downloadListPosts() {
        $LoadModel(listPosts);
    }
}
