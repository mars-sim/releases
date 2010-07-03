/**
 * Mars Simulation Project
 * Biologist.java
 * @version 2.87 2009-06-18
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.job;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Lab;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.Skill;
import org.mars_sim.msp.core.person.ai.mission.BuildingConstructionMission;
import org.mars_sim.msp.core.person.ai.mission.BuildingSalvageMission;
import org.mars_sim.msp.core.person.ai.mission.Exploration;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.MissionManager;
import org.mars_sim.msp.core.person.ai.mission.RescueSalvageVehicle;
import org.mars_sim.msp.core.person.ai.mission.RoverMission;
import org.mars_sim.msp.core.person.ai.mission.TravelToSettlement;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingException;
import org.mars_sim.msp.core.structure.building.function.Research;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.Vehicle;

/** 
 * The Biologist class represents a job for a biologist.
 */
public class Biologist extends Job implements Serializable {

    private static String CLASS_NAME = "org.mars_sim.msp.simulation.person.ai.job.Biologist";
    
    private static Logger logger = Logger.getLogger(CLASS_NAME);

    /**
     * Constructor
     */
    public Biologist() {
        // Use Job constructor
        super("Biologist");
        
        // Add biologist-related tasks.
        
        // Add biologist-related missions.
        jobMissionJoins.add(Exploration.class);
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
        
        int biologySkill = person.getMind().getSkillManager().getSkillLevel(Skill.BIOLOGY);
        result = biologySkill;
        
        NaturalAttributeManager attributes = person.getNaturalAttributeManager();
        int academicAptitude = attributes.getAttribute(NaturalAttributeManager.ACADEMIC_APTITUDE);
        result+= result * ((academicAptitude - 50D) / 100D);
        
        if (person.getPhysicalCondition().hasSeriousMedicalProblems()) result = 0D;
        
        return result;
    }

    @Override
    public double getSettlementNeed(Settlement settlement) {
        double result = 0D;
        
        // Add (labspace * tech level / 2) for all labs with biology specialities.
        List<Building> laboratoryBuildings = settlement.getBuildingManager().getBuildings(Research.NAME);
        Iterator<Building> i = laboratoryBuildings.iterator();
        while (i.hasNext()) {
            Building building = i.next();
            try {
                Research lab = (Research) building.getFunction(Research.NAME);
                if (lab.hasSpeciality(Skill.BIOLOGY)) 
                    result += (lab.getLaboratorySize() * lab.getTechnologyLevel() / 2D);
            }
            catch (BuildingException e) {
                logger.log(Level.SEVERE,"Issues in getSettlementNeeded", e);
            }
        }

        // Add (labspace * tech level / 2) for all parked rover labs with biology specialities.
        Iterator<Vehicle> j = settlement.getParkedVehicles().iterator();
        while (j.hasNext()) {
            Vehicle vehicle = j.next();
            if (vehicle instanceof Rover) {
                Rover rover = (Rover) vehicle;
                if (rover.hasLab()) {
                    Lab lab = rover.getLab();
                    if (lab.hasSpeciality(Skill.BIOLOGY))
                        result += (lab.getLaboratorySize() * lab.getTechnologyLevel() / 2D);
                }
            }
        }
        
        // Add (labspace * tech level / 2) for all labs with biology specialities in rovers out on missions.
        MissionManager missionManager = Simulation.instance().getMissionManager();
        Iterator<Mission> k = missionManager.getMissionsForSettlement(settlement).iterator();
        while (k.hasNext()) {
            Mission mission = k.next();
            if (mission instanceof RoverMission) {
                Rover rover = ((RoverMission) mission).getRover();
                if ((rover != null) && !settlement.getParkedVehicles().contains(rover)) {
                    if (rover.hasLab()) {
                        Lab lab = rover.getLab();
                        if (lab.hasSpeciality(Skill.BIOLOGY))
                            result += (lab.getLaboratorySize() * lab.getTechnologyLevel() / 2D);
                    }
                }
            }
        }
        
        result *= 5D;
        
        return result;  
    }
}