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
package com.sun.max.vm.compiler.eir.ia32.unix;

import static com.sun.max.vm.compiler.eir.ia32.IA32EirRegister.General.*;
import static com.sun.max.vm.compiler.eir.ia32.IA32EirRegister.XMM.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.ia32.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * This ABI is mostly adhered to by unix operating systems such as Solaris, Darwin and Linux.
 *
 * We introduce the notion of a frame pointer to give us some flexibility wrt. to template-based code generation.
 * The use of the stack pointer for register spilling, local variable and parameter access conflict with the explicit
 * use of the stack pointer by the templates for managing an evaluation stack. To abstract away from these details,
 * we introduce a frame pointer, which the optimizing compiler uses as a base for register spilling,
 * local variable and parameters. The ABI will return the stack pointer as a frame pointer to the compiler, unless it
 * is customized for use by a template-based compiler, in which case it returns a distinct register.
 *
 * @author Bernd Mathiske
 */
public abstract class UnixIA32EirABI extends IA32EirABI {

    @Override
    public EirLocation getResultLocation(Kind kind) {
        if (kind != null) {
            switch (kind.asEnum()) {
                case VOID:
                    return null;
                case BYTE:
                case BOOLEAN:
                case SHORT:
                case CHAR:
                case INT:
                case LONG:
                case WORD:
                case REFERENCE:
                    return IA32EirRegister.General.EAX;
                case FLOAT:
                case DOUBLE:
                    return IA32EirRegister.XMM.XMM0;
                default:
                    ProgramError.unknownCase();
                    return null;
            }
        }
        return null;
    }

    private final IndexedSequence<IA32EirRegister> _generalParameterRegisters = new ArraySequence<IA32EirRegister>();

    @Override
    public Sequence<IA32EirRegister> integerParameterRegisters() {
        return _generalParameterRegisters;
    }

    private final IndexedSequence<IA32EirRegister> _xmmParameterRegisters = new ArraySequence<IA32EirRegister>(XMM0, XMM1, XMM2, XMM3);

    @Override
    public Sequence<IA32EirRegister> floatingPointParameterRegisters() {
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

    @Override
    public IA32EirRegister getScratchRegister(Kind kind) {
        switch (kind.asEnum()) {
            case BYTE:
            case BOOLEAN:
            case SHORT:
            case CHAR:
            case INT:
            case LONG:
            case WORD:
            case REFERENCE:
                return EDI;
            case FLOAT:
            case DOUBLE:
                return XMM7;
            default:
                ProgramError.unknownCase();
                return null;
        }
    }

    @Override
    public IA32EirRegister.General stackPointer() {
        return ESP;
    }

    private PoolSet<IA32EirRegister> createUnallocatableRegisterPoolSet() {
        final PoolSet<IA32EirRegister> result = PoolSet.noneOf(IA32EirRegister.pool());
        result.add(stackPointer());
        result.add(framePointer());
        result.add(EBP); // TODO: for now until we have a JIT that does not need it
        result.add(integerRegisterActingAs(VMRegister.Role.SAFEPOINT_LATCH));
        for (Kind kind : Kind.PRIMITIVE_VALUES) {
            result.add(getScratchRegister(kind));
        }
        return result;
    }

    private final PoolSet<IA32EirRegister> _unallocatableRegisters = createUnallocatableRegisterPoolSet();

    @Override
    public PoolSet<IA32EirRegister> unallocatableRegisters() {
        return _unallocatableRegisters;
    }

    private PoolSet<IA32EirRegister> createAllocatableRegisterPoolSet() {
        final PoolSet<IA32EirRegister> result = PoolSet.noneOf(IA32EirRegister.pool());
        result.addAll();
        for (IA32EirRegister register : _unallocatableRegisters) {
            result.remove(register);
        }
        return result;
    }

    private final PoolSet<IA32EirRegister> _allocatableRegisters = createAllocatableRegisterPoolSet();

    @Override
    public PoolSet<IA32EirRegister> allocatableRegisters() {
        return _allocatableRegisters;
    }

    private final PoolSet<IA32EirRegister> _resultRegisters;

    @Override
    public PoolSet<IA32EirRegister> resultRegisters() {
        return _resultRegisters;
    }

    protected UnixIA32EirABI(VMConfiguration vmConfiguration) {
        super(vmConfiguration, null);
        _resultRegisters = PoolSet.noneOf(IA32EirRegister.pool());
        _resultRegisters.add(EAX);
        _resultRegisters.add(XMM0);
    }

}
