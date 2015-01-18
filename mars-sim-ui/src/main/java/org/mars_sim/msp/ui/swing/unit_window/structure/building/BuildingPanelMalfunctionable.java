/**
 * Mars Simulation Project
 * BuildingPanelMalfunctionable.java
 * @version 3.07 2014-11-21
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.unit_window.structure.building;

import org.apache.commons.collections.CollectionUtils;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.malfunction.Malfunction;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.unit_window.MalfunctionPanel;

import javax.swing.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * The BuildingPanelMalfunctionable class is a building function panel representing 
 * the malfunctions of a settlement building.
 */
public class BuildingPanelMalfunctionable
extends BuildingFunctionPanel {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** The malfunctionable building. */
	private Malfunctionable malfunctionable;
	/** List of malfunction panels. */
	private Collection<MalfunctionPanel> malfunctionPanels;
	/** List of malfunctions in building. */
	private Collection<Malfunction> malfunctionCache;
	/** Malfunction list panel. */
	private JPanel malfunctionListPanel;

	/**
	 * Constructor.
	 * @param malfunctionable the malfunctionable building the panel is for.
	 * @param desktop The main desktop.
	 */
	public BuildingPanelMalfunctionable(Malfunctionable malfunctionable, MainDesktopPane desktop) {

		// Use BuildingFunctionPanel constructor
		super((Building) malfunctionable, desktop);

		// Initialize data members.
		this.malfunctionable = malfunctionable;

		// Set panel layout
		setLayout(new BorderLayout());

		// Create malfunctions label
		// 2014-11-21 Changed font type, size and color and label text
		// 2014-11-21 Added internationalization for labels
		JLabel malfunctionsLabel = new JLabel(Msg.getString("BuildingPanelMalfunctionable.title"), JLabel.CENTER);
		malfunctionsLabel.setFont(new Font("Serif", Font.BOLD, 16));
		malfunctionsLabel.setForeground(new Color(102, 51, 0)); // dark brown
		add(malfunctionsLabel, BorderLayout.NORTH);

		// Create scroll panel for malfunction list
		JScrollPane scrollPanel = new JScrollPane();
		scrollPanel.setPreferredSize(new Dimension(170, 90));
		add(scrollPanel, BorderLayout.CENTER);
        scrollPanel.setOpaque(false);
        scrollPanel.setBackground(new Color(0,0,0,128));
        scrollPanel.getViewport().setOpaque(false);
        scrollPanel.getViewport().setBackground(new Color(0, 0, 0, 0));
        scrollPanel.setBorder( BorderFactory.createLineBorder(Color.LIGHT_GRAY) );

		// Create malfunction list main panel.
		JPanel malfunctionListMainPanel = new JPanel(new BorderLayout(0, 0));
		scrollPanel.setViewportView(malfunctionListMainPanel);
		malfunctionListMainPanel.setOpaque(false);
		malfunctionListMainPanel.setBackground(new Color(0,0,0,128));
		
		// Create malfunction list panel
		malfunctionListPanel = new JPanel();
		malfunctionListPanel.setLayout(new BoxLayout(malfunctionListPanel, BoxLayout.Y_AXIS));
		malfunctionListMainPanel.add(malfunctionListPanel, BorderLayout.NORTH);
		malfunctionListPanel.setOpaque(false);
		malfunctionListPanel.setBackground(new Color(0,0,0,128));
		
		// Create malfunction panels
		malfunctionCache = new ArrayList<Malfunction>(malfunctionable.getMalfunctionManager().getMalfunctions());
		malfunctionPanels = new ArrayList<MalfunctionPanel>();
		Iterator<Malfunction> i = malfunctionCache.iterator();
		while (i.hasNext()) {
			MalfunctionPanel panel = new MalfunctionPanel(i.next());
			malfunctionListPanel.add(panel);
			malfunctionPanels.add(panel);
		}
	}

	@Override
	public void update() {

		Collection<Malfunction> malfunctions = malfunctionable.getMalfunctionManager().getMalfunctions();

		// Update malfunction panels if necessary.
		if (!CollectionUtils.isEqualCollection(malfunctionCache, malfunctions)) {
			// Add malfunction panels for new malfunctions.
			Iterator<Malfunction> iter1 = malfunctions.iterator();
			while (iter1.hasNext()) {
				Malfunction malfunction = iter1.next();
				if (!malfunctionCache.contains(malfunction)) {
					MalfunctionPanel panel = new MalfunctionPanel(malfunction);
					malfunctionPanels.add(panel);
					malfunctionListPanel.add(panel);
					panel.setOpaque(false);
					panel.setBackground(new Color(0,0,0,128));
				}
			}

			// Remove malfunction panels for repaired malfunctions.
			Iterator<Malfunction> iter2 = malfunctionCache.iterator();
			while (iter2.hasNext()) {
				Malfunction malfunction = iter2.next();
				if (!malfunctions.contains(malfunction)) {
					MalfunctionPanel panel = getMalfunctionPanel(malfunction);
					if (panel != null) {
						malfunctionPanels.remove(panel);
						malfunctionListPanel.remove(panel);
					}
				}
			}

			// Update malfunction cache.
			malfunctionCache = new ArrayList<Malfunction>(malfunctions);
		}

		// Have each malfunction panel update.
		Iterator<MalfunctionPanel> i = malfunctionPanels.iterator();
		while (i.hasNext()) i.next().update();
	}

	/**
	 * Gets an existing malfunction panel for a given malfunction.
	 * @param malfunction the given malfunction
	 * @return malfunction panel or null if none.
	 */
	private MalfunctionPanel getMalfunctionPanel(Malfunction malfunction) {
		MalfunctionPanel result = null;

		Iterator<MalfunctionPanel> i = malfunctionPanels.iterator();
		while (i.hasNext()) {
			MalfunctionPanel panel = i.next();
			if (panel.getMalfunction() == malfunction) result = panel;
		}

		return result;
	}
}