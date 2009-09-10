/*
 * ome.formats.importer.DebugMessenger
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2005 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *
 *------------------------------------------------------------------------------
 */
package ome.formats.importer;

import java.awt.BorderLayout;
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.Style;
import javax.swing.text.StyledDocument;

import ome.formats.importer.util.Actions;
import ome.formats.importer.util.ErrorContainer;
import ome.formats.importer.util.GuiCommonElements;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;

import layout.TableLayout;

/**
 * @author TheBrain
 *
 */
public class DebugMessenger extends JDialog implements ActionListener, IObservable
{
    private static final long serialVersionUID = -1026712513033611084L;

    
    private Preferences    userPrefs = 
        Preferences.userNodeForPackage(Main.class);

    private String userEmail = userPrefs.get("userEmail", "");
    private boolean sendFiles = userPrefs.getBoolean("sendFiles", true);
    
    boolean debug = false;
    
    private static final String ICON = "gfx/nuvola_error64.png";
    
    ArrayList<IObserver> observers = new ArrayList<IObserver>();
    
    GuiCommonElements       gui;
    
    JFrame                  owner;
    
    JPanel                  mainPanel;
    JPanel                  commentPanel;
    JPanel                  debugPanel;

    JButton                 quitBtn;
    JButton                 cancelBtn;
    JButton                 sendBtn;
    JButton                 sendWithFilesBtn;
    JButton                 ignoreBtn;
    JButton                 copyBtn;
    
    JTextField              emailTextField;
    String                  emailText           = "";          
    
    JTextArea               commentTextArea;
    String                  commentText         = "";
    
    JTextPane               debugTextPane;
    StyledDocument          debugDocument;
    Style                   debugStyle;

	private ArrayList<ErrorContainer> errorsArrayList;


	private JCheckBox uploadCheckmark;
    
    DebugMessenger(JFrame owner, String title, Boolean modal, ArrayList<ErrorContainer> errorsArrayList)
    {
        super(owner);
        gui = new GuiCommonElements();
        this.owner = owner;
        this.errorsArrayList = errorsArrayList;
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        setTitle(title);
        setModal(modal);
        setResizable(true);
        setSize(new Dimension(680, 400));
        setLocationRelativeTo(owner);
        
        // Set up the main panel for tPane, quit, and send buttons
        double mainTable[][] =
                {{10, 150, TableLayout.FILL, 100, 5, 150, 5, 150, 10}, // columns
                {TableLayout.FILL, 20, 40}}; // rows
        
        mainPanel = gui.addMainPanel(this, mainTable, 10, 10, 10, 10, debug);

        cancelBtn = gui.addButton(mainPanel, "Cancel", 'C',
                "Cancel your message", "5, 2, f, c", debug);
        cancelBtn.addActionListener(this);

        sendBtn = gui.addButton(mainPanel, "Send Comment", 'S',
                "Send your comment to the development team", "7, 2, f, c", debug);
        sendBtn.addActionListener(this);

        this.getRootPane().setDefaultButton(sendBtn);
        gui.enterPressesWhenFocused(sendBtn);
        
        uploadCheckmark = gui.addCheckBox(mainPanel, "Send selected images.", "1,1,7,c", debug);
        uploadCheckmark.setSelected(sendFiles);
                
        // fill out the comments panel (changes according to icon existance)        
        Icon icon = gui.getImageIcon(ICON);
        
        int iconSpace = 0;
        if (icon != null) iconSpace = icon.getIconWidth() + 10;
        
        double commentTable[][] = 
        {{iconSpace, (160 - iconSpace), TableLayout.FILL}, // columns
                {100, 30, TableLayout.FILL, 110}}; // rows
        
        commentPanel = gui.addMainPanel(this, commentTable, 10, 10, 10, 10, debug);

        String message = "To help us improve our software, please fill " +
        "out the following form. \n\nPlease note that providing your email " +
        "address is optional, however doing so will make it easier for us " +
        "to contact you for addition information about your errors, and for " +
        "you to track their status.";

        JLabel iconLabel = new JLabel(icon);
        commentPanel.add(iconLabel, "0,0, l, c");
        
        @SuppressWarnings("unused")
        JTextPane instructions = 
                gui.addTextPane(commentPanel, message, "1,0,2,0", debug);

        emailTextField = gui.addTextField(commentPanel, "Email: ", emailText, 'E',
        "Input your email address here.", "(Optional)", TableLayout.PREFERRED, "0, 1, 2, 1", debug);
        
        emailTextField.setText(userEmail);
        
        commentTextArea = gui.addTextArea(commentPanel, "Please provide any additional information of importance.", 
                "", 'W', "0, 2, 2, 3", debug);
        
        // Add the tab panel to the main panel
        mainPanel.add(commentPanel, "0, 0, 8, 0");
        
        add(mainPanel, BorderLayout.CENTER);
        
        setVisible(true);      
    }

    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();
        
        
        if (source == quitBtn)
        {
            if (gui.quitConfirmed(this, "Abandon your import and quit the application?") == true)
            {
                System.exit(0);
            }
        }
        
        
        if (source == cancelBtn)
        {
            dispose();
        }
        
        if (source == sendBtn)
        {           
            emailText = emailTextField.getText();
            commentText = commentTextArea.getText();
            
            if (!validEmail(emailText))
            {
                JOptionPane.showMessageDialog(this, 
                        "Your email address must be valid\n" +
                        "(or blank) to send feedback.");              
            }
            else
            {
                userPrefs.put("userEmail", emailText);
                userPrefs.putBoolean("sendFiles", uploadCheckmark.isSelected()); 
                sendRequest(emailText, commentText, "Extra data goes here.");
                dispose();
            }
        }
        
        if (source == ignoreBtn)
        {
            dispose();
        }
        
        if (source == copyBtn)
        {
            debugTextPane.selectAll();
            debugTextPane.copy();
        }
    }

    private void sendRequest(String emailText, String commentText, String extraText)
    {
        
    	for (ErrorContainer errorContainer : errorsArrayList)
    	{
            errorContainer.setEmail(emailText);
            errorContainer.setComment(commentText);
            errorContainer.setExtra(extraText);
    	}

        final Object[] observerArgs;
        observerArgs = new Object[1];
        observerArgs[0] = uploadCheckmark.isSelected();
    	
    	notifyObservers(Actions.DEBUG_SEND, observerArgs);
    }
    

    // Observable methods    
    public boolean addObserver(IObserver object)
    {
        return observers.add(object);
    }
    
    public boolean deleteObserver(IObserver object)
    {
        return observers.remove(object);
        
    }

    public void notifyObservers(Object message, Object[] args)
    {
        for (IObserver observer:observers)
        {
            observer.update(this, message, args);
        }
    }
    
    
    // Validate the basic construct for the user's email
    public boolean validEmail(String emailAddress)
    {
        String[] parts = emailAddress.split("@");
        if (parts.length == 2 && parts[0].length() != 0 && parts[1].length() != 0)
            return true;
        else
            return false;
    }
    
    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception
    {       
        try {
                HttpClient client = new HttpClient();
                PostMethod method = new PostMethod( "blarg" );
                client.executeMethod( method );
        }
        catch (Exception e)
        {
            new DebugMessenger(null, "Error Dialog Test", true, null);
        }
    }
}
