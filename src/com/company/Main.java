package com.company;

public class Main {

    public static void main(String[] args) {
	 PageAllocator allocator = new PageAllocator();
        allocator.memAlloc(10);
//        allocator.memDamp();
        allocator.memAlloc(10);
        allocator.memAlloc(8);
        allocator.memAlloc(64);
        allocator.memAlloc(217);
        allocator.memDamp();
        System.out.println(allocator.memFree("0018"));
        System.out.println(allocator.memAlloc(10));
//        allocator.memDamp();
        System.out.println(allocator.memFree("0140"));
//        allocator.memDamp();
        allocator.memAlloc(200);
//        allocator.memDamp();
       allocator.memRealloc("0018", 20);
       allocator.memRealloc("00C0", 30);
        allocator.memDamp();

    }
}
