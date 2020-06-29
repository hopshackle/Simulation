package hopshackle.simulation;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class Organisation<T extends Agent> extends Location implements Persistent {

    protected World world;
    private long id;
    private static AtomicLong idFountain = new AtomicLong(1);
    private Map<Long, MembershipDetails> membership;
    private String name;
    private long founder;
    private long founded;
    private long dissolved = 0;

    public Organisation(String name, Location loc, List<T> founders) {
        super(loc);
        world = getWorld();
        if (founders == null)
            founders = new ArrayList<T>();
        this.name = name;
        if (loc != null) {
            world = loc.getWorld();
            founded = world.getCurrentTime();
        }
        membership = new HashMap<Long, MembershipDetails>();
        id = idFountain.getAndIncrement();
        for (T founder : founders) {
            newMember(founder);
        }
        if (!founders.isEmpty() && !(founders.get(0) == null)) {
            founder = founders.get(0).getUniqueID();
        }
    }

    public void newMember(T newMember) {
        if (!isOrHasBeenMember(newMember)) {
            membership.put(newMember.getUniqueID(), new MembershipDetails(newMember, this));
        }
    }

    public void memberLeaves(T exMember) {
        if (isOrHasBeenMember(exMember)) {
            MembershipDetails md = membership.get(exMember.getUniqueID());
            md.membershipTerminates(world.getCurrentTime());
        }
        if (getCurrentSize() == 0) {
            dissolved = world.getCurrentTime();
            setParentLocation(null);
        }
    }

    public boolean isOrHasBeenMember(T agent) {
        return membership.containsKey(agent.getUniqueID());
    }

    public boolean isCurrentMember(T agent) {
        if (isOrHasBeenMember(agent)) {
            MembershipDetails md = membership.get(agent.getUniqueID());
            return !md.hasTerminated();
        }
        return false;
    }

    @Override
    public World getWorld() {
        return world;
    }

    public String getName() {
        return name;
    }

    public long getUniqueID() {
        return id;
    }

    public long getFounder() {
        return founder;
    }

    public long getFounded() {
        return founded;
    }

    public int getCurrentSize() {
        int total = 0;
        for (MembershipDetails md : membership.values()) {
            if (!md.hasTerminated())
                total++;
        }
        return total;
    }

    @Override
    public String toString() {
        return name + " [" + id + "]";
    }

    public boolean isExtant() {
        return (dissolved == 0);
    }

    @SuppressWarnings("unchecked")
    public List<T> getCurrentMembership() {
        List<T> retValue = new ArrayList<T>();
        for (MembershipDetails md : membership.values()) {
            if (!md.hasTerminated()) {
                T member = (T) AgentArchive.getAgent(md.getMember());
                if (member != null)
                    retValue.add(member);
            }
        }

        retValue.sort((m1, m2) -> {
            Long id1 = m1.getUniqueID();
            Long id2 = m2.getUniqueID();
            return (int) (membership.get(id1).getMembershipStart() - membership.get(id2).getMembershipStart());
        });

        return retValue;
    }
}
