/**
 * Mars Simulation Project
 * TransportRoverUIProxy.java
 * @version 2.74 2002-03-15
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.standard;

import org.mars_sim.msp.simulation.*;
import org.mars_sim.msp.simulation.vehicle.*;
import java.awt.*;
import javax.swing.*;

/**
 * User interface proxy for an transport rover.
 */
public class TransportRoverUIProxy extends GroundVehicleUIProxy {

    // Data members
    private TransportRover rover;

    /** Constructs an TransportRoverUIProxy object 
     *  @param TransportRover the transport rover 
     *  @param proxyManager the unit UI proxy manager
     */
    public TransportRoverUIProxy(TransportRover rover,
            UIProxyManager proxyManager) {
        super(rover, proxyManager);

        this.rover = rover;
        buttonIcon = new ImageIcon("images/TransportRoverIcon.gif");
    }

    /** Returns dialog window for transport rover. 
     *  @return dialog window for transport rover 
     */
    public UnitDialog getUnitDialog(MainDesktopPane desktop) {
        if (unitDialog == null) unitDialog = new TransportRoverDialog(desktop, this);
        return unitDialog;
    }
}
