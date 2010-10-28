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

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.c1x.*;
import com.sun.c1x.target.amd64.*;
import com.sun.c1x.util.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.cri.xir.*;
import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.platform.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.amd64.*;

/**
 * Integration of the C1X compiler into Maxine's compilation framework.
 *
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public class C1XCompilerScheme extends AbstractVMScheme implements RuntimeCompilerScheme {

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
    private RiXirGenerator xirGenerator;

    /**
     * The C1X compiler instance configured for the Maxine runtime.
     */
    private C1XCompiler compiler;

    @HOSTED_ONLY
    private static RiRegisterConfig selectStubRegisterConfig() {
        Platform platform = Platform.platform();
        if (platform.isa == ISA.AMD64) {
            switch (platform.os) {
                case DARWIN:
                case GUESTVM:
                case LINUX:
                case SOLARIS: {
                    return AMD64UnixRegisterConfig.STUB;
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

    /**
     * Gets the Maxine specific implementation of the {@linkplain RiXirGenerator interface} used by C1X
     * to incorporate runtime specific details when translating bytecode methods.
     */
    public RiXirGenerator getXirGenerator() {
        if (isHosted() && xirGenerator == null) {
            // Lazy initialization to resolve Maxine scheme initialization boot strapping
            xirGenerator = new MaxXirGenerator(vmConfig(), target, runtime);
        }
        return xirGenerator;
    }

    /**
     * Gets the C1X compiler instance configured for the Maxine runtime.
     */
    public C1XCompiler getCompiler() {
        if (isHosted() && compiler == null) {
            compiler = new C1XCompiler(runtime, target, getXirGenerator(), selectStubRegisterConfig());
        }
        return compiler;
    }

    @Override
    public <Type extends TargetMethod> Class<Type> compiledType() {
        Class<Class<Type>> type = null;
        return Utils.cast(type, C1XTargetMethod.class);
    }

    public final TargetMethod compile(final ClassMethodActor classMethodActor) {
        RiMethod method = classMethodActor;
        if (classMethodActor.isTrapStub()) {
            if (isHosted()) {
                return genTrapStub();
            }
            FatalError.unexpected("Trap stub must be compiled into boot image");
        }
        CiTargetMethod compiledMethod = getCompiler().compileMethod(method, -1, getXirGenerator()).targetMethod();
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

    @HOSTED_ONLY
    private TargetMethod genTrapStub() {
        if (platform().isa == ISA.AMD64) {
            AMD64UnixRegisterConfig registerConfig = AMD64UnixRegisterConfig.TRAP_STUB;
            AMD64MacroAssembler asm = new AMD64MacroAssembler(compiler, registerConfig);
            CiRegisterSaveArea rsa = AMD64TrapStateAccess.RSA;
            CiRegister latch = AMD64Safepoint.LATCH_REGISTER;
            CiRegister scratch = registerConfig.getScratchRegister();
            int frameSize = rsa.size;

            // the very first instruction must save the flags.
            // we save them twice and overwrite one copy with the trap instruction/return address.
            int pushfq = 0x9c;
            asm.emitByte(pushfq);
            asm.emitByte(pushfq);

            // now allocate the frame for this method
            asm.subq(AMD64.rsp, frameSize - 16);
            asm.setFrameSize(frameSize);

            // save all the general purpose registers
            CiRegister[] calleeSave = registerConfig.getCalleeSaveRegisters();
            asm.save(calleeSave, rsa, 0);

            // Now that we have saved all general purpose registers (including the scratch register),
            // store the value of the latch register from the thread locals into the trap state
            asm.movq(scratch, new CiAddress(CiKind.Word, latch.asValue(), TRAP_LATCH_REGISTER.offset));
            asm.movq(new CiAddress(CiKind.Word, AMD64.rsp.asValue(), rsa.offsetOf(latch)), scratch);

            // write the return address pointer to the end of the frame
            asm.movq(scratch, new CiAddress(CiKind.Word, latch.asValue(), TRAP_INSTRUCTION_POINTER.offset));
            asm.movq(new CiAddress(CiKind.Word, AMD64.rsp.asValue(), frameSize), scratch);

            // save the trap number
            asm.movq(scratch, new CiAddress(CiKind.Word, latch.asValue(), TRAP_NUMBER.offset));
            asm.movq(new CiAddress(CiKind.Word, AMD64.rsp.asValue(), AMD64TrapStateAccess.TRAP_NUMBER_OFFSET), scratch);

            // now load the trap parameter information into registers from the VM thread locals
            CiKind[] trapStubParameters = Util.signatureToKinds(Trap.trapStub.classMethodActor.signature(), null);
            CiValue[] locations = registerConfig.getJavaCallingConvention(trapStubParameters, false, target).locations;

            // load the trap number into the first parameter register
            asm.movq(locations[0].asRegister(), new CiAddress(CiKind.Word, latch.asValue(), TRAP_NUMBER.offset));
            // load the trap state pointer into the second parameter register
            asm.leaq(locations[1].asRegister(), new CiAddress(CiKind.Word, AMD64.rsp.asValue(), frameSize));
            // load the fault address into the third parameter register
            asm.movq(locations[2].asRegister(), new CiAddress(CiKind.Word, latch.asValue(), TRAP_FAULT_ADDRESS.offset));

            asm.directCall(Trap.handleTrap.classMethodActor, null);

            asm.restore(calleeSave, rsa, 0);

            // now pop the flags register off the stack before returning
            int popfq = 0x9D;
            asm.addq(AMD64.rsp, frameSize - 16);
            asm.emitByte(popfq);
            asm.ret(0);

            return new C1XTargetMethod("trap-stub", asm.finishTargetMethod("trap-stub", runtime, -1));
        }
        throw FatalError.unimplemented();
    }
}
