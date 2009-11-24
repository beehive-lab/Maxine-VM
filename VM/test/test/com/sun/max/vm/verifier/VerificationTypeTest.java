/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package test.com.sun.max.vm.verifier;

import static com.sun.max.vm.verifier.types.VerificationType.*;

import java.io.*;

import junit.framework.*;
import test.com.sun.max.vm.compiler.*;

import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.verifier.*;
import com.sun.max.vm.verifier.types.*;

/**
 * @author David Liu
 * @author Doug Simon
 */
public class VerificationTypeTest extends CompilerTestCase<CPSTargetMethod> {

    public static Test suite() {
        final TestSuite suite = new TestSuite(VerificationTypeTest.class.getSimpleName());
        suite.addTestSuite(VerificationTypeTest.class);
        return new VerifierTestSetup(suite);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(VerificationTypeTest.suite());
    }

    public VerificationTypeTest(String name) {
        super(name);
    }

    private Verifier verifier;

    private ObjectType classAType;
    private ObjectType classBType;
    private ObjectType classCType;

    private ObjectType classAArrayType;
    private ObjectType classBArrayType;
    private ObjectType classCArrayType;

    private ObjectType interfaceAType;
    private ObjectType interfaceBType;
    private ObjectType interfaceCType;

    private ObjectType getObjectType(TypeDescriptor typeDescriptor) {
        return verifier.getObjectType(typeDescriptor);
    }

    @Override
    public void setUp() {
        if (verifier != null) {
            return;
        }
        final ClassActor classActor = ClassActor.fromJava(VerificationTypeTest.class);
        final ConstantPool constantPool = classActor.constantPool();
        verifier = new Verifier(constantPool);

        classAType = getObjectType(JavaTypeDescriptor.forJavaClass(TestClassA.class));
        classBType = getObjectType(JavaTypeDescriptor.forJavaClass(TestClassB.class));
        classCType = getObjectType(JavaTypeDescriptor.forJavaClass(TestClassC.class));

        classAArrayType = getObjectType(JavaTypeDescriptor.getArrayDescriptorForDescriptor(JavaTypeDescriptor.forJavaClass(TestClassA.class), 1));
        classBArrayType = getObjectType(JavaTypeDescriptor.getArrayDescriptorForDescriptor(JavaTypeDescriptor.forJavaClass(TestClassB.class), 1));
        classCArrayType = getObjectType(JavaTypeDescriptor.getArrayDescriptorForDescriptor(JavaTypeDescriptor.forJavaClass(TestClassC.class), 1));

        interfaceAType = getObjectType(JavaTypeDescriptor.forJavaClass(TestInterfaceA.class));
        interfaceBType = getObjectType(JavaTypeDescriptor.forJavaClass(TestInterfaceB.class));
        interfaceCType = getObjectType(JavaTypeDescriptor.forJavaClass(TestInterfaceC.class));
    }

    public void test_classfileTag() {
        verifier.constantPool().edit(new ConstantPoolEditorClient() {
            public void edit(ConstantPoolEditor constantPoolEditor) {
                for (VerificationType type : ALL_PREDEFINED_TYPES) {
                    Trace.line(1, "test_classfileTag: " + type);
                    final int classfileTag = type.classfileTag();
                    if (classfileTag != -1) {
                        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        try {
                            type.write(new DataOutputStream(byteArrayOutputStream), constantPoolEditor);
                        } catch (IOException e) {
                            e.printStackTrace();
                            fail();
                        }
                        final ClassfileStream classfileStream = new ClassfileStream(byteArrayOutputStream.toByteArray());
                        final VerificationType readType = VerificationType.readVerificationType(classfileStream, verifier);
                        assertTrue(readType + " != " + type, type.classfileTag() == readType.classfileTag());
                    } else {
                        try {
                            type.write(null, constantPoolEditor);
                            fail("expected IllegalArgumentException when writing " + type);
                        } catch (IllegalArgumentException e) {
                            // succeed
                        } catch (Exception e) {
                            e.printStackTrace();
                            fail();
                        }
                    }
                }
            }
        });
    }

    public void test_identity() {
        for (VerificationType type : ALL_PREDEFINED_TYPES) {
            final TypeDescriptor typeDescriptor = type.typeDescriptor();
            if (typeDescriptor != null) {
                final VerificationType retrievedType = verifier.getVerificationType(typeDescriptor);
                assertTrue(type + " != " + retrievedType, type == retrievedType);
                if (type instanceof ObjectType) {
                    final ObjectType retrievedObjectType = verifier.getObjectType(typeDescriptor);
                    assertTrue(type + " != " + retrievedObjectType, type == retrievedObjectType);
                }
            }
        }

        final int[] testAddresses = {0, 1, 10, 100, 1000, 10000, Character.MAX_VALUE - 1, Character.MAX_VALUE};

        for (int address : testAddresses) {
            final UninitializedNewType uninitializedNewType = verifier.getUninitializedNewType(address);
            assertTrue(uninitializedNewType == verifier.getUninitializedNewType(address));
        }
    }

    private static void assertIsAssignableFrom(VerificationType to, VerificationType from) {
        if (!to.isAssignableFrom(from)) {
            fail(to + " should be assignable from " + from);
        }
    }

    private static void assertNotIsAssignableFrom(VerificationType to, VerificationType from) {
        if (to.isAssignableFrom(from)) {
            fail(to + " should not be assignable from " + from);
        }
    }

    public void test_isAssignableFrom() {

        assertIsAssignableFrom(REFERENCE, classAType);
        assertIsAssignableFrom(REFERENCE, classAArrayType);

        //TODO uninitialized

        assertIsAssignableFrom(classAType, NULL);
        assertIsAssignableFrom(classAArrayType, NULL);

        assertTrue(JavaTypeDescriptor.BOOLEAN.equals(BOOLEAN.typeDescriptor()));
        assertTrue(JavaTypeDescriptor.BYTE.equals(BYTE.typeDescriptor()));
        assertTrue(JavaTypeDescriptor.SHORT.equals(SHORT.typeDescriptor()));
        assertTrue(JavaTypeDescriptor.CHAR.equals(CHAR.typeDescriptor()));
        assertTrue(JavaTypeDescriptor.INT.equals(INTEGER.typeDescriptor()));
        assertTrue(JavaTypeDescriptor.FLOAT.equals(FLOAT.typeDescriptor()));
        assertTrue(JavaTypeDescriptor.LONG.equals(LONG.typeDescriptor()));
        assertTrue(JavaTypeDescriptor.DOUBLE.equals(DOUBLE.typeDescriptor()));

        // For assignments, interfaces are treated like Object.
        assertIsAssignableFrom(interfaceAType, classAType);
        assertNotIsAssignableFrom(classAType, interfaceAType);
        assertNotIsAssignableFrom(classCType, interfaceCType);
        assertIsAssignableFrom(interfaceCType, classCType);

        assertIsAssignableFrom(classAType, classBType);
        assertNotIsAssignableFrom(classBType, classAType);
        assertNotIsAssignableFrom(classCType, classAType);
        assertNotIsAssignableFrom(classAType, classCType);

        // Arrays are subtypes of Object, Cloneable and java.io.Serializable.
        for (VerificationType type : ALL_PREDEFINED_TYPES) {
            if (type.isArray()) {
                assertIsAssignableFrom(OBJECT, type);
                assertIsAssignableFrom(CLONEABLE, type);
                assertIsAssignableFrom(SERIALIZABLE, type);
            }
        }
        assertIsAssignableFrom(OBJECT, classAArrayType);
        assertIsAssignableFrom(CLONEABLE, classAArrayType);
        assertIsAssignableFrom(SERIALIZABLE, classAArrayType);

        // The subtyping relation between arrays of primitive types is the identity relation.
        for (VerificationType type : PRIMITIVE_ARRAY_TYPES) {
            assertIsAssignableFrom(type, type);
            for (VerificationType otherType : PRIMITIVE_ARRAY_TYPES) {
                if (type != otherType) {
                    assertNotIsAssignableFrom(type, otherType);
                }
            }
        }

        // Subtyping between arrays of reference type is covariant.
        assertIsAssignableFrom(classAType, classBType);
        assertIsAssignableFrom(classAArrayType, classBArrayType);

        // Null is assignable to any reference type
        assertIsAssignableFrom(classAType, NULL);
        assertIsAssignableFrom(classAArrayType, NULL);

        // Subclassing is reflexive.

    }

    private static class TestClassA {
    }

    private static class TestClassB extends TestClassA {
    }

    private static class TestClassC implements TestInterfaceC {
    }

    private static interface TestInterfaceA {
    }

    private static interface TestInterfaceB extends TestInterfaceA {
    }

    private static interface TestInterfaceC {
    }
}
