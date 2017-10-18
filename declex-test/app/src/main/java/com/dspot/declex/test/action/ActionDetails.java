package com.dspot.declex.test.action;

import com.dspot.declex.test.util.Calc;

import org.androidannotations.annotations.EBean;

import static com.dspot.declex.Action.*;

@EBean
public class ActionDetails {
        public  void calcSumValues(int first, int second) {
            $CalcBasic(Calc.SUM, first, second);
        }
}
