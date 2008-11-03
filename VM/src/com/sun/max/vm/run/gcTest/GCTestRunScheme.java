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
/*
 * Copyright (c) 2007 Sun Microsystems, Inc. All rights reserved. Use is subject to license terms.
 */
package com.sun.max.vm.run.gcTest;

import com.sun.max.collect.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.run.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Sunil Soman
 */
public class GCTestRunScheme  extends AbstractVMScheme implements RunScheme {

    public GCTestRunScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    private static final Object _token = new Object();
    /**
     * The run method for the main Java thread.
     */
    @Override
    public void run() {
        VMConfiguration.hostOrTarget().initializeSchemes(MaxineVM.Phase.PRISTINE);

        Debug.println("initialized schemes pristine");

        Trap.initialize();
        MaxineVM.hostOrTarget().setPhase(MaxineVM.Phase.STARTING);

        // Now we can decode all the other VM arguments using the full language
        VMOptions.parseStarting();

        VMConfiguration.hostOrTarget().initializeSchemes(MaxineVM.Phase.STARTING);
        Debug.println("initialized schemes starting");

        final HelloWorldThread[] thread = new HelloWorldThread[1];

        for (int i = 0; i < thread.length; ++i) {
            thread[i] = new HelloWorldThread(i);
        }

        for (int i = 0; i < thread.length; ++i) {
            thread[i].start();
        }
        synchronized (_token) {
            try {
                _token.wait();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        Debug.println("Running gctest");
        final Object[] o = new Object[50000];

        Debug.print(Reference.fromJava(o).toOrigin().asAddress());

        final Object object = new Object();




        for (int i = 0; i < thread.length; ++i) {
            thread[i].setObject(object);
        }


        synchronized (_token) {
            try {
                _token.wait();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    static class HelloWorldThread extends Thread {
        boolean _done;
        int _id;
        Object _object = null;
        public HelloWorldThread(int id) {
            super("HelloWordThread-" + id);
            _id = id;
        }

        public void setObject(Object obj) {
            _object = obj;
        }

        @Override
        public void run() {
            if (_id == 0) {
                synchronized (_token) {
                    _token.notify();
                }
            }
            int counter = 0;
            for (long i = 0; i < 10000; ++i) {
                ++counter;
                if (counter == 1000) {
                    try {
                        sleep(100);
                        counter = 0;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            Debug.println("done executing loop");
            if (_id == 0) {
                synchronized (_token) {
                    _token.notify();
                }
            }
            Debug.println(this.getName() + " done");
            _done = true;
        }
    }


    @Override
    public IterableWithLength<MethodActor> gatherNativeInitializationMethods() {
        return Iterables.empty();
    }

    @Override
    public void runNativeInitializationMethods() {
    }

    @Override
    public void finalize(Phase phase) {
    }

    @Override
    public void initialize(Phase phase) {
    }

    @Override
    public VMConfiguration vmConfiguration() {
        return null;
    }

    @Override
    public String name() {
        return null;
    }

}
