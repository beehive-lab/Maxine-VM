/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.compiler.c1x;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.compiler.target.TargetMethod.Flavor.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.util.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.io.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * The {@code MaxRiRuntime} class implements the runtime interface needed by C1X.
 * This includes access to runtime features such as class and method representations,
 * constant pools, as well as some compiler tuning.
 *
 * @author Ben L. Titzer
 */
public class MaxRiRuntime implements RiRuntime {

    private RiSnippets snippets;

    public MaxRiRuntime() {
    }

    /**
     * Gets the constant pool for a specified method.
     * @param method the compiler interface method
     * @return the compiler interface constant pool for the specified method
     */
    public RiConstantPool getConstantPool(RiMethod method) {
        return asClassMethodActor(method, "getConstantPool()").compilee().codeAttribute().constantPool;
    }

    /**
     * Gets the OSR frame for a particular method at a particular bytecode index.
     * @param method the compiler interface method
     * @param bci the bytecode index
     * @return the OSR frame
     */
    public RiOsrFrame getOsrFrame(RiMethod method, int bci) {
        throw FatalError.unimplemented();
    }

    /**
     * Remove once C1X can compile native method stubs.
     */
    public static final boolean CAN_COMPILE_NATIVE_METHODS = "true".equals(System.getenv("C1X_CAN_COMPILE_NATIVE_METHODS"));

    /**
     * Remove once C1X implements the semantics of the ACCESSOR annotation.
     */
    public static final boolean CAN_COMPILE_ACCESSOR_METHODS = "true".equals(System.getenv("C1X_CAN_COMPILE_ACCESSOR_METHODS"));

    /**
     * Checks whether the runtime requires inlining of the specified method.
     * @param method the method to inline
     * @return {@code true} if the method must be inlined; {@code false}
     * to allow the compiler to use its own heuristics
     */
    public boolean mustInline(RiMethod method) {
        if (!method.isResolved()) {
            return false;
        }
        final ClassMethodActor classMethodActor = asClassMethodActor(method, "mustNotInline()");
        if (classMethodActor.accessor() != null && !CAN_COMPILE_ACCESSOR_METHODS) {
            return false;
        }
        if (classMethodActor.isNative() && !CAN_COMPILE_NATIVE_METHODS) {
            return false;
        }
        return classMethodActor.isInline();
    }

    /**
     * Checks whether the runtime forbids inlining of the specified method.
     * @param method the method to inline
     * @return {@code true} if the runtime forbids inlining of the specified method;
     * {@code false} to allow the compiler to use its own heuristics
     */
    public boolean mustNotInline(RiMethod method) {
        if (!method.isResolved()) {
            return false;
        }
        final ClassMethodActor classMethodActor = asClassMethodActor(method, "mustNotInline()");
        if (classMethodActor.accessor() != null && !CAN_COMPILE_ACCESSOR_METHODS) {
            return true;
        }
        if (classMethodActor.isNative() && !CAN_COMPILE_NATIVE_METHODS) {
            return true;
        }

        return classMethodActor.originalCodeAttribute(true) == null || classMethodActor.isNeverInline();
    }

    /**
     * Checks whether the runtime forbids compilation of the specified method.
     * @param method the method to compile
     * @return {@code true} if the runtime forbids compilation of the specified method;
     * {@code false} to allow the compiler to compile the method
     */
    public boolean mustNotCompile(RiMethod method) {
        return false;
    }

    static ClassMethodActor asClassMethodActor(RiMethod method, String operation) {
        if (method instanceof ClassMethodActor) {
            return (ClassMethodActor) method;
        }
        throw new CiUnresolvedException("invalid RiMethod instance: " + method.getClass());
    }

    public int basicObjectLockOffsetInBytes() {
        // Must not be called if the size of the lock object is 0.
        throw Util.shouldNotReachHere();
    }

    public int sizeOfBasicObjectLock() {
        // locks are not placed on the stack
        return 0;
    }

    public int codeOffset() {
        return CallEntryPoint.OPTIMIZED_ENTRY_POINT.offset();
    }

    @Override
    public String disassemble(RiMethod method) {
        ClassMethodActor classMethodActor = asClassMethodActor(method, "disassemble()");
        return classMethodActor.format("%f %R %H.%n(%P)") + String.format("%n%s", CodeAttributePrinter.toString(classMethodActor.codeAttribute()));
    }

    public String disassemble(byte[] code, long address) {
        if (MaxineVM.isHosted()) {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            final IndentWriter writer = new IndentWriter(new OutputStreamWriter(byteArrayOutputStream));
            writer.flush();
            Platform platform = Platform.platform();
            final InlineDataDecoder inlineDataDecoder = null;
            final Pointer startAddress = Pointer.fromLong(address);
            final DisassemblyPrinter disassemblyPrinter = new DisassemblyPrinter(false);
            Disassembler.disassemble(byteArrayOutputStream, code, platform.isa, platform.wordWidth(), startAddress.toLong(), inlineDataDecoder, disassemblyPrinter);
            return byteArrayOutputStream.toString();
        }
        return "";
    }

    @Override
    public String disassemble(final CiTargetMethod targetMethod) {
        if (MaxineVM.isHosted()) {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            final IndentWriter writer = new IndentWriter(new OutputStreamWriter(byteArrayOutputStream));
            writer.flush();
            final Platform platform = Platform.platform();
            final InlineDataDecoder inlineDataDecoder = null;
            final Pointer startAddress = Pointer.fromInt(0);
            final DisassemblyPrinter disassemblyPrinter = new MaxDisassemblyPrinter(targetMethod);
            byte[] code = Arrays.copyOf(targetMethod.targetCode(), targetMethod.targetCodeSize());
            Disassembler.disassemble(byteArrayOutputStream, code, platform.isa, platform.wordWidth(), startAddress.toLong(), inlineDataDecoder, disassemblyPrinter);
            return byteArrayOutputStream.toString();
        }
        return "";
    }

    static class CachedInvocation {
        public CachedInvocation(Value[] args) {
            this.args = args;
        }
        final Value[] args;
        CiConstant result;
    }

    /**
     * Cache to speed up compile-time folding. This works as an invocation of a {@linkplain FOLD foldable}
     * method is guaranteed to be idempotent with respect its arguments.
     */
    private final HashMap<MethodActor, CachedInvocation> cache = new HashMap<MethodActor, CachedInvocation>();

    @Override
    public CiConstant invoke(RiMethod method, CiMethodInvokeArguments args) {
        if (C1XOptions.CanonicalizeFoldableMethods && method.isResolved()) {
            MethodActor methodActor = (MethodActor) method;
            if (Actor.isDeclaredFoldable(methodActor.flags())) {
                Value[] values;
                int length = methodActor.descriptor().argumentCount(!methodActor.isStatic());
                if (length == 0) {
                    values = Value.NONE;
                } else {
                    values = new Value[length];
                    for (int i = 0; i < length; ++i) {
                        CiConstant arg = args.nextArg();
                        if (arg == null) {
                            return null;
                        }
                        Value value;
                        // Checkstyle: stop
                        switch (arg.kind) {
                            case Boolean: value = BooleanValue.from(arg.asBoolean()); break;
                            case Byte:    value = ByteValue.from((byte) arg.asInt()); break;
                            case Char:    value = CharValue.from((char) arg.asInt()); break;
                            case Double:  value = DoubleValue.from(arg.asDouble()); break;
                            case Float:   value = FloatValue.from(arg.asFloat()); break;
                            case Int:     value = IntValue.from(arg.asInt()); break;
                            case Long:    value = LongValue.from(arg.asLong()); break;
                            case Object:  value = ReferenceValue.from(arg.asObject()); break;
                            case Short:   value = ShortValue.from((short) arg.asInt()); break;
                            case Word:    value = WordValue.from(Address.fromLong(arg.asLong())); break;
                            default: throw new IllegalArgumentException();
                        }
                        // Checkstyle: resume
                        values[i] = value;
                    }
                }

                CachedInvocation cachedInvocation = null;
                if (!MaxineVM.isHosted()) {
                    synchronized (cache) {
                        cachedInvocation = cache.get(methodActor);
                        if (cachedInvocation != null) {
                            if (Arrays.equals(values, cachedInvocation.args)) {
                                return cachedInvocation.result;
                            }
                        } else {
                            cachedInvocation = new CachedInvocation(values);
                            cache.put(methodActor, cachedInvocation);
                        }
                    }
                }

                try {
                    // attempt to invoke the method
                    CiConstant result = methodActor.invoke(values).asCiConstant();
                    // set the result of this instruction to be the result of invocation
                    C1XMetrics.MethodsFolded++;

                    if (!MaxineVM.isHosted()) {
                        cachedInvocation.result = result;
                    }

                    return result;
                    // note that for void, we will have a void constant with value null
                } catch (IllegalAccessException e) {
                    // folding failed; too bad
                } catch (InvocationTargetException e) {
                    // folding failed; too bad
                } catch (ExceptionInInitializerError e) {
                    // folding failed; too bad
                }
                return null;
            }
        }
        return null;
    }

    public CiConstant foldWordOperation(int opcode, CiMethodInvokeArguments args) {
        CiConstant x = args.nextArg();
        CiConstant y = args.nextArg();
        switch (opcode) {
            case WDIV:
                return CiConstant.forWord(Address.fromLong(x.asLong()).dividedBy(Address.fromLong(y.asLong())).toLong());
            case WDIVI:
                return CiConstant.forWord(Address.fromLong(x.asLong()).dividedBy(y.asInt()).toLong());
            case WREM:
                return CiConstant.forWord(Address.fromLong(x.asLong()).remainder(Address.fromLong(y.asLong())).toLong());
            case WREMI:
                return CiConstant.forInt(Address.fromLong(x.asLong()).remainder(y.asInt()));
        }
        return null;
    }

    public Object registerGlobalStub(CiTargetMethod ciTargetMethod, String name) {
        return new C1XTargetMethod(GlobalStub, name, ciTargetMethod);
    }

    public RiType getRiType(Class<?> javaClass) {
        return ClassActor.fromJava(javaClass);
    }

    public RiMethod getRiMethod(Method method) {
        return MethodActor.fromJava(method);
    }

    public RiMethod getRiMethod(Constructor< ? > constructor) {
        return MethodActor.fromJavaConstructor(constructor);
    }

    public RiField getRiField(Field field) {
        return FieldActor.fromJava(field);
    }

    public RiSnippets getSnippets() {
        if (snippets == null) {
            snippets = new MaxRiSnippets(this);
        }
        return snippets;
    }

    public boolean compareConstantObjects(Object x, Object y) {
        return x == y;
    }

    public boolean recordLeafMethodAssumption(RiMethod method) {
        return ClassDirectory.recordLeafMethodAssumption(method);
    }

    public RiRegisterConfig getRegisterConfig(RiMethod method) {
        return vm().registerConfigs.getRegisterConfig((ClassMethodActor) method);
    }

    @Override
    public int getCustomStackAreaSize() {
        return 0;
    }

    @Override
    public boolean supportsArrayCopyIntrinsic() {
        return false;
    }
}
