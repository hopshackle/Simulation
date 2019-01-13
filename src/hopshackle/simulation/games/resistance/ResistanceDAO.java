package hopshackle.simulation.games.resistance;

import hopshackle.simulation.*;
import java.util.Arrays;

public class ResistanceDAO implements DAO<Resistance> {
    @Override
    public String getTableCreationSQL(String tableSuffix) {

        return  "CREATE TABLE IF NOT EXISTS Games_" + tableSuffix +
                " ( id 		INT			PRIMARY KEY AUTO_INCREMENT	,"		+
                " players 	TINYINT		        NOT NULL, "	+
                " traitors  TINYINT		        NOT NULL, " +
                " traitorWin TINYINT		NOT NULL,"		+
                " totalMissions TINYINT 	NOT NULL,"		+
                " successfulMissions TINYINT NOT NULL," +
                " gameID    INT             NOT NULL" +
                ");";
    }

    @Override
    public String getTableUpdateSQL(String tableSuffix) {
        return "INSERT INTO Games_" + tableSuffix +
                " (players, traitors, traitorWin, totalMissions, successfulMissions, gameID) VALUES";
    }

    @Override
    public String getValues(Resistance game) {

        return String.format(" (%d, %d, %d, %d, %d, %d)",
                game.getAllPlayers().size(),
                game.getTraitors().size(),
                Arrays.stream(game.getFinalScores()).filter(i -> i == 1.0).count() == game.getTraitors().size() ? 1 : 0,
                game.getMission() - 1,
                game.getSuccessfulMissions(),
                game.getID()
        );
    }

    @Override
    public String getTableDeletionSQL(String tableSuffix) {
        return "DROP TABLE IF EXISTS Games_" + tableSuffix + ";";    }
}
