/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package jtt.optimize;

import java.util.*;


/*
 * @Harness: java
 * @Runs: 0=0; 10=10; 20=20; 40=38;
 */
public class Conditional01 {
    public int negative;
    private static final int RAM_SIZE = 0x100;

    public Conditional01() {
        negative = -5;
    }

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
            i2.r2.num = rnd.nextInt(0x100);
            try {
                c.visit(i2);
            } catch (RuntimeException re) {
            }
        }
        return c.cyclesConsumed;
    }

    private static class Register {
        int negative;
        int val;
        int num;

        Register() {
            negative = -8;
            val = -2;
            num = -3;
        }
    }

    private static class CPC {
        public int negative;
        public Register r1;
        public Register r2;

        CPC() {
            negative = -1;
            r1 = new Register();
            r2 = new Register();
        }

        public void setNegative() {
            negative = -1;
        }
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
        int tmp0 = getRegisterByte(i.r1);
        int tmp1 = getRegisterByte(i.r2);
        int tmp2 = bit(C);
        int tmp3 = tmp0 - tmp1 - tmp2;
        boolean tmp4 = (tmp0 & 128) != 0;
        boolean tmp5 = (tmp1 & 128) != 0;
        boolean tmp6 = (tmp3 & 128) != 0;
        boolean tmp7 = (tmp0 & 8) != 0;
        boolean tmp8 = (tmp1 & 8) != 0;
        boolean tmp9 = (tmp3 & 8) != 0;
        H = !tmp7 && tmp8 || tmp8 && tmp9 || tmp9 && !tmp7;
        C = !tmp4 && tmp5 || tmp5 && tmp6 || tmp6 && !tmp4;
        N = tmp6;
        Z = low(tmp3) == 0 && Z;
        V = tmp4 && !tmp5 && !tmp6 || !tmp4 && tmp5 && tmp6;
        S = N != V;
        cyclesConsumed++;
    }

    public int getRegisterByte(Register r1) {
        if ((r1.val % 10) == 0) {
            return sram[r1.num];
        }
        return r1.val;
    }

    public int low(int tmp3) {
        return tmp3 & 0x01;
    }

    public int bit(boolean c2) {
        return c2 ? 1 : 0;
    }
}
