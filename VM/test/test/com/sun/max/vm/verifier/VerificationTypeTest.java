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
public class VerificationTypeTest extends CompilerTestCase<TargetMethod> {

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

    private Verifier _verifier;

    private ObjectType _classAType;
    private ObjectType _classBType;
    private ObjectType _classCType;

    private ObjectType _classAArrayType;
    private ObjectType _classBArrayType;
    private ObjectType _classCArrayType;

    private ObjectType _interfaceAType;
    private ObjectType _interfaceBType;
    private ObjectType _interfaceCType;

    private ObjectType getObjectType(TypeDescriptor typeDescriptor) {
        return _verifier.getObjectType(typeDescriptor);
    }

    @Override
    public void setUp() {
        if (_verifier != null) {
            return;
        }
        final ClassActor classActor = ClassActor.fromJava(VerificationTypeTest.class);
        final ConstantPool constantPool = classActor.constantPool();
        _verifier = new Verifier(constantPool);

        _classAType = getObjectType(JavaTypeDescriptor.forJavaClass(TestClassA.class));
        _classBType = getObjectType(JavaTypeDescriptor.forJavaClass(TestClassB.class));
        _classCType = getObjectType(JavaTypeDescriptor.forJavaClass(TestClassC.class));

        _classAArrayType = getObjectType(JavaTypeDescriptor.getArrayDescriptorForDescriptor(JavaTypeDescriptor.forJavaClass(TestClassA.class), 1));
        _classBArrayType = getObjectType(JavaTypeDescriptor.getArrayDescriptorForDescriptor(JavaTypeDescriptor.forJavaClass(TestClassB.class), 1));
        _classCArrayType = getObjectType(JavaTypeDescriptor.getArrayDescriptorForDescriptor(JavaTypeDescriptor.forJavaClass(TestClassC.class), 1));

        _interfaceAType = getObjectType(JavaTypeDescriptor.forJavaClass(TestInterfaceA.class));
        _interfaceBType = getObjectType(JavaTypeDescriptor.forJavaClass(TestInterfaceB.class));
        _interfaceCType = getObjectType(JavaTypeDescriptor.forJavaClass(TestInterfaceC.class));
    }

    public void test_classfileTag() {
        _verifier.constantPool().edit(new ConstantPoolEditorClient() {
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
                        final VerificationType readType = VerificationType.readVerificationType(classfileStream, _verifier);
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
                final VerificationType retrievedType = _verifier.getVerificationType(typeDescriptor);
                assertTrue(type + " != " + retrievedType, type == retrievedType);
                if (type instanceof ObjectType) {
                    final ObjectType retrievedObjectType = _verifier.getObjectType(typeDescriptor);
                    assertTrue(type + " != " + retrievedObjectType, type == retrievedObjectType);
                }
            }
        }

        final int[] testAddresses = {0, 1, 10, 100, 1000, 10000, Character.MAX_VALUE - 1, Character.MAX_VALUE};

        for (int address : testAddresses) {
            final UninitializedNewType uninitializedNewType = _verifier.getUninitializedNewType(address);
            assertTrue(uninitializedNewType == _verifier.getUninitializedNewType(address));
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

        assertIsAssignableFrom(REFERENCE, _classAType);
        assertIsAssignableFrom(REFERENCE, _classAArrayType);

        //TODO uninitialized

        assertIsAssignableFrom(_classAType, NULL);
        assertIsAssignableFrom(_classAArrayType, NULL);

        assertTrue(JavaTypeDescriptor.BOOLEAN.equals(BOOLEAN.typeDescriptor()));
        assertTrue(JavaTypeDescriptor.BYTE.equals(BYTE.typeDescriptor()));
        assertTrue(JavaTypeDescriptor.SHORT.equals(SHORT.typeDescriptor()));
        assertTrue(JavaTypeDescriptor.CHAR.equals(CHAR.typeDescriptor()));
        assertTrue(JavaTypeDescriptor.INT.equals(INTEGER.typeDescriptor()));
        assertTrue(JavaTypeDescriptor.FLOAT.equals(FLOAT.typeDescriptor()));
        assertTrue(JavaTypeDescriptor.LONG.equals(LONG.typeDescriptor()));
        assertTrue(JavaTypeDescriptor.DOUBLE.equals(DOUBLE.typeDescriptor()));


        // For assignments, interfaces are treated like Object.
        assertIsAssignableFrom(_interfaceAType, _classAType);
        assertNotIsAssignableFrom(_classAType, _interfaceAType);
        assertNotIsAssignableFrom(_classCType, _interfaceCType);
        assertIsAssignableFrom(_interfaceCType, _classCType);

        assertIsAssignableFrom(_classAType, _classBType);
        assertNotIsAssignableFrom(_classBType, _classAType);
        assertNotIsAssignableFrom(_classCType, _classAType);
        assertNotIsAssignableFrom(_classAType, _classCType);

        // Arrays are subtypes of Object, Cloneable and java.io.Serializable.
        for (VerificationType type : ALL_PREDEFINED_TYPES) {
            if (type.isArray()) {
                assertIsAssignableFrom(OBJECT, type);
                assertIsAssignableFrom(CLONEABLE, type);
                assertIsAssignableFrom(SERIALIZABLE, type);
            }
        }
        assertIsAssignableFrom(OBJECT, _classAArrayType);
        assertIsAssignableFrom(CLONEABLE, _classAArrayType);
        assertIsAssignableFrom(SERIALIZABLE, _classAArrayType);

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
        assertIsAssignableFrom(_classAType, _classBType);
        assertIsAssignableFrom(_classAArrayType, _classBArrayType);

        // Null is assignable to any reference type
        assertIsAssignableFrom(_classAType, NULL);
        assertIsAssignableFrom(_classAArrayType, NULL);

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
