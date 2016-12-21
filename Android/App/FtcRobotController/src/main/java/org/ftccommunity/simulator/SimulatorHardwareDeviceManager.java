package org.ftccommunity.simulator;

import android.content.Context;

import com.qualcomm.hardware.HardwareDeviceManager;
import com.qualcomm.robotcore.eventloop.EventLoopManager;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.usb.RobotUsbManager;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcontroller.internal.FtcRobotControllerActivity;

import java.lang.reflect.Field;

public class SimulatorHardwareDeviceManager extends HardwareDeviceManager {
    public enum Mode {
        DEFAULT,
        ENABLE_DEVICE_EMULATION,
        ENABLE_DEVICE_SIMULATION
    }

    public static Mode operationMode = Mode.DEFAULT;
    public FtcRobotControllerActivity activity;

    /**
     * @param context Context of current Android app
     * @param manager event loop manager
     * @throws RobotCoreException if unable to open FTDI D2XX manager
     */
    public SimulatorHardwareDeviceManager(Context context, EventLoopManager manager) throws RobotCoreException {
        super(context, manager);
        activity = (FtcRobotControllerActivity) context;
        if (operationMode == Mode.ENABLE_DEVICE_SIMULATION) {
            for (Field field : HardwareDeviceManager.class.getDeclaredFields()) { // it is located in the parent
                try {
                    field.setAccessible(true);
                    if (field.get(this) instanceof RobotUsbManager) { // find the right field
                        RobotUsbManagerSimulator simulator = new RobotUsbManagerSimulator();
                        field.set(this, simulator);
                        RobotLog.i("[SIM] Injection successful");
                    }
                } catch (IllegalAccessException e) {
                    RobotLog.e("[SIM] issue injected RobotUsbManagerSimulator");
                }
            }
        }
    }

    void requestRobotRestart() {

    }
}
