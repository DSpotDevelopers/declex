package com.dspot.declex.test.action;

import org.androidannotations.annotations.EBean;

import static com.dspot.declex.Action.$ActionMainActivity;
import static com.dspot.declex.Action.$ActionMainFragment;

@EBean
public class ActionBean {

    public void callMainFragment() {
        $ActionMainFragment();
    }

//    public void callMainActivity() { $ActionMainActivity(); }

}
