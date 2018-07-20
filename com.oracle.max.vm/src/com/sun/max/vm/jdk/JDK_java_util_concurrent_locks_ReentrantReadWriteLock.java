package com.sun.max.vm.jdk;

import com.sun.max.annotate.*;
import com.sun.max.vm.thread.*;

import java.util.concurrent.locks.*;

/**
 * Method substitutions for {@link java.util.concurrent.locks.ReentrantReadWriteLock}.
 *
 */
@METHOD_SUBSTITUTIONS(ReentrantReadWriteLock.class)
public final class JDK_java_util_concurrent_locks_ReentrantReadWriteLock {
   
    @SUBSTITUTE(optional = true)
    static final long getThreadId(Thread thread) {
	return thread.getId();
    }
}
