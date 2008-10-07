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
/*VCSID=d36cc8de-de73-4bcc-8455-d1a8a934f616*/
package com.sun.max.ins.gui;

import com.sun.max.ins.*;
import com.sun.max.tele.object.*;


/**
 * An abstract base class for a dialog that enables the user to search for an object in the tele VM.
 * The dialog is composed of a list of names of objects and a text field that can be used to filter the list.
 *
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public abstract class TeleObjectSearchDialog extends FilteredListDialog<TeleObject> {

    @Override
    protected TeleObject noSelectedObject() {
        return null;
    }

    protected TeleObjectSearchDialog(Inspection inspection, String title, String filterFieldLabel, String actionName, boolean multiSelection) {
        super(inspection, title, filterFieldLabel, actionName, multiSelection);
    }

    protected TeleObjectSearchDialog(Inspection inspection, String title, String filterFieldLabel, boolean multiSelection) {
        this(inspection, title, filterFieldLabel, null, multiSelection);
    }

}
