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
package com.sun.max.ins;

import java.awt.*;

import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.vm.*;


/**
 * A singleton inspector that displays {@link VMConfiguration}  information in the {@link TeleVM} boot image.
 *
 * @author Michael Van De Vanter
 */
public final class BootImageInspector extends Inspector  implements TableColumnViewPreferenceListener {

    // Set to null when inspector closed.
    private static BootImageInspector _bootImageInspector;

    /**
     * Displays the (singleton) BootImage inspector.
     * @return  The BootImage inspector, possibly newly created.
     */
    public static BootImageInspector make(Inspection inspection) {
        if (_bootImageInspector == null) {
            _bootImageInspector = new BootImageInspector(inspection);
        }
        return _bootImageInspector;
    }

    private final SaveSettingsListener _saveSettingsListener = createGeometrySettingsClient(this, "bootImageInspector");

    // This is a singleton viewer, so only use a single level of view preferences.
    private final BootImageViewPreferences _viewPreferences;

    private BootImageTable _table;

    private BootImageInspector(Inspection inspection) {
        super(inspection);
        Trace.begin(1, tracePrefix() + "initializing");
        _viewPreferences = BootImageViewPreferences.globalPreferences(inspection());
        _viewPreferences.addListener(this);
        createFrame(null);
        Trace.end(1, tracePrefix() + "initializing");
    }

    @Override
    protected Rectangle defaultFrameBounds() {
        return inspection().geometry().bootImageFrameDefaultBounds();
    }

    @Override
    protected void createView(long epoch) {
        _table = new BootImageTable(inspection(), _viewPreferences);
        frame().setContentPane(new InspectorScrollPane(inspection(), _table));
    }

    @Override
    protected SaveSettingsListener saveSettingsListener() {
        return _saveSettingsListener;
    }

    @Override
    public String getTextForTitle() {
        return "Boot Image: " + teleVM().bootImageFile().getAbsolutePath();
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.Dialog<BootImageColumnKind>(inspection(), "Boot Image View Options", _viewPreferences);
            }
        };
    }

    @Override
    protected void refreshView(long epoch, boolean force) {
        _table.refresh(epoch, force);
        super.refreshView(epoch, force);
    }

    public void viewConfigurationChanged(long epoch) {
        reconstructView();
    }

    public void tableColumnViewPreferencesChanged() {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        _bootImageInspector = null;
        _viewPreferences.removeListener(this);
        super.inspectorClosing();
    }

    @Override
    public void vmProcessTerminated() {
        Trace.line(1, tracePrefix() + " closing - process terminated");
        _bootImageInspector = null;
        _viewPreferences.removeListener(this);
        dispose();
    }

}
