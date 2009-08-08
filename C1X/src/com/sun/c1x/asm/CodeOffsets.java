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
package com.sun.c1x.asm;

import com.sun.c1x.util.*;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public class CodeOffsets {
    public enum Entries { Entry,
        VerifiedEntry,
        FrameComplete, // Offset in the code where the frame setup is (for forte stackwalks) is complete
        OSREntry,
        DtraceTrap,  // dtrace probes can never have an OSR entry so reuse it*/
        Exceptions,     // Offset where exception handler lives
        Deopt          // Offset where deopt handler lives
         }

    public void setValue(Entries osrentry, int offset) {
        Util.nonFatalUnimplemented();
    }
}
