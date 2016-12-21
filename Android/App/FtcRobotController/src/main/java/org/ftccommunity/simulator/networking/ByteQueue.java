// File: ByteQueue.java from the package edu.colorado.collections
// Complete documentation is available from the ByteQueue link in:
//   http://www.cs.colorado.edu/~main/docs/

package org.ftccommunity.simulator.networking;

import java.util.NoSuchElementException;

/**
 * A <CODE>ByteQueue</CODE> is a queue of byte values.
 * <p>
 * <b>Limitations:</b>
 * <ol>
 * <li>The capacity of one of these queues can change after it's created, but
 * the maximum capacity is limited by the amount of free memory on the
 * machine. The constructor, <CODE>add</CODE>,
 * and <CODE>union</CODE> will result in an
 * <CODE>OutOfMemoryError</CODE> when free memory is exhausted.</li>
 * <li>A queue's capacity cannot exceed the maximum integer 2,147,483,647
 * (<CODE>Integer.MAX_VALUE</CODE>). Any attempt to create a larger capacity
 * results in a failure due to an arithmetic overflow.</li>
 * </ol>
 * <p>
 * <b>Java Source Code for this class:</b>
 * <A HREF="http://www.cs.colorado.edu/~main/edu/colorado/collections/ByteQueue.java">
 * http://www.cs.colorado.edu/~main/edu/colorado/collections/ByteQueue.java
 * </A>
 *
 * @author Michael Main
 *         <A HREF="mailto:main@colorado.edu"> (main@colorado.edu) </A>
 * @version Feb 10, 2016
 ******************************************************************************/
public class ByteQueue {
    // Invariant of the ByteQueue class:
    //   1. The number of items in the queue is in the instance variable manyItems.
    //   2. For a non-empty queue, the items are stored in a circular array
    //      beginning at data[front] and continuing through data[rear].
    //   3. For an empty queue, manyItems is zero and data is a reference to an
    //      array, but we don't care about front and rear.
    private byte[] data;
    private int manyItems;
    private int front;
    private int rear;


    /**
     * Initialize an empty queue with an initial capacity of 10.  Note that the
     * <CODE>insert</CODE> method works efficiently (without needing more
     * memory) until this capacity is reached.
     * <b>Postcondition:</b>
     * This queue is empty and has an initial capacity of 10.
     *
     * @throws OutOfMemoryError Indicates insufficient memory for:
     *                          <CODE>new byte[10]</CODE>.
     **/
    public ByteQueue() {
        final int INITIAL_CAPACITY = 10;
        manyItems = 0;
        data = new byte[INITIAL_CAPACITY];
        // We don't care about front and rear for an empty queue.
    }


    /**
     * Initialize an empty queue with a specified initial capacity. Note that the
     * <CODE>insert</CODE> method works efficiently (without needing more
     * memory) until this capacity is reached.
     *
     * @param initialCapacity the initial capacity of this queue
     *                        <b>Precondition:</b>
     *                        <CODE>initialCapacity</CODE> is non-negative.
     *                        <b>Postcondition:</b>
     *                        This queue is empty and has the given initial capacity.
     * @throws IllegalArgumentException Indicates that initialCapacity is negative.
     * @throws OutOfMemoryError         Indicates insufficient memory for:
     *                                  <CODE>new byte[initialCapacity]</CODE>.
     **/
    public ByteQueue(int initialCapacity) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("initialCapacity is negative: " + initialCapacity);
        manyItems = 0;
        data = new byte[initialCapacity];
        // We don't care about front and rear for an empty queue.
    }


    /**
     * Change the current capacity of this queue.
     *
     * @param minimumCapacity the new capacity for this queue
     *                        <b>Postcondition:</b>
     *                        This queue's capacity has been changed to at least <CODE>minimumCapacity</CODE>.
     *                        If the capacity was already at or greater than <CODE>minimumCapacity</CODE>,
     *                        then the capacity is left unchanged.
     * @throws OutOfMemoryError Indicates insufficient memory for: <CODE>new byte[minimumCapacity]</CODE>.
     **/
    public void ensureCapacity(int minimumCapacity) {
        byte biggerArray[];
        int n1, n2;

        if (data.length >= minimumCapacity)
            // No change needed.
            return;
        else if (manyItems == 0)
            // Just increase the size of the array because the queue is empty.
            data = new byte[minimumCapacity];
        else if (front <= rear) {  // Create larger array and copy data[front]...data[rear] into it.
            biggerArray = new byte[minimumCapacity];
            System.arraycopy(data, front, biggerArray, front, manyItems);
            data = biggerArray;
        } else {  // Create a bigger array, but be careful about copying items into it. The queue items
            // occur in two segments. The first segment goes from data[front] to the end of the
            // array, and the second segment goes from data[0] to data[rear]. The variables n1
            // and n2 will be set to the number of items in these two segments. We will copy
            // these segments to biggerArray[0...manyItems-1].
            biggerArray = new byte[minimumCapacity];
            n1 = data.length - front;
            n2 = rear + 1;
            System.arraycopy(data, front, biggerArray, 0, n1);
            System.arraycopy(data, 0, biggerArray, n1, n2);
            front = 0;
            rear = manyItems - 1;
            data = biggerArray;
        }
    }

    /**
     * Accessor method to get the current capacity of this queue.
     * The <CODE>insert</CODE> method works efficiently (without needing
     * more memory) until this capacity is reached.
     *
     * @return the current capacity of this queue
     **/
    public int getCapacity() {
        return data.length;
    }

    /**
     * Get the front item, removing it from this queue.
     * <b>Precondition:</b>
     * This queue is not empty.
     *
     * @return The return value is the front item of this queue, and the item has
     * been removed.
     * @throws NoSuchElementException Indicates that this queue is empty.
     **/
    public byte getFront() {
        byte answer;

        if (manyItems == 0)
            throw new NoSuchElementException("Queue underflow");
        answer = data[front];
        front = nextIndex(front);
        manyItems--;
        return answer;
    }

    /**
     * Insert a new item in this queue.  If the addition
     * would take this queue beyond its current capacity, then the capacity is
     * increased before adding the new item. The new item may be the null
     * reference.
     *
     * @param item the item to be pushed onto this queue
     *             <b>Postcondition:</b>
     *             The item has been pushed onto this queue.
     * @throws OutOfMemoryError Indicates insufficient memory for increasing the queue's capacity.
     *                          <b>Note:</b>
     *                          An attempt to increase the capacity beyond
     *                          <CODE>Integer.MAX_VALUE</CODE> will cause the queue to fail with an
     *                          arithmetic overflow.
     **/
    public void insert(byte item) {
        if (manyItems == data.length) {
            // Double the capacity and add 1; this works even if manyItems is 0. However, in
            // case that manyItems*2 + 1 is beyond Integer.MAX_VALUE, there will be an
            // arithmetic overflow and the bag will fail.
            ensureCapacity(manyItems * 2 + 1);
        }

        if (manyItems == 0) {
            front = 0;
            rear = 0;
        } else
            rear = nextIndex(rear);

        data[rear] = item;
        manyItems++;
    }

    /**
     * Determine whether this queue is empty.
     *
     * @return <CODE>true</CODE> if this queue is empty;
     * <CODE>false</CODE> otherwise.
     **/
    public boolean isEmpty() {
        return manyItems == 0;
    }


    private int nextIndex(int i)
    // Precondition: 0 <= i and i < data.length
    // Postcondition: If i+1 is data.length,
    // then the return value is zero; otherwise
    // the return value is i+1.
    {
        if (++i == data.length)
            return 0;
        else
            return i;
    }


    /**
     * Accessor method to determine the number of items in this queue.
     *
     * @return the number of items in this queue
     **/
    public int size() {
        return manyItems;
    }


    /**
     * Reduce the current capacity of this queue to its actual size (i.e., the
     * number of items it contains).
     * <b>Postcondition:</b>
     * This queue's capacity has been changed to its current size.
     *
     * @throws OutOfMemoryError Indicates insufficient memory for altering the capacity.
     **/
    public void trimToSize() {
        byte trimmedArray[];
        int n1, n2;

        if (data.length == manyItems)
        // No change needed.
        {
        } else if (manyItems == 0)
            // Just change the size of the array to 0 because the queue is empty.
            data = new byte[0];
        else if (front <= rear) {  // Create trimmed array and copy data[front]...data[rear] into it.
            trimmedArray = new byte[manyItems];
            System.arraycopy(data, front, trimmedArray, front, manyItems);
            data = trimmedArray;
        } else {  // Create a trimmed array, but be careful about copying items into it. The queue items
            // occur in two segments. The first segment goes from data[front] to the end of the
            // array, and the second segment goes from data[0] to data[rear]. The variables n1
            // and n2 will be set to the number of items in these two segments. We will copy
            // these segments to trimmedArray[0...manyItems-1].
            trimmedArray = new byte[manyItems];
            n1 = data.length - front;
            n2 = rear + 1;
            System.arraycopy(data, front, trimmedArray, 0, n1);
            System.arraycopy(data, 0, trimmedArray, n1, n2);
            front = 0;
            rear = manyItems - 1;
            data = trimmedArray;
        }
    }

    public byte[] backing() {
        return data;
    }

    public void empty() {
        manyItems = 0;
        trimToSize();
    }
}
