/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.lir;

/**
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 * Port of the backend (LIR) of the client compiler to Java.
 *
 * List of ported files:
 *
 * c1_LIR.hpp
 * LIR_Op -> LIRInstruction (Status: Ported)
 * LIR_Op0 -> LIROp0 (Status: Ported)
 * LIR_OpLabel -> LIRLabel (Status: Ported)
 * LIR_Op1 -> LIROp1 (Status: Ported)
 * LIR_OpBranch -> LIRBranch (Status: Ported)
 * LIR_OpConvert -> LIRConvert (Status: Ported)
 * LIR_OPAllocObj -> LIRAllocObj (Status : Ported)
 * LIR_OpRoundFP -> LIRRoundFP (Status : Ported)
 * LIR_Op2 -> LIROp2 (Status : Ported)
 * LIR_OpDelay -> LIRDelay (Status : Ported)
 * LIR_Op3 -> LIROp3 (Status : Ported)
 * LIR_OpAllocArray -> LIRAllocArray (Status : Ported)
 * LIR_OpCall -> LIRCall (Status : Ported)
 * LIR_OpJavaCall -> LIRJavaCall (Status : Ported)
 * LIR_OpRTCall -> LIRRTCall (Status : Ported)
 * LIR_OpArrayCopy -> LIRArrayCopy (Status : Ported)
 * LIR_OPLock -> LIRLock (Status : Ported)
 * LIR_OpTypeCheck -> LIRTypeCheck (Status : Ported)
 * LIR_OpCompareAndSwap -> LIRCompareAndSwap (Status : Ported)
 * LIR_OpProfileCall -> LIRProfileCall (Status : Ported)
 * LIR_List -> LIRList (Status: Identified)
 *
 */
