package hagerty.simulator;

import hagerty.simulator.modules.BrickSimulator;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

public class RobotSimulator  {

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

		// Start the module info server
    	System.out.println("Starting Module Lister...");
        gBrickListGenerator = new BrickListGenerator(mainApp);  // Runnable
        Thread moduleListerThread = new Thread(gBrickListGenerator,"");
        moduleListerThread.start();

        // Start the individual threads for each module
        // Read the current list of modules from the GUI MainApp class
        List<BrickSimulator> brickList = mainApp.getBrickData();

        for (BrickSimulator temp : brickList) {
        	Thread t = new Thread(temp,temp.getAlias());  // Make a thread from the object and also set the process name
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
    	Thread coppeliaThread = new Thread(gCoppeliaApiClient,"");
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

    public static void GetPhoneConnectionDetails(int phonePort, InetAddress phoneIpAddress) {
        gPhonePort = phonePort;
        gPhoneIPAddress = phoneIpAddress;
    }
}