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
/*VCSID=88479f87-71c1-44fd-840f-4c0b8b81b8ec*/
// Checkstyle: stop

package test.hotpath;

/*
 * @Harness: java
 * @Runs: 100 = 0.6248571921291398d;
 */
public class HP_series {

    public static double test(int count) {
        final int arrayRows = count;
        final double[][] testArray = new double[2][arrayRows];
        double omega; // Fundamental frequency.
        testArray[0][0] = TrapezoidIntegrate(0.0, // Lower bound.
                        2.0, // Upper bound.
                        1000, // # of steps.
                        0.0, // No omega*n needed.
                        0) / 2.0; // 0 = term A[0].
        omega = 3.1415926535897932;
        for (int i = 1; i < arrayRows; i++) {
            testArray[0][i] = TrapezoidIntegrate(0.0, 2.0, 1000, omega * i, 1); // 1 = cosine
            // term.
            testArray[1][i] = TrapezoidIntegrate(0.0, 2.0, 1000, omega * i, 2); // 2 = sine
            // term.
        }
        final double ref[][] = { { 2.8729524964837996, 0.0}, { 1.1161046676147888, -1.8819691893398025}, { 0.34429060398168704, -1.1645642623320958}, { 0.15238898702519288, -0.8143461113044298}};
        double error = 0.0;
        double sum = 0.0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 2; j++) {
                error += Math.abs(testArray[j][i] - ref[i][j]);
                sum += testArray[j][i];
            }
        }
        return sum + error;
    }

    private static double TrapezoidIntegrate(double x0, // Lower bound.
                    double x1, // Upper bound.
                    int nsteps, // # of steps.
                    double omegan, // omega * n.
                    int select) // Term type.
    {
        double x; // Independent variable.
        double dx; // Step size.
        double rvalue; // Return value.

        x = x0;
        dx = (x1 - x0) / nsteps;
        rvalue = thefunction(x0, omegan, select) / 2.0;
        if (nsteps != 1) {
            --nsteps; // Already done 1 step.
            while (--nsteps > 0) {
                x += dx;
                rvalue += thefunction(x, omegan, select);
            }
        }
        rvalue = (rvalue + thefunction(x1, omegan, select) / 2.0) * dx;
        return (rvalue);
    }

    private static double thefunction(double x, // Independent variable.
                    double omegan, // Omega * term.
                    int select) // Choose type.
    {
        switch (select) {
            case 0:
                return (Math.pow(x + 1.0, x));
            case 1:
                return (Math.pow(x + 1.0, x) * Math.cos(omegan * x));
            case 2:
                return (Math.pow(x + 1.0, x) * Math.sin(omegan * x));
        }
        return (0.0);
    }
}
