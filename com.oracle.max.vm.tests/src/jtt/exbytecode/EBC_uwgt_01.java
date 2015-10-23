/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
/*
 * @Harness: java
 * @Runs: (64, 47) = true; (-2, 1) = true
 */
package jtt.exbytecode;

import com.oracle.max.cri.intrinsics.*;

public class EBC_uwgt_01 {
    public static boolean test(long a, long b) {
	boolean value = false;
	System.out.println(a);
	System.out.println(b);
	boolean trues[] = {false, true, false, true};
	boolean falses[] = {false, false, true,  true};

	for(int i = 0; i < trues.length;i++)	{
		System.out.println( i + " IS " + (trues[i] ^ falses[i]));
	}
/*
@INTRINSIC(UCMP_AT)
    public static boolean aboveThan(long a, long b) {
        //return (a > b) ^ ((a < 0) != (b < 0));
        return (a > b) ^ ((a < 0L) != (b < 0L));
*/
	boolean termAgtB = a >b;
	boolean termAlt0 = a < 0;
	boolean termBlt0 = b < 0;
	boolean termRHS = (termAlt0 != termBlt0);
	boolean resVal = termAgtB ^ termRHS;
	System.out.println("(a>b) " + termAgtB);
	System.out.println("(a<0) " +termAlt0);
	System.out.println("(b < 0) " + termBlt0);
	System.out.println("((a<0) != (b<0) " + termRHS);
	System.out.println("(a>b) ^ (rhs) " + resVal);

	int zz = 0;
	do {
		value = UnsignedMath.aboveThan(a, b);
		if(zz++ < 20) {
			System.out.println("print max 20times... were ina  loop");
			System.out.println(" above than 64 47 " +  UnsignedMath.aboveThan(64L, 47L));
			System.out.println(" above than 11 11 " +  UnsignedMath.aboveThan(11L, 11L));
			System.out.println(" above than -2,1 " +  UnsignedMath.aboveThan(-2L, 1L));

		}
			
	} while(value == false) ;
        return UnsignedMath.aboveThan(a, b);
    }

}
