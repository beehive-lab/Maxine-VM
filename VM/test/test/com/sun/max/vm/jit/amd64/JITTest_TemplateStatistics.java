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

import com.sun.max.program.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.template.source.*;
/**
 * Just a quick test to count spill dependencies. Not part of regression.
 *
 * @author Laurent Daynes
 */
public class JITTest_TemplateStatistics extends TemplateTableTestCase {

    public static Test suite() {
        final TestSuite suite = new TestSuite(JITTest_TemplateStatistics.class.getSimpleName());
        // $JUnit-BEGIN$
        suite.addTestSuite(JITTest_TemplateStatistics.class);
        // $JUnit-END$
        return new AMD64JITTestSetup(suite);
    }

    /**
     * Builds a histogram of stop positions and return maximum number of stop positions for any templates.
     */
    private int countStopPositions(TemplateTable templateTable, Class< ? > templateSource, final int [] stopPositionHistogram) {
        final int [] maxStopPosition = new int[] {0 };
        final TemplateProcessor processor = new TemplateProcessor() {
            public void processTemplate(CompiledBytecodeTemplate template) {
                final int numStopPositions = template.targetMethod().numberOfStopPositions();
                if (numStopPositions > maxStopPosition[0]) {
                    maxStopPosition[0] = numStopPositions;
                }
                if (numStopPositions < stopPositionHistogram.length) {
                    stopPositionHistogram[numStopPositions]++;
                } else {
                    stopPositionHistogram[stopPositionHistogram.length - 1]++;
                }
                if (numStopPositions > 0) {
                    final TargetMethod targetTemplate = template.targetMethod();
                    Trace.line(1, targetTemplate.name() + " comprises #"  + numStopPositions + "stop positions (#direct calls: " + targetTemplate.numberOfDirectCalls() +
                                    ", #indirect calls " + targetTemplate.numberOfIndirectCalls() + ")");

                }
            }
        };
        templateTableIterate(templateTable, templateSource, processor);
        return maxStopPosition[0];
    }

    public void test_countStopPositions() {
        Trace.on(1);
        final int [] stopPositionsHistogram = new int [100];
        final TemplateTable templateTable = new TemplateTable(TemplateTableConfiguration.OPTIMIZED_TEMPLATE_SOURCES);
        for (Class templateSource : TemplateTableConfiguration.OPTIMIZED_TEMPLATE_SOURCES) {
            final int maxSopOffset = countStopPositions(templateTable, templateSource, stopPositionsHistogram);
            Trace.line(1, "Max stop positions for template sources " + templateSource.getCanonicalName() + " " + maxSopOffset);
            Trace.line(1, "--------------------------------------------------------------------------------------------------------------------------\n");
        }
        printHistogram("Histogram of stop positions", "stop positions",  "templates", stopPositionsHistogram);
    }


    private void printHistogram(String title, String indexTitle, String valueTitle, int [] histogram) {
        Trace.line(1, title);
        Trace.line(1, "# " + indexTitle + ", # " + valueTitle);
        final int last =  histogram.length - 1;
        int totalNonNullValue = -histogram[0];
        for (int i = 0; i < last; i++) {
            if (histogram[i] > 0) {
                Trace.line(1, i + ", " +  histogram[i]);
                totalNonNullValue += histogram[i];
            }
        }
        if (histogram[last] > 0) {
            Trace.line(1, ">= " + last + ", " +  histogram[last]);
        }
        Trace.line(1, "Total # " + valueTitle + " with " + indexTitle + " : " + totalNonNullValue);
    }
}
