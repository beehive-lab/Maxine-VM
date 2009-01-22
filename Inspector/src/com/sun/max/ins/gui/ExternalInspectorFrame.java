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

import java.awt.*;
import java.lang.reflect.*;

import javax.swing.*;
import javax.swing.plaf.metal.*;

import com.sun.max.ins.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

public final class ExternalInspectorFrame extends JFrame implements InspectorFrame {

    private final Inspector _inspector;

    public Inspector inspector() {
        return _inspector;
    }

    private final InspectorMenu _defaultInspectorFrameMenu;

    public synchronized void add(InspectorMenuItems inspectorMenuItems) {
        _defaultInspectorFrameMenu.add(inspectorMenuItems);
    }

    public InspectorMenu menu() {
        return _defaultInspectorFrameMenu;
    }

    public void setMenu(InspectorMenu menu) {
        Problem.unimplemented("setMenu() not supported for External Frames");
    }

    public Inspection inspection() {
        return _inspector.inspection();
    }

    public synchronized void refresh(long epoch, boolean force) {
        menu().refresh(epoch, force);
    }

    public void redisplay() {
    }

    public Container asContainer() {
        return this;
    }

    private static final Method MetalRootPaneUI_getTitlePane = Classes.getDeclaredMethod(MetalRootPaneUI.class, "getTitlePane");
    private static final Field MetalTitlePane_menuBar = Classes.getDeclaredField(Classes.forName("javax.swing.plaf.metal.MetalTitlePane"), "menuBar");
    static {
        MetalRootPaneUI_getTitlePane.setAccessible(true);
        MetalTitlePane_menuBar.setAccessible(true);
    }

    protected ExternalInspectorFrame(Inspector inspector) {
        _inspector = inspector;
        setResizable(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // This is a very Metal LAF specific hack to replace the items in the title bar system menu of a JFrame.
        final JMenu systemMenu;
        try {
            final JComponent titlePane = (JComponent) MetalRootPaneUI_getTitlePane.invoke(getRootPane().getUI());
            final JMenuBar systemMenuBar = (JMenuBar) MetalTitlePane_menuBar.get(titlePane);
            systemMenu = systemMenuBar.getMenu(0);
            systemMenu.removeAll();
        } catch (Exception exception) {
            throw ProgramError.unexpected(exception);
        }

        _defaultInspectorFrameMenu = new InspectorMenu(inspector, null, systemMenu);
        setIconImage(FRAME_ICON.getImage());
    }

    public void moveToFront() {
        toFront();
    }

    public void moveToMiddle() {
    }

    public void setLocationOnScreen(Point location) {
        final Dimension screenSize = getToolkit().getScreenSize();
        if (getWidth() > screenSize.width) {
            setSize(screenSize.width, getHeight());
        }
        if (getHeight() > screenSize.height) {
            setSize(getWidth(), screenSize.height);
        }
        setLocation(location);
    }

    public boolean isSelected() {
        Problem.unimplemented();
        return false;
    }

    public void setSelected() {
        Problem.unimplemented();
    }

    public void flash(Color borderFlashColor) {
        InspectorFrame.Static.flash(this, borderFlashColor);
    }

    public void replaceFrameCloseAction(InspectorAction action) {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        Problem.unimplemented("replaceFrameCloseAction unimplemented for external frames");
    }

    @Override
    public void dispose() {
        super.dispose();
        _inspector.inspectorClosing();
    }

}
