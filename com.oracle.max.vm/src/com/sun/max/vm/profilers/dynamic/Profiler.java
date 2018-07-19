package com.sun.max.vm.profilers.dynamic;

import com.sun.max.unsafe.Size;
import com.sun.max.vm.Log;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.thread.VmThread;

import java.util.Hashtable;

public class Profiler {

    /**
     * Histogram: the data structure that stores the profiling outcome
     */

    public Hashtable histogram;


    //constructor
    public Profiler() {
        this.histogram = new Hashtable();
    }


    /**
     * Methods to manage the histogram
     */

    /**
     * This method is called when a profiled object is allocated.
     * Increments the number of the equal-size allocated objects.
     * */
    public void record(Size size){
        //histogram.get(size);
        Log.println("Size="+size.toLong()+" Bytes, ThreadId="+VmThread.current().id());

    }

    public void profile(Size size){
        // if the thread local profiling flag is enabled
        if(!VmThread.current().PROFILE){
            if(MaxineVM.isRunning()){
                record(size);
            }
        }
    }

    
}
