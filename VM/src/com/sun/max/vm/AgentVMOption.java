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
package com.sun.max.vm;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;

/**
 * Support for the (repeatable) agent command line options.
 * Implemented using delegation to a list of {@link VMStringOption}
 * This could be genericized if necessary.
 *
 * @author Mick Jordan
 */

public class AgentVMOption extends VMOption {
    private List<VMStringOption> optionList = new ArrayList<VMStringOption>();

    @HOSTED_ONLY
    public AgentVMOption(String prefix, String help) {
        super(prefix, help);
    }

    /**
     * Return the number of instances of this option.
     * @return the number of instances of this option.
     */
    public int count() {
        return optionList.size();
    }

    /**
     * Returns the ith option value.
     * @param index into list of option values
     * @return the ith option value
     */
    public String getValue(int i) {
        return optionList.get(i).getValue();
    }

    @Override
    public boolean parseValue(Pointer optionValue) {
        VMStringOption option = new VMStringOption(prefix);
        optionList.add(option);
        return option.parseValue(optionValue);
    }

    @Override
    public void printHelp() {
        VMOptions.printHelpForOption(category(), prefix, ":<jarpath>[=<options>]", help);
    }

}
