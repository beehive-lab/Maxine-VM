/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package test.crossisa.riscv64.asm;

import static com.oracle.max.asm.target.riscv.RISCV64.*;

import org.junit.*;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.riscv.*;
import com.sun.cri.ci.*;

import test.crossisa.*;

public class RISCV64AssemblerTest {

    private RISCVAssembler asm;

    public RISCV64AssemblerTest() {
        CiTarget risc64 = new CiTarget(new RISCV64(), true, 8, 0, 4096, 0, false, false, false, true);
        asm = new RISCVAssembler(risc64);
    }

    private static final boolean[] testValues = new boolean[MaxineRISCV64Tester.NUM_REGS];

    // Each test should set the contents of this array appropriately,
    // it enables the instruction under test to select the specific bit values for
    // comparison i.e. for example ignoring upper or lower 16bits for movt, movw
    // and for ignoring specific bits in the status register etc
    // concerning whether a carry has been set
    private static final MaxineRISCV64Tester.BitsFlag[] bitmasks =
            new MaxineRISCV64Tester.BitsFlag[MaxineRISCV64Tester.NUM_REGS];
    static {
        MaxineRISCV64Tester.setAllBitMasks(bitmasks, MaxineRISCV64Tester.BitsFlag.All64Bits);
    }

    private static void initializeTestValues() {
        for (int i = 0; i < testValues.length; i++) {
            testValues[i] = false;
        }
    }

    // The following values will be updated
    // to those expected to be found in a register after simulated execution of code.
    private static final long[] expectedValues = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};

    private static void initialiseExpectedValues() {
        for (int i = 0; i < MaxineRISCV64Tester.NUM_REGS; i++) {
            expectedValues[i] = i;
        }
    }

    /**
     * Sets the expected value of a register and enables it for inspection.
     *
     * @param cpuRegister the number of the cpuRegister
     * @param expectedValue the expected value
     */
    private static void setExpectedValue(CiRegister cpuRegister, long expectedValue) {
        final int index = cpuRegister.number - 1; // -1 to compensate for the zero register
        expectedValues[index] = expectedValue;
        testValues[index] = true;
    }

    private void generateAndTest(long[] expected, boolean[] tests, MaxineRISCV64Tester.BitsFlag[] masks, Buffer codeBuffer) throws Exception {
        RISCV64CodeWriter code = new RISCV64CodeWriter(codeBuffer);
        code.createCodeFile();
        MaxineRISCV64Tester r = new MaxineRISCV64Tester(expected, tests, masks);
        if (!CrossISATester.ENABLE_SIMULATOR) {
            System.out.println("Code Generation is disabled!");
            System.exit(1);
        }
        r.assembleStartup();
        r.compile();
        r.link();
        r.runSimulation();
        if (!r.validateLongRegisters()) {
            r.reset();
            assert false : "Error while validating long registers";
        }
        r.reset();
    }

    @Test
    public void lui() throws Exception {
        initialiseExpectedValues();
        MaxineRISCV64Tester.setAllBitMasks(bitmasks, MaxineRISCV64Tester.BitsFlag.All64Bits);
        initializeTestValues();
        asm.codeBuffer.reset();
        asm.lui(t0, 0xFF);
        asm.lui(t1, 0xFF << 12);
        asm.lui(t2, 0xFFFFFFFF << 12);
        setExpectedValue(t0, 0);
        setExpectedValue(t1, 0xFF000);
        setExpectedValue(t2, 0xFFFFF000);
        generateAndTest(expectedValues, testValues, bitmasks, asm.codeBuffer);
    }

}
