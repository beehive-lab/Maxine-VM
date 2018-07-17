package com.sun.max.vm.profilers.dynamic;

import com.sun.max.vm.profiler.Histogram.Map;

public class Main {

    public static void main(String[] args) {
	// write your code here
        Histogram h = new Histogram();
        Map<String, Integer>map = h.new Map<>();
        map.add("this",1 );
        map.add("coder",2 );
        map.add("this",4 );
        map.add("hi",5 );
        System.out.println(map.size());
        System.out.println(map.remove("this"));
        System.out.println(map.remove("this"));
        System.out.println(map.size());
        System.out.println(map.isEmpty());
    }
}
