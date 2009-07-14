/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.c1x.lir;


/**
 * The <code>LIRVisitState</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class LIRVisitState {

    public enum OperandMode {
        InputMode(0),
        FirstMode(0),
        TempMode(1),
        OutputMode(2),
        NumModes(3),
        InvalidMode(-1);

        private final int value;

        OperandMode(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    public static final int MAXNUMBEROFOPERANDS = 14;
    public static final int MAXNUMBEROFINFOS = 4;

    private LIROperand operand;

    // optimization: the operands and infos are not stored in a variable-length
    //               list, but in a fixed-size array to save time of size checks and resizing
    private int [] oprsLen;
    LIROperand [][] oprsNew;
    CodeEmitInfo [] infoNew;
    int infoLen;
    boolean hasCall;
    private boolean hasSlowCase;

    public LIRVisitState() {
        oprsLen = new int[OperandMode.NumModes.value];
        oprsNew = new LIROperand[OperandMode.NumModes.value][MAXNUMBEROFOPERANDS];
        infoNew = new CodeEmitInfo[MAXNUMBEROFINFOS];
        reset();
    }

    /**
     * Reset the instance fields values to default values.
     *
     */
    public void reset() {
        operand = null;
        hasCall = false;
        hasSlowCase = false;
        oprsLen[OperandMode.InputMode.value] = 0;
        oprsLen[OperandMode.TempMode.value] = 0;
        oprsLen[OperandMode.OutputMode.value] = 0;
        infoLen = 0;
      }

    /**
     * Gets the operand of this visitor.
     *
     * @return the operand
     */
    public LIROperand operand() {
        return operand;
    }


    /**
     * Sets the operand instance variable.
     *
     * @param operand the operand to set
     */
    public void setOperand(LIROperand operand) {
        reset();
        this.operand = operand;
    }


    /**
     * Checks if this visitor has a slow case.
     *
     * @return true if the visitor has a slow case.
     */
    public boolean hasSlowCase() {
        return hasSlowCase;
    }


    /**
     * Sets the hasCall instance variable.
     *
     * @param hasCall the hasCall to set
     */
    public void setHasCall(boolean hasCall) {
        this.hasCall = hasCall;
    }

    /**
     *
     */
    public void doSlowCase() {
        // TODO to be completed later
    }

    public void doSlowCase(CodeEmitInfo info) {
        // TODO to be completed later
    }

    /**
     *
     */
    public void doInput(LIROperand input) {
        // TODO to be completed later
    }

   public void doOutput(LIROperand result) {
       // TODO to be completed later
   }

    /**
     * @param lockReg
     */
    public void doTemp(LIROperand lockReg) {
        // TODO Auto-generated method stub

    }

}
