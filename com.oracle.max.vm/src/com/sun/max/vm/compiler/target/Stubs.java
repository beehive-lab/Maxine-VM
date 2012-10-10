/*
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
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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

import com.oracle.max.asm.target.amd64.*;
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
import com.sun.max.vm.compiler.target.Stub.Type;
import com.sun.max.vm.compiler.target.amd64.*;
import com.sun.max.vm.intrinsics.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.amd64.*;
import com.sun.max.vm.thread.*;

/**
 * Stubs are pieces of hand crafted assembly code for expressing semantics that cannot otherwise be expressed as Java.
 * For example, trampolines are stubs used to lazily link call sites to their targets at runtime.
 */
public class Stubs {

    /**
     * The stubs called to link an interface method call.
     */
    private final ArrayList<Stub> virtualTrampolines = new ArrayList<Stub>();

    /**
     * The stubs called to link an interface method call.
     */
    private final ArrayList<Stub> interfaceTrampolines = new ArrayList<Stub>();

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
     * The deopt stub used for a frame stopped at a safepoint poll.
     * This stub saves the registers, making them available for deoptimization.
     */
    private Stub deoptStubForSafepointPoll;

    private CriticalMethod resolveVirtualCall;
    private CriticalMethod resolveInterfaceCall;
    private CiValue[] resolveVirtualCallArgs;
    private CiValue[] resolveInterfaceCallArgs;
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
                resolveVirtualCallArgs = registerConfigs.trampoline.getCallingConvention(JavaCall,
                                CiUtil.signatureToKinds(resolveVirtualCall.classMethodActor), target(), false).locations;
                resolveInterfaceCallArgs = registerConfigs.trampoline.getCallingConvention(JavaCall,
                                CiUtil.signatureToKinds(resolveInterfaceCall.classMethodActor), target(), false).locations;
                staticTrampoline = genStaticTrampoline();
                trapStub = genTrapStub();

                CriticalMethod unroll = new CriticalMethod(Stubs.class, "unroll", null);
                CiValue[] unrollArgs = registerConfigs.standard.getCallingConvention(JavaCall,
                                CiUtil.signatureToKinds(unroll.classMethodActor), target(), false).locations;
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
                        CiValue[] unwindArgs = registerConfigs.standard.getCallingConvention(JavaCall,
                                        CiUtil.signatureToKinds(unwind.classMethodActor), target(), false).locations;
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

    public synchronized CodePointer interfaceTrampoline(int iIndex) {
        if (interfaceTrampolines.size() <= iIndex) {
            for (int i = interfaceTrampolines.size(); i <= iIndex; i++) {
                String stubName = "itrampoline<" + i + ">";
                traceBeforeStubCreation(stubName);
                Stub stub = genDynamicTrampoline(i, true, stubName);
                interfaceTrampolines.add(stub);
                traceAfterStubCreation(stubName);
            }
        }
        return VTABLE_ENTRY_POINT.in(interfaceTrampolines.get(iIndex));
    }

    public synchronized CodePointer virtualTrampoline(int vTableIndex) {
        if (virtualTrampolines.size() <= vTableIndex) {
            for (int i = virtualTrampolines.size(); i <= vTableIndex; i++) {
                String stubName = "vtrampoline<" + i + ">";
                traceBeforeStubCreation(stubName);
                Stub stub = genDynamicTrampoline(i, false, stubName);
                virtualTrampolines.add(stub);
                traceAfterStubCreation(stubName);
            }
        }
        return VTABLE_ENTRY_POINT.in(virtualTrampolines.get(vTableIndex));
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
            return AMD64TargetMethodUtil.isPatchedJumpTo(tm, OPTIMIZED_ENTRY_POINT.offset(),  vm().stubs.staticTrampoline().codeAt(OPTIMIZED_ENTRY_POINT.offset()));
        } else {
            throw FatalError.unimplemented();
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
        final TargetMethod selectedCalleeTargetMethod =  selectedCallee.makeTargetMethod(caller);
        FatalError.check(selectedCalleeTargetMethod.invalidated() == null, "resolved virtual method must not be invalidated");
        CodePointer vtableEntryPoint = selectedCalleeTargetMethod.getEntryPoint(VTABLE_ENTRY_POINT);
        if (selectedCallee.hadDeopt && Deoptimization.deoptLogger.enabled()) {
            Deoptimization.deoptLogger.logResolveVTable(vTableIndex,  hub.classActor, vtableEntryPoint.toAddress());
        }
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

            // save all the callee save registers
            asm.save(csl, frameToCSA);

            CiValue[] args = isInterface ? resolveInterfaceCallArgs : resolveVirtualCallArgs;

            // the receiver is already in the first arg register
            //asm.movq(locations[0].asRegister(), locations[0].asRegister());

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
        }
        throw FatalError.unimplemented();
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

    /**
     * Generates a stub that links a call to a method whose actor is available in
     * data {@linkplain TargetMethod#callSiteToCallee(CodePointer) associated} with the call site.
     * The stub also saves and restores all the callee-saved registers specified in the
     * {@linkplain RegisterConfigs#trampoline trampoline} register configuration.
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
        }
        throw FatalError.unimplemented();
    }

    /**
     * Generates the stub called by the native level trap handler (see trap.c).
     * The stub:
     * <ol>
     * <li>flushes all the registers specified in the {@linkplain RegisterConfigs#trapStub trap stub}
     * register configuration to the stack (plus the trap number and any platform specific
     * state such as the flags register on AMD64),</li>
     * <li>adjusts the return address of the trap frame to be the address of the trapped instruction,</li>
     * <li>calls {@link Trap#handleTrap},</li>
     * <li>restores the saved registers and platform-specific state, and</li>
     * <li>returns execution to the trapped frame to re-execute the trapped instruction.</li>
     * </ol>
     *
     * For traps resulting in runtime exceptions (e.g. {@link NullPointerException}), the handler
     * will directly transfer execution to the exception handler, by-passing steps 4 and 5 above.
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
        }
        throw FatalError.unimplemented();
    }

    /**
     * Unwinds the current thread execution state to a given (caller) frame and instruction pointer.
     * The frame must be an existing caller frame on the stack and the instruction pointer
     * must be a valid address within the code associated with the frame.
     *
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
        }
        throw FatalError.unimplemented();
    }

    /**
     * Expands the stack by a given amount and then calls {@link Deoptimization#unroll(Info)}.
     * The stack expansion is required to fit the deoptimized frames encoded in {@code info}.
     *
     * @param info the argument to pass onto {@link Deoptimization#unroll(Info)}
     * @param frameSize the amount by which the stack should be expanded (must be >= 0)
     */
    @NEVER_INLINE
    public static void unroll(Info info, int frameSize) {
        FatalError.unexpected("stub should be overwritten");
    }

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
        }
        throw FatalError.unimplemented();
    }

    /**
     * Stub initialization that must be done at runtime.
     */
    static abstract class RuntimeInitialization {
        abstract void apply();
    }

    /**
     * Helper to patch the address of a deoptimization runtime routine into a deopt stub.
     * This can only be done at runtime once the address is known and relocated.
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

        public AMD64DeoptStubPatch(int pos, CriticalMethod runtimeRoutine, Stub stub) {
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
     * Generates a stub to deoptimize a method upon returning to it.
     *
     * @param kind the return value kind
     */
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
                // Copy return value into arg 4
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
        }
        throw FatalError.unimplemented();
    }

    /**
     * Generates a stub to deoptimize an method upon returning to it. This stub creates a new frame for saving the registers
     * specified by the {@link CiCalleeSaveLayout} of a given register configuration.
     *
     * @param kind the return value kind or {@code null} if generating the stub used when returning from a safepoint trap
     * @param returnValueOnStack specifies if the return value is on the stack (ignored if {@code kind == null})
     */
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
             *   int3                                          // should not reach here
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
        }
        throw FatalError.unimplemented();
    }

    /**
     * Generates the code that makes the transition from a use of {@link Infopoints#uncommonTrap()}
     * to {@link Deoptimization#uncommonTrap(Pointer, Pointer, Pointer, Pointer)}.
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

            String name = "uncommonTrap";
            final CriticalMethod uncommonTrap = new CriticalMethod(Deoptimization.class, name, null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);

            CiValue[] args = registerConfig.getCallingConvention(JavaCall, new CiKind[] {WordUtil.archKind(), WordUtil.archKind(), WordUtil.archKind(), WordUtil.archKind()}, target(), false).locations;

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
        }
        throw FatalError.unimplemented();
    }

    /**
     * Reads the virtual dispatch index out of the frame of a dynamic trampoline.
     *
     * @param calleeSaveStart the address within the frame where the callee-saved registers are located
     */
    public int readVirtualDispatchIndexFromTrampolineFrame(Pointer calleeSaveStart) {
        CiRegisterConfig registerConfig = registerConfigs.trampoline;
        CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
        return calleeSaveStart.plus(csl.offsetOf(registerConfig.getScratchRegister())).getInt();
    }
}
