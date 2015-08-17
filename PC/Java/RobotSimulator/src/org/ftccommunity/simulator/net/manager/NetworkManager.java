package org.ftccommunity.simulator.net.manager;

import com.google.common.base.Charsets;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.net.InetAddresses;
import com.sun.istack.internal.NotNull;
import org.ftccommunity.simulator.net.Client;
import org.ftccommunity.simulator.net.protocol.SimulatorData;
import org.ftccommunity.simulator.net.tasks.HeartbeatTask;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class NetworkManager {
    public static final String host = "192.168.42.129";
    public static final int port = 7002;
    private final static LinkedListMultimap<SimulatorData.Type.Types, SimulatorData.Data> main = LinkedListMultimap.create();
    private final static LinkedList<SimulatorData.Data> receivedQueue = new LinkedList<>();
    private final static LinkedList<SimulatorData.Data> sendingQueue = new LinkedList<>();
    private static InetSocketAddress robotAddress;
    private static boolean isReady;

    /**
     * Add a recieved packet to the processing queue for deferred processing
     * @param data The data to add to processing queue
     */
    public static void add(@NotNull SimulatorData.Data data) {
        synchronized (receivedQueue) {
            receivedQueue.add(data);
        }
    }

    /**
     * Force the queue to sort the processing queue into their respective types
     */
    public synchronized static void processQueue() {
        int size;
        synchronized (receivedQueue) {
            size = receivedQueue.size();
        }
        while (size < 1 && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            synchronized (receivedQueue) {
                size = receivedQueue.size();
            }
        }

        ConcurrentLinkedQueue<SimulatorData.Data> temp = new ConcurrentLinkedQueue<>();
        // Move the contents over to an new queue
        synchronized (receivedQueue) {
            temp.addAll(receivedQueue);
            receivedQueue.clear();
        }

        // Flip the old moved data into a new container
        LinkedList<SimulatorData.Data> tempB = new LinkedList<>();
        while (!temp.isEmpty()) {
            tempB.add(temp.poll());
        }
        temp.clear();

        // Then, add the flipped data so the oldest gets processed first
        while (!tempB.isEmpty()) {
            SimulatorData.Data data = tempB.poll();
            main.put(data.getType().getType(), data);
        }
        tempB.clear();
    }

    /**
     * Find what the latest data is and returns it based on the type need
     * @param type the type of packet to get
     * @return The latest message in the queue, based on the type
     */
    @NotNull
    public static SimulatorData.Data getLatestMessage(@NotNull SimulatorData.Type.Types type) throws InterruptedException {
            return getLatestMessage(type, false);
    }

    /**
     * Find what the latest data is and returns it based on the type need
     * @param type the type of packet to get
     * @param cache delete the value
     * @return The latest message in the queue, based on the type
     */
    @NotNull
    public static SimulatorData.Data getLatestMessage(@NotNull SimulatorData.Type.Types type, boolean cache) throws InterruptedException {
        int size;
        synchronized (main) {
            size = main.get(type).size();
        }
        while ((size <= 0) && !(Thread.currentThread().isInterrupted())) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedException();
            }
            // Fetch the size again
            synchronized (main) {
                size = main.get(type).size();
            }
        }

        synchronized (main) {
            System.out.println("Retrieving latest packet of " + type.getValueDescriptor().getName());
            if (cache) {
                SimulatorData.Data data =  main.get(type).get(main.get(type).size() - 1);
                clear(type);
                main.put(type, data);
                return data;
            } else {
                return main.get(type).remove(main.get(type).size() - 1);
            }
        }
    }

    /**
     *  This returns that data information based on the type specifed
     * @param type the type of data to get
     * @return a byte array of the latest data
     */
    @NotNull
    public static byte[] getLatestData(@NotNull SimulatorData.Type.Types type) throws InterruptedException {
        return getLatestData(type, false);
    }

    /**
     *  This returns that data information based on the type specifed
     * @param type the type of data to get
     * @param cache delete the old version
     * @return a byte array of the latest data
     */
    @NotNull
    public static byte[] getLatestData(@NotNull SimulatorData.Type.Types type, boolean cache) throws InterruptedException {
        SimulatorData.Data data = getLatestMessage(type, cache);
        return data.getInfo(0).getBytes(Charsets.US_ASCII);
    }

    /**
     * Clears the queue for a specific type
     * @param type the type of data to get
     */
    public static void clear(SimulatorData.Type.Types type) {
        main.get(type).clear();
    }

    public static boolean isReadyToFetch(SimulatorData.Type.Types type) {
        return main.get(type).size() > 0;
    }

    /**
     * Request the packets to be sent, the sending does not have a guarantee to be sent
     * @param type the type of data to send
     * @param module which module correlates to the data being sent
     * @param data a byte array of data to send
     */
    public static void requestSend(SimulatorData.Type.Types type, SimulatorData.Data.Modules module, byte[] data) {
        String dataString = new String(data, Charsets.US_ASCII);
        System.out.println(dataString);
        requestSend(type, module, dataString);
    }

    /**
     * Request the packets to be sent, the sending does not have a guarantee to be sent
     * @param type the type of data to send
     * @param module which module correlates to the data being sent
     * @param data a string of data to send
     */
    public static void requestSend(SimulatorData.Type.Types type, SimulatorData.Data.Modules module, String data) {
        SimulatorData.Data.Builder sendDataBuilder = SimulatorData.Data.newBuilder();
        sendDataBuilder.setType(SimulatorData.Type.newBuilder().setType(type).build())
                .setModule(module)
                .addInfo(data);
        synchronized (sendingQueue) {
            sendingQueue.add(sendDataBuilder.build());
        }
    }

    /**
     * Gets the next data to send
     * @return the next data to send
     */
    @NotNull
    public static SimulatorData.Data getNextSend() {
        int size;
        synchronized (sendingQueue) {
            size = sendingQueue.size();
        }
        if (size > 100) {
            LinkedList<SimulatorData.Data> temp = new LinkedList<>();
            synchronized (sendingQueue) {
                for (int i = size - 1; i > size / 2; i--) {
                    temp.add(sendingQueue.pollLast());
                }
                sendingQueue.clear();
                sendingQueue.addAll(temp);
            }
        }

        if (size > 0) {
            System.out.println("Getting next send.");
            return sendingQueue.removeFirst();
        } else {
            return HeartbeatTask.buildMessage();
        }
    }

    /**
     * Rertrieve the next datas to send
     * @return an array of the entire sending queue
     */
    public static SimulatorData.Data[] getNextSends() {
        return getNextSends(sendingQueue.size());
    }

    /**
     * Rertieve an the next datas to send based on a specificed amount
     * @param size the maximum, inclusive size of the data array
     * @return a data array of the next datas to send up to a limit
     */
    public static SimulatorData.Data[] getNextSends(int size) {
        return getNextSends(size, true);
    }

    /**
     * Retrieve an array of the next datas to send up to a specific size
     * @param size the maximum size of the returned array
     * @param autoShrink if true this automatically adjusts the size returned
     * @return a data array of the next datas to send
     */
    @NotNull
    public static SimulatorData.Data[] getNextSends(final int size, final boolean autoShrink) {
        int currentSize = size;
        if (currentSize <= sendingQueue.size() / 2) {
            cleanup();
        }

        if (size > sendingQueue.size() && !autoShrink) {
            throw new IndexOutOfBoundsException("Size is bigger then sending queue");
        }

        if (autoShrink) {
            if (size >  sendingQueue.size()) {
                currentSize = sendingQueue.size();
            }
        }

        SimulatorData.Data[] datas;
        if (currentSize == 0) {
            datas = new SimulatorData.Data[1];
            datas[0] = HeartbeatTask.buildMessage();
        } else {
            datas = new SimulatorData.Data[currentSize];
            synchronized (sendingQueue) {
                for (int i = 0; i < datas.length; i++) {
                    datas[i] = sendingQueue.removeLast();
                }
            }
        }
        return datas;
    }

    /**
     * Cleanup the sending queue
     */
    private static void cleanup() {
        synchronized (sendingQueue) {
            if (sendingQueue.size() > 100) {
                LinkedList<SimulatorData.Data> temp = new LinkedList<>();
                for (int i = sendingQueue.size() - 1; i > sendingQueue.size() / 2; i--) {
                    temp.add(sendingQueue.get(i));
                }
                sendingQueue.clear();
                sendingQueue.addAll(temp);
            }
        }
    }

    public static InetSocketAddress getRobotAddress() {
        return new InetSocketAddress(InetAddresses.forString(host), port);
    }

    /**
     * Sets the current Robot IP address
     * @param robotAddress an <code>InetAddress</code> of the Robot Controller
     */
    public static void setRobotAddress(@NotNull InetSocketAddress robotAddress) {
        NetworkManager.robotAddress = robotAddress;
    }

    /**
     * Returns if enough data has been received to start up
     * @return whether or not the robot server can start up
     */
    @NotNull
    public static boolean isReady() {
        return isReady;
    }

    /**
     * Change the readiness state of the Manager
     * @param isReady what the current status is of the network
     */
    public static void changeReadiness(boolean isReady) {
        NetworkManager.isReady = isReady;
    }

    public static Thread start() {
       Thread clientListener = new Thread(new Client(), "Client");
        clientListener.start();
        return clientListener;
    }

}

