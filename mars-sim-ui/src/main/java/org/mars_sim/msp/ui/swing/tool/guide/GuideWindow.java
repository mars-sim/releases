/**
 * Mars Simulation Project
 * GuideWindow.java
 * @version 3.06 2014-01-29
 * @author Lars Naesbye Christensen
 */

package org.mars_sim.msp.ui.swing.tool.guide;

import org.mars_sim.msp.ui.swing.HTMLContentPane;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.tool.ToolWindow;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.net.URL;

/**
 * The GuideWindow is a tool window that displays the built-in User Guide,
 * About Box and Tutorial.
 */
public class GuideWindow extends ToolWindow implements ActionListener, 
        HyperlinkListener, ComponentListener {

    // Tool name
    public static final String NAME = "Help";

    // Data members
    private JViewport viewPort; // The view port for the text pane

    private HTMLContentPane htmlPane; // our HTML content pane

    //private URL guideURL = GuideWindow.class.getClassLoader().getResource("docs" + File.separator + 
    //        "help" + File.separator + "userguide.html");
    /* [landrus, 27.11.09]: load the url in the contructor. */
    private URL guideURL;

    private JButton homeButton = new JButton("Home");

    private JButton backButton = new JButton("Back");

    private JButton forwardButton = new JButton("Forward");

    /**
     * Constructs a TableWindow object
     * 
     * @param desktop the desktop pane
     */
    public GuideWindow(MainDesktopPane desktop) {

        // Use TableWindow constructor
        super(NAME, desktop);
        /* [landrus, 27.11.09]: use classloader compliant paths */
        guideURL = getClass().getResource("/docs/help/userguide.html");
        // Create the main panel
        JPanel mainPane = new JPanel(new BorderLayout());
        mainPane.setBorder(new MarsPanelBorder());
        setContentPane(mainPane);

        homeButton.setActionCommand("home");
        homeButton.setToolTipText("Go to Home");
        homeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                htmlPane.goToURL(guideURL);
                updateButtons();
            }
        });

        backButton.setActionCommand("back");
        backButton.setToolTipText("Back");
        backButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                htmlPane.back();
                updateButtons();
            }
        });

        forwardButton.setActionCommand("forward");
        forwardButton.setToolTipText("Forward");
        forwardButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                htmlPane.forward();
                updateButtons();
            }
        });

        // A toolbar to hold all our buttons
        JPanel toolPanel = new JPanel();
        toolPanel.add(homeButton);
        toolPanel.add(backButton);
        toolPanel.add(forwardButton);

        htmlPane = new HTMLContentPane();
        htmlPane.addHyperlinkListener(this);
        htmlPane.goToURL(guideURL);

        htmlPane.setBackground(Color.lightGray);
        htmlPane.setBorder(new EmptyBorder(2, 2, 2, 2));

        JScrollPane scrollPane = new JScrollPane(htmlPane);
        scrollPane.setBorder(new MarsPanelBorder());
        viewPort = scrollPane.getViewport();
        viewPort.addComponentListener(this);
        viewPort.setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);

        mainPane.add(scrollPane);
        mainPane.add(toolPanel, BorderLayout.NORTH);

        // Have to define a starting size
        setSize(new Dimension(575, 475));

        // Allow the window to be resized by the user.
        setResizable(true);
        setMaximizable(true);
        updateButtons();

        // Show the window
        setVisible(true);

    }

    /**
     * Handles a click on a link
     * 
     * @param event the HyperlinkEvent
     */
    public void hyperlinkUpdate(HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            htmlPane.goToURL(event.getURL());
            updateButtons();
        }
    }

    /**
     * Updates navigation buttons.
     */
    public void updateButtons() {
        homeButton.setEnabled(true);
        backButton.setEnabled(!htmlPane.isFirst());
        forwardButton.setEnabled(!htmlPane.isLast());
    }

    /**
     * Set a display URL .
     */
    public void setURL(String fileloc) {
    	htmlPane.goToURL(getClass().getResource(fileloc));

    }

    // Implementing ActionListener method
    public void actionPerformed(ActionEvent event) {
        dispose();
    }

    // Implement ComponentListener interface.
    // Make sure the text is scrolled to the top.
    // Need to find a better way to do this <Scott>
    public void componentResized(ComponentEvent e) {
        viewPort.setViewPosition(new Point(0, 0));
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }

    /**
     * Prepare tool window for deletion.
     */
    public void destroy() {
    }
}