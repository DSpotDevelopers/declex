package com.dspot.declex.test.action;

import android.support.v7.app.AppCompatActivity;

import com.dspot.declex.test.R;

import org.androidannotations.annotations.EActivity;

import static com.dspot.declex.Action.$ActionMainFragment;

@EActivity(R.layout.activity_main)
public class ActionMainActivity extends AppCompatActivity {

    public void callMainFragment() {
        $ActionMainFragment();
    }

}
