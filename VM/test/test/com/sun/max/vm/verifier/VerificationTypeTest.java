/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.verifier;

import static com.sun.max.vm.verifier.types.VerificationType.*;

import java.io.*;

import junit.framework.*;
import test.com.sun.max.vm.*;

import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.verifier.*;
import com.sun.max.vm.verifier.types.*;

/**
 * @author David Liu
 * @author Doug Simon
 */
public class VerificationTypeTest extends VmTestCase {

    public static Test suite() {
        final TestSuite suite = new TestSuite(VerificationTypeTest.class.getSimpleName());
        suite.addTestSuite(VerificationTypeTest.class);
        return new VmTestSetup(suite);
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
        return (ObjectType) verifier.getObjectType(typeDescriptor);
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
                    final ObjectType retrievedObjectType = (ObjectType) verifier.getObjectType(typeDescriptor);
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
