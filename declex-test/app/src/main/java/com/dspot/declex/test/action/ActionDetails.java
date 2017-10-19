package com.dspot.declex.test.action;

import com.dspot.declex.test.util.Calc;

import org.androidannotations.annotations.EBean;

import static com.dspot.declex.Action.*;

@EBean
public class ActionDetails {
    public int result = 0;

    public void calcSumValues(int first, int second) {
        $CalcBasic(result).operation(Calc.SUM).numberFirst(first).numberSecond(second);
        if($CalcBasic.Done) {
            result = 9;
        }
    }

    public int getResult() {
        return result;
    }
}
