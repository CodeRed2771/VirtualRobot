package com.coderedrobotics.virtualrobot;

import com.coderedrobotics.libs.VirtualRobot;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author Michael
 */
public class CodeLoader {

    VirtualRobot robot;

    public enum MODE {

        TELEOP, AUTON, TEST
    }
    private MODE mode = MODE.TELEOP;
    private Thread codeThread; // 60 hz
    private boolean run = true;
    private boolean loaded = false;

    public void loadCode(String pathOnDisk, String iterativeRobotPath) {
        try {
            File jarFile = new File(pathOnDisk);
            ClassLoader loader = URLClassLoader.newInstance(new URL[]{jarFile.toURI().toURL()});
            robot = (VirtualRobot) loader.loadClass(iterativeRobotPath).newInstance();
            loaded = true;
            robot.robotInit();
        } catch (MalformedURLException | InstantiationException | IllegalAccessException ex) {
            Logger.getLogger(CodeLoader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            JOptionPane.showMessageDialog(null, "A VirtualRobot object was not found at that location."
                    + "\nPlease check both paths and try again.", 
                    "VirtualRobot Not Found", JOptionPane.ERROR_MESSAGE);
        } catch (ClassCastException ex) {
            JOptionPane.showMessageDialog(null, "Could not load the object at the given path as\n"
                    + "a VirtualRobot (ClassCastException).  \nDid you implement VirtualRobot?", 
                    "Cast Exception", JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(CodeLoader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsatisfiedLinkError | NoClassDefFoundError ex) {
            JOptionPane.showMessageDialog(null, "An error occured while creating an instance\n"
                    + "of the VirtualRobot.  This usually happens when you forget\n"
                    + "to remove extends IterativeRobot or use the Talon, DigitalInput,\n"
                    + "or AnalogInput objects instead of the PWMController, \n"
                    + "VirtualizableDigitalInput, and VirtualizableAnalogInput objects.", 
                    "Instantiation Error", JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(CodeLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setMode(MODE mode) {
        this.mode = mode;
    }

    public void unloadCode() {
        loaded = false;
        run = false;
        robot = null;
        codeThread = null;
    }
    
    public void enable() {
        run = true;
        codeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                switch (mode) {
                    case TELEOP:
                        robot.teleopInit();
                        while (run && loaded) {
                            robot.teleopPeriodic();
                            try {
                                Thread.sleep(15);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(CodeLoader.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        break;
                    case AUTON:
                        robot.autonomousInit();
                        while (run && loaded) {
                            robot.autonomousPeriodic();
                            try {
                                Thread.sleep(15);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(CodeLoader.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        break;
                    case TEST:
                        robot.testInit();
                        while (run && loaded) {
                            robot.testPeriodic();
                            try {
                                Thread.sleep(15);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(CodeLoader.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        break;
                }
            }
        });
        codeThread.start();
    }

    public void disable() {
        run = false;
        codeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                robot.disabledInit();
                while (!run && loaded) {
                    robot.disabledPeriodic();
                    try {
                        Thread.sleep(15);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(CodeLoader.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        codeThread.start();
    }
}
