/*
 * Copyright © 2016 David Sargent
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM,OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ftccommunity.simulator;

import android.app.Activity;
import android.content.Context;

import com.qualcomm.ftccommon.FtcEventLoop;
import com.qualcomm.ftccommon.ProgrammingModeController;
import com.qualcomm.ftccommon.UpdateUI;
import com.qualcomm.hardware.HardwareFactory;
import com.qualcomm.robotcore.eventloop.EventLoopManager;
import com.qualcomm.robotcore.eventloop.opmode.OpModeRegister;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.network.CallbackResult;

import java.lang.reflect.Field;
import java.util.concurrent.Semaphore;

/**
 * Override for {@link FtcEventLoop}. This fixes some issues present in the standard OpMode Event
 * Loop, and allows for the Hardware factory takeover
 *
 * @author David Sargent, Bob Atkinson
 * @version 1
 */
public class SimulatorEventLoop extends FtcEventLoop {
    private Semaphore semaphore = new Semaphore(0);

    public SimulatorEventLoop(HardwareFactory hardwareFactory, OpModeRegister register,
                              UpdateUI.Callback callback, Context robotControllerContext,
                              ProgrammingModeController programmingModeController) {
        super(hardwareFactory, register, callback, (Activity) robotControllerContext, programmingModeController);
        // Hopefully unnecessary
        //ftcEventLoopHandler = new XtensibleEventLoopHandler(ftcEventLoopHandler);
        for (Field field : ftcEventLoopHandler.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                final Object o = field.get(ftcEventLoopHandler);
                if (!(o instanceof HardwareFactory)) return;
                HardwareFactory simulatorHwFactory = new SimulatorHardwareFactory(robotControllerContext);
                field.set(ftcEventLoopHandler, simulatorHwFactory);
                RobotLog.i("[SIM] Takeover successful of HardwareFactory");
            } catch (IllegalAccessException e) {
                RobotLog.e("[SIM] Error on takeover of HardwareFactory", e);
            }
        }
        semaphore = new Semaphore(0);
    }

    @Override
    public void init(EventLoopManager eventLoopManager) throws
            RobotCoreException, InterruptedException {
        super.init(eventLoopManager);
        semaphore.release();
    }

    @Override
    public CallbackResult processCommand(Command command) throws RobotCoreException {
        try {
            semaphore.acquire();
            CallbackResult result = super.processCommand(command);
            semaphore.release();
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

}
