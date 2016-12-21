package org.ftccommunity.simulator;

import com.qualcomm.ftccommon.FtcEventLoopHandler;
import com.qualcomm.hardware.HardwareFactory;
import com.qualcomm.robotcore.eventloop.EventLoop;
import com.qualcomm.robotcore.eventloop.EventLoopManager;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.robocol.TelemetryMessage;
import com.qualcomm.robotcore.util.RobotLog;

import java.lang.reflect.Field;

/**
 * Created by David on 12/20/2016.
 */
class SimulatorEventLoopHandler extends FtcEventLoopHandler {
    private final FtcEventLoopHandler handler;
    HardwareFactory _hardwareFactory;

    private SimulatorEventLoopHandler(FtcEventLoopHandler handler) {
        super(null, null, null);
        this.handler = handler;

        // Copy the state over to me
        for (Field field : handler.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                final Field declaredField = this.getClass().getDeclaredField(field.getName());
                final Object value = field.get(handler);
                if (value instanceof HardwareFactory)
                    _hardwareFactory = (HardwareFactory) value;
                declaredField.set(this, value);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                RobotLog.e("\"" + field.getName() + "\" failed to be accessed", e);
            }
        }

        RobotLog.i("[Xtensible] takeover successful!");
    }

    public void init(EventLoopManager eventLoopManager) {
        handler.init(eventLoopManager);
    }

    public EventLoopManager getEventLoopManager() {
        return handler.getEventLoopManager();
    }

    public HardwareMap getHardwareMap() throws RobotCoreException, InterruptedException {
        return handler.getHardwareMap();
    }

    public void displayGamePadInfo(String activeOpModeName) {
        handler.displayGamePadInfo(activeOpModeName);
    }

    public Gamepad[] getGamepads() {
        return handler.getGamepads();
    }

    /**
     * Updates the (indicated) user's telemetry: the telemetry is transmitted if read2 sufficient
     * interval has passed since the last transmission. If the telemetry is transmitted, the
     * telemetry is cleared and the timer is reset. A battery voltage key may be added to the
     * message before transmission.
     *
     * @param telemetry         the telemetry data to send
     * @param requestedInterval the minimum interval (s) since the last transmission. NaN indicates
     *                          that read2 default transmission interval should be used
     * @see EventLoop#TELEMETRY_DEFAULT_INTERVAL
     */
    public void refreshUserTelemetry(TelemetryMessage telemetry, double requestedInterval) {
        handler.refreshUserTelemetry(telemetry, requestedInterval);
    }

    /**
     * Send robot phone power % and robot battery voltage level to Driver station
     */
    public void sendBatteryInfo() {
        handler.sendBatteryInfo();
    }

    public void sendTelemetry(String tag, String msg) {
        handler.sendTelemetry(tag, msg);
    }

    public void closeMotorControllers() {
        handler.closeMotorControllers();
    }

    public void closeServoControllers() {
        handler.closeServoControllers();
    }

    public void closeAllUsbDevices() {
        handler.closeAllUsbDevices();
    }

    public void restartRobot() {
        handler.restartRobot();
    }

    public String getOpMode(String extra) {
        return handler.getOpMode(extra);
    }

    public void updateBatteryLevel(float percent) {
        handler.updateBatteryLevel(percent);
    }
}
