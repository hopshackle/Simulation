package hopshackle.simulation;

import java.awt.event.*;
import java.util.ArrayList;

public class DatabaseWriter<T extends Persistent> {

    private String lastSuffix;
    private StringBuffer buffer;
    private int numberInBuffer;
    private static int bufferLimit = SimProperties.getPropertyAsInteger("DatabaseWriterBufferLimit", "10");
    private ArrayList<DatabaseAccessUtility> dbus;
    private DAO<T> DAO;

    public DatabaseWriter(DAO<T> DAO) {
        this.DAO = DAO;
        numberInBuffer = 0;
        dbus = new ArrayList<>();
    }

    public void write(T thing, String tableSuffix) {
        if (lastSuffix == null || !lastSuffix.equals(tableSuffix)) {
            lastSuffix = tableSuffix;
            updateWorldListeners(thing.getWorld());
            writeBuffer(thing.getWorld().getDBU());

            String sqlDelete = DAO.getTableDeletionSQL(tableSuffix);
            thing.getWorld().updateDatabase(sqlDelete);
            String sqlQuery = DAO.getTableCreationSQL(tableSuffix);
            thing.getWorld().updateDatabase(sqlQuery);
        }

        addToBuffer(thing);
    }

    private void updateWorldListeners(World world) {
        DatabaseAccessUtility dbu = world.getDBU();
        if (dbu != null && !dbus.contains(dbu)) {
            dbus.add(dbu);
            dbu.registerDatabaseWriter(this);
        }
    }

    private void addToBuffer(T thing) {
        if (!buffer.substring(buffer.length() - 6).equals("VALUES"))
            buffer.append(",");

        buffer.append(DAO.getValues(thing));
        numberInBuffer++;
        if (numberInBuffer >= bufferLimit)
            writeBuffer(thing.getWorld().getDBU());
    }

    public void writeBuffer(DatabaseAccessUtility dbu) {
        // write if not null
        if (dbu != null && numberInBuffer > 0) {
            dbu.addUpdate(buffer.toString());
        }

        // initialise new buffer
        buffer = new StringBuffer(DAO.getTableUpdateSQL(lastSuffix));

        numberInBuffer = 0;
    }

    public String toString() {
        return lastSuffix;
    }
}
