package com.company;

import java.util.Arrays;
import java.util.LinkedList;

/*
 * state --
 *  Page can be presented by 4 types of states:
 *   0 ~ free at all
 *   1 ~ present some class of blocks
 *   2 ~ is part of the multi-page block
 *   3 ~ have no free blocks to use. Presented by particular class*/

public class PageAllocator {
    private final int DEFAULT_PAGE_SIZE = (int) Math.pow(2, 6);
    private int REQ_MEMORY = (int) Math.pow(2, 10);
    private final int HEADER_SIZE = 16;
    private final int BLOCK_HEADER = 8;
    private final int PART_SIZE = 4;
    private final int PAGE_NUMBER = Math.round(REQ_MEMORY / (DEFAULT_PAGE_SIZE));
    private int[] desc_address = new int[PAGE_NUMBER];
    private byte[] MEMORY;

    public PageAllocator() {
        for (int i = 0; i < desc_address.length; i++) {
            desc_address[i] = i * DEFAULT_PAGE_SIZE;
        }
        this.MEMORY = new byte[REQ_MEMORY];

    }


    public String memAlloc(int size) {
        int rounded = roundToPow(size);
        Integer address = findFreePage(rounded);
        return String.format("%04X", address);
    }

    public String memRealloc(String address, int size){
        byte[] backup = new byte[REQ_MEMORY];
        backup = MEMORY;
        boolean isDel = memFree(address);
        if (!isDel) {
            MEMORY = backup;
            return null;
        }
        String newAddress = memAlloc(size);
        if (newAddress == null) {
            MEMORY = backup;
        }
        return newAddress;
    }

    public boolean memFree(String address) {
        int real = Integer.parseInt(address, 16);
        Integer page = findPage(real);
        if (page == null) {
            return false;
        } else {
            byte[] header = Arrays.copyOfRange(MEMORY, desc_address[page], HEADER_SIZE + desc_address[page]);
            int state = HeaderConvector.CONVECTOR.charArrayToInt(Arrays.copyOfRange(header, 0, PART_SIZE));
            int size = HeaderConvector.CONVECTOR.charArrayToInt(Arrays.copyOfRange(header, PART_SIZE, 2 * PART_SIZE));
            int counter = HeaderConvector.CONVECTOR.charArrayToInt(Arrays.copyOfRange(header, 2 * PART_SIZE, 3 * PART_SIZE));
            if (state == 2) {
                byte[] tempoHeader;
                LinkedList<Integer> toFreePages = new LinkedList<>();
                for (int i = 0; i < counter; i++) {
                    tempoHeader = Arrays.copyOfRange(MEMORY, desc_address[page + i], HEADER_SIZE + desc_address[page + i]);
                    if (HeaderConvector.CONVECTOR.charArrayToInt(Arrays.copyOfRange(tempoHeader, 0, PART_SIZE)) != 2 ||
                            HeaderConvector.
                                    CONVECTOR.
                                    charArrayToInt(Arrays.copyOfRange(tempoHeader, PART_SIZE, 2 * PART_SIZE)) != size) {
                        return false;
                    } else {
                        toFreePages.add(page + i);

                    }
                }
                int iterator;
                int actual;
                for (int i = 0; i < counter; i++) {
                    iterator = toFreePages.poll();
                    tempoHeader = Arrays.copyOfRange(MEMORY, desc_address[iterator], HEADER_SIZE + desc_address[iterator]);
                    actual = HeaderConvector.
                            CONVECTOR.
                            charArrayToInt(Arrays.copyOfRange(tempoHeader, 3 * PART_SIZE, HEADER_SIZE));
                    rewriteHeader(size, actual, 0, 0, 0);
                    memFree(String.format("%04X", desc_address[iterator]));
                    desc_address[iterator] = actual;
                }

            } else {
                int free = HeaderConvector.CONVECTOR.charArrayToInt(Arrays.copyOfRange(header, 3 * PART_SIZE, HEADER_SIZE));
                int newFree = free > real || free == 0 ? real : free;
                int newState = state;
                int n = (DEFAULT_PAGE_SIZE - HEADER_SIZE) / (size + BLOCK_HEADER);
                if (n == counter + 1) {
                    newState--;
                } else {
                    if (state == 3) {
                        newState = 1;
                    }
                }
                rewriteHeader(size, desc_address[page], ++counter, newFree, newState);
                int prev = HeaderConvector.CONVECTOR.charArrayToInt(
                        Arrays.copyOfRange(MEMORY, real - BLOCK_HEADER, real - BLOCK_HEADER / 2));
                rewriteBlockHeader(real - BLOCK_HEADER, prev, 0);
            }

        }


        return true;
    }

    private Integer findPage(int real) {
        for (int i = 0; i < PAGE_NUMBER; i++) {
            if (real >= i * DEFAULT_PAGE_SIZE && real < (i + 1) * DEFAULT_PAGE_SIZE) {
                return i;
            }
        }
        return null;
    }


    private int writeTo(Integer address, int rounded) {
        byte[] header = Arrays.copyOfRange(MEMORY, address, HEADER_SIZE + address);
        int counter = HeaderConvector.CONVECTOR.charArrayToInt(Arrays.copyOfRange(header, 2 * PART_SIZE, 3 * PART_SIZE));
        int free = HeaderConvector.CONVECTOR.charArrayToInt(Arrays.copyOfRange(header, 3 * PART_SIZE, HEADER_SIZE));

        boolean wouldBeFull = counter - 1 == 0;
        if (wouldBeFull) {
            rewriteHeader(rounded, address, 0, 0, 3);

        } else {
            rewriteHeader(rounded, address, counter - 1, findNextFree(rounded, free), 1);

        }
        int number = (DEFAULT_PAGE_SIZE - HEADER_SIZE) / (rounded + BLOCK_HEADER);

        if (counter == number) {
            rewriteBlockHeader(free - BLOCK_HEADER, free, 1);
        } else {
            rewriteBlockHeader(free - BLOCK_HEADER,
                    getPreviousBlock(free - BLOCK_HEADER, rounded, address + HEADER_SIZE), 1);
        }
        return free;
    }

    private int findNextFree(int size, int start) {
        int state = 1;
        byte[] header;
        int i = 1;
        while (state != 0) {
            header = Arrays.copyOfRange(MEMORY, start + i * (size) + (i - 1) * BLOCK_HEADER, start + i * (size + BLOCK_HEADER));
            state = HeaderConvector.CONVECTOR.charArrayToInt(Arrays.copyOfRange(header, 4, BLOCK_HEADER));
            i++;
        }
        return start + (i - 1) * (size + BLOCK_HEADER);
    }


    private Integer findFreePage(int size) {
        int address;
        if (size > DEFAULT_PAGE_SIZE / 2) {
            int count = Math.round(size / DEFAULT_PAGE_SIZE);
            LinkedList<Integer> freePages = new LinkedList<>();
            freePages.poll();
            for (int i = 0; i < desc_address.length; i++) {
                address = desc_address[i];
                if (checkPage(size, address, true)) {
                    freePages.add(i);
                }
            }
            freePages.poll();
            if (count <= freePages.size()) {

                // free memory for desc_address
                Integer newDescAddress;
                address = freePages.poll();
                Integer first = desc_address[address];
                for (int i = 0; i < count; i++) {

                    newDescAddress = Integer.parseInt(memAlloc(16), 16);
                    if (newDescAddress == null) {
                        return null;
                    }
                    rewriteHeader(size, newDescAddress, count, desc_address[address], 2);
                    desc_address[address] = newDescAddress;

                    address = freePages.poll();
                }

                return first;
            }


        } else {
            int finder = 0;
            for (int i = 0; i < desc_address.length; i++) {
                address = desc_address[i];
                if (checkPage(size, address, false)) {
                    finder = i;
                    break;
                }

            }
            return writeTo(desc_address[finder], size);

        }

        return null;
    }

    private boolean checkPage(int size, int address, boolean isFree) {
        byte[] bytes = Arrays.copyOfRange(MEMORY, address, address + HEADER_SIZE);
        boolean good;
        int state = HeaderConvector.CONVECTOR.charArrayToInt(Arrays.copyOfRange(bytes, 0, PART_SIZE));
        int className = HeaderConvector.CONVECTOR.charArrayToInt(Arrays.copyOfRange(bytes, PART_SIZE, 2 * PART_SIZE));
        if (isFree) {
            good = state == 0;

        } else {
            good =
                    (state == 1 || state == 0) &&
                            (className == size || className == 0);
            if (state == 0) {
                rewriteHeader(size, address,
                        (DEFAULT_PAGE_SIZE - HEADER_SIZE) / (size + BLOCK_HEADER),
                        address + HEADER_SIZE + BLOCK_HEADER, 1);
            }
        }
        return good;
    }

    private void rewriteHeader(int size, int address, int count, int free, int state) {
        byte[] stateArr = HeaderConvector.CONVECTOR.intToArray(state);
        byte[] sizeArr = HeaderConvector.CONVECTOR.intToArray(size);
        byte[] countArr = HeaderConvector.CONVECTOR.intToArray(count);
        byte[] freeArr = HeaderConvector.CONVECTOR.intToArray(free);
        byte[] header = new byte[HEADER_SIZE];
        System.arraycopy(stateArr, 0, header, 0, PART_SIZE);
        System.arraycopy(sizeArr, 0, header, PART_SIZE, PART_SIZE);
        System.arraycopy(countArr, 0, header, 2 * PART_SIZE, PART_SIZE);
        System.arraycopy(freeArr, 0, header, 3 * PART_SIZE, PART_SIZE);
        for (int i = 0; i < HEADER_SIZE; i++) {
            MEMORY[address + i] = header[i];

        }
    }


    private void rewriteBlockHeader(int address, int previous, int empty) {
        byte[] header = new byte[BLOCK_HEADER];
        System.arraycopy(HeaderConvector.CONVECTOR.intToArray(previous), 0, header, 0, BLOCK_HEADER / 2);
        System.arraycopy(HeaderConvector.CONVECTOR.intToArray(empty), 0, header, BLOCK_HEADER / 2, BLOCK_HEADER / 2);
        for (int i = 0; i < BLOCK_HEADER; i++) {
            MEMORY[address + i] = header[i];
        }
    }

    private int getPreviousBlock(Integer address, int rounded, int minAddress) {
        int state = 0;
        byte[] previous;
        int i = 1;
        if (minAddress > address - i * (rounded + BLOCK_HEADER)) {
            return 0;
        }
        while (state != 1) {
            previous = Arrays.copyOfRange(MEMORY, address - i * (rounded + BLOCK_HEADER),
                    address - i * rounded - (i - 1) * BLOCK_HEADER);
            state = HeaderConvector.CONVECTOR.charArrayToInt(Arrays.copyOfRange(previous, 4, BLOCK_HEADER));
            i++;
        }
        return address - (i - 1) * rounded - (i - 2) * BLOCK_HEADER;
    }

    private int roundToPow(int size) {
        int i = 0;
        while (size > Math.pow(2, i)) {
            i++;
        }
        return (int) Math.pow(2, i);
    }

    public void memDamp() {
        int address;
        System.out.println("Number of pages:: " + PAGE_NUMBER);
        for (int i = 0; i < desc_address.length; i++) {
            address = desc_address[i];
            System.out.println("Page-- " + i);
            printPage(address);
        }
    }

    private void printPage(int address) {
        byte[] header = Arrays.copyOfRange(MEMORY, address, address + HEADER_SIZE);
        int state = HeaderConvector.CONVECTOR.charArrayToInt(Arrays.copyOfRange(header, 0, PART_SIZE));

        if (state == 0) {
            System.out.println("empty page");
        } else {
            int className = HeaderConvector.CONVECTOR.charArrayToInt(Arrays.copyOfRange(header, PART_SIZE, 2 * PART_SIZE));
            int counter = HeaderConvector.CONVECTOR.charArrayToInt(Arrays.copyOfRange(header, 2 * PART_SIZE, 3 * PART_SIZE));
            int free = HeaderConvector.CONVECTOR.charArrayToInt(Arrays.copyOfRange(header, 3 * PART_SIZE, HEADER_SIZE));
            System.out.println("Page state::" + state);
            System.out.println("Page class::" + className);
            System.out.println("Page count of free::" + counter);
            String freeString = free != 0 ? String.format("%04X", free) : "none";
            System.out.println("Page first free address::" + freeString);
            int number = (DEFAULT_PAGE_SIZE - HEADER_SIZE) / (className + BLOCK_HEADER);
            if (state != 2) {
                int actualAddress = address + HEADER_SIZE;
                byte[] head;
                int empty;
                System.out.println("//Blocks//");
                for (int i = 0; i < number; i++) {
                    head = Arrays.copyOfRange(MEMORY, actualAddress + i * (className + BLOCK_HEADER),
                            actualAddress + BLOCK_HEADER + i * (className + BLOCK_HEADER));
                    empty = HeaderConvector.CONVECTOR.charArrayToInt(Arrays.copyOfRange(head, 4, 8));
                    if (empty != 0) {
                        System.out.print("address::" +
                                String.format("%04X", actualAddress + i * (className + BLOCK_HEADER) + BLOCK_HEADER));
                        System.out.print(" || state::" + empty);
                        System.out.println();
                    }
                }

            } else {
                System.out.println("part of multi-page block");
            }

        }
    }

}
