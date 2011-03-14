/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.cps.bytecode;

import static com.sun.cri.bytecode.Bytecodes.*;

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
        public BytecodeAbsent(int opcode) {
            super(nameOf(opcode));
        }
    }

    public static class BytecodePresent extends RuntimeException {
        public BytecodePresent() {
        }
        public BytecodePresent(int opcode) {
            super(nameOf(opcode));
        }
    }

    private enum Pass {REQUEST, CONFIRM}

    private Pass pass = Pass.REQUEST;

    public BytecodeConfirmation(ClassMethodActor classMethodActor) {
        try {
            final BytecodeScanner bytecodeScanner = new BytecodeScanner(this);
            final byte[] initBytes = new byte[256];
            for (int opcode = 0; opcode <= LAST_JVM_OPCODE; ++opcode) {
                if (opcode != XXXUNUSEDXXX) {
                    if (opcode == WIDE) {
                        bytecodeScanner.scan(new BytecodeBlock(initBytes, 0, 0)); // scan nop byte code, then patch:
                        initBytes[0] = (byte) WIDE;
                        wide();
                    } else {
                        initBytes[0] = (byte) opcode;
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
        int opcode = code()[currentOpcodeBCI()] & 0xff;
        switch (pass) {
            case REQUEST:
                isOpcodePresenceRequested[opcode] = true;
                break;
            case CONFIRM:
                isOpcodePresent[opcode] = true;
                if (opcode == WIDE) {
                    opcode = code()[currentOpcodeBCI() + 1] & 0xff;
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
