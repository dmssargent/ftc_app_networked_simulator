Things to work on

- We are at the stage were we need to get some sensors working on the V-Rep simulator to be able to try some autonomous programs. V-rep has a follow line example with three light sensors that might be a good starting point.  One project would be to add one of those sensors to the example robot and figure out how to read the value back into the PC java program and then pass it back to the Android.

- Take a look at how we are sending packets from the Android to the PC.  We have replaced the FTDI USB driver and are using UDP packets.  One initial problem was that when packets were lost the FTC code didn't handle it very well (10 errors and they give up).  We decided to loop back the initial handshaking packets so they didn't run the risk of getting lost.  Then we respected the 100ms timeout that the FTC code requested when it issued a FTDI "read()" call, then if we get a timeout we just pass the previously received packet back to them instead of waiting for a potentially lost packet.  This seems to work but we need to look at what happens when a packet is not lost but is just late.  I think we might get backed up by one packet in the queue and never catch up.  Also, we made use of transmit and receive threads that feed queues in the FTDI driver area.  Not sure if we really need them but it seemed like they were needed at the time.

- In the GUI code we were not able to marshal all of the classes we wanted to.  In partitular, the array of SimData objects in the LegacyBrickSimulator class would not marshal to xml. We had to keep to seperate 6 element arrays to hold the legacy module port types and names.  Should have been able to put the data in the individual SimData objects. We use the JAXB to serialize the objects to disk and also to send the current configuration to the Android when it is querying the USB bus for devices.

- In the new GUI code we are working on how to process packets received from all of the different modules (legacy, motor, usb, sensor) and get the needed data to the V-REP simulator.   We have made a thread for each different module that communicates between the phone and the PC.  We also have a V-REP thread.  Before, we used a simple queue to pass the motor values to the V-REP thread but we are worried about that case where it gets behind and we need to flush the queue.  Now we are looking at just using synchronized blocks in each thread and have the V-REP thread read from them in a round-robin fashion.  New values would just be overwritten when they come in.  Need to do the same thing for outgoing reads, like from a light sensor.

- We have figured out the packet structure for the two types of motor controllers but we don't have it written down.  The legacy controllers use a 208 byte packet that has a 16 byte section followed by six 32 byte sections (one for each port).  The 32 byte sections start with four bytes that are normally (1,2,40h,14h) in hex (1=mode, 2=i2c address, 40=i2c register start, 14=i2c number of bytes to read/write)  It would help to look at the document "328_HiTechnic-Motor-Controller-Specification-v1.4.pdf" in the register section on page 5 to understand the registers. They are a direct mapping. For example, using a motor contoller on the 2nd port, the motor power values are written to i2c bytes 45H and 46H. Our buffer starts at 40h.  If you subtract the 40H starting register offset, we need to get the motor power from bytes 5 and 6, which are in the 1st 32 byte section after the initial 16 bytes so 16+32+4+5/6 for a tetrix motor connected to the first port.  Sorry, need a document.  There are more details about where to read other sensors.  The 1st 16 bytes also correspond to the six ports for simple sensors.
The USB motor controller just uses a 94 byte packet the same size as the Tetrix motor controller documented on page 5. 