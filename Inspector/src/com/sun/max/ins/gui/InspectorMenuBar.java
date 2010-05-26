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

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.Inspector.*;
import com.sun.max.tele.*;

/**
 * A menu bar specialized for use in the VM Inspector.
 * <br>
 * Instances of {@link InspectorMenu} can be added, and they can be retrieved by name.
 *
 * @author Michael Van De Vanter
 */
public class InspectorMenuBar extends JMenuBar implements Prober, InspectionHolder {

    private static final ImageIcon FRAME_ICON = InspectorImageIcon.createDownTriangle(12, 14);

    private final Inspection inspection;
    private final String tracePrefix;

    private final AppendableSequence<InspectorMenu> menus = new ArrayListSequence<InspectorMenu>(10);

    /**
     * Creates a new {@JMenuBar}, specialized for use in the VM Inspector.
     */
    protected InspectorMenuBar(Inspection inspection) {
        this.inspection = inspection;
        this.tracePrefix = "[" + getClass().getSimpleName() + "] ";
        setOpaque(true);
    }

    public void add(InspectorMenu inspectorMenu) {
        assert inspectorMenu.getMenuName() != null;
        super.add(inspectorMenu);
        menus.append(inspectorMenu);
    }

    private InspectorMenu findMenu(String name) {
        for (InspectorMenu inspectorMenu : menus) {
            if (inspectorMenu.getMenuName().equals(name)) {
                return inspectorMenu;
            }
        }
        return null;
    }

    /**
     * @param name a menu name
     * @return the menu in the menu bar with that name, or
     * a new empty one if it doesn't already exist.
     */
    public InspectorMenu makeMenu(MenuKind menuKind) {
        InspectorMenu menu = findMenu(menuKind.label());
        if (menu != null) {
            return menu;
        }
        menu = new InspectorMenu(menuKind.label());
        if (menuKind == MenuKind.DEFAULT_MENU) {
            menu.setIcon(FRAME_ICON);
        }
        add(menu);
        return menu;
    }

    public final Inspection inspection() {
        return inspection;
    }

    public final MaxVM vm() {
        return inspection.vm();
    }

    public final InspectorGUI gui() {
        return inspection.gui();
    }

    public final InspectorStyle style() {
        return inspection.style();
    }

    public final InspectionFocus focus() {
        return inspection.focus();
    }

    public final InspectionActions actions() {
        return inspection.actions();
    }

    public void redisplay() {
    }

    public void refresh(boolean force) {
        for (InspectorMenu menu : menus) {
            menu.refresh(force);
        }
    }

    /**
     * @return default prefix text for trace messages; identifies the class being traced.
     */
    protected String tracePrefix() {
        return tracePrefix;
    }
}
