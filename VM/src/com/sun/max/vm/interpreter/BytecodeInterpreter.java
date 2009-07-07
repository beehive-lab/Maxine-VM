/*
 * Copyright (c) 2008 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.interpreter;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;

/**
 * This class is simply a sketch of what a Java bytecode interpreter would look like.
 *
 * Questions:
 *
 * - How to model the stack frame and operations on the stack frame? Allocating actual
 * frame objects and manipulating their fields will likely be too slow for a real
 * interpreter implementation, although it is fine for a hosted implementation.
 * Eventually, these operations should boil down to pointer accesses into a stack frame
 * for the interpreted method's local variables and stack slots (in the same format as
 * the JVM language spec). This frame can live in a special segment reserved for interpreter
 * frames within the thread's overall stack frame.
 *
 * - How to efficiently transfer control between compiled code and interpreter code?
 * - Do compiled methods still need adapter code in order to be entered from the interpreter?
 *
 * @author Ben L. Titzer
 */
@Hypothetical
public class BytecodeInterpreter {

    abstract class Frame {
        abstract boolean popBoolean();
        abstract byte popByte();
        abstract short popShort();
        abstract char popChar();
        abstract int popInt();
        abstract long popLong();
        abstract float popFloat();
        abstract double popDouble();
        abstract Object popObject();

        abstract void pushBoolean(boolean val);
        abstract void pushInt(int val);
        abstract void pushLong(long val);
        abstract void pushFloat(float val);
        abstract void pushDouble(double val);
        abstract void pushObject(Object val);

        abstract boolean getBoolean(int index);
        abstract byte getByte(int index);
        abstract short getShort(int index);
        abstract char getChar(int index);
        abstract int getInt(int index);
        abstract long getLong(int index);
        abstract float getFloat(int index);
        abstract double getDouble(int index);
        abstract Object getObject(int index);

        abstract void setBoolean(int index, boolean val);
        abstract void setInt(int index, int val);
        abstract void setLong(int index, long val);
        abstract void setFloat(int index, float val);
        abstract void setDouble(int index, double val);
        abstract void setObject(int index, Object val);
    }

    public void run(Frame frame, int position, byte[] bytecodeArray) throws Throwable {
        while (true) {
            final byte bytecode = bytecodeArray[position];
            switch(bytecode) {
                // semantics of each bytecode here
                case 0: { // e.g. invokestatic
                    final StaticMethodActor methodActor = null;
                    call(frame, methodActor);
                }
            }
        }
    }

    void execute(Frame previous) throws Throwable {
        final Frame frame = acquireFrame(previous, 0, 0, 0);
        int position = 0;
        while (true) {
            try {
                run(frame, position, null);
                releaseFrame(frame);

            } catch (Throwable t) {
                // determine whether there is an exception handler in this method
                if (false) {
                    final int stackBottom = 0;
                    frame.setObject(stackBottom, t);
                    final int handler = 0;
                    position = handler;
                } else {
                    releaseFrame(frame);
                    throw t;
                }
            }
        }
    }

    void enter(Address caller, Address parameterState, ClassMethodActor calleeMethod) {
        // lookup the caller's callsite
    }

    void call(Frame frame, ClassMethodActor actor) throws Throwable {
        if (MaxineVM.isPrototyping()) {
            // just interpret the method
            call(frame, null);
        } else {
            final TargetMethod compiledVersion = CompilationScheme.Static.getCurrentTargetMethod(actor);
            if (compiledVersion != null) {
                // invoke the compiled version
            } else {
                call(frame, null);
            }
        }
    }

    Frame acquireFrame(Frame previous, int numParams, int numLocals, int numSlots) {
        return null;
    }
    void releaseFrame(Frame frame) {

    }
}
