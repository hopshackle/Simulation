package hopshackle.simulation.games;

import hopshackle.simulation.MCTS.*;
import hopshackle.simulation.games.resistance.*;
import hopshackle.simulation.*;
import hopshackle.simulation.metric.StatsCollator;

import javax.xml.crypto.Data;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RunGame {

    public static void main(String[] args) {
        String runName = HopshackleUtilities.getArgument(args, 0, "default");
        int numberOfPlayers = HopshackleUtilities.getArgument(args, 1, 5);
        int numberOfTraitors = HopshackleUtilities.getArgument(args, 2, 2);
        int numberOfGames = HopshackleUtilities.getArgument(args, 3, 100);
        String propertiesFile = HopshackleUtilities.getArgument(args, 4, "");
        if (!propertiesFile.equals("")) {
            SimProperties.setFileLocation(propertiesFile);
        }

        boolean includeRandom = SimProperties.getProperty("ResistanceIncludeRandom", "false").equals("true");
        boolean useSameDeciderForWholeTeam = SimProperties.getProperty("ResistanceSameDeciderForTeam", "false").equals("true");

        RandomDecider<ResistancePlayer> randomDecider = new RandomDecider<>(new SingletonStateFactory<>());
        DatabaseAccessUtility dbu = new DatabaseAccessUtility();
        Thread t = new Thread(dbu);
        t.start();

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss 'on' dd-LLL");
        //       StringBuilder allNames = new StringBuilder();
        //       deciderTypes.iterator().forEachRemaining(allNames::append);
        System.out.println("Starting run at " + dateFormat.format(ZonedDateTime.now(ZoneId.of("UTC")))); // + " for " + allNames.toString());
        StatsCollator.clear();
        DatabaseWriter<Resistance> gameWriter = new DatabaseWriter<>(new ResistanceDAO(), dbu);
        DatabaseWriter<MCTSDecision> mctsWriter = new DatabaseWriter<>(new MCTSDecisionDAO(), dbu);
        DatabaseWriter<ResistancePlayer> playerWriter = new DatabaseWriter<>(new ResistancePlayerDAO(), dbu);

        for (int i = 0; i < numberOfGames; i++) {
            Set<String> deciderTypes = SimProperties.allDeciderNames();
            List<Decider<ResistancePlayer>> deciders = new ArrayList<>();
            if (includeRandom) deciders.add(randomDecider);
            for (String deciderName : deciderTypes) {
                MCTSMasterDecider<ResistancePlayer> mctsDecider;
                switch (SimProperties.getDeciderProperties(deciderName).getProperty("MonteCarloConstructor", "IS-MCTS")) {
                    case "IS-MCTS":
                        mctsDecider = new MCTSMasterDecider<>(new SingletonStateFactory<>(), null, null);
                        break;
                    case "MRIS":
                        mctsDecider = new MRISMCTSDecider<>(new SingletonStateFactory<>(), null, null);
                        break;
                    default:
                        throw new AssertionError("Unknown constructor type " + SimProperties.getDeciderProperties(deciderName).getProperty("MonteCarloConstructor", "IS-MCTS"));
                }
                mctsDecider.injectProperties(SimProperties.getDeciderProperties(deciderName));
                mctsDecider.setName(deciderName);
                mctsDecider.setWriter(mctsWriter, runName);
                deciders.add(mctsDecider);
                Collections.shuffle(deciders);
            }
            Iterator<Decider<ResistancePlayer>> iterator = deciders.iterator();
            Resistance game = new Resistance(numberOfPlayers, numberOfTraitors, new World());
            if (useSameDeciderForWholeTeam && deciders.size() > 1) {
                game.getAllPlayers().stream()
                        .forEach(p -> {
                            Decider<ResistancePlayer> deciderToUse = p.isTraitor() ? deciders.get(0) : deciders.get(1);
                            p.setDecider(deciderToUse);
                        });
            } else {
                for (ResistancePlayer player : game.getAllPlayers()) {
                    if (!iterator.hasNext())
                        iterator = deciders.iterator();

                    player.setDecider(iterator.next());
                }
            }
            game.playGame();
            gameWriter.write(game, runName);
            game.getAllPlayers().forEach(p -> playerWriter.write(p, runName));
            String winningTeam = game.getFinalScores()[game.getTraitors().get(0) - 1] >= 1.0 ? "TRAITORS" : "LOYALISTS";
            System.out.println(String.format("Finished Game %d with victory for %s at %s", i + 1, winningTeam, dateFormat.format(ZonedDateTime.now(ZoneId.of("UTC")))));
            StatsCollator.addStatistics("TRAITOR_WIN", winningTeam.equals("TRAITORS") ? 1.0 : 0.0);
            List<Integer> traitors = game.getTraitors();
            for (int j = 1; j <= numberOfPlayers; j++) {
                String playerType = traitors.contains(j) ? "TRAITOR" : "LOYALISTS";
                String key = game.getPlayer(j).getDecider().toString() + "|" + playerType + "_SCORE";
                StatsCollator.addStatistics(key, game.finalScores[j - 1]);
            }
        }
        dbu.flushWriters();
        dbu.addUpdate("EXIT");
        System.out.println(StatsCollator.summaryString());
    }
}
