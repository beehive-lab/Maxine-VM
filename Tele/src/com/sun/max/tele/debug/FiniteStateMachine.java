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
/*VCSID=650ceda4-5e91-4149-a9b3-0097ec4fb77c*/
package com.sun.max.tele.debug;

import java.util.*;
/**
 * A generic finite state machine to represent the state dependent behavior of
 * TeleProcess and the TeleProcess controller. It has a listener that it
 * notifies about any state change. The {@link TeleProcess} or the {@link TeleProcessController}
 * calls the transition to change the state of the machine.
 *
 * @author Aritra Bandyopadhyay
 *
 */

public class FiniteStateMachine<T> {

    private final T[] _states;
    private T _currentState;

    public interface Listener<T> {
        void handleStateTransition(T oldState, T newState);
    }

    private List<FiniteStateMachine.Listener<T>> _listeners = new ArrayList<FiniteStateMachine.Listener<T>>();

    protected FiniteStateMachine(T[] states, T initialState, List<FiniteStateMachine.Listener<T>> listeners) {
        _states = states;
        _currentState = initialState;
        _listeners = listeners;
    }

    protected FiniteStateMachine(T[] states, T initialState) {
        _states = states;
        _currentState = initialState;
    }

    protected void addStateListener(FiniteStateMachine.Listener<T> listener) {
        _listeners.add(listener);
    }

    protected void removeStateListener(FiniteStateMachine.Listener<T> listener) {
        if (_listeners.contains(listener)) {
            _listeners.remove(listener);
        }
    }

    protected synchronized void transition(final T finalState) {
        final T oldState = _currentState;
        _currentState  = finalState;
        for (final Listener<T> listener : _listeners) {
            listener.handleStateTransition(oldState, finalState);
        }
    }

    /**
     * Perform transition without notifying the state listeners.
     *
     * @param finalState
     */
    protected synchronized void transitionSilently(final T finalState) {
        _currentState = finalState;
    }

    public synchronized T getCurrentState() {
        return _currentState;
    }

    public T[] getStates() {
        return _states;
    }

}
