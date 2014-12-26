/**
 * Mars Simulation Project
 * SettlementWindow.java
 * @version 3.07 2014-12-20
 * @author Lars Naesbye Christensen
 */
package org.mars_sim.msp.ui.swing.tool.settlement;


import java.awt.Color;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;


import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.tool.ToolWindow;

/**
 * The SettlementWindow is a tool window that displays the Settlement Map Tool.
 */
public class SettlementWindow extends ToolWindow {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// default logger.
	//private static Logger logger = Logger.getLogger(SettlementWindow.class.getName());
	
	/** Tool name. */
	public static final String NAME = Msg.getString("SettlementWindow.title"); //$NON-NLS-1$

	private Settlement settlement;
	private PopUpBuildingMenu menu;

	/** The main desktop. */
	private MainDesktopPane desktop;
	/** Map panel. */
	private SettlementMapPanel mapPanel;
	/** Last X mouse drag position. */
	private int xLast;
	/** Last Y mouse drag position. */
	private int yLast;
	/**
	 * Constructor.
	 * @param desktop the main desktop panel.
	 */
	public SettlementWindow(final MainDesktopPane desktop) {
		// Use ToolWindow constructor
		super(NAME, desktop);
		this.desktop = desktop;	
		final SettlementWindow settlementWindow = this;
	 
		setMaximizable(true);

		mapPanel = new SettlementMapPanel(desktop, this);
	      
		setContentPane(mapPanel);
		mapPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent evt) {
				// Set initial mouse drag position.
				xLast = evt.getX();
				yLast = evt.getY();
			}

			@Override
			public void mouseClicked(MouseEvent evt) {
				// Select person if clicked on.
				mapPanel.selectPersonAt(evt.getX(), evt.getY());
			}

		});
 
		//2014-11-22 Added PopClickListener() to detect mouse right click
		class PopClickListener extends MouseAdapter {
		    public void mousePressed(MouseEvent evt){
				 if (evt.isPopupTrigger()) {
					//logger.info("mousePressed() : Yes, popup is triggered");
					 doPop(evt);
				 }
		    }

		    public void mouseReleased(MouseEvent evt){
				 if (evt.isPopupTrigger()) {
					//logger.info("mouseReleased() : Yes, popup is triggered");
					 doPop(evt);
				 }
					xLast = evt.getX();
					yLast = evt.getY();
		    }

		    private void doPop(MouseEvent evt){
		    	final Building building = mapPanel.selectBuildingAt(evt.getX(), evt.getY());
		        // if NO building is selected, do NOT call popup menu
		    	if (building != null) {
		    		
		    	    //SwingUtilities.invokeLater(new Runnable(){
		    	        //public void run()  {
				        	menu = new PopUpBuildingMenu(settlementWindow, building);
		    	       // } });
		        	menu.show(evt.getComponent(), evt.getX(), evt.getY());
		        }
		    }
		}
		mapPanel.addMouseListener(new PopClickListener());
		
		
		mapPanel.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent evt) {
				// Move map center based on mouse drag difference.
				double xDiff = evt.getX() - xLast;
				double yDiff = evt.getY() - yLast;
				mapPanel.moveCenter(xDiff, yDiff);
				xLast = evt.getX();
				yLast = evt.getY();
			}
		});

		pack();
		setVisible(true);
	}

    
	/**
	 * Ask for a new Settlement name
	 * @return pop up jDialog
	 */
	// 2014-10-26 Added askNameDialog()
	public String askNameDialog() {
		return JOptionPane
			.showInputDialog(desktop, 
					Msg.getString("SettlementWindow.JDialog.changeSettlementName.input"), //$NON-NLS-1$
					Msg.getString("SettlementWindow.JDialog.changeSettlementName.title"), //$NON-NLS-1$
			        JOptionPane.QUESTION_MESSAGE);
	}
	/**
	 * Change and validate the new name of the Settlement
	 * @return call Dialog popup
	 */
	// 2014-10-26 Modified renameSettlement()
	public void renameSettlement() {
		JDialog.setDefaultLookAndFeelDecorated(true);
		//String nameCache = settlement.getType();
		String settlementNewName = askNameDialog();
				
		if ( settlementNewName.trim() == null || settlementNewName.trim().length() == 0)
			settlementNewName = askNameDialog();
		else {
			settlement.changeName(settlementNewName);
		}
		// desktop.clearDesktop();
		desktop.closeToolWindow(SettlementWindow.NAME);
		desktop.openToolWindow(SettlementWindow.NAME);	
	}
	
	public void setSettlement(Settlement settlement) {
		this.settlement = settlement;
	}
	
	public Settlement getSettlement() {
		return settlement ;
	}
	
	public class JCustomCheckBoxMenuItem extends JCheckBoxMenuItem {

		public JCustomCheckBoxMenuItem(String s, boolean b) {
			super(s, b);
		}
		private static final long serialVersionUID = 1L;

				
		/*
		public void paint(Graphics g) { 
			//protected void paintComponent(Graphics g) {
				//super.paintComponent(g);
		
                Graphics2D g2d = (Graphics2D) g.create(); 
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f)); 
                super.paint(g2d); 
                g2d.dispose(); 
        } 
        */
	}
	/**
	 * Create the labels popup menu.
	 * @return popup menu.
	 */
	public JPopupMenu createLabelsMenu() {
		JPopupMenu result = new JPopupMenu(Msg.getString("SettlementWindow.menu.labelOptions")); //$NON-NLS-1$

		//result.setOpaque(false);
		result.setBorder(BorderFactory.createLineBorder(Color.orange));
		result.setBackground(new Color(222,184,135));
		//UIDefaults ui = UIManager.getLookAndFeelDefaults();
		//ui.put("PopupMenu.background", Color.GRAY);
		//ui.put("Menu.background", Color.ORANGE);
		//ui.put("Menu.opaque", true);
		//ui.put("MenuItem.background", Color.ORANGE);
		//ui.put("MenuItem.opaque", true);
		//ui.put("PopupMenu.contentMargins", null);
		//UIManager.put("MenuItem.background", Color.ORANGE);
		//UIManager.put("MenuItem.opaque", true);
		
		// Create building label menu item.
		JCustomCheckBoxMenuItem buildingLabelMenuItem = new JCustomCheckBoxMenuItem(
				Msg.getString("SettlementWindow.menu.buildings"), getMapPanel().isShowBuildingLabels()); //$NON-NLS-1$
		// 2014-12-24 Added setting setForeground setContentAreaFilled setOpaque
		buildingLabelMenuItem.setForeground(Color.ORANGE);
		buildingLabelMenuItem.setBackground(new Color(222,184,135));
		buildingLabelMenuItem.setContentAreaFilled(false);
		//buildingLabelMenuItem.setOpaque(false);
		buildingLabelMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				getMapPanel().setShowBuildingLabels(!getMapPanel().isShowBuildingLabels());
			}
		});
		result.add(buildingLabelMenuItem);

		// Create construction/salvage label menu item.
		JCustomCheckBoxMenuItem constructionLabelMenuItem = new JCustomCheckBoxMenuItem(
				Msg.getString("SettlementWindow.menu.constructionSites"), getMapPanel().isShowConstructionLabels()); //$NON-NLS-1$
		constructionLabelMenuItem.setForeground(Color.ORANGE);
		constructionLabelMenuItem.setBackground(new Color(222,184,135));
		constructionLabelMenuItem.setContentAreaFilled(false);
		//constructionLabelMenuItem.setOpaque(false);
		constructionLabelMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				getMapPanel().setShowConstructionLabels(!getMapPanel().isShowConstructionLabels());
			}
		});
		result.add(constructionLabelMenuItem);

		// Create vehicle label menu item.
		JCustomCheckBoxMenuItem vehicleLabelMenuItem = new JCustomCheckBoxMenuItem(
				Msg.getString("SettlementWindow.menu.vehicles"), getMapPanel().isShowVehicleLabels()); //$NON-NLS-1$
		vehicleLabelMenuItem.setForeground(Color.ORANGE);
		vehicleLabelMenuItem.setBackground(new Color(222,184,135));
		vehicleLabelMenuItem.setContentAreaFilled(false);
		//vehicleLabelMenuItem.setOpaque(false);
		vehicleLabelMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				getMapPanel().setShowVehicleLabels(!getMapPanel().isShowVehicleLabels());
			}
		});
		result.add(vehicleLabelMenuItem);

		// Create person label menu item.
		JCustomCheckBoxMenuItem personLabelMenuItem = new JCustomCheckBoxMenuItem(
				Msg.getString("SettlementWindow.menu.people"), getMapPanel().isShowPersonLabels()); //$NON-NLS-1$
		personLabelMenuItem.setForeground(Color.ORANGE);
		personLabelMenuItem.setBackground(new Color(222,184,135));
		personLabelMenuItem.setContentAreaFilled(false);
		//personLabelMenuItem.setOpaque(false);
		personLabelMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				getMapPanel().setShowPersonLabels(!getMapPanel().isShowPersonLabels());
			}
		});
		result.add(personLabelMenuItem);

		result.pack();

		return result;
	}

	/**
	 * Gets the settlement map panel.
	 * @return the settlement map panel.
	 */
	public SettlementMapPanel getMapPanel() {
		return mapPanel;
	}

	/**
	 * Gets the main desktop panel for this tool.
	 * @return main desktop panel.
	 */
	public MainDesktopPane getDesktop() {
		return desktop;
	}

	@Override
	public void destroy() {
		//settlementCBModel.destroy();
		mapPanel.destroy();
	}

}