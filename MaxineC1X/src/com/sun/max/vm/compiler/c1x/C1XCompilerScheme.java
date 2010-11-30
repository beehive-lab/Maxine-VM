/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.compiler.c1x;

import static com.sun.cri.ci.CiCallingConvention.Type.*;
import static com.sun.max.lang.Classes.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.compiler.CallEntryPoint.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.target.amd64.*;
import com.sun.c1x.util.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.cri.xir.*;
import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.c1x.MaxXirGenerator.RuntimeCalls;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.amd64.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.amd64.*;
import com.sun.max.vm.stack.amd64.*;
import com.sun.max.vm.trampoline.*;
import com.sun.max.vm.type.*;

/**
 * Integration of the C1X compiler into Maxine's compilation framework.
 *
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public class C1XCompilerScheme extends AbstractVMScheme implements RuntimeCompilerScheme, DynamicTrampolineScheme {

    /**
     * The Maxine specific implementation of the {@linkplain RiRuntime runtime interface} needed by C1X.
     */
    public final MaxRiRuntime runtime = new MaxRiRuntime();

    /**
     * The {@linkplain CiTarget target} environment derived from a Maxine {@linkplain Platform platform} description.
     */
    public final CiTarget target = platform().target;

    /**
     * The Maxine specific implementation of the {@linkplain RiXirGenerator interface} used by C1X
     * to incorporate runtime specific details when translating bytecode methods.
     */
    public final RiXirGenerator xirGenerator = new MaxXirGenerator();

    /**
     * The C1X compiler instance configured for the Maxine runtime.
     */
    private C1XCompiler compiler;

    @FOLD
    static RiRegisterConfig getGlobalStubRegisterConfig() {
        Platform platform = Platform.platform();
        if (platform.isa == ISA.AMD64) {
            switch (platform.os) {
                case DARWIN:
                case GUESTVM:
                case LINUX:
                case SOLARIS: {
                    return AMD64UnixRegisterConfig.GLOBAL_STUB;
                }
                default:
                    throw FatalError.unimplemented();

            }
        }
        throw FatalError.unimplemented();
    }

    public static final VMIntOption c1xOptLevel = VMOptions.register(new VMIntOption("-C1X:OptLevel=", 1,
        "Set the optimization level of C1X.") {
            @Override
            public boolean parseValue(com.sun.max.unsafe.Pointer optionValue) {
                boolean result = super.parseValue(optionValue);
                if (result) {
                    C1XOptions.setOptimizationLevel(getValue());
                    return true;
                }
                return false;
            }
        }, MaxineVM.Phase.STARTING);

    @HOSTED_ONLY
    public C1XCompilerScheme() {
        VMOptions.addFieldOptions("-C1X:", C1XOptions.class, C1XOptions.helpMap);
    }

    @Override
    public <T extends TargetMethod> Class<T> compiledType() {
        Class<Class<T>> type = null;
        return Utils.cast(type, C1XTargetMethod.class);
    }

    private ClassMethodActor vTableTrampoline;
    private ClassMethodActor iTableTrampoline;
    private ClassMethodActor staticTrampoline;
    private byte[] vTableTrampolinePrologue;
    private byte[] iTableTrampolinePrologue;
    private TargetMethod staticTrampolineCode;

    @TRAMPOLINE(invocation = TRAMPOLINE.Invocation.VIRTUAL)
    private static native Address vTableTrampoline() throws Throwable;

    @TRAMPOLINE(invocation = TRAMPOLINE.Invocation.INTERFACE)
    private static native Address iTableTrampoline() throws Throwable;

    @TRAMPOLINE(invocation = TRAMPOLINE.Invocation.STATIC)
    private static native Address staticTrampoline() throws Throwable;

    @HOSTED_ONLY
    private byte[] adapterPrologueFor(ClassMethodActor callee) {
        AdapterGenerator generator = AdapterGenerator.forCallee(callee, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        if (generator != null) {
            ByteArrayOutputStream os = new ByteArrayOutputStream(8);
            generator.adapt(callee, os);
            return os.toByteArray();
        }
        return new byte[0];
    }

    @Override
    public void initialize(Phase phase) {
        if (isHosted() && phase == Phase.BOOTSTRAPPING) {
            compiler = new C1XCompiler(runtime, target, xirGenerator, getGlobalStubRegisterConfig());
            // search for the runtime call and register critical methods
            for (Method m : RuntimeCalls.class.getDeclaredMethods()) {
                int flags = m.getModifiers();
                if (Modifier.isStatic(flags) && Modifier.isPublic(flags)) {
                    // Log.out.println("Registered critical method: " + m.getName() + " / " + SignatureDescriptor.create(m.getReturnType(), m.getParameterTypes()).toString());
                    new CriticalMethod(RuntimeCalls.class, m.getName(), SignatureDescriptor.create(m.getReturnType(), m.getParameterTypes()));
                }
            }

            vTableTrampoline = ClassMethodActor.fromJava(getDeclaredMethod(C1XCompilerScheme.class, "vTableTrampoline"));
            iTableTrampoline = ClassMethodActor.fromJava(getDeclaredMethod(C1XCompilerScheme.class, "iTableTrampoline"));
            staticTrampoline = ClassMethodActor.fromJava(getDeclaredMethod(C1XCompilerScheme.class, "staticTrampoline"));
            vTableTrampolinePrologue = adapterPrologueFor(vTableTrampoline);
            iTableTrampolinePrologue = adapterPrologueFor(iTableTrampoline);

            staticTrampolineCode = genStaticTrampoline(adapterPrologueFor(staticTrampoline));
            StaticTrampoline.codeStart = staticTrampolineCode.codeStart();
        } else if (phase == Phase.PRIMORDIAL) {
            StaticTrampoline.codeStart = staticTrampolineCode.codeStart();
        }
    }

    public C1XCompiler compiler() {
        if (isHosted() && compiler == null) {
            initialize(Phase.BOOTSTRAPPING);
        }
        return compiler;
    }

    public final TargetMethod compile(final ClassMethodActor classMethodActor) {
        RiMethod method = classMethodActor;
        if (classMethodActor.isTrapStub()) {
            if (isHosted()) {
                return genTrapStub();
            }
            FatalError.unexpected("Trap stub must be compiled into boot image");
        }
        if (classMethodActor.isInterfaceTrampoline()) {
            return genDynamicTrampoline(0, true);
        }

        if (classMethodActor.isVirtualTrampoline()) {
            return genDynamicTrampoline(0, false);
        }

        CiTargetMethod compiledMethod = compiler().compileMethod(method, -1, xirGenerator).targetMethod();
        if (compiledMethod != null) {
            C1XTargetMethod c1xTargetMethod = new C1XTargetMethod(classMethodActor, compiledMethod);
            CompilationScheme.Inspect.notifyCompilationComplete(c1xTargetMethod);
            return c1xTargetMethod;
        }
        throw FatalError.unexpected("bailout"); // compilation failed
    }

    @Override
    public CallEntryPoint calleeEntryPoint() {
        return CallEntryPoint.OPTIMIZED_ENTRY_POINT;
    }

    /**
     * Gets the offset from the frame pointer to the CSA in the
     * frame of a callee-saved method.
     *
     * @param frameSize the size of the frame (which includes the CSA)
     * @param csa the details of callee-save area within the frame
     */
    static int offsetOfCSAInFrame(int frameSize, CiCalleeSaveArea csa) {
        return frameSize - csa.size;
    }

    @HOSTED_ONLY
    private TargetMethod genTrapStub() {
        if (platform().isa == ISA.AMD64) {
            AMD64UnixRegisterConfig registerConfig = AMD64UnixRegisterConfig.TRAP_STUB;
            AMD64MacroAssembler asm = new AMD64MacroAssembler(compiler, registerConfig);
            CiCalleeSaveArea csa = registerConfig.getCalleeSaveArea();
            CiRegister latch = AMD64Safepoint.LATCH_REGISTER;
            CiRegister scratch = registerConfig.getScratchRegister();
            int frameSize = platform().target.alignFrameSize(csa.size);
            int frameToCSA = offsetOfCSAInFrame(frameSize, csa);
            CiKind[] trapStubParameters = Util.signatureToKinds(Trap.trapStub.classMethodActor.signature(), null);
            CiValue[] locations = registerConfig.getCallingConvention(JavaCallee, trapStubParameters, target).locations;

            // the very first instruction must save the flags.
            // we save them twice and overwrite the first copy with the trap instruction/return address.
            int pushfq = 0x9c;
            asm.emitByte(pushfq);
            asm.emitByte(pushfq);

            // now allocate the frame for this method (first word of which was allocated by the second pushfq above)
            asm.subq(AMD64.rsp, frameSize - 8);
            asm.setFrameSize(frameSize);

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
            asm.movq(locations[0].asRegister(), new CiAddress(CiKind.Word, latch.asValue(), TRAP_NUMBER.offset));
            // also save the trap number into the trap state
            asm.movq(new CiAddress(CiKind.Word, AMD64.rsp.asValue(), frameToCSA + AMD64TrapStateAccess.TRAP_NUMBER_OFFSET), locations[0].asRegister());
            // load the trap state pointer into the second parameter register
            asm.leaq(locations[1].asRegister(), new CiAddress(CiKind.Word, AMD64.rsp.asValue(), frameToCSA));
            // load the fault address from the thread locals into the third parameter register
            asm.movq(locations[2].asRegister(), new CiAddress(CiKind.Word, latch.asValue(), TRAP_FAULT_ADDRESS.offset));

            asm.directCall(Trap.handleTrap.classMethodActor, null);

            asm.restore(csa, frameToCSA);

            // now pop the flags register off the stack before returning
            int popfq = 0x9D;
            asm.addq(AMD64.rsp, frameSize - 8);
            asm.emitByte(popfq);
            asm.ret(0);

            return new C1XTargetMethod(Trap.trapStub.classMethodActor, asm.finishTargetMethod(Trap.trapStub.classMethodActor, runtime, -1));
        }
        throw FatalError.unimplemented();
    }

    @PLATFORM(cpu = "amd64")
    private static void patchStaticTrampolineCallSiteAMD64(Pointer callSite) {
        final TargetMethod caller = Code.codePointerToTargetMethod(callSite);

        final ClassMethodActor callee = caller.callSiteToCallee(callSite);

        // Use the caller's abi to get the correct entry point.
        final Address calleeEntryPoint = CompilationScheme.Static.compile(callee, caller.callEntryPoint);
        AMD64TargetMethodUtil.mtSafePatchCallDisplacement(caller, callSite, calleeEntryPoint);
       // final int calleeOffset = calleeEntryPoint.minus(callSite.plus(AMD64OptStackWalking.RIP_CALL_INSTRUCTION_SIZE)).toInt();
       // AMD64TargetMethodUtil.mtSafePatchCallSite(caller, callSite, calleeOffset);
    }

    @HOSTED_ONLY
    private TargetMethod genStaticTrampoline(byte[] adapterPrologue) {
        if (platform().isa == ISA.AMD64) {
            AMD64UnixRegisterConfig registerConfig = AMD64UnixRegisterConfig.TRAMPOLINE;
            AMD64MacroAssembler asm = new AMD64MacroAssembler(compiler, registerConfig);
            CiCalleeSaveArea csa = registerConfig.getCalleeSaveArea();
            int frameSize = platform().target.alignFrameSize(csa.size);
            int frameToCSA = offsetOfCSAInFrame(frameSize, csa);

            for (byte b : adapterPrologue) {
                asm.emitByte(0xff & b);
            }

            // compute the static trampoline call site
            CiRegister callSite = registerConfig.getScratchRegister();
            asm.movq(callSite, new CiAddress(CiKind.Word, AMD64.rsp.asValue()));
            asm.subq(callSite, AMD64OptStackWalking.RIP_CALL_INSTRUCTION_SIZE);

            // now allocate the frame for this method
            asm.subq(AMD64.rsp, frameSize);
            asm.setFrameSize(frameSize);

            // save all the callee save registers
            asm.save(csa, frameToCSA);

            ClassMethodActor patchStaticTrampolineCallSite = ClassMethodActor.fromJava(Classes.getDeclaredMethod(C1XCompilerScheme.class, "patchStaticTrampolineCallSiteAMD64", Pointer.class));
            CiKind[] trampolineParameters = {CiKind.Object};
            CiValue[] locations = registerConfig.getCallingConvention(JavaCall, trampolineParameters, target).locations;

            // load the static trampoline call site into the first parameter register
            asm.movq(locations[0].asRegister(), callSite);

            asm.directCall(patchStaticTrampolineCallSite, null);

            // restore all parameter registers before returning
            int registerRestoreEpilogueOffset = asm.codeBuffer.position();
            asm.restore(csa, frameToCSA);

            // undo the frame
            asm.addq(AMD64.rsp, frameSize);

            // patch the return address to re-execute the static call
            asm.movq(callSite, new CiAddress(CiKind.Word, AMD64.rsp.asValue()));
            asm.subq(callSite, AMD64OptStackWalking.RIP_CALL_INSTRUCTION_SIZE);
            asm.movq(new CiAddress(CiKind.Word, AMD64.rsp.asValue()), callSite);

            asm.ret(0);

            return new C1XTargetMethod(staticTrampoline, asm.finishTargetMethod(staticTrampoline, runtime, registerRestoreEpilogueOffset));
        }
        throw FatalError.unimplemented();
    }

    private TargetMethod genDynamicTrampoline(int index, boolean isInterface) {
        if (platform().isa == ISA.AMD64) {
            AMD64UnixRegisterConfig registerConfig = AMD64UnixRegisterConfig.TRAMPOLINE;
            AMD64MacroAssembler asm = new AMD64MacroAssembler(compiler, registerConfig);
            CiCalleeSaveArea csa = registerConfig.getCalleeSaveArea();
            int frameSize = platform().target.alignFrameSize(csa.size);
            int frameToCSA = offsetOfCSAInFrame(frameSize, csa);
            DynamicTrampoline trampoline = new DynamicTrampoline(index, null);

            byte[] prologue = isInterface ? iTableTrampolinePrologue : vTableTrampolinePrologue;
            for (byte b : prologue) {
                asm.emitByte(0xff & b);
            }

            // now allocate the frame for this method
            asm.subq(AMD64.rsp, frameSize);
            asm.setFrameSize(frameSize);

            // save all the callee save registers
            asm.save(csa, frameToCSA);

            CiKind[] trampolineParameters = Util.signatureToKinds(DynamicTrampoline.trampolineReturnAddress.classMethodActor.signature(), CiKind.Object);
            CiValue[] locations = registerConfig.getCallingConvention(JavaCall, trampolineParameters, target).locations;

            // load the receiver into the second parameter register
            asm.movq(locations[1].asRegister(), locations[0].asRegister());

            // load the trampoline object into the first parameter register
            asm.movq(locations[0].asRegister(), asm.recordDataReferenceInCode(CiConstant.forObject(trampoline)));

            // load the stack pointer into the third parameter register
            asm.movq(locations[2].asRegister(), AMD64.rsp);

            asm.directCall(DynamicTrampoline.trampolineReturnAddress.classMethodActor, null);

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

            ClassMethodActor classMethodActor = isInterface ? iTableTrampoline : vTableTrampoline;
            return new C1XTargetMethod(classMethodActor, asm.finishTargetMethod(classMethodActor, runtime, registerRestoreEpilogueOffset));
        }
        throw FatalError.unimplemented();
    }

    private final ArrayList<TargetMethod> vTrampolines = new ArrayList<TargetMethod>();
    private final ArrayList<TargetMethod> iTrampolines = new ArrayList<TargetMethod>();


    public synchronized Address makeInterfaceCallEntryPoint(int iIndex) {
        if (iTrampolines.size() <= iIndex) {
            for (int i = iTrampolines.size(); i <= iIndex; i++) {
                iTrampolines.add(genDynamicTrampoline(i, true));
            }
        }
        return VTABLE_ENTRY_POINT.in(iTrampolines.get(iIndex));
    }

    public synchronized Address makeVirtualCallEntryPoint(int vTableIndex) {
        if (iTrampolines.size() <= vTableIndex) {
            for (int i = iTrampolines.size(); i <= vTableIndex; i++) {
                iTrampolines.add(genDynamicTrampoline(i, false));
            }
        }
        return VTABLE_ENTRY_POINT.in(iTrampolines.get(vTableIndex));
    }

    public DynamicTrampolineExit dynamicTrampolineExit() {
        return dynamicTrampolineExit;
    }

    static RiRegisterConfig getRegisterConfig(ClassMethodActor method) throws FatalError {
        Platform platform = Platform.platform();
        if (platform.isa == ISA.AMD64) {
            if (platform.os.unix  || platform.os == OS.GUESTVM) {
                if (method.isTrapStub()) {
                    return AMD64UnixRegisterConfig.TRAP_STUB;
                }
                if (method.isVmEntryPoint()) {
                    return AMD64UnixRegisterConfig.N2J;
                }
                if (method.isCFunction()) {
                    return AMD64UnixRegisterConfig.J2N;
                }
                assert !method.isTemplate();
                if (method.isTrampoline()) {
                    return AMD64UnixRegisterConfig.TRAMPOLINE;
                }
                return AMD64UnixRegisterConfig.STANDARD;
            }
            throw FatalError.unimplemented();
        }
        throw FatalError.unimplemented();
    }

    private DynamicTrampolineExit dynamicTrampolineExit = DynamicTrampolineExit.create();

}
