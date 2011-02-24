/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.c1x.ir;

import com.sun.c1x.debug.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * A branch on the outcome of an instanceof test.
 *
 * @author Ben L. Titzer
 */
public final class IfInstanceOf extends BlockEnd {

    final RiType targetClass;
    Value object;
    boolean testIsInstance;
    final int instanceofBCI;

    /**
     * Constructs a new {@link IfInstanceOf} instruction.
     * @param targetClass the class to check against
     * @param object the instruction which produces the object value
     * @param testIsInstance {@code true} if positive instanceof check implies going to true successor
     * @param instanceofBCI the bytecode index of the instanceof instruction
     * @param trueSucc the block representing the true successor
     * @param falseSucc the block representing the false successor
     */
    public IfInstanceOf(RiType targetClass, Value object, boolean testIsInstance, int instanceofBCI, BlockBegin trueSucc, BlockBegin falseSucc) {
        super(CiKind.Illegal, null, false); // XXX: why don't we need the state before??
        this.targetClass = targetClass;
        this.object = object;
        this.testIsInstance = testIsInstance;
        this.instanceofBCI = instanceofBCI;
        successors.add(trueSucc);
        successors.add(falseSucc);
    }

    /**
     * Gets the target class of the instanceof operation in this branch.
     * @return the target class
     */
    public RiType targetClass() {
        return targetClass;
    }

    /**
     * Gets the instruction that produces the object that is input to the instanceof.
     * @return the instruction producing the object
     */
    public Value object() {
        return object;
    }

    /**
     * Gets the bytecode index of the instanceof bytecode.
     * @return the bytecode index
     */
    public int instanceofBCI() {
        return instanceofBCI;
    }

    /**
     * Checks whether this instruction succeeds if the object is or is not an instance of the target class.
     * @return {@code true} if a successful instanceof implies going to the true successor
     */
    public boolean testIsInstance() {
        return testIsInstance;
    }

    /**
     * Gets the block corresponding to the true successor.
     * @return the true successor
     */
    public BlockBegin trueSuccessor() {
        return successors.get(0);
    }

    /**
     * Gets the block corresponding to the false successor.
     * @return the false successor
     */
    public BlockBegin falseSuccessor() {
        return successors.get(1);
    }

    /**
     * Gets the block corresponding to the specified outcome of the branch.
     * @param istrue {@code true} if the true successor is requested, {@code false} otherwise
     * @return the corresponding successor
     */
    public BlockBegin successor(boolean istrue) {
        return successors.get(istrue ? 0 : 1);
    }

    /**
     * Swaps the successor blocks to this if and negates the condition (e.g. == goes to !=)
     * @see Condition#negate()
     */
    public void swapSuccessors() {
        BlockBegin t = successors.get(0);
        BlockBegin f = successors.get(1);
        successors.set(0, f);
        successors.set(1, t);
        testIsInstance = !testIsInstance;
    }

    @Override
    public void inputValuesDo(ValueClosure closure) {
        object = closure.apply(object);
    }

    @Override
    public void accept(ValueVisitor v) {
        v.visitIfInstanceOf(this);
    }

    @Override
    public void print(LogStream out) {
        out.print("<IfInstanceOf>");
    }
}
