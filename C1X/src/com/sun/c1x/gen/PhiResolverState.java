package com.sun.c1x.gen;

import java.util.*;


public class PhiResolverState {
    private List<ResolveNode> virtualOperands;
    private List<ResolveNode> otherOperands;
    private List<ResolveNode> vregTable;


    public PhiResolverState() {
        virtualOperands = new ArrayList<ResolveNode>();
    }

}
