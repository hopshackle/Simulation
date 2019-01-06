package hopshackle.simulation;

import java.util.*;

public class AgentArchive {

    private static boolean switchedOn = false;
    private static Map<Long, Agent> cacheOfTheLiving = new HashMap<>();
    private static Map<Long, String> cacheOfTheDead = new HashMap<>();
    private static Queue<Agent> queueForTheFerryman = new LinkedList<>();
    private static int deadAgentsToHoldInLivingCache = 1000;

    public static void switchOn(boolean on) {
        switchedOn = on;
    }

    public static void newAgent(Agent a) {
        if (switchedOn)
            cacheOfTheLiving.put(a.getUniqueID(), a);
    }

    public static void deathOf(Agent a) {
        if (!switchedOn) return;
		if (queueForTheFerryman.size() >= deadAgentsToHoldInLivingCache) {
			Agent agentToRemoveFromCache = queueForTheFerryman.poll();
			cacheOfTheLiving.remove(agentToRemoveFromCache.getUniqueID());
			cacheOfTheDead.put(agentToRemoveFromCache.getUniqueID(), agentToRemoveFromCache.getWorld().toString());
		}
		queueForTheFerryman.offer(a);
    }


    public static void clearAndResetCacheBuffer(int i) {
        if (i < 1) i = 1;
        deadAgentsToHoldInLivingCache = i;
        queueForTheFerryman.clear();
        cacheOfTheDead.clear();
        cacheOfTheLiving.clear();
    }

    public static Agent getAgent(long uniqueRef) {
        Agent retValue = cacheOfTheLiving.get(uniqueRef);
        return retValue;
    }

    public static Agent getAgent(long uniqueRef, AgentRetriever<?> agentRetriever, World world) {
        Agent firstAttempt = getAgent(uniqueRef);
        if (firstAttempt != null)
            return firstAttempt;

        Agent secondAttempt = null;
        String worldName = cacheOfTheDead.get(uniqueRef);
        if (worldName != null && worldName != "" && agentRetriever != null && world != null) {
            secondAttempt = agentRetriever.getAgent(uniqueRef, worldName, world);
        }
        return secondAttempt;
    }
}
