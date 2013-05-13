package org.mars_sim.msp.core;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * JUnit test suite
 */
public class AllJUnitTests extends TestCase {

	private static final Class thisClass = AllJUnitTests.class;

	/**
	 * Run all JUnit tests.
	 */
	public static void main(String[] args) {
		TestRunner.run(thisClass);
	}
	
	/**
	 * Collection of external test suites to be included in current testing.
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(thisClass);
		
		suite.addTestSuite(org.mars_sim.msp.core.TestCoordinates.class);
		suite.addTestSuite(org.mars_sim.msp.core.TestInventory.class);
		suite.addTestSuite(org.mars_sim.msp.core.equipment.JUnitTests.class);
		suite.addTestSuite(org.mars_sim.msp.core.events.JUnitTests.class);
		suite.addTestSuite(org.mars_sim.msp.core.malfunction.TestMalfunctionManager.class);
		suite.addTestSuite(org.mars_sim.msp.core.person.JUnitTests.class);
		suite.addTestSuite(org.mars_sim.msp.core.person.ai.JUnitTests.class);
		suite.addTestSuite(org.mars_sim.msp.core.person.ai.mission.JUnitTests.class);
		suite.addTest(org.mars_sim.msp.core.person.ai.task.JUnitTests.suite());
		suite.addTestSuite(org.mars_sim.msp.core.person.medical.JUnitTests.class);
		suite.addTestSuite(org.mars_sim.msp.core.resource.TestAmountResourceStorage.class);
		suite.addTestSuite(org.mars_sim.msp.core.resource.TestAmountResourcePhaseStorage.class);
		suite.addTestSuite(org.mars_sim.msp.core.resource.TestAmountResourceTypeStorage.class);
		suite.addTestSuite(org.mars_sim.msp.core.resource.TestItemResource.class);
		suite.addTestSuite(org.mars_sim.msp.core.structure.JUnitTests.class);
		suite.addTestSuite(org.mars_sim.msp.core.structure.building.JUnitTests.class);
        suite.addTest(org.mars_sim.msp.core.structure.construction.JUnitTests.suite());
        suite.addTestSuite(org.mars_sim.msp.core.structure.goods.TestGoods.class);
		suite.addTestSuite(org.mars_sim.msp.core.structure.goods.TestCreditManager.class);
		suite.addTestSuite(org.mars_sim.msp.core.vehicle.JUnitTests.class);
        
		return suite;
	}
	
	/**
	 * Every JUnit test suite needs at least one test.This one obviously does nothing.
	 * Any others begining with "test..." will be automatically included as well.
	 */
	public void testNothing() {
	}
}
