/**
 * Mars Simulation Project
 * Engineer.java
 * @version 3.06 2014-05-09
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.job;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.mission.BuildingConstructionMission;
import org.mars_sim.msp.core.person.ai.mission.BuildingSalvageMission;
import org.mars_sim.msp.core.person.ai.mission.EmergencySupplyMission;
import org.mars_sim.msp.core.person.ai.mission.RescueSalvageVehicle;
import org.mars_sim.msp.core.person.ai.mission.TravelToSettlement;
import org.mars_sim.msp.core.person.ai.task.DigLocalIce;
import org.mars_sim.msp.core.person.ai.task.DigLocalRegolith;
import org.mars_sim.msp.core.person.ai.task.ManufactureGood;
import org.mars_sim.msp.core.person.ai.task.SalvageGood;
import org.mars_sim.msp.core.person.ai.task.UnloadVehicleEVA;
import org.mars_sim.msp.core.person.ai.task.UnloadVehicleGarage;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.structure.building.function.Manufacture;

/** 
 * The Engineer class represents an engineer job focusing on repair and maintenance of buildings and 
 * vehicles.
 */
public class Engineer
extends Job
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	//	private static Logger logger = Logger.getLogger(Engineer.class.getName());

	/** Constructor. */
	public Engineer() {
		// Use Job constructor
		super(Engineer.class);

		// Add engineer-related tasks.
		jobTasks.add(UnloadVehicleEVA.class);
		jobTasks.add(UnloadVehicleGarage.class);
		jobTasks.add(ManufactureGood.class);
		jobTasks.add(DigLocalRegolith.class);
		jobTasks.add(DigLocalIce.class);
		jobTasks.add(SalvageGood.class);

		// Add engineer-related missions.
		jobMissionStarts.add(TravelToSettlement.class);
		jobMissionJoins.add(TravelToSettlement.class);	
		jobMissionStarts.add(RescueSalvageVehicle.class);
		jobMissionJoins.add(RescueSalvageVehicle.class);
		jobMissionJoins.add(BuildingConstructionMission.class);
		jobMissionJoins.add(BuildingSalvageMission.class);
		jobMissionStarts.add(EmergencySupplyMission.class);
		jobMissionJoins.add(EmergencySupplyMission.class);
	}

	/**
	 * Gets a person's capability to perform this job.
	 * @param person the person to check.
	 * @return capability (min 0.0).
	 */
	public double getCapability(Person person) {

		double result = 0D;

		int materialsScienceSkill = person.getMind().getSkillManager().getSkillLevel(SkillType.MATERIALS_SCIENCE);
		result = materialsScienceSkill;

		NaturalAttributeManager attributes = person.getNaturalAttributeManager();
		int academicAptitude = attributes.getAttribute(NaturalAttribute.ACADEMIC_APTITUDE);
		int experienceAptitude = attributes.getAttribute(NaturalAttribute.EXPERIENCE_APTITUDE);
		double averageAptitude = (academicAptitude + experienceAptitude) / 2D;
		result+= result * ((averageAptitude - 50D) / 100D);

		if (person.getPhysicalCondition().hasSeriousMedicalProblems()) result = 0D;

		return result;
	}

	/**
	 * Gets the base settlement need for this job.
	 * @param settlement the settlement in need.
	 * @return the base need >= 0
	 */
	public double getSettlementNeed(Settlement settlement) {

		double result = 0D;

		// Add (tech level * process number / 2) for all manufacture buildings.
		List<Building> manufactureBuildings = settlement.getBuildingManager().getBuildings(BuildingFunction.MANUFACTURE);
		Iterator<Building> i = manufactureBuildings.iterator();
		while (i.hasNext()) {
			Building building = i.next();
			Manufacture workshop = (Manufacture) building.getFunction(BuildingFunction.MANUFACTURE);
			result += workshop.getTechLevel() * workshop.getConcurrentProcesses() / 2D;
		}

		return result;	
	}
}