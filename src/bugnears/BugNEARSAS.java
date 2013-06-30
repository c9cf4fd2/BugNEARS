/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bugnears;

/**
 *
 * @author c9cf4fd2NTUT
 */
public class BugNEARSAS {

    private BugNEARSASGUI GUI;

    public BugNEARSAS() {
        GUI = new BugNEARSASGUI();
    }

    public void start() {
        GUI.setVisible(true);
    }
}
