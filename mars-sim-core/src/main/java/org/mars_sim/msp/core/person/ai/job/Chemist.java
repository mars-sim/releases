/**
 * Mars Simulation Project
 * Chemist.java
 * @version 3.05 2013-08-16
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.job;

import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.Skill;
import org.mars_sim.msp.core.person.ai.mission.BuildingConstructionMission;
import org.mars_sim.msp.core.person.ai.mission.BuildingSalvageMission;
import org.mars_sim.msp.core.person.ai.mission.EmergencySupplyMission;
import org.mars_sim.msp.core.person.ai.mission.RescueSalvageVehicle;
import org.mars_sim.msp.core.person.ai.mission.TravelToSettlement;
import org.mars_sim.msp.core.person.ai.task.AssistScientificStudyResearcher;
import org.mars_sim.msp.core.person.ai.task.CompileScientificStudyResults;
import org.mars_sim.msp.core.person.ai.task.DigLocalIce;
import org.mars_sim.msp.core.person.ai.task.DigLocalRegolith;
import org.mars_sim.msp.core.person.ai.task.InviteStudyCollaborator;
import org.mars_sim.msp.core.person.ai.task.PeerReviewStudyPaper;
import org.mars_sim.msp.core.person.ai.task.PerformLaboratoryExperiment;
import org.mars_sim.msp.core.person.ai.task.PerformLaboratoryResearch;
import org.mars_sim.msp.core.person.ai.task.ProposeScientificStudy;
import org.mars_sim.msp.core.person.ai.task.ResearchScientificStudy;
import org.mars_sim.msp.core.person.ai.task.RespondToStudyInvitation;
import org.mars_sim.msp.core.person.ai.task.StudyFieldSamples;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.Research;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/** 
 * The Chemist class represents a job for a chemist.
 */
public class Chemist extends Job implements Serializable {
    
    private static Logger logger = Logger.getLogger(Chemist.class.getName());

    /**
     * Constructor
     */
    public Chemist() {
        // Use Job constructor
        super("Chemist");
        
        // Add chemist-related tasks.
        jobTasks.add(AssistScientificStudyResearcher.class);
        jobTasks.add(CompileScientificStudyResults.class);
        jobTasks.add(DigLocalRegolith.class);
        jobTasks.add(DigLocalIce.class);
        jobTasks.add(InviteStudyCollaborator.class);
        jobTasks.add(PeerReviewStudyPaper.class);
        jobTasks.add(PerformLaboratoryExperiment.class);
        jobTasks.add(PerformLaboratoryResearch.class);
        jobTasks.add(ProposeScientificStudy.class);
        jobTasks.add(ResearchScientificStudy.class);
        jobTasks.add(RespondToStudyInvitation.class);
        jobTasks.add(StudyFieldSamples.class);
        
        // Add chemist-related missions.
        jobMissionStarts.add(TravelToSettlement.class);
        jobMissionJoins.add(TravelToSettlement.class);  
        jobMissionStarts.add(RescueSalvageVehicle.class);
        jobMissionJoins.add(RescueSalvageVehicle.class);
        jobMissionJoins.add(BuildingConstructionMission.class);
        jobMissionJoins.add(BuildingSalvageMission.class);
        jobMissionStarts.add(EmergencySupplyMission.class);
        jobMissionJoins.add(EmergencySupplyMission.class);
    }
    
    @Override
    public double getCapability(Person person) {
        double result = 0D;
        
        int chemistrySkill = person.getMind().getSkillManager().getSkillLevel(Skill.CHEMISTRY);
        result = chemistrySkill;
        
        NaturalAttributeManager attributes = person.getNaturalAttributeManager();
        int academicAptitude = attributes.getAttribute(NaturalAttributeManager.ACADEMIC_APTITUDE);
        result+= result * ((academicAptitude - 50D) / 100D);
        
        if (person.getPhysicalCondition().hasSeriousMedicalProblems()) result = 0D;
        
        return result;
    }

    @Override
    public double getSettlementNeed(Settlement settlement) {
        double result = 0D;
        
        // Add (labspace * tech level / 2) for all labs with chemistry specialities.
        List<Building> laboratoryBuildings = settlement.getBuildingManager().getBuildings(Research.NAME);
        Iterator<Building> i = laboratoryBuildings.iterator();
        while (i.hasNext()) {
            Building building = i.next();
            Research lab = (Research) building.getFunction(Research.NAME);
            if (lab.hasSpeciality(Skill.CHEMISTRY)) 
                result += (lab.getLaboratorySize() * lab.getTechnologyLevel() / 2D);
        }
        
        return result;  
    }
}