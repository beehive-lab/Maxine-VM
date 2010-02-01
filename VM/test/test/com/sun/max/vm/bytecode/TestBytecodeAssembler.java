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
package test.com.sun.max.vm.bytecode;

import static com.sun.max.vm.classfile.constant.SymbolTable.*;

import java.io.*;
import java.lang.reflect.*;

import com.sun.max.io.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.graft.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.ClassfileWriter.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.type.*;

/**
 * A facility for creating a class method actor via bytecode assembly. A subclass simply has to override
 * {@link #generateCode()} to emit the desired bytecode.
 *
 * @author Doug Simon
 */
public abstract class TestBytecodeAssembler extends BytecodeAssembler {

    private final boolean isStatic;
    private final Utf8Constant methodName;
    private final Utf8Constant className;
    private final SignatureDescriptor signature;
    private ClassMethodActor classMethodActor;

    /**
     * Generates a class method actor via bytecode assembly.
     *
     * @param isStatic specifies if the generated class method is static
     * @param className the {@linkplain ClassActor#name() name} of the class actor. If null, then the name will be
     *            derived from the {@code superClass} parameter provided to {@link #compile(Class)}.
     * @param methodName the {@linkplain Actor#name() name} of the class method actor
     * @param signature the {@linkplain MethodActor#descriptor() signature} of the class method actor
     */
    public TestBytecodeAssembler(boolean isStatic, String className, String methodName, SignatureDescriptor signature) {
        super(new ConstantPool(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER).edit());
        this.isStatic = isStatic;
        this.methodName = makeSymbol(methodName);
        this.className = className == null ? null : makeSymbol(className);
        this.signature = signature;
        this.codeStream = new SeekableByteArrayOutputStream();
        allocateParameters(isStatic, signature);
    }

    public TestBytecodeAssembler(boolean isStatic, String methodName, SignatureDescriptor signature) {
        this(isStatic, null, methodName, signature);
    }

    private final SeekableByteArrayOutputStream codeStream;

    @Override
    protected void setWritePosition(int position) {
        codeStream.seek(position);
    }

    @Override
    protected void writeByte(byte b) {
        codeStream.write(b);
    }

    @Override
    public byte[] code() {
        fixup();
        return codeStream.toByteArray();
    }

    protected abstract void generateCode();

    /**
     * Generates a default constructor that simply calls the super class' default constructor. If the
     * latter does not exist, then this method returns null.
     */
    private ClassMethodActor generateDefaultConstructor(Class<?> superClass) {
        final ByteArrayBytecodeAssembler asm = new ByteArrayBytecodeAssembler(constantPoolEditor());
        asm.allocateLocal(Kind.REFERENCE);

        try {
            final Constructor superDefaultConstructor = superClass.getDeclaredConstructor();
            asm.aload(0);
            asm.invokespecial(PoolConstantFactory.createClassMethodConstant(superDefaultConstructor), 1, 0);
            asm.vreturn();

            final CodeAttribute codeAttribute = new CodeAttribute(
                            constantPool(),
                            asm.code(),
                            (char) asm.maxStack(),
                            (char) asm.maxLocals(),
                            CodeAttribute.NO_EXCEPTION_HANDLER_TABLE,
                            LineNumberTable.EMPTY,
                            LocalVariableTable.EMPTY,
                            null);
            return new VirtualMethodActor(
                            SymbolTable.INIT,
                            SignatureDescriptor.fromJava(Void.TYPE),
                            Modifier.PUBLIC | Actor.INITIALIZER,
                            codeAttribute);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * @param superClass the super class of generated holder for the generated class method actor
     */
    public ClassMethodActor classMethodActor(Class superClass) {
        if (classMethodActor == null) {
            generateCode();
            final CodeAttribute codeAttribute = new CodeAttribute(
                            constantPool(),
                            code(),
                            (char) 100, // TODO: compute max stack
                            (char) maxLocals(),
                            CodeAttribute.NO_EXCEPTION_HANDLER_TABLE,
                            LineNumberTable.EMPTY,
                            LocalVariableTable.EMPTY,
                            null);
            classMethodActor = isStatic ?
                new StaticMethodActor(
                                methodName,
                                signature,
                                Modifier.PUBLIC | Modifier.STATIC,
                                codeAttribute) :
                new VirtualMethodActor(
                                methodName,
                                signature,
                                Modifier.PUBLIC,
                                codeAttribute);
            final Utf8Constant className = this.className == null ? makeSymbol(superClass.getName() + "_$GENERATED$_" + methodName) : this.className;
            final ClassMethodActor defaultConstructor = generateDefaultConstructor(superClass);
            final ClassMethodActor[] classMethodActors;
            if (defaultConstructor != null) {
                classMethodActors = new ClassMethodActor[]{classMethodActor, defaultConstructor};
            } else {
                classMethodActors = new ClassMethodActor[]{classMethodActor};
            }
            final ClassActor classActor = ClassActorFactory.createTupleOrHybridClassActor(
                constantPool(),
                BootClassLoader.BOOT_CLASS_LOADER,
                className,
                ClassfileReader.JAVA_1_5_VERSION,
                (char) 0,
                Modifier.PUBLIC | Actor.GENERATED,
                ClassActor.fromJava(superClass),
                new InterfaceActor[0],
                new FieldActor[0],
                classMethodActors,
                Actor.NO_GENERIC_SIGNATURE,
                Actor.NO_RUNTIME_VISIBLE_ANNOTATION_BYTES,
                ClassActor.NO_SOURCE_FILE_NAME,
                ClassActor.NO_INNER_CLASSES,
                ClassActor.NO_OUTER_CLASS,
                ClassActor.NO_ENCLOSING_METHOD_INFO);
            try {
                ClassfileWriter.saveGeneratedClass(new ClassInfo(classActor), constantPoolEditor());
            } catch (IOException e) {
                throw (NoClassDefFoundError) new NoClassDefFoundError(className.toString()).initCause(e);
            }
            constantPoolEditor().release();
        }
        return classMethodActor;
    }
}
