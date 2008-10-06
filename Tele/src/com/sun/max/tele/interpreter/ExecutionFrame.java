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
/*VCSID=37476465-c911-403f-b7cc-ff17467d26ea*/
package com.sun.max.tele.interpreter;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.value.*;

/**
 * Instances of this class represent individual execution frame entries on a given ExecutionThread's execution stack.
 *
 * @author Athul Acharya
 */
class ExecutionFrame {

    private ClassMethodActor _method;
    private int _bcp;
    private Value[] _locals;
    private Stack<Value> _operands;
    private ExecutionFrame _previousFrame;

    public ExecutionFrame(ExecutionFrame prev, ClassMethodActor method) {
        _method = method;
        _bcp = -1;
        _locals = new Value[method.codeAttribute().maxLocals()];
        _operands = new Stack<Value>();
        _previousFrame = prev;
    }

    public ExecutionFrame previousFrame() {
        return _previousFrame;
    }

    public void setLocal(int index, Value value) {
        _locals[index] = value;
    }

    public void incrementBCP(int amount) {
        _bcp += amount;
    }

    public int bcp() {
        return _bcp;
    }

    public void setBCP(int bcp) {
        _bcp = bcp;
    }

    public byte[] code() {
        return _method.codeAttribute().code();
    }

    public Value getLocal(int index) {
        return _locals[index];
    }

    public Stack<Value> stack() {
        return _operands;
    }

    public ConstantPool constantPool() {
        return _method.codeAttribute().constantPool();
    }

    public ClassMethodActor method() {
        return _method;
    }

    public class ExceptionHandlerTable {
        Sequence<ExceptionHandlerEntry> _handlers;

        public ExceptionHandlerTable() {
            _handlers = method().codeAttribute().exceptionHandlerTable();
        }

        public ExceptionHandlerEntry findHandler(ClassActor exceptionClassActor, int codeOffset) {
            for (ExceptionHandlerEntry handler : _handlers) {
                if (codeOffset >= handler.startPosition() && codeOffset < handler.endPosition() &&
                        constantPool().classAt(handler.catchTypeIndex()).resolve(constantPool(), handler.catchTypeIndex()).isAssignableFrom(exceptionClassActor)) {
                    return handler;
                }
            }

            return null;
        }
    }
}
