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

import java.io.*;
import java.util.*;

import junit.framework.*;
import test.com.sun.max.vm.compiler.*;

import com.sun.max.collect.*;
import com.sun.max.lang.Arrays;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.bir.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.verifier.*;

/**
 * Tests for both the {@link TypeCheckingVerifier} and the {@link TypeInferencingVerifier}.
 * Unless program arguments are supplied, the tests simply verify all the methods in the
 * Java prototype.
 *
 * -jcklist=test/test/com/sun/max/vm/verifier/jck.classes.txt
 *
 * @author Doug Simon
 * @author David Liu
 */
public class VerifierTest extends CompilerTestCase<BirMethod> {

    private static final OptionSet options = new OptionSet();

    private static final Option<String> CLASS = options.newStringOption("class", null,
            "This option specifies the Java class to verify.");
    private static final Option<String> CLASSES = options.newStringOption("list", null,
            "This option specifies the name of a file that contains the classes to test.");
    private static final Option<File> JCKCLASSES = options.newFileOption("jcklist", (File) null,
            "This option specifies the name of a file that lists JCK classes to test.");
    private static final Option<Policy> POLICY = options.newEnumOption("policy", Policy.standard, Policy.class,
            "This option specifies the verification policy to use.");
    private static final Option<Integer> FAILURES = options.newIntegerOption("k", 1,
            "This option specifies how many tests to allow to fail before terminating.");
    private static final Option<Boolean> VMCLASSES = options.newBooleanOption("vmclasses", false,
            "This option determines whether the verifier will attempt to verify all classes in the VM class registry.");

    private static int failedTestThreshold;


    public static void main(String[] args) {
        setProgramArguments(args);
        junit.textui.TestRunner.run(VerifierTest.suite());
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(VerifierTest.class.getSimpleName());
        suite.addTestSuite(VerifierTest.class);
        return new VerifierTestSetup(suite);
    }

    private enum Policy {
        standard, jsr202, dfa
    }

    private static synchronized void parseProgramArguments() {
        if (initialized) {
            return;
        }
        initialized = true;
        Trace.addTo(options);

        options.parseArguments(getProgramArguments());
        setProgramArguments(options.getArgumentsAndUnrecognizedOptions().asArguments());
        failedTestThreshold = FAILURES.getValue();
    }

    public VerifierTest(String name) {
        super(name);
        parseProgramArguments();
    }

    private static boolean initialized;

    Sequence<String> readClassNames(File file) throws IOException {
        final AppendableSequence<String> lines = new ArrayListSequence<String>();
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            final String className = line.trim();
            if (!line.startsWith("#") && line.length() > 0) {
                lines.append(className);
            }
        }
        return lines;
    }

    public void test() throws Exception {
        verify("java.lang.Class", true);
        verify(JdtBadStackMapTable.class.getName(), true);

        int numberOfClassesVerified = 0;

        if (JsrInliningTestSource.compile()) {
            verify(JsrInliningTestSource.class.getName(), true);
            verify(JsrInliningTestSource.Unverifiable.class.getName(), false);
            numberOfClassesVerified += 2;
        }

        if (JCKCLASSES.getValue() != null) {
            Trace.line(1, "Running JCK test classes in " + JCKCLASSES.getValue().getAbsolutePath());
            for (String className : readClassNames(JCKCLASSES.getValue())) {
                verify(className, !className.endsWith("n"));
                ++numberOfClassesVerified;
            }
        }

        if (CLASSES.getValue() != null) {
            for (String className : readClassNames(new File(CLASSES.getValue()))) {
                verify(className, true);
                ++numberOfClassesVerified;
            }
        }

        if (CLASS.getValue() != null) {
            verify(CLASS.getValue(), true);
            ++numberOfClassesVerified;
        }

        int classActorIndex = 0;
        if (numberOfClassesVerified == 0 || VMCLASSES.getValue()) {
            ClassActor[] classActors = Arrays.from(ClassActor.class, ClassRegistry.vmClassRegistry());
            while (true) {
                while (classActorIndex != classActors.length) {
                    final ClassActor classActor = classActors[classActorIndex++];
                    if (classActor.isTupleClassActor()) {
                        final String name = classActor.name.toString();
                        if (!verifiedClasses.contains(name) && name.startsWith("com.") || name.startsWith("java.")) {
                            verify(name, true);
                            ++numberOfClassesVerified;
                        }
                    }
                }
                if (classActorIndex == ClassRegistry.vmClassRegistry().numberOfClassActors()) {
                    break;
                }
                classActors = Arrays.from(ClassActor.class, ClassRegistry.vmClassRegistry());
            }
        }
    }

    private void verify(String name, boolean isPositive) {
        try {
            verify0(name, isPositive);
        } catch (RuntimeException runtimeException) {
            if (--failedTestThreshold > 0) {
                addTestError(runtimeException);
            } else {
                throw runtimeException;
            }
        } catch (Error error) {
            if (--failedTestThreshold > 0) {
                addTestError(error);
            } else {
                throw error;
            }
        } finally {
            Trace.stream().flush();
        }
    }

    private static final Set<String> verifiedClasses = new HashSet<String>();

    private void verify0(String name, boolean isPositive) {

        verifiedClasses.add(name);

        ClassVerifier classVerifier = null;
        final ClassfileVersion classfileVersion = new ClassfileVersion(name, PrototypeClassLoader.PROTOTYPE_CLASS_LOADER.classpath());
        try {
            Trace.line(1, "verifying " + name);

            switch (POLICY.getValue()) {
                case jsr202:
                    if (classfileVersion.major < 50) {
                        return;
                    }
                    classVerifier = new TypeCheckingVerifier(loadClassActor(name));
                    break;
                case dfa:
                    classVerifier = new TypeInferencingVerifier(loadClassActor(name));
                    break;
                case standard:
                    classVerifier = Verifier.verifierFor(loadClassActor(name));
                    break;
                default:
                    ProgramError.unexpected();
            }

            classVerifier.verify();
            if (!isPositive) {
                repeatVerificationToDiagnoseFailure(classVerifier, null);
                fail(name + " did not fail verification as expected");
            }
        } catch (NoClassDefFoundError noClassDefFoundError) {
            if (isPositive) {
                throw noClassDefFoundError;
            }
        } catch (ClassFormatError classFormatError) {
            System.err.println(classFormatError);
            if (isPositive) {
                throw classFormatError;
            }
        } catch (VerifyError verifyError) {
            System.err.println(verifyError);
            if (isPositive) {
                if (verifyError instanceof ExtendedVerifyError) {
                    repeatVerificationToDiagnoseFailure(classVerifier, (ExtendedVerifyError) verifyError);
                }
                throw verifyError;
            }
        } catch (AssertionFailedError assertionFailedError) {
            throw assertionFailedError;
        } catch (Throwable verifyError) {
            repeatVerificationToDiagnoseFailure(classVerifier, null);
        }
    }

    private void repeatVerificationToDiagnoseFailure(ClassVerifier classVerifier, ExtendedVerifyError extendedVerifyError) {
        // Only repeat the very last failure
        if (failedTestThreshold == 1) {
            if (classVerifier != null) {
                classVerifier.verbose = true;
                if (extendedVerifyError != null) {
                    System.out.println(extendedVerifyError.getMessage());
                    extendedVerifyError.printCode(System.out);
                    System.out.flush();
                    classVerifier.verify(extendedVerifyError.classMethodActor, extendedVerifyError.codeAttribute);
                } else {
                    classVerifier.verify();
                }
            }
        }
    }

    private ClassActor loadClassActor(String name) {
        final TypeDescriptor typeDescriptor = JavaTypeDescriptor.getDescriptorForJavaString(name);
        final PrototypeClassLoader prototypeClassLoader = PrototypeClassLoader.PROTOTYPE_CLASS_LOADER;
        final Classpath classpath = prototypeClassLoader.classpath();
        ClassActor classActor = ClassRegistry.get(prototypeClassLoader, typeDescriptor, false);
        if (classActor == null) {
            final ClasspathFile classpathFile = classpath.readClassFile(name);
            if (classpathFile == null) {
                fail("Could not find class " + name + " on class path: " + classpath);
            }
            classActor = ClassfileReader.defineClassActor(name, VmClassLoader.VM_CLASS_LOADER, classpathFile.contents, null, classpathFile.classpathEntry);
        }
        return classActor;
    }

    public void test_jsr() throws Exception {
        final MethodActor classMethodActor = new TestBytecodeAssembler(true, "PerformJsr", "perform_jsr", SignatureDescriptor.create(int.class)) {
            @Override
            public void generateCode() {
                final Label subroutine = newLabel();
                final Label cont = newLabel();
                final int entry = currentAddress();
                final int retAddress = allocateLocal(Kind.REFERENCE);
                final int var = allocateLocal(Kind.INT);
                iconst(42);
                istore(var);
                jsr(subroutine);
                iload(var);
                ireturn();

                subroutine.bind();
                setStack(1);
                astore(retAddress);
                iconst(1);
                ifne(cont);

                aconst_null();
                astore(var);
                goto_(entry);

                cont.bind();
                ret(retAddress);
            }
        }.classMethodActor(Object.class);

        verify(classMethodActor.holder().name.toString(), true);

        assertTrue(classMethodActor.toJava().invoke(null).equals(42));
    }

    /**
     * Tests that a subroutine with two different entry points causes a verification error.
     */
    public void test_jsr2() throws Exception {
        final TestBytecodeAssembler asm = new TestBytecodeAssembler(true, "PerformJsr2", "perform_jsr2", SignatureDescriptor.create(int.class)) {
            @Override
            public void generateCode() {
                final Label subroutineEntry1 = newLabel();
                final Label subroutineEntry2 = newLabel();
                final int retAddress = allocateLocal(Kind.REFERENCE);
                final int var = allocateLocal(Kind.INT);
                iconst(42);
                istore(var);
                jsr(subroutineEntry1);
                jsr(subroutineEntry2);
                iload(var);
                ireturn();

                subroutineEntry1.bind();
                setStack(1);
                iconst(43);
                pop();
                subroutineEntry2.bind();
                astore(retAddress);
                iconst(44);
                pop();
                ret(retAddress);
            }
        };
        try {
            final MethodActor classMethodActor = asm.classMethodActor(Object.class);
            verify(classMethodActor.holder().name.toString(), false);
        } catch (VerifyError verifyError) {
            // success
        }
    }


    public static class Super {
        void callsPrivateMethodOnSubclassInstance() {
            final Super instance = new Super() {};
            instance.privateMethod();
        }

        private void privateMethod() {
        }
    }

    public void test_Super() {
        verify(Super.class.getName(), true);
    }
}
