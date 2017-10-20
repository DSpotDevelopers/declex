package com.dspot.declex.test.action;

import com.dspot.declex.annotation.Event;
import com.dspot.declex.test.util.Calc;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;

import static com.dspot.declex.Action.*;

@EBean
public class ActionDetails {
    public int result = 0;

    public int getResult() {
        return result;
    }

    public void calcSumValues(int first, int second) {
        $CalcBasic(result).operation(Calc.SUM).numberFirst(first).numberSecond(second);
        if($CalcBasic.Done) {
            result = 9;
        }
    }

    public void calcSubtValues(int first, int second) {
        $CalcBasic(result).operation(Calc.SUBT).numberFirst(first).numberSecond(second);
        if($CalcBasic.Done) {
            result = 1;
        }
    }

    public void callMainFragment() {
        $ActionMainFragment();
    }

    public void callEvent() {
        $GenerateResult();
    }

    // What happens if two actions are called in the same method
    public void callTwoActions(int first, int second) {
        $GenerateResult();

        {
            $CalcBasic(result).operation(Calc.SUBT).numberFirst(first).numberSecond(second);
            if($CalcBasic.Done) {
                result = result * 2;
            }
        }
    }

    @Background
    public void calcSumBackground(int first, int second) {
        $CalcBasic(result).operation(Calc.SUM).numberFirst(first).numberSecond(second);
        if($CalcBasic.Done) {
            result = 9;
        }
    }

    @Event
    void onGenerateResult() {
        result = 4 * 5;
    }
}
