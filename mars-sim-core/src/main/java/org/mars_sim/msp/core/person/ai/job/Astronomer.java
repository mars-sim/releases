/**
 * Mars Simulation Project
 * Astronomer.java
 * @version 3.03 2012-07-01
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.job;

import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.Skill;
import org.mars_sim.msp.core.person.ai.mission.BuildingConstructionMission;
import org.mars_sim.msp.core.person.ai.mission.BuildingSalvageMission;
import org.mars_sim.msp.core.person.ai.mission.RescueSalvageVehicle;
import org.mars_sim.msp.core.person.ai.mission.TravelToSettlement;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.AstronomicalObservation;
import org.mars_sim.msp.core.structure.building.function.Research;

import java.io.Serializable;
import java.util.Iterator;
import java.util.logging.Logger;

/** 
 * The Astronomer class represents a job for an astronomer.
 */
public class Astronomer extends Job implements Serializable {
    
    private static Logger logger = Logger.getLogger(Astronomer.class.getName());

    /**
     * Constructor
     */
    public Astronomer() {
        // Use Job constructor
        super("Astronomer");
        
        // Add astronomer-related tasks.
        
        // Add astronomer-related missions.
        jobMissionStarts.add(TravelToSettlement.class);
        jobMissionJoins.add(TravelToSettlement.class);  
        jobMissionStarts.add(RescueSalvageVehicle.class);
        jobMissionJoins.add(RescueSalvageVehicle.class);
        jobMissionJoins.add(BuildingConstructionMission.class);
        jobMissionJoins.add(BuildingSalvageMission.class);
    }
    
    @Override
    public double getCapability(Person person) {
        double result = 0D;
        
        int astronomySkill = person.getMind().getSkillManager().getSkillLevel(Skill.ASTRONOMY);
        result = astronomySkill;
        
        NaturalAttributeManager attributes = person.getNaturalAttributeManager();
        int academicAptitude = attributes.getAttribute(NaturalAttributeManager.ACADEMIC_APTITUDE);
        result+= result * ((academicAptitude - 50D) / 100D);
        
        if (person.getPhysicalCondition().hasSeriousMedicalProblems()) result = 0D;
        
        return result;
    }

    @Override
    public double getSettlementNeed(Settlement settlement) {
        double result = 0D;
        
        BuildingManager manager = settlement.getBuildingManager();
        
        // Add (labspace * tech level / 2) for all labs with astronomy specialties.
        Iterator<Building> i = manager.getBuildings(Research.NAME).iterator();
        while (i.hasNext()) {
            Building building = i.next();
            Research lab = (Research) building.getFunction(Research.NAME);
            if (lab.hasSpeciality(Skill.ASTRONOMY)) 
                result += lab.getLaboratorySize() * lab.getTechnologyLevel() / 2D;
        }

        // Add astronomical observatories (observer capacity * tech level * 2).
        Iterator<Building> j = manager.getBuildings(AstronomicalObservation.NAME).iterator();
        while (j.hasNext()) {
            Building building = j.next();
            AstronomicalObservation observatory = (AstronomicalObservation) 
            building.getFunction(AstronomicalObservation.NAME);
            result += observatory.getObservatoryCapacity() * observatory.getTechnologyLevel() * 2D;
        }
        
        return result;  
    }
}