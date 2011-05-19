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
import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.compiler.CallEntryPoint.*;
import static com.sun.max.vm.compiler.CompilationScheme.Static.*;
import static com.sun.max.vm.compiler.target.TargetMethod.Flavor.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.util.*;

import com.oracle.max.asm.target.amd64.*;
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
import com.sun.max.vm.compiler.target.TargetMethod.*;
import com.sun.max.vm.compiler.target.amd64.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.amd64.*;

/**
 * Stubs are pieces of hand crafted assembly code for expressing semantics that cannot otherwise be expressed as Java.
 * For example, trampolines are stubs used to lazily link call sites to their targets at runtime.
 */
public class Stubs {

    /**
     * The stubs called to link an interface method call.
     */
    private final ArrayList<TargetMethod> virtualTrampolines = new ArrayList<TargetMethod>();

    /**
     * The stubs called to link an interface method call.
     */
    private final ArrayList<TargetMethod> interfaceTrampolines = new ArrayList<TargetMethod>();

    /**
     * The stub called to link a call site where the exact method being called is known.
     */
    private TargetMethod staticTrampoline;

    /**
     * The stub called by the native level trap handler.
     *
     * @see Trap
     */
    private TargetMethod trapStub;

    private CriticalMethod resolveVirtualCall;
    private CriticalMethod resolveInterfaceCall;
    private CiValue[] resolveVirtualCallArgs;
    private CiValue[] resolveInterfaceCallArgs;

    public Stubs(RegisterConfigs registerConfigs) {
        this.registerConfigs = registerConfigs;
    }

    /**
     * Gets the stub called to link a call site where the exact method being called is known.
     */
    public TargetMethod staticTrampoline() {
        return staticTrampoline;
    }

    /**
     * Gets the stub called by the native level trap handler.
     *
     * @see #genTrapStub()
     */
    public TargetMethod trapStub() {
        return trapStub;
    }

    private void delayedInit() {
        if (MaxineVM.isHosted()) {
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

                CriticalMethod unwind = new CriticalMethod(Stubs.class, "unwind", null);
                CiValue[] unwindArgs = registerConfigs.globalStub.getCallingConvention(JavaCall,
                                CRIUtil.signatureToKinds(unwind.classMethodActor.signature(), CiKind.Object), target()).locations;
                unwind.classMethodActor.targetState = genUnwind(unwindArgs);
            }
        }
    }

    public final RegisterConfigs registerConfigs;

    private int prologueSize = -1;

    public synchronized Address interfaceTrampoline(int iIndex) {
        if (interfaceTrampolines.size() <= iIndex) {
            for (int i = interfaceTrampolines.size(); i <= iIndex; i++) {
                interfaceTrampolines.add(genDynamicTrampoline(i, true));
            }
        }
        return VTABLE_ENTRY_POINT.in(interfaceTrampolines.get(iIndex));
    }

    public synchronized Address virtualTrampoline(int vTableIndex) {
        if (virtualTrampolines.size() <= vTableIndex) {
            for (int i = virtualTrampolines.size(); i <= vTableIndex; i++) {
                virtualTrampolines.add(genDynamicTrampoline(i, false));
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
        final Address vtableEntryPoint = compile(selectedCallee).getEntryPoint(VTABLE_ENTRY_POINT).asAddress();
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
        final Address itableEntryPoint = compile(selectedCallee).getEntryPoint(VTABLE_ENTRY_POINT).asAddress();
        hub.setWord(hub.iTableStartIndex + iIndex, itableEntryPoint);
        return adjustEntryPointForCaller(itableEntryPoint, pcInCaller);
    }

    private TargetMethod genDynamicTrampoline(int index, boolean isInterface) {
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

            String stubName = (isInterface ? 'i' : 'v') + "trampoline<" + index + ">";
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
        final Address calleeEntryPoint = compile(callee).getEntryPoint(caller.callEntryPoint).asAddress();
        AMD64TargetMethodUtil.mtSafePatchCallDisplacement(caller, callSite, calleeEntryPoint);
    }

    /**
     * Generates a stub that links a call to a method whose actor is available in
     * data {@linkplain TargetMethod#callSiteToCallee(Address) associated} with the call site.
     * The stub also saves and restores all the callee-saved registers specified in the
     * {@linkplain RegisterConfigs#trampoline trampoline} register configuration.
     */
    @HOSTED_ONLY
    private TargetMethod genStaticTrampoline() {
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
    public TargetMethod genTrapStub() {
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

    @NEVER_INLINE
    public static void unwind(Address catchAddress, Pointer sp, Pointer fp) {
        // This is a placeholder method so that the unwind stub (which is generated by genUnwind) can be called comfortably via a normal method call.
        FatalError.unexpected("stub should be overwritten");
    }

    @HOSTED_ONLY
    private TargetMethod genUnwind(CiValue[] unwindArgs) {
        if (platform().isa == ISA.AMD64) {
            CiRegisterConfig registerConfig = MaxineVM.vm().stubs.registerConfigs.globalStub;
            AMD64MacroAssembler asm = new AMD64MacroAssembler(target(), registerConfig);
            int frameSize = platform().target.alignFrameSize(0);

            for (int i = 0; i < prologueSize; ++i) {
                asm.nop();
            }

            CiValue[] args = unwindArgs;

            CiRegister catchAddress = args[0].asRegister();
            CiRegister catchSP = args[1].asRegister();
            CiRegister catchFP = args[2].asRegister();

            // Push 'catchAddress' to the handler's stack frame and update RSP to point to the pushed value.
            // When the RET instruction is executed, the pushed 'catchAddress' will be popped from the stack
            // and the stack will be in the correct state for the handler.
            asm.subq(catchSP, Word.size());
            asm.movq(new CiAddress(CiKind.Word, catchSP.asValue()), catchAddress);
            asm.movq(AMD64.rbp, catchFP);
            asm.movq(AMD64.rsp, catchSP);
            asm.ret(0);

            byte[] code = asm.codeBuffer.close(true);
            return new Stub(GlobalStub, "unwindStub", frameSize, code, -1, null, -1);
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
