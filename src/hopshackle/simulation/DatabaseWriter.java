package hopshackle.simulation;

import java.awt.event.*;
import java.util.ArrayList;

public class DatabaseWriter<T extends Persistent> {

    private String lastSuffix;
    private StringBuffer buffer;
    private int numberInBuffer;
    private static int bufferLimit = SimProperties.getPropertyAsInteger("DatabaseWriterBufferLimit", "10");
    private ArrayList<World> worlds;
    private DAO<T> DAO;

    public DatabaseWriter(DAO<T> DAO) {
        this.DAO = DAO;
        numberInBuffer = 0;
        worlds = new ArrayList<World>();
    }

    public void write(T thing, String tableSuffix) {
        if (lastSuffix == null || !lastSuffix.equals(tableSuffix)) {
            lastSuffix = tableSuffix;
            updateWorldListeners(thing.getWorld());
            writeBuffer(thing.getWorld());

            String sqlDelete = DAO.getTableDeletionSQL(tableSuffix);
            thing.getWorld().updateDatabase(sqlDelete);
            String sqlQuery = DAO.getTableCreationSQL(tableSuffix);
            thing.getWorld().updateDatabase(sqlQuery);
        }

        addToBuffer(thing);
    }

    private void updateWorldListeners(World world) {
        if (!worlds.contains(world)) {
            world.addListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    if (arg0.getActionCommand().equals("Death")) {
                        World dyingWorld = (World) arg0.getSource();
                        worlds.remove(dyingWorld); // to prevent heap dumps if DAO keeps historic worlds from being garbage collected
                        if (lastSuffix.equals(dyingWorld.toString())) {
                            writeBuffer(dyingWorld);
                            // only if this is the world we are currently writing characters for
                        }
                    }
                }
            });
        }
    }


    private void addToBuffer(T thing) {
        if (!buffer.substring(buffer.length() - 6).equals("VALUES"))
            buffer.append(",");

        buffer.append(DAO.getValues(thing));
        numberInBuffer++;
        if (numberInBuffer >= bufferLimit)
            writeBuffer(thing.getWorld());
    }

    public void writeBuffer(World w) {
        // write if not null
        if (numberInBuffer > 0) {
            w.updateDatabase(buffer.toString());
        }

        // initialise new buffer
        buffer = new StringBuffer(DAO.getTableUpdateSQL(lastSuffix));

        numberInBuffer = 0;
    }
}
