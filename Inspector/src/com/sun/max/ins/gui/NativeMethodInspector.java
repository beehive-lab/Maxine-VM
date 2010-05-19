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
package com.sun.max.ins.gui;

import com.sun.max.ins.*;
import com.sun.max.ins.method.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;

/**
 * Visual inspector and debugger for code discovered in the VM that is not compiled Java.
 * It is runtime assembled code such as a {@linkplain RuntimeStub stub} or
 * is other native code about which little is known.
 *
 * @author Michael Van De Vanter
 * @author Doug Simon
 */
public final class NativeMethodInspector extends MethodInspector {

    private final MaxExternalCode externalCode;
    private TargetCodeViewer targetCodeViewer = null;
    private final String shortName;
    private final String longName;

    public NativeMethodInspector(Inspection inspection, MethodInspectorContainer parent, MaxExternalCode externalCode) {
        super(inspection, parent);
        this.externalCode = externalCode;
        shortName = inspection().nameDisplay().shortName(externalCode);
        longName = inspection().nameDisplay().longName(externalCode);
        createTabFrame(parent);
    }

    @Override
    public MaxExternalCode machineCode() {
        return externalCode;
    }

    @Override
    public TeleClassMethodActor teleClassMethodActor() {
        return null;
    }

    @Override
    public String getTextForTitle() {
        return shortName;
    }

    @Override
    public String getToolTip() {
        return longName;
    }

    @Override
    public void createView() {
        targetCodeViewer =  new JTableTargetCodeViewer(inspection(), this, externalCode);
        getContentPane().add(targetCodeViewer);
        pack();
    }

    @Override
    protected void refreshView(boolean force) {
        if (getJComponent().isShowing() || force) {
            targetCodeViewer.refresh(force);
        }
    }

    public void viewConfigurationChanged() {
        targetCodeViewer.redisplay();
    }

    @Override
    public void print() {
        targetCodeViewer.print(getTextForTitle());
    }

    /**
     * Receive request from codeViewer to close; there's only one, so close the whole MethodInspector.
     */
    @Override
    public void closeCodeViewer(CodeViewer codeViewer) {
        assert codeViewer == targetCodeViewer;
        close();
    }

    /**
     * Global code selection has changed; update viewer.
     */
    @Override
    public void codeLocationFocusSet(MaxCodeLocation codeLocation, boolean interactiveForNative) {
        if (targetCodeViewer.updateCodeFocus(codeLocation) && !isSelected()) {
            highlight();
        }
    }
}
