package com.sun.c1x.gen;

import com.sun.c1x.ir.*;


public class SwitchRange {

    private int lowKey;
    private int highKey;
    private BlockBegin sux;

    public SwitchRange(int startKey, BlockBegin sux) {
        lowKey = startKey;
        highKey = startKey;
        this.sux = sux;
    }

    public int highKey() {
        return highKey;
    }

    public int lowKey() {
        return lowKey;
    }

    public BlockBegin sux() {
        return sux;
    }

    public void setHighKey(int key) {
        highKey = key;
    }

}
