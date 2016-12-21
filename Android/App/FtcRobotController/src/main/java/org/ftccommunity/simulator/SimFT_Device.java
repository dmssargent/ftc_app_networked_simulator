package org.ftccommunity.simulator;

import com.ftdi.j2xx.D2xxManager;
import com.qualcomm.robotcore.util.SerialNumber;

import org.ftccommunity.simulator.networking.SimulatorConnection;

public class SimFT_Device {
    private final SerialNumber serialNumber;
    private int baudRate;
    private byte latencyTimer;
    private int readTimeout;
    private boolean open;
    private D2xxManager.FtDeviceInfoListNode deviceInfo;
    private final String resqId;

    public SimFT_Device(SerialNumber serialNumber) {
        this.serialNumber = serialNumber;
        resqId = serialNumber.toString();
    }

    public void purge(byte purgeWhat) {

    }

    public boolean setBaudRate(int baudRate) {
        this.baudRate = baudRate;
        return true;
    }

    public boolean setDataCharacteristics(byte dataBits, byte stopBits, byte parity) {
        return true;
    }

    public boolean setLatencyTimer(byte latencyTimer) {
        this.latencyTimer = latencyTimer;

        return true;
    }

    public boolean setBreakOn() {

        return true;
    }


    public boolean setBreakOff() {

        return true;
    }

    private boolean setBreak(int OnOrOff) {
        boolean rc = false;
        int wValue = this.deviceInfo.breakOnParam;
        wValue |= OnOrOff;
        if (!this.isOpen()) {
            return false;
        } else {

            //int status = this.getConnection().controlTransfer(64, 4, wValue, this.mInterfaceID, (byte[])null, 0, 0);
            //

            return true;
        }
    }

    public void write(byte[] data) {
        SimulatorConnection.write(resqId, data, data.length);
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public int read(byte[] data, int cbToRead, long msTimeout) {
        return SimulatorConnection.read(resqId, data, cbToRead);
    }

    public void close() {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    public D2xxManager.FtDeviceInfoListNode getDeviceInfo() {
        return deviceInfo;
    }
}
