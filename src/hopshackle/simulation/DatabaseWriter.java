package hopshackle.simulation;

import java.util.*;

public class DatabaseWriter<T> {

    private List<String> knownSuffixes = new ArrayList<>();
    private String lastSuffix;
    private StringBuffer buffer;
    private int numberInBuffer;
    private static int bufferLimit = SimProperties.getPropertyAsInteger("DatabaseWriterBufferLimit", "10");
    private DatabaseAccessUtility databaseAccessUtility;
    private DAO<T> DAO;

    public DatabaseWriter(DAO<T> DAO, DatabaseAccessUtility dbu) {
        this.DAO = DAO;
        numberInBuffer = 0;
        databaseAccessUtility = dbu;
        dbu.registerWriter(this);
    }

    public void write(T thing, String tableSuffix) {
        if (lastSuffix == null || !lastSuffix.equals(tableSuffix)) {
            lastSuffix = tableSuffix;

            if (knownSuffixes.contains(tableSuffix)) {
                writeBuffer();
                // do not recreate tables if we have already processed data for this suffix
                // we just want to incrementally add
            } else {
                writeBuffer();

                String sqlDelete = DAO.getTableDeletionSQL(tableSuffix);
                databaseAccessUtility.addUpdate(sqlDelete);
                String sqlQuery = DAO.getTableCreationSQL(tableSuffix);
                databaseAccessUtility.addUpdate(sqlQuery);

                knownSuffixes.add(tableSuffix);
            }
        }

        addToBuffer(thing);
    }

    private void addToBuffer(T thing) {
        if (!buffer.substring(buffer.length() - 6).equals("VALUES"))
            buffer.append(",");

        buffer.append(DAO.getValues(thing));
        numberInBuffer++;
        if (numberInBuffer >= bufferLimit)
            writeBuffer();
    }

    public void writeBuffer() {
        // write if not null
        if (databaseAccessUtility != null && numberInBuffer > 0) {
            if (DAO instanceof DAODuplicateUpdate)
                databaseAccessUtility.addUpdate(buffer.toString() + ((DAODuplicateUpdate) DAO).getOnDuplicateKey());
            else
                databaseAccessUtility.addUpdate(buffer.toString());
        }

        // initialise new buffer
        buffer = new StringBuffer(DAO.getTableUpdateSQL(lastSuffix));

        numberInBuffer = 0;
    }

    public String toString() {
        return lastSuffix;
    }
}
