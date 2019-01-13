package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;

public class MCTSDecisionDAO implements DAO<MCTSDecision> {
    @Override
    public String getTableCreationSQL(String tableSuffix) {
        return "CREATE TABLE IF NOT EXISTS MCTSDecision_" + tableSuffix +
                " ( id 		    INT			PRIMARY KEY AUTO_INCREMENT	," +
                " totalVisits 	INT		        NOT NULL, " +
                " iterations    INT		        NOT NULL, " +
                " chosenVisits  INT		        NOT NULL," +
                " actionChosen  VARCHAR(25)     NOT NULL," +
                " secondAction  VARCHAR(25)     NOT NULL," +
                " entropy       FLOAT           NOT NULL," +
                " player        TINYINT         NOT NULL," +
                " time          INT             NOT NULL," +
                " percentFirst  FLOAT           NOT NULL," +
                " percentSecond FLOAT           NOT NULL," +
                " actionSize    TINYINT         NOT NULL," +
                " nodes         INT             NOT NULL," +
                " depth         INT             NOT NULL," +
                " decider       VARCHAR(25)     NOT NULL" +
                ");";
    }

    @Override
    public String getTableUpdateSQL(String tableSuffix) {
        return "INSERT INTO MCTSDecision_" + tableSuffix +
                " (totalVisits, iterations, chosenVisits, actionChosen, secondAction, entropy, player, time, " +
                "percentFirst, percentSecond, actionSize, nodes, depth, decider) VALUES";
    }

    @Override
    public String getValues(MCTSDecision decision) {
        return String.format(" (%d, %d, %d, '%s', '%s', %.4f, %d, %d, %.4f, %.4f, %d, %d, %d, '%s')",
                decision.N,
                decision.iterations,
                decision.n,
                decision.actionChosen.toString(),
                decision.actions == 1 ? "" : decision.allActions.get(1),
                decision.entropy,
                decision.player,
                decision.turn,
                decision.n / (double) decision.N,
                (decision.actions == 1 ? 0 : decision.all_n.get(1)) / (double) decision.N,
                decision.actions,
                decision.nodesCreated,
                decision.maxDepth,
                decision.descriptor
        );
    }

    @Override
    public String getTableDeletionSQL(String tableSuffix) {
        return "DROP TABLE IF EXISTS MCTSDecision_" + tableSuffix + ";";
    }
}
