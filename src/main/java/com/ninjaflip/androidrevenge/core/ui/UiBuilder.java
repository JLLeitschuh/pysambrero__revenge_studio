package com.ninjaflip.androidrevenge.core.ui;

import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.scrapper.ScrapperProcessBuilder;
import com.ninjaflip.androidrevenge.core.apktool.UserProcessBuilder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

/**
 * Created by Solitario on 18/05/2017.
 */
public class UiBuilder {
    private static UiBuilder INSTANCE;

    private SystemTray tray;
    private TrayIcon trayIcon;

    private UiBuilder() {
    }

    public static UiBuilder getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UiBuilder();
        }
        return INSTANCE;
    }


    public void createPanel() {
        JFrame frame = new JFrame();//creating instance of JFrame

        JButton b = new JButton("click");//creating instance of JButton
        b.setBounds(130, 100, 100, 40);//x axis, y axis, width, height

        frame.add(b);//adding button in JFrame

        frame.setSize(300, 200);
        frame.setLayout(null);//using no layout managers
        frame.setVisible(true);//making the frame visible
    }


    public void setStaticImageTray() {
        URL imageURL = UiBuilder.class.getClassLoader().getResource("images/tray.png");
        Image image = new ImageIcon(imageURL, "").getImage();
        trayIcon.setImage(image);
    }

    public void setAnimatedImageTray() {
        URL imageURL = UiBuilder.class.getClassLoader().getResource("images/tray-anim.gif");
        Image image = new ImageIcon(imageURL, "").getImage();
        trayIcon.setImage(image);
    }


    public void createSystemTray() throws IOException {
        //Check the SystemTray support
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            return;
        }
        final PopupMenu popup = new PopupMenu();

        URL imageURL = UiBuilder.class.getClassLoader().getResource("images/tray-anim.gif");
        Image image = new ImageIcon(imageURL, "").getImage();
        trayIcon = new TrayIcon(image);
        trayIcon.setImageAutoSize(true);

        tray = SystemTray.getSystemTray();

        // Create a popup menu components
        MenuItem openItem = new MenuItem("Open in browser");
        MenuItem aboutItem = new MenuItem("About");

        /*
        CheckboxMenuItem cb1 = new CheckboxMenuItem("Set auto size");
        CheckboxMenuItem cb2 = new CheckboxMenuItem("Set tooltip");

        Menu displayMenu = new Menu("Display");
        MenuItem errorItem = new MenuItem("Error");
        MenuItem warningItem = new MenuItem("Warning");
        MenuItem infoItem = new MenuItem("Info");
        MenuItem noneItem = new MenuItem("None");
        */

        MenuItem exitItem = new MenuItem("Exit");

        // settings menu
        //Menu settingsMenu = new Menu("Settings");
        //MenuItem portItem = new MenuItem("Edit port");



        //Add components to popup menu
        popup.add(openItem);
        popup.addSeparator();
        popup.add(aboutItem);
        popup.addSeparator();
        /*

        popup.addSeparator();
        //popup.add(cb1);
        //popup.add(cb2);
        popup.addSeparator();

        // add settings menu
        popup.add(settingsMenu);
        settingsMenu.add(portItem);

        popup.addSeparator();


        popup.add(displayMenu);
        displayMenu.add(errorItem);
        displayMenu.add(warningItem);
        displayMenu.add(infoItem);
        displayMenu.add(noneItem);

        popup.addSeparator();
        */

        popup.add(exitItem);


        trayIcon.setPopupMenu(popup);

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.out.println("TrayIcon could not be added.");
            return;
        }
        // Tray tooltip on mouse hover
        trayIcon.setToolTip("RevEnge Studio");

        trayIcon.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                URL imageURL = UiBuilder.class.getClassLoader().getResource("images/tray.png");
                ImageIcon image = new ImageIcon(imageURL, "");
                JOptionPane.showMessageDialog(null,
                        "RevEnge Studio running on localhost port 49500","INFORMATION",
                        JOptionPane.INFORMATION_MESSAGE, image);
            }
        });

        openItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    // start default browser
                    if (Desktop.isDesktopSupported()) {
                        Desktop desktop = Desktop.getDesktop();
                        if (desktop.isSupported(Desktop.Action.BROWSE)) {
                            // http://localhost:49500/static/public/html/signin.html
                            URI uri = new java.net.URI(Configurator.getInstance().getSPARK_HTTP_PROTOCOL()
                                    + "//localhost:" + Configurator.getInstance().getSPARK_PORT());
                            desktop.browse(uri);
                        }
                    }
                }catch (Exception ex){
                    JOptionPane.showMessageDialog(null,
                            "Could not open browser!");
                }
            }
        });

        aboutItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                URL imageURL = UiBuilder.class.getClassLoader().getResource("images/tray.png");
                ImageIcon image = new ImageIcon(imageURL, "");
                JOptionPane.showMessageDialog(null,
                        "RevEnge Studio running on localhost port 49500","INFORMATION",
                        JOptionPane.INFORMATION_MESSAGE, image);
            }
        });

        /*cb1.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                int cb1Id = e.getStateChange();
                if (cb1Id == ItemEvent.SELECTED) {
                    trayIcon.setImageAutoSize(true);
                } else {
                    trayIcon.setImageAutoSize(false);
                }
            }
        });*/

        /*cb2.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                int cb2Id = e.getStateChange();
                if (cb2Id == ItemEvent.SELECTED) {
                    trayIcon.setToolTip("Sun TrayIcon");
                } else {
                    trayIcon.setToolTip(null);
                }
            }
        });

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MenuItem item = (MenuItem) e.getSource();
                //TrayIcon.MessageType type = null;
                System.out.println(item.getLabel());
                if ("Error".equals(item.getLabel())) {
                    //type = TrayIcon.MessageType.ERROR;
                    trayIcon.displayMessage("Sun TrayIcon Demo",
                            "This is an error message", TrayIcon.MessageType.ERROR);

                } else if ("Warning".equals(item.getLabel())) {
                    //type = TrayIcon.MessageType.WARNING;
                    trayIcon.displayMessage("Sun TrayIcon Demo",
                            "This is a warning message", TrayIcon.MessageType.WARNING);

                } else if ("Info".equals(item.getLabel())) {
                    //type = TrayIcon.MessageType.INFO;
                    trayIcon.displayMessage("Sun TrayIcon Demo",
                            "This is an info message", TrayIcon.MessageType.INFO);

                } else if ("None".equals(item.getLabel())) {
                    //type = TrayIcon.MessageType.NONE;
                    trayIcon.displayMessage("Sun TrayIcon Demo",
                            "This is an ordinary message", TrayIcon.MessageType.NONE);
                } else if ("Edit port".equals(item.getLabel())) {

                }
            }
        };

        errorItem.addActionListener(listener);
        warningItem.addActionListener(listener);
        infoItem.addActionListener(listener);
        noneItem.addActionListener(listener);

        portItem.addActionListener(listener);
        */

        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                int totalProcesses = UserProcessBuilder.getRunningProcessCount() + ScrapperProcessBuilder.getRunningProcessCount();
                if (totalProcesses > 0) {
                    URL imageURL = UiBuilder.class.getClassLoader().getResource("images/tray.png");
                    ImageIcon image = new ImageIcon(imageURL, "");
                    if (JOptionPane.showConfirmDialog(null
                            , "Thera are  " + totalProcesses + " running process(es)\n"
                                    + "Do you want to exit anyway?", "WARNING",
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, image) == JOptionPane.YES_OPTION) {
                        // yes option
                        tray.remove(trayIcon);
                        System.exit(0);
                    } else {
                        // no option
                    }
                } else {
                    tray.remove(trayIcon);
                    System.exit(0);
                }
            }
        });
    }

    public void showTrayMessage(String caption, String text, TrayIcon.MessageType messageTye) {
        // Task tray notification balloon
        if (trayIcon != null) {
            trayIcon.displayMessage(caption, text, messageTye);
        }
    }

    /*public void removeSystemTray(){
        // Task tray notification balloon
        if(tray != null) {
            tray.remove(trayIcon);
        }
    }*/
}
