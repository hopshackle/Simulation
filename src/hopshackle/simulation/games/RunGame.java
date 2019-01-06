package hopshackle.simulation.games;

import hopshackle.simulation.MCTS.*;
import hopshackle.simulation.games.resistance.*;
import hopshackle.simulation.*;
import hopshackle.simulation.metric.StatsCollator;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RunGame {

    public static void main(String[] args) {
        int numberOfPlayers = HopshackleUtilities.getArgument(args, 0, 5);
        int numberOfTraitors = HopshackleUtilities.getArgument(args, 1, 2);
        int numberOfGames = HopshackleUtilities.getArgument(args, 2, 100);
        String propertiesFile = HopshackleUtilities.getArgument(args, 3, "");

        if (!propertiesFile.equals(""))
            SimProperties.setFileLocation(propertiesFile);

        RandomDecider<ResistancePlayer> randomDecider = new RandomDecider<>(new SingletonStateFactory<>());

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss 'on' dd-LLL");
 //       StringBuilder allNames = new StringBuilder();
 //       deciderTypes.iterator().forEachRemaining(allNames::append);
        System.out.println("Starting run at " + dateFormat.format(ZonedDateTime.now(ZoneId.of("UTC")))); // + " for " + allNames.toString());
        StatsCollator.clear();

        for (int i = 0; i < numberOfGames; i++) {
            Set<String> deciderTypes = SimProperties.allDeciderNames();
            List<Decider<ResistancePlayer>> deciders = new ArrayList<>();
            for (String deciderName : deciderTypes) {
                MCTSMasterDecider<ResistancePlayer> mctsDecider = new MCTSMasterDecider<>(new SingletonStateFactory<>(), null, null);
                mctsDecider.injectProperties(SimProperties.getDeciderProperties(deciderName));
                mctsDecider.setName(deciderName);
                deciders.add(mctsDecider);
                Collections.shuffle(deciders);
            }
            Iterator<Decider<ResistancePlayer>> iterator = deciders.iterator();
            Resistance game = new Resistance(numberOfPlayers, numberOfTraitors, new World());
            for (ResistancePlayer player : game.getAllPlayers()) {
                if (!iterator.hasNext()) {
                    iterator = deciders.iterator();
                    player.setDecider(randomDecider);
                } else {
                    player.setDecider(iterator.next());
                }
            }
            game.playGame();
            String winningTeam = game.getFinalScores()[game.getTraitors().get(0) - 1] >= 1.0 ? "TRAITORS" : "LOYALISTS";
            System.out.println(String.format("Finished Game %d with victory for %s", i + 1, winningTeam));
            StatsCollator.addStatistics("TRAITOR_WIN", winningTeam.equals("TRAITORS") ? 1.0 : 0.0);
            List<Integer> traitors = game.getTraitors();
            for (int j = 1; j <= numberOfPlayers; j++) {
                String playerType = traitors.contains(j) ? "TRAITOR" : "LOYALISTS";
                String key = game.getPlayer(j).getDecider().toString() + "|" + playerType + "_SCORE";
                StatsCollator.addStatistics(key, game.finalScores[j - 1]);
            }
        }
        System.out.println(StatsCollator.summaryString());
    }
}
