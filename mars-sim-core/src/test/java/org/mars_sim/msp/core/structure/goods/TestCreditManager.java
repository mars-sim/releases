package org.mars_sim.msp.core.structure.goods;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import junit.framework.TestCase;

import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.structure.MockSettlement;
import org.mars_sim.msp.core.structure.Settlement;

public class TestCreditManager extends TestCase {

    @Override
    public void setUp() throws Exception {
        SimulationConfig.loadConfig();
    }

    public void testSetCredit() {
			Collection<Settlement> settlements = new ConcurrentLinkedQueue<Settlement>();
			Settlement settlement1 = new MockSettlement();
			settlements.add(settlement1);
			Settlement settlement2 = new MockSettlement();
			settlements.add(settlement2);
			CreditManager manager = new CreditManager(settlements);
			
			manager.setCredit(settlement1, settlement2, 100D);
			assertEquals( 100D, manager.getCredit(settlement1, settlement2));
			
			manager.setCredit(settlement1, settlement2, -100D);
			assertEquals( -100D, manager.getCredit(settlement1, settlement2));
			
			manager.setCredit(settlement2, settlement1, 100D);
			assertEquals( -100D, manager.getCredit(settlement1, settlement2));
	}
}