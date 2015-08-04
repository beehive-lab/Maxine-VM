/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
#include <stdio.h>
#include <pthread.h>

unsigned int getTID(unsigned int tid) {
	static pthread_mutex_t simulationLock = PTHREAD_MUTEX_INITIALIZER;
        static unsigned int maxTID = 0;
        static unsigned int pthreadsToSimThreads[32]; // ARMV7 limited to 16 so more than enough
	unsigned int i;
        

        pthread_mutex_lock(&simulationLock);

	for(i = 0; i < maxTID;i++) {
		if(pthreadsToSimThreads[i] == tid)	{
			tid = i;
			pthread_mutex_unlock(&simulationLock);
			return tid;
		}
	}
	pthreadsToSimThreads[maxTID] = tid;
	tid = maxTID++;
	pthread_mutex_unlock(&simulationLock);
        return tid;
}

void pushsimulation(unsigned int thread, unsigned int address) {

	/*
	The address has the LSBs set to indicate data/code address and r/w 
	John to insert his code here
	*/	
}
/*
Interface to the simulation platform,  done in C
could not get cpp to link easily with the make script
#include <iostream>
#include <map>
#include <mutex>
#ifdef arm

std::mutex simulationLock;

extern "C" unsigned int getTID(unsigned int tid) {
	static unsigned int maxTID = 0;
	static std::map <unsigned int, unsigned int> pthreadsToSimThreads;
	std::map <unsigned int, int>::iterator it;
	

	simulationLock.lock();
	if(tid == -1) {
		// utility to get the maximum number of threads to be processed.
		tid = maxTID;
		simulationLock.unlock();
		return tid;
	}
	it = pthreadsToSimThreads.find(tid);
	if(it == pthreadsToSimThreads.end()) 	{
		// the pthread_self tid is not in the map so we
		// need to insert it and increment the maxTID.
		pthreadsToSimThreads.insert(std>::pair<unsigned int, unsigned int> (tid, maxTID++));
		it = pthreadsToSimThreads.find(tid);
		tid = (*it).second;
	} else {
		tid = (*it).second;
	}
	std::cout << "THREAD ID is  " << tid std::endl;
	simulationLock.unlock();
	return tid;
}
#endif
 */
