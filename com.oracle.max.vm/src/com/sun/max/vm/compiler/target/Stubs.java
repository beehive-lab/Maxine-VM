/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.VMOptions.*;
import static com.sun.max.vm.compiler.CallEntryPoint.*;
import static com.sun.max.vm.compiler.CompilationScheme.Static.*;
import static com.sun.max.vm.compiler.deopt.Deoptimization.*;
import static com.sun.max.vm.compiler.target.TargetMethod.Flavor.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.util.*;

import com.oracle.max.asm.target.amd64.*;
import com.sun.cri.bytecode.Bytecodes.Infopoints;
import com.sun.cri.ci.*;
import com.sun.cri.util.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.CompilationScheme.CompilationFlag;
import com.sun.max.vm.compiler.deopt.*;
import com.sun.max.vm.compiler.deopt.Deoptimization.Info;
import com.sun.max.vm.compiler.target.TargetMethod.Flavor;
import com.sun.max.vm.compiler.target.amd64.*;
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
     */
    public Stub deoptStub(CiKind returnValueKind) {
        return deoptStubs[returnValueKind.ordinal()];
    }

    /**
     * Determines if a given target method is one of the deoptimization stubs.
     */
    public boolean isDeoptStub(TargetMethod tm) {
        if (!(tm instanceof Stub)) {
            return false;
        }
        for (Stub stub : deoptStubs) {
            if (isHosted()) {
                // Object identity of stubs is not preserved when in the Inspector;
                // use region name instead
                if (stub != null && tm.regionName().equals(stub.regionName())) {
                    return true;
                }
            } else {
                if (stub == tm) {
                    return true;
                }
            }
        }
        return false;
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
                // TODO: Compute prologue size properly
                prologueSize = vmConfig().needsAdapters() ? 8 : 0;
                resolveVirtualCall = new CriticalMethod(Stubs.class, "resolveVirtualCall", null);
                resolveInterfaceCall = new CriticalMethod(Stubs.class, "resolveInterfaceCall", null);
                resolveVirtualCallArgs = registerConfigs.trampoline.getCallingConvention(JavaCall,
                                CRIUtil.signatureToKinds(resolveVirtualCall.classMethodActor.signature(), CiKind.Object), target()).locations;
                resolveInterfaceCallArgs = registerConfigs.trampoline.getCallingConvention(JavaCall,
                                CRIUtil.signatureToKinds(resolveInterfaceCall.classMethodActor.signature(), CiKind.Object), target()).locations;
                staticTrampoline = genStaticTrampoline();
                trapStub = genTrapStub();

                CriticalMethod unroll = new CriticalMethod(Stubs.class, "unroll", null);
                CiValue[] unrollArgs = registerConfigs.globalStub.getCallingConvention(JavaCall,
                                CRIUtil.signatureToKinds(unroll.classMethodActor.signature(), null), target()).locations;
                unroll.classMethodActor.targetState = genUnroll(unrollArgs);

                for (CiKind kind : CiKind.VALUES) {
                    deoptStubs[kind.ordinal()] = genDeoptStub(kind);

                    String name = "unwind";
                    if (!kind.isVoid()) {
                        name = name + kind.name();
                    }
                    try {
                        CriticalMethod unwind = new CriticalMethod(Stubs.class, name, null);
                        CiValue[] unwindArgs = registerConfigs.globalStub.getCallingConvention(JavaCall,
                                        CRIUtil.signatureToKinds(unwind.classMethodActor.signature(), null), target()).locations;
                        unwind.classMethodActor.targetState = genUnwind(unwindArgs);
                    } catch (NoSuchMethodError e) {
                        // No unwind method for this kind
                    }
                }
            }
        }
    }


    public final RegisterConfigs registerConfigs;

    private int prologueSize = -1;

    public synchronized Address interfaceTrampoline(int iIndex) {
        if (interfaceTrampolines.size() <= iIndex) {
            for (int i = interfaceTrampolines.size(); i <= iIndex; i++) {
                String stubName = "itrampoline<" + i + ">";
                if (verboseOption.verboseCompilation) {
                    VmThread thread = VmThread.current();
                    Log.println(thread.getName() + "[id=" + thread.id() + "]: Creating stub " + stubName);
                }
                Stub stub = genDynamicTrampoline(i, true, stubName);
                interfaceTrampolines.add(stub);
                if (verboseOption.verboseCompilation) {
                    VmThread thread = VmThread.current();
                    Log.println(thread.getName() + "[id=" + thread.id() + "]: Created stub " + stub.regionName());
                }
            }
        }
        return VTABLE_ENTRY_POINT.in(interfaceTrampolines.get(iIndex));
    }

    public synchronized Address virtualTrampoline(int vTableIndex) {
        if (virtualTrampolines.size() <= vTableIndex) {
            for (int i = virtualTrampolines.size(); i <= vTableIndex; i++) {
                String stubName = "vtrampoline<" + i + ">";
                if (verboseOption.verboseCompilation) {
                    VmThread thread = VmThread.current();
                    Log.println(thread.getName() + "[id=" + thread.id() + "]: Creating stub " + stubName);
                }
                Stub stub = genDynamicTrampoline(i, false, stubName);
                virtualTrampolines.add(stub);
                if (verboseOption.verboseCompilation) {
                    VmThread thread = VmThread.current();
                    Log.println(thread.getName() + "[id=" + thread.id() + "]: Created stub " + stub.regionName());
                }
            }
        }
        return VTABLE_ENTRY_POINT.in(virtualTrampolines.get(vTableIndex));
    }

    private static Address adjustEntryPointForCaller(Address virtualDispatchEntryPoint, Pointer pcInCaller) {
        final TargetMethod caller = Code.codePointerToTargetMethod(pcInCaller);
        CallEntryPoint callEntryPoint = caller.callEntryPoint;
        return virtualDispatchEntryPoint.plus(callEntryPoint.offset() - VTABLE_ENTRY_POINT.offset());
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
        final Hub hub = ObjectAccess.readHub(receiver);
        final VirtualMethodActor selectedCallee = hub.classActor.getVirtualMethodActorByVTableIndex(vTableIndex);
        if (selectedCallee.isAbstract()) {
            throw new AbstractMethodError();
        }
        final Address vtableEntryPoint = compile(selectedCallee, CompilationFlag.NONE).getEntryPoint(VTABLE_ENTRY_POINT).asAddress();
        hub.setWord(vTableIndex, vtableEntryPoint);
        return adjustEntryPointForCaller(vtableEntryPoint, pcInCaller);
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
        final Hub hub = ObjectAccess.readHub(receiver);
        final VirtualMethodActor selectedCallee = hub.classActor.getVirtualMethodActorByIIndex(iIndex);
        if (selectedCallee.isAbstract()) {
            throw new AbstractMethodError();
        }
        final Address itableEntryPoint = compile(selectedCallee, CompilationFlag.NONE).getEntryPoint(VTABLE_ENTRY_POINT).asAddress();
        hub.setWord(hub.iTableStartIndex + iIndex, itableEntryPoint);
        return adjustEntryPointForCaller(itableEntryPoint, pcInCaller);
    }

    private Stub genDynamicTrampoline(int index, boolean isInterface, String stubName) {
        delayedInit();
        if (platform().isa == ISA.AMD64) {
            CiRegisterConfig registerConfig = registerConfigs.trampoline;
            AMD64MacroAssembler asm = new AMD64MacroAssembler(target(), registerConfig);
            CiCalleeSaveArea csa = registerConfig.getCalleeSaveArea();
            int frameSize = target().alignFrameSize(csa.size);
            final int frameToCSA = 0;

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }

            // now allocate the frame for this method
            asm.subq(AMD64.rsp, frameSize);

            // save the index in the scratch register. This register is then callee-saved
            // so that the stack walker can find it.
            asm.movl(registerConfig.getScratchRegister(), index);

            // save all the callee save registers
            asm.save(csa, frameToCSA);

            CiValue[] args = isInterface ? resolveInterfaceCallArgs : resolveVirtualCallArgs;

            // the receiver is already in the first arg register
            //asm.movq(locations[0].asRegister(), locations[0].asRegister());

            // load the index into the second arg register
            asm.movl(args[1].asRegister(), index);

            // load the return address into the third arg register
            asm.movq(args[2].asRegister(), new CiAddress(CiKind.Word, AMD64.rsp.asValue(), frameSize));

            asm.alignCall();
            int callPosition = asm.codeBuffer.position();
            ClassMethodActor callee = isInterface ? resolveInterfaceCall.classMethodActor : resolveVirtualCall.classMethodActor;
            asm.call();

            // Put the entry point of the resolved method on the stack just below the
            // return address of the trampoline itself. By adjusting RSP to point at
            // this second return address and executing a 'ret' instruction, execution
            // continues in the resolved method as if it was called by the trampoline's
            // caller which is exactly what we want.
            CiRegister returnReg = registerConfig.getReturnRegister(CiKind.Word);
            asm.movq(new CiAddress(CiKind.Word, AMD64.rsp.asValue(), frameSize - 8), returnReg);

            // Restore all parameter registers before returning
            int registerRestoreEpilogueOffset = asm.codeBuffer.position();
            asm.restore(csa, frameToCSA);

            // Adjust RSP as mentioned above and do the 'ret' that lands us in the
            // trampolined-to method.
            asm.addq(AMD64.rsp, frameSize - 8);
            asm.ret(0);

            Flavor flavor = isInterface ? InterfaceTrampoline : VirtualTrampoline;
            byte[] code = asm.codeBuffer.close(true);

            return new Stub(flavor, stubName, frameSize, code, callPosition, callee, registerRestoreEpilogueOffset);
        }
        throw FatalError.unimplemented();
    }

    @PLATFORM(cpu = "amd64")
    private static void patchStaticTrampolineCallSiteAMD64(Pointer callSite) {
        final TargetMethod caller = Code.codePointerToTargetMethod(callSite);

        final ClassMethodActor callee = caller.callSiteToCallee(callSite);

        // Use the caller's entry point to get the correct entry point.
        final Address calleeEntryPoint = compile(callee, CompilationFlag.NONE).getEntryPoint(caller.callEntryPoint).asAddress();
        AMD64TargetMethodUtil.mtSafePatchCallDisplacement(caller, callSite, calleeEntryPoint);
    }

    /**
     * Generates a stub that links a call to a method whose actor is available in
     * data {@linkplain TargetMethod#callSiteToCallee(Address) associated} with the call site.
     * The stub also saves and restores all the callee-saved registers specified in the
     * {@linkplain RegisterConfigs#trampoline trampoline} register configuration.
     */
    @HOSTED_ONLY
    private Stub genStaticTrampoline() {
        if (platform().isa == ISA.AMD64) {
            CiRegisterConfig registerConfig = registerConfigs.trampoline;
            AMD64MacroAssembler asm = new AMD64MacroAssembler(target(), registerConfig);
            CiCalleeSaveArea csa = registerConfig.getCalleeSaveArea();
            int frameSize = target().alignFrameSize(csa.size);
            int frameToCSA = 0;

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }

            // compute the static trampoline call site
            CiRegister callSite = registerConfig.getScratchRegister();
            asm.movq(callSite, new CiAddress(CiKind.Word, AMD64.rsp.asValue()));
            asm.subq(callSite, AMD64TargetMethodUtil.RIP_CALL_INSTRUCTION_SIZE);

            // now allocate the frame for this method
            asm.subq(AMD64.rsp, frameSize);

            // save all the callee save registers
            asm.save(csa, frameToCSA);

            CriticalMethod patchStaticTrampoline = new CriticalMethod(Stubs.class, "patchStaticTrampolineCallSiteAMD64", null);
            CiKind[] trampolineParameters = {CiKind.Object};
            CiValue[] locations = registerConfig.getCallingConvention(JavaCall, trampolineParameters, target()).locations;

            // load the static trampoline call site into the first parameter register
            asm.movq(locations[0].asRegister(), callSite);

            asm.alignCall();
            int callPosition = asm.codeBuffer.position();
            ClassMethodActor callee = patchStaticTrampoline.classMethodActor;
            asm.call();

            // restore all parameter registers before returning
            int registerRestoreEpilogueOffset = asm.codeBuffer.position();
            asm.restore(csa, frameToCSA);

            // undo the frame
            asm.addq(AMD64.rsp, frameSize);

            // patch the return address to re-execute the static call
            asm.movq(callSite, new CiAddress(CiKind.Word, AMD64.rsp.asValue()));
            asm.subq(callSite, AMD64TargetMethodUtil.RIP_CALL_INSTRUCTION_SIZE);
            asm.movq(new CiAddress(CiKind.Word, AMD64.rsp.asValue()), callSite);

            asm.ret(0);

            String stubName = "strampoline";
            byte[] code = asm.codeBuffer.close(true);

            return new Stub(StaticTrampoline, stubName, frameSize, code, callPosition, callee, registerRestoreEpilogueOffset);
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
     * @see AMD64TrapStateAccess
     */
    @HOSTED_ONLY
    public Stub genTrapStub() {
        if (platform().isa == ISA.AMD64) {
            CiRegisterConfig registerConfig = registerConfigs.trapStub;
            AMD64MacroAssembler asm = new AMD64MacroAssembler(target(), registerConfig);
            CiCalleeSaveArea csa = registerConfig.getCalleeSaveArea();
            CiRegister latch = AMD64Safepoint.LATCH_REGISTER;
            CiRegister scratch = registerConfig.getScratchRegister();
            int frameSize = platform().target.alignFrameSize(csa.size);
            int frameToCSA = 0;
            CiKind[] handleTrapParameters = CRIUtil.signatureToKinds(Trap.handleTrap.classMethodActor.signature(), null);
            CiValue[] args = registerConfig.getCallingConvention(JavaCallee, handleTrapParameters, target()).locations;

            // the very first instruction must save the flags.
            // we save them twice and overwrite the first copy with the trap instruction/return address.
            asm.pushfq();
            asm.pushfq();

            // now allocate the frame for this method (first word of which was allocated by the second pushfq above)
            asm.subq(AMD64.rsp, frameSize - 8);

            // save all the callee save registers
            asm.save(csa, frameToCSA);

            // Now that we have saved all general purpose registers (including the scratch register),
            // store the value of the latch register from the thread locals into the trap state
            asm.movq(scratch, new CiAddress(CiKind.Word, latch.asValue(), TRAP_LATCH_REGISTER.offset));
            asm.movq(new CiAddress(CiKind.Word, AMD64.rsp.asValue(), frameToCSA + csa.offsetOf(latch)), scratch);

            // write the return address pointer to the end of the frame
            asm.movq(scratch, new CiAddress(CiKind.Word, latch.asValue(), TRAP_INSTRUCTION_POINTER.offset));
            asm.movq(new CiAddress(CiKind.Word, AMD64.rsp.asValue(), frameSize), scratch);


            // load the trap number from the thread locals into the first parameter register
            asm.movq(args[0].asRegister(), new CiAddress(CiKind.Word, latch.asValue(), TRAP_NUMBER.offset));
            // also save the trap number into the trap state
            asm.movq(new CiAddress(CiKind.Word, AMD64.rsp.asValue(), frameToCSA + AMD64TrapStateAccess.TRAP_NUMBER_OFFSET), args[0].asRegister());
            // load the trap state pointer into the second parameter register
            asm.leaq(args[1].asRegister(), new CiAddress(CiKind.Word, AMD64.rsp.asValue(), frameToCSA));
            // load the fault address from the thread locals into the third parameter register
            asm.movq(args[2].asRegister(), new CiAddress(CiKind.Word, latch.asValue(), TRAP_FAULT_ADDRESS.offset));

            asm.alignCall();
            int callPosition = asm.codeBuffer.position();
            ClassMethodActor callee = Trap.handleTrap.classMethodActor;
            asm.call();

            asm.restore(csa, frameToCSA);

            // now pop the flags register off the stack before returning
            asm.addq(AMD64.rsp, frameSize - 8);
            asm.popfq();
            asm.ret(0);

            byte[] code = asm.codeBuffer.close(true);

            return new Stub(TrapStub, "trapStub", frameSize, code, callPosition, callee, -1);
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
    public static void unwindWord(Address ip, Pointer sp, Pointer fp, Word returnValue) {
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
            CiRegisterConfig registerConfig = MaxineVM.vm().stubs.registerConfigs.globalStub;
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
                    case Word:
                        asm.movq(registerConfig.getReturnRegister(CiKind.Int), reg);
                        break;
                    case Float:
                        asm.movflt(registerConfig.getReturnRegister(CiKind.Float), reg);
                        break;
                    case Double:
                        asm.movdbl(registerConfig.getReturnRegister(CiKind.Double), reg);
                        break;
                    default:
                        FatalError.unexpected("unexpecte kind: " + kind);
                }
            }

            // Push 'pc' to the handler's stack frame and update RSP to point to the pushed value.
            // When the RET instruction is executed, the pushed 'pc' will be popped from the stack
            // and the stack will be in the correct state for the handler.
            asm.subq(sp, Word.size());
            asm.movq(new CiAddress(CiKind.Word, sp.asValue()), pc);
            asm.movq(AMD64.rbp, fp);
            asm.movq(AMD64.rsp, sp);
            asm.ret(0);

            byte[] code = asm.codeBuffer.close(true);
            return new Stub(GlobalStub, name, frameSize, code, -1, null, -1);
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
            CiRegisterConfig registerConfig = MaxineVM.vm().stubs.registerConfigs.globalStub;
            AMD64MacroAssembler asm = new AMD64MacroAssembler(target(), registerConfig);
            int frameSize = platform().target.alignFrameSize(0);

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }

            asm.subq(AMD64.rsp, AMD64.rsi);

            CriticalMethod unroll = new CriticalMethod(Deoptimization.class, "unroll", null);
            asm.alignCall();
            int callPosition = asm.codeBuffer.position();
            ClassMethodActor callee = unroll.classMethodActor;
            asm.call();

            // Should never reach here
            asm.hlt();

            byte[] code = asm.codeBuffer.close(true);
            return new Stub(GlobalStub, "unrollStub", frameSize, code, callPosition, callee, -1);
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
            Pointer patchAddr = stub.codeStart.plus(pos);
            patchAddr.writeLong(0, runtimeRoutine.address().toLong());
        }
    }

    @HOSTED_ONLY
    private synchronized Stub genDeoptStub(CiKind kind) {
        if (platform().isa == ISA.AMD64) {
            /*
             * The deopt stub initially executes in the frame of the method that was returned to and is about to be
             * deoptimized. It then allocates a temporary frame of 2 slots to transfer control to the deopt
             * routine via "returning" to it. As execution enters the deopt routine, the stack looks like
             * the about-to-be-deoptimized frame called the deopt routine directly.
             *
             * [ mov  rcx, rax ]                               // copy return value into arg3 (omitted for void/float/double values)
             *   mov  rdi [rsp + DEOPT_RETURN_ADDRESS_OFFSET]  // copy deopt IP into arg0
             *   mov  rsi, rsp                                 // copy deopt SP into arg1
             *   mov  rdx, rbp                                 // copy deopt FP into arg2
             *   subq rsp, 16                                  // allocate 2 slots
             *   mov  [rsp + 8], rdi                           // put deopt IP (i.e. original return address) into first slot
             *   mov  scratch, 0xFFFFFFFFFFFFFFFFL             // put (placeholder) address of deopt ...
             *   mov  [rsp], scratch                           // ... routine into second slot
             *   ret                                           // call deopt method by "returning" to it
             */
            CiRegisterConfig registerConfig = registerConfigs.globalStub;
            AMD64MacroAssembler asm = new AMD64MacroAssembler(target(), registerConfig);
            int frameSize = platform().target.alignFrameSize(0);

            String name = "deoptimize" + kind.name();
            final CriticalMethod runtimeRoutine;
            try {
                runtimeRoutine = new CriticalMethod(Deoptimization.class, name, null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
            } catch (NoSuchMethodError e) {
                // No deoptimization stub for kind
                return null;
            }

            CiValue[] args;
            if (!kind.isVoid()) {
                args = registerConfig.getCallingConvention(JavaCall, new CiKind[] {CiKind.Word, CiKind.Word, CiKind.Word, kind}, target()).locations;
                // Copy return value into arg 3
                CiRegister returnRegister = registerConfig.getReturnRegister(kind);
                CiRegister arg3 = args[3].asRegister();
                if (arg3 != returnRegister) {
                    if (kind.isFloat()) {
                        asm.movflt(arg3, returnRegister);
                    } else if (kind.isDouble()) {
                        asm.movdbl(arg3, returnRegister);
                    } else {
                        asm.movq(arg3, returnRegister);
                    }
                }
            } else {
                args = registerConfig.getCallingConvention(JavaCall, new CiKind[] {CiKind.Word, CiKind.Word, CiKind.Word}, target()).locations;
            }

            // Copy original return address into arg 0 (i.e. 'ip')
            CiRegister arg0 = args[0].asRegister();
            asm.movq(arg0, new CiAddress(CiKind.Word, AMD64.RSP, DEOPT_RETURN_ADDRESS_OFFSET));

            // Copy original stack pointer into arg 1 (i.e. 'sp')
            CiRegister arg1 = args[1].asRegister();
            asm.movq(arg1, AMD64.rsp);

            // Copy original frame pointer into arg 2 (i.e. 'sp')
            CiRegister arg2 = args[2].asRegister();
            asm.movq(arg2, AMD64.rbp);

            // Allocate 2 extra stack slots
            asm.subq(AMD64.rsp, 16);

            // Put original return address into high slot
            asm.movq(new CiAddress(CiKind.Word, AMD64.RSP, 8), arg0);

            // Put deopt method entry point into low slot
            CiRegister scratch = registerConfig.getScratchRegister();
            asm.movq(scratch, 0xFFFFFFFFFFFFFFFFL);
            final int patchPos = asm.codeBuffer.position() - 8;
            asm.movq(new CiAddress(CiKind.Word, AMD64.RSP), scratch);

            // "return" to deopt routine
            asm.ret(0);

            String stubName = name + "Stub";
            byte[] code = asm.codeBuffer.close(true);
            final Stub stub = new Stub(GlobalStub, stubName, frameSize, code, -1, null, -1);

            AMD64DeoptStubPatch patch = new AMD64DeoptStubPatch(patchPos, runtimeRoutine, stub);
            runtimeInits = Arrays.copyOf(runtimeInits, runtimeInits.length + 1);
            runtimeInits[runtimeInits.length - 1] = patch;

            return stub;
        }
        throw FatalError.unimplemented();
    }

    /**
     * Generates the code that makes the transition from a use of {@link Infopoints#uncommonTrap()}
     * to {@link Deoptimization#uncommonTrap(Pointer, Pointer, Pointer, Pointer)}.
     */
    @HOSTED_ONLY
    public synchronized Stub genUncommonTrapStub() {
        if (platform().isa == ISA.AMD64) {
            CiRegisterConfig registerConfig = registerConfigs.globalStub;
            AMD64MacroAssembler asm = new AMD64MacroAssembler(target(), registerConfig);
            CiCalleeSaveArea csa = registerConfig.getCalleeSaveArea();
            int frameSize = platform().target.alignFrameSize(csa.size);
            int frameToCSA = 0;

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }

            // now allocate the frame for this method
            asm.subq(AMD64.rsp, frameSize);

            // save all the registers
            asm.save(csa, frameToCSA);

            String name = "uncommonTrap";
            final CriticalMethod uncommonTrap = new CriticalMethod(Deoptimization.class, name, null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);

            CiValue[] args = registerConfig.getCallingConvention(JavaCall, new CiKind[] {CiKind.Word, CiKind.Word, CiKind.Word, CiKind.Word}, target()).locations;

            // Copy register save area address into arg 0 (i.e. 'rsa')
            CiRegister arg0 = args[0].asRegister();
            asm.leaq(arg0, new CiAddress(CiKind.Word, AMD64.RSP, frameToCSA));

            // Copy return address into arg 1 (i.e. 'ip')
            CiRegister arg1 = args[1].asRegister();
            asm.movq(arg1, new CiAddress(CiKind.Word, AMD64.RSP, frameSize));

            // Copy stack pointer into arg 1 (i.e. 'sp')
            CiRegister arg2 = args[2].asRegister();
            asm.leaq(arg2, new CiAddress(CiKind.Word, AMD64.RSP, frameSize + 8));

            // Copy original frame pointer into arg 2 (i.e. 'fp')
            CiRegister arg3 = args[3].asRegister();
            asm.movq(arg3, AMD64.rbp);

            asm.alignCall();
            int callPosition = asm.codeBuffer.position();
            ClassMethodActor callee = uncommonTrap.classMethodActor;
            asm.call();

            // Should never reach here
            asm.hlt();

            String stubName = name + "Stub";
            byte[] code = asm.codeBuffer.close(true);
            return new Stub(GlobalStub, stubName, frameSize, code, callPosition, callee, -1);
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
        CiCalleeSaveArea csa = registerConfig.getCalleeSaveArea();
        return calleeSaveStart.plus(csa.offsetOf(registerConfig.getScratchRegister())).getInt();
    }
}
