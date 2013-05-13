package org.mars_sim.msp.core.vehicle;

import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.structure.Settlement;

public class MockVehicle extends Vehicle {

	public MockVehicle(Settlement settlement) throws Exception {
		// Use Vehicle constructor
		super("Mock Vehicle", "Mock Vehicle", settlement, 10D, 100D, 1D);
	}
	
	public boolean isAppropriateOperator(VehicleOperator operator) {
		return false;
	}

	public AmountResource getFuelType() {
		return null;
	}
	
    @Override
    public void determinedSettlementParkedLocationAndFacing() {
    	// Do nothing
    }
}