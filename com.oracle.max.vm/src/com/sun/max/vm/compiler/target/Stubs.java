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

import com.oracle.max.asm.Label;
import com.oracle.max.asm.target.amd64.AMD64;
import com.oracle.max.asm.target.amd64.AMD64MacroAssembler;
import com.oracle.max.asm.target.armv7.ARMV7;
import com.oracle.max.asm.target.armv7.ARMV7Assembler;
import com.oracle.max.asm.target.armv7.ARMV7MacroAssembler;
import com.oracle.max.asm.target.armv7.ARMV7Assembler.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.HOSTED_ONLY;
import com.sun.max.annotate.NEVER_INLINE;
import com.sun.max.annotate.PLATFORM;
import com.sun.max.lang.ISA;
import com.sun.max.lang.WordWidth;
import com.sun.max.unsafe.Address;
import com.sun.max.unsafe.CodePointer;
import com.sun.max.unsafe.Pointer;
import com.sun.max.unsafe.Word;
import com.sun.max.vm.Log;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.VMOptions;
import com.sun.max.vm.actor.holder.DynamicHub;
import com.sun.max.vm.actor.holder.Hub;
import com.sun.max.vm.actor.member.ClassMethodActor;
import com.sun.max.vm.actor.member.VirtualMethodActor;
import com.sun.max.vm.code.Code;
import com.sun.max.vm.code.CodeManager;
import com.sun.max.vm.compiler.CallEntryPoint;
import com.sun.max.vm.compiler.WordUtil;
import com.sun.max.vm.compiler.deopt.Deoptimization;
import com.sun.max.vm.compiler.deopt.Deoptimization.Info;
import com.sun.max.vm.compiler.target.Stub.Type;
import com.sun.max.vm.compiler.target.amd64.AMD64TargetMethodUtil;
import com.sun.max.vm.compiler.target.arm.ARMTargetMethodUtil;
import com.sun.max.vm.intrinsics.Infopoints;
import com.sun.max.vm.object.ObjectAccess;
import com.sun.max.vm.runtime.CriticalMethod;
import com.sun.max.vm.runtime.FatalError;
import com.sun.max.vm.runtime.Trap;
import com.sun.max.vm.runtime.amd64.AMD64SafepointPoll;
import com.sun.max.vm.runtime.amd64.AMD64TrapFrameAccess;
import com.sun.max.vm.runtime.arm.ARMSafepointPoll;
import com.sun.max.vm.runtime.arm.ARMTrapFrameAccess;
import com.sun.max.vm.thread.VmThread;

import java.util.ArrayList;
import java.util.Arrays;

import static com.sun.cri.ci.CiCallingConvention.Type.JavaCall;
import static com.sun.cri.ci.CiCallingConvention.Type.JavaCallee;
import static com.sun.max.platform.Platform.platform;
import static com.sun.max.platform.Platform.target;
import static com.sun.max.vm.MaxineVM.isHosted;
import static com.sun.max.vm.MaxineVM.vm;
import static com.sun.max.vm.VMOptions.verboseOption;
import static com.sun.max.vm.compiler.CallEntryPoint.OPTIMIZED_ENTRY_POINT;
import static com.sun.max.vm.compiler.CallEntryPoint.VTABLE_ENTRY_POINT;
import static com.sun.max.vm.compiler.deopt.Deoptimization.DEOPT_RETURN_ADDRESS_OFFSET;
import static com.sun.max.vm.compiler.target.Stub.Type.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

/**
 * Stubs are pieces of hand crafted assembly code for expressing semantics that cannot otherwise be expressed as Java.
 * For example, trampolines are stubs used to lazily link call sites to their targets at runtime.
 */
public class Stubs {

    final private class TrampolineList extends ArrayList<Stub> {
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
     * Position of the instruction in virtual / interface trampolines loading the immediate index in the scratch register.
     * Used to quickly retrieve the itable / vtable index the trampoline dispatch to.
     */
    private int indexMovInstrPos;

    /**
     * Return the index, relative to Hub's origin, to the entry of dispatch tables (virtual or interface) the stub is assigned to.
     *
     * @param stub a virtual or interface trampoline stub
     * @return an index to a virtual table entry if the stub is a virtual call trampoline stub, an index to a interface table entry if the stub is a interface call trampoline.
     */
    public int getDispatchTableIndex(TargetMethod stub) {
        assert stub.is(VirtualTrampoline) || stub.is(InterfaceTrampoline);
        int tmpindex = 0;
        if (platform().isa == ISA.ARM) {
            Pointer callSitePointer = stub.codeStart().toPointer();

            if (((callSitePointer.readByte(3) & 0xff) == 0xe3) && ((callSitePointer.readByte(4 + 3) & 0xff) == 0xe3)) {
                tmpindex = (callSitePointer.readByte(4 + 0) & 0xff) | ((callSitePointer.readByte(4 + 1) & 0xf) << 8) | ((callSitePointer.readByte(4 + 2) & 0xf) << 12);
                tmpindex = tmpindex << 16;
                tmpindex += (callSitePointer.readByte(0) & 0xff) | ((callSitePointer.readByte(1) & 0xf) << 8) | ((callSitePointer.readByte(2) & 0xf) << 12);

            } else {
                assert 0 == 1 : "Failed to obtian correct getDispatchTableIndex for ARM indexMovInstrn wrong\n";
            }
        } else if (platform().isa == ISA.AMD64) {
            tmpindex = stub.codeStart().toPointer().readInt(indexMovInstrPos);
        } else {
            throw FatalError.unimplemented();

        }
        final int index = tmpindex;
        //final int index = stub.codeStart().toPointer().readInt(indexMovInstrPos);
        assert stub.is(VirtualTrampoline) ? (virtualTrampolines.size() > index && virtualTrampolines.get(index) == stub) : (interfaceTrampolines.size() > index && interfaceTrampolines.get(index) == stub);
        return index;
    }

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
     *                         {@linkplain Stub.Type#CompilerStub compiler stub}. Compiler stubs return values via the stack.
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
		  com.sun.max.vm.Log.println("ARMTargetMethoUtil.isJumpTo needs to be implemented");
		 return true;
            //return ARMTargetMethodUtil.isJumpTo(tm, OPTIMIZED_ENTRY_POINT.offset(), OPTIMIZED_ENTRY_POINT.in(vm().stubs.staticTrampoline()));
        } else {
            throw FatalError.unimplemented();
        }
    }

    /**
     * Resolves the vtable entry denoted by a given receiver object and vtable index.
     *
     * @param receiver    the receiver of a virtual call
     * @param vTableIndex the vtable index of the call
     * @param pcInCaller  an instruction address somewhere in the caller (usually the return address) that can be used to
     *                    look up the caller in the code cache
     */
    private static Address resolveVirtualCall(Object receiver, int vTableIndex, Pointer pcInCaller) {
        // pcInCaller must be dealt with before any safepoint
        if (VMOptions.verboseOption.verboseCompilation) {
            Log.println("STUBS:resolveVirtualCall");
            Log.print("INDEX ");
            Log.print(vTableIndex);
            Log.print("CALLERPC ");
            Log.println(pcInCaller);
            Log.print("RECEIVER ");
            Log.println(receiver.getClass());
        }
        CodePointer cpCallSite = CodePointer.from(pcInCaller);
        if (VMOptions.verboseOption.verboseCompilation) {
            Log.print("CALLSITE ");
            Log.println(cpCallSite);
        }
        final TargetMethod caller = cpCallSite.toTargetMethod();

        final Hub hub = ObjectAccess.readHub(receiver);
        if (VMOptions.verboseOption.verboseCompilation) {
            //Log.print("RECEIVER ");Log.println(receiver.getClass());
            //Log.print("HUB ");Log.println(hub);
            Log.print("index ");
            Log.println(vTableIndex);
        }

        final VirtualMethodActor selectedCallee = hub.classActor.getVirtualMethodActorByVTableIndex(vTableIndex);
        if (selectedCallee.isAbstract()) {
            throw new AbstractMethodError();
        }
        if (VMOptions.verboseOption.verboseCompilation) {
            //Log.println(selectedCallee);
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
        if (VMOptions.verboseOption.verboseCompilation) {
            Log.print("STUBS:resolvevirtual returns ");
            Log.println(adjustedEntryPoint.toAddress());
            Log.print("ORIGINAL pcInCaller ");
            Log.println(pcInCaller);
        }
        return adjustedEntryPoint.toAddress();
    }

    /**
     * Resolves the itable entry denoted by a given receiver object and index operand of an interface call.
     *
     * @param receiver   the receiver of an interface call
     * @param iIndex     the index operand of the call
     * @param pcInCaller an instruction address somewhere in the caller (usually the return address) that can be used to
     *                   look up the caller in the code cache
     */
    private static Address resolveInterfaceCall(Object receiver, int iIndex, Pointer pcInCaller) {
        // pcInCaller must be dealt with before any safepoint
        if (VMOptions.verboseOption.verboseCompilation) {
            Log.println("STUBS:resolveInterfaceCall");
            Log.println(receiver.getClass());
            Log.println(pcInCaller);


        }


        CodePointer cpCallSite = CodePointer.from(pcInCaller);
        final TargetMethod caller = cpCallSite.toTargetMethod();
        if (VMOptions.verboseOption.verboseCompilation) {
            Log.print("CALLSITE ");
            Log.println(cpCallSite);
            Log.print("RECEIVER ");
            Log.println(receiver.getClass());
            Log.print("index ");
            Log.println(iIndex);
        }

        final Hub hub = ObjectAccess.readHub(receiver);
        final VirtualMethodActor selectedCallee = hub.classActor.getVirtualMethodActorByIIndex(iIndex);
        if (VMOptions.verboseOption.verboseCompilation) {
            Log.print("HUB ");
            Log.println(hub);
            Log.print("CALLEE ");
            Log.println(selectedCallee);
        }
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
        if (VMOptions.verboseOption.verboseCompilation) {
            Log.print("ADJUSTEDENTRY ");
            Log.println(adjustedEntryPoint.toAddress());
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
            if (isHosted() && index == 0) {
                indexMovInstrPos = asm.codeBuffer.position() - WordWidth.BITS_32.numberOfBytes;
            }


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
        } else if (platform().isa == ISA.ARM) {
            CiRegisterConfig registerConfig = registerConfigs.trampoline;
            ARMV7MacroAssembler asm = new ARMV7MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            int frameSize = target().alignFrameSize(csl.size);
            final int frameToCSA = csl.frameOffsetToCSA;

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }
            asm.push(ARMV7Assembler.ConditionFlag.Always, 1 << 14);


            //Label forever = new Label();
            //asm.bind(forever);
            //asm.mov32BitConstant(ARMV7.r12, 0x11111111);
            //asm.branch(forever);

            // now allocate the frame for this method
            asm.subq(ARMV7.r13, frameSize);

            // save the index in the scratch register. This register is then callee-saved
            // so that the stack walker can find it.
            //asm.mov32BitConstant(registerConfig.getScratchRegister(), index);

            asm.mov32BitConstant(ConditionFlag.Always, ARMV7.r8, index); // hard coded!!!
        /* R8 is an "alternate scratch we put the index in here and we added R8 to the end of the trampoline
        csl as r12 is used during calculations of stores and gets trashed on ARM
	    */

            if (isHosted() && index == 0) {
                indexMovInstrPos = asm.codeBuffer.position() - 8;// 2*WordWidth.BITS_32.numberOfBytes;
            }

            // save all the callee save registers
            asm.save(csl, frameToCSA);

            CiValue[] args = isInterface ? resolveInterfaceCallArgs : resolveVirtualCallArgs;

            // the receiver is already in the first arg register
            //asm.movq(locations[0].asRegister(), locations[0].asRegister());

            // load the index into the second arg register
            asm.mov32BitConstant(ConditionFlag.Always, args[1].asRegister(), index);

            if (!isInterface) asm.mov32BitConstant(ConditionFlag.Always, ARMV7.r12, 0xbeefbeef);// debugging resolveVirtualCall
            // load the return address into the third arg register

            // we will need to test this carefully
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.r13.asValue(), frameSize));
            //asm.movq(args[2].asRegister(), new CiAddress(WordUtil.archKind(), ARMV7.rsp.asValue(), frameSize));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, args[2].asRegister(), asm.scratchRegister, 0);
            // We already have the return address in R14 so jus tmove it rather than doing the load from stack
            //asm.mov(ARMV7Assembler.ConditionFlag.Always, false, args[2].asRegister(), ARMV7.r14);


            asm.alignForPatchableDirectCall(); // insert nops so that the call is in an allowed position
            // obeying alignment rules
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = isInterface ? resolveInterfaceCall.classMethodActor : resolveVirtualCall.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // Put the entry point of the resolved method on the stack just below the
            // return address of the trampoline itself. By adjusting RSP to point at
            // this second return address and executing a 'ret' instruction, execution
            // continues in the resolved method as if it was called by the trampoline's
            // caller which is exactly what we want.

            // APN I need to change this the CALL/RETURN is different for ARM
            // this may well be broken !!!!!!

            CiRegister returnReg = registerConfig.getReturnRegister(WordUtil.archKind());
	    // TEST COMMENT OUT OCTOBER ...
            //asm.addq(returnReg, 4); // BECAUSE IT IS NOT A CALL/BLX AND THE RETURN REGI IS ALREADY PUSHED TO THE STACK
            // this is done so when we load this to the PC we do not push the return address!
            //NOTE this is the slot used by r12 which we do not really need to restore?
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.r13.asValue(), frameSize - 4));
            //asm.movq(new CiAddress(WordUtil.archKind(), ARMV7.rsp.asValue(), frameSize - 8), returnReg);
            asm.str(ARMV7Assembler.ConditionFlag.Always, returnReg, asm.scratchRegister, 0);

            // Restore all parameter registers before returning
            int registerRestoreEpilogueOffset = asm.codeBuffer.position();
            asm.restore(csl, frameToCSA);
            // NEW
            //asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.r13.asValue(), frameSize    ));
            //asm.ldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.r14,ARMV7.r12, 0);


            // Adjust RSP as mentioned above and do the 'ret' that lands us in the
            // trampolined-to method.
            //----asm.addq(ARMV7.r13, frameSize-4  ); // points at entry point of trampolined method`
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.r13.asValue(), frameSize - 4));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, ARMV7.r12, 0);
            //asm.addq(ARMV7.r13, frameSize); OCTOBER
	    asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.r13.asValue(), frameSize));
	    asm.ldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.r14, ARMV7.r12,0);
	    asm.addq(ARMV7.r13, frameSize+4);
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r15, ARMV7.r8);
            //Label forever2 = new Label();
            //asm.bind(forever2);
            //asm.mov32BitConstant(ARMV7.r12, 0x22222222);
            //asm.branch(forever2);

            //AUGasm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.r13.asValue(), 4));
            //AUGasm.ldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.r14, asm.scratchRegister,0); // set up R14 as if it were a blx.
            // the stack slot +4  holds the return address of the caller originially pushed onto the stack


            //asm.addq(ARMV7.r13,8); // basicall we need to get the stack back to the state where it was
            //AUGasm.addq(ARMV7.r13,8); // basicall we need to get the stack back to the state where it was
            // on entry to this method, so that when we LDR R15 then we will
            // be able to push the LR back onto the stack etc.
            //asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.r13.asValue(), -8));
            //asm.ldr(ARMV7Assembler.ConditionFlag.Always,ARMV7.r15,ARMV7.r12,0);
            //AUGasm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.r13.asValue(), -8));
            //AUGasm.ldr(ARMV7Assembler.ConditionFlag.Always,ARMV7.r15,ARMV7.r12,0);


            // essentially the LDR does a jump to the trampolined to method.
            asm.insertForeverLoop();
            //asm.ret(0);


            byte[] code = asm.codeBuffer.close(true);
            final Type type = isInterface ? InterfaceTrampoline : VirtualTrampoline;
            return new Stub(type, stubName, frameSize, code, callPos, callSize, callee, registerRestoreEpilogueOffset);
        } else {
            throw FatalError.unimplemented();
        }
    }

    @PLATFORM(cpu = "armv7")
    private static void patchStaticTrampolineCallSiteARMV7(Pointer callSite) {
        if (VMOptions.verboseOption.verboseCompilation) {
            Log.println("ENTER-ARM patchStaticTrampoline");
        }
        CodePointer cpCallSite = CodePointer.from(callSite);
        if (VMOptions.verboseOption.verboseCompilation) {
            Log.print("TRAMPOLINE CALL SITE ");
            Log.println(cpCallSite);
        }

        final TargetMethod caller = cpCallSite.toTargetMethod();
        if (VMOptions.verboseOption.verboseCompilation) {
            Log.print("TRAMPOLINE CALLER ");
            Log.println(caller.toString());
        }

        final ClassMethodActor callee = caller.callSiteToCallee(cpCallSite);
        if (VMOptions.verboseOption.verboseCompilation) {
            Log.print("TRAMPOLINE CALLEE ");
            Log.println(callee.toString());
        }

        final CodePointer calleeEntryPoint = callee.makeTargetMethod(caller).getEntryPoint(caller.callEntryPoint);
        if (VMOptions.verboseOption.verboseCompilation) {
            Log.print("TRAMPOLINE ENTRYPOINT ");
            Log.println(calleeEntryPoint);
        }

        ARMTargetMethodUtil.mtSafePatchCallDisplacement(caller, cpCallSite, calleeEntryPoint);

        // remember calls from boot code region to baseline code cache
        if (Code.bootCodeRegion().contains(cpCallSite.toAddress()) && Code.getCodeManager().getRuntimeBaselineCodeRegion().contains(calleeEntryPoint.toAddress())) {
            if (VMOptions.verboseOption.verboseCompilation) {
                Log.println("TRAMPOLINE recordBootToBaseline");
            }

            CodeManager.recordBootToBaselineCaller(caller);
        }
        if (VMOptions.verboseOption.verboseCompilation) {
            Log.println("EXIT-ARM patchStaticTrampoline");
        }
    }

    @PLATFORM(cpu = "amd64")
    private static void patchStaticTrampolineCallSiteAMD64(Pointer callSite) {
        if (VMOptions.verboseOption.verboseCompilation) {
            Log.println("ENTER-AMD patchStaticTrampoline");
        }

        CodePointer cpCallSite = CodePointer.from(callSite);
        if (VMOptions.verboseOption.verboseCompilation) {
            Log.print("TRAMPOLINE CALL SITE ");
            Log.println(cpCallSite);
        }

        final TargetMethod caller = cpCallSite.toTargetMethod();
        if (VMOptions.verboseOption.verboseCompilation) {
            Log.print("TRAMPOLINE CALLER ");
            Log.println(caller.toString());
        }
        final ClassMethodActor callee = caller.callSiteToCallee(cpCallSite);
        if (VMOptions.verboseOption.verboseCompilation) {
            Log.print("TRAMPOLINE CALLEE ");
            Log.println(callee.toString());
        }

        final CodePointer calleeEntryPoint = callee.makeTargetMethod(caller).getEntryPoint(caller.callEntryPoint);
        if (VMOptions.verboseOption.verboseCompilation) {
            Log.print("TRAMPOLINE ENTRYPOINT ");
            Log.println(calleeEntryPoint);
        }
        AMD64TargetMethodUtil.mtSafePatchCallDisplacement(caller, cpCallSite, calleeEntryPoint);

        // remember calls from boot code region to baseline code cache
        if (Code.bootCodeRegion().contains(cpCallSite.toAddress()) && Code.getCodeManager().getRuntimeBaselineCodeRegion().contains(calleeEntryPoint.toAddress())) {
            CodeManager.recordBootToBaselineCaller(caller);
        }
        if (VMOptions.verboseOption.verboseCompilation) {
            Log.println("EXIT-AMD patchStaticTrampoline");
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
        } else if (platform().isa == ISA.ARM) {

            CiRegisterConfig registerConfig = registerConfigs.trampoline;
            ARMV7MacroAssembler asm = new ARMV7MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            int frameSize = target().alignFrameSize(csl.size);
            int frameToCSA = csl.frameOffsetToCSA;



            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }
            asm.push(ARMV7Assembler.ConditionFlag.Always, 1 << 14);      // SAVE ret address

            // compute the static trampoline call site
            CiRegister callSite = ARMV7.r8; /// was scratch but we use scratch so use r8
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP));
            //asm.movq(callSite, new CiAddress(WordUtil.archKind(), ARMV7.rsp.asValue()));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, callSite, ARMV7.r12, 0); // we need callSite and r12 to be different registers
            asm.subq(callSite, ARMTargetMethodUtil.RIP_CALL_INSTRUCTION_SIZE + 12); // this adjusts callSite to contain the correxct
            //   callSite address


            // now allocate the frame for this method

            asm.subq(ARMV7.r13, frameSize);

            // save all the callee save registers
            asm.save(csl, frameToCSA);

            CriticalMethod patchStaticTrampoline = new CriticalMethod(Stubs.class, "patchStaticTrampolineCallSiteARMV7", null);
            CiKind[] trampolineParameters = CiUtil.signatureToKinds(patchStaticTrampoline.classMethodActor);
            CiValue[] locations = registerConfig.getCallingConvention(JavaCall, trampolineParameters, target(), false).locations;

            // load the static trampoline call site into the first parameter register
            // APN ie it holds the address of the trampoline call site.
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, locations[0].asRegister(), callSite);

            asm.alignForPatchableDirectCall(); // we have this but everything is aligned ok for ARM32 bit ARMV7.
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = patchStaticTrampoline.classMethodActor;
            asm.call(); // 3 instructions = 12 bytes on ARMV7 ON AMD64-X86 this is an E8 CALL!
            int callSize = asm.codeBuffer.position() - callPos;

            // restore all parameter registers before returning
            int registerRestoreEpilogueOffset = asm.codeBuffer.position();
            asm.restore(csl, frameToCSA); // APN now follows X86 way ....

            // undo the frame
            asm.addq(ARMV7.r13, frameSize);

            // SAVe r8?
            //asm.push(ARMV7Assembler.ConditionFlag.Always, 1<< 8);

            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.r13.asValue()));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, callSite, asm.scratchRegister, 0);
            asm.subq(callSite, ARMTargetMethodUtil.RIP_CALL_INSTRUCTION_SIZE + 12);
            asm.setUpRegister(ARMV7.r12, new CiAddress(WordUtil.archKind(), ARMV7.r13.asValue()));
            //asm.movq(new CiAddress(WordUtil.archKind(), ARMV7.rsp.asValue()), callSite);

            // ok so this should patch the call site address?
        /*
	    THIS CODE PATCHES AN ADDRESS ON THE STACK THAT IS THE TARGET OF THE MOVW MOT ADD BLX CALL SEQUENCE, SO WE ARE OK TO POP
	    IT OFF THE STACK AND THEN THE BLX IS OK

	    NOTE FOR genDyanmicTrampoline,  WE MUST FAKE THE CORRECT $LR AND THEN DO THE RET, AS WE DO ***not**** ACTUALLY DO A BLX TO GET TO THE
	   ENTRY POINT
	    */
            asm.strImmediate(ARMV7Assembler.ConditionFlag.Always, 0, 0, 0, callSite, ARMV7.r12, 0);
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r12, callSite);
            //asm.flushicache(ARMV7.r12, 24);
            //asm.pop(ARMV7Assembler.ConditionFlag.Always, 1<< 8);
            asm.ret(0); // ret(0) is a C3 in X86

            String stubName = "strampoline";
            byte[] code = asm.codeBuffer.close(true);

            return new Stub(StaticTrampoline, stubName, frameSize, code, callPos, callSize, callee, registerRestoreEpilogueOffset);
        } else {
            throw FatalError.unimplemented();
        }
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
     * <p/>
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
        } else if (platform().isa == ISA.ARM) {

            /*
	    This needs to be aligned with what is expected to happen in Trap.java TrapFrameAccess.java and ARMTrapFrameAccess.java
            TODO Currently there seems to be an issue with the offsets etc and where we save things. We will need to get this right.

            check the information on ARM linux regarding what is returned and do
            something appropriate to attempt to recover from the error/issue causing
            the trap.
             */
            CiRegisterConfig registerConfig = registerConfigs.trapStub;
            ARMV7MacroAssembler asm = new ARMV7MacroAssembler(target(), registerConfig);
            CiCalleeSaveLayout csl = registerConfig.getCalleeSaveLayout();
            CiRegister latch = ARMSafepointPoll.LATCH_REGISTER;
            //CiRegister scratch = registerConfig.getScratchRegister();
            int frameSize = platform().target.alignFrameSize(csl.size);
            int frameToCSA = csl.frameOffsetToCSA;
            CiKind[] handleTrapParameters = CiUtil.signatureToKinds(Trap.handleTrap.classMethodActor);
            CiValue[] args = registerConfig.getCallingConvention(JavaCallee, handleTrapParameters, target(), false).locations;


            //asm.push(ARMV7Assembler.ConditionFlag.Always,1<<14); // we always push the LR
            asm.push(ARMV7Assembler.ConditionFlag.Always, 1 << 12);      // SAVE r12 ... our save/restore uses r12 so we must push it here
            // this will be overwritten with the RETURN  ADDRESS of the trapping instruction
            asm.push(ARMV7Assembler.ConditionFlag.Always, 1 << 12);
            asm.mrsReadAPSR(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12);
            asm.push(ARMV7Assembler.ConditionFlag.Always, 1 << 12);

            // NOW allocate the frame for this method (note TWO wordas of which WERE allocated by the second push above)
            asm.subq(ARMV7.r13, frameSize - 8/*4*/);

            // save all the callee save registers
            asm.save(csl, frameToCSA); // NOTE our save/restore trashes r12 ... that is why we push r12

            // Now that we have saved all general purpose registers (including the scratch register),
            // store the value of the latch register from the thread locals into the trap frame
            //asm.movq(scratch, new CiAddress(WordUtil.archKind(), latch.asValue(), TRAP_LATCH_REGISTER.offset));
            //asm.movq(new CiAddress(WordUtil.archKind(), ARMV7.rsp.asValue(), frameToCSA + csl.offsetOf(latch)), scratch);
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), latch.asValue(), TRAP_LATCH_REGISTER.offset));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, ARMV7.r12, 0);
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.r13.asValue(), frameToCSA + csl.offsetOf(latch)));
            asm.str(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, ARMV7.r12, 0);

            // r8 has the value we want
            // write the return address pointer to the end of the frame
            //asm.movq(scratch, new CiAddress(WordUtil.archKind(), latch.asValue(), TRAP_INSTRUCTION_POINTER.offset));
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), latch.asValue(), TRAP_INSTRUCTION_POINTER.offset));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, ARMV7.r12, 0);
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.r13.asValue(), frameSize /*+ 4*/));// we have sdaved r12 so it is one slot past the end of the frame
            asm.str(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, ARMV7.r12, 0);

            // BEGIN  DIRTY HACK

            // WE READ THE fpsrc AND IF A DIV ZERO HAS OCCURED
            // WE CHANGE THE TRAP NUMBER
            /*asm.vmrs(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12);
            asm.movw(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, 2); // DZC is updated to 1 -- means it is a  divide by zero
            asm.and(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r12, ARMV7.r8, ARMV7.r12, 0, 0);
            asm.cmp(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, ARMV7.r12, 0, 0);
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), latch.asValue(), TRAP_NUMBER.offset));
            asm.mov32BitConstant(ARMV7.r8, 3); // TRAP 3 is arithmetic
            asm.str(ARMV7Assembler.ConditionFlag.Equal, ARMV7.r8, ARMV7.r12, 0);
            asm.mov32BitConstant(ARMV7.r8, 0xfffffd00);
            asm.vmrs(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12);
            asm.and(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r8, ARMV7.r8, ARMV7.r12, 0, 0);
            asm.vmsr(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8);
	    */
            // WE ALSO NEED TO RESET THE divz AS above --- ie clear and FPExceptions
            // END DIRTY HACK

            // load the trap number from the thread locals into the first parameter register
            //asm.movq(args[0].asRegister(), new CiAddress(WordUtil.archKind(), latch.asValue(), TRAP_NUMBER.offset));
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), latch.asValue(), TRAP_NUMBER.offset));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, args[0].asRegister(), ARMV7.r12, 0);

            // also save the trap number into the trap frame
            //asm.movq(new CiAddress(WordUtil.archKind(), ARMV7.rsp.asValue(), frameToCSA + ARMTrapFrameAccess.TRAP_NUMBER_OFFSET), args[0].asRegister());
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.r13.asValue(), frameToCSA + ARMTrapFrameAccess.TRAP_NUMBER_OFFSET));
            asm.str(ARMV7Assembler.ConditionFlag.Always, args[0].asRegister(), asm.scratchRegister, 0);

            // load the trap frame pointer into the second parameter register
            //asm.leaq(args[1].asRegister(), new CiAddress(WordUtil.archKind(), ARMV7.rsp.asValue(), frameToCSA));
            asm.leaq(args[1].asRegister(), new CiAddress(WordUtil.archKind(), ARMV7.r13.asValue(), frameToCSA));

            // load the fault address from the thread locals into the third parameter register
            //asm.movq(args[2].asRegister(), new CiAddress(WordUtil.archKind(), latch.asValue(), TRAP_FAULT_ADDRESS.offset));
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), latch.asValue(), TRAP_FAULT_ADDRESS.offset));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, args[2].asRegister(), asm.scratchRegister, 0);

            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = Trap.handleTrap.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;
            asm.restore(csl, frameToCSA);

            // now pop the flags register off the stack before returning
            // asm.addq(ARMV7.rsp, frameSize - 8);
            // asm.popfq();
            // APN maybe I should have saved and restored the FLAGS?
            // my understanding is that normal handler code will do this?
            // Will r14 be correctly set to the appropriate return address?
            //asm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r15, ARMV7.r14);
            asm.addq(ARMV7.r13, frameSize - 8/*4*/); // added
            asm.pop(ARMV7Assembler.ConditionFlag.Always, 1 << 12);// pops flags so we need to do ...
            asm.msrWriteAPSR(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12);
            asm.pop(ARMV7Assembler.ConditionFlag.Always, 1 << 12); // POP scratch
            asm.ret(0);

            byte[] code = asm.codeBuffer.close(true);

            return new Stub(TrapStub, "trapStub", frameSize, code, callPos, callSize, callee, -1);
        } else {
            throw FatalError.unimplemented();
        }
    }

    /**
     * Unwinds the current thread execution state to a given (caller) frame and instruction pointer.
     * The frame must be an existing caller frame on the stack and the instruction pointer
     * must be a valid address within the code associated with the frame.
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

    //TODO: Fix ARM version of it
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
        } else if (platform().isa == ISA.ARM) {
            CiRegisterConfig registerConfig = MaxineVM.vm().stubs.registerConfigs.standard;
            ARMV7MacroAssembler asm = new ARMV7MacroAssembler(target(), registerConfig);
            int frameSize = platform().target.alignFrameSize(0);

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }

            asm.push(ARMV7Assembler.ConditionFlag.Always, 1 << 14);

            CiValue[] args = unwindArgs;
            assert args.length == 3 || args.length == 4;

            CiRegister pc = args[0].asRegister();
            CiRegister sp = args[1].asRegister();
            CiRegister fp = args[2].asRegister();

            String name = "unwindStub";
            if (args.length == 4) {
                CiValue retValue = args[3];
                CiRegister reg = null;
                CiAddress stackAddr = null;
                CiKind kind = retValue.kind.stackKind();
                name = "unwind" + kind.name() + "Stub";
                //Log.print("UNWIND ");
                //Log.println(args[3].toString());
                switch (kind) {

                    case Long:
                        stackAddr = new CiAddress(kind, ARMV7.RSP, ((CiStackSlot) retValue).index() * Word.size());
                        asm.setUpScratch(stackAddr);
                        asm.ldrd(ARMV7Assembler.ConditionFlag.Always, registerConfig.getReturnRegister(CiKind.Long), ARMV7.r12, 0);
                        break;

                    case Int:
                    case Object:
                        reg = retValue.asRegister();
                        asm.mov(ARMV7Assembler.ConditionFlag.Always, false, registerConfig.getReturnRegister(CiKind.Int), reg);
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
            // APN not sure about it, but it seems to be ok.
            // DEBUG MARKER
            asm.mov32BitConstant(ConditionFlag.Always, ARMV7.r12, 0xfadad0d0);


            // Push 'pc' to the handler's stack frame and update RSP to point to the pushed value.
            // When the RET instruction is executed, the pushed 'pc' will be popped from the stack
            // and the stack will be in the correct state for the handler.
            asm.subq(sp, Word.size());
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), sp.asValue()));
            asm.str(ARMV7Assembler.ConditionFlag.Always, pc, asm.scratchRegister, 0);
            //asm.movq(new CiAddress(WordUtil.archKind(), sp.asValue()), pc);
            //asm.movq(ARMV7.rbp, fp);
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r11, fp);
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r13, sp);
            //asm.movq(ARMV7.rsp, sp);
            asm.ret(0);
            byte[] code = asm.codeBuffer.close(true);
            return new Stub(UnwindStub, name, frameSize, code, -1, -1, null, -1);
        } else {
            throw FatalError.unimplemented();
        }
    }

    /**
     * Expands the stack by a given amount and then calls {@link Deoptimization#unroll(Info)}.
     * The stack expansion is required to fit the deoptimized frames encoded in {@code info}.
     *
     * @param info      the argument to pass onto {@link Deoptimization#unroll(Info)}
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
        } else if (platform().isa == ISA.ARM) {
            CiRegisterConfig registerConfig = MaxineVM.vm().stubs.registerConfigs.standard;
            ARMV7MacroAssembler asm = new ARMV7MacroAssembler(target(), registerConfig);
            int frameSize = platform().target.alignFrameSize(0);

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }
            asm.push(ARMV7Assembler.ConditionFlag.Always, 1 << 14);


            asm.sub(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r13, ARMV7.r13, ARMV7.r1, 0, 0); // rsi is argument (second)?

            CriticalMethod unroll = new CriticalMethod(Deoptimization.class, "unroll", null);
            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = unroll.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;
            Label forever = new Label();
            asm.bind(forever);
            asm.mov32BitConstant(ConditionFlag.Always, ARMV7.r12, 0x50115011);
            asm.branch(forever);

            // Should never reach here
            asm.hlt();

            byte[] code = asm.codeBuffer.close(true);
            return new Stub(UnrollStub, "unrollStub", frameSize, code, callPos, callSize, callee, -1);
        } else {
            throw FatalError.unimplemented();
        }
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

        public ARMV7DeoptStubPatch(int pos, CriticalMethod runtimeRoutine, Stub stub) {
            this.pos = pos;
            this.runtimeRoutine = runtimeRoutine;
            this.stub = stub;
        }

        @Override
        void apply() {
            Pointer patchAddr = stub.codeAt(pos).toPointer();
            //patchAddr.writeLong(0, runtimeRoutine.address().toLong());
            patchAddr.writeInt(0, runtimeRoutine.address().toInt());
            if (VMOptions.verboseOption.verboseCompilation) {
                Log.print("!!!!!!!!!!!!!!MODS NEEDED!!!!!!!!!! DEOPT PATCH ADDR ");
                Log.println(patchAddr);
            }
        }
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

	                asm.insertForeverLoop();

            String runtimeRoutineName = "deoptimize" + kind.name();
            final CriticalMethod runtimeRoutine;
            try {
                runtimeRoutine = new CriticalMethod(Deoptimization.class, runtimeRoutineName, null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
            } catch (NoSuchMethodError e) {
                // No deoptimization stub for kind
                return null;
            }

            asm.push(ARMV7Assembler.ConditionFlag.Always, 1 << 14);
            CiKind[] params = CiUtil.signatureToKinds(runtimeRoutine.classMethodActor);
            CiValue[] args = registerConfig.getCallingConvention(JavaCall, params, target(), false).locations;
            if (!kind.isVoid()) {
                // Copy return value into arg 4
                // this must be on the stack for ARM?

                // APN
                 /*
                 If we have 5th register, then it has to go on the stack? What if we have some integer and some
                  floating point? do we need to do the copy.
                 */
                // CiRegister arg4 = args[4].asRegister();
                CiAddress arg4;
                CiRegister returnRegister = registerConfig.getReturnRegister(kind);


                switch (kind) {
                    case Byte:
                    case Boolean:
                    case Short:
                    case Char:
                    case Int:
                    case Object:
                        assert args[4].isRegister() == false;

                        arg4 = new CiAddress(kind, ARMV7.RSP, ((CiStackSlot) args[4]).index() * 4);
                        asm.setUpScratch(arg4);
                        asm.ldr(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, asm.scratchRegister, 0);
                        // TODO NEEDS TO BE STOREd ON THE STACK
                        break;

                    case Long:
                        assert args[4].isRegister() == false;

                        arg4 = new CiAddress(kind, ARMV7.RSP, ((CiStackSlot) args[4]).index() * 4);
                        asm.setUpScratch(arg4);
                        asm.ldrd(ARMV7Assembler.ConditionFlag.Always, ARMV7.r8, ARMV7.r12, 0);
                        // TODO NEEDS TO BE STORED ON THE STACK
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
            // APN honestly believe this is wrong ....
            // we need arg0 to be returnvalue ...

            // Copy original return address into arg 0 (i.e. 'ip')
            CiRegister arg0 = args[0].asRegister();
            //asm.movq(arg0, new CiAddress(WordUtil.archKind(), ARMV7.RSP, DEOPT_RETURN_ADDRESS_OFFSET));
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP, DEOPT_RETURN_ADDRESS_OFFSET));
            asm.ldr(ARMV7Assembler.ConditionFlag.Always, arg0, asm.scratchRegister, 0);
            //asm.mov(ARMV7Assembler.ConditionFlag.Always, false, arg0, ARMV7.r12);
            // Copy original stack pointer into arg 1 (i.e. 'sp')
            CiRegister arg1 = args[1].asRegister();
            //asm.movq(arg1, ARMV7.rsp);
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, arg1, ARMV7.r13);

            // Copy original frame pointer into arg 2 (i.e. 'sp')
            CiRegister arg2 = args[2].asRegister();
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, arg2, ARMV7.r11);

            // Zero arg 3 (i.e. 'csa')
            CiRegister arg3 = args[3].asRegister();
            asm.xorq(arg3, arg3);

            // Allocate 2 extra stack slots ? one in ARM?
            asm.subq(ARMV7.r13, 8);

            // Put original return address into high slot
            //asm.movq(new CiAddress(WordUtil.archKind(), ARMV7.r13, 4), arg0);
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP, 4));
            asm.str(ARMV7Assembler.ConditionFlag.Always, arg0, ARMV7.r12, 0); // might be the wrong way round


            // Put deopt method entry point into low slot
            //CiRegister scratch = registerConfig.getScratchRegister();
            //asm.movq(scratch, 0xFFFFFFFFFFFFFFFFL);
            asm.movw(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, 0xffff); // is r8 free ?
            final int patchPos = asm.codeBuffer.position() - 4; // as we have two by 4 byte instructions to patch
            asm.movt(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, 0xffff);
            // APN ok not sure if we have spare registers
            // if we return? who does the restore?
            asm.setUpRegister(ARMV7.r8, new CiAddress(WordUtil.archKind(), ARMV7.RSP));


            asm.str(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, ARMV7.r8, 0);
            Label forever = new Label();
            asm.bind(forever);
            asm.mov32BitConstant(ConditionFlag.Always, ARMV7.r12, 0xfeeff00f);
            asm.branch(forever);
            asm.ret(0);

            String stubName = runtimeRoutineName + "Stub";
            byte[] code = asm.codeBuffer.close(true);
            final Stub stub = new Stub(DeoptStub, stubName, frameSize, code, -1, 0, null, -1);

            ARMV7DeoptStubPatch patch = new ARMV7DeoptStubPatch(patchPos, runtimeRoutine, stub);
            runtimeInits = Arrays.copyOf(runtimeInits, runtimeInits.length + 1);
            runtimeInits[runtimeInits.length - 1] = patch;

            return stub;
        } else {
            throw FatalError.unimplemented();
        }
    }

    /**
     * Generates a stub to deoptimize an method upon returning to it. This stub creates a new frame for saving the registers
     * specified by the {@link CiCalleeSaveLayout} of a given register configuration.
     *
     * @param kind               the return value kind or {@code null} if generating the stub used when returning from a safepoint trap
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
                CiStackSlot ss = (CiStackSlot) registerConfigs.compilerStub.getCallingConvention(JavaCall, new CiKind[]{kind}, target(), true).locations[0];
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
             *   int3                                          // should not reach here
             */
            CiCalleeSaveLayout csl = registerConfig.csl;
            ARMV7MacroAssembler asm = new ARMV7MacroAssembler(target(), registerConfig);
            int frameSize = platform().target.alignFrameSize(csl.size);
            int cfo = frameSize + 4; // APN 4 bytes for ARM? Caller frame offset

	    asm.insertForeverLoop();
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
            asm.subq(ARMV7.r13, frameSize + 4);
            // save all the callee save registers
            asm.save(csl, csl.frameOffsetToCSA);

            CiKind[] params = CiUtil.signatureToKinds(runtimeRoutine.classMethodActor);
            CiValue[] args = registerConfig.getCallingConvention(JavaCall, params, target(), false).locations;
            if (kind != null && !kind.isVoid()) {
                // Copy return value into arg 4

                //CiRegister arg4 = args[4].asRegister();
                CiAddress arg4;
                switch (kind) {
                    case Byte:
                    case Boolean:
                    case Short:
                    case Char:
                    case Int:
                    case Object:
                        assert args[4].isRegister() == false;

                        arg4 = new CiAddress(kind, ARMV7.RSP, ((CiStackSlot) args[4]).index() * 4);
                        asm.setUpScratch(arg4);
                        asm.ldr(ARMV7Assembler.ConditionFlag.Always, 0, 0, 0, ARMV7.r12, ARMV7.r12, ARMV7.r12, 0, 0);
                        asm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r0, ARMV7.r12);

                        break;
                    case Long:
                        assert args[4].isRegister() == false;

                        // broken needs TWO registers TODO
                        arg4 = new CiAddress(kind, ARMV7.RSP, ((CiStackSlot) args[4]).index() * 4);
                        asm.setUpScratch(arg4);
                        asm.ldr(ARMV7Assembler.ConditionFlag.Always, 0, 0, 0, ARMV7.r12, ARMV7.r12, ARMV7.r12, 0, 0);
                        asm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r0, ARMV7.r12);

                        break;

                    case Float:
                        CiRegister tmp = args[4].asRegister();

                        if (tmp.number <= 15) {
                            asm.push(ARMV7Assembler.ConditionFlag.Always, 1 << tmp.number);
                        } else {
                            asm.vpush(ARMV7Assembler.ConditionFlag.Always, tmp, tmp, CiKind.Float, CiKind.Float);
                        }


                        break;
                    case Double:
                        CiRegister tmp2arg4 = args[4].asRegister();
                        // aPN TODO this is broken beyond belief
                        // we will be trying to move a FP reg into a core register?
                        //        assumptions on where to put float/doubles and if they go on the stack or if they
                        // are in registers is all hazy so dont expect this to work first time.
                        // Aslo assumptions about index slot sizes are a bit broken and offsets as some types are bigger than
                        // 32bits --- long/double so multiplying by 4 will give an incorrect offset in general.
                        // broken we need TWO registers so this needs VLDR!!!!!!!!!
                        if (tmp2arg4.number <= 15) {
                            asm.push(ARMV7Assembler.ConditionFlag.Always, (1 << tmp2arg4.number) | (1 << (tmp2arg4.number + 1)));
                        } else {
                            asm.vpush(ARMV7Assembler.ConditionFlag.Always, tmp2arg4, tmp2arg4, CiKind.Double, CiKind.Double);
                        }

                        // aPN TODO this is broken beyond belief
                        // we will be trying to move a FP reg into a core register?
                        //

                        break;

                    default:
                        throw new InternalError("Unexpected parameter kind: " + kind);
                }

                /*CiAddress arg4 = new CiAddress(kind,ARMV7.RSP,((CiStackSlot)args[4]).index()*4);
                asm.setUpScratch(arg4);
                //CiRegister arg4 = args[4].asRegister();
                CiStackSlot ss = (CiStackSlot) registerConfigs.compilerStub.getCallingConvention(JavaCall, new CiKind[] {kind}, target(), true).locations[0];
                assert ss.index() == 1 : "compiler stub return value slot index has changed?";
                CiAddress src = new CiAddress(kind, ARMV7.RSP, cfo + (ss.index() * 4));

                if (kind.isFloat()) {
                    //asm.movflt(arg4, src);
                    // TODO throw (new InternalError("floats not implemented"));
                } else if (kind.isDouble()) {
                   // asm.movdbl(arg4, src);
                   // TODO  throw (new InternalError("floats not implemented"));

                } else {
                    asm.setUpScratch(src);
                    asm.ldr(ARMV7Assembler.ConditionFlag.Always, 0, 0, 0, ARMV7.r8, ARMV7.r12, ARMV7.r12, 0, 0);
                    asm.setUpScratch(arg4);
                    asm.str(ARMV7Assembler.ConditionFlag.Always, 0, 0, 0, ARMV7.r8, ARMV7.r12, ARMV7.r12, 0, 0);
                  //  asm.movq(arg4, src);
                }
                */
            }


            // Copy original return address into arg 0 (i.e. 'ip')
            CiRegister arg0 = args[0].asRegister();
            //asm.movq(arg0, new CiAddress(WordUtil.archKind(), ARMV7.RSP, cfo + DEOPT_RETURN_ADDRESS_OFFSET));
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP, cfo + DEOPT_RETURN_ADDRESS_OFFSET));
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, arg0, ARMV7.r12);
            // Copy original stack pointer into arg 1 (i.e. 'sp')
            CiRegister arg1 = args[1].asRegister();
            //asm.leaq(arg1, new CiAddress(WordUtil.archKind(), ARMV7.RSP, cfo));
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP, cfo));
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, arg1, ARMV7.r12);

            // Copy original frame pointer into arg 2 (i.e. 'sp')
            CiRegister arg2 = args[2].asRegister();
            //asm.movq(arg2, ARMV7.rbp);
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, arg2, ARMV7.r11);

            // Copy callee save area into arg3 (i.e. 'csa')
            CiRegister arg3 = args[3].asRegister();
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, arg3, ARMV7.r13);
            //asm.movq(arg3, ARMV7.rsp);

            // Patch return address of deopt stub frame to look
            // like it was called by frame being deopt'ed.

            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP, frameSize));
            asm.str(ARMV7Assembler.ConditionFlag.Always, 0, 0, 0, arg0, ARMV7.r12, ARMV7.r12, 0, 0);
            //asm.movq(new CiAddress(WordUtil.archKind(), ARMV7.RSP, frameSize), arg0);


            // Call runtime routine
            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // should never reach here
            asm.int3();
            Label forever = new Label();
            asm.bind(forever);
            asm.mov32BitConstant(ConditionFlag.Always, ARMV7.r12, 0xc5a0c5a0);
            asm.branch(forever);


            String stubName = runtimeRoutineName + "StubWithCSA";
            byte[] code = asm.codeBuffer.close(true);
            Type stubType = kind == null ? DeoptStubFromSafepoint : DeoptStubFromCompilerStub;
            return new Stub(stubType, stubName, frameSize, code, callPos, callSize, runtimeRoutine.classMethodActor, -1);
        } else {
            throw FatalError.unimplemented();
        }
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

            CiValue[] args = registerConfig.getCallingConvention(JavaCall, new CiKind[]{WordUtil.archKind(), WordUtil.archKind(), WordUtil.archKind(), WordUtil.archKind()}, target(), false).locations;

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

            asm.subq(ARMV7.rsp, frameSize);

            // save all the registers
            asm.save(csl, frameToCSA);

            String name = "uncommonTrap";
            final CriticalMethod uncommonTrap = new CriticalMethod(Deoptimization.class, name, null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);

            //CiValue[] args = registerConfig.getCallingConvention(JavaCall, new CiKind[] {WordUtil.archKind(), WordUtil.archKind(), WordUtil.archKind(), WordUtil.archKind()}, target(), false).locations;

            // Copy callee save area address into arg 0 (i.e. 'csa')
            //CiRegister arg0 = args[0].asRegister();
            //asm.leaq(arg0, new CiAddress(WordUtil.archKind(), ARMV7.RSP, frameToCSA));

            // Copy return address into arg 1 (i.e. 'ip')
            //CiRegister arg1 = args[1].asRegister();
            //asm.movq(arg1, new CiAddress(WordUtil.archKind(), ARMV7.RSP, frameSize));

            // Copy stack pointer into arg 2 (i.e. 'sp')
            //CiRegister arg2 = args[2].asRegister();
            //asm.leaq(arg2, new CiAddress(WordUtil.archKind(), ARMV7.RSP, frameSize + 8));

            // Copy original frame pointer into arg 3 (i.e. 'fp')
            //CiRegister arg3 = args[3].asRegister();
            //asm.movq(arg3, ARMV7.rbp);


            asm.alignForPatchableDirectCall();
            int callPos = asm.codeBuffer.position();
            ClassMethodActor callee = uncommonTrap.classMethodActor;
            asm.call();
            int callSize = asm.codeBuffer.position() - callPos;

            // Should never reach here
            int registerRestoreEpilogueOffset = asm.codeBuffer.position();
            Label forever = new Label();
            asm.bind(forever);
            asm.mov32BitConstant(ConditionFlag.Always, ARMV7.r12, 0xffffffff);
            asm.blx(ARMV7.r12); //expect it to crash
            asm.branch(forever);

            asm.hlt();

            String stubName = name + "Stub";
            byte[] code = asm.codeBuffer.close(true);
            return new Stub(UncommonTrapStub, stubName, frameSize, code, callPos, callSize, callee, registerRestoreEpilogueOffset);
        } else {
            throw FatalError.unimplemented();
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
        return calleeSaveStart.plus(csl.offsetOf(registerConfig.getScratchRegister())).getInt();
    }
}
