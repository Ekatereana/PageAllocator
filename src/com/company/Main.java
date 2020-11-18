package com.company;

public class Main {

    public static void main(String[] args) {
	 PageAllocator allocator = new PageAllocator();
        System.out.println(allocator.memAlloc(10));
        System.out.println(allocator.memAlloc(10));
        System.out.println(allocator.memAlloc(8));
        System.out.println(allocator.memAlloc(64));
        System.out.println("2017 " + allocator.memAlloc(217));
//        allocator.memDamp();
        System.out.println(allocator.memFree("0018"));
        System.out.println(allocator.memAlloc(10));
//        allocator.memDamp();
        System.out.println(allocator.memFree("0140"));
//        allocator.memDamp();
        allocator.memAlloc(200);
//        allocator.memDamp();
        System.out.println(allocator.memRealloc("0018", 20));
        System.out.println(allocator.memRealloc("00C0", 30));
        allocator.memDamp();

    }
}
