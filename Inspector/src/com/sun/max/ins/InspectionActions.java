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

import java.awt.datatransfer.*;
import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.type.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.value.*;


// TODO (mlvdv) migrate actions from InspectionMenus;
// rework to make more uniform, self-contained, and self-refreshing.

/**
 * Provider of {@link InspectorAction}s that are of general use.
 * <p>
 * <b>How to create an {@link InspectorAction} to perform "doSomething":</b>
 *
 * <ol>
 *
 * <li><b>Create an action class:</b>
 * <ul>
 * <li> <code>public final class DoSometingAction extends {@link InspectorAction}</code></li>
 * <li> Add a title:  <code>private static final DEFAULT_NAME = "do something"</code>.  If the
 * action is interactive, for example if it produces a dialog, then the name should conclude with "...".
 * Capitalize the first word of the title but not the others, except for distinguished names such as
 * "Inspector" and acronyms.</li>
 * <li> For singletons, add a package scope constructor with one argument:  <code>String title</code></li>
 * <li> For non-singletons, package scope constructor contains additional arguments that
 * customize the action, for example that specify to what "something" is to be done.</li>
 * <li> In the constructor: <code>super(inspection(), title == null ? DEFAULT_TITLE : title);</code></li>
 * <li> If a singleton and if it contains state, for example enabled/disabled, that might change
 * depending on external circumstances, then register for general notification:
 * <code>_refreshableActions.append(this);</code> in the constructor.</li>
 * <li> Alternately, if state updates depend on a more specific kind of event, register
 * in the constructor explicitly for that event with a listener, for example
 * <code>focus().addListener(new InspectionFocusAdapter() { .... });</code>
 * The body of the listener should call <code>refresh</code>.</li>
 * <li>Override <code>protected void procedure()</code> with a method that does what
 * needs to be done.</li>
 * <li>If a singleton and if it contains state that might be changed depending on
 * external circumstances, override <code>public void refresh(long epoch, boolean force)</code>
 * with a method that updates the state.</li>
 * </ul></li>
 *
 *<li><b>Create a singleton variable if needed</b>:
 *<ul>
 * <li>If the command is a singleton, create an initialized variable, static if possible.</li>
 * <li><code>private static InspectorAction _doSomething = new DoSomethingAction(null);</code></li>
 * </ul></li>
 *
 * <li><b>Create an accessor:</b>
 * <ul>
 * <li>Singleton: <code>public InspectorAction doSomething()</code>.</li>
 * <li> Singleton accessor returns the singleton variable.</li>
 * <li>Non-singletons have additional arguments that customize the action, e.g. specifying to what "something"
 * is to be done; they also take a <code>String title</code> argument that permits customization of the
 * action's name, for example when it appears in menus.</li>
 * <li> Non-singletons return <code>new DoSomethignAction(args, String title)</code>.</li>
 * <li>Add a descriptive Javadoc comment:  "@return an Action that does something".</li>
 * </ul></li>
 *
 * </ol>
 * <p>
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 * @author Aritra Bandyopadhyay
 */
public class InspectionActions extends InspectionHolder implements Prober{

    private static final int TRACE_VALUE = 2;

    /**
     * Actions that are held and shared; they have state that will be refreshed.
     * This is particularly important for actions that enable/disable, depending on the inspection state.
     */
    private final AppendableSequence<InspectorAction> _refreshableActions = new ArrayListSequence<InspectorAction>();

    InspectionActions(Inspection inspection) {
        super(inspection);
        Trace.line(TRACE_VALUE, "InspectionActions initialized.");

    }

    public final void refresh(long epoch, boolean force) {
        for (Prober prober : _refreshableActions) {
            prober.refresh(epoch, force);
        }
    }

    public final void redisplay() {
        // non-op
    }


    /**
     * Action:  refreshes all data from the {@link TeleVM}.
     */
    private final class RefreshAllAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Refresh all views";

        RefreshAllAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            inspection().refreshAll(true);
        }
    }

    private final InspectorAction _refreshAll = new RefreshAllAction(null);

    /**
     * @return an Action that updates all displayed information read from the {@link TeleVM}.
     */
    public final InspectorAction refreshAll() {
        return _refreshAll;
    }


    /**
     * Action:  closes all open inspectors.
     */
    private final class CloseAllAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Close all views";

        CloseAllAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            inspection().desktopPane().removeAll();
            inspection().repaint();
        }
    }

    private final InspectorAction _closeAll = new CloseAllAction(null);

    /**
     * @return an Action that closes all open inspectors.
     */
    public final InspectorAction closeAll() {
        return _closeAll;
    }


    /**
     * Action:  quits inspector.
     */
    final class QuitAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Quit Inspector";

        QuitAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            inspection().quit();
        }
    }

    private final InspectorAction _quit = new QuitAction(null);

    public final InspectorAction quit() {
        return _quit;
    }


    /**
     * Action:  relocates the boot image, assuming that the inspector was invoked
     * with the option {@link MaxineInspector#suspendingBeforeRelocating()} set.
     */
    private final class RelocateBootImageAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Relocate Boot Image";

        RelocateBootImageAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected synchronized void procedure() {
            assert MaxineInspector.suspendingBeforeRelocating();
            try {
                teleVM().advanceToJavaEntryPoint();
            } catch (IOException ioException) {
                inspection().errorMessage("error during relocation of boot image");
            }
            setEnabled(false);
        }
    }

    private final InspectorAction _relocateBootImage = new RelocateBootImageAction(null);

    /**
     * @return an Action that relocates the boot image, assuming that the inspector was invoked
     * with the option {@link MaxineInspector#suspendingBeforeRelocating()} set.
     */
    public InspectorAction relocateBootImage() {
        return _relocateBootImage;
    }


    /**
     * Action:  sets level of trace output in inspector code.
     */
    private final class SetInspectorTraceLevelAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Set Inspector trace level...";

        SetInspectorTraceLevelAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            final int oldLevel = Trace.level();
            int newLevel = oldLevel;
            final String input = inspection().inputDialog(DEFAULT_TITLE, Integer.toString(oldLevel));
            try {
                newLevel = Integer.parseInt(input);
            } catch (NumberFormatException numberFormatException) {
                inspection().errorMessage(numberFormatException.toString());
            }
            if (newLevel != oldLevel) {
                Trace.on(newLevel);
            }
        }
    }

    private final InspectorAction _setInspectorTraceLevel = new SetInspectorTraceLevelAction(null);

    /**
     * @return an interactive Action that permits setting the level of inspector {@link Trace} output.
     */
    public final InspectorAction setInspectorTraceLevel() {
        return _setInspectorTraceLevel;
    }


    /**
     * Action:  changes the threshold determining when the Inspectors uses its
     * {@linkplain InspectorInterpeter interpreter} for access to {@link TeleVM} state.
     */
    private final class ChangeInterpreterUseLevelAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Change Interpreter use level...";

        ChangeInterpreterUseLevelAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            final int oldLevel = teleVM().interpreterUseLevel();
            int newLevel = oldLevel;
            final String input = inspection().inputDialog("Change interpreter use level (0=none, 1=some, etc)", Integer.toString(oldLevel));
            try {
                newLevel = Integer.parseInt(input);
            } catch (NumberFormatException numberFormatException) {
                inspection().errorMessage(numberFormatException.toString());
            }
            if (newLevel != oldLevel) {
                teleVM().setInterpreterUseLevel(newLevel);
            }
        }
    }

    private final InspectorAction _changeInterpreterUseLevel = new ChangeInterpreterUseLevelAction(null);

    /**
     * @return an interactive action that permits changing the level at which the {@linkplain InspectorInterpreter interpreter}
     * will be used.
     */
    public final InspectorAction changeInterpreterUseLevel() {
        return _changeInterpreterUseLevel;
    }


    /**
     * Action:  sets debugging level for transport.
     * Appears unused October '08 (mlvdv)
     */
    private final class SetTransportDebugLevelAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Set transport debug level...";

        SetTransportDebugLevelAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            final int oldLevel = teleProcess().transportDebugLevel();
            int newLevel = oldLevel;
            final String input = inspection().inputDialog(" (Set transport debug level, 0=none, 1=some, etc)", Integer.toString(oldLevel));
            try {
                newLevel = Integer.parseInt(input);
            } catch (NumberFormatException numberFormatException) {
                inspection().errorMessage(numberFormatException.toString());
            }
            if (newLevel != oldLevel) {
                teleProcess().setTransportDebugLevel(newLevel);
            }
        }
    }

    private final InspectorAction _setTransportDebugLevel = new SetTransportDebugLevelAction(null);

    /**
     * @return an interactive action that permits setting the debugging level for transport.
     */
    public final InspectorAction setTransportDebugLevel() {
        return _setTransportDebugLevel;
    }


    /**
     * Action: runs Inspector commands from a specified file.
     */
    private final class RunFileCommandsAction extends InspectorAction {

        RunFileCommandsAction() {
            super(inspection(), FileCommands.actionName());
        }

        @Override
        protected void procedure() {
            final String value = inspection().inputDialog("File name: ", FileCommands.defaultCommandFile());
            if (value != null && !value.equals("")) {
                FileCommands.executeCommandsFromFile(inspection(), value);
            }
        }
    }

    private final InspectorAction _runFileCommands = new RunFileCommandsAction();

    /**
     * @return an interactive Action that will run Inspector commands from a specified file.
     */
    public final InspectorAction runFileCommands() {
        return _runFileCommands;
    }


    /**
     * Action:  updates the {@linkplain TeleVM#updateLoadableTypeDescriptorsFromClasspath() types available} on
     * the {@link TeleVM}'s class path by rescanning the complete class path for types.
     */
    private final class UpdateClasspathTypesAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Rescan class path for types";

        UpdateClasspathTypesAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            teleVM().updateLoadableTypeDescriptorsFromClasspath();
        }
    }

    private final InspectorAction _updateClasspathTypes = new UpdateClasspathTypesAction(null);

    /**
     * @return an Action that updates the {@linkplain TeleVM#updateLoadableTypeDescriptorsFromClasspath() types available} on
     * the {@link TeleVM}'s class path by rescanning the complete class path for types.
     */
    public final InspectorAction updateClasspathTypes() {
        return _updateClasspathTypes;
    }


    /**
     * Action:  copies a hex string version of a {@link Word} to the system clipboard.
     */
    private final class CopyWordAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Copy word to clipboard";
        private final Word _word;

        private CopyWordAction(Word word, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            _word = word;
        }

        @Override
        public void procedure() {
            final Clipboard clipboard = inspection().getToolkit().getSystemClipboard();
            final StringSelection selection = new StringSelection(_word.toHexString());
            clipboard.setContents(selection, selection);
        }
    }

    /**
     * @param a {@link Word} from the {@link TeleVM}.
     * @param title a string to use as the title of the action, uses default name if null.
     * @return an Action that copies the word's text value in hex to the system clipboard
     */
    public final InspectorAction copyWord(Word word, String title) {
        return new CopyWordAction(word, title);
    }

    /**
     * @param a {@link Word} wrapped as a {@link Value} from the {@link TeleVM}.
     * @param title a string to use as the title of the action, uses default name if null.
     * @return an Action that copies the word's text value in hex to the system clipboard,
     * null if not a word.
     */
    public final InspectorAction copyValue(Value value, String title) {
        Word word = Word.zero();
        try {
            word = value.asWord();
        } catch (Throwable throwable) {
        }
        final InspectorAction action = new CopyWordAction(word, title);
        if (word.isZero()) {
            action.setEnabled(false);
        }
        return action;
    }


    /**
     * Action:  inspect a memory region, interactive if no location is specified.
     */
    private final class InspectMemoryAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect memory";
        private final Address _address;
        private final TeleObject _teleObject;

        InspectMemoryAction() {
            super(inspection(), "Inspect memory at address...");
            _address = null;
            _teleObject = null;
        }

        InspectMemoryAction(Address address, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            _address = address;
            _teleObject = null;
        }

        InspectMemoryAction(TeleObject teleObject, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            _address = null;
            _teleObject = teleObject;
        }

        @Override
        protected void procedure() {
            if (_teleObject != null) {
                MemoryInspector.create(inspection(), _teleObject);
            } else if (_address != null) {
                MemoryInspector.create(inspection(), _address);
            } else {
                new AddressInputDialog(inspection(), teleVM().bootImageStart(), "Inspect memory ataddress...", "Inspect") {

                    @Override
                    public void entered(Address address) {
                        MemoryInspector.create(inspection(), address);
                    }
                };
            }
        }
    }

    private final InspectorAction _inspectMemoryAction = new InspectMemoryAction();

    /**
     * @return an interactive Action that will create a Memory Inspector
     */
    public final InspectorAction inspectMemory() {
        return _inspectMemoryAction;
    }

    /**
     * @param teleObject surrogate for an object in the {@link TeleVM}
     * @param title a string name for the Action, uses default name if null
     * @return an Action that will create a Memory Inspector at the address
     */
    public final InspectorAction inspectMemory(TeleObject teleObject, String title) {
        return new InspectMemoryAction(teleObject, title);
    }

    /**
     * @param address a valid memory {@link Address} in the {@link TeleVM}
     * @param title a string name for the action, uses default name if null
     * @return an interactive Action that will create a Memory Inspector at the address
     */
    public final InspectorAction inspectMemory(Address address, String title) {
        return new InspectMemoryAction(address, title);
    }


    /**
     * Action: inspect memory starting at the beginning of the boot heap.
     */
    private final class InspectBootHeapMemoryAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect memory at boot heap start";

        InspectBootHeapMemoryAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            MemoryInspector.create(inspection(), teleVM().bootImageStart());
        }
    }

    private final InspectorAction _inspectBootHeapMemory = new InspectBootHeapMemoryAction(null);

    /**
     * @return an Action that will create a Memory Inspector at the start of the boot heap
     */
    public final InspectorAction inspectBootHeapMemory() {
        return _inspectBootHeapMemory;
    }

    /**
     * Action: inspect memory starting at the beginning of the boot code region.
     */
    private final class InspectBootCodeMemoryAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect memory at boot code start";

        InspectBootCodeMemoryAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            MemoryInspector.create(inspection(),  teleVM().teleCodeManager().teleBootCodeRegion().start());
        }
    }

    private final InspectorAction _inspectBootCodeMemory = new InspectBootCodeMemoryAction(null);

    /**
     * @return an Action that will create a Memory Inspector at the start of the boot code
     */
    public final InspectorAction inspectBootCodeMemory() {
        return _inspectBootCodeMemory;
    }


    /**
     * Action:   inspect a memory region as words, interactive if no location specified.
     */
    private final class InspectMemoryWordsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect memory words";
        private final Address _address;
        private final TeleObject _teleObject;

        InspectMemoryWordsAction() {
            super(inspection(), "Inspect memory words at address...");
            _address = null;
            _teleObject = null;
        }

        InspectMemoryWordsAction(Address address, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            _address = address;
            _teleObject = null;
        }

        InspectMemoryWordsAction(TeleObject teleObject, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            _address = null;
            _teleObject = teleObject;
        }

        @Override
        protected void procedure() {
            if (_teleObject != null) {
                MemoryWordInspector.create(inspection(), _teleObject);
            }
            if (_address != null) {
                MemoryWordInspector.create(inspection(), _address);
            } else {
                new AddressInputDialog(inspection(), teleVM().bootImageStart(), "Inspect memory words at address...", "Inspect") {

                    @Override
                    public void entered(Address address) {
                        MemoryWordInspector.create(inspection(), address);
                    }
                };
            }
        }
    }

    private final InspectorAction _inspectMemoryWordsAction = new InspectMemoryWordsAction();

    /**
     * @return an interactive Action that will create a MemoryWords Inspector
     */
    public final InspectorAction inspectMemoryWords() {
        return _inspectMemoryWordsAction;
    }

    /**
     * @param teleObject a surrogate for a valid object in the {@link TeleVM}
     * @param title a name for the action
     * @return an Action that will create a Memory Words Inspector at the address
     */
    public final InspectorAction inspectMemoryWords(TeleObject teleObject, String title) {
        return new InspectMemoryWordsAction(teleObject, title);
    }

    /**
     * @param address a valid memory {@link Address} in the {@link TeleVM}
     * @param title a name for the action
     * @return an Action that will create a Memory Words Inspector at the address
     */
    public final InspectorAction inspectMemoryWords(Address address, String title) {
        return new InspectMemoryWordsAction(address, title);
    }


    /**
     * Action: inspect memory starting at the beginning of the boot heap.
     */
    private final class InspectBootHeapMemoryWordsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect memory words at boot heap start";

        InspectBootHeapMemoryWordsAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            MemoryWordInspector.create(inspection(), teleVM().bootImageStart());
        }
    }

    private final InspectorAction _inspectBootHeapMemoryWords = new InspectBootHeapMemoryWordsAction(null);

    /**
     * @return an Action that will create a Memory Inspector at the start of the boot heap
     */
    public final InspectorAction inspectBootHeapMemoryWords() {
        return _inspectBootHeapMemoryWords;
    }


    /**
     * Action: inspect memory starting at the beginning of the boot code region.
     */
    private final class InspectBootCodeMemoryWordsAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect memory words at boot code start";

        InspectBootCodeMemoryWordsAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            MemoryWordInspector.create(inspection(), teleVM().teleCodeManager().teleBootCodeRegion().start());
        }
    }

    private final InspectorAction _inspectBootCodeMemoryWords = new InspectBootCodeMemoryWordsAction(null);

    /**
     * @return an Action that will create a Memory Inspector at the start of the boot code
     */
    public final InspectorAction inspectBootCodeMemoryWords() {
        return _inspectBootCodeMemoryWords;
    }

    /**
     * Action: create an Object Inspector, interactively specified by address..
     */
    private final class InspectObjectAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect object at address...";

        InspectObjectAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            new AddressInputDialog(inspection(), teleVM().teleHeapManager().teleBootHeapRegion().start(), "Inspect Object at Address...", "Inspect") {

                @Override
                public void entered(Address address) {
                    final Pointer pointer = address.asPointer();
                    if (teleVM().isValidOrigin(pointer)) {
                        final Reference objectReference = teleVM().originToReference(pointer);
                        final TeleObject teleObject = TeleObject.make(teleVM(), objectReference);
                        focus().setHeapObject(teleObject);
                    } else {
                        inspection().errorMessage("heap object not found at 0x"  + address.toHexString());
                    }
                }
            };
        }
    }

    private final InspectorAction _inspectObject = new InspectObjectAction(null);

    /**
     * @return an Action that will create an Object Inspector interactively, prompting the user for a numeric object ID
     */
    public final InspectorAction inspectObject() {
        return _inspectObject;
    }


    /**
     * Action:  creates an inspector for a specific heap object in the {@link TeleVM}.
     */
    private final class InspectSpecifiedObjectAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect object";
        final TeleObject _teleObject;

        InspectSpecifiedObjectAction(TeleObject teleObject, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            _teleObject = teleObject;
        }

        @Override
        protected void procedure() {
            focus().setHeapObject(_teleObject);
        }
    }

    /**
     * @param surrogate for a heap object in the {@link TeleVM}.
     * @param title a string name for the Action, uses default name if null
     * @return an Action that will create an Object Inspector
     */
    public final InspectorAction inspectObject(TeleObject teleObject, String title) {
        return new InspectSpecifiedObjectAction(teleObject, title);
    }


    /**
     * Action: create an Object Inspector, interactively specified by the inspector's OID.
     */
    private final class InspectObjectByIDAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect heap object by ID...";

        InspectObjectByIDAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            final String input = inspection().inputDialog("Inspect heap object by ID..", "");
            try {
                final long oid = Long.parseLong(input);
                final TeleObject teleObject = TeleObject.lookupObject(oid);
                if (teleObject != null) {
                    focus().setHeapObject(teleObject);
                } else {
                    inspection().errorMessage("failed to find heap object for ID: " + input);
                }
            } catch (NumberFormatException numberFormatException) {
                inspection().errorMessage("Not a ID: " + input);
            }
        }
    }

    private final InspectorAction _inspectObjectByID = new InspectObjectByIDAction(null);

    /**
     * @return an Action that will create an Object Inspector interactively, prompting the user for a numeric object ID
     */
    public final InspectorAction inspectObjectByID() {
        return _inspectObjectByID;
    }

    /**
     * Action:  inspect a {@link ClassActor} object for an interactively named class loaded in the {@link TeleVM},
     * specified by class name.
     */
    final class InspectClassActorByNameAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect ClassActor by name...";

        InspectClassActorByNameAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            final TeleClassActor teleClassActor = ClassActorSearchDialog.show(inspection(), "Inspect ClassActor ...", "Inspect");
            if (teleClassActor != null) {
                focus().setHeapObject(teleClassActor);
            }
        }
    }

    private final InspectorAction _inspectClassActorByName = new InspectClassActorByNameAction(null);

    /**
     * @return an interactive Action that inspects a {@link ClassActor} object for a class loaded in the {@link TeleVM},
     * specified by class name.
     */
    public final InspectorAction inspectClassActorByName() {
        return _inspectClassActorByName;
    }

    /**
     * Action:  inspect a {@link ClassActor} for an interactively named class loaded in the {@link TeleVM},
     * specified by the class ID in hex.
     */
    private final class InspectClassActorByHexIdAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect ClassActor by ID (Hex) ...";

        InspectClassActorByHexIdAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            final String value = inspection().questionMessage("ID (hex): ");
            if (value != null && !value.equals("")) {
                try {
                    final int serial = Integer.parseInt(value, 16);
                    final TeleClassActor teleClassActor = teleVM().teleClassRegistry().findTeleClassActorByID(serial);
                    if (teleClassActor == null) {
                        inspection().errorMessage("failed to find classActor for ID:  0x" + Integer.toHexString(serial));
                    } else {
                        focus().setHeapObject(teleClassActor);
                    }
                } catch (NumberFormatException ex) {
                    inspection().errorMessage("Hex integer required");
                }
            }
        }
    }

    private final InspectorAction _inspectClassActorByHexId = new InspectClassActorByHexIdAction(null);

    /**
     * @return an interactive Action that inspects a {@link ClassActor} object for a class loaded in the {@link TeleVM},
     * specified by class ID in hex.
     */
    public InspectorAction inspectClassActorByHexId() {
        return _inspectClassActorByHexId;
    }


    /**
     * Action:  inspect a {@link ClassActor} for an interactively named class loaded in the {@link TeleVM},
     * specified by the class ID in decimal.
     */
    private final class InspectClassActorByDecimalIdAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect ClassActor by ID (decimal) ...";
        InspectClassActorByDecimalIdAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            final String value = inspection().questionMessage("ID (decimal): ");
            if (value != null && !value.equals("")) {
                try {
                    final int serial = Integer.parseInt(value, 10);
                    final TeleClassActor teleClassActor = teleVM().teleClassRegistry().findTeleClassActorByID(serial);
                    if (teleClassActor == null) {
                        inspection().errorMessage("failed to find ClassActor for ID: " + serial);
                    } else {
                        focus().setHeapObject(teleClassActor);
                    }
                } catch (NumberFormatException ex) {
                    inspection().errorMessage("Hex integer required");
                }
            }
        }
    }
    private final InspectorAction _inspectClassActorByDecimalId = new InspectClassActorByDecimalIdAction(null);

    /**
     * @return an interactive Action that inspects a {@link ClassActor} object for a class loaded in the {@link TeleVM},
     * specified by class ID in decimal.
     */
    public InspectorAction inspectClassActorByDecimalId() {
        return _inspectClassActorByDecimalId;
    }


    /**
     * Action: create an Object Inspector for the boot {@link ClassRegistry} in the {@link TeleVM}.
     */
    private final class InspectBootClassRegistryAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Inspect boot class registry";

        InspectBootClassRegistryAction(String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
        }

        @Override
        protected void procedure() {
            final TeleObject teleBootClassRegistry = TeleObject.make(teleVM(), teleVM().bootClassRegistryReference());
            focus().setHeapObject(teleBootClassRegistry);
        }
    }

    private final InspectorAction _inspectBootClassRegistry = new InspectBootClassRegistryAction(null);

    /**
     * @return an action that will create an Object Inspector for the boot {@link ClassRegistry} in the {@link TeleVM}.
     */
    public final InspectorAction inspectBootClassRegistry() {
        return _inspectBootClassRegistry;
    }


    /**
     * Action: removes a specific  breakpoint in the {@link TeleVM}.
     */
    private final class RemoveBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Remove breakpoint";

        final TeleBreakpoint _teleBreakpoint;

        RemoveBreakpointAction(TeleBreakpoint teleBreakpoint, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            _teleBreakpoint = teleBreakpoint;
        }

        @Override
        protected void procedure() {
            if (focus().breakpoint() == _teleBreakpoint) {
                focus().setBreakpoint(null);
            }
            _teleBreakpoint.remove();
        }
    }

    /**
     * @param surrogate for a breakpoint in the {@link TeleVM}.
     * @param title a string name for the Action, uses default name if null.
     * @return an Action that will remove the breakpoint
     */
    public final InspectorAction removeBreakpoint(TeleBreakpoint teleBreakpoint, String title) {
        return new RemoveBreakpointAction(teleBreakpoint, title);
    }


    /**
     * Action: enables a specific  breakpoint in the {@link TeleVM}.
     */
    private final class EnableBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Enable breakpoint";
        final TeleBreakpoint _teleBreakpoint;

        EnableBreakpointAction(TeleBreakpoint teleBreakpoint, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            _teleBreakpoint = teleBreakpoint;
        }

        @Override
        protected void procedure() {
            if (focus().breakpoint() == _teleBreakpoint) {
                focus().setBreakpoint(null);
            }
            _teleBreakpoint.setEnabled(true);
            inspection().refreshAll(false);
        }
    }

    /**
     * @param surrogate for a breakpoint in the {@link TeleVM}.
     * @param title a string name for the Action, uses default name if null.
     * @return an Action that will enable the breakpoint
     */
    public final InspectorAction enableBreakpoint(TeleBreakpoint teleBreakpoint, String title) {
        return new EnableBreakpointAction(teleBreakpoint, title);
    }


    /**
     * Action: disables a specific  breakpoint in the {@link TeleVM}.
     */
    private final class DisableBreakpointAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "Disable breakpoint";

        final TeleBreakpoint _teleBreakpoint;

        DisableBreakpointAction(TeleBreakpoint teleBreakpoint, String title) {
            super(inspection(), title == null ? DEFAULT_TITLE : title);
            _teleBreakpoint = teleBreakpoint;
        }

        @Override
        protected void procedure() {
            if (focus().breakpoint() == _teleBreakpoint) {
                focus().setBreakpoint(null);
            }
            _teleBreakpoint.setEnabled(false);
            inspection().refreshAll(false);
        }
    }

    /**
     * @param surrogate for a breakpoint in the {@link TeleVM}.
     * @param title a string name for the Action, uses default name if null.
     * @return an Action that will disable the breakpoint
     */
    public final InspectorAction disableBreakpoint(TeleBreakpoint teleBreakpoint, String title) {
        return new DisableBreakpointAction(teleBreakpoint, title);
    }


    /**
     * Action:  toggle on/off a breakpoint at the target code location of the current focus.
     */
    final class ToggleTargetCodeBreakpointAction extends InspectorAction {

        private long _epoch = -1;

        ToggleTargetCodeBreakpointAction() {
            super(inspection(), "Toggle target code breakpoint");
            _refreshableActions.append(this);
            focus().addListener(new InspectionFocusAdapter() {
                @Override
                public void codeLocationFocusSet(TeleCodeLocation codeLocation, boolean interactiveForNative) {
                    refresh(_epoch, false);
                }
            });
        }

        @Override
        protected void procedure() {
            final Address targetCodeInstructionAddress = focus().codeLocation().targetCodeInstructionAddresss();
            if (!targetCodeInstructionAddress.isZero()) {
                TeleTargetBreakpoint breakpoint = teleProcess().targetBreakpointFactory().getNonTransientBreakpointAt(targetCodeInstructionAddress);
                if (breakpoint == null) {
                    breakpoint = teleProcess().targetBreakpointFactory().makeBreakpoint(targetCodeInstructionAddress, false);
                    focus().setBreakpoint(breakpoint);
                } else {
                    teleProcess().targetBreakpointFactory().removeBreakpointAt(targetCodeInstructionAddress);
                    focus().setBreakpoint(null);
                }
            }
        }

        @Override
        public void refresh(long epoch, boolean force) {
            setEnabled(inspection().hasProcess()  && focus().codeLocation().hasTargetCodeLocation());
            _epoch = epoch;
        }
    }

    private InspectorAction _toggleTargetCodeBreakpoint = new ToggleTargetCodeBreakpointAction();

    /**
     * @return an Action that will toggle on/off a breakpoint at the target code location of the current focus.
     */
    public final InspectorAction toggleTargetCodeBreakpoint() {
        return _toggleTargetCodeBreakpoint;
    }

}
