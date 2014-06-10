/***
 * ASM tests
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.objectweb.asm;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 * Super class for test suites based on a jar file.
 * 
 * @author Eugene Kuleshov
 * @author Eric Bruneton
 */
public abstract class AbstractTest extends TestCase {

    protected String n;

    protected InputStream is;

    public AbstractTest() {
        super("test");
    }

    protected void init(final String n, final InputStream is) {
        this.n = n;
        this.is = is;
    }

    protected TestSuite getSuite() throws Exception {
        TestSuite suite = new TestSuite(getClass().getName());
        String files = System.getProperty("asm.test") + ",";
        String clazz = System.getProperty("asm.test.class");
        String partcount = System.getProperty("parts");
        String partid = System.getProperty("part");
        int parts = partcount == null ? 1 : Integer.parseInt(partcount);
        int part = partid == null ? 0 : Integer.parseInt(partid);
        int id = 0;
        while (files.indexOf(',') != -1) {
            String file = files.substring(0, files.indexOf(','));
            files = files.substring(files.indexOf(',') + 1);
            File f = new File(file);
            if (f.isDirectory()) {
                scanDirectory("", f, suite, clazz);
            } else {
                ZipFile zip = new ZipFile(file);
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    String n = e.getName();
                    String p = n.replace('/', '.');
                    if (n.endsWith(".class")
                            && (clazz == null || p.indexOf(clazz) != -1)) {
                        n = p.substring(0, p.length() - 6);
                        if (id % parts == part) {
                            InputStream is = zip.getInputStream(e);
                            AbstractTest t = getClass().newInstance();
                            t.init(n, is);
                            suite.addTest(t);
                        }
                        ++id;
                    }
                }
            }
        }
        return suite;
    }

    private void scanDirectory(final String path, final File f,
            final TestSuite suite, final String clazz) throws Exception {
        File[] fs = f.listFiles();
        for (int i = 0; i < fs.length; ++i) {
            String n = fs[i].getName();
            String qn = path.length() == 0 ? n : path + "." + n;
            if (fs[i].isDirectory()) {
                scanDirectory(qn, fs[i], suite, clazz);
            } else if (qn.endsWith(".class") && !qn.startsWith("invalid.")) {
                if (clazz == null || qn.startsWith("pkg.")
                        || qn.indexOf(clazz) != -1) {
                    qn = qn.substring(0, qn.length() - 6);
                    InputStream is = new FileInputStream(fs[i]);
                    AbstractTest t = getClass().newInstance();
                    t.init(qn, is);
                    suite.addTest(t);
                }
            }
        }
    }

    public abstract void test() throws Exception;

    public void assertEquals(final ClassReader cr1, final ClassReader cr2)
            throws Exception {
        assertEquals(cr1, cr2, null, null);
    }

    public void assertEquals(final ClassReader cr1, final ClassReader cr2,
            final ClassVisitor filter1, final ClassVisitor filter2)
            throws Exception {
        if (!Arrays.equals(cr1.b, cr2.b)) {
            StringWriter sw1 = new StringWriter();
            StringWriter sw2 = new StringWriter();
            ClassVisitor cv1 = new TraceClassVisitor(new PrintWriter(sw1));
            ClassVisitor cv2 = new TraceClassVisitor(new PrintWriter(sw2));
            if (filter1 != null) {
                filter1.cv = cv1;
            }
            if (filter2 != null) {
                filter2.cv = cv2;
            }
            cr1.accept(filter1 == null ? cv1 : filter1, 0);
            cr2.accept(filter2 == null ? cv2 : filter2, 0);
            String s1 = sw1.toString();
            String s2 = sw2.toString();
            try {
                assertEquals("different data", s1, s2);
            } catch (Throwable e) {
                /*
                 * ClassReader may introduce unused labels in the code, due to
                 * the way uninitialized frame types are handled (false
                 * positives may occur, see the doc of ClassReader). These
                 * unused labels may differ before / after a transformation, but
                 * this does not change the method code itself. Thus, if a
                 * difference occurs, we retry a comparison, this time by
                 * removing the unused labels first (we do not do this in the
                 * first place because it is costly).
                 */
                sw1 = new StringWriter();
                sw2 = new StringWriter();
                cv1 = new RemoveUnusedLabelsAdapter(new TraceClassVisitor(
                        new PrintWriter(sw1)));
                cv2 = new RemoveUnusedLabelsAdapter(new TraceClassVisitor(
                        new PrintWriter(sw2)));
                if (filter1 != null) {
                    filter1.cv = cv1;
                }
                if (filter2 != null) {
                    filter2.cv = cv2;
                }
                cr1.accept(filter1 == null ? cv1 : filter1, 0);
                cr2.accept(filter2 == null ? cv2 : filter2, 0);
                s1 = sw1.toString();
                s2 = sw2.toString();
                assertEquals("different data", s1, s2);
            }
        }
    }

    @Override
    public String getName() {
        return super.getName() + ": " + n;
    }

    public static class VerifierTest extends TestCase {

        public VerifierTest() {
            super("testVerifier");
        }

        public void testVerifier() throws Exception {
            try {
                Class.forName("invalid.Invalid", true, getClass()
                        .getClassLoader());
                fail("The new JDK 7 verifier does not trigger!");
            } catch (VerifyError ve) {
                // This is expected since the class is invalid.
                ve.printStackTrace();
            }
        }
    }

    static class RemoveUnusedLabelsAdapter extends ClassVisitor {

        public RemoveUnusedLabelsAdapter(ClassVisitor cv) {
            super(Opcodes.ASM5, cv);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                String signature, String[] exceptions) {
            return new MethodNode(Opcodes.ASM5, access, name, desc, signature,
                    exceptions) {

                /**
                 * The labels used in this method.
                 */
                Set<LabelNode> usedLabels = new HashSet<LabelNode>();

                @Override
                public void visitLabel(final Label label) {
                    instructions.add(super.getLabelNode(label));
                }

                @Override
                public void visitEnd() {
                    // removes unused labels
                    ListIterator<AbstractInsnNode> i = instructions.iterator();
                    while (i.hasNext()) {
                        AbstractInsnNode n = i.next();
                        if (n instanceof LabelNode && !usedLabels.contains(n)) {
                            i.remove();
                        }
                    }
                    // visits the transformed code
                    accept(cv);
                }

                @Override
                protected LabelNode getLabelNode(final Label l) {
                    LabelNode n = super.getLabelNode(l);
                    usedLabels.add(n);
                    return n;
                }
            };
        }
    }
}
