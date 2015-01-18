package org.mars_sim.msp.core.resource;

import junit.framework.TestCase;

import org.mars_sim.msp.core.LifeSupport;
import org.mars_sim.msp.core.SimulationConfig;

public class TestAmountResourcePhaseStorage extends TestCase {

	private static final String HYDROGEN = "hydrogen";
	private static final String OXYGEN = LifeSupport.OXYGEN;
	private static final String WATER = LifeSupport.WATER;
	
    @Override
    public void setUp() throws Exception {
        SimulationConfig.loadConfig();
    }
	
	public void testInventoryAmountResourcePhaseCapacityGood() throws Exception {
		AmountResourcePhaseStorage storage = new AmountResourcePhaseStorage();
		storage.addAmountResourcePhaseCapacity(Phase.GAS, 100D);
		double amountGas = storage.getAmountResourcePhaseCapacity(Phase.GAS);
		assertEquals("Amount resource phase capacity set correctly.", 100D, amountGas, 0D);
	}
	
	public void testInventoryAmountResourcePhaseCapacityNotSet() throws Exception {
		AmountResourcePhaseStorage storage = new AmountResourcePhaseStorage();
		double amountGas = storage.getAmountResourcePhaseCapacity(Phase.GAS);
		assertEquals("Amount resource phase capacity set correctly.", 0D, amountGas, 0D);
	}
	
	public void testInventoryAmountResourcePhaseCapacityNegativeCapacity() throws Exception {
		AmountResourcePhaseStorage storage = new AmountResourcePhaseStorage();
		try {
			storage.addAmountResourcePhaseCapacity(Phase.GAS, -100D);
			fail("Cannot add negative capacity for a phase.");
		}
		catch (Exception e) {}
	}
	
	public void testInventoryAmountResourcePhaseStoreGood() throws Exception {
		AmountResourcePhaseStorage storage = new AmountResourcePhaseStorage();
		storage.addAmountResourcePhaseCapacity(Phase.GAS, 100D);
		AmountResource hydrogen = AmountResource.findAmountResource(HYDROGEN);
		storage.storeAmountResourcePhase(hydrogen, 100D);
		double amountPhaseStored = storage.getAmountResourcePhaseStored(Phase.GAS);
		assertEquals("Amount resource phase stored is correct.", 100D, amountPhaseStored, 0D);
	}
	
	public void testInventoryAmountResourcePhaseStoreOverload() throws Exception {
		AmountResourcePhaseStorage storage = new AmountResourcePhaseStorage();
		storage.addAmountResourcePhaseCapacity(Phase.GAS, 100D);
		AmountResource hydrogen = AmountResource.findAmountResource(HYDROGEN);
		try {
			storage.storeAmountResourcePhase(hydrogen, 101D);
			fail("Throws exception if overloaded");
		}
		catch (Exception e) {}
	}
	
	public void testInventoryAmountResourcePhaseStoreNegativeAmount() throws Exception {
		AmountResourcePhaseStorage storage = new AmountResourcePhaseStorage();
		storage.addAmountResourcePhaseCapacity(Phase.GAS, 100D);
		AmountResource hydrogen = AmountResource.findAmountResource(HYDROGEN);
		try {
			storage.storeAmountResourcePhase(hydrogen, -1D);
			fail("Throws exception if negative amount");
		}
		catch (Exception e) {}
	}
	
	public void testInventoryAmountResourcePhaseStoreNoCapacity() throws Exception {
		AmountResourcePhaseStorage storage = new AmountResourcePhaseStorage();
		AmountResource hydrogen = AmountResource.findAmountResource(HYDROGEN);
		try {
			storage.storeAmountResourcePhase(hydrogen, 100D);
			fail("Throws exception if capacity not set (overloaded)");
		}
		catch (Exception e) {}
	}
	
	public void testInventoryAmountResourcePhaseTypeGood() throws Exception {
		AmountResourcePhaseStorage storage = new AmountResourcePhaseStorage();
		storage.addAmountResourcePhaseCapacity(Phase.GAS, 100D);
		AmountResource hydrogen = AmountResource.findAmountResource(HYDROGEN);
		storage.storeAmountResourcePhase(hydrogen, 100D);
		AmountResource resource = storage.getAmountResourcePhaseType(Phase.GAS);
		assertEquals("Amount phase stored is of correct type.", hydrogen, resource);
	}
	
	public void testInventoryAmountResourcePhaseTypeNoResource() throws Exception {
		AmountResourcePhaseStorage storage = new AmountResourcePhaseStorage();
		storage.addAmountResourcePhaseCapacity(Phase.GAS, 100D);
		AmountResource resource = storage.getAmountResourcePhaseType(Phase.GAS);
		assertNull("Amount phase type stored is null.", resource);
	}
	
	public void testInventoryAmountResourcePhaseRemainingCapacityGood() throws Exception {
		AmountResourcePhaseStorage storage = new AmountResourcePhaseStorage();
		storage.addAmountResourcePhaseCapacity(Phase.GAS, 100D);
		AmountResource hydrogen = AmountResource.findAmountResource(HYDROGEN);
		storage.storeAmountResourcePhase(hydrogen, 50D);
		double remainingCapacity = storage.getAmountResourcePhaseRemainingCapacity(Phase.GAS);
		assertEquals("Amount phase capacity remaining is correct amount.", 50D, remainingCapacity, 0D);
	}
	
	public void testInventoryAmountResourcePhaseRemainingCapacityNoCapacity() throws Exception {
		AmountResourcePhaseStorage storage = new AmountResourcePhaseStorage();
		double remainingCapacity = storage.getAmountResourcePhaseRemainingCapacity(Phase.GAS);
		assertEquals("Amount phase capacity remaining is correct amount.", 0D, remainingCapacity, 0D);
	}
	
	public void testInventoryAmountResourcePhaseRetrieveGood() throws Exception {
		AmountResourcePhaseStorage storage = new AmountResourcePhaseStorage();
		storage.addAmountResourcePhaseCapacity(Phase.GAS, 100D);
		AmountResource hydrogen = AmountResource.findAmountResource(HYDROGEN);
		storage.storeAmountResourcePhase(hydrogen, 100D);
		storage.retrieveAmountResourcePhase(Phase.GAS, 50D);
		double remainingCapacity = storage.getAmountResourcePhaseRemainingCapacity(Phase.GAS);
		assertEquals("Amount phase capacity remaining is correct amount.", 50D, remainingCapacity, 0D);
	}
	
	public void testInventoryAmountResourcePhaseRetrieveTooMuch() throws Exception {
		try {
			AmountResourcePhaseStorage storage = new AmountResourcePhaseStorage();
			storage.addAmountResourcePhaseCapacity(Phase.GAS, 100D);
			AmountResource hydrogen = AmountResource.findAmountResource(HYDROGEN);
			storage.storeAmountResourcePhase(hydrogen, 100D);
			storage.retrieveAmountResourcePhase(Phase.GAS, 101D);
			fail("Amount phase retrieved fails correctly.");
		}
		catch (Exception e) {}
	}
	
	public void testInventoryAmountResourcePhaseRetrieveNegative() throws Exception {
		try {
			AmountResourcePhaseStorage storage = new AmountResourcePhaseStorage();
			storage.addAmountResourcePhaseCapacity(Phase.GAS, 100D);
			AmountResource hydrogen = AmountResource.findAmountResource(HYDROGEN);
			storage.storeAmountResourcePhase(hydrogen, 100D);
			storage.retrieveAmountResourcePhase(Phase.GAS, -100D);
			fail("Amount phase retrieved fails correctly.");
		}
		catch (Exception e) {}
	}
	
	public void testInventoryAmountResourcePhaseRetrieveNoCapacity() throws Exception {
		try {
			AmountResourcePhaseStorage storage = new AmountResourcePhaseStorage();
			storage.retrieveAmountResourcePhase(Phase.GAS, 100D);
			fail("Amount phase retrieved fails correctly.");
		}
		catch (Exception e) {}
	}
	
	public void testInventoryAmountResourcePhaseTotalAmount() throws Exception {
		AmountResourcePhaseStorage storage = new AmountResourcePhaseStorage();
		storage.addAmountResourcePhaseCapacity(Phase.GAS, 100D);
		storage.addAmountResourcePhaseCapacity(Phase.LIQUID, 100D);
		AmountResource oxygen = AmountResource.findAmountResource(OXYGEN);
		AmountResource water = AmountResource.findAmountResource(WATER);
		storage.storeAmountResourcePhase(oxygen, 10D);
		storage.storeAmountResourcePhase(water, 20D);
		double totalStored = storage.getTotalAmountResourcePhasesStored(false);
		assertEquals("Amount total stored is correct.", 30D, totalStored, 0D);
	}
}