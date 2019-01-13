package hopshackle.simulation.games.resistance;
import hopshackle.simulation.*;
import java.util.*;

public class ResistancePlayerDAO implements DAO<ResistancePlayer> {
    @Override
    public String getTableCreationSQL(String tableSuffix) {

        return  "CREATE TABLE IF NOT EXISTS Players_" + tableSuffix +
                " ( id 		INT			PRIMARY KEY AUTO_INCREMENT	,"		+
                " gameID 	INT		        NOT NULL, "	+
                " player    TINYINT		        NOT NULL, " +
                " traitor   TINYINT		NOT NULL,"		+
                " winner    TINYINT 	NOT NULL,"		+
                " deciderName VARCHAR(25) NOT NULL" +
                ");";
    }

    @Override
    public String getTableUpdateSQL(String tableSuffix) {
        return "INSERT INTO Players_" + tableSuffix +
                " (gameID, player, traitor, winner, deciderName) VALUES";
    }

    @Override
    public String getValues(ResistancePlayer player) {

        return String.format(" (%d, %d, %d, %d, '%s')",
                player.getGame().getID(),
                player.getActorRef(),
                player.isTraitor() ? 1 : 0,
                player.getGame().getFinalScores()[player.getActorRef()-1] == 1.0 ? 1 : 0,
                player.getDecider().toString()
        );
    }

    @Override
    public String getTableDeletionSQL(String tableSuffix) {
        return "DROP TABLE IF EXISTS Players_" + tableSuffix + ";";    }
}
