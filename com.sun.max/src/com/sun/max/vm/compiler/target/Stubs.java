/*
 * Copyright (c) 2017-2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
 */
package com.sun.max.vm.compiler.target;

import static com.sun.cri.ci.CiCallingConvention.Type.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.VMOptions.*;
import static com.sun.max.vm.compiler.CallEntryPoint.*;
import static com.sun.max.vm.compiler.deopt.Deoptimization.*;
import static com.sun.max.vm.compiler.target.Stub.Type.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.util.*;

import com.oracle.max.asm.target.aarch64.*;
import com.oracle.max.asm.target.amd64.*;
import com.oracle.max.asm.target.armv7.*;
import com.oracle.max.asm.target.armv7.ARMV7Assembler.*;
import com.oracle.max.asm.target.riscv64.RISCV64;
import com.oracle.max.asm.target.riscv64.RISCV64Address;
import com.oracle.max.asm.target.riscv64.RISCV64MacroAssembler;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.deopt.*;
import com.sun.max.vm.compiler.deopt.Deoptimization.Info;
import com.sun.max.vm.compiler.target.Stub.*;
import com.sun.max.vm.compiler.target.aarch64.*;
import com.sun.max.vm.compiler.target.amd64.*;
import com.sun.max.vm.compiler.target.arm.*;
import com.sun.max.vm.compiler.target.riscv64.RISCV64TargetMethodUtil;
import com.sun.max.vm.intrinsics.*;
import com.sun.max.vm.methodhandle.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.aarch64.*;
import com.sun.max.vm.runtime.amd64.*;
import com.sun.max.vm.runtime.arm.*;
import com.sun.max.vm.runtime.riscv64.RISCV64SafepointPoll;
import com.sun.max.vm.runtime.riscv64.RISCV64TrapFrameAccess;
import com.sun.max.vm.thread.*;

/**
 * Stubs are pieces of hand crafted assembly code for expressing semantics that cannot otherwise be expressed as Java.
 * For example, trampolines are stubs used to lazily link call sites to their targets at runtime.
 */
public class Stubs {

    final private class TrampolineList extends ArrayList<Stub> {
        private static final long serialVersionUID = 2119150483440944666L;
        final boolean isInterface;
        final char stubNamePrefix;

        TrampolineList(boolean isInterface) {
            this.isInterface = isInterface;
            stubNamePrefix = isInterface ? 'i' : 'v';
        }

        /**
         * Initialize the first invalid index with the canonical invalid index stub to avoid wasting memory with never used trampoline stubs.
         *
         * @param firstValidIndex the first valid index for a type of dispatch table.
         */
        @HOSTED_ONLY
        void initializeInvalidIndexEntries(int firstValidIndex) {
            for (int i = 0; i < firstValidIndex; i++) {
                add(Stub.canonicalInvalidIndexStub());
            }
        }

        /**
         * Generate trampolines for indexes up to the specified index.
         * This is the only method adding stubs to the list (and generally, modifying the list).
         *
         * @param index an index for a trampoline to resolve methods invoked via table-driven dispatch.
         */
        synchronized void makeTrampolines(int index) {
            for (int i = size(); i <= index; i++) {
                final String stubName = stubNamePrefix + "trampoline<" + i + ">";
                traceBeforeStubCreation(stubName);
                Stub stub = genDynamicTrampoline(i, isInterface, stubName);
                add(stub);
                traceAfterStubCreation(stubName);
            }
        }

        CodePointer getTrampoline(int index) {
            if (size() <= index) {
                makeTrampolines(index);
            }
            return VTABLE_ENTRY_POINT.in(get(index));
        }
    }

    /**
     * The stubs called to link an interface method call.
     */
    private final TrampolineList virtualTrampolines = new TrampolineList(false);

    /**
     * The stubs called to link an interface method call.
     */
    private final TrampolineList interfaceTrampolines = new TrampolineList(true);

    /**
     * The stub called to link a call site where the exact method being called is known.
     */
    private Stub staticTrampoline;

    /**
     * The stub called by the native level trap handler.
     *
     * @see Trap
     */
    private Stub trapStub;

    /**
     * The deopt stub per return value kind.
     */
    private final Stub[] deoptStubs = new Stub[CiKind.VALUES.length];

    /**
     * The deopt stub per return value kind for deoptimizing upon returning from a compiler stub.
     */
    private final Stub[] deoptStubsForCompilerStubs = new Stub[CiKind.VALUES.length];

    /**
     * Stub for method handle invokebasic intrinsic.
     */
    private Stub invokeBasicStub;

    /**
     * Position of the instruction in virtual / interface trampolines loading the immediate index in the scratch
     * register. Used to quickly retrieve the itable / vtable index the trampoline dispatch to.
     */
    private int indexMovInstrPos;

    /**
     * Return the index, relative to Hub's origin, to the entry of dispatch tables (virtual or interface) the stub is
     * assigned to.
     *
     * @param stub a virtual or interface trampoline stub
     * @return an index to a virtual table entry if the stub is a virtual call trampoline stub, an index to a interface
     *         table entry if the stub is a interface call trampoline.
     */
    public int getDispatchTableIndex(TargetMethod stub) {
        assert stub.is(VirtualTrampoline) || stub.is(InterfaceTrampoline);
        int tmpindex = 0;
        // TODO: Cleanup
        if (platform().isa == ISA.ARM) {
            Pointer callSitePointer = stub.codeStart().toPointer();
            if (((callSitePointer.readByte(3) & 0xff) == 0xe3) && ((callSitePointer.readByte(4 + 3) & 0xff) == 0xe3)) {
                tmpindex = (callSitePointer.readByte(4 + 0) & 0xff) | ((callSitePointer.readByte(4 + 1) & 0xf) << 8) | ((callSitePointer.readByte(4 + 2) & 0xf) << 12);
                tmpindex = tmpindex << 16;
                tmpindex += (callSitePointer.readByte(0) & 0xff) | ((callSitePointer.readByte(1) & 0xf) << 8) | ((callSitePointer.readByte(2) & 0xf) << 12);
            }
        } else if (platform().isa == ISA.AMD64) {
            tmpindex = stub.codeStart().toPointer().readInt(indexMovInstrPos);
        } else {
            throw FatalError.unimplemented("com.sun.max.vm.compiler.target.Stubs.getDispatchTableIndex");
        }
        final int index = tmpindex;
        assert stub.is(VirtualTrampoline) ? (virtualTrampolines.size() > index && virtualTrampolines.get(index) == stub)
                : (interfaceTrampolines.size() > index && interfaceTrampolines.get(index) == stub);
        return index;
    }

    /**
     * The deopt stub used for a frame stopped at a safepoint poll. This stub saves the registers, making them available
     * for deoptimization.
     */
    private Stub deoptStubForSafepointPoll;

    private CriticalMethod resolveVirtualCall;
    private CriticalMethod resolveInterfaceCall;
    private CriticalMethod resolveInvokeBasicCall;
    private CiValue[] resolveVirtualCallArgs;
    private CiValue[] resolveInterfaceCallArgs;
    private CiValue[] resolveInvokeBasicCallArgs;
    private RuntimeInitialization[] runtimeInits = {};

    public Stubs(RegisterConfigs registerConfigs) {
        this.registerConfigs = registerConfigs;
    }

    /**
     * Gets the stub called to link a call site where the exact method being called is known.
     */
    public Stub staticTrampoline() {
        return staticTrampoline;
    }

    /**
     * Returns the stub to resolve the target to a MethodHandle.invokeBasic intrinsic.
     * @return
     */
    public Stub invokeBasic() {
        return invokeBasicStub;
    }

    /**
     * Gets the stub called by the native level trap handler.
     *
     * @see #genTrapStub()
     */
    public Stub trapStub() {
        return trapStub;
    }

    /**
     * Gets the deoptimization stub for a given return value kind.
     *
     * @param fromCompilerStub specifies if the requested deopt stub is for use when patching a return from a
     *            {@linkplain Stub.Type#CompilerStub compiler stub}. Compiler stubs return values via the stack.
     */
    public Stub deoptStub(CiKind returnValueKind, boolean fromCompilerStub) {
        if (fromCompilerStub) {
            return deoptStubsForCompilerStubs[returnValueKind.stackKind().ordinal()];
        }
        return deoptStubs[returnValueKind.stackKind().ordinal()];
    }

    public Stub deoptStubForSafepointPoll() {
        return deoptStubForSafepointPoll;
    }

    /**
     * Performs all stub-related runtime initialization.
     */
    public void intialize() {
        for (RuntimeInitialization ri : runtimeInits) {
            ri.apply();
        }
    }

    private void delayedInit() {
        if (isHosted()) {
            if (prologueSize == -1) {
                prologueSize = OPTIMIZED_ENTRY_POINT.offset();
                resolveVirtualCall = new CriticalMethod(Stubs.class, "resolveVirtualCall", null);
                resolveInterfaceCall = new CriticalMethod(Stubs.class, "resolveInterfaceCall", null);
                resolveVirtualCallArgs = registerConfigs.trampoline.getCallingConvention(JavaCall, CiUtil.signatureToKinds(resolveVirtualCall.classMethodActor), target(), false).locations;
                resolveInterfaceCallArgs = registerConfigs.trampoline.getCallingConvention(JavaCall, CiUtil.signatureToKinds(resolveInterfaceCall.classMethodActor), target(), false).locations;
                resolveInvokeBasicCall = new CriticalMethod(Stubs.class, "resolveInvokeBasic", null);
                resolveInvokeBasicCallArgs = registerConfigs.trampoline.getCallingConvention(JavaCall, CiUtil.signatureToKinds(resolveInvokeBasicCall.classMethodActor), target(), false).locations;
                staticTrampoline = genStaticTrampoline();
                trapStub = genTrapStub();
                invokeBasicStub = genResolveInvokeBasicTarget();

                CriticalMethod unroll = new CriticalMethod(Stubs.class, "unroll", null);
                CiValue[] unrollArgs = registerConfigs.standard.getCallingConvention(JavaCall, CiUtil.signatureToKinds(unroll.classMethodActor), target(), false).locations;
                unroll.classMethodActor.compiledState = new Compilations(null, genUnroll(unrollArgs));

                deoptStubForSafepointPoll = genDeoptStubWithCSA(null, registerConfigs.trapStub, false);
                for (CiKind kind : CiKind.VALUES) {
                    deoptStubs[kind.ordinal()] = genDeoptStub(kind);
                    deoptStubsForCompilerStubs[kind.ordinal()] = genDeoptStubWithCSA(kind, registerConfigs.compilerStub, true);

                    String name = "unwind";
                    if (!kind.isVoid()) {
                        name = name + kind.name();
                    }
                    try {
                        CriticalMethod unwind = new CriticalMethod(Stubs.class, name, null);
                        CiValue[] unwindArgs = registerConfigs.standard.getCallingConvention(JavaCall, CiUtil.signatureToKinds(unwind.classMethodActor), target(), false).locations;
                        unwind.classMethodActor.compiledState = new Compilations(null, genUnwind(unwindArgs));
                    } catch (NoSuchMethodError e) {
                        // No unwind method for this kind
                    }
                }
            }
        }
    }

    public final RegisterConfigs registerConfigs;

    private int prologueSize = -1;

    public CodePointer interfaceTrampoline(int iIndex) {
        if (isHosted() && interfaceTrampolines.size() == 0) {
            interfaceTrampolines.initializeInvalidIndexEntries(DynamicHub.firstValidInterfaceIndex());
        }
        return interfaceTrampolines.getTrampoline(iIndex);
    }

    public CodePointer virtualTrampoline(int vTableIndex) {
        if (isHosted() && virtualTrampolines.size() == 0) {
            virtualTrampolines.initializeInvalidIndexEntries(Hub.vTableStartIndex());
        }
        return virtualTrampolines.getTrampoline(vTableIndex);
    }

    protected void traceBeforeStubCreation(String stubName) {
        if (verboseOption.verboseCompilation) {
            if (isHosted()) {
                Thread thread = Thread.currentThread();
                Log.println(thread.getName() + "[id=" + thread.getId() + "]: Creating stub " + stubName);
            } else {
                VmThread thread = VmThread.current();
                Log.println(thread.getName() + "[id=" + thread.id() + "]: Creating stub " + stubName);
            }
        }
    }

    protected void traceAfterStubCreation(String stubName) {
        if (verboseOption.verboseCompilation) {
            if (isHosted()) {
                Thread thread = Thread.currentThread();
                Log.println(thread.getName() + "[id=" + thread.getId() + "]: Created stub " + stubName);
            } else {
                VmThread thread = VmThread.current();
                Log.println(thread.getName() + "[id=" + thread.id() + "]: Created stub " + stubName);
            }
        }
    }

    private static CodePointer adjustEntryPointForCaller(CodePointer virtualDispatchEntryPoint, TargetMethod caller) {
        CallEntryPoint callEntryPoint = caller.callEntryPoint;
        return virtualDispatchEntryPoint.plus(callEntryPoint.offset() - VTABLE_ENTRY_POINT.offset());
    }

    public static boolean isJumpToStaticTrampoline(TargetMethod tm) {
        if (platform().isa == ISA.AMD64) {
            return AMD64TargetMethodUtil.isJumpTo(tm, OPTIMIZED_ENTRY_POINT.offset(), OPTIMIZED_ENTRY_POINT.in(vm().stubs.staticTrampoline()));
        } else if (platform().isa == ISA.ARM) {
            return ARMTargetMethodUtil.isJumpTo(tm, OPTIMIZED_ENTRY_POINT.offset(), OPTIMIZED_ENTRY_POINT.in(vm().stubs.staticTrampoline()));
        } else if (platform().isa == ISA.Aarch64) {
            return Aarch64TargetMethodUtil.isJumpTo(tm, OPTIMIZED_ENTRY_POINT.offset(), OPTIMIZED_ENTRY_POINT.in(vm().stubs.staticTrampoline()));
        } else if (platform().isa == ISA.RISCV64) {
            return RISCV64TargetMethodUtil.isJumpTo(tm, OPTIMIZED_ENTRY_POINT.offset(), OPTIMIZED_ENTRY_POINT.in(vm().stubs.staticTrampoline()));
        } else {
            throw FatalError.unimplemented("com.sun.max.vm.compiler.target.Stubs.isJumpToStaticTrampoline");
        }
    }

    /**
     * Resolves the vtable entry denoted by a given receiver object and vtable index.
     *
     * @param receiver the receiver of a virtual call
     * @param vTableIndex the vtable index of the call
     * @param pcInCaller an instruction address somewhere in the caller (usually the return address) that can be used to
     *            look up the caller in the code cache
     */
    private static Address resolveVirtualCall(Object receiver, int vTableIndex, Pointer pcInCaller) {
        // pcInCaller must be dealt with before any safepoint
        CodePointer cpCallSite = CodePointer.from(pcInCaller);
        final TargetMethod caller = cpCallSite.toTargetMethod();
        final Hub hub = ObjectAccess.readHub(receiver);
        final VirtualMethodActor selectedCallee = hub.classActor.getVirtualMethodActorByVTableIndex(vTableIndex);
        if (selectedCallee.isAbstract()) {
            throw new AbstractMethodError();
        }

        final TargetMethod selectedCalleeTargetMethod = selectedCallee.makeTargetMethod(caller);
        FatalError.check(selectedCalleeTargetMethod.invalidated() == null, "resolved virtual method must not be invalidated");
        CodePointer vtableEntryPoint = selectedCalleeTargetMethod.getEntryPoint(VTABLE_ENTRY_POINT);
        hub.setWord(vTableIndex, vtableEntryPoint.toAddress());

        CodePointer adjustedEntryPoint = adjustEntryPointForCaller(vtableEntryPoint, caller);

        // remember calls from boot code region to baseline code cache
        if (Code.bootCodeRegion().contains(cpCallSite.toAddress()) && Code.getCodeManager().getRuntimeBaselineCodeRegion().contains(adjustedEntryPoint.toAddress())) {
            CodeManager.recordBootToBaselineCaller(caller);
        }

        return adjustedEntryPoint.toAddress();
    }

    /**
     * Resolves the itable entry denoted by a given receiver object and index operand of an interface call.
     *
     * @param receiver the receiver of an interface call
     * @param iIndex the index operand of the call
     * @param pcInCaller an instruction address somewhere in the caller (usually the return address) that can be used to
     *            look up the caller in the code cache
     */
    private static Address resolveInterfaceCall(Object receiver, int iIndex, Pointer pcInCaller) {
        // pcInCaller must be dealt with before any safepoint
        CodePointer cpCallSite = CodePointer.from(pcInCaller);
        final TargetMethod caller = cpCallSite.toTargetMethod();
        final Hub hub = ObjectAccess.readHub(receiver);
        final VirtualMethodActor selectedCallee = hub.classActor.getVirtualMethodActorByIIndex(iIndex);
        if (selectedCallee.isAbstract()) {
            throw new AbstractMethodError();
        }

        CodePointer itableEntryPoint = selectedCallee.makeTargetMethod(caller).getEntryPoint(VTABLE_ENTRY_POINT);
        hub.setWord(hub.iTableStartIndex + iIndex, itableEntryPoint.toAddress());

        CodePointer adjustedEntryPoint = adjustEntryPointForCaller(itableEntryPoint, caller);

        // remember calls from boot code region to baseline code cache
        if (Code.bootCodeRegion().contains(cpCallSite.toAddress()) && Code.getCodeManager().getRuntimeBaselineCodeRegion().contains(adjustedEntryPoint.toAddress())) {
            CodeManager.recordBootToBaselineCaller(caller);
        }

        return adjustedEntryPoint.toAddress();
    }

    /**
     * Resolves an invokeBasic target. Note that this resolves to the address of the baseline
     * entry point.
     * @param mh
     * @return
     */
    private static Address resolveInvokeBasic(Object mh, Pointer pcInCaller) {
        CodePointer cpCallSite = CodePointer.from(pcInCaller);
        final TargetMethod caller = cpCallSite.toTargetMethod();
        ClassMethodActor target = MaxMethodHandles.getInvokerForInvokeBasic(mh);
        TargetMethod tm = target.makeTargetMethod(caller);
        Address address = tm.getEntryPoint(caller.callEntryPoint).toAddress();
        return address;
    }

    /**
     * Generates the stub for invokeBasic target resolution.
     * @return
     */
    private Stub genResolveInvokeBasicTarget() {
        delayedInit();
        if (platform().isa == ISA.AMD64) {
            CiRegisterConfig registerConfig = registerConfigs.trampoline;
            AMD64MacroAssembler asm = new AMD64MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            int frameSize = target().alignFrameSize(csl.size);
            final int frameToCSA = csl.frameOffsetToCSA;

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }

            // now allocate the frame for this method
            asm.subq(AMD64.rsp, frameSize);

            // save all the callee save registers
            asm.save(csl, frameToCSA);

            CiValue[] args = resolveInvokeBasicCallArgs;
            asm.movq(args[1].asRegister(), new CiAddress(WordUtil.archKind(), AMD64.rsp.asValue(), frameSize));

            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = resolveInvokeBasicCall.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // Put the entry point of the resolved method on the stack just below the
            // return address of the trampoline itself. By adjusting RSP to point at
            // this second return address and executing a 'ret' instruction, execution
            // continues in the resolved method as if it was called by the trampoline's
            // caller which is exactly what we want.
            CiRegister returnReg = registerConfig.getReturnRegister(WordUtil.archKind());
            asm.movq(new CiAddress(WordUtil.archKind(), AMD64.rsp.asValue(), frameSize - 8), returnReg);

            // Restore all parameter registers before returning
            int registerRestoreEpilogueOffset = asm.codeBuffer.position();
            asm.restore(csl, frameToCSA);

            // Adjust RSP as mentioned above and do the 'ret' that lands us in the
            // trampolined-to method.
            asm.addq(AMD64.rsp, frameSize - 8);
            asm.ret(0);

            byte[] code = asm.codeBuffer.close(true);

            String stubName = "invokeBasic";
            return new Stub(InvokeBasic, stubName, frameSize, code, callPos, callSize, callee, registerRestoreEpilogueOffset);
        } else if (platform().isa == ISA.ARM) {
            CiRegisterConfig registerConfig = registerConfigs.trampoline;
            ARMV7MacroAssembler asm = new ARMV7MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            int frameSize = target().alignFrameSize(csl.size);
            final int frameToCSA = csl.frameOffsetToCSA;

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }
            asm.push(ARMV7Assembler.ConditionFlag.Always, 1 << 14, true);

            // now allocate the frame for this method
            asm.subq(ARMV7.rsp, frameSize);

            // save all the callee save registers
            asm.save(csl, frameToCSA);

            CiValue[] args = resolveInvokeBasicCallArgs;
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.rsp.asValue(), frameSize));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, args[1].asRegister(), asm.scratchRegister, 0);

            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = resolveInvokeBasicCall.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // Put the entry point of the resolved method on the stack just below the
            // return address of the trampoline itself. By adjusting RSP to point at
            // this second return address and executing a 'ret' instruction, execution
            // continues in the resolved method as if it was called by the trampoline's
            // caller which is exactly what we want.
            CiRegister returnReg = registerConfig.getReturnRegister(WordUtil.archKind());
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.rsp.asValue(), frameSize - 4));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, returnReg, asm.scratchRegister, 0);

            // Restore all parameter registers before returning
            int registerRestoreEpilogueOffset = asm.codeBuffer.position();
            asm.restore(csl, frameToCSA);

            // Adjust RSP as mentioned above and do the 'ret' that lands us in the
            // trampolined-to method.
            asm.addq(ARMV7.rsp, frameSize - 4);
            asm.ret(0);

            byte[] code = asm.codeBuffer.close(true);

            String stubName = "invokeBasic";
            return new Stub(InvokeBasic, stubName, frameSize, code, callPos, callSize, callee, registerRestoreEpilogueOffset);
        } else if (platform().isa == ISA.Aarch64) {
            CiRegisterConfig registerConfig = registerConfigs.trampoline;
            Aarch64MacroAssembler asm = new Aarch64MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            int frameSize = target().alignFrameSize(csl.size);
            final int frameToCSA = csl.frameOffsetToCSA;

            asm.nop(prologueSize / Aarch64Assembler.INSTRUCTION_SIZE);

            asm.push(Aarch64.linkRegister);

            // now allocate the frame for this method
            asm.sub(64, Aarch64.sp, Aarch64.sp, frameSize);

            // save all the callee save registers
            asm.save(csl, frameToCSA);

            CiValue[] args = resolveInvokeBasicCallArgs;
            asm.ldr(64, args[1].asRegister(), Aarch64Address.createUnscaledImmediateAddress(Aarch64.sp, frameSize));

            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = resolveInvokeBasicCall.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // Store the result in the second scratch register (the first will be restored) before restoring
            CiRegister returnReg = registerConfig.getReturnRegister(WordUtil.archKind());
            asm.mov(64, registerConfig.getScratchRegister1(), returnReg);

            // Restore all parameter registers before returning
            int registerRestoreEpilogueOffset = asm.codeBuffer.position();
            asm.restore(csl, frameToCSA);

            // pop this method's frame
            asm.add(64, Aarch64.sp, Aarch64.sp, frameSize);
            // restore linkRegister
            asm.pop(Aarch64.linkRegister);

            // jump to the resolved method
            asm.jmp(registerConfig.getScratchRegister1());

            byte[] code = asm.codeBuffer.close(true);

            String stubName = "invokeBasic";
            return new Stub(InvokeBasic, stubName, frameSize, code, callPos, callSize, callee, registerRestoreEpilogueOffset);
        } else if (platform().isa == ISA.RISCV64) {
            CiRegisterConfig registerConfig = registerConfigs.trampoline;
            RISCV64MacroAssembler asm = new RISCV64MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            int frameSize = target().alignFrameSize(csl.size);
            final int frameToCSA = csl.frameOffsetToCSA;

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }
            asm.push(64, RISCV64.ra);

            // now allocate the frame for this method
            asm.subi(RISCV64.sp, RISCV64.sp, frameSize);

            // save all the callee save registers
            asm.save(csl, frameToCSA);

            CiValue[] args = resolveInvokeBasicCallArgs;
            asm.ldru(64, args[1].asRegister(), RISCV64Address.createImmediateAddress(RISCV64.sp, frameSize));

            asm.alignForPatchableDirectCall(asm.codeBuffer.position());
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = resolveInvokeBasicCall.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // Store the result in the second scratch register (the first will be restored) before restoring
            CiRegister returnReg = registerConfig.getReturnRegister(WordUtil.archKind());
            asm.mov(registerConfig.getScratchRegister1(), returnReg);

            // Restore all parameter registers before returning
            int registerRestoreEpilogueOffset = asm.codeBuffer.position();
            asm.restore(csl, frameToCSA);

            // pop this method's frame
            asm.addi(RISCV64.sp, RISCV64.sp, frameSize);
            // restore linkRegister
            asm.pop(64, RISCV64.ra, true);

            // jump to the resolved method
            asm.jalr(RISCV64.zero, registerConfig.getScratchRegister1(), 0);

            byte[] code = asm.codeBuffer.close(true);

            String stubName = "invokeBasic";
            return new Stub(InvokeBasic, stubName, frameSize, code, callPos, callSize, callee, registerRestoreEpilogueOffset);
        } else {
            throw FatalError.unimplemented("com.sun.max.vm.compiler.target.Stubs.genResolveInvokeBasicTarget");
        }
    }

    private Stub genDynamicTrampoline(int index, boolean isInterface, String stubName) {
        delayedInit();
        if (platform().isa == ISA.AMD64) {
            CiRegisterConfig registerConfig = registerConfigs.trampoline;
            AMD64MacroAssembler asm = new AMD64MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            int frameSize = target().alignFrameSize(csl.size);
            final int frameToCSA = csl.frameOffsetToCSA;

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }

            // now allocate the frame for this method
            asm.subq(AMD64.rsp, frameSize);

            // save the index in the scratch register. This register is then callee-saved
            // so that the stack walker can find it.
            asm.movl(registerConfig.getScratchRegister(), index);
            if (isHosted() && index == 0) {
                indexMovInstrPos = asm.codeBuffer.position() - WordWidth.BITS_32.numberOfBytes;
            }

            // save all the callee save registers
            asm.save(csl, frameToCSA);

            CiValue[] args = isInterface ? resolveInterfaceCallArgs : resolveVirtualCallArgs;

            // the receiver is already in the first arg register
            // asm.movq(locations[0].asRegister(), locations[0].asRegister());

            // load the index into the second arg register
            asm.movl(args[1].asRegister(), index);

            // load the return address into the third arg register
            asm.movq(args[2].asRegister(), new CiAddress(WordUtil.archKind(), AMD64.rsp.asValue(), frameSize));

            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = isInterface ? resolveInterfaceCall.classMethodActor : resolveVirtualCall.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // Put the entry point of the resolved method on the stack just below the
            // return address of the trampoline itself. By adjusting RSP to point at
            // this second return address and executing a 'ret' instruction, execution
            // continues in the resolved method as if it was called by the trampoline's
            // caller which is exactly what we want.
            CiRegister returnReg = registerConfig.getReturnRegister(WordUtil.archKind());
            asm.movq(new CiAddress(WordUtil.archKind(), AMD64.rsp.asValue(), frameSize - 8), returnReg);

            // Restore all parameter registers before returning
            int registerRestoreEpilogueOffset = asm.codeBuffer.position();
            asm.restore(csl, frameToCSA);

            // Adjust RSP as mentioned above and do the 'ret' that lands us in the
            // trampolined-to method.
            asm.addq(AMD64.rsp, frameSize - 8);
            asm.ret(0);

            byte[] code = asm.codeBuffer.close(true);
            final Type type = isInterface ? InterfaceTrampoline : VirtualTrampoline;
            return new Stub(type, stubName, frameSize, code, callPos, callSize, callee, registerRestoreEpilogueOffset);
        } else if (platform().isa == ISA.ARM) {
            CiRegisterConfig registerConfig = registerConfigs.trampoline;
            ARMV7MacroAssembler asm = new ARMV7MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            int frameSize = target().alignFrameSize(csl.size);
            final int frameToCSA = csl.frameOffsetToCSA;

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }
            asm.push(ARMV7Assembler.ConditionFlag.Always, 1 << 14, true);
            // now allocate the frame for this method
            asm.subq(ARMV7.rsp, frameSize);

            // Save the index in the scratch register
            asm.movImm32(ConditionFlag.Always, ARMV7.r8, index);
            if (isHosted() && index == 0) {
                indexMovInstrPos = asm.codeBuffer.position() - 8;
            }

            // save all the callee save registers
            asm.save(csl, frameToCSA);

            CiValue[] args = isInterface ? resolveInterfaceCallArgs : resolveVirtualCallArgs;

            // load the index into the second arg register
            asm.movImm32(ConditionFlag.Always, args[1].asRegister(), index);

            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP, frameSize));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, args[2].asRegister(), asm.scratchRegister, 0);

            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = isInterface ? resolveInterfaceCall.classMethodActor : resolveVirtualCall.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // Put the entry point of the resolved method on the stack just below the
            // return address of the trampoline itself. By adjusting RSP to point at
            // this second return address and executing a 'ret' instruction, execution
            // continues in the resolved method as if it was called by the trampoline's
            // caller which is exactly what we want.
            CiRegister returnReg = registerConfig.getReturnRegister(WordUtil.archKind());
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP, frameSize - 4));
            asm.str(ARMV7Assembler.ConditionFlag.Always, returnReg, asm.scratchRegister, 0);

            // Restore all parameter registers before returning
            int registerRestoreEpilogueOffset = asm.codeBuffer.position();
            asm.restore(csl, frameToCSA);

            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP, frameSize - 4));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, asm.scratchRegister, 0);
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP, frameSize));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.LR, asm.scratchRegister, 0);
            asm.addq(ARMV7.rsp, frameSize + 4);
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.PC, ARMV7.r8);

            byte[] code = asm.codeBuffer.close(true);
            final Type type = isInterface ? InterfaceTrampoline : VirtualTrampoline;
            return new Stub(type, stubName, frameSize, code, callPos, callSize, callee, registerRestoreEpilogueOffset);
        } else if (platform().isa == ISA.Aarch64) {
            CiRegisterConfig registerConfig = registerConfigs.trampoline;
            Aarch64MacroAssembler asm = new Aarch64MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            int frameSize = target().alignFrameSize(csl.size);
            final int frameToCSA = csl.frameOffsetToCSA;

            asm.nop(prologueSize / Aarch64Assembler.INSTRUCTION_SIZE);
            asm.push(Aarch64.linkRegister);
            // now allocate the frame for this method
            asm.sub(64, Aarch64.sp, Aarch64.sp, frameSize);

            // save the index in the scratch register. This register is then callee-saved
            // so that the stack walker can find it.
            if (isHosted() && index == 0) {
                indexMovInstrPos = asm.codeBuffer.position();
            }
            asm.mov(registerConfig.getScratchRegister(), index);

            // save all the callee save registers
            asm.save(csl, frameToCSA);

            CiValue[] args = isInterface ? resolveInterfaceCallArgs : resolveVirtualCallArgs;

            // the receiver is already in the first arg register

            // load the index into the second arg register
            asm.mov(args[1].asRegister(), index);

            // load the return address into the third arg register
            asm.ldr(64, args[2].asRegister(), Aarch64Address.createUnscaledImmediateAddress(Aarch64.sp, frameSize));

            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = isInterface ? resolveInterfaceCall.classMethodActor : resolveVirtualCall.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // Store the result in the second scratch register (the first will be restored) before restoring
            CiRegister returnReg = registerConfig.getReturnRegister(WordUtil.archKind());
            asm.mov(64, registerConfig.getScratchRegister1(), returnReg);

            // Restore all parameter registers before returning
            int registerRestoreEpilogueOffset = asm.codeBuffer.position();
            asm.restore(csl, frameToCSA);

            asm.add(64, Aarch64.sp, Aarch64.sp, frameSize);
            asm.pop(Aarch64.linkRegister);

            // Jump to the entry point of the resolved method
            asm.jmp(registerConfig.getScratchRegister1());

            byte[] code = asm.codeBuffer.close(true);
            final Type type = isInterface ? InterfaceTrampoline : VirtualTrampoline;
            return new Stub(type, stubName, frameSize, code, callPos, callSize, callee, registerRestoreEpilogueOffset, asm.trampolines(1));
        } else if (platform().isa == ISA.RISCV64) {
            CiRegisterConfig registerConfig = registerConfigs.trampoline;
            RISCV64MacroAssembler asm = new RISCV64MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            int frameSize = target().alignFrameSize(csl.size);
            final int frameToCSA = csl.frameOffsetToCSA;

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }
            asm.push(64, RISCV64.ra);
            // now allocate the frame for this method
            asm.subi(RISCV64.sp, RISCV64.sp, frameSize);

            // save the index in the scratch register. This register is then callee-saved
            // so that the stack walker can find it.
            if (isHosted() && index == 0) {
                indexMovInstrPos = asm.codeBuffer.position();
            }
            asm.mov(registerConfig.getScratchRegister(), index);

            // save all the callee save registers
            asm.save(csl, frameToCSA);

            CiValue[] args = isInterface ? resolveInterfaceCallArgs : resolveVirtualCallArgs;

            // the receiver is already in the first arg register

            // load the index into the second arg register
            asm.mov(args[1].asRegister(), index);

            // load the return address into the third arg register
            asm.ldru(64, args[2].asRegister(), RISCV64Address.createImmediateAddress(RISCV64.sp, frameSize));

            asm.alignForPatchableDirectCall(asm.codeBuffer.position());
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = isInterface ? resolveInterfaceCall.classMethodActor : resolveVirtualCall.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // Store the result in the second scratch register (the first will be restored) before restoring
            CiRegister returnReg = registerConfig.getReturnRegister(WordUtil.archKind());
            asm.mov(registerConfig.getScratchRegister1(), returnReg);

            // Restore all parameter registers before returning
            int registerRestoreEpilogueOffset = asm.codeBuffer.position();
            asm.restore(csl, frameToCSA);

            asm.addi(RISCV64.sp, RISCV64.sp, frameSize);
            asm.pop(64, RISCV64.ra, true);

            // Jump to the entry point of the resolved method
            asm.jalr(RISCV64.zero, registerConfig.getScratchRegister1(), 0);

            byte[] code = asm.codeBuffer.close(true);
            final Type type = isInterface ? InterfaceTrampoline : VirtualTrampoline;
            return new Stub(type, stubName, frameSize, code, callPos, callSize, callee, registerRestoreEpilogueOffset);
        } else {
            throw FatalError.unimplemented("com.sun.max.vm.compiler.target.Stubs.genDynamicTrampoline");
        }
    }

    @PLATFORM(cpu = "armv7")
    private static void patchStaticTrampolineCallSiteARMV7(Pointer callSite) {
        CodePointer cpCallSite = CodePointer.from(callSite);
        final TargetMethod caller = cpCallSite.toTargetMethod();
        final ClassMethodActor callee = caller.callSiteToCallee(cpCallSite);
        final CodePointer calleeEntryPoint = callee.makeTargetMethod(caller).getEntryPoint(caller.callEntryPoint);
        ARMTargetMethodUtil.mtSafePatchCallDisplacement(caller, cpCallSite, calleeEntryPoint);

        // remember calls from boot code region to baseline code cache
        if (Code.bootCodeRegion().contains(cpCallSite.toAddress()) && Code.getCodeManager().getRuntimeBaselineCodeRegion().contains(calleeEntryPoint.toAddress())) {
            CodeManager.recordBootToBaselineCaller(caller);
        }
    }

    @PLATFORM(cpu = "amd64")
    private static void patchStaticTrampolineCallSiteAMD64(Pointer callSite) {
        CodePointer cpCallSite = CodePointer.from(callSite);
        final TargetMethod caller = cpCallSite.toTargetMethod();
        final ClassMethodActor callee = caller.callSiteToCallee(cpCallSite);
        final CodePointer calleeEntryPoint = callee.makeTargetMethod(caller).getEntryPoint(caller.callEntryPoint);

        AMD64TargetMethodUtil.mtSafePatchCallDisplacement(caller, cpCallSite, calleeEntryPoint);

        // remember calls from boot code region to baseline code cache
        if (Code.bootCodeRegion().contains(cpCallSite.toAddress()) && Code.getCodeManager().getRuntimeBaselineCodeRegion().contains(calleeEntryPoint.toAddress())) {
            CodeManager.recordBootToBaselineCaller(caller);
        }
    }

    @PLATFORM(cpu = "aarch64")
    private static void patchStaticTrampolineCallSiteAarch64(Pointer callSite) {
        CodePointer cpCallSite = CodePointer.from(callSite);

        final TargetMethod caller = cpCallSite.toTargetMethod();
        final ClassMethodActor callee = caller.callSiteToCallee(cpCallSite);

        final CodePointer calleeEntryPoint = callee.makeTargetMethod(caller).getEntryPoint(caller.callEntryPoint);
        Aarch64TargetMethodUtil.mtSafePatchCallDisplacement(caller, cpCallSite, calleeEntryPoint);

        // remember calls from boot code region to baseline code cache
        if (Code.bootCodeRegion().contains(cpCallSite.toAddress()) && Code.getCodeManager().getRuntimeBaselineCodeRegion().contains(calleeEntryPoint.toAddress())) {
            CodeManager.recordBootToBaselineCaller(caller);
        }
    }

    @PLATFORM(cpu = "riscv64")
    private static void patchStaticTrampolineCallSiteRISCV64(Pointer callSite) {
        CodePointer cpCallSite = CodePointer.from(callSite);

        final TargetMethod caller = cpCallSite.toTargetMethod();
        final ClassMethodActor callee = caller.callSiteToCallee(cpCallSite);

        final CodePointer calleeEntryPoint = callee.makeTargetMethod(caller).getEntryPoint(caller.callEntryPoint);
        RISCV64TargetMethodUtil.mtSafePatchCallDisplacement(caller, cpCallSite, calleeEntryPoint);

        // remember calls from boot code region to baseline code cache
        if (Code.bootCodeRegion().contains(cpCallSite.toAddress()) && Code.getCodeManager().getRuntimeBaselineCodeRegion().contains(calleeEntryPoint.toAddress())) {
            CodeManager.recordBootToBaselineCaller(caller);
        }
    }

    /**
     * Generates a stub that links a call to a method whose actor is available in data
     * {@linkplain TargetMethod#callSiteToCallee(CodePointer) associated} with the call site. The stub also saves and
     * restores all the callee-saved registers specified in the {@linkplain RegisterConfigs#trampoline trampoline}
     * register configuration.
     */
    @HOSTED_ONLY
    private Stub genStaticTrampoline() {
        if (platform().isa == ISA.AMD64) {
            CiRegisterConfig registerConfig = registerConfigs.trampoline;
            AMD64MacroAssembler asm = new AMD64MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            int frameSize = target().alignFrameSize(csl.size);
            int frameToCSA = csl.frameOffsetToCSA;

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }

            // compute the static trampoline call site
            CiRegister callSite = registerConfig.getScratchRegister();
            asm.movq(callSite, new CiAddress(WordUtil.archKind(), AMD64.rsp.asValue()));
            asm.subq(callSite, AMD64TargetMethodUtil.RIP_CALL_INSTRUCTION_SIZE);

            // now allocate the frame for this method
            asm.subq(AMD64.rsp, frameSize);

            // save all the callee save registers
            asm.save(csl, frameToCSA);

            CriticalMethod patchStaticTrampoline = new CriticalMethod(Stubs.class, "patchStaticTrampolineCallSiteAMD64", null);
            CiKind[] trampolineParameters = CiUtil.signatureToKinds(patchStaticTrampoline.classMethodActor);
            CiValue[] locations = registerConfig.getCallingConvention(JavaCall, trampolineParameters, target(), false).locations;

            // load the static trampoline call site into the first parameter register
            asm.movq(locations[0].asRegister(), callSite);

            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = patchStaticTrampoline.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // restore all parameter registers before returning
            int registerRestoreEpilogueOffset = asm.codeBuffer.position();
            asm.restore(csl, frameToCSA);

            // undo the frame
            asm.addq(AMD64.rsp, frameSize);

            // patch the return address to re-execute the static call
            asm.movq(callSite, new CiAddress(WordUtil.archKind(), AMD64.rsp.asValue()));
            asm.subq(callSite, AMD64TargetMethodUtil.RIP_CALL_INSTRUCTION_SIZE);
            asm.movq(new CiAddress(WordUtil.archKind(), AMD64.rsp.asValue()), callSite);

            asm.ret(0);

            String stubName = "strampoline";
            byte[] code = asm.codeBuffer.close(true);

            return new Stub(StaticTrampoline, stubName, frameSize, code, callPos, callSize, callee, registerRestoreEpilogueOffset);
        } else if (platform().isa == ISA.ARM) {
            CiRegisterConfig registerConfig = registerConfigs.trampoline;
            ARMV7MacroAssembler asm = new ARMV7MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            int frameSize = target().alignFrameSize(csl.size);
            int frameToCSA = csl.frameOffsetToCSA;

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }
            // Push LR on the stack to make asm.ret work
            asm.push(ARMV7Assembler.ConditionFlag.Always, 1 << 14, true);

            // compute the static trampoline call site
            CiRegister callSite = ARMV7.r8;
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, callSite, asm.scratchRegister, 0);
            asm.subq(callSite, ARMTargetMethodUtil.RIP_CALL_INSTRUCTION_SIZE);

            // now allocate the frame for this method
            asm.subq(ARMV7.rsp, frameSize);

            // save all the callee save registers
            asm.save(csl, frameToCSA);

            CriticalMethod patchStaticTrampoline = new CriticalMethod(Stubs.class, "patchStaticTrampolineCallSiteARMV7", null);
            CiKind[] trampolineParameters = CiUtil.signatureToKinds(patchStaticTrampoline.classMethodActor);
            CiValue[] locations = registerConfig.getCallingConvention(JavaCall, trampolineParameters, target(), false).locations;

            // load the static trampoline call site into the first parameter register
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, locations[0].asRegister(), callSite);

            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = patchStaticTrampoline.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // restore all parameter registers before returning
            int registerRestoreEpilogueOffset = asm.codeBuffer.position();
            asm.restore(csl, frameToCSA);

            // undo the frame
            asm.addq(ARMV7.rsp, frameSize);

            // patch the return address to re-execute the static call
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, asm.scratchRegister, ARMV7.rsp, 0);
            asm.subq(asm.scratchRegister, ARMTargetMethodUtil.RIP_CALL_INSTRUCTION_SIZE);
            asm.strImmediate(ARMV7Assembler.ConditionFlag.Always, 1, 1, 0, asm.scratchRegister, ARMV7.rsp, 0);

            asm.ret(0);
            String stubName = "strampoline";
            byte[] code = asm.codeBuffer.close(true);
            return new Stub(StaticTrampoline, stubName, frameSize, code, callPos, callSize, callee, registerRestoreEpilogueOffset);
        } else if (platform().isa == ISA.Aarch64) {
            CiRegisterConfig registerConfig = registerConfigs.trampoline;
            Aarch64MacroAssembler asm = new Aarch64MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            int frameSize = target().alignFrameSize(csl.size);
            int frameToCSA = csl.frameOffsetToCSA;

            asm.nop(prologueSize / Aarch64Assembler.INSTRUCTION_SIZE);

            // compute the static trampoline call site
            CiRegister callSite = registerConfig.getScratchRegister();
            asm.sub(64, callSite, Aarch64.linkRegister, Aarch64MacroAssembler.RIP_CALL_INSTRUCTION_SIZE);

            // Push the link register
            asm.push(Aarch64.linkRegister);

            // now allocate the frame for this method
            asm.sub(64, Aarch64.sp, Aarch64.sp, frameSize);

            // save all the callee save registers
            asm.save(csl, frameToCSA);

            CriticalMethod patchStaticTrampoline = new CriticalMethod(Stubs.class, "patchStaticTrampolineCallSiteAarch64", null);
            CiKind[] trampolineParameters = CiUtil.signatureToKinds(patchStaticTrampoline.classMethodActor);
            CiValue[] locations = registerConfig.getCallingConvention(JavaCall, trampolineParameters, target(), false).locations;

            // load the static trampoline call site into the first parameter register
            asm.mov(64, locations[0].asRegister(), callSite);

            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = patchStaticTrampoline.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // restore all parameter registers before returning
            int registerRestoreEpilogueOffset = asm.codeBuffer.position();
            asm.restore(csl, frameToCSA);

            // undo the frame
            asm.add(64, Aarch64.sp, Aarch64.sp, frameSize);

            // Pop the link register
            asm.pop(Aarch64.linkRegister);

            // re-execute the static call. Now that the call has been patched we need to return to the beginning of the
            // patched call site, thus we need to subtract from the link register the size of the segment preparing the call
            asm.sub(64, callSite, Aarch64.linkRegister, Aarch64MacroAssembler.RIP_CALL_INSTRUCTION_SIZE);
            asm.ret(callSite);

            String stubName = "strampoline";
            byte[] code = asm.codeBuffer.close(true);
            return new Stub(StaticTrampoline, stubName, frameSize, code, callPos, callSize, callee, registerRestoreEpilogueOffset);
        } else if (platform().isa == ISA.RISCV64) {
            CiRegisterConfig registerConfig = registerConfigs.trampoline;
            RISCV64MacroAssembler asm = new RISCV64MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            int frameSize = target().alignFrameSize(csl.size);
            int frameToCSA = csl.frameOffsetToCSA;

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }

            // compute the static trampoline call site
            CiRegister callSite = registerConfig.getScratchRegister();
            asm.subi(callSite, RISCV64.ra, RISCV64MacroAssembler.RIP_CALL_INSTRUCTION_SIZE);

            // Push the link register
            asm.push(64, RISCV64.ra);

            // now allocate the frame for this method
            asm.subi(RISCV64.sp, RISCV64.sp, frameSize);

            // save all the callee save registers
            asm.save(csl, frameToCSA);

            CriticalMethod patchStaticTrampoline = new CriticalMethod(Stubs.class, "patchStaticTrampolineCallSiteRISCV64", null);
            CiKind[] trampolineParameters = CiUtil.signatureToKinds(patchStaticTrampoline.classMethodActor);
            CiValue[] locations = registerConfig.getCallingConvention(JavaCall, trampolineParameters, target(), false).locations;

            // load the static trampoline call site into the first parameter register
            asm.mov(locations[0].asRegister(), callSite);

            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = patchStaticTrampoline.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // restore all parameter registers before returning
            int registerRestoreEpilogueOffset = asm.codeBuffer.position();
            asm.restore(csl, frameToCSA);

            // undo the frame
            asm.add(64, RISCV64.sp, RISCV64.sp, frameSize);

            // Pop the link register
            asm.pop(64, RISCV64.ra, true);

            // re-execute the static call. Now that the call has been patched we need to return to the beginning of the
            // patched call site, thus we need to subtract from the link register the size of the segment preparing the call
            asm.subi(callSite, RISCV64.ra, RISCV64MacroAssembler.RIP_CALL_INSTRUCTION_SIZE);
            asm.ret(callSite);

            String stubName = "strampoline";
            byte[] code = asm.codeBuffer.close(true);
            return new Stub(StaticTrampoline, stubName, frameSize, code, callPos, callSize, callee, registerRestoreEpilogueOffset);
        } else {
            throw FatalError.unimplemented("com.sun.max.vm.compiler.target.Stubs.genStaticTrampoline");
        }
    }

    /**
     * Generates the stub called by the native level trap handler (see trap.c). The stub:
     * <ol>
     * <li>flushes all the registers specified in the {@linkplain RegisterConfigs#trapStub trap stub} register
     * configuration to the stack (plus the trap number and any platform specific state such as the flags register on
     * AMD64),</li>
     * <li>adjusts the return address of the trap frame to be the address of the trapped instruction,</li>
     * <li>calls {@link Trap#handleTrap},</li>
     * <li>restores the saved registers and platform-specific state, and</li>
     * <li>returns execution to the trapped frame to re-execute the trapped instruction.</li>
     * </ol>
     * <p/>
     * For traps resulting in runtime exceptions (e.g. {@link NullPointerException}), the handler will directly transfer
     * execution to the exception handler, by-passing steps 4 and 5 above.
     *
     * @see Trap
     * @see AMD64TrapFrameAccess
     */
    @HOSTED_ONLY
    public Stub genTrapStub() {
        if (platform().isa == ISA.AMD64) {
            CiRegisterConfig registerConfig = registerConfigs.trapStub;
            AMD64MacroAssembler asm = new AMD64MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            CiRegister latch = AMD64SafepointPoll.LATCH_REGISTER;
            CiRegister scratch = registerConfig.getScratchRegister();
            int frameSize = platform().target.alignFrameSize(csl.size);
            int frameToCSA = csl.frameOffsetToCSA;
            CiKind[] handleTrapParameters = CiUtil.signatureToKinds(Trap.handleTrap.classMethodActor);
            CiValue[] args = registerConfig.getCallingConvention(JavaCallee, handleTrapParameters, target(), false).locations;

            // the very first instruction must save the flags.
            // we save them twice and overwrite the first copy with the trap instruction/return address.
            asm.pushfq();
            asm.pushfq();

            // now allocate the frame for this method (first word of which was allocated by the second pushfq above)
            asm.subq(AMD64.rsp, frameSize - 8);

            // save all the callee save registers
            asm.save(csl, frameToCSA);

            // Now that we have saved all general purpose registers (including the scratch register),
            // store the value of the latch register from the thread locals into the trap frame
            asm.movq(scratch, new CiAddress(WordUtil.archKind(), latch.asValue(), TRAP_LATCH_REGISTER.offset));
            asm.movq(new CiAddress(WordUtil.archKind(), AMD64.rsp.asValue(), frameToCSA + csl.offsetOf(latch)), scratch);

            // write the return address pointer to the end of the frame
            asm.movq(scratch, new CiAddress(WordUtil.archKind(), latch.asValue(), TRAP_INSTRUCTION_POINTER.offset));
            asm.movq(new CiAddress(WordUtil.archKind(), AMD64.rsp.asValue(), frameSize), scratch);

            // load the trap number from the thread locals into the first parameter register
            asm.movq(args[0].asRegister(), new CiAddress(WordUtil.archKind(), latch.asValue(), TRAP_NUMBER.offset));
            // also save the trap number into the trap frame
            asm.movq(new CiAddress(WordUtil.archKind(), AMD64.rsp.asValue(), frameToCSA + AMD64TrapFrameAccess.TRAP_NUMBER_OFFSET), args[0].asRegister());
            // load the trap frame pointer into the second parameter register
            asm.leaq(args[1].asRegister(), new CiAddress(WordUtil.archKind(), AMD64.rsp.asValue(), frameToCSA));
            // load the fault address from the thread locals into the third parameter register
            asm.movq(args[2].asRegister(), new CiAddress(WordUtil.archKind(), latch.asValue(), TRAP_FAULT_ADDRESS.offset));

            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = Trap.handleTrap.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;
            asm.restore(csl, frameToCSA);

            // now pop the flags register off the stack before returning
            asm.addq(AMD64.rsp, frameSize - 8);
            asm.popfq();
            asm.ret(0);

            byte[] code = asm.codeBuffer.close(true);

            return new Stub(TrapStub, "trapStub", frameSize, code, callPos, callSize, callee, -1);
        } else if (platform().isa == ISA.ARM) {
            CiRegisterConfig registerConfig = registerConfigs.trapStub;
            ARMV7MacroAssembler asm = new ARMV7MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            CiRegister latch = ARMSafepointPoll.LATCH_REGISTER;
            int frameSize = platform().target.alignFrameSize(csl.size);
            int frameToCSA = csl.frameOffsetToCSA;
            CiKind[] handleTrapParameters = CiUtil.signatureToKinds(Trap.handleTrap.classMethodActor);
            CiValue[] args = registerConfig.getCallingConvention(JavaCallee, handleTrapParameters, target(), false).locations;

            asm.push(ARMV7Assembler.ConditionFlag.Always, 1 << 12, true);
            asm.push(ARMV7Assembler.ConditionFlag.Always, 1 << 12, true);
            asm.mrsReadAPSR(ARMV7Assembler.ConditionFlag.Always, asm.scratchRegister);
            asm.push(ARMV7Assembler.ConditionFlag.Always, 1 << 12, true);

            asm.subq(ARMV7.rsp, frameSize - 8);
            asm.save(csl, frameToCSA);

            // Now that we have saved all general purpose registers (including the scratch register),
            // store the value of the latch register from the thread locals into the trap frame
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), latch.asValue(), TRAP_LATCH_REGISTER.offset));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, asm.scratchRegister, 0);
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP, frameToCSA + csl.offsetOf(latch)));
            asm.str(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, asm.scratchRegister, 0);
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), latch.asValue(), TRAP_INSTRUCTION_POINTER.offset));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, asm.scratchRegister, 0);
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP, frameSize));
            asm.str(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, asm.scratchRegister, 0);

            // load the trap number from the thread locals into the first parameter register
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), latch.asValue(), TRAP_NUMBER.offset));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, args[0].asRegister(), asm.scratchRegister, 0);

            // also save the trap number into the trap frame
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP, frameToCSA + ARMTrapFrameAccess.TRAP_NUMBER_OFFSET));
            asm.str(ARMV7Assembler.ConditionFlag.Always, args[0].asRegister(), asm.scratchRegister, 0);

            // load the trap frame pointer into the second parameter register
            asm.leaq(args[1].asRegister(), new CiAddress(WordUtil.archKind(), ARMV7.RSP, frameToCSA));

            // load the fault address from the thread locals into the third parameter register
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), latch.asValue(), TRAP_FAULT_ADDRESS.offset));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, args[2].asRegister(), asm.scratchRegister, 0);

            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = Trap.handleTrap.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;
            asm.restore(csl, frameToCSA);

            asm.addq(ARMV7.rsp, frameSize - 8);
            asm.pop(ARMV7Assembler.ConditionFlag.Always, 1 << 12, true);
            asm.msrWriteAPSR(ARMV7Assembler.ConditionFlag.Always, asm.scratchRegister);
            asm.pop(ARMV7Assembler.ConditionFlag.Always, 1 << 12, true);
            asm.clearex();
            asm.ret(0);
            asm.insertForeverLoop();
            byte[] code = asm.codeBuffer.close(true);
            return new Stub(TrapStub, "trapStub", frameSize, code, callPos, callSize, callee, -1);
        } else if (platform().isa == ISA.Aarch64) {
            CiRegisterConfig registerConfig = registerConfigs.trapStub;
            Aarch64MacroAssembler asm = new Aarch64MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            CiRegister latch = Aarch64SafepointPoll.LATCH_REGISTER;
            int frameSize = platform().target.alignFrameSize(csl.size);
            int frameToCSA = csl.frameOffsetToCSA;
            CiKind[] handleTrapParameters = CiUtil.signatureToKinds(Trap.handleTrap.classMethodActor);
            CiValue[] args = registerConfig.getCallingConvention(JavaCallee, handleTrapParameters, target(), false).locations;

            // the very first instruction must save the flags.
            // we save them twice and overwrite the first copy with the trap instruction/return address.
            asm.push(registerConfig.getScratchRegister());
            asm.push(registerConfig.getScratchRegister());

            // now allocate the frame for this method (first word of which was allocated by the second push above)
            asm.sub(64, Aarch64.sp, Aarch64.sp, frameSize - platform().target.stackAlignment);

            // save all the callee save registers
            asm.save(csl, frameToCSA);

            // Now that we have saved all general purpose registers (including the scratch register),
            // store the value of the latch register from the thread locals into the trap frame
            asm.ldr(64, registerConfig.getScratchRegister(), Aarch64Address.createUnscaledImmediateAddress(latch, TRAP_LATCH_REGISTER.offset));
            asm.str(64, registerConfig.getScratchRegister(), Aarch64Address.createUnscaledImmediateAddress(Aarch64.sp, frameToCSA + csl.offsetOf(latch)));

            // write the return address pointer to the end of the frame
            asm.ldr(64, registerConfig.getScratchRegister1(), Aarch64Address.createUnscaledImmediateAddress(latch, TRAP_INSTRUCTION_POINTER.offset));
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), Aarch64.rsp, frameSize));
            asm.str(64, registerConfig.getScratchRegister1(), Aarch64Address.createBaseRegisterOnlyAddress(registerConfig.getScratchRegister()));

            // load the trap number from the thread locals into the first parameter register
            asm.ldr(64, args[0].asRegister(), Aarch64Address.createUnscaledImmediateAddress(latch, TRAP_NUMBER.offset));
            // also save the trap number into the trap frame
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), Aarch64.rsp, frameToCSA + Aarch64TrapFrameAccess.TRAP_NUMBER_OFFSET));
            asm.str(64, args[0].asRegister(), Aarch64Address.createBaseRegisterOnlyAddress(registerConfig.getScratchRegister()));
            // load the trap frame pointer into the second parameter register
            asm.leaq(args[1].asRegister(), new CiAddress(WordUtil.archKind(), Aarch64.rsp, frameToCSA));
            // load the fault address from the thread locals into the third parameter register
            asm.ldr(64, args[2].asRegister(), Aarch64Address.createUnscaledImmediateAddress(latch, TRAP_FAULT_ADDRESS.offset));

            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = Trap.handleTrap.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;
            asm.restore(csl, frameToCSA);

            // now pop the flags register off the stack before returning
            asm.add(64, Aarch64.sp, Aarch64.sp, frameSize - platform().target.stackAlignment);
            asm.pop(registerConfig.getScratchRegister());
            asm.ret();

            byte[] code = asm.codeBuffer.close(true);
            return new Stub(TrapStub, "trapStub", frameSize, code, callPos, callSize, callee, -1);
        } else if (platform().isa == ISA.RISCV64) {

            CiRegisterConfig registerConfig = registerConfigs.trapStub;
            RISCV64MacroAssembler asm = new RISCV64MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            CiRegister latch = RISCV64SafepointPoll.LATCH_REGISTER;
            int frameSize = platform().target.alignFrameSize(csl.size);
            int frameToCSA = csl.frameOffsetToCSA;
            CiKind[] handleTrapParameters = CiUtil.signatureToKinds(Trap.handleTrap.classMethodActor);
            CiValue[] args = registerConfig.getCallingConvention(JavaCallee, handleTrapParameters, target(), false).locations;

            // the very first instruction must save the flags.
            // we save them twice and overwrite the first copy with the trap instruction/return address.
            asm.push(64, registerConfig.getScratchRegister());
            asm.push(64, registerConfig.getScratchRegister());

            // now allocate the frame for this method (first word of which was allocated by the second push above)
            asm.subi(RISCV64.sp, RISCV64.sp, frameSize - platform().target.stackAlignment);

            // save all the callee save registers
            asm.save(csl, frameToCSA);

            // Now that we have saved all general purpose registers (including the scratch register),
            // store the value of the latch register from the thread locals into the trap frame
            asm.ldru(64, registerConfig.getScratchRegister(), RISCV64Address.createImmediateAddress(latch, TRAP_LATCH_REGISTER.offset));
            asm.str(64, registerConfig.getScratchRegister(), RISCV64Address.createImmediateAddress(RISCV64.sp, frameToCSA + csl.offsetOf(latch)));

            // write the return address pointer to the end of the frame
            asm.ldru(64, registerConfig.getScratchRegister1(), RISCV64Address.createImmediateAddress(latch, TRAP_INSTRUCTION_POINTER.offset));
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), RISCV64.rsp, frameSize));
            asm.str(64, registerConfig.getScratchRegister1(), RISCV64Address.createBaseRegisterOnlyAddress(registerConfig.getScratchRegister()));

            // load the trap number from the thread locals into the first parameter register
            asm.ldru(64, args[0].asRegister(), RISCV64Address.createImmediateAddress(latch, TRAP_NUMBER.offset));
            // also save the trap number into the trap frame
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), RISCV64.rsp, frameToCSA + RISCV64TrapFrameAccess.TRAP_NUMBER_OFFSET));
            asm.str(64, args[0].asRegister(), RISCV64Address.createBaseRegisterOnlyAddress(registerConfig.getScratchRegister()));
            // load the trap frame pointer into the second parameter register
            asm.leaq(args[1].asRegister(), new CiAddress(WordUtil.archKind(), RISCV64.rsp, frameToCSA));
            // load the fault address from the thread locals into the third parameter register
            asm.ldru(64, args[2].asRegister(), RISCV64Address.createImmediateAddress(latch, TRAP_FAULT_ADDRESS.offset));

            asm.alignForPatchableDirectCall(asm.codeBuffer.position());
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = Trap.handleTrap.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;
            asm.restore(csl, frameToCSA);

            // now pop the flags register off the stack before returning
            asm.addi(RISCV64.sp, RISCV64.sp, frameSize - platform().target.stackAlignment);
            asm.pop(64, registerConfig.getScratchRegister(), true);
            asm.ret();

            byte[] code = asm.codeBuffer.close(true);
            return new Stub(TrapStub, "trapStub", frameSize, code, callPos, callSize, callee, -1);
        } else {
            throw FatalError.unimplemented("com.sun.max.vm.compiler.target.Stubs.genTrapStub");
        }
    }

    /**
     * Unwinds the current thread execution state to a given (caller) frame and instruction pointer. The frame must be
     * an existing caller frame on the stack and the instruction pointer must be a valid address within the code
     * associated with the frame.
     * <p/>
     * The variants of this method further below also setup the register holding a return value
     */
    @NEVER_INLINE
    public static void unwind(Address ip, Pointer sp, Pointer fp) {
        // This is a placeholder method so that the unwind stub (which is generated by genUnwind)
        // can be called via a normal method call.
        FatalError.unexpected("stub should be overwritten");
    }

    @NEVER_INLINE
    public static void unwindObject(Address ip, Pointer sp, Pointer fp, Object returnValue) {
        FatalError.unexpected("stub should be overwritten");
    }

    @NEVER_INLINE
    public static void unwindInt(Address ip, Pointer sp, Pointer fp, int returnValue) {
        FatalError.unexpected("stub should be overwritten");
    }

    @NEVER_INLINE
    public static void unwindLong(Address ip, Pointer sp, Pointer fp, long returnValue) {
        FatalError.unexpected("stub should be overwritten");
    }

    @NEVER_INLINE
    public static void unwindFloat(Address ip, Pointer sp, Pointer fp, float returnValue) {
        FatalError.unexpected("stub should be overwritten");
    }

    @NEVER_INLINE
    public static void unwindDouble(Address ip, Pointer sp, Pointer fp, double returnValue) {
        FatalError.unexpected("stub should be overwritten");
    }

    @HOSTED_ONLY
    private Stub genUnwind(CiValue[] unwindArgs) {
        if (platform().isa == ISA.AMD64) {
            CiRegisterConfig registerConfig = MaxineVM.vm().stubs.registerConfigs.standard;
            AMD64MacroAssembler asm = new AMD64MacroAssembler(target(), registerConfig);
            int frameSize = platform().target.alignFrameSize(0);

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }

            CiValue[] args = unwindArgs;
            assert args.length == 3 || args.length == 4;

            CiRegister pc = args[0].asRegister();
            CiRegister sp = args[1].asRegister();
            CiRegister fp = args[2].asRegister();

            String name = "unwindStub";
            if (args.length == 4) {
                CiValue retValue = args[3];
                CiRegister reg = retValue.asRegister();
                CiKind kind = retValue.kind.stackKind();
                name = "unwind" + kind.name() + "Stub";
                switch (kind) {
                    case Int:
                    case Long:
                    case Object:
                        asm.movq(registerConfig.getReturnRegister(CiKind.Int), reg);
                        break;
                    case Float:
                        asm.movflt(registerConfig.getReturnRegister(CiKind.Float), reg);
                        break;
                    case Double:
                        asm.movdbl(registerConfig.getReturnRegister(CiKind.Double), reg);
                        break;
                    default:
                        FatalError.unexpected("unexpected kind: " + kind);
                }
            }

            // Push 'pc' to the handler's stack frame and update RSP to point to the pushed value.
            // When the RET instruction is executed, the pushed 'pc' will be popped from the stack
            // and the stack will be in the correct state for the handler.
            asm.subq(sp, Word.size());
            asm.movq(new CiAddress(WordUtil.archKind(), sp.asValue()), pc);
            asm.movq(AMD64.rbp, fp);
            asm.movq(AMD64.rsp, sp);
            asm.ret(0);

            byte[] code = asm.codeBuffer.close(true);
            return new Stub(UnwindStub, name, frameSize, code, -1, -1, null, -1);
        } else if (platform().isa == ISA.ARM) {  // TODO (ck): Fix ARM version
            CiRegisterConfig registerConfig = MaxineVM.vm().stubs.registerConfigs.standard;
            ARMV7MacroAssembler asm = new ARMV7MacroAssembler(target(), registerConfig);
            int frameSize = platform().target.alignFrameSize(0);

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }

            // called from Java so we should push the return address register
            asm.push(ARMV7Assembler.ConditionFlag.Always, 1 << 14, true);

            CiValue[] args = unwindArgs;
            assert args.length == 3 || args.length == 4;

            CiRegister pc = args[0].asRegister();
            CiRegister sp = args[1].asRegister();
            CiRegister fp = args[2].asRegister();

            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, asm.scratchRegister, pc);
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r8, sp);
            String name = "unwindStub";
            if (args.length == 4) {
                CiValue retValue = args[3];
                CiRegister reg = null;
                CiAddress stackAddr = null;
                CiKind kind = retValue.kind.stackKind();
                name = "unwind" + kind.name() + "Stub";
                switch (kind) {
                    case Long:
                        stackAddr = new CiAddress(kind, ARMV7.RSP, ((CiStackSlot) retValue).index() * Word.size());
                        asm.setUpRegister(asm.scratchRegister, stackAddr);
                        asm.ldrd(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, asm.scratchRegister, 0);
                        break;
                    case Int:
                    case Object:
                        assert args[3].isRegister() : " genUnwind args[3] is not a register?";
                        break;
                    case Float:
                        reg = retValue.asRegister();
                        asm.movflt(registerConfig.getReturnRegister(CiKind.Float), reg, CiKind.Float, CiKind.Float);
                        break;
                    case Double:
                        reg = retValue.asRegister();
                        asm.movflt(registerConfig.getReturnRegister(CiKind.Double), reg, CiKind.Double, CiKind.Double);
                        break;

                    default:
                        FatalError.unexpected("unexpected kind: " + kind);
                }
            }
            // we have a problem with where to put the return value.
            // we need to clear space first, and finish using the registers

            // Push 'pc' to the handler's stack frame and update RSP to point to the pushed value.
            // When the RET instruction is executed, the pushed 'pc' will be popped from the stack
            // and the stack will be in the correct state for the handler.
            asm.subq(sp, Word.size());
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), sp.asValue()));
            asm.str(ARMV7Assembler.ConditionFlag.Always, pc, asm.scratchRegister, 0);
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r11, fp);
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.rsp, sp);

            if (args.length == 4) {
                CiValue retValue = args[3];
                CiKind kind = retValue.kind.stackKind();
                switch (kind) {
                    case Long:
                        asm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r0, ARMV7.r8);
                        asm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r1, ARMV7.r9);
                        break;
                    case Int:
                    case Object:
                        asm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r0, args[3].asRegister());
                        break;
                    case Float:
                    case Double:
                        // nothing to do as already in the correct register
                        break;
                    default:
                        FatalError.unexpected("unexpected kind: " + kind);
                }
            }

            asm.ret(0);
            byte[] code = asm.codeBuffer.close(true);
            return new Stub(UnwindStub, name, frameSize, code, -1, -1, null, -1);
        } else if (platform().isa == ISA.Aarch64) {
            CiRegisterConfig registerConfig = MaxineVM.vm().stubs.registerConfigs.standard;
            Aarch64MacroAssembler asm = new Aarch64MacroAssembler(target(), registerConfig);
            int frameSize = platform().target.alignFrameSize(0);

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }

            CiValue[] args = unwindArgs;
            assert args.length == 3 || args.length == 4;

            CiRegister pc = args[0].asRegister();
            CiRegister sp = args[1].asRegister();
            CiRegister fp = args[2].asRegister();

            String name = "unwindStub";
            if (args.length == 4) {
                CiValue retValue = args[3];
                CiRegister reg = retValue.asRegister();
                CiKind kind = retValue.kind.stackKind();
                name = "unwind" + kind.name() + "Stub";
                switch (kind) {
                    case Int:
                    case Long:
                    case Object:
                        // PC is kept in x0 which happens to be the return register as well, thus we need to move its
                        // value to the scratch register and then jump to it
                        asm.mov(64, asm.scratchRegister, pc);
                        pc = asm.scratchRegister;
                        asm.mov(64, registerConfig.getReturnRegister(CiKind.Int), reg);
                        break;
                    case Float:
                        asm.fmov(32, registerConfig.getReturnRegister(CiKind.Float), reg);
                        break;
                    case Double:
                        asm.fmov(64, registerConfig.getReturnRegister(CiKind.Double), reg);
                        break;
                    default:
                        FatalError.unexpected("unexpected kind: " + kind);
                }
            }

            asm.mov(64, Aarch64.fp, fp);
            asm.mov(64, Aarch64.sp, sp);
            asm.jmp(pc);

            byte[] code = asm.codeBuffer.close(true);
            return new Stub(UnwindStub, name, frameSize, code, -1, -1, null, -1);
        } else if (platform().isa == ISA.RISCV64) {
            CiRegisterConfig registerConfig = MaxineVM.vm().stubs.registerConfigs.standard;
            RISCV64MacroAssembler asm = new RISCV64MacroAssembler(target(), registerConfig);
            int frameSize = platform().target.alignFrameSize(0);

            // TODO INFINITE LOOP
            asm.jal(RISCV64.zero, 0);

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }

            CiValue[] args = unwindArgs;
            assert args.length == 3 || args.length == 4;

            CiRegister pc = args[0].asRegister();
            CiRegister sp = args[1].asRegister();
            CiRegister fp = args[2].asRegister();

            String name = "unwindStub";
            if (args.length == 4) {
                CiValue retValue = args[3];
                CiRegister reg = retValue.asRegister();
                CiKind kind = retValue.kind.stackKind();
                name = "unwind" + kind.name() + "Stub";
                switch (kind) {
                    case Int:
                    case Long:
                    case Object:
                        // PC is kept in x0 which happens to be the return register as well, thus we need to move its
                        // value to the scratch register and then jump to it
                        asm.mov(asm.scratchRegister, pc);
                        pc = asm.scratchRegister;
                        asm.mov(registerConfig.getReturnRegister(CiKind.Int), reg);
                        break;
                    case Float:
                        asm.fmov(32, registerConfig.getReturnRegister(CiKind.Float), reg);
                        break;
                    case Double:
                        asm.fmov(64, registerConfig.getReturnRegister(CiKind.Double), reg);
                        break;
                    default:
                        FatalError.unexpected("unexpected kind: " + kind);
                }
            }

            asm.mov(RISCV64.fp, fp);
            asm.mov(RISCV64.sp, sp);
            asm.jalr(RISCV64.zero, pc, 0);

            byte[] code = asm.codeBuffer.close(true);
            return new Stub(UnwindStub, name, frameSize, code, -1, -1, null, -1);
        } else {
            throw FatalError.unimplemented("com.sun.max.vm.compiler.target.Stubs.genUnwind");
        }
    }

    /**
     * Expands the stack by a given amount and then calls {@link Deoptimization#unroll(Info)}. The stack expansion is
     * required to fit the deoptimized frames encoded in {@code info}.
     *
     * @param info the argument to pass onto {@link Deoptimization#unroll(Info)}
     * @param frameSize the amount by which the stack should be expanded (must be >= 0)
     */
    @NEVER_INLINE
    public static void unroll(Info info, int frameSize) {
        FatalError.unexpected("stub should be overwritten");
    }

    // TODO: Is ARM version working?
    @HOSTED_ONLY
    private Stub genUnroll(CiValue[] unrollArgs) {
        if (platform().isa == ISA.AMD64) {
            CiRegisterConfig registerConfig = MaxineVM.vm().stubs.registerConfigs.standard;
            AMD64MacroAssembler asm = new AMD64MacroAssembler(target(), registerConfig);
            int frameSize = platform().target.alignFrameSize(0);

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }

            asm.subq(AMD64.rsp, AMD64.rsi);

            CriticalMethod unroll = new CriticalMethod(Deoptimization.class, "unroll", null);
            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = unroll.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // Should never reach here
            asm.hlt();

            byte[] code = asm.codeBuffer.close(true);
            return new Stub(UnrollStub, "unrollStub", frameSize, code, callPos, callSize, callee, -1);
        } else if (platform().isa == ISA.ARM) {
            CiRegisterConfig registerConfig = MaxineVM.vm().stubs.registerConfigs.standard;
            ARMV7MacroAssembler asm = new ARMV7MacroAssembler(target(), registerConfig);
            int frameSize = platform().target.alignFrameSize(0);

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }
            // We are called from Java so we do need to push the LR.
            asm.push(ARMV7Assembler.ConditionFlag.Always, 1 << 14, true);
            asm.sub(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.rsp, ARMV7.rsp, ARMV7.r1, 0, 0);

            CriticalMethod unroll = new CriticalMethod(Deoptimization.class, "unroll", null);
            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = unroll.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // should never reach here ...
            asm.movImm32(ConditionFlag.Always, asm.scratchRegister, 0xffffffff);
            asm.mov(ConditionFlag.Always, false, ARMV7.PC, asm.scratchRegister);

            // Should never reach here
            asm.hlt();

            byte[] code = asm.codeBuffer.close(true);
            return new Stub(UnrollStub, "unrollStub", frameSize, code, callPos, callSize, callee, -1);
        } else if (platform().isa == ISA.Aarch64) {
            CiRegisterConfig registerConfig = MaxineVM.vm().stubs.registerConfigs.standard;
            Aarch64MacroAssembler asm = new Aarch64MacroAssembler(target(), registerConfig);
            int frameSize = platform().target.alignFrameSize(0);

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }

            asm.mov(64, asm.scratchRegister, Aarch64.sp);
            asm.sub(64, asm.scratchRegister, asm.scratchRegister, Aarch64.r1);
            asm.mov(64, Aarch64.sp, asm.scratchRegister);

            CriticalMethod unroll = new CriticalMethod(Deoptimization.class, "unroll", null);
            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = unroll.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // Should never reach here
            asm.crashme();
            asm.hlt();

            byte[] code = asm.codeBuffer.close(true);
            return new Stub(UnrollStub, "unrollStub", frameSize, code, callPos, callSize, callee, -1);
        } else if (platform().isa == ISA.RISCV64) {

            CiRegisterConfig registerConfig = MaxineVM.vm().stubs.registerConfigs.standard;
            RISCV64MacroAssembler asm = new RISCV64MacroAssembler(target(), registerConfig);
            int frameSize = platform().target.alignFrameSize(0);

            // TODO INFINITE LOOP
            asm.jal(RISCV64.zero, 0);

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }

            asm.mov(asm.scratchRegister, RISCV64.sp);
            asm.sub(64, asm.scratchRegister, asm.scratchRegister, RISCV64.a2);
            asm.mov(RISCV64.sp, asm.scratchRegister);

            CriticalMethod unroll = new CriticalMethod(Deoptimization.class, "unroll", null);
            asm.alignForPatchableDirectCall(asm.codeBuffer.position());
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = unroll.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // Should never reach here
            asm.crashme();
            asm.hlt();

            byte[] code = asm.codeBuffer.close(true);
            return new Stub(UnrollStub, "unrollStub", frameSize, code, callPos, callSize, callee, -1);
        } else {
            throw FatalError.unimplemented("com.sun.max.vm.compiler.target.Stubs.genUnroll");
        }
    }

    /**
     * Stub initialization that must be done at runtime.
     */
    static abstract class RuntimeInitialization {

        abstract void apply();
    }

    /**
     * Helper to patch the address of a deoptimization runtime routine into a deopt stub. This can only be done at
     * runtime once the address is known and relocated.
     */
    @PLATFORM(cpu = "armv7")
    static class ARMV7DeoptStubPatch extends RuntimeInitialization {

        /**
         * The position of the 64-bit operand to be patched.
         */
        final int pos;

        /**
         * The routine whose relocated address is the patch value.
         */
        final CriticalMethod runtimeRoutine;

        /**
         * The stub whose code is to be patched.
         */
        final Stub stub;

        ARMV7DeoptStubPatch(int pos, CriticalMethod runtimeRoutine, Stub stub) {
            this.pos = pos;
            this.runtimeRoutine = runtimeRoutine;
            this.stub = stub;
        }

        @Override
        void apply() {
            Pointer patchAddr = stub.codeAt(pos).toPointer();
            int disp = runtimeRoutine.address().toInt();
            int instruction = ARMV7Assembler.movwHelper(ConditionFlag.Always, ARMV7.r12, disp & 0xffff);
            patchAddr.writeInt(0, instruction);
            instruction = ARMV7Assembler.movtHelper(ConditionFlag.Always, ARMV7.r12, (disp >> 16) & 0xffff);
            patchAddr.writeInt(4, instruction);
            MaxineVM.maxine_cache_flush(patchAddr, 8);
        }
    }

    /**
     * Helper to patch the address of a deoptimization runtime routine into a deopt stub. This can only be done at
     * runtime once the address is known and relocated.
     */
    @PLATFORM(cpu = "amd64")
    static class AMD64DeoptStubPatch extends RuntimeInitialization {

        /**
         * The position of the 64-bit operand to be patched.
         */
        final int pos;

        /**
         * The routine whose relocated address is the patch value.
         */
        final CriticalMethod runtimeRoutine;

        /**
         * The stub whose code is to be patched.
         */
        final Stub stub;

        AMD64DeoptStubPatch(int pos, CriticalMethod runtimeRoutine, Stub stub) {
            this.pos = pos;
            this.runtimeRoutine = runtimeRoutine;
            this.stub = stub;
        }

        @Override
        void apply() {
            Pointer patchAddr = stub.codeAt(pos).toPointer();
            patchAddr.writeLong(0, runtimeRoutine.address().toLong());
        }
    }

    /**
     * Helper to patch the address of a deoptimization runtime routine into a deopt stub. This can only be done at
     * runtime once the address is known and relocated.
     */
    @PLATFORM(cpu = "aarch64")
    static class Aarch64DeoptStubPatch extends RuntimeInitialization {

        /**
         * The position of the 64-bit operand to be patched.
         */
        final int pos;

        /**
         * The routine whose relocated address is the patch value.
         */
        final CriticalMethod runtimeRoutine;

        /**
         * The stub whose code is to be patched.
         */
        final Stub stub;

        Aarch64DeoptStubPatch(int pos, CriticalMethod runtimeRoutine, Stub stub) {
            this.pos = pos;
            this.runtimeRoutine = runtimeRoutine;
            this.stub = stub;
        }

        @Override
        void apply() {
            Pointer patchAddr = stub.codeAt(pos).toPointer();
            long disp = runtimeRoutine.address().toLong();

            int instruction = Aarch64MacroAssembler.movzHelper(64, Aarch64.r16, (int) disp & 0xffff, 0);
            patchAddr.writeInt(0, instruction);
            if (disp >> 16 != 0) {
                instruction = Aarch64MacroAssembler.movkHelper(64, Aarch64.r16, (int) (disp >> 16) & 0xffff, 16);
                patchAddr.writeInt(4, instruction);
                if (disp >> 32 != 0) {
                    instruction = Aarch64MacroAssembler.movkHelper(64, Aarch64.r16, (int) (disp >> 32) & 0xffff, 32);
                    patchAddr.writeInt(8, instruction);
                    if (disp >> 48 != 0) {
                        instruction = Aarch64MacroAssembler.movkHelper(64, Aarch64.r16, (int) (disp >> 48) & 0xffff, 48);
                        patchAddr.writeInt(12, instruction);
                    }
                }
            }

            MaxineVM.maxine_cache_flush(patchAddr, 16);
        }
    }

    /**
     * Helper to patch the address of a deoptimization runtime routine into a deopt stub. This can only be done at
     * runtime once the address is known and relocated.
     */
    @PLATFORM(cpu = "riscv64")
    static class RISCV64DeoptStubPatch extends RuntimeInitialization {

        /**
         * The position of the 64-bit operand to be patched.
         */
        final int pos;

        /**
         * The routine whose relocated address is the patch value.
         */
        final CriticalMethod runtimeRoutine;

        /**
         * The stub whose code is to be patched.
         */
        final Stub stub;

        RISCV64DeoptStubPatch(int pos, CriticalMethod runtimeRoutine, Stub stub) {
            this.pos = pos;
            this.runtimeRoutine = runtimeRoutine;
            this.stub = stub;
        }

        @Override
        void apply() {
            Pointer patchAddr = stub.codeAt(pos).toPointer();
            long disp = runtimeRoutine.address().toLong();

            // move disp >>> 32 in scratch1
            int ldips32 = (int) (disp >>> 32);
            int luiImmediate = ldips32;
            if ((ldips32 & 0xFFF) >>> 11 != 0b0) {
                luiImmediate = ldips32 - (ldips32 | 0xFFFFF000);
            }
            int instr = RISCV64MacroAssembler.loadUpperImmediateHelper(RISCV64.x29, luiImmediate);
            patchAddr.writeInt(0, instr);

            if (ldips32 > 0) {
                // addiw(dst, dst, ldisp32);
                instr = RISCV64MacroAssembler.addImmediateWordHelper(RISCV64.x29, RISCV64.x29, ldips32);
            } else {
                // addi(dst, dst, ldips32);
                instr = RISCV64MacroAssembler.addImmediateHelper(RISCV64.x29, RISCV64.x29, ldips32);
            }

            patchAddr.writeInt(4, instr);

            int shiftLeftInstr = RISCV64MacroAssembler.shiftLeftLogicImmediateHelper(RISCV64.x29, RISCV64.x29, 32);
            patchAddr.writeInt(8, shiftLeftInstr);

            // move disp & 0xFFFF in scratch
            int rdips32 = (int) disp;
            luiImmediate = rdips32;
            if ((rdips32 & 0xFFF) >>> 11 != 0b0) {
                luiImmediate = rdips32 - (rdips32 | 0xFFFFF000);
            }
            instr = RISCV64MacroAssembler.loadUpperImmediateHelper(RISCV64.x28, luiImmediate);
            patchAddr.writeInt(12, instr);

            if (rdips32 > 0) {
                // addiw(dst, dst, rdips32);
                instr = RISCV64MacroAssembler.addImmediateWordHelper(RISCV64.x28, RISCV64.x28, rdips32);
            } else {
                // addi(dst, dst, rdips32);
                instr = RISCV64MacroAssembler.addImmediateHelper(RISCV64.x28, RISCV64.x28, rdips32);
            }
            patchAddr.writeInt(16, instr);

            int addInstr = RISCV64MacroAssembler.addSubInstructionHelper(RISCV64.x28, RISCV64.x28, RISCV64.x29, false);
            patchAddr.writeInt(20, addInstr);

            MaxineVM.maxine_cache_flush(patchAddr, 24);
        }
    }

    /**
     * Generates a stub to deoptimize a method upon returning to it.
     *
     * @param kind the return value kind
     */
    // TODO: Fix ARM version
    @HOSTED_ONLY
    private Stub genDeoptStub(CiKind kind) {
        if (platform().isa == ISA.AMD64) {
            /*
             * The deopt stub initially executes in the frame of the method that was returned to and is about to be
             * deoptimized. It then allocates a temporary frame of 2 slots to transfer control to the deopt
             * routine by "returning" to it. As execution enters the deopt routine, the stack looks like
             * the about-to-be-deoptimized frame called the deopt routine directly.
             *
             * [ mov  rcx, rax ]                               // if non-void return value, copy it into arg3 (omitted for void/float/double values)
             *   mov  rdi [rsp + DEOPT_RETURN_ADDRESS_OFFSET]  // copy deopt IP into arg0
             *   mov  rsi, rsp                                 // copy deopt SP into arg1
             *   mov  rdx, rbp                                 // copy deopt FP into arg2
             *   subq rsp, 16                                  // allocate 2 slots
             *   mov  [rsp + 8], rdi                           // put deopt IP (i.e. original return address) into first slot
             *   mov  scratch, 0xFFFFFFFFFFFFFFFFL             // put (placeholder) address of deopt ...
             *   mov  [rsp], scratch                           // ... routine into second slot
             *   ret                                           // call deopt method by "returning" to it
             */
            CiRegisterConfig registerConfig = registerConfigs.standard;
            CiCalleeSaveLayout csl = registerConfig.csl;
            AMD64MacroAssembler asm = new AMD64MacroAssembler(target(), registerConfig);
            int frameSize = platform().target.alignFrameSize(csl == null ? 0 : csl.size);

            String runtimeRoutineName = "deoptimize" + kind.name();
            final CriticalMethod runtimeRoutine;
            try {
                runtimeRoutine = new CriticalMethod(Deoptimization.class, runtimeRoutineName, null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
            } catch (NoSuchMethodError e) {
                // No deoptimization stub for kind
                return null;
            }

            CiKind[] params = CiUtil.signatureToKinds(runtimeRoutine.classMethodActor);
            CiValue[] args = registerConfig.getCallingConvention(JavaCall, params, target(), false).locations;
            if (!kind.isVoid()) {
                CiRegister arg4 = args[4].asRegister();
                CiRegister returnRegister = registerConfig.getReturnRegister(kind);
                if (arg4 != returnRegister) {
                    if (kind.isFloat()) {
                        asm.movflt(arg4, returnRegister);
                    } else if (kind.isDouble()) {
                        asm.movdbl(arg4, returnRegister);
                    } else {
                        asm.movq(arg4, returnRegister);
                    }
                }
            }

            // Copy original return address into arg 0 (i.e. 'ip')
            CiRegister arg0 = args[0].asRegister();
            asm.movq(arg0, new CiAddress(WordUtil.archKind(), AMD64.RSP, DEOPT_RETURN_ADDRESS_OFFSET));

            // Copy original stack pointer into arg 1 (i.e. 'sp')
            CiRegister arg1 = args[1].asRegister();
            asm.movq(arg1, AMD64.rsp);

            // Copy original frame pointer into arg 2 (i.e. 'sp')
            CiRegister arg2 = args[2].asRegister();
            asm.movq(arg2, AMD64.rbp);

            // Zero arg 3 (i.e. 'csa')
            CiRegister arg3 = args[3].asRegister();
            asm.xorq(arg3, arg3);

            // Allocate 2 extra stack slots
            asm.subq(AMD64.rsp, 16);

            // Put original return address into high slot
            asm.movq(new CiAddress(WordUtil.archKind(), AMD64.RSP, 8), arg0);

            // Put deopt method entry point into low slot
            CiRegister scratch = registerConfig.getScratchRegister();
            asm.movq(scratch, 0xFFFFFFFFFFFFFFFFL);
            final int patchPos = asm.codeBuffer.position() - 8;
            asm.movq(new CiAddress(WordUtil.archKind(), AMD64.RSP), scratch);

            // "return" to deopt routine
            asm.ret(0);

            String stubName = runtimeRoutineName + "Stub";
            byte[] code = asm.codeBuffer.close(true);
            final Stub stub = new Stub(DeoptStub, stubName, frameSize, code, -1, 0, null, -1);

            AMD64DeoptStubPatch patch = new AMD64DeoptStubPatch(patchPos, runtimeRoutine, stub);
            runtimeInits = Arrays.copyOf(runtimeInits, runtimeInits.length + 1);
            runtimeInits[runtimeInits.length - 1] = patch;

            return stub;
        } else if (platform().isa == ISA.ARM) {
            /*
             * The deopt stub initially executes in the frame of the method that was returned to and is about to be
             * deoptimized. It then allocates a temporary frame of 2 slots to transfer control to the deopt
             * routine by "returning" to it. As execution enters the deopt routine, the stack looks like
             * the about-to-be-deoptimized frame called the deopt routine directly.
             *
             * [ mov  rcx, rax ]                               // if non-void return value, copy it into arg3 (omitted for void/float/double values)
             *   mov  rdi [rsp + DEOPT_RETURN_ADDRESS_OFFSET]  // copy deopt IP into arg0
             *   mov  rsi, rsp                                 // copy deopt SP into arg1
             *   mov  rdx, rbp                                 // copy deopt FP into arg2
             *   subq rsp, 16                                  // allocate 2 slots
             *   mov  [rsp + 8], rdi                           // put deopt IP (i.e. original return address) into first slot
             *   mov  scratch, 0xFFFFFFFFFFFFFFFFL             // put (placeholder) address of deopt ...
             *   mov  [rsp], scratch                           // ... routine into second slot
             *   ret                                           // call deopt method by "returning" to it
             */
            CiRegisterConfig registerConfig = registerConfigs.standard;
            CiCalleeSaveLayout csl = registerConfig.csl;
            ARMV7MacroAssembler asm = new ARMV7MacroAssembler(target(), registerConfig);
            int frameSize = platform().target.alignFrameSize(csl == null ? 0 : csl.size);

            asm.movImm32(ConditionFlag.Always, asm.scratchRegister, 0xdef2def2);

            String runtimeRoutineName = "deoptimize" + kind.name();
            final CriticalMethod runtimeRoutine;
            try {
                runtimeRoutine = new CriticalMethod(Deoptimization.class, runtimeRoutineName, null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
            } catch (NoSuchMethodError e) {
                // No deoptimization stub for kind
                return null;
            }
            CiKind[] params = CiUtil.signatureToKinds(runtimeRoutine.classMethodActor);
            CiValue[] args = registerConfig.getCallingConvention(JavaCall, params, target(), false).locations;
            if (!kind.isVoid()) {
                CiAddress arg4;
                CiRegister returnRegister = registerConfig.getReturnRegister(kind);
                switch (kind) {
                    case Byte:
                    case Boolean:
                    case Short:
                    case Char:
                    case Int:
                    case Object:
                        assert !args[4].isRegister();
                        arg4 = new CiAddress(kind, ARMV7.RSP, ((CiStackSlot) args[4]).index() * 4);
                        asm.setUpScratch(arg4);
                        asm.str(ConditionFlag.Always, returnRegister, asm.scratchRegister, 0);
                        break;
                    case Long:
                        assert !args[4].isRegister();
                        arg4 = new CiAddress(kind, ARMV7.RSP, ((CiStackSlot) args[4]).index() * 4);
                        asm.setUpScratch(arg4);
                        asm.strd(ConditionFlag.Always, returnRegister, asm.scratchRegister, 0);
                        break;
                    case Float:
                    case Double:
                        CiRegister tmp = args[4].asRegister();
                        if (tmp != returnRegister) {
                            asm.movflt(returnRegister, tmp, kind, kind);
                        }
                        break;
                    default:
                        throw new InternalError("Unexpected parameter kind: " + kind);
                }
            }

            // Copy original return address into arg 0 (i.e. 'ip')
            CiRegister arg0 = args[0].asRegister();
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP, DEOPT_RETURN_ADDRESS_OFFSET));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, arg0, asm.scratchRegister, 0);

            // Copy original stack pointer into arg 1 (i.e. 'sp')
            CiRegister arg1 = args[1].asRegister();
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, arg1, ARMV7.rsp);

            // Copy original frame pointer into arg 2 (i.e. 'sp')
            CiRegister arg2 = args[2].asRegister();
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, arg2, ARMV7.FP);

            // Zero arg 3 (i.e. 'csa')
            CiRegister arg3 = args[3].asRegister();
            asm.xorq(arg3, arg3);

            // Allocate 2 extra stack slots ? one in ARM?
            asm.subq(ARMV7.rsp, 8);

            // Put original return address into high slot
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP, 4));
            asm.str(ARMV7Assembler.ConditionFlag.Always, arg0, asm.scratchRegister, 0);

            // Put deopt method entry point into low slot
            asm.movw(ARMV7Assembler.ConditionFlag.Always, asm.scratchRegister, 0xffff);
            final int patchPos = asm.codeBuffer.position() - 4;
            asm.movt(ARMV7Assembler.ConditionFlag.Always, asm.scratchRegister, 0xffff);
            asm.setUpRegister(ARMV7.r8, new CiAddress(WordUtil.archKind(), ARMV7.RSP));
            asm.str(ARMV7Assembler.ConditionFlag.Always, asm.scratchRegister, ARMV7.r8, 0);

            asm.movImm32(ConditionFlag.Always, asm.scratchRegister, 0xfeeff00f);
            asm.ret(0);

            String stubName = runtimeRoutineName + "Stub";
            byte[] code = asm.codeBuffer.close(true);
            final Stub stub = new Stub(DeoptStub, stubName, frameSize, code, -1, 0, null, -1);

            ARMV7DeoptStubPatch patch = new ARMV7DeoptStubPatch(patchPos, runtimeRoutine, stub);
            runtimeInits = Arrays.copyOf(runtimeInits, runtimeInits.length + 1);
            runtimeInits[runtimeInits.length - 1] = patch;

            return stub;
        } else if (platform().isa == ISA.Aarch64) {
            CiRegisterConfig registerConfig = registerConfigs.standard;
            CiCalleeSaveLayout csl = registerConfig.csl;
            Aarch64MacroAssembler asm = new Aarch64MacroAssembler(target(), registerConfig);
            int frameSize = platform().target.alignFrameSize(csl == null ? 0 : csl.size);

            String runtimeRoutineName = "deoptimize" + kind.name();
            final CriticalMethod runtimeRoutine;
            try {
                runtimeRoutine = new CriticalMethod(Deoptimization.class, runtimeRoutineName, null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
            } catch (NoSuchMethodError e) {
                // No deoptimization stub for kind
                return null;
            }
            CiKind[] params = CiUtil.signatureToKinds(runtimeRoutine.classMethodActor);
            CiValue[] args = registerConfig.getCallingConvention(JavaCall, params, target(), false).locations;
            if (!kind.isVoid()) {
                CiRegister arg4 = args[4].asRegister();
                CiRegister returnRegister = registerConfig.getReturnRegister(kind);
                if (arg4 != returnRegister) {
                    if (kind.isFloat()) {
                        asm.fmov(32, arg4, returnRegister);
                    } else if (kind.isDouble()) {
                        asm.fmov(64, arg4, returnRegister);
                    } else {
                        asm.mov(64, arg4, returnRegister);
                    }
                }
            }

            // Copy original return address into arg 0 (i.e. 'ip')
            CiRegister arg0 = args[0].asRegister();
            asm.ldr(64, arg0, Aarch64Address.createUnscaledImmediateAddress(Aarch64.sp, DEOPT_RETURN_ADDRESS_OFFSET));

            // Copy original stack pointer into arg 1 (i.e. 'sp')
            CiRegister arg1 = args[1].asRegister();
            asm.mov(64, arg1, Aarch64.sp);

            // Copy original frame pointer into arg 2 (i.e. 'fp')
            CiRegister arg2 = args[2].asRegister();
            asm.mov(64, arg2, Aarch64.fp);

            // Zero arg 3 (i.e. 'csa')
            CiRegister arg3 = args[3].asRegister();
            asm.mov(arg3, 0);

            // Put deopt method entry point into scratch register
            CiRegister scratch = registerConfig.getScratchRegister();
            final int patchPos = asm.codeBuffer.position();
            asm.nop(4); // This will be patched by com.sun.max.vm.compiler.target.Stubs.Aarch64DeoptStubPatch.apply
            asm.ret(scratch);

            String stubName = runtimeRoutineName + "Stub";
            byte[] code = asm.codeBuffer.close(true);
            final Stub stub = new Stub(DeoptStub, stubName, frameSize, code, -1, 0, null, -1);

            Aarch64DeoptStubPatch patch = new Aarch64DeoptStubPatch(patchPos, runtimeRoutine, stub);
            runtimeInits = Arrays.copyOf(runtimeInits, runtimeInits.length + 1);
            runtimeInits[runtimeInits.length - 1] = patch;

            return stub;
        } else if (platform().isa == ISA.RISCV64) {
            CiRegisterConfig registerConfig = registerConfigs.standard;
            CiCalleeSaveLayout csl = registerConfig.csl;
            RISCV64MacroAssembler asm = new RISCV64MacroAssembler(target(), registerConfig);
            int frameSize = platform().target.alignFrameSize(csl == null ? 0 : csl.size);

            // TODO INFINITE LOOP
            asm.jal(RISCV64.zero, 0);

            String runtimeRoutineName = "deoptimize" + kind.name();
            final CriticalMethod runtimeRoutine;
            try {
                runtimeRoutine = new CriticalMethod(Deoptimization.class, runtimeRoutineName, null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
            } catch (NoSuchMethodError e) {
                // No deoptimization stub for kind
                return null;
            }
            CiKind[] params = CiUtil.signatureToKinds(runtimeRoutine.classMethodActor);
            CiValue[] args = registerConfig.getCallingConvention(JavaCall, params, target(), false).locations;
            if (!kind.isVoid()) {
                CiRegister arg4 = args[4].asRegister();
                CiRegister returnRegister = registerConfig.getReturnRegister(kind);
                if (arg4 != returnRegister) {
                    if (kind.isFloat()) {
                        asm.fmov(32, arg4, returnRegister);
                    } else if (kind.isDouble()) {
                        asm.fmov(64, arg4, returnRegister);
                    } else {
                        asm.mov(arg4, returnRegister);
                    }
                }
            }

            // Copy original return address into arg 0 (i.e. 'ip')
            CiRegister arg0 = args[0].asRegister();
            asm.ldru(64, arg0, RISCV64Address.createImmediateAddress(RISCV64.sp, DEOPT_RETURN_ADDRESS_OFFSET));

            // Copy original stack pointer into arg 1 (i.e. 'sp')
            CiRegister arg1 = args[1].asRegister();
            asm.mov(arg1, RISCV64.sp);

            // Copy original frame pointer into arg 2 (i.e. 'fp')
            CiRegister arg2 = args[2].asRegister();
            asm.mov(arg2, RISCV64.fp);

            // Zero arg 3 (i.e. 'csa')
            CiRegister arg3 = args[3].asRegister();
            asm.mov(arg3, 0);

            // Put deopt method entry point into scratch register
            CiRegister scratch = registerConfig.getScratchRegister();
            final int patchPos = asm.codeBuffer.position();
            asm.nop(10); // This will be patched by com.sun.max.vm.compiler.target.Stubs.RISCV64DeoptStubPatch.apply
            asm.ret(scratch);

            String stubName = runtimeRoutineName + "Stub";
            byte[] code = asm.codeBuffer.close(true);
            final Stub stub = new Stub(DeoptStub, stubName, frameSize, code, -1, 0, null, -1);

            RISCV64DeoptStubPatch patch = new RISCV64DeoptStubPatch(patchPos, runtimeRoutine, stub);
            runtimeInits = Arrays.copyOf(runtimeInits, runtimeInits.length + 1);
            runtimeInits[runtimeInits.length - 1] = patch;

            return stub;
        } else {
            throw FatalError.unimplemented("com.sun.max.vm.compiler.target.Stubs.genDeoptStub");
        }
    }

    /**
     * Generates a stub to deoptimize an method upon returning to it. This stub creates a new frame for saving the
     * registers specified by the {@link CiCalleeSaveLayout} of a given register configuration.
     *
     * @param kind the return value kind or {@code null} if generating the stub used when returning from a safepoint
     *            trap
     * @param returnValueOnStack specifies if the return value is on the stack (ignored if {@code kind == null})
     */
    // TODO: Fix ARM version
    @HOSTED_ONLY
    private Stub genDeoptStubWithCSA(CiKind kind, CiRegisterConfig registerConfig, boolean returnValueOnStack) {
        if (platform().isa == ISA.AMD64) {
            /*
             * The deopt stub initially executes in the frame of the method that was returned to (i.e. the method about to be
             * deoptimized). It then allocates a new frame, saves all registers, sets up args to deopt routine
             * and calls it.
             *
             *   subq rsp <frame size>                         // allocate frame
             *   mov  [rsp], rax                               // save ...
             *   mov  [rsp + 8], rcx                           //   all ...
             *   ...                                           //     the ...
             *   movq [rsp + 248], xmm15                       //       registers
             * { mov  rdx/xmm0, [rsp + <cfo> + 8] }            // if non-void return value, copy it from stack into arg4 (or xmm0)
             *   mov  rdi  [rsp + <cfo> + DEOPT_RETURN_ADDRESS_OFFSET]  // copy deopt IP into arg0
             *   lea  rsi, [rsp + <cfo>]                       // copy deopt SP into arg1
             *   mov  rdx, rbp                                 // copy deopt FP into arg2
             *   mov  rcx, rbp                                 // copy callee save area into arg3
             *   mov  [rsp + <frame size>], rdi                // restore deopt IP (i.e. original return address) into return address slot
             *   call <deopt routine>                          // call deoptimization routine
             *   brk                                           // should not reach here
             */
            CiCalleeSaveLayout csl = registerConfig.csl;
            AMD64MacroAssembler asm = new AMD64MacroAssembler(target(), registerConfig);
            int frameSize = platform().target.alignFrameSize(csl.size);
            int cfo = frameSize + 8; // Caller frame offset

            String runtimeRoutineName;
            if (kind == null) {
                runtimeRoutineName = "deoptimizeAtSafepoint";
            } else {
                runtimeRoutineName = "deoptimize" + kind.name();
            }
            final CriticalMethod runtimeRoutine;
            try {
                runtimeRoutine = new CriticalMethod(Deoptimization.class, runtimeRoutineName, null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
            } catch (NoSuchMethodError e) {
                // No deoptimization stub for kind
                return null;
            }

            // now allocate the frame for this method (including return address slot)
            asm.subq(AMD64.rsp, frameSize + 8);

            // save all the callee save registers
            asm.save(csl, csl.frameOffsetToCSA);

            CiKind[] params = CiUtil.signatureToKinds(runtimeRoutine.classMethodActor);
            CiValue[] args = registerConfig.getCallingConvention(JavaCall, params, target(), false).locations;
            if (kind != null && !kind.isVoid()) {
                // Copy return value into arg 4

                CiRegister arg4 = args[4].asRegister();
                CiStackSlot ss = (CiStackSlot) registerConfigs.compilerStub.getCallingConvention(JavaCall, new CiKind[] {kind}, target(), true).locations[0];
                assert ss.index() == 1 : "compiler stub return value slot index has changed?";
                CiAddress src = new CiAddress(kind, AMD64.RSP, cfo + (ss.index() * 8));
                if (kind.isFloat()) {
                    asm.movflt(arg4, src);
                } else if (kind.isDouble()) {
                    asm.movdbl(arg4, src);
                } else {
                    asm.movq(arg4, src);
                }
            }

            // Copy original return address into arg 0 (i.e. 'ip')
            CiRegister arg0 = args[0].asRegister();
            asm.movq(arg0, new CiAddress(WordUtil.archKind(), AMD64.RSP, cfo + DEOPT_RETURN_ADDRESS_OFFSET));

            // Copy original stack pointer into arg 1 (i.e. 'sp')
            CiRegister arg1 = args[1].asRegister();
            asm.leaq(arg1, new CiAddress(WordUtil.archKind(), AMD64.RSP, cfo));

            // Copy original frame pointer into arg 2 (i.e. 'sp')
            CiRegister arg2 = args[2].asRegister();
            asm.movq(arg2, AMD64.rbp);

            // Copy callee save area into arg3 (i.e. 'csa')
            CiRegister arg3 = args[3].asRegister();
            asm.movq(arg3, AMD64.rsp);

            // Patch return address of deopt stub frame to look
            // like it was called by frame being deopt'ed.
            asm.movq(new CiAddress(WordUtil.archKind(), AMD64.RSP, frameSize), arg0);

            // Call runtime routine
            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // should never reach here
            asm.int3();

            String stubName = runtimeRoutineName + "StubWithCSA";
            byte[] code = asm.codeBuffer.close(true);
            Type stubType = kind == null ? DeoptStubFromSafepoint : DeoptStubFromCompilerStub;
            return new Stub(stubType, stubName, frameSize, code, callPos, callSize, runtimeRoutine.classMethodActor, -1);
        } else if (platform().isa == ISA.ARM) {
            /*
             * The deopt stub initially executes in the frame of the method that was returned to (i.e. the method about to be
             * deoptimized). It then allocates a new frame, saves all registers, sets up args to deopt routine
             * and calls it.
             *
             *   subq rsp <frame size>                         // allocate frame
             *   mov  [rsp], rax                               // save ...
             *   mov  [rsp + 8], rcx                           //   all ...
             *   ...                                           //     the ...
             *   movq [rsp + 248], xmm15                       //       registers
             * { mov  rdx/xmm0, [rsp + <cfo> + 8] }            // if non-void return value, copy it from stack into arg4 (or xmm0)
             *   mov  rdi  [rsp + <cfo> + DEOPT_RETURN_ADDRESS_OFFSET]  // copy deopt IP into arg0
             *   lea  rsi, [rsp + <cfo>]                       // copy deopt SP into arg1
             *   mov  rdx, rbp                                 // copy deopt FP into arg2
             *   mov  rcx, rbp                                 // copy callee save area into arg3
             *   mov  [rsp + <frame size>], rdi                // restore deopt IP (i.e. original return address) into return address slot
             *   call <deopt routine>                          // call deoptimization routine
             *   brk                                           // should not reach here
             */
            CiCalleeSaveLayout csl = registerConfig.csl;
            ARMV7MacroAssembler asm = new ARMV7MacroAssembler(target(), registerConfig);
            int frameSize = platform().target.alignFrameSize(csl.size);
            int cfo = frameSize + 4;

            asm.movImm32(ConditionFlag.Always, asm.scratchRegister, 0xdef1def1);
            String runtimeRoutineName;
            if (kind == null) {
                runtimeRoutineName = "deoptimizeAtSafepoint";
            } else {
                runtimeRoutineName = "deoptimize" + kind.name();
            }
            final CriticalMethod runtimeRoutine;
            try {
                runtimeRoutine = new CriticalMethod(Deoptimization.class, runtimeRoutineName, null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
            } catch (NoSuchMethodError e) {
                // No deoptimization stub for kind
                return null;
            }

            // now allocate the frame for this method (including return address slot)
            asm.subq(ARMV7.rsp, frameSize + 4);
            // save all the callee save registers
            asm.save(csl, csl.frameOffsetToCSA);

            CiKind[] params = CiUtil.signatureToKinds(runtimeRoutine.classMethodActor);
            CiValue[] args = registerConfig.getCallingConvention(JavaCall, params, target(), false).locations;
            if (kind != null && !kind.isVoid()) {
                CiAddress src = null;
                CiStackSlot ss = null;
                CiStackSlot arg4STACK = null;

                switch (kind) {
                    case Int:
                    case Object:
                    case Float:
                        if (kind != CiKind.Float) {
                            assert !args[4].isRegister();
                        }
                        ss = (CiStackSlot) registerConfigs.compilerStub.getCallingConvention(JavaCall, new CiKind[] {kind}, target(), true).locations[0];
                        assert ss.index() == 1 : "compiler stub return value slot index has changed?";
                        src = new CiAddress(kind, ARMV7.RSP, cfo + ss.index() * 4);
                        asm.setUpScratch(src);
                        asm.ldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, asm.scratchRegister, 0);
                        if (kind == CiKind.Float) {
                            asm.vmov(ConditionFlag.Always, args[4].asRegister(), ARMV7.r8, null, CiKind.Float, CiKind.Int);
                        } else {
                            assert args[4].isStackSlot() : "compiler stub arg4 is not STACK SLOT changed registerConfig changed!!!";
                            // we need to put it on the stack
                            arg4STACK = (CiStackSlot) args[4];
                            src = new CiAddress(kind, ARMV7.RSP, arg4STACK.index() * 4);
                            asm.setUpScratch(src);
                            asm.str(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, asm.scratchRegister, 0);
                        }
                        break;
                    case Long:
                    case Double:
                        if (kind != CiKind.Double) {
                            assert !args[4].isRegister();
                        }
                        ss = (CiStackSlot) registerConfigs.compilerStub.getCallingConvention(JavaCall, new CiKind[] {kind}, target(), true).locations[0];
                        src = new CiAddress(kind, ARMV7.RSP, cfo + ss.index() * 4);
                        asm.setUpScratch(src);
                        asm.ldrd(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, asm.scratchRegister, 0);
                        if (kind == CiKind.Double) {
                            asm.vmov(ConditionFlag.Always, ARMV7.s0, ARMV7.r8, ARMV7.r9, CiKind.Double, CiKind.Long);
                        } else {
                            assert args[4].isStackSlot() : "compiler stub arg4 is not STACK SLOT changed registerConfig changed!!!";
                            // we need to put it on the stack
                            arg4STACK = (CiStackSlot) args[4];
                            src = new CiAddress(kind, ARMV7.RSP, arg4STACK.index() * 4);
                            asm.setUpScratch(src);
                            asm.strd(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, asm.scratchRegister, 0);
                        }
                        break;
                    default:
                        throw new InternalError("Unexpected parameter kind: " + kind);
                }
            }

            // Copy original return address into arg 0 (i.e. 'ip')
            CiRegister arg0 = args[0].asRegister();
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP, cfo + DEOPT_RETURN_ADDRESS_OFFSET));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, arg0, asm.scratchRegister, 0);
            // Copy original stack pointer into arg 1 (i.e. 'sp')
            CiRegister arg1 = args[1].asRegister();
            asm.leaq(arg1, new CiAddress(WordUtil.archKind(), ARMV7.RSP, cfo));

            // Copy original frame pointer into arg 2 (i.e. 'sp')
            CiRegister arg2 = args[2].asRegister();
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, arg2, ARMV7.FP);

            // Copy callee save area into arg3 (i.e. 'csa')
            asm.movImm32(ARMV7Assembler.ConditionFlag.Always, asm.scratchRegister, 0);
            asm.addRegisters(ARMV7Assembler.ConditionFlag.Always, false, asm.scratchRegister, asm.scratchRegister, ARMV7.rsp, 0, 0);
            CiRegister arg3 = args[3].asRegister();
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, arg3, asm.scratchRegister);

            // Patch return address of deopt stub frame to look
            // like it was called by frame being deopt'ed.
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP, frameSize));
            asm.str(ARMV7Assembler.ConditionFlag.Always, arg0, asm.scratchRegister, 0);

            // Call runtime routine
            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // should never reach here
            asm.int3();
            ARMV7Label forever = new ARMV7Label();
            asm.bind(forever);
            asm.movImm32(ConditionFlag.Always, asm.scratchRegister, 0xc5a0c5a0);
            asm.branch(forever);

            String stubName = runtimeRoutineName + "StubWithCSA";
            byte[] code = asm.codeBuffer.close(true);
            Type stubType = kind == null ? DeoptStubFromSafepoint : DeoptStubFromCompilerStub;
            return new Stub(stubType, stubName, frameSize, code, callPos, callSize, runtimeRoutine.classMethodActor, -1);
        } else if (platform().isa == ISA.Aarch64) {
            CiCalleeSaveLayout csl = registerConfig.csl;
            Aarch64MacroAssembler asm = new Aarch64MacroAssembler(target(), registerConfig);
            int frameSize = target().alignFrameSize(csl.size);
            int cfo = frameSize + target().stackAlignment; // Caller frame offset

            String runtimeRoutineName;
            if (kind == null) {
                runtimeRoutineName = "deoptimizeAtSafepoint";
            } else {
                runtimeRoutineName = "deoptimize" + kind.name();
            }
            final CriticalMethod runtimeRoutine;
            try {
                runtimeRoutine = new CriticalMethod(Deoptimization.class, runtimeRoutineName, null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
            } catch (NoSuchMethodError e) {
                // No deoptimization stub for kind
                return null;
            }

            // now allocate the frame for this method (including return address slot)
            asm.sub(64, Aarch64.sp, Aarch64.sp, frameSize + target().stackAlignment);

            // save all the callee save registers
            asm.save(csl, csl.frameOffsetToCSA);

            CiKind[] params = CiUtil.signatureToKinds(runtimeRoutine.classMethodActor);
            CiValue[] args = registerConfig.getCallingConvention(JavaCall, params, target(), false).locations;
            if (kind != null && !kind.isVoid()) {
                // Copy return value into arg 4
                CiRegister arg4 = args[4].asRegister();
                CiStackSlot ss = (CiStackSlot) registerConfigs.compilerStub.getCallingConvention(JavaCall, new CiKind[] {kind}, target(), true).locations[0];
                assert ss.index() == 1 : "compiler stub return value slot index has changed?";
                asm.mov(64, asm.scratchRegister, Aarch64.sp);
                asm.add(64, asm.scratchRegister, asm.scratchRegister, cfo + (ss.index() * 8));
                Aarch64Address src = Aarch64Address.createBaseRegisterOnlyAddress(asm.scratchRegister);
                if (kind.isFloat()) {
                    asm.fldr(32, arg4, src);
                } else if (kind.isDouble()) {
                    asm.fldr(64, arg4, src);
                } else {
                    asm.ldr(64, arg4, src);
                }
            }


            // Copy original return address into arg 0 (i.e. 'ip')
            CiRegister arg0 = args[0].asRegister();
            asm.mov(64, asm.scratchRegister, Aarch64.sp);
            asm.add(64, asm.scratchRegister, asm.scratchRegister, cfo + DEOPT_RETURN_ADDRESS_OFFSET);
            asm.ldr(64, arg0, Aarch64Address.createBaseRegisterOnlyAddress(asm.scratchRegister));

            // Copy original stack pointer into arg 1 (i.e. 'sp')
            CiRegister arg1 = args[1].asRegister();
            asm.add(64, arg1, Aarch64.sp, cfo);

            // Copy original frame pointer into arg 2 (i.e. 'fp')
            CiRegister arg2 = args[2].asRegister();
            asm.mov(64, arg2, Aarch64.fp);

            // Copy callee save area into arg3 (i.e. 'csa')
            CiRegister arg3 = args[3].asRegister();
            asm.mov(64, arg3, Aarch64.sp);

            // Patch return address of deopt stub frame to look like it was called by frame being deopt'ed.
            asm.mov(64, asm.scratchRegister, Aarch64.sp);
            asm.add(64, asm.scratchRegister, asm.scratchRegister, frameSize);
            asm.str(64, arg0, Aarch64Address.createBaseRegisterOnlyAddress(asm.scratchRegister));

            // Call runtime routine
            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // should never reach here
            asm.crashme();
            asm.brk();

            String stubName = runtimeRoutineName + "StubWithCSA";
            byte[] code = asm.codeBuffer.close(true);
            Type stubType = kind == null ? DeoptStubFromSafepoint : DeoptStubFromCompilerStub;
            return new Stub(stubType, stubName, frameSize, code, callPos, callSize, runtimeRoutine.classMethodActor, -1);
        } else if (platform().isa == ISA.RISCV64) {

            CiCalleeSaveLayout csl = registerConfig.csl;
            RISCV64MacroAssembler asm = new RISCV64MacroAssembler(target(), registerConfig);
            int frameSize = target().alignFrameSize(csl.size);
            int cfo = frameSize + target().stackAlignment; // Caller frame offset

            // TODO INFINITE LOOP
            asm.jal(RISCV64.zero, 0);

            String runtimeRoutineName;
            if (kind == null) {
                runtimeRoutineName = "deoptimizeAtSafepoint";
            } else {
                runtimeRoutineName = "deoptimize" + kind.name();
            }
            final CriticalMethod runtimeRoutine;
            try {
                runtimeRoutine = new CriticalMethod(Deoptimization.class, runtimeRoutineName, null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
            } catch (NoSuchMethodError e) {
                // No deoptimization stub for kind
                return null;
            }

            // now allocate the frame for this method (including return address slot)
            asm.subi(RISCV64.sp, RISCV64.sp, frameSize + target().stackAlignment);

            // save all the callee save registers
            asm.save(csl, csl.frameOffsetToCSA);

            CiKind[] params = CiUtil.signatureToKinds(runtimeRoutine.classMethodActor);
            CiValue[] args = registerConfig.getCallingConvention(JavaCall, params, target(), false).locations;
            if (kind != null && !kind.isVoid()) {
                // Copy return value into arg 4
                CiRegister arg4 = args[4].asRegister();
                CiStackSlot ss = (CiStackSlot) registerConfigs.compilerStub.getCallingConvention(JavaCall, new CiKind[] {kind}, target(), true).locations[0];
                assert ss.index() == 1 : "compiler stub return value slot index has changed?";
                asm.mov(asm.scratchRegister, RISCV64.sp);
                asm.addi(asm.scratchRegister, asm.scratchRegister, cfo + (ss.index() * 8));
                RISCV64Address src = RISCV64Address.createBaseRegisterOnlyAddress(asm.scratchRegister);
                // TODO check fldr on general purpose register works as expected. Otherwise, try using normal ldr
                if (kind.isFloat()) {
                    asm.fldr(32, arg4, src);
                } else if (kind.isDouble()) {
                    asm.fldr(64, arg4, src);
                } else {
                    asm.ldru(64, arg4, src);
                }
            }

            // Copy original return address into arg 0 (i.e. 'ip')
            CiRegister arg0 = args[0].asRegister();
            asm.mov(asm.scratchRegister, RISCV64.sp);
            asm.addi(asm.scratchRegister, asm.scratchRegister, cfo + DEOPT_RETURN_ADDRESS_OFFSET);
            asm.ldru(64, arg0, RISCV64Address.createBaseRegisterOnlyAddress(asm.scratchRegister));

            // Copy original stack pointer into arg 1 (i.e. 'sp')
            CiRegister arg1 = args[1].asRegister();
            asm.addi(arg1, RISCV64.sp, cfo);

            // Copy original frame pointer into arg 2 (i.e. 'fp')
            CiRegister arg2 = args[2].asRegister();
            asm.mov(arg2, RISCV64.fp);

            // Copy callee save area into arg3 (i.e. 'csa')
            CiRegister arg3 = args[3].asRegister();
            asm.mov(arg3, RISCV64.sp);

            // Patch return address of deopt stub frame to look like it was called by frame being deopt'ed.
            asm.mov(asm.scratchRegister, RISCV64.sp);
            asm.addi(asm.scratchRegister, asm.scratchRegister, frameSize);
            asm.str(64, arg0, RISCV64Address.createBaseRegisterOnlyAddress(asm.scratchRegister));

            // Call runtime routine
            asm.alignForPatchableDirectCall(asm.codeBuffer.position());
            int callPos = asm.codeBuffer.position();
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // should never reach here
            asm.crashme();
            asm.ebreak();

            String stubName = runtimeRoutineName + "StubWithCSA";
            byte[] code = asm.codeBuffer.close(true);
            Type stubType = kind == null ? DeoptStubFromSafepoint : DeoptStubFromCompilerStub;
            return new Stub(stubType, stubName, frameSize, code, callPos, callSize, runtimeRoutine.classMethodActor, -1);
        } else {
            throw FatalError.unimplemented("com.sun.max.vm.compiler.target.Stubs.genDeoptStubWithCSA");
        }
    }

    /**
     * Generates the code that makes the transition from a use of {@link Infopoints#uncommonTrap()} to
     * {@link Deoptimization#uncommonTrap(Pointer, Pointer, Pointer, Pointer)}.
     */
    @HOSTED_ONLY
    public Stub genUncommonTrapStub() {
        if (platform().isa == ISA.AMD64) {
            CiRegisterConfig registerConfig = registerConfigs.uncommonTrapStub;
            AMD64MacroAssembler asm = new AMD64MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            int frameSize = platform().target.alignFrameSize(csl.size);
            int frameToCSA = csl.frameOffsetToCSA;

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }

            // now allocate the frame for this method
            asm.subq(AMD64.rsp, frameSize);

            // save all the registers
            asm.save(csl, frameToCSA);

            String name = "uncommonTrap";         /*
             * The deopt stub initially executes in the frame of the method that was returned to (i.e. the method about to be
             * deoptimized). It then allocates a new frame, saves all registers, sets up args to deopt routine
             * and calls it.
             *
             *   subq rsp <frame size>                         // allocate frame
             *   mov  [rsp], rax                               // save ...
             *   mov  [rsp + 8], rcx                           //   all ...
             *   ...                                           //     the ...
             *   movq [rsp + 248], xmm15                       //       registers
             * { mov  rdx/xmm0, [rsp + <cfo> + 8] }            // if non-void return value, copy it from stack into arg4 (or xmm0)
             *   mov  rdi  [rsp + <cfo> + DEOPT_RETURN_ADDRESS_OFFSET]  // copy deopt IP into arg0
             *   lea  rsi, [rsp + <cfo>]                       // copy deopt SP into arg1
             *   mov  rdx, rbp                                 // copy deopt FP into arg2
             *   mov  rcx, rbp                                 // copy callee save area into arg3
             *   mov  [rsp + <frame size>], rdi                // restore deopt IP (i.e. original return address) into return address slot
             *   call <deopt routine>                          // call deoptimization routine
             *   brk                                           // should not reach here
             */

            final CriticalMethod uncommonTrap = new CriticalMethod(Deoptimization.class, name, null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);

            CiValue[] args = registerConfig.getCallingConvention(JavaCall, new CiKind[] {WordUtil.archKind(), WordUtil.archKind(), WordUtil.archKind(), WordUtil.archKind()}, target(),
                    false).locations;

            // Copy callee save area address into arg 0 (i.e. 'csa')
            CiRegister arg0 = args[0].asRegister();
            asm.leaq(arg0, new CiAddress(WordUtil.archKind(), AMD64.RSP, frameToCSA));

            // Copy return address into arg 1 (i.e. 'ip')
            CiRegister arg1 = args[1].asRegister();
            asm.movq(arg1, new CiAddress(WordUtil.archKind(), AMD64.RSP, frameSize));

            // Copy stack pointer into arg 2 (i.e. 'sp')
            CiRegister arg2 = args[2].asRegister();
            asm.leaq(arg2, new CiAddress(WordUtil.archKind(), AMD64.RSP, frameSize + 8));

            // Copy original frame pointer into arg 3 (i.e. 'fp')
            CiRegister arg3 = args[3].asRegister();
            asm.movq(arg3, AMD64.rbp);

            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = uncommonTrap.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // Should never reach here
            int registerRestoreEpilogueOffset = asm.codeBuffer.position();
            asm.hlt();

            String stubName = name + "Stub";
            byte[] code = asm.codeBuffer.close(true);
            return new Stub(UncommonTrapStub, stubName, frameSize, code, callPos, callSize, callee, registerRestoreEpilogueOffset);
        } else if (platform().isa == ISA.ARM) {
            CiRegisterConfig registerConfig = registerConfigs.uncommonTrapStub;
            ARMV7MacroAssembler asm = new ARMV7MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            int frameSize = platform().target.alignFrameSize(csl.size);
            int frameToCSA = csl.frameOffsetToCSA;

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }

            // now allocate the frame for this method
            asm.push(ARMV7Assembler.ConditionFlag.Always, 1 << 14, true);
            asm.subq(ARMV7.rsp, frameSize);

            // save all the registers
            asm.save(csl, frameToCSA);

            String name = "uncommonTrap";
            final CriticalMethod uncommonTrap = new CriticalMethod(Deoptimization.class, name, null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);

            CiValue[] args = registerConfig.getCallingConvention(JavaCall, new CiKind[] {WordUtil.archKind(), WordUtil.archKind(), WordUtil.archKind(), WordUtil.archKind()}, target(),
                    false).locations;

            // Copy callee save area address into arg 0 (i.e. 'csa')
            CiRegister arg0 = args[0].asRegister();
            asm.leaq(arg0, new CiAddress(WordUtil.archKind(), ARMV7.RSP, frameToCSA));

            // Copy return address into arg 1 (i.e. 'ip')
            CiRegister arg1 = args[1].asRegister();
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP, frameSize));
            asm.ldr(ConditionFlag.Always, arg1, asm.scratchRegister, 0);

            // Copy stack pointer into arg 2 (i.e. 'sp')
            CiRegister arg2 = args[2].asRegister();
            asm.leaq(arg2, new CiAddress(WordUtil.archKind(), ARMV7.RSP, frameSize + 4));

            // Copy original frame pointer into arg 3 (i.e. 'fp')
            CiRegister arg3 = args[3].asRegister();
            asm.mov(ConditionFlag.Always, false, arg3, ARMV7.FP);

            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = uncommonTrap.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // Should never reach here
            int registerRestoreEpilogueOffset = asm.codeBuffer.position();
            ARMV7Label forever = new ARMV7Label();
            asm.bind(forever);
            asm.movImm32(ConditionFlag.Always, asm.scratchRegister, 0xffffffff);
            asm.blx(asm.scratchRegister); // expect it to crash
            asm.branch(forever);

            asm.hlt();
            String stubName = name + "Stub";
            byte[] code = asm.codeBuffer.close(true);
            return new Stub(UncommonTrapStub, stubName, frameSize, code, callPos, callSize, callee, registerRestoreEpilogueOffset);
        } else if (platform().isa == ISA.Aarch64) {
            CiRegisterConfig registerConfig = registerConfigs.uncommonTrapStub;
            Aarch64MacroAssembler masm = new Aarch64MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            int frameSize = platform().target.alignFrameSize(csl.size);
            int frameToCSA = csl.frameOffsetToCSA;

            masm.nop(prologueSize / Aarch64Assembler.INSTRUCTION_SIZE);
            masm.crashme();

            // now allocate the frame for this method
            masm.sub(64, Aarch64.sp, Aarch64.sp, frameSize);

            // save all the registers
            masm.save(csl, frameToCSA);

            String name = "uncommonTrap";

            final CriticalMethod uncommonTrap = new CriticalMethod(Deoptimization.class, name, null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);

            CiValue[] args = registerConfig.getCallingConvention(JavaCall,
                    new CiKind[] {WordUtil.archKind(), WordUtil.archKind(), WordUtil.archKind(), WordUtil.archKind()},
                    target(), false).locations;

            // Copy callee save area address into arg 0 (i.e. 'csa')
            CiRegister arg0 = args[0].asRegister();
            masm.leaq(arg0, new CiAddress(WordUtil.archKind(), Aarch64.rsp, frameToCSA));

            // Copy return address into arg 1 (i.e. 'ip')
            CiRegister arg1 = args[1].asRegister();
            masm.load(arg1, new CiAddress(WordUtil.archKind(), Aarch64.rsp, frameSize), WordUtil.archKind());

            // Copy stack pointer into arg 2 (i.e. 'sp')
            CiRegister arg2 = args[2].asRegister();
            masm.leaq(arg2, new CiAddress(WordUtil.archKind(), Aarch64.rsp, frameSize + 8));

            // Copy original frame pointer into arg 3 (i.e. 'fp')
            CiRegister arg3 = args[3].asRegister();
            masm.mov(64, arg3, Aarch64.fp);

            masm.alignForPatchableDirectCall();
            int callPos = masm.codeBuffer.position();
            ClassMethodActor callee = uncommonTrap.classMethodActor;
            masm.call();
            int callSize = masm.codeBuffer.position() - callPos;

            // Should never reach here
            int registerRestoreEpilogueOffset = masm.codeBuffer.position();
            masm.hlt();

            String stubName = name + "Stub";
            byte[] code = masm.codeBuffer.close(true);
            return new Stub(UncommonTrapStub, stubName, frameSize, code, callPos, callSize, callee, registerRestoreEpilogueOffset);
        } else if (platform().isa == ISA.RISCV64) {
            CiRegisterConfig registerConfig = registerConfigs.uncommonTrapStub;
            RISCV64MacroAssembler masm = new RISCV64MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            int frameSize = platform().target.alignFrameSize(csl.size);
            int frameToCSA = csl.frameOffsetToCSA;

            // TODO INFINITE LOOP
            masm.jal(RISCV64.zero, 0);


            for (int i = 0; i < prologueSize; ++i) {
                masm.nop();
            }
            masm.crashme();

            // now allocate the frame for this method
            masm.subi(RISCV64.sp, RISCV64.sp, frameSize);

            // save all the registers
            masm.save(csl, frameToCSA);

            String name = "uncommonTrap";

            final CriticalMethod uncommonTrap = new CriticalMethod(Deoptimization.class, name, null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);

            CiValue[] args = registerConfig.getCallingConvention(JavaCall,
                    new CiKind[] {WordUtil.archKind(), WordUtil.archKind(), WordUtil.archKind(), WordUtil.archKind()},
                    target(), false).locations;

            // Copy callee save area address into arg 0 (i.e. 'csa')
            CiRegister arg0 = args[0].asRegister();
            masm.leaq(arg0, new CiAddress(WordUtil.archKind(), RISCV64.rsp, frameToCSA));

            // Copy return address into arg 1 (i.e. 'ip')
            CiRegister arg1 = args[1].asRegister();
            masm.load(arg1, new CiAddress(WordUtil.archKind(), RISCV64.rsp, frameSize), WordUtil.archKind());

            // Copy stack pointer into arg 2 (i.e. 'sp')
            CiRegister arg2 = args[2].asRegister();
            masm.leaq(arg2, new CiAddress(WordUtil.archKind(), RISCV64.rsp, frameSize + 8));

            // Copy original frame pointer into arg 3 (i.e. 'fp')
            CiRegister arg3 = args[3].asRegister();
            masm.mov(arg3, RISCV64.fp);

            masm.alignForPatchableDirectCall(masm.codeBuffer.position());
            int callPos = masm.codeBuffer.position();
            ClassMethodActor callee = uncommonTrap.classMethodActor;
            masm.call();
            int callSize = masm.codeBuffer.position() - callPos;

            // Should never reach here
            int registerRestoreEpilogueOffset = masm.codeBuffer.position();
            masm.hlt();

            String stubName = name + "Stub";
            byte[] code = masm.codeBuffer.close(true);
            return new Stub(UncommonTrapStub, stubName, frameSize, code, callPos, callSize, callee, registerRestoreEpilogueOffset);
        } else {
            throw FatalError.unimplemented("com.sun.max.vm.compiler.target.Stubs.genUncommonTrapStub");
        }
    }

    /**
     * Reads the virtual dispatch index out of the frame of a dynamic trampoline.
     *
     * @param calleeSaveStart the address within the frame where the callee-saved registers are located
     */
    public int readVirtualDispatchIndexFromTrampolineFrame(Pointer calleeSaveStart) {
        CiRegisterConfig registerConfig = registerConfigs.trampoline;
        CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
        if (platform().isa == ISA.ARM) {
            return calleeSaveStart.plus(csl.offsetOf(ARMV7.r8)).getInt();
        }
        return calleeSaveStart.plus(csl.offsetOf(registerConfig.getScratchRegister())).getInt();
    }
}
