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
/*VCSID=84213ebe-43d1-4aa7-b32c-e52f76baa9ad*/
package com.sun.max.ins;


/**
 * A display element of an {@link Inspector} that presents visually some aspect of the state of the {@link TeleVM}.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public interface Prober {

    /**
     * @return the inspection session of which this {@link Prober} is a part.
     */
    Inspection inspection();

    /**
     * Brings prober/view up to date with the state of the tele VM.
     *
     * @param epoch current execution age of the tele process.
     */
    void refresh(long epoch);

    /**
     * Revise the display to account for any changes in view configuration or style information; does not imply a state change in the {@link TeleVM}.
     */
    void redisplay();

}
