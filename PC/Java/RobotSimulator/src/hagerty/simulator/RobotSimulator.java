package hagerty.simulator;

import hagerty.simulator.modules.BrickSimulator;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RobotSimulator  {
    private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private static BrickListGenerator gBrickListGenerator;
    private static CoppeliaApiClient gCoppeliaApiClient;
    private static volatile boolean gThreadsAreRunning = true;
    private static LinkedList<Thread> threadLinkedList = new LinkedList<>();
    private static int gPhonePort;
    private static InetAddress gPhoneIPAddress;

    private static boolean simulatorStarted = false;
    private static boolean visualizerStarted = false;

    static public void startSimulator(hagerty.gui.MainApp mainApp) {
    	simulatorStarted = true;
        try {
            System.out.println("Waiting for Robot Controller seeker...");
            DatagramSocket mSeekerSocket = new DatagramSocket(7000);

            byte[] seekerBytes = new byte[1024];
            DatagramPacket mSeekerPacket = new DatagramPacket(seekerBytes, seekerBytes.length);
            mSeekerSocket.receive(mSeekerPacket);
            System.out.println("Robot Controller discovered at " + mSeekerPacket.getAddress());

            System.out.println("Replying back...");
            byte[] handShakeBytes = InetAddress.getLocalHost().toString().getBytes(StandardCharsets.US_ASCII);

            DatagramPacket mHandshake = new DatagramPacket(handShakeBytes, handShakeBytes.length);
            mSeekerSocket.send(mHandshake);
            mSeekerSocket.close();
        } catch (Exception ex) {
            System.err.println(ex.toString());
            logger.log(Level.SEVERE, ex.toString());
        }

		// Start the module info server
    	System.out.println("Starting Module Lister...");
        gBrickListGenerator = new BrickListGenerator(mainApp);  // Runnable
        Thread moduleListerThread = new Thread(gBrickListGenerator,"");
        moduleListerThread.start();

        // Start the individual threads for each module
        // Read the current list of modules from the GUI MainApp class
        List<BrickSimulator> brickList = mainApp.getBrickData();

        for (BrickSimulator temp : brickList) {
            Thread t = new Thread(temp, temp.getAlias());  // Make a thread from the object and also set the process name
            t.start();
            threadLinkedList.add(t);
            System.out.println(temp.getAlias() + " " + temp.getName());
		}
    }

    static public boolean simulatorStarted() {
    	return simulatorStarted;
    }

    static public void startVisualizer(hagerty.gui.MainApp mainApp) {
        visualizerStarted = true;

		// Start the module info server
    	System.out.println("Starting Visualizer...");
    	gCoppeliaApiClient = new CoppeliaApiClient(mainApp); // Runnable
        Thread coppeliaThread = new Thread(gCoppeliaApiClient);
        if (gCoppeliaApiClient.init()) {
    		coppeliaThread.start();
    	} else {
    		System.out.println("Initialization of Visualizer failed");
    	}
    }

    static public boolean visualizerStarted() {
    	return visualizerStarted;
    }

    public static boolean isgThreadsAreRunning() {
        return gThreadsAreRunning;
    }

    public static void requestTermination() {
        gThreadsAreRunning = false;
        try {
            Thread.currentThread().wait(50);
        } catch (InterruptedException ex) {
            ex.toString(); // Do nothing
        }
        threadLinkedList.forEach(Thread::interrupt);
        Thread.currentThread().interrupt();
    }

    public static int getPhonePort() {
        return gPhonePort;
    }

    public static InetAddress getPhoneIPAddress() {
        return gPhoneIPAddress;
    }

    public static void setPhoneConnectionDetails(int phonePort, InetAddress phoneIpAddress) {
        gPhonePort = phonePort;
        gPhoneIPAddress = phoneIpAddress;
    }
}
