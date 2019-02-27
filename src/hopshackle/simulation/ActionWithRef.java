package hopshackle.simulation;

public class ActionWithRef<A extends Agent> {
    public ActionEnum<A> actionTaken;
    public int agentRef;

    public ActionWithRef(ActionEnum<A> action, int ref) {
        actionTaken = action;
        agentRef = ref;
    }

    public boolean equals(Object o) {
        if (o instanceof ActionWithRef) {
            ActionWithRef comparator = (ActionWithRef) o;
            if (comparator.actionTaken == null && actionTaken == null && comparator.agentRef == agentRef)
                return true;
            if (comparator.actionTaken.equals(actionTaken) && comparator.agentRef == agentRef)
                return true;
        }
        return false;
    }

    public int hashCode() {
        return 2 + agentRef * 43 + (actionTaken == null ? 1 : actionTaken.hashCode());
    }

    public String toString() {
        return (actionTaken != null ? actionTaken.toString() : "null") + "|" + agentRef;
    }
}
