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
package com.sun.max.tele;

import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;

/**
 * Interface for Tele layer objects that refer to general resources about the VM.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public interface TeleVMHolder {

    /**
     * @return the instance of {@link TeleVM} being managed by this code.
     */
    TeleVM teleVM();

    /**
     * Gets the manager for locating and managing code related information in the VM.
     * <br>
     * Thread-safe
     *
     * @return the singleton manager for information about code in the VM.
     */
    CodeManager codeManager();

    /**
     * Gets the factory for creating and managing VM breakpoints.
     * <br>
     * Thread-safe
     *
     * @return the singleton factory for creating and managing VM breakpoints
     */
    TeleBreakpointFactory breakpointFactory();

    /**
     * Gets the factory for creating and managing VM watchpoints; null
     * if watchpoints are not supported on this platform.
     * <br>
     * Thread-safe
     *
     * @return the singleton factory for creating and managing VM watchpoints, or
     * null if watchpoints not supported.
     */
    TeleWatchpoint.WatchpointManager watchpointManager();

}
