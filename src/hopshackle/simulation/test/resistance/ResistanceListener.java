package hopshackle.simulation.test.resistance;

import hopshackle.simulation.games.*;
import hopshackle.simulation.games.resistance.*;
import java.util.*;

public class ResistanceListener implements GameListener<ResistancePlayer> {

    public Map<Integer, List<GameEvent<ResistancePlayer>>> data;

    public ResistanceListener(Resistance game) {
        data = new HashMap<>();
        for (int i = 1; i <= game.getAllPlayers().size(); i++) {
            data.put(i, new ArrayList<>());
        }
        game.registerListener(this);
    }

    @Override
    public void processGameEvent(GameEvent<ResistancePlayer> event) {
        for (int playerRef : event.visibleTo()) {
            data.get(playerRef).add(event);
        }
    }
}
