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

import com.sun.max.vm.VMConfiguration;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.AbstractVMScheme;
import com.sun.max.vm.runtime.VMRegister;
import com.sun.max.vm.runtime.FatalError;
import com.sun.max.vm.stack.StackFrameWalker;
import com.sun.max.vm.stack.StackUnwindingContext;
import com.sun.max.vm.actor.member.VirtualMethodActor;
import com.sun.max.vm.actor.member.ClassMethodActor;
import com.sun.max.vm.actor.member.MethodActor;
import com.sun.max.vm.compiler.ir.IrGenerator;
import com.sun.max.vm.compiler.ir.IrMethod;
import com.sun.max.vm.compiler.builtin.Builtin;
import com.sun.max.vm.compiler.CompilerScheme;
import com.sun.max.vm.compiler.target.TargetMethod;
import com.sun.max.vm.compiler.target.TargetABI;
import com.sun.max.vm.compiler.target.RegisterRoleAssignment;
import com.sun.max.annotate.PROTOTYPE_ONLY;
import com.sun.max.PackageLoader;
import com.sun.max.asm.InstructionSet;
import com.sun.max.util.Symbol;
import com.sun.max.collect.AppendableSequence;
import com.sun.max.collect.Sequence;
import com.sun.max.unsafe.Word;
import com.sun.max.unsafe.Pointer;
import com.sun.c1x.target.Target;
import com.sun.c1x.target.Architecture;
import com.sun.c1x.target.Register;
import com.sun.c1x.*;
import com.sun.c1x.ci.*;

import java.util.*;

/**
 * @author Ben L. Titzer
 */
public class C1XCompilerScheme extends AbstractVMScheme implements CompilerScheme {

    private Target c1xTarget;
    private MaxCiRuntime c1xRuntime;
    private C1XCompiler compiler;

    @PROTOTYPE_ONLY
    private final Map<TargetMethod, C1XTargetMethodGenerator> targetMap = new HashMap<TargetMethod, C1XTargetMethodGenerator>();

    public C1XCompilerScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    public IrGenerator irGenerator() {
        return null;
    }

    public Sequence<IrGenerator> irGenerators() {
        return null;
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.PROTOTYPING) {
            // create the Target object passed to C1X
            InstructionSet isa = vmConfiguration().platform().processorKind.instructionSet;
            Architecture arch = Architecture.findArchitecture(isa.name().toLowerCase());
            TargetABI targetABI = vmConfiguration().targetABIsScheme().optimizedJavaABI();

            // get the unallocatable registers
            Set<String> unallocatable = new HashSet<String>();
            RegisterRoleAssignment roles = targetABI.registerRoleAssignment();
            markUnallocatable(unallocatable, roles, VMRegister.Role.SAFEPOINT_LATCH);
            markUnallocatable(unallocatable, roles, VMRegister.Role.CPU_STACK_POINTER);
            markUnallocatable(unallocatable, roles, VMRegister.Role.CPU_FRAME_POINTER);
            markUnallocatable(unallocatable, roles, VMRegister.Role.ABI_SCRATCH);
            markUnallocatable(unallocatable, roles, VMRegister.Role.LITERAL_BASE_POINTER);

            // create the CiRuntime object passed to C1X
            c1xRuntime = MaxCiRuntime.globalRuntime;

            // configure the allocatable registers
            List<Register> allocatable = new ArrayList<Register>(arch.registers.length);
            for (Register r : arch.registers) {
                if (!unallocatable.contains(r.name.toLowerCase()) && r != c1xRuntime.exceptionOopRegister()) {
                    allocatable.add(r);
                }
            }
            Register[] allocRegs = allocatable.toArray(new Register[allocatable.size()]);

            // TODO (tw): Initialize target differently
            c1xTarget = new Target(arch, allocRegs, allocRegs, vmConfiguration().platform.pageSize, true);
            c1xTarget.stackAlignment = targetABI.stackFrameAlignment();


            compiler = new C1XCompiler(c1xRuntime, c1xTarget);
        }
    }

    private void markUnallocatable(Set<String> unallocatable, RegisterRoleAssignment roles, VMRegister.Role register) {
        Symbol intReg = roles.integerRegisterActingAs(register);
        if (intReg != null) {
            unallocatable.add(intReg.name().toLowerCase());
        }
        Symbol floatReg = roles.floatingPointRegisterActingAs(register);
        if (floatReg != null) {
            unallocatable.add(floatReg.name().toLowerCase());
        }
    }

    public long numberOfCompilations() {
        return 0;
    }

    public void createBuiltins(PackageLoader packageLoader) {
        // do nothing.
    }

    public void createSnippets(PackageLoader packageLoader) {
        // do nothing.
    }

    public boolean areSnippetsCompiled() {
        return true;
    }

    public void compileSnippets() {
        // do nothing
    }

    public Word createInitialVTableEntry(int index, VirtualMethodActor dynamicMethodActor) {
        return Word.zero();
    }

    public Word createInitialITableEntry(int index, VirtualMethodActor dynamicMethodActor) {
        return Word.zero();
    }

    public void staticTrampoline() {
        throw new UnsupportedOperationException();
    }

    public final IrMethod compile(ClassMethodActor classMethodActor) {
        // ignore compilation directive for now
        CiMethod method = c1xRuntime.getCiMethod(classMethodActor);
        CiTargetMethod compiledMethod = compiler.compileMethod(method);
        if (compiledMethod != null) {

            C1XTargetMethodGenerator generator = new C1XTargetMethodGenerator(this, classMethodActor, compiledMethod);
            C1XTargetMethod targetMethod = generator.finish();

            if (MaxineVM.isPrototyping()) {
                // in prototyping mode, we need to be able to iterate over the calls in the code
                // for the closure process
                targetMap.put(targetMethod, generator);
            }
            assert targetMethod != null;
            return targetMethod;
        }
        throw FatalError.unexpected("bailout"); // compilation failed
    }


    @PROTOTYPE_ONLY
    public void gatherCalls(TargetMethod targetMethod, AppendableSequence<MethodActor> directCalls, AppendableSequence<MethodActor> virtualCalls, AppendableSequence<MethodActor> interfaceCalls) {
        // iterate over all the calls in this target method and add them to the appropriate lists
        // this is used in code reachability during prototyping
        C1XTargetMethodGenerator ciTargetMethod = targetMap.get(targetMethod);
        assert ciTargetMethod != null : "no registered MaxCiTargetMethod for this TargetMethod";
        ciTargetMethod.gatherCalls(directCalls, virtualCalls, interfaceCalls);
    }

    public void initializeForJitCompilations() {
    }

    public boolean walkFrame(StackFrameWalker stackFrameWalker, boolean isTopFrame, TargetMethod targetMethod, StackFrameWalker.Purpose purpose, Object context) {
        throw FatalError.unimplemented();
    }

    public void advance(StackFrameWalker stackFrameWalker, Word instructionPointer, Word stackPointer, Word framePointer) {
        throw FatalError.unimplemented();
    }

    public Pointer namedVariablesBasePointer(Pointer stackPointer, Pointer framePointer) {
        throw FatalError.unimplemented();
    }

    public StackUnwindingContext makeStackUnwindingContext(Word stackPointer, Word framePointer, Throwable throwable) {
        throw new UnsupportedOperationException();
    }

    public boolean isBuiltinImplemented(Builtin builtin) {
        return true;
    }

}
