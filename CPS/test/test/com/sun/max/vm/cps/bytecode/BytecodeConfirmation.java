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
package test.com.sun.max.vm.cps.bytecode;

import static com.sun.max.vm.bytecode.Bytecode.*;
import static com.sun.max.vm.bytecode.Bytecode.Flags.*;

import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;

/**
 * Confirmation that requested byte codes actually occur or do not occur in a method.
 * 
 * Use it to confirm that a CIR generator test case dedicated
 * to certain byte codes actually does or does not contain said byte codes.
 */
public class BytecodeConfirmation extends BytecodeAdapter {

    public static class BytecodeAbsent extends RuntimeException {
        public BytecodeAbsent() {
        }
        public BytecodeAbsent(Bytecode opcode) {
            super(opcode.toString());
        }
        public BytecodeAbsent(int opcode) {
            this(Bytecode.values()[opcode]);
        }
    }

    public static class BytecodePresent extends RuntimeException {
        public BytecodePresent() {
        }
        public BytecodePresent(Bytecode opcode) {
            super(opcode.toString());
        }
        public BytecodePresent(int opcode) {
            this(Bytecode.values()[opcode]);
        }
    }

    private enum Pass {REQUEST, CONFIRM}

    private Pass pass = Pass.REQUEST;

    public BytecodeConfirmation(ClassMethodActor classMethodActor) {
        try {
            final BytecodeScanner bytecodeScanner = new BytecodeScanner(this);
            final byte[] initBytes = new byte[256];
            for (Bytecode opcode : Bytecode.values()) {
                if (!(opcode.isLegalInClassfile() || opcode.is(EXTENSION))) {
                    if (opcode == WIDE) {
                        bytecodeScanner.scan(new BytecodeBlock(initBytes, 0, 0)); // scan nop byte code, then patch:
                        initBytes[0] = (byte) WIDE.ordinal();
                        wide();
                    } else {
                        initBytes[0] = (byte) opcode.ordinal();
                        bytecodeScanner.scan(new BytecodeBlock(initBytes, 0, 0));
                    }
                }
            }
            pass = Pass.CONFIRM;
            bytecodeScanner.scan(classMethodActor);
        } catch (Throwable throwable) {
            ProgramError.unexpected("error scanning byte codes in method: " + classMethodActor, throwable);
        }
        check();
    }

    private boolean[] isOpcodePresenceRequested = new boolean[256];
    private boolean[] isOpcodeAbsencePresent = new boolean[256];
    private boolean[] isOpcodePresent = new boolean[256];

    private void check() {
        int n = 0;
        for (int i = 0; i < 256; i++) {
            if (isOpcodePresenceRequested[i] != isOpcodePresent[i]) {
                throw new BytecodeAbsent(i);
            }
            n++;
        }
        if (n == 0) {
            throw new BytecodeAbsent();
        }
    }

    /**
     * Overload specific BytecodeAdapter methods and call this method in them.
     * Thus it will be checked if the corresponding byte codes occur as it should.
     */
    protected void confirmPresence() {
        int opcode = code()[currentOpcodePosition()] & 0xff;
        switch (pass) {
            case REQUEST:
                isOpcodePresenceRequested[opcode] = true;
                break;
            case CONFIRM:
                isOpcodePresent[opcode] = true;
                if (opcode == WIDE.ordinal()) {
                    opcode = code()[currentOpcodePosition() + 1] & 0xff;
                    isOpcodePresent[opcode] = true;
                }
                break;
        }
    }

    /**
     * Overload specific BytecodeAdapter methods and call this method in them.
     * Thus it will be checked whether the corresponding byte codes does not occur.
     */
    protected void confirmAbsense() {
        throw new BytecodePresent();
    }
}
