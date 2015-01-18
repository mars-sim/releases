/**
 * Mars Simulation Project
 * BiologyStudyFieldMissionMeta.java
 * @version 3.07 2014-09-18
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.mission.meta;

import java.util.Iterator;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.person.ai.mission.BiologyStudyFieldMission;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.RoverMission;
import org.mars_sim.msp.core.person.ai.mission.VehicleMission;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.science.ScientificStudy;
import org.mars_sim.msp.core.science.ScientificStudyManager;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.vehicle.Rover;

/**
 * A meta mission for the BiologyStudyFieldMission mission.
 */
public class BiologyStudyFieldMissionMeta implements MetaMission {
    
    /** Mission name */
    private static final String NAME = Msg.getString(
            "Mission.description.biologyStudyFieldMission"); //$NON-NLS-1$
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Mission constructInstance(Person person) {
        return new BiologyStudyFieldMission(person);
    }

    @Override
    public double getProbability(Person person) {
        
        double result = 0D;

        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
            Settlement settlement = person.getSettlement();

            // Check if a mission-capable rover is available.
            boolean reservableRover = RoverMission.areVehiclesAvailable(settlement, false);

            // Check if available backup rover.
            boolean backupRover = RoverMission.hasBackupRover(settlement);

            // Check if minimum number of people are available at the settlement.
            // Plus one to hold down the fort.
            boolean minNum = RoverMission.minAvailablePeopleAtSettlement(settlement, 
                    (BiologyStudyFieldMission.MIN_PEOPLE + 1));

            // Check for embarking missions.
            boolean embarkingMissions = VehicleMission.hasEmbarkingMissions(settlement);

            // Check if settlement has enough basic resources for a rover mission.
            boolean hasBasicResources = RoverMission.hasEnoughBasicResources(settlement);

            // Check if min number of EVA suits at settlement.
            boolean enoughSuits = (Mission.getNumberAvailableEVASuitsAtSettlement(person.getSettlement()) 
                    > BiologyStudyFieldMission.MIN_PEOPLE); 

            // Check if starting settlement has minimum amount of methane fuel.
            AmountResource methane = AmountResource.findAmountResource("methane");
            boolean enoughMethane = settlement.getInventory().getAmountResourceStored(methane, false) >= 
                    RoverMission.MIN_STARTING_SETTLEMENT_METHANE;

            if (reservableRover && backupRover && minNum && !embarkingMissions && hasBasicResources 
                    && enoughSuits && enoughMethane) {

                // Get available rover.
                Rover rover = (Rover) RoverMission.getVehicleWithGreatestRange(settlement, false);
                if (rover != null) {

                    ScienceType biology = ScienceType.BIOLOGY;

                    // Add probability for researcher's primary study (if any).
                    ScientificStudyManager studyManager = Simulation.instance().getScientificStudyManager();
                    ScientificStudy primaryStudy = studyManager.getOngoingPrimaryStudy(person);
                    if ((primaryStudy != null) && ScientificStudy.RESEARCH_PHASE.equals(primaryStudy.getPhase())) {
                        if (!primaryStudy.isPrimaryResearchCompleted()) {
                            if (biology == primaryStudy.getScience()) 
                                result += 2D;
                        }
                    }

                    // Add probability for each study researcher is collaborating on.
                    Iterator<ScientificStudy> i = studyManager.getOngoingCollaborativeStudies(person).iterator();
                    while (i.hasNext()) {
                        ScientificStudy collabStudy = i.next();
                        if (ScientificStudy.RESEARCH_PHASE.equals(collabStudy.getPhase())) {
                            if (!collabStudy.isCollaborativeResearchCompleted(person)) {
                                if (biology == collabStudy.getCollaborativeResearchers().get(person))
                                    result += 1D;
                            }
                        }
                    }
                }
            }

            // Crowding modifier
            int crowding = settlement.getCurrentPopulationNum() - settlement.getPopulationCapacity();
            if (crowding > 0) result *= (crowding + 1);  

            // Job modifier.
            Job job = person.getMind().getJob();
            if (job != null) {
                result *= job.getStartMissionProbabilityModifier(BiologyStudyFieldMission.class);
            }
        }

        return result;
    }
}