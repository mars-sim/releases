/**
 * Mars Simulation Project
 * InfoDialog.java
 * @version 3.06 2014-01-29
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.tool.mission.edit;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.mission.CollectResourcesMission;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.RoverMission;
import org.mars_sim.msp.core.person.ai.mission.TravelMission;
import org.mars_sim.msp.core.person.ai.mission.VehicleMission;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.ui.swing.JComboBoxMW;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;

/**
 * The mission info panel for the edit mission dialog.
 */
public class InfoPanel extends JPanel {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	/** action text. */
	final static String ACTION_NONE = "None";
	/** action text. */
	final static String ACTION_CONTINUE = "End EVA and Continue to Next Site";
	/** action text. */
	final static String ACTION_HOME = "Return to Home Settlement and End Mission";
	/** action text. */
	final static String ACTION_NEAREST = "Go to Nearest Settlement and End Mission";
	
	// Data members.
	Mission mission;
	Dialog parent;
	JTextField descriptionField;
	JComboBoxMW<?> actionDropDown;
	DefaultListModel<Person> memberListModel;
	JList<Person> memberList;
	JButton addMembersButton;
	JButton removeMembersButton;
	
	/**
	 * Constructor.
	 * @param mission {@link Mission} the mission to edit.
	 * @param parent {@link Dialog} the parent dialog.
	 */
	public InfoPanel(Mission mission, Dialog parent) {
		
		// Data members
		this.mission = mission;
		this.parent = parent;
		
		// Sets the layout.
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		// Sets the border.
		setBorder(new MarsPanelBorder());
		
		// Create the description panel.
		JPanel descriptionPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
		descriptionPane.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(descriptionPane);
		
		// Create the description label.
		JLabel descriptionLabel = new JLabel("Description: ");
		descriptionPane.add(descriptionLabel);
		
		// Create the description text field.
		descriptionField = new JTextField(mission.getDescription(), 20);
		descriptionPane.add(descriptionField);
		
		// Create the action panel.
		JPanel actionPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
		actionPane.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(actionPane);
		
		// Create the action label.
		JLabel actionLabel = new JLabel("Action: ");
		actionPane.add(actionLabel);
		
		// Create the action drop down box.
		actionDropDown = new JComboBoxMW<String>(getActions(mission));
		actionDropDown.setEnabled(actionDropDown.getItemCount() > 1);
		actionPane.add(actionDropDown);
		
		// Create the members panel.
		JPanel membersPane = new JPanel(new BorderLayout());
		membersPane.setBorder(MainDesktopPane.newEmptyBorder());
		membersPane.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(membersPane);
		
		// Create the members label.
		JLabel membersLabel = new JLabel("Members: ");
		membersLabel.setVerticalAlignment(JLabel.TOP);
		membersPane.add(membersLabel, BorderLayout.WEST);
		
		// Create the member list panel.
		JPanel memberListPane = new JPanel(new BorderLayout(0, 0));
		membersPane.add(memberListPane, BorderLayout.CENTER);
		
        // Create scroll panel for member list.
        JScrollPane memberScrollPane = new JScrollPane();
        memberScrollPane.setPreferredSize(new Dimension(100, 100));
        memberListPane.add(memberScrollPane, BorderLayout.CENTER);
        
        // Create member list model
        memberListModel = new DefaultListModel<Person>();
        Iterator<Person> i = mission.getPeople().iterator();
        while (i.hasNext()) memberListModel.addElement(i.next());
        
        // Create member list
        memberList = new JList<Person>(memberListModel);
        memberList.addListSelectionListener(
        		new ListSelectionListener() {
        			@Override
        			public void valueChanged(ListSelectionEvent e) {
        				// Enable remove members button if there are members in the list.
        				removeMembersButton.setEnabled(memberList.getSelectedValues().length > 0);
        			}
        		}
        	);
        memberScrollPane.setViewportView(memberList);
        
        // Create the member button panel.
        JPanel memberButtonPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
        memberListPane.add(memberButtonPane, BorderLayout.SOUTH);
        
        // Create the add members button.
        addMembersButton = new JButton("Add Members");
        addMembersButton.setEnabled(canAddMembers());
        addMembersButton.addActionListener(
        		new ActionListener() {
        			public void actionPerformed(ActionEvent e) {
        				// Open the add member dialog.
        				addMembers();
        			}
        		});
        memberButtonPane.add(addMembersButton);
        
        // Create the remove members button.
        removeMembersButton = new JButton("Remove Members");
        removeMembersButton.setEnabled(false);
        removeMembersButton.addActionListener(
        		new ActionListener() {
        			public void actionPerformed(ActionEvent e) {
        				// Remove selected members from the list.
        				removeMembers();
        			}
        		});
        memberButtonPane.add(removeMembersButton);
	}
	
	/**
	 * Checks if members can be added to the mission.
	 * @return true if members can be added.
	 */
	private boolean canAddMembers() {
		boolean roomInMission = (memberListModel.size() < mission.getMissionCapacity());
		boolean availablePeople = (getAvailablePeople().size() > 0);
		return (roomInMission && availablePeople);
	}
	
	/**
	 * Open the add members dialog.
	 */
	private void addMembers() {
		new AddMembersDialog(parent, mission, memberListModel, getAvailablePeople());
		addMembersButton.setEnabled(canAddMembers());
	}
	
	/**
	 * Remove selected members from the list.
	 */
	private void removeMembers() {
		int[] selectedIndexes = memberList.getSelectedIndices();
		Object[] selectedPeople = new Object[selectedIndexes.length];
		for (int x = 0; x < selectedIndexes.length; x++) 
			selectedPeople[x] = memberListModel.elementAt(selectedIndexes[x]);
        for (Object aSelectedPeople : selectedPeople) memberListModel.removeElement(aSelectedPeople);
		addMembersButton.setEnabled(canAddMembers());
	}
	
	/**
	 * Gets a vector of possible actions for the mission.
	 * @param mission {@link Mission} the mission 
	 * @return vector of actions.
	 */
	private Vector<String> getActions(Mission mission) {
		Vector<String> actions = new Vector<String>();
		actions.add(ACTION_NONE);
		
		String phase = mission.getPhase();
		
		// Check if continue action can be added.
		if (phase.equals(CollectResourcesMission.COLLECT_RESOURCES)) {
			CollectResourcesMission collectResourcesMission = (CollectResourcesMission) mission;
			if (collectResourcesMission.getNumCollectionSites() > collectResourcesMission.getNumCollectionSitesVisited())
				actions.add(ACTION_CONTINUE);
		}
		
		// Check if go home action can be added.
		if (mission instanceof TravelMission) {
			TravelMission travelMission = (TravelMission) mission;
			int nextNavpointIndex = travelMission.getNextNavpointIndex();
			if ((nextNavpointIndex > -1) && (nextNavpointIndex < (travelMission.getNumberOfNavpoints() - 1))) {
				if (!mission.getPhase().equals(VehicleMission.EMBARKING))
					actions.add(ACTION_HOME);
			}
		}
		
		// Check if nearest settlement action can be added.
		if (mission instanceof VehicleMission) {
			VehicleMission vehicleMission = (VehicleMission) mission;
			try {
				Settlement closestSettlement = vehicleMission.findClosestSettlement();
				if ((closestSettlement != null) && !closestSettlement.equals(vehicleMission.getAssociatedSettlement())) {
					if (!mission.getPhase().equals(VehicleMission.EMBARKING))
						actions.add(ACTION_NEAREST);
				}
			}
			catch (Exception e) {}
		}
		
		return actions;
	}
	
	/**
	 * Gets a collection of people available to be added to the mission.
	 * @return collection of available people.
	 */
	private Collection<Person> getAvailablePeople() {
		Collection<Person> result = new ConcurrentLinkedQueue<Person>();
	
		// Add people in the settlement or rover.
		if (mission instanceof RoverMission) {
			Rover rover = ((RoverMission) mission).getRover();
			String phase = mission.getPhase();
			Collection<Person> peopleAtLocation = null;
			if (rover != null) {
				if (phase.equals(RoverMission.EMBARKING) || 
						phase.equals(RoverMission.DISEMBARKING)) {
					// Add available people at the local settlement.
					Settlement settlement = rover.getSettlement();
					if (settlement != null) peopleAtLocation = settlement.getInhabitants();
				}
				else {
					// Add available people in the rover.
					peopleAtLocation = rover.getCrew();
				}
			}
			
			// Add people.
			Iterator<Person> i = peopleAtLocation.iterator();
			while (i.hasNext()) {
				Person person = i.next();
				if (!memberListModel.contains(person)) result.add(person);
			}
		}
		
		// Add people who are outside at this location as well.
		try {
			Coordinates missionLocation = mission.getCurrentMissionLocation();
			Iterator<Person> i = Simulation.instance().getUnitManager().getPeople().iterator();
			while (i.hasNext()) {
				Person person = i.next();
				if (person.getLocationSituation().equals(Person.OUTSIDE)) {
					if (person.getCoordinates().equals(missionLocation)) {
						if (!memberListModel.contains(person)) result.add(person);
					}
				}
			}
		}
		catch (Exception e) {}
		
		return result;
	}
}