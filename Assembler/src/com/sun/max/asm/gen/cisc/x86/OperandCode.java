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
package com.sun.max.asm.gen.cisc.x86;

import static com.sun.max.asm.gen.cisc.x86.AddressingMethodCode.*;
import static com.sun.max.asm.gen.cisc.x86.OperandTypeCode.*;

import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.asm.gen.*;
import com.sun.max.collect.*;
import com.sun.max.util.*;

/**
 * @author Bernd Mathiske
 */
public enum OperandCode implements WrappableSpecification {

    Ap(A, p),
    Cd(C, d),
    Cq(C, q),
    Dd(D, d),
    Dq(D, q),
    Eb(E, b),
    Ed(E, d),
    Ed_q(E, d_q),
    Ev(E, v),
    Ew(E, w),
    Fv(F, v),
    Gb(G, b),
    Gd(G, d),
    Gd_q(G, d_q),
    Gv(G, v),
    Gw(G, w),
    Gq(G, q),
    Gz(G, z),
    Ib(I, b),
    ICb(IC, b),
    Iv(I, v),
    Iw(I, w),
    Iz(I, z),
    Jb(J, b),
    Jv(J, v),
    Jz(J, z),
    Ma(M, a),
    Mb(M, b),
    Md(M, d),
    Md_q(M, d_q),
    Mp(M, p),
    Mq(M, q),
    Mdq(M, dq),
    Ms(M, s),
    Mv(M, v),
    Mw(M, w),
    Nb(N, b),
    Nd(N, d),
    Nd_q(N, d_q),
    Nv(N, v),
    Ob(O, b),
    Ov(O, v),
    Pd(P, d),
    Pdq(P, dq),
    Pq(P, q),
    PRq(PR, q),
    Qd(Q, d),
    Qq(Q, q),
    Rd(R, d),
    Rq(R, q),
    Rv(R, v),
    Sw(S, w),
    Vdq(V, dq),
    Vpd(V, pd),
    Vps(V, ps),
    Vq(V, q),
    Vsd(V, sd),
    Vss(V, ss),
    VRdq(VR, dq),
    VRpd(VR, pd),
    VRps(VR, ps),
    VRq(VR, q),
    Wdq(W, dq),
    Wpd(W, pd),
    Wps(W, ps),
    Wq(W, q),
    Wsd(W, sd),
    Wss(W, ss),
    Xb(X, b),
    Xv(X, v),
    Xz(X, z),
    Yb(Y, b),
    Yv(Y, v),
    Yz(Y, z);

    private final AddressingMethodCode _addressingMethodCode;
    private final OperandTypeCode _operandTypeCode;

    private OperandCode(AddressingMethodCode addressingMethodCode, OperandTypeCode operandTypeCode) {
        _addressingMethodCode = addressingMethodCode;
        _operandTypeCode = operandTypeCode;
    }

    public AddressingMethodCode addressingMethodCode() {
        return _addressingMethodCode;
    }

    public OperandTypeCode operandTypeCode() {
        return _operandTypeCode;
    }

    public TestArgumentExclusion excludeDisassemblerTestArguments(Argument... arguments) {
        return new TestArgumentExclusion(AssemblyTestComponent.DISASSEMBLER, this, Sets.from(arguments));
    }

    public TestArgumentExclusion excludeExternalTestArguments(Argument... arguments) {
        return new TestArgumentExclusion(AssemblyTestComponent.EXTERNAL_ASSEMBLER, this, Sets.from(arguments));
    }

    public TestArgumentExclusion excludeExternalTestArguments(Enumerator... argumentEnumerators) {
        final Set<Argument> arguments = new HashSet<Argument>();
        for (Enumerator argumentEnumerator : argumentEnumerators) {
            for (Object e : argumentEnumerator) {
                arguments.add((Argument) e);
            }
        }
        return new TestArgumentExclusion(AssemblyTestComponent.EXTERNAL_ASSEMBLER, this, arguments);
    }

    public ArgumentRange range(long minValue, long maxValue) {
        return new ArgumentRange(this, minValue, maxValue);
    }

    public ArgumentRange externalRange(long minValue, long maxValue) {
        final ArgumentRange argumentRange = new ArgumentRange(this, minValue, maxValue);
        argumentRange.doNotApplyInternally();
        return argumentRange;
    }
}
