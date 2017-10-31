package com.dspot.declex.test.manager;

import android.content.Context;

import com.dspot.declex.annotation.Event;
import com.dspot.declex.annotation.Populate;
import com.dspot.declex.test.util.CalcHelper;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import static com.dspot.declex.Action.*;

@EBean(scope = EBean.Scope.Singleton)
public class CalcManager
{
    @Event static class CalcError{String errorMessage;}

    @Event static class CalcSum{String errorMessage;}

    @Event static class CalcSubt{String errorMessage;}

    @RootContext
    Context context;

    /**
     *  Test populate variable secondary
     * **/
    @Populate
    int result = 0;

    public  void calculateSumVirtual(int first, int second) {
        $CalcBasic(result).operation(CalcHelper.SUM).numberFirst(first).numberSecond(second);
        if($CalcBasic.Done) {
            {$CalcSum("Calc operation correct");}
        }

        $CalcError("An error occurred in sum operation");
    }

    public  void calculateSubtVirtual(int first, int second) {
        $CalcBasic(result).operation(CalcHelper.SUBT).numberFirst(first).numberSecond(second);
        if($CalcBasic.Done) {
            {$CalcSubt("Calc operation correct");}
        }

        $CalcError("An error occurred in subt operation");
    }
}
