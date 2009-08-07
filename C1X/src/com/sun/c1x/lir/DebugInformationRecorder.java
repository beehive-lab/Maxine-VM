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

import java.util.*;

import com.sun.c1x.ci.*;

/**
 * @author Thomas Wuerthinger
 *
 */
public class DebugInformationRecorder {

    public boolean recordingNonSafepoints() {
        // TODO Auto-generated method stub
        return false;
    }

    public int lastPcOffset() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void addNonSafepoint(int pcOffset) {
        // TODO Auto-generated method stub

    }

    public void describeScope(int pcOffset, CiMethod method, int[] sBci) {
        // TODO Auto-generated method stub

    }

    public void endNonSafepoint(int pcOffset) {
        // TODO Auto-generated method stub

    }

    /**
     * @param locals
     * @return
     */
    public long createScopeValues(List<ScopeValue> locals) {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * @param monitors
     * @return
     */
    public long createMonitorValues(List<MonitorValue> monitors) {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * @param pcOffset
     * @param method
     * @param bci
     * @param locvals
     * @param expvals
     * @param monvals
     */
    public void describeScope(int pcOffset, CiMethod method, int bci, long locvals, long expvals, long monvals) {
        // TODO Auto-generated method stub

    }

    /**
     * @param pcOffset
     * @param deepCopy
     */
    public void addSafepoint(int pcOffset, OopMap deepCopy) {
        // TODO Auto-generated method stub

    }

    /**
     * @param pcOffset
     */
    public void endSafepoint(int pcOffset) {
        // TODO Auto-generated method stub

    }

}
