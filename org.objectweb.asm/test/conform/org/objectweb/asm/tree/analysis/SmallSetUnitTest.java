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
package org.objectweb.asm.tree.analysis;

import java.util.Set;

import junit.framework.TestCase;

/**
 * SmallSet unit tests.
 * 
 * @author Eric Bruneton
 */
public class SmallSetUnitTest extends TestCase {

    private final Object A = new Object();
    private final Object B = new Object();
    private final Object C = new Object();
    private final Object D = new Object();

    public void testSubsetUnion() {
        SmallSet<Object> s1 = new SmallSet<Object>(A, B);
        SmallSet<Object> s2 = new SmallSet<Object>(A, null);
        Set<Object> u = s1.union(s2);
        Set<Object> v = s2.union(s1);
        assertEquals(u, v);
        s1.remove();
    }

    public void testDisjointUnion() {
        SmallSet<Object> s1 = new SmallSet<Object>(A, B);
        SmallSet<Object> s2 = new SmallSet<Object>(C, D);
        Set<Object> u = s1.union(s2);
        Set<Object> v = s2.union(s1);
        assertEquals(u, v);
    }
}
