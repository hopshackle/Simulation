package hopshackle.simulation;

import java.util.*;

public class DatabaseWriter<T extends Persistent> {

    private List<String> knownSuffixes = new ArrayList<>();
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

            if (knownSuffixes.contains(tableSuffix)) {
                writeBuffer(thing.getWorld().getDBU());
                // do not recreate tables if we have already processed data for this suffix
                // we just want to incrementally add
            } else {
                updateWorldListeners(thing.getWorld());
                writeBuffer(thing.getWorld().getDBU());

                String sqlDelete = DAO.getTableDeletionSQL(tableSuffix);
                thing.getWorld().updateDatabase(sqlDelete);
                String sqlQuery = DAO.getTableCreationSQL(tableSuffix);
                thing.getWorld().updateDatabase(sqlQuery);

                knownSuffixes.add(tableSuffix);
            }
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
