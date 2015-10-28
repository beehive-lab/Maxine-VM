package jtt.bootimagetest;

import java.io.*;
import java.util.*;

/**
 * Created by andyn on 29/10/16
 * Simple example to test heap allocaton
 */

public class TestHeapAllocation {


    public TestHeapAllocation() {
    }

    public static boolean test() {
	/*
	 The prints are there for debuggin purposes, they result in indirect calls via $r8 on ARM
	*/
	System.out.println("TestHeapAllocation.test ENTER");
        TestHeapAllocation  tmp = new TestHeapAllocation();
	System.out.println("TestHeapAllocation.test DONEALLOC");
	return tmp != null;

   }
}
