/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
/*VCSID=8be0a814-c2ca-4a28-bed7-9a0ef486c5a0*/
package com.sun.max.tele.interpreter;

import com.sun.max.vm.actor.member.*;

/**
 * Instances of this class contain the execution state of a single thread in the system.
 * NOTE: currently there _is_ only one thread. Please ignore the man behind the curtain.
 *
 * @author Athul Acharya
 */
class ExecutionThread {

    private ExecutionFrame _frame;
    //private int _prio;
    //private ThreadType _threadType;

    public ExecutionThread(int prio, ThreadType threadType) {
        //_prio = prio;
        //_threadType = threadType;
        _frame = null;
    }

    public ExecutionFrame pushFrame(ClassMethodActor method) {
        _frame = new ExecutionFrame(_frame, method);
        return _frame;
    }

    public ExecutionFrame popFrame() {
        _frame = _frame.previousFrame();
        return _frame;
    }

    public ExecutionFrame frame() {
        return _frame;
    }

    public static enum ThreadType {
        NORMAL_THREAD,
        VM_THREAD,
    }

}
