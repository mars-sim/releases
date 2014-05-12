/**
 * Mars Simulation Project
 * TabPanelActivity.java
 * @version 3.06 2014-01-29
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.unit_window.person;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.Mind;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.person.ai.job.JobManager;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.task.TaskManager;
import org.mars_sim.msp.core.person.medical.DeathInfo;
import org.mars_sim.msp.ui.swing.ImageLoader;
import org.mars_sim.msp.ui.swing.JComboBoxMW;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.tool.mission.MissionWindow;
import org.mars_sim.msp.ui.swing.tool.monitor.PersonTableModel;
import org.mars_sim.msp.ui.swing.unit_window.TabPanel;


/** 
 * The TabPanelActivity is a tab panel for a person's current activities
 * and to change their job.
 */
public class TabPanelActivity
extends TabPanel
implements ActionListener {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	private JTextArea taskTextArea;
	private JTextArea taskPhaseTextArea;
	private JTextArea missionTextArea;
	private JTextArea missionPhaseTextArea;
	private JLabel jobLabel;
	private JComboBoxMW<?> jobComboBox;
	private JButton monitorButton;
	private JButton missionButton;

	/** data cache */
	private String jobCache = ""; //$NON-NLS-1$
	/** data cache */
	private String taskCache = ""; //$NON-NLS-1$
	/** data cache */
	private String taskPhaseCache = ""; //$NON-NLS-1$
	/** data cache */
	private String missionCache = ""; //$NON-NLS-1$
	/** data cache */
	private String missionPhaseCache = ""; //$NON-NLS-1$

	/**
	 * Constructor.
	 * @param unit {@link Unit} the unit to display.
	 * @param desktop {@link MainDesktopPane} the main desktop.
	 */
	public TabPanelActivity(Unit unit, MainDesktopPane desktop) { 
		// Use the TabPanel constructor
		super(
			Msg.getString("TabPanelActivity.title"), //$NON-NLS-1$
			null,
			Msg.getString("TabPanelActivity.tooltip"), //$NON-NLS-1$
			unit, desktop
		);

		Person person = (Person) unit;
		Mind mind = person.getMind();
		boolean dead = person.getPhysicalCondition().isDead();
		DeathInfo deathInfo = person.getPhysicalCondition().getDeathDetails();

		// Prepare activity label panel
		JPanel activityLabelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		topContentPanel.add(activityLabelPanel);

		// Prepare activity label
		JLabel activityLabel = new JLabel(Msg.getString("TabPanelActivity.label"), JLabel.CENTER); //$NON-NLS-1$
		activityLabelPanel.add(activityLabel);

		// Prepare job panel
		JPanel jobPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		jobPanel.setBorder(new MarsPanelBorder());
		topContentPanel.add(jobPanel);

		// Prepare job label
		jobLabel = new JLabel(Msg.getString("TabPanelActivity.job"), JLabel.CENTER); //$NON-NLS-1$
		jobPanel.add(jobLabel);        

		// Prepare job combo box
		jobCache = mind.getJob().getName(person.getGender());
		List<String> jobNames = new ArrayList<String>();
		for (Job job : JobManager.getJobs()) {
			jobNames.add(job.getName(person.getGender()));
		}
		Collections.sort(jobNames);
		jobComboBox = new JComboBoxMW<Object>(jobNames.toArray());
		jobComboBox.setSelectedItem(jobCache);
		jobComboBox.addActionListener(this);
		jobPanel.add(jobComboBox);

		// Prepare activity panel
		JPanel activityPanel = new JPanel(new GridLayout(2, 1, 0, 0));
		centerContentPanel.add(activityPanel);

		// Prepare task top panel
		JPanel taskTopPanel = new JPanel(new GridLayout(2, 1, 0, 0));
		taskTopPanel.setBorder(new MarsPanelBorder());
		activityPanel.add(taskTopPanel);

		// Prepare task panel
		JPanel taskPanel = new JPanel(new BorderLayout(0, 0));
		taskTopPanel.add(taskPanel);

		// Prepare task label
		JLabel taskLabel = new JLabel(Msg.getString("TabPanelActivity.task"), JLabel.CENTER); //$NON-NLS-1$
		taskPanel.add(taskLabel, BorderLayout.NORTH);

		// Prepare task text area
		if (dead) taskCache = deathInfo.getTask();
		else taskCache = mind.getTaskManager().getTaskDescription();
		taskTextArea = new JTextArea(2, 20);
		if (taskCache != null) taskTextArea.setText(taskCache);
		taskTextArea.setLineWrap(true);
		taskTextArea.setEditable(false);
		taskPanel.add(new JScrollPane(taskTextArea), BorderLayout.CENTER);

		// Prepare task phase panel
		JPanel taskPhasePanel = new JPanel(new BorderLayout(0, 0));
		taskTopPanel.add(taskPhasePanel);

		// Prepare task phase label
		JLabel taskPhaseLabel = new JLabel(Msg.getString("TabPanelActivity.taskPhase"), JLabel.CENTER); //$NON-NLS-1$
		taskPhasePanel.add(taskPhaseLabel, BorderLayout.NORTH);

		// Prepare task phase text area
		if (dead) taskPhaseCache = deathInfo.getTaskPhase();
		else taskPhaseCache = mind.getTaskManager().getPhase();
		taskPhaseTextArea = new JTextArea(2, 20);
		if (taskPhaseCache != null) taskPhaseTextArea.setText(taskPhaseCache);
		taskPhaseTextArea.setLineWrap(true);
		taskPhaseTextArea.setEditable(false);
		taskPhasePanel.add(new JScrollPane(taskPhaseTextArea), BorderLayout.CENTER);

		// Prepare mission top panel
		JPanel missionTopPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		missionTopPanel.setBorder(new MarsPanelBorder());
		activityPanel.add(missionTopPanel);

		// Prepare mission left panel
		JPanel missionLeftPanel = new JPanel(new GridLayout(2, 1, 0, 0));
		missionTopPanel.add(missionLeftPanel, BorderLayout.CENTER);

		// Prepare mission panel
		JPanel missionPanel = new JPanel(new BorderLayout(0, 0));
		missionLeftPanel.add(missionPanel);

		// Prepare mission label
		JLabel missionLabel = new JLabel(Msg.getString("TabPanelActivity.mission"), JLabel.CENTER); //$NON-NLS-1$
		missionPanel.add(missionLabel, BorderLayout.NORTH);

		// Prepare mission text area
		if (dead) missionCache = deathInfo.getMission();
		else if (mind.getMission() != null) missionCache = mind.getMission().getDescription();
		missionTextArea = new JTextArea(2, 20);
		if (missionCache != null) missionTextArea.setText(missionCache);
		missionTextArea.setLineWrap(true);
		missionTextArea.setEditable(false);
		missionPanel.add(new JScrollPane(missionTextArea), BorderLayout.CENTER);

		// Prepare mission phase panel
		JPanel missionPhasePanel = new JPanel(new BorderLayout(0, 0));
		missionLeftPanel.add(missionPhasePanel);

		// Prepare mission phase label
		JLabel missionPhaseLabel = new JLabel(Msg.getString("TabPanelActivity.missionPhase"), JLabel.CENTER); //$NON-NLS-1$
		missionPhasePanel.add(missionPhaseLabel, BorderLayout.NORTH);

		// Prepare mission phase text area
		if (dead) missionPhaseCache = deathInfo.getMissionPhase();
		else if (mind.getMission() != null) missionPhaseCache = mind.getMission().getPhaseDescription();
		missionPhaseTextArea = new JTextArea(2, 20);
		if (missionPhaseCache != null) missionPhaseTextArea.setText(missionPhaseCache);
		missionPhaseTextArea.setLineWrap(true);
		missionPhaseTextArea.setEditable(false);
		missionPhasePanel.add(new JScrollPane(missionPhaseTextArea), BorderLayout.CENTER);

		// Prepare mission button panel.
		JPanel missionButtonPanel = new JPanel(new GridLayout(2, 1, 0, 2));
		missionTopPanel.add(missionButtonPanel);

		// Prepare mission tool button.
		missionButton = new JButton(ImageLoader.getIcon(Msg.getString("img.mission"))); //$NON-NLS-1$
		missionButton.setMargin(new Insets(1, 1, 1, 1));
		missionButton.setToolTipText(Msg.getString("TabPanelActivity.tooltip.mission")); //$NON-NLS-1$
		missionButton.addActionListener(this);
		missionButton.setEnabled(mind.getMission() != null);
		missionButtonPanel.add(missionButton);

		// Prepare mission monitor button
		monitorButton = new JButton(ImageLoader.getIcon(Msg.getString("img.monitor"))); //$NON-NLS-1$
		monitorButton.setMargin(new Insets(1, 1, 1, 1));
		monitorButton.setToolTipText(Msg.getString("TabPanelActivity.tooltip.monitor")); //$NON-NLS-1$
		monitorButton.addActionListener(this);
		monitorButton.setEnabled(mind.getMission() != null);
		missionButtonPanel.add(monitorButton);
	}

	/**
	 * Updates the info on this panel.
	 */
	@Override
	public void update() {

		Person person = (Person) unit;
		Mind mind = person.getMind();
		boolean dead = person.getPhysicalCondition().isDead();
		DeathInfo deathInfo = person.getPhysicalCondition().getDeathDetails();

		// Update job if necessary.
		if (dead) {
			jobCache = deathInfo.getJob();
			jobComboBox.setEnabled(false);
		} 
		else jobCache = mind.getJob().getName(person.getGender());
		if (!jobCache.equals(jobComboBox.getSelectedItem())) jobComboBox.setSelectedItem(jobCache);

		TaskManager taskManager = null;
		Mission mission = null;
		if (!dead) {
			taskManager = mind.getTaskManager();
			if (mind.hasActiveMission()) mission = mind.getMission();
		}

		// Update task text area if necessary.
		if (dead) taskCache = deathInfo.getTask();
		else taskCache = taskManager.getTaskDescription();
		if (!taskCache.equals(taskTextArea.getText())) 
			taskTextArea.setText(taskCache);

		// Update task phase text area if necessary.
		if (dead) taskPhaseCache = deathInfo.getTaskPhase();
		else taskPhaseCache = taskManager.getPhase();
		if (!taskPhaseCache.equals(taskPhaseTextArea.getText())) 
			taskPhaseTextArea.setText(taskPhaseCache);

		// Update mission text area if necessary.
		if (dead) missionCache = deathInfo.getMission();
		else if (mission != null) missionCache = mission.getDescription();
		else missionCache = ""; //$NON-NLS-1$
		if ((missionCache != null) && !missionCache.equals(missionTextArea.getText())) 
			missionTextArea.setText(missionCache);

		// Update mission phase text area if necessary.
		if (dead) missionPhaseCache = deathInfo.getMissionPhase();
		else if (mission != null) missionPhaseCache = mission.getPhaseDescription();
		else missionPhaseCache = ""; //$NON-NLS-1$
		if ((missionPhaseCache != null) && !missionPhaseCache.equals(missionPhaseTextArea.getText())) 
			missionPhaseTextArea.setText(missionPhaseCache);

		// Update mission and monitor buttons.
		missionButton.setEnabled(mission != null);
		monitorButton.setEnabled(mission != null);
	}

	/** 
	 * Action event occurs.
	 * @param event {@link ActionEvent} the action event
	 */
	@Override
	public void actionPerformed(ActionEvent event) {
		Object source = event.getSource();

		if ((source == missionButton) || (source == monitorButton)) {
			Person person = (Person) unit;
			if (!person.getPhysicalCondition().isDead()) {
				Mind mind = person.getMind();
				if (mind.hasActiveMission()) {
					if (source == missionButton) {
						((MissionWindow) desktop.getToolWindow(MissionWindow.NAME)).selectMission(mind.getMission());
						getDesktop().openToolWindow(MissionWindow.NAME);
					}
					else if (source == monitorButton) desktop.addModel(new PersonTableModel(mind.getMission()));
				}
			}
		}
		else if (source == jobComboBox) {
			int jobIndex = jobComboBox.getSelectedIndex();
			Job job = JobManager.getJobs().get(jobIndex);
			((Person) unit).getMind().setJob(job, true);
		}
	}
}       