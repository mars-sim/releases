/**
 * Mars Simulation Project
 * Architect.java
 * @version 3.06 2014-05-09
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.job;

import java.io.Serializable;

import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.mission.BuildingConstructionMission;
import org.mars_sim.msp.core.person.ai.mission.BuildingSalvageMission;
import org.mars_sim.msp.core.person.ai.mission.EmergencySupplyMission;
import org.mars_sim.msp.core.person.ai.mission.RescueSalvageVehicle;
import org.mars_sim.msp.core.person.ai.mission.TravelToSettlement;
import org.mars_sim.msp.core.person.ai.task.ConstructBuilding;
import org.mars_sim.msp.core.person.ai.task.ManufactureConstructionMaterials;
import org.mars_sim.msp.core.person.ai.task.SalvageBuilding;
import org.mars_sim.msp.core.structure.Settlement;

/** 
 * The Architect class represents an architect job focusing on construction of buildings, settlement 
 * and other structures.
 */
public class Architect
extends Job
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	//private static Logger logger = Logger.getLogger(Architect.class.getName());

	/** Constructor. */
	public Architect() {
		// Use Job constructor.
		super(Architect.class);

		// Add architect-related tasks.
		jobTasks.add(ManufactureConstructionMaterials.class);
		jobTasks.add(ConstructBuilding.class);
		jobTasks.add(SalvageBuilding.class);

		// Add architect-related missions.
		jobMissionStarts.add(BuildingConstructionMission.class);
		jobMissionJoins.add(BuildingConstructionMission.class);
		jobMissionStarts.add(BuildingSalvageMission.class);
		jobMissionJoins.add(BuildingSalvageMission.class);
		jobMissionStarts.add(TravelToSettlement.class);
		jobMissionJoins.add(TravelToSettlement.class);  
		jobMissionStarts.add(RescueSalvageVehicle.class);
		jobMissionJoins.add(RescueSalvageVehicle.class);
		jobMissionStarts.add(EmergencySupplyMission.class);
		jobMissionJoins.add(EmergencySupplyMission.class);
	}

	@Override
	public double getCapability(Person person) {

		double result = 0D;

		int constructionSkill = person.getMind().getSkillManager().getSkillLevel(SkillType.CONSTRUCTION);
		result = constructionSkill;

		NaturalAttributeManager attributes = person.getNaturalAttributeManager();
		int academicAptitude = attributes.getAttribute(NaturalAttribute.ACADEMIC_APTITUDE);
		int experienceAptitude = attributes.getAttribute(NaturalAttribute.EXPERIENCE_APTITUDE);
		double averageAptitude = (academicAptitude + experienceAptitude) / 2D;
		result+= result * ((averageAptitude - 50D) / 100D);

		if (person.getPhysicalCondition().hasSeriousMedicalProblems()) result = 0D;

		return result;
	}

	@Override
	public double getSettlementNeed(Settlement settlement) {
		double result = 0D;
		// Add number of buildings currently at settlement.
		result += settlement.getBuildingManager().getBuildingNum() / 10D;
		return result;  
	}
}