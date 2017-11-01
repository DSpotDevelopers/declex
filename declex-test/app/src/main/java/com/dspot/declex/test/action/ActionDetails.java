package com.dspot.declex.test.action;

import android.content.Context;

import com.dspot.declex.annotation.Event;
import com.dspot.declex.annotation.OnEvent;
import com.dspot.declex.event.CalcError;
import com.dspot.declex.event.CalcSum;
import com.dspot.declex.test.manager.CalcService_;
import com.dspot.declex.test.util.CalcHelper;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import static com.dspot.declex.Action.*;

@EBean
public class ActionDetails {
    @RootContext
    Context context;

    public int result = 0;

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public void calcSumValues(int first, int second) {
        $CalcBasic(result).operation(CalcHelper.SUM).numberFirst(first).numberSecond(second);
        if($CalcBasic.Done) {
            result = 9;
        }
    }

    public void calcSubtValues(int first, int second) {
        $CalcBasic(result).operation(CalcHelper.SUBT).numberFirst(first).numberSecond(second);
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
            $CalcBasic(result).operation(CalcHelper.SUBT).numberFirst(first).numberSecond(second);
            if($CalcBasic.Done) {
                result = result * 2;
            }
        }
    }

    // What happens if two actions are executed in parallel
    public void callActionsInParallel(int first, int second) {
        {
            $Background();
            $CalcBasic(result).operation(CalcHelper.SUM).numberFirst(first).numberSecond(second);
            if($CalcBasic.Done) {
                result = 9;
            }
        }

        {
            $Background();
            $CalcBasic(result).operation(CalcHelper.SUBT).numberFirst(first).numberSecond(second);
            if($CalcBasic.Done) {
                result = 1;
            }
        }
    }

    // What happens if two actions are executed in parallel with only background
    public void callActionsInParallelOnlyBackground(int first, int second) {
        $Background();
        {
            $CalcBasic(result).operation(CalcHelper.SUM).numberFirst(first).numberSecond(second);
            if($CalcBasic.Done) {
                result = 9;
            }

            $CalcBasic(result).operation(CalcHelper.SUBT).numberFirst(first).numberSecond(second);
            if($CalcBasic.Done) {
                result = 1;
            }
        }

        $UIThread();
        setResult(result);
    }

    // What happens with the services
    public void callServiceCalSum(int first, int second) {
        CalcService_.intent(context)
                .calculateSumFromService(first, second)
                .start();
    }

    public void callServiceCalSubt(int first, int second) {
        CalcService_.intent(context)
                .calculateSubtFromService(first, second)
                .start();
    }

    @OnEvent(CalcSum.class)
    public void serviceCalSumCorrect() {
        result = 9;
    }

    @OnEvent(CalcSum.class)
    public void serviceCalSubtCorrect() {
        result = 1;
    }

    @OnEvent(CalcError.class)
    public void serviceCalError() {
        result = -1;
    }

    @Background
    public void calcSumBackground(int first, int second) {
        $CalcBasic(result).operation(CalcHelper.SUM).numberFirst(first).numberSecond(second);
        if($CalcBasic.Done) {
            result = 9;
        }
    }

    @Event
    void onGenerateResult() {
        result = 4 * 5;
    }
}
