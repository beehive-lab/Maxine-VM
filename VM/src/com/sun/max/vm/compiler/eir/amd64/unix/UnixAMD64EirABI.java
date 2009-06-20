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
package com.sun.max.vm.compiler.eir.amd64.unix;

import static com.sun.max.vm.compiler.eir.amd64.AMD64EirRegister.General.*;
import static com.sun.max.vm.compiler.eir.amd64.AMD64EirRegister.XMM.*;

import com.sun.max.asm.amd64.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.amd64.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.debug.*;
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

    private static final IndexedSequence<AMD64EirRegister> _generalParameterRegisters = new ArraySequence<AMD64EirRegister>(RDI, RSI, RDX, RCX, R8, R9);

    @Override
    public Sequence<AMD64EirRegister> integerParameterRegisters() {
        return _generalParameterRegisters;
    }

    private static final IndexedSequence<AMD64EirRegister> _xmmParameterRegisters = new ArraySequence<AMD64EirRegister>(XMM0, XMM1, XMM2, XMM3, XMM4, XMM5, XMM6, XMM7);

    @Override
    public Sequence<AMD64EirRegister> floatingPointParameterRegisters() {
        return _xmmParameterRegisters;
    }

    @Override
    public EirLocation[] getParameterLocations(EirStackSlot.Purpose stackSlotPurpose, Kind... kinds) {
        final EirLocation[] result = new EirLocation[kinds.length];
        int iGeneral = 0;
        int iXMM = 0;
        for (int i = 0; i < kinds.length; i++) {
            switch (kinds[i].asEnum()) {
                case BYTE:
                case BOOLEAN:
                case SHORT:
                case CHAR:
                case INT:
                case LONG:
                case WORD:
                case REFERENCE: {
                    if (iGeneral < _generalParameterRegisters.length()) {
                        result[i] = _generalParameterRegisters.get(iGeneral);
                        iGeneral++;
                    }
                    break;
                }
                case FLOAT:
                case DOUBLE: {
                    if (iXMM < _xmmParameterRegisters.length()) {
                        result[i] = _xmmParameterRegisters.get(iXMM);
                        iXMM++;
                    } else {
                        DebugBreak.here();
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
        result.add(stackPointer());
        result.add(framePointer());
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
        final AMD64GeneralRegister64[] result = new AMD64GeneralRegister64[_generalParameterRegisters.length()];
        for (int i = 0; i < _generalParameterRegisters.length(); i++) {
            final AMD64EirRegister.General r = (AMD64EirRegister.General) _generalParameterRegisters.get(i);
            result[i] = r.as64();
        }
        return result;
    }

    private static AMD64XMMRegister[] getTargetFloatingPointParameterRegisters() {
        final AMD64XMMRegister[] result = new AMD64XMMRegister[_xmmParameterRegisters.length()];
        for (int i = 0; i < _xmmParameterRegisters.length(); i++) {
            final AMD64EirRegister.XMM r = (AMD64EirRegister.XMM) _xmmParameterRegisters.get(i);
            result[i] = r.as();
        }
        return result;
    }

    protected void makeUnallocatable(AMD64EirRegister register) {
        unallocatableRegisters.add(register);
        allocatableRegisters.remove(register);
    }

    private static TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> targetABI(VMConfiguration vmConfiguration) {
        final Class<TargetABI<AMD64GeneralRegister64, AMD64XMMRegister>> type = null;
        return StaticLoophole.cast(type, vmConfiguration.targetABIsScheme().optimizedJavaABI());
    }

    protected UnixAMD64EirABI(VMConfiguration vmConfiguration) {
        super(vmConfiguration, targetABI(vmConfiguration));
        resultRegisters = PoolSet.noneOf(AMD64EirRegister.pool());
        resultRegisters.add((AMD64EirRegister) getResultLocation(Kind.LONG));
        resultRegisters.add((AMD64EirRegister) getResultLocation(Kind.DOUBLE));
    }

}
