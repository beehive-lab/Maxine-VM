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
package com.sun.c1x.target.x86;

import com.sun.c1x.asm.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.globalstub.*;
import com.sun.c1x.target.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;


public class X86GlobalStubEmitter implements GlobalStubEmitter {

    private X86MacroAssembler asm;
    private final Target target;
    private int frameSize;
    private CiRuntime runtime;

    public X86GlobalStubEmitter(CiRuntime runtime, Target target) {
        this.target = target;
        this.runtime = runtime;
    }

    public CiTargetMethod emit(GlobalStub stub) {
        asm = new X86MacroAssembler(target);
        this.frameSize = 0;


        switch(stub) {
            case SlowSubtypeCheck:
                emitSlowSubtypeCheck();
                break;
            default:
                throw Util.shouldNotReachHere();
        }

        return asm.finishTargetMethod(runtime, frameSize);
    }

    private void loadArgument(int index, Register register) {
        asm.movptr(register, new Address(X86.rsp, (index + 1) * target.arch.wordSize + frameSize));
    }

    private void storeArgument(int index, Register register) {
        asm.movptr(new Address(X86.rsp, (index + 1) * target.arch.wordSize + frameSize), register);
    }

    private int savedRegistersSize() {

        Register[] registers = X86.allRegisters;
        if (target.arch.is64bit()) {
            registers = X86.allRegisters64;
        }

        return registers.length * target.arch.wordSize;
    }

    private void saveRegisters() {

        Register[] registers = X86.allRegisters;
        if (target.arch.is64bit()) {
            registers = X86.allRegisters64;
        }

        int index = 0;
        for (Register r : registers) {
            if (r != X86.rsp) {
                asm.movq(new Address(X86.rsp, index * target.arch.wordSize), r);
                index++;
            }
        }

        int frameSize = index * target.arch.wordSize;
        assert this.frameSize >= frameSize;
    }

    private void restoreRegisters() {

        Register[] registers = X86.allRegisters;
        if (target.arch.is64bit()) {
            registers = X86.allRegisters64;
        }

        int index = 0;
        for (Register r : registers) {

            if (r == X86.rsp) {
                continue;
            }

            asm.movq(r, new Address(X86.rsp, index * target.arch.wordSize));


            index++;
        }
    }


    private void emitSlowSubtypeCheck() {

        this.frameSize = savedRegistersSize();

        asm.makeOffset(runtime.codeOffset());

        // Modify rsp
        asm.subq(X86.rsp, this.frameSize);

        forwardRuntimeCall(CiRuntimeCall.SlowSubtypeCheck, BasicType.Object, BasicType.Object, BasicType.Object);

        // Restore rsp
        asm.addq(X86.rsp, this.frameSize);

        asm.ret(0);
    }

    private void forwardRuntimeCall(CiRuntimeCall call, BasicType returnType, BasicType... args) {

        // Save registers
        saveRegisters();

        // Load arguments
        CiLocation[] result = new CiLocation[args.length];
        runtime.runtimeCallingConvention(args, result);
        for (int i = 0; i < args.length; i++) {
            loadArgument(i, result[i].first);
        }

        // Call to the runtime
        asm.callRuntime(call);

        this.storeArgument(0, runtime.returnRegister(BasicType.Object));

        // Restore registers including rsp
        restoreRegisters();
    }

}
