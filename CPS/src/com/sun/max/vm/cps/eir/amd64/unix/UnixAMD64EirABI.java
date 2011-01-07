/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.eir.amd64.unix;

import static com.sun.max.vm.cps.eir.amd64.AMD64EirRegister.General.*;
import static com.sun.max.vm.cps.eir.amd64.AMD64EirRegister.XMM.*;

import java.util.*;

import com.sun.max.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.EirStackSlot.Purpose;
import com.sun.max.vm.cps.eir.amd64.*;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * This ABI is close to the <a href="http://www.x86-64.org/documentation/abi-0.96.pdf">AMD64 ABI</a>
 * that is mostly adhered to by unix operating systems such as Solaris and Linux.
 *
 * We introduce the notion of a frame pointer to give us some flexibility wrt. to template-based code generation.
 * The use of the stack pointer for register spilling, local variable and parameter access conflict with the explicit
 * use of the stack pointer by the templates for managing an evaluation stack. To abstract away from these details,
 * we introduce a frame pointer, which the optimizing compiler uses as a base for register spilling,
 * local variable and parameters. The ABI will return the stack pointer as a frame pointer to the compiler, unless it
 * is customized for use by a template-based compiler, in which case it returns a distinct register.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Laurent Daynes
 */
public abstract class UnixAMD64EirABI extends AMD64EirABI {

    private static final List<AMD64EirRegister> generalParameterRegisters = Arrays.asList(new AMD64EirRegister[] {RDI, RSI, RDX, RCX, R8, R9});

    @Override
    public List<AMD64EirRegister> integerParameterRegisters() {
        return generalParameterRegisters;
    }

    private static final List<AMD64EirRegister> xmmParameterRegisters = Arrays.asList(new AMD64EirRegister[] {XMM0, XMM1, XMM2, XMM3, XMM4, XMM5, XMM6, XMM7});

    @Override
    public List<AMD64EirRegister> floatingPointParameterRegisters() {
        return xmmParameterRegisters;
    }

    @Override
    public EirLocation[] getParameterLocations(Purpose stackSlotPurpose, Kind... kinds) {
        final EirLocation[] result = new EirLocation[kinds.length];
        int iGeneral = 0;
        int iXMM = 0;
        for (int i = 0; i < kinds.length; i++) {
            switch (kinds[i].asEnum) {
                case BYTE:
                case BOOLEAN:
                case SHORT:
                case CHAR:
                case INT:
                case LONG:
                case WORD:
                case REFERENCE: {
                    if (iGeneral < generalParameterRegisters.size()) {
                        result[i] = generalParameterRegisters.get(iGeneral);
                        iGeneral++;
                    }
                    break;
                }
                case FLOAT:
                case DOUBLE: {
                    if (iXMM < xmmParameterRegisters.size()) {
                        result[i] = xmmParameterRegisters.get(iXMM);
                        iXMM++;
                    }
                    break;
                }
                default: {
                    ProgramError.unknownCase();
                    return null;
                }
            }
        }
        int stackOffset = 0;
        for (int i = 0; i < kinds.length; i++) {
            if (result[i] == null) {
                result[i] = new EirStackSlot(stackSlotPurpose, stackOffset);
                stackOffset += stackSlotSize();
            }
        }
        return result;
    }

    private PoolSet<AMD64EirRegister> createUnallocatableRegisterPoolSet() {
        final PoolSet<AMD64EirRegister> result = PoolSet.noneOf(AMD64EirRegister.pool());
        result.add(AMD64EirRegister.General.RSP);
        result.add(AMD64EirRegister.General.RBP);
        result.add(integerRegisterActingAs(VMRegister.Role.SAFEPOINT_LATCH));
        for (Kind kind : Kind.PRIMITIVE_VALUES) {
            result.add(getScratchRegister(kind));
        }
        return result;
    }

    private final PoolSet<AMD64EirRegister> unallocatableRegisters = createUnallocatableRegisterPoolSet();

    @Override
    public PoolSet<AMD64EirRegister> unallocatableRegisters() {
        return unallocatableRegisters;
    }

    private PoolSet<AMD64EirRegister> createAllocatableRegisterPoolSet() {
        final PoolSet<AMD64EirRegister> result = PoolSet.noneOf(AMD64EirRegister.pool());
        result.addAll();
        for (AMD64EirRegister register : unallocatableRegisters) {
            result.remove(register);
        }
        return result;
    }

    private final PoolSet<AMD64EirRegister> allocatableRegisters = createAllocatableRegisterPoolSet();

    @Override
    public PoolSet<AMD64EirRegister> allocatableRegisters() {
        return allocatableRegisters;
    }

    private final PoolSet<AMD64EirRegister> resultRegisters;

    @Override
    public PoolSet<AMD64EirRegister> resultRegisters() {
        return resultRegisters;
    }

    private static AMD64GeneralRegister64[] getTargetIntegerParameterRegisters() {
        final AMD64GeneralRegister64[] result = new AMD64GeneralRegister64[generalParameterRegisters.size()];
        for (int i = 0; i < generalParameterRegisters.size(); i++) {
            final AMD64EirRegister.General r = (AMD64EirRegister.General) generalParameterRegisters.get(i);
            result[i] = r.as64();
        }
        return result;
    }

    private static AMD64XMMRegister[] getTargetFloatingPointParameterRegisters() {
        final AMD64XMMRegister[] result = new AMD64XMMRegister[xmmParameterRegisters.size()];
        for (int i = 0; i < xmmParameterRegisters.size(); i++) {
            final AMD64EirRegister.XMM r = (AMD64EirRegister.XMM) xmmParameterRegisters.get(i);
            result[i] = r.as();
        }
        return result;
    }

    protected void makeUnallocatable(AMD64EirRegister register) {
        unallocatableRegisters.add(register);
        allocatableRegisters.remove(register);
    }

    private static TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> initTargetABI() {
        final Class<TargetABI<AMD64GeneralRegister64, AMD64XMMRegister>> type = null;
        return Utils.cast(type, TargetABIsScheme.INSTANCE.optimizedJavaABI);
    }

    protected UnixAMD64EirABI() {
        super(initTargetABI());
        resultRegisters = PoolSet.noneOf(AMD64EirRegister.pool());
        resultRegisters.add((AMD64EirRegister) getResultLocation(Kind.LONG));
        resultRegisters.add((AMD64EirRegister) getResultLocation(Kind.DOUBLE));
    }

}
