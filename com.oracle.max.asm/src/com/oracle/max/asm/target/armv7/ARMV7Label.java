package com.oracle.max.asm.target.armv7;

import java.util.*;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.armv7.ARMV7Assembler.*;

public class ARMV7Label extends Label {

    public static class BranchInfo {


        public  enum BranchType { JCC, JMP, TABLESWITCH, BRANCH, UNKNOWN;}

        private BranchType type;
        private ConditionFlag flag;
        private boolean instrumented;

        public BranchInfo(BranchType type, ConditionFlag flag, boolean instrumented) {
            this.type = type;
            this.flag = flag;
            this.instrumented = instrumented;
        }

        public BranchType getBranchType() {
            return type;
        }

        public ConditionFlag getConditionFlag() {
            return flag;
        }

        public static BranchType fromValue(int value) {
            switch (value) {
                case 0xbeef:
                    return BranchType.JCC;
                case 0xdead:
                    return BranchType.JMP;
                case 0xd0d0:
                    return BranchType.BRANCH;
                default:
                    return BranchType.TABLESWITCH;
            }
        }

        public boolean isInstrumented() {
            return instrumented;
        }

    }

    public ARMV7Label() {

    }

    public ARMV7Label(Label label) {
        super(label.getPatchPositions(), label.positionCopy());
    }

    private ArrayList<BranchInfo> branchInfo = new ArrayList<BranchInfo>(4);

    public void addPatchAt(int branchLocation, BranchInfo type) {
        assert !isBound() : "Label is already bound";
        addPatchAt(branchLocation);
        branchInfo.add(type);
    }

    protected void patchInstructions(ARMV7Assembler masm) {
        assert isBound() : "Label should be bound";
        int target = position;
        for (int i = 0; i < patchPositions.size(); ++i) {
            int pos = patchPositions.get(i);
            masm.patchJumpTarget(pos, target, branchInfo.get(i));
        }
    }
}
