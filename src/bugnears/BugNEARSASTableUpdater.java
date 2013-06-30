/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bugnears;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author c9cf4fd2NTUT
 */
public class BugNEARSASTableUpdater {

    private String updateSQLTableName;
    private JTable updateTable;
    private Timer timer;

    public BugNEARSASTableUpdater() {
        timer = null;
    }

    public void setUpdateSQLTableName(String updateSQLTableName) {
        this.updateSQLTableName = updateSQLTableName;
    }

    public void setUpdateTable(JTable updateTable) {
        this.updateTable = updateTable;
    }

    public void startUpdate() {
        timer = new Timer("SQLTableUpdateTimer", true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                update();
            }
        }, 5000, 5000);
    }

    public void stopUpdate() {
        if (timer != null) {
            timer.cancel();
        }
        timer = null;
    }

    public synchronized void update() {
        String queryStr;
        try {
            if (updateSQLTableName.equals("ftp")) {
                queryStr = "select * from "
                        + "(select * from bugnerdb.ftp order by time desc limit 50) as temp "
                        + "order by time asc";
                ResultSet RS = SQLConnector.getInstance().query(queryStr);
                DefaultTableModel model = (DefaultTableModel) updateTable.getModel();
                model.setRowCount(0);
                while (RS.next()) {
                    long time = RS.getLong("time");
                    String ISID = RS.getString("ifID");
                    String clientIP = RS.getString("clientIP");
                    String hostIP = RS.getString("hostIP");
                    String fileName = RS.getString("info");
                    model.addRow(new Object[]{time, ISID, clientIP, hostIP, fileName});
                }
            } else if (updateSQLTableName.equals("http")) {
            } else if (updateSQLTableName.equals("voip")) {
            } else if (updateSQLTableName.equals("ab")) {
            }
        } catch (SQLException ex) {
            Logger.getLogger(BugNEARSASTableUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
