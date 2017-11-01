package com.dspot.declex.test.util;

import org.androidannotations.annotations.EBean;

import static com.dspot.declex.Action.*;

@EBean
public class CalcStandard extends CalcHelper {
    public CalcStandard() {
        super();
    }

    @Override
    public int Sum() {
        return getNumberFirst() * getNumberSecond() + 2;
    }

    @Override
    public int Subt() {
        return getNumberFirst() * getNumberSecond() - 2;
    }

    public void $createOperation() {
        $CalcBasic(0).operation(getOperation()).numberFirst(getNumberFirst()).numberSecond(getNumberSecond());
        if ($CalcBasic.Done) {
            setResultOperation((isSum()) ? 9 : 1);
        }
    }

//    @Override
//    public void createOperation() {
//        super.createOperation();
//
//        {
//            $CalcBasic(0).operation(CalcHelper.SUM).numberFirst(getNumberFirst()).numberSecond(getNumberSecond());
//            if($CalcBasic.Done) {
//                setResultOperation(9);
//            }
//        }
//    }
}