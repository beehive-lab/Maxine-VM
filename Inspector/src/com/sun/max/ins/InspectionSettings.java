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
import java.awt.event.*;
import java.io.*;
import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.*;

/**
 * Manages saving and restoring of settings between Inspection sessions.
 *
 * @author Michael Van De Vanter
 * @author Doug Simon
 */
public class InspectionSettings {

    private static final int TRACE_VALUE = 1;

    private String tracePrefix() {
        return "[Inspector] ";
    }

    /**
     * @return a unique key uniquely identifying a key belonging to a client that stores and retrieves settings.
     */
    private String makeClientKey(SaveSettingsListener saveSettingsListener, String key) {
        return saveSettingsListener.name() + "." + key;
    }

    /**
     * An event requesting that a {@link SaveSettingsListener} write its settings to a persistent store. The
     * event object includes a method for a listener to {@linkplain #save(String, Object) save} its settings.
     */
    public final class SaveSettingsEvent {
        private final Properties _props;
        private final SaveSettingsListener _client;

        /**
         * Creates an object that will populate a given properties object with the settings of a given client.
         *
         * @param saveSettingsListener a settings listener that will save it settings via this object
         * @param properties the properties object into which the settings will be written
         */
        private SaveSettingsEvent(SaveSettingsListener saveSettingsListener, Properties properties) {
            _props = properties;
            _client = saveSettingsListener;
        }

        /**
         * Saves a value for a setting corresponding to a given name.
         *
         * @param key the name of the setting
         * @param value the value of the setting. The value saved is the result of calling {@link String#valueOf(Object)} on this value.
         */
        public void save(String key, Object value) {
            final String clientKey = makeClientKey(_client, key);
            _props.setProperty(clientKey, String.valueOf(value));
        }
    }

    /**
     * An object that uses settings to configure itself and therefore wants to participate in the saving of settings to
     * a persistent store.
     */
    public static interface SaveSettingsListener {
        /**
         * Notifies this object that it should save it settings.
         *
         * @param saveSettingsEvent an object to which this client should write its persistent settings
         */
        void saveSettings(SaveSettingsEvent saveSettingsEvent);

        /**
         * Gets a name for this client that is unique amongst all other clients. This name forms the
         * prefix of the key under which any persistent setting of this client is saved.
         */
        String name();

        /**
         * The component responsible for the displaying this object in a GUI. If this object does not have a visual
         * presentation, then this method should return null.
         *
         * In addition to the other listener specific settings, the {@linkplain Component#getBounds() geometry} of the
         * {@linkplain #component() component} responsible for the displaying the object are automatically saved
         * whenever the inspection settings are {@linkplain InspectionSettings#save() persisted}. The inspection
         * settings are persisted any time the component is
         * {@linkplain ComponentListener#componentMoved(ComponentEvent) moved} or
         * {@linkplain ComponentListener#componentResized(ComponentEvent) resized} and whenever the component is made
         * {@linkplain ComponentListener#componentShown(ComponentEvent) visible}, its bounds are updated from the
         * settings.
         */
        Component component();
    }

    /**
     * A convenience class that simplifies implementing the {@link SaveSettingsListener} interface.
     */
    public abstract static class AbstractSaveSettingsListener implements SaveSettingsListener {
        protected final String _name;
        protected final Component _component;

        protected AbstractSaveSettingsListener(String name, Component component) {
            _name = name;
            _component = component;
        }

        public Component component() {
            return _component;
        }

        public String name() {
            return _name;
        }
        @Override
        public String toString() {
            return name();
        }
    }

    private final SaveSettingsListener _bootimageClient;
    private final Properties _properties = new SortedProperties();
    private final File _settingsFile;
    private final boolean _bootImageChanged;
    private final Map<String, SaveSettingsListener> _clients;

    public InspectionSettings(Inspection inspection, File settingsFile) {
        _settingsFile = settingsFile;
        _clients = new IdentityHashMap<String, SaveSettingsListener>();
        try {
            final FileReader fileReader = new FileReader(_settingsFile);
            Trace.begin(1, tracePrefix() + "loading preferences from: " + _settingsFile.toString());
            _properties.load(fileReader);
            Trace.end(1, tracePrefix() + "loading preferences from: " + _settingsFile.toString());
            fileReader.close();
        } catch (FileNotFoundException ioException) {
        } catch (IOException ioException) {
            ProgramWarning.message(tracePrefix() + "Error while loading settings from " + _settingsFile + ": " + ioException.getMessage());
        }

        final TeleVM teleVM = inspection.teleVM();
        _bootimageClient = new AbstractSaveSettingsListener("bootimage", null) {
            public void saveSettings(SaveSettingsEvent settings) {
                settings.save("version", String.valueOf(teleVM.bootImage().header()._version));
                settings.save("id", String.valueOf(teleVM.bootImage().header()._randomID));
            }
        };

        addSaveSettingsListener(_bootimageClient);
        final int version = get(_bootimageClient, "version", OptionTypes.INT_TYPE, 0);
        final int randomID = get(_bootimageClient, "id", OptionTypes.INT_TYPE, 0);
        _bootImageChanged = version != teleVM.bootImage().header()._version || randomID != teleVM.bootImage().header()._randomID;
        _bootimageClient.saveSettings(new SaveSettingsEvent(_bootimageClient, _properties));
        _saver = new Saver();
    }

    public void addSaveSettingsListener(final SaveSettingsListener saveSettingsListener) {
        final SaveSettingsListener oldClient = _clients.put(saveSettingsListener.name(), saveSettingsListener);
        assert oldClient == null || oldClient == saveSettingsListener;

        final Component component = saveSettingsListener.component();
        if (component != null) {
            component.addComponentListener(new ComponentListener() {
                public void componentHidden(ComponentEvent e) {
                }
                public void componentMoved(ComponentEvent e) {
                    save();
                }
                public void componentResized(ComponentEvent e) {
                    save();
                }
                public void componentShown(ComponentEvent e) {
                    refreshComponentClient(saveSettingsListener);
                }
            });
            refreshComponentClient(saveSettingsListener);
        }
    }

    public void removeSaveSettingsListener(final SaveSettingsListener saveSettingsListener) {
        _clients.remove(saveSettingsListener.name());
    }

    public void refreshComponentClient(SaveSettingsListener saveSettingsListener) {
        final Component component = saveSettingsListener.component();
        if (component != null) {
            final Rectangle oldBounds = component.getBounds();
            final Rectangle newBounds = new Rectangle(
                            get(saveSettingsListener, "x", OptionTypes.INT_TYPE, oldBounds.x),
                            get(saveSettingsListener, "y", OptionTypes.INT_TYPE, oldBounds.y),
                            get(saveSettingsListener, "width", OptionTypes.INT_TYPE, oldBounds.width),
                            get(saveSettingsListener, "height", OptionTypes.INT_TYPE, oldBounds.height));
            if (!newBounds.equals(oldBounds)) {
                component.setBounds(newBounds);
            }
        }
    }

    /**
     * Determines if the boot image identified in the file from which these settings were loaded
     * is different from the boot image of the current {@link TeleVM}.
     */
    public boolean bootImageChanged() {
        return _bootImageChanged;
    }

    /**
     * Gets the value for a setting corresponding to a given name.
     *
     * @param key the name of the setting for which the value is required
     * @param type an object that encodes the (boxed) type of the value as well as the capability for parsing a string
     *            into a value of the type
     * @param defaultValue the value to return if there is not value in this object corresponding to {@code key}
     * @throws Option.Error if there is an error parsing the string representation of the value into a value of the
     *             expected type
     */
    public <Value_Type> Value_Type get(SaveSettingsListener saveSettingsListener, String key, Option.Type<Value_Type> type, Value_Type defaultValue) {
        if (!_clients.containsKey(saveSettingsListener.name())) {
            throw new IllegalArgumentException("Unregistered settings client: " + saveSettingsListener.name());
        }
        final String clientKey = makeClientKey(saveSettingsListener, key);
        final String value = _properties.getProperty(clientKey);
        if (value == null) {
            return defaultValue;
        }
        try {
            return type.parseValue(value);
        } catch (Option.Error optionError) {
            throw new Option.Error(String.format("Problem occurred while parsing %s setting:%n%s", clientKey, optionError.getMessage()));
        }
    }

    private boolean _needsSaving;

    /**
     * A helper task that is run in a separate thread for writing the persistent settings to a file.
     * Performing this task asynchronously means that a number of changes can be batched to the
     * file.
     */
    private class Saver implements Runnable {
        @Override
        public void run() {
            while (!_done) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                if (!_done && _needsSaving) {
                    doSave();
                    _needsSaving = false;
                }
            }
        }

        boolean _done;
        void quit() {
            _done = true;
            doSave();
        }

        Saver() {
            new Thread(this, "SettingsSaver").start();
        }
    }

    private final Saver _saver;

    /**
     * Determines if there is a setting in this object named by a given key.
     */
    public boolean containsKey(SaveSettingsListener saveSettingsListener, String key) {
        final String clientKey = makeClientKey(saveSettingsListener, key);
        return _properties.containsKey(clientKey);
    }

    public synchronized void quit() {
        _saver.quit();
    }

    /**
     * Indicates that the persistent settings represented by this object have changed
     * and should be saved to a {@linkplain #settingsFile() file}. The actual writing
     * to the file happens asynchronously.
     */
    public void save() {
        _needsSaving = true;
    }

    /**
     * Writes the persistent settings represented by this object to a {@linkplain #settingsFile() file}.
     */
    private synchronized void doSave() {
        final Properties newProperties = new SortedProperties();
        Trace.line(TRACE_VALUE, tracePrefix() + "saving settings");
        for (SaveSettingsListener saveSettingsListener : _clients.values()) {
            final SaveSettingsEvent saveSettingsEvent = new SaveSettingsEvent(saveSettingsListener, newProperties);
            saveSettingsListener.saveSettings(saveSettingsEvent);
            final Component component = saveSettingsListener.component();
            if (component != null) {
                final Rectangle bounds = component.getBounds();
                saveSettingsEvent.save("x", bounds.x);
                saveSettingsEvent.save("y", bounds.y);
                saveSettingsEvent.save("width", bounds.width);
                saveSettingsEvent.save("height", bounds.height);
            }
        }
        try {
            final FileWriter fileWriter = new FileWriter(_settingsFile);
            newProperties.store(fileWriter, null);
            fileWriter.close();
        } catch (IOException ioException) {
            ProgramWarning.message(tracePrefix() + "Error while saving settings to " + _settingsFile + ": " + ioException.getMessage());
        }
        _properties.clear();
        _properties.putAll(newProperties);
    }
}
