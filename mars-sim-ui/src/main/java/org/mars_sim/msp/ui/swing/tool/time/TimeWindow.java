/**
 * Mars Simulation Project
 * TimeWindow.java
 * @version 3.07 2014-12-06

 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.time;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.time.ClockListener;
import org.mars_sim.msp.core.time.EarthClock;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.time.MasterClock;
import org.mars_sim.msp.core.time.UpTimer;
import org.mars_sim.msp.ui.swing.JSliderMW;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.tool.ToolWindow;

/** 
 * The TimeWindow is a tool window that displays the current 
 * Martian and Earth time.<br/>
 * The numbers below have been tweaked with some care. At 20, the realworld:sim ratio is 1:1
 * above 20, the numbers start climbing logarithmically maxing out at around 100K this is really fast
 * Below 20, the simulation goes in slow motion, 1:0.0004 is around the slowest. The increments may be
 * so small at this point that events can't progress at all. When run too quickly, lots of accidents occur,
 * and lots of settlers die.
 */
public class TimeWindow
extends ToolWindow
implements ClockListener {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(TimeWindow.class.getName());

	/** Tool name. */
	public static final String NAME = Msg.getString("TimeWindow.title");		 //$NON-NLS-1$

	/** the "default" ratio that will be set at 50, the middle of the scale. */
	private static final double ratioatmid = 1000d;

	/** the max ratio the sim can be set at. */
	private static final double maxratio = 10800d;

	/** the minimum ratio the sim can be set at. */
	private static final double minfracratio = 0.001d;

	/** the largest fractional ratio the sim can be set at. */
	private static final double maxfracratio = 0.98d;

	// don't recommend changing these:
	private static final double minslider = 20d;
	private static final double midslider = (50d - minslider);
	private static final double maxslider = 100d - minslider;
	private static final double minfracpos = 1d;
	private static final double maxfracpos = minslider - 1d;

	// Data members
	/** Master Clock. */
	private MasterClock master;
	/** Martian Clock. */
	private MarsClock marsTime;
	/** Earth Clock. */
	private EarthClock earthTime;
	/** Uptime Timer. */
	private UpTimer uptimer;
	/** Martian calendar panel. */
	private MarsCalendarDisplay calendarDisplay;
	/** label for Martian time. */
	private JLabel martianTimeLabel;
	/** label for Martian month. */
	private JLabel martianMonthLabel;
	/** label for Northern hemisphere season. */
	private JLabel northernSeasonLabel;
	/** label for Southern hemisphere season. */
	private JLabel southernSeasonLabel;
	/** label for Earth time. */
	private JLabel earthTimeLabel;
	/** label for uptimer. */
	private JLabel uptimeLabel;
	/** label for pulses per second label. */
	private JLabel pulsespersecondLabel; 
	/** slider for pulse. */
	private JSliderMW pulseSlider;
	private int sliderpos = 50;
	private JButton pauseButton;

	/**
	 * Constructs a TimeWindow object 
	 * @param desktop the desktop pane
	 */
	public TimeWindow(final MainDesktopPane desktop) {
		// Use TimeWindow constructor
		super(NAME, desktop);

		// Set window resizable to false.
		setResizable(false);

		// Initialize data members
		master = Simulation.instance().getMasterClock();
		master.addClockListener(this);
		marsTime = master.getMarsClock();
		earthTime = master.getEarthClock();
		uptimer = master.getUpTimer(); 

		// Get content pane
		JPanel mainPane = new JPanel(new BorderLayout());
		mainPane.setBorder(new MarsPanelBorder());
		setContentPane(mainPane);

		// Create Martian time panel
		JPanel martianTimePane = new JPanel(new BorderLayout());
		martianTimePane.setBorder(new CompoundBorder(new EtchedBorder(), MainDesktopPane.newEmptyBorder()));
		mainPane.add(martianTimePane, BorderLayout.NORTH);

		// Create Martian time header label
		JLabel martianTimeHeaderLabel = new JLabel(Msg.getString("TimeWindow.martianTime"), JLabel.CENTER); //$NON-NLS-1$
		martianTimePane.add(martianTimeHeaderLabel, BorderLayout.NORTH);

		// Create Martian time label
		martianTimeLabel = new JLabel(marsTime.getTimeStamp(), JLabel.CENTER);
		martianTimePane.add(martianTimeLabel, BorderLayout.SOUTH);

		// Create Martian calendar panel
		JPanel martianCalendarPane = new JPanel(new FlowLayout());
		martianCalendarPane.setBorder(new CompoundBorder(new EtchedBorder(), MainDesktopPane.newEmptyBorder()));
		mainPane.add(martianCalendarPane, BorderLayout.CENTER);

		// Create Martian calendar month panel
		JPanel calendarMonthPane = new JPanel(new BorderLayout());
		martianCalendarPane.add(calendarMonthPane);

		// Create martian month label
		martianMonthLabel = new JLabel(marsTime.getMonthName(), JLabel.CENTER);
		calendarMonthPane.add(martianMonthLabel, BorderLayout.NORTH);

		// Create Martian calendar display
		calendarDisplay = new MarsCalendarDisplay(marsTime);
		JPanel innerCalendarPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		innerCalendarPane.setBorder(new BevelBorder(BevelBorder.LOWERED));
		innerCalendarPane.add(calendarDisplay);
		calendarMonthPane.add(innerCalendarPane, BorderLayout.CENTER);        

		JPanel southPane = new JPanel(new BorderLayout());
		mainPane.add(southPane, BorderLayout.SOUTH);

		JPanel simulationPane = new JPanel(new BorderLayout());
		southPane.add(simulationPane, BorderLayout.SOUTH);

		// Create Martian season panel
		JPanel marsSeasonPane = new JPanel(new BorderLayout());
		marsSeasonPane.setBorder(new CompoundBorder(new EtchedBorder(), MainDesktopPane.newEmptyBorder()));
		southPane.add(marsSeasonPane, BorderLayout.NORTH);

		// Create Martian season label
		JLabel marsSeasonLabel = new JLabel(Msg.getString("TimeWindow.martianSeasons"), JLabel.CENTER); //$NON-NLS-1$
		marsSeasonPane.add(marsSeasonLabel, BorderLayout.NORTH);

		// Create Northern season label
		northernSeasonLabel = new JLabel(
			Msg.getString(
				"TimeWindow.northernHemisphere", //$NON-NLS-1$
				marsTime.getSeason(MarsClock.NORTHERN_HEMISPHERE)
			), JLabel.CENTER
		);
		marsSeasonPane.add(northernSeasonLabel, BorderLayout.CENTER);

		// Create Southern season label
		southernSeasonLabel = new JLabel(
			Msg.getString(
				"TimeWindow.southernHemisphere", //$NON-NLS-1$
				marsTime.getSeason(MarsClock.SOUTHERN_HEMISPHERE)
			), JLabel.CENTER
		);
		marsSeasonPane.add(southernSeasonLabel, BorderLayout.SOUTH);

		// Create Earth time panel
		JPanel earthTimePane = new JPanel(new BorderLayout());
		earthTimePane.setBorder(new CompoundBorder(new EtchedBorder(), MainDesktopPane.newEmptyBorder()));
		southPane.add(earthTimePane, BorderLayout.CENTER);

		// Create Earth time header label
		JLabel earthTimeHeaderLabel = new JLabel(Msg.getString("TimeWindow.earthTime"), JLabel.CENTER); //$NON-NLS-1$
		earthTimePane.add(earthTimeHeaderLabel, BorderLayout.NORTH);

		// Create Earth time label
		earthTimeLabel = new JLabel(earthTime.getTimeStamp(), JLabel.CENTER);
		earthTimePane.add(earthTimeLabel, BorderLayout.SOUTH);

		// Create uptime panel
		JPanel uptimePane = new JPanel(new BorderLayout());
		uptimePane.setBorder(new CompoundBorder(new EtchedBorder(), MainDesktopPane.newEmptyBorder()));
		simulationPane.add(uptimePane, BorderLayout.NORTH);

		JPanel pulsespersecondPane = new JPanel(new BorderLayout());
		pulsespersecondPane.setBorder(new CompoundBorder(new EtchedBorder(), MainDesktopPane.newEmptyBorder()));
		uptimePane.add(pulsespersecondPane, BorderLayout.SOUTH);

		JPanel pausePane = new JPanel( new BorderLayout());
		southPane.add(pausePane, BorderLayout.NORTH);

		pauseButton = new JButton(Msg.getString("TimeWindow.button.pause")); //$NON-NLS-1$
		pauseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				master.setPaused(!master.isPaused());

			}
		});

		// Create uptime header label
		JLabel uptimeHeaderLabel = new JLabel(Msg.getString("TimeWindow.simUptime"), JLabel.CENTER); //$NON-NLS-1$
		uptimePane.add(uptimeHeaderLabel, BorderLayout.NORTH);

		JLabel pulsespersecondHeaderLabel = new JLabel(Msg.getString("TimeWindow.ticksPerSecond"), JLabel.CENTER); //$NON-NLS-1$
		pulsespersecondPane.add(pulsespersecondHeaderLabel, BorderLayout.NORTH);

		// Create uptime label
		uptimeLabel = new JLabel(uptimer.getUptime(), JLabel.CENTER);
		uptimePane.add(uptimeLabel, BorderLayout.CENTER);

		DecimalFormat formatter = new DecimalFormat(Msg.getString("TimeWindow.decimalFormat")); //$NON-NLS-1$
		String pulsePerSecond = formatter.format(master.getPulsesPerSecond());
		pulsespersecondLabel = new JLabel(pulsePerSecond, JLabel.CENTER);
		pulsespersecondPane.add(pulsespersecondLabel, BorderLayout.CENTER);

		// Create uptime panel
		JPanel pulsePane = new JPanel(new BorderLayout());
		pulsePane.setBorder(new CompoundBorder(new EtchedBorder(), MainDesktopPane.newEmptyBorder()));
		simulationPane.add(pulsePane, BorderLayout.SOUTH);

		// Create pulse header label
		final JLabel pulseHeaderLabel = new JLabel(Msg.getString("TimeWindow.pulseHeader"), JLabel.CENTER); //$NON-NLS-1$
		pulsePane.add(pulseHeaderLabel, BorderLayout.NORTH);

		pulsespersecondPane.add(pauseButton, BorderLayout.SOUTH);

		// create time ratio readout showing real / earth / mars time ratios currently set
		try {
			setTimeRatioFromSlider(sliderpos);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		//String s = String.format("1 : %5.3f : %5.3f", master.getTimeRatio(),
		//		MarsClock.convertSecondsToMillisols(master.getTimeRatio()) ).toString() ;
		String s = master.getTimeString(master.getTimeRatio());
		final JLabel pulseCurRatioLabel = new JLabel(s, JLabel.CENTER);
		pulsePane.add(pulseCurRatioLabel, BorderLayout.CENTER);

		pulseCurRatioLabel.addMouseListener(new MouseInputAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				if (pulseCurRatioLabel.getText().contains(":")) { //$NON-NLS-1$
					pulseCurRatioLabel.setText(String.format(Msg.getString("TimeWindow.timeFormat"), master.getTimeRatio() ) ); //$NON-NLS-1$
				} else {
					pulseCurRatioLabel.setText(master.getTimeString(master.getTimeRatio()) );
				}
			}
		});

		// Create pulse slider
		pulseSlider = new JSliderMW(1, 100, sliderpos);
		pulseSlider.setMajorTickSpacing(10);
		pulseSlider.setMinorTickSpacing(2);
		pulseSlider.setPaintTicks(true);
		pulseSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				try {
					setTimeRatioFromSlider(pulseSlider.getValue());
					if (pulseCurRatioLabel.getText().contains(":") )  //$NON-NLS-1$
					{pulseCurRatioLabel.setText(master.getTimeString(master.getTimeRatio()));}
					else 
					{pulseCurRatioLabel.setText(String.format(Msg.getString("TimeWindow.timeFormat"), master.getTimeRatio() ) );} //$NON-NLS-1$
				}
				catch (Exception e2) {
					logger.log(Level.SEVERE,e2.getMessage());
				}

			}
		});
		pulsePane.add(pulseSlider, BorderLayout.SOUTH);


		// Pack window
		pack();

		// Add 10 pixels to packed window width
		Dimension windowSize = getSize();
		setSize(new Dimension((int)windowSize.getWidth() + 10, (int) windowSize.getHeight()));
	}

	/**
	 * Sets the time ratio for the simulation based on the slider value.
	 */
	private void setTimeRatioFromSlider(int sliderValue) {
		double slope; 
		double offset;
		double timeRatio;

		// sliderValue should be in the range 1..100 inclusive, if not it defaults to
		// 1:15 real:sim ratio
		if ((sliderValue > 0) && (sliderValue <= 100)) {
			if (sliderValue >= (midslider + minslider)) {

				// Creates exponential curve between ratioatmid and maxratio.
				double a = ratioatmid;
				double b = maxratio / ratioatmid;
				double T = maxslider - midslider;
				double expo = (sliderValue - minslider - midslider) / T;
				timeRatio = a * Math.pow(b, expo);
			}
			else if (sliderValue >= minslider) {

				// Creates exponential curve between 1 and ratioatmid.
				double a = 1D;
				double b = ratioatmid;
				double T = midslider;
				double expo = (sliderValue - minslider) / T;
				timeRatio = a * Math.pow(b, expo);
			} 
			else {
				// generates ratios < 1
				offset = minfracratio;
				slope = (maxfracratio - minfracratio) / (maxfracpos - minfracpos);
				timeRatio = (sliderValue - minfracpos) * slope + offset;
			}
		}
		else {
			timeRatio = 15D;
			throw new IllegalArgumentException(Msg.getString("TimeWindow.log.ratioError")); //$NON-NLS-1$
		}

		master.setTimeRatio(timeRatio);
	}

	public void setTimeRatioSlider(int r) {
		//moves the slider bar appropriately given the ratio 
		if (r>=pulseSlider.getMinimum() && r <= pulseSlider.getMaximum()) {
			pulseSlider.setValue(r);
		}
	}

	@Override
	public void clockPulse(double time) {
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
	
					if (marsTime != null) {
						martianTimeLabel.setText(marsTime.getTimeStamp());
						martianMonthLabel.setText(marsTime.getMonthName());
						northernSeasonLabel.setText(
							Msg.getString(
								"TimeWindow.northernHemisphere",  //$NON-NLS-1$
								marsTime.getSeason(MarsClock.NORTHERN_HEMISPHERE)
							)
						);
						southernSeasonLabel.setText(
							Msg.getString(
								"TimeWindow.southernHemisphere", //$NON-NLS-1$
								marsTime.getSeason(MarsClock.SOUTHERN_HEMISPHERE)
							)
						);
					}
	
					if (earthTime != null) {
						earthTimeLabel.setText(earthTime.getTimeStamp());
					}
	
					if (master != null) {
						DecimalFormat formatter = new DecimalFormat(Msg.getString("TimeWindow.decimalFormat")); //$NON-NLS-1$
						String pulsePerSecond = formatter.format(master.getPulsesPerSecond());
						pulsespersecondLabel.setText(pulsePerSecond);
					}
	
					if (uptimer != null) {
						uptimeLabel.setText(uptimer.getUptime());
					}
	
					calendarDisplay.update();
				}
			}
		);
	}

	@Override
	public void pauseChange(boolean isPaused) {
		// Update pause/resume button text based on master clock pause state.
		if (isPaused) {
			pauseButton.setText(Msg.getString("TimeWindow.button.resume")); //$NON-NLS-1$
			desktop.openAnnouncementWindow(Msg.getString("MainWindow.pausingSim"));
		}
		else {
			pauseButton.setText(Msg.getString("TimeWindow.button.pause")); //$NON-NLS-1$
			desktop.disposeAnnouncementWindow();
		}
	}

	/**
	 * Prepare tool window for deletion.
	 */
	@Override
	public void destroy() {
		if (master != null) {
			master.removeClockListener(this);
		}
		master = null;
		marsTime = null;
		earthTime = null;
		uptimer = null;
	}
}