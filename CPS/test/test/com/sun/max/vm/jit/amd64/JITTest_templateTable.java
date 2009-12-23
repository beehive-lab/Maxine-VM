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
package test.com.sun.max.vm.jit.amd64;

import junit.framework.*;
import test.com.sun.max.vm.jit.*;

import com.sun.max.vm.template.source.*;
/**
 * Testing of template generation, for exploratory purposes (not part of regression tests).
 *
 * @author Laurent Daynes
 */
public class JITTest_templateTable extends TemplateTableTestCase {
    public static void main(String[] args) {
        junit.textui.TestRunner.run(JITTest_templateTable.suite());
    }
/*
    public void test_generate() {
        generateTable(TestBytecodeSource.class);
    }

    public void test_generateOptimized() {
        generateTable(TemplateTableConfiguration.OPTIMIZED_TEMPLATE_SOURCES);
    }

    public void test_generateResolved() {
        generateTable(Arrays.append(TemplateTableConfiguration.UNOPTIMIZED_TEMPLATE_SOURCES, TemplateTableConfiguration.RESOLVED_TEMPLATE_SOURCES));
    }*/

    public void test_generatedUnoptimized() {
        generateTable(TemplateTableConfiguration.UNOPTIMIZED_TEMPLATE_SOURCES);
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(JITTest_templateTable.class.getSimpleName());
        suite.addTestSuite(JITTest_templateTable.class);
        return new AMD64JITTestSetup(suite);
    }
}
