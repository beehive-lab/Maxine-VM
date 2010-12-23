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

import com.sun.max.ins.InspectionSettings.AbstractSaveSettingsListener;
import com.sun.max.ins.InspectionSettings.SaveSettingsEvent;
import com.sun.max.ins.util.*;
import com.sun.max.program.option.*;

/**
 * Singleton manager for the Inspector's persistent notes:  a place
 * where the user can write arbitrary textual data that persists across session.
 * <br>
 * There is only one notepad at present, but this could be generalized to more.
 *
 * @author Michael Van De Vanter
 */
public final class NotepadManager extends AbstractInspectionHolder {

    // TODO (mlvdv)  Generalize to support multiple notepads, stored by name.
    // some of the machinery to support more is already in place.

    // Key used for making data persistent
    private static final String PREFIX_KEY = "notepad";
    private static final String DEFAULT_NOTEPAD_KEY = "default";
    private static final String EMPTY_CONTENTS = "<empty>";

    /**
     * A simple notepad implementation that holds text and can
     * save its contents with other persistent inspection settings.
     *
     * @author Michael Van De Vanter
     */
    private final class NotepadImpl extends AbstractSaveSettingsListener implements InspectorNotepad {
        private final InspectionSettings inspectionSettings;
        private final NotepadManager notepadManager;
        private final String key;

        // Contents are never null until disposed, at which point the notepad is useless.
        private String contents = EMPTY_CONTENTS;

        public NotepadImpl(InspectionSettings inspectionSettings, NotepadManager notepadManager, String key) {
            super(PREFIX_KEY);
            this.inspectionSettings = inspectionSettings;
            this.notepadManager = notepadManager;
            this.key = key;
            // Register with the persistence service; must do this before load.
            inspectionSettings.addSaveSettingsListener(this);
            // Load contents of this notepad, if exists, from persistent settings
            contents = inspectionSettings.get(this, key, OptionTypes.STRING_TYPE, EMPTY_CONTENTS);
        }

        public String getName() {
            return key;
        }

        public String getContents() {
            return contents;
        }

        public void setContents(String contents) throws IllegalArgumentException {
            if (contents == null) {
                throw new IllegalArgumentException();
            }
            this.contents = contents;
            inspectionSettings.save();
        }

        public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
            // Should not be receiving save events if disposed.
            assert contents != null;
            saveSettingsEvent.save(key, contents);
        }

        public void dispose() {
            assert contents != null;
            inspectionSettings.removeSaveSettingsListener(this);
            this.contents = null;
            notepadManager.dispose(this);
        }
    }

    // Only a single notepad supported at present
    private final NotepadImpl defaultNotepad;

    public NotepadManager(Inspection inspection) {
        super(inspection);
        // Create the default notepad, a singleton for now, but there could be more.
        this.defaultNotepad = new NotepadImpl(inspection.settings(), this, DEFAULT_NOTEPAD_KEY);
    }

    /**
     * @return the default notepad, a place where unstructured, persistent text can be stored.
     */
    public InspectorNotepad getNotepad() {
        return defaultNotepad;
    }

    /**
     * Notifies this manager that a notepad has been disposed and should be forgotten.
     *
     * @param notepad the notepad being disposed.
     */
    private void dispose(InspectorNotepad notepad) {
        InspectorError.unimplemented("notepad disposal not yet supported");
        // Remove the associated entry from the current inspection settings
    }

}
