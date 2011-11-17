/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package jtt.optimize;

import java.util.*;


/*
 * @Harness: java
 * @Runs: 0=0; 10=10; 20=20; 40=38;
 */
public class Conditional01 {
    private static final int RAM_SIZE = 0x100;

    public static int test(int arg) {
        Conditional01 c = new Conditional01();
        Random rnd = new Random();
        for (int i = 0; i < arg; i++) {
            CPC i2 = new CPC();
            i2.r1 = new Register();
            i2.r1.val = i;
            i2.r1.num = i + RAM_SIZE - 20;
            i2.r2 = new Register();
            i2.r2.val = rnd.nextInt();
            i2.r2.num = rnd.nextInt(RAM_SIZE);
            try {
                c.visit(i2);
            } catch (RuntimeException re) {

            }
        }
        return c.cyclesConsumed;
    }

    private static class Register {
        int val;
        int num;
    }

    private static class CPC {
        public Register r1;
        public Register r2;

    }

    private int nextPC;
    private int pc;
    private boolean C;
    private boolean H;
    private boolean N;
    private boolean Z;
    private boolean V;
    private boolean S;
    private int cyclesConsumed;
    private int[] sram = new int[RAM_SIZE];

    public void visit(CPC i) {
        nextPC = pc + 2;
        int tmp_0 = getRegisterByte(i.r1);
        int tmp_1 = getRegisterByte(i.r2);
        int tmp_2 = bit(C);
        int tmp_3 = tmp_0 - tmp_1 - tmp_2;
        boolean tmp_4 = ((tmp_0 & 128) != 0);
        boolean tmp_5 = ((tmp_1 & 128) != 0);
        boolean tmp_6 = ((tmp_3 & 128) != 0);
        boolean tmp_7 = ((tmp_0 & 8) != 0);
        boolean tmp_8 = ((tmp_1 & 8) != 0);
        boolean tmp_9 = ((tmp_3 & 8) != 0);
        H = !tmp_7 && tmp_8 || tmp_8 && tmp_9 || tmp_9 && !tmp_7;
        C = !tmp_4 && tmp_5 || tmp_5 && tmp_6 || tmp_6 && !tmp_4;
        N = tmp_6;
        Z = low(tmp_3) == 0 && Z;
        V = tmp_4 && !tmp_5 && !tmp_6 || !tmp_4 && tmp_5 && tmp_6;
        S = (N != V);
        cyclesConsumed++;
    }

    public int getRegisterByte(Register r1) {
        if ((r1.val % 10) == 0) {
            return sram [r1.num];
        }
        return r1.val;
    }

    public int low(int tmp_3) {
        return tmp_3 & 0x01;
    }

    public int bit(boolean c2) {
        return c2 ? 1 : 0;
    }
}
