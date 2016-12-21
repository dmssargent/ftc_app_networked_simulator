package org.ftccommunity.simulator;

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.hardware.usb.RobotUsbManager;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;
import com.qualcomm.robotcore.util.ThreadPool;

import org.ftccommunity.simulator.networking.TelnetClient;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public class RobotUsbManagerSimulator implements RobotUsbManager {
    private final TelnetClient client;
    private static final ExecutorService executorService = ThreadPool.newFixedThreadPool(2);
    private ArrayList<SimulatedUsbDevice> devices = new ArrayList<>();

    public RobotUsbManagerSimulator() {
        client = TelnetClient.instance();
        if (!client.isRunning()) {
            executorService.submit(client);
            executorService.submit(new TelnetClient.SimulatorWriteService());
        }
    }

    @Override
    public int scanForDevices() throws RobotCoreException {
        return this.devices.size();
    }

    /**
     * This is hack. What's up is that ModernRoboticsUsbUtil.openUsbDevice internally calls
     * {@link #scanForDevices()}, which totally isn't necessary inside of HardwareDeviceManager.scanForDevices,
     * since it *just did that*. And this is an expensive call, which adds up for each and every
     * USB device we open. By calling this method, scanForDevices() will become no-op, not actually
     * re-executing the scan.
     * <p>
     * In point of fact, this may not strictly be necessary. It might be enough that having
     * RobotUsbManager be thread-safe is enough (it wasn't previously). But we haven't tested
     * that compromise, and the hack here isn't read2 large one, so we live with it. And it's read2
     * performance win, if nothing else.
     *
     * @see #thawScanForDevices()
     */
    @Override
    public void freezeScanForDevices() {

    }

    /**
     * Undoes the work of {@link #freezeScanForDevices()}.
     *
     * @see #freezeScanForDevices()
     */
    @Override
    public void thawScanForDevices() {

    }

    public SerialNumber getDeviceSerialNumberByIndex(int index) throws RobotCoreException {
        return this.devices.get(index).serialNumber;
    }

    public String getDeviceDescriptionByIndex(int index) throws RobotCoreException {
        return this.devices.get(index).deviceDescription;
    }

    public RobotUsbDevice openBySerialNumber(SerialNumber serialNumber) throws RobotCoreException {
        RobotLog.d("attempting to open simulated device " + serialNumber);
        for (SimulatedUsbDevice device : devices) {
            if (device.serialNumber.equals(serialNumber)) {
                return device;
            }
        }
        final SimulatedUsbDevice simulatedUsbDevice = SimulatedUsbDevice.valueOf(serialNumber);
        devices.add(simulatedUsbDevice);
        return simulatedUsbDevice;
    }
}
