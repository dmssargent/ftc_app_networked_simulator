package org.ftccommunity.simulator.networking;


import java.util.Enumeration;
import java.util.Hashtable;

public final class SimulatorConnection {
    private static final Hashtable<String, SimulatorDeviceHandle> handles = new Hashtable<>();
    public static final String VERSION = "0.0.1";

    public static int read(String resqId, byte[] data, int length) {
        final SimulatorDeviceHandle simulatorDeviceHandle = handles.get(resqId);

        // Wait until we have the data we want
        ByteQueue queue = simulatorDeviceHandle.readQueue;
        while (queue.size() < length) {
            // Wait for more data
            if (idle()) return -1;
        }

        try {
            synchronized (simulatorDeviceHandle.lock) {
                simulatorDeviceHandle.isLocked = true;
                for (int i = length; i > 0; i--) {
                    data[length - i] = queue.getFront();
                }
            }
        } finally {
            simulatorDeviceHandle.isLocked = false;
        }

        return length;
    }

    private static boolean idle() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // re-interrupt current thread
            return true;
        }
        return false;
    }

    public static int write(String resqId, byte[] data, int length) {
        final SimulatorDeviceHandle simulatorDeviceHandle = handles.get(resqId);

        // Copy the data onto our queue
        ByteQueue queue = simulatorDeviceHandle.writeQueue;
        try {
            simulatorDeviceHandle.isLocked = true;
            synchronized (simulatorDeviceHandle.lock) {
                simulatorDeviceHandle.isLocked = true;
                for (int i = 0; i < length; i++) {
                    queue.insert(data[i]);
                }
                simulatorDeviceHandle.writeUpdate = true;
            }
        } finally {
            simulatorDeviceHandle.isLocked = false;
        }

        while (!simulatorDeviceHandle.writeUpdate) {
            if (idle()) return -1;
        }

        return length;
    }

    static Enumeration<SimulatorDeviceHandle> handles() {
        return handles.elements();
    }

    static void readInData(String id, String... bytes) {
        final SimulatorDeviceHandle handle = handles.get(id);
        synchronized (handle.lock) {
            for (String aByte : bytes) {
                handle.readQueue.insert(Byte.parseByte(aByte));
            }
        }
    }

    static void makeDevice(String id) {
        handles.put(id, new SimulatorDeviceHandle(id));
    }

    static class SimulatorDeviceHandle {
        final String id;
        final ByteQueue readQueue;
        final ByteQueue writeQueue;
        final Object lock;

        public volatile boolean isLocked;
        public volatile boolean writeUpdate;

        private SimulatorDeviceHandle(String id) {
            this.id = id;
            lock = new Object();
            readQueue = new ByteQueue();
            writeQueue = new ByteQueue();
        }
    }
}
