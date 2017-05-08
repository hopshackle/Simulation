package hopshackle.simulation;

import org.apache.commons.math3.analysis.function.Exp;

import java.util.*;

public class OnInstructionTeacher<A extends Agent> extends Teacher<A> {

    protected List<List<ExperienceRecord<A>>> pastData = new ArrayList<List<ExperienceRecord<A>>>();
    protected int pastDataLimit;
    protected boolean excludeSingleChoiceExperienceRecords;

    public OnInstructionTeacher() {
        this(0, false);
    }

    public OnInstructionTeacher(int pastDataToKeep, boolean excludeSingleChoice) {
        excludeSingleChoiceExperienceRecords = excludeSingleChoice;
        pastDataLimit = pastDataToKeep;
    }

    @Override
    public void processEvent(AgentEvent event) {
        // do nothing
        // we wait for explicit instructions
    }

    private boolean updateData() {
        List<ExperienceRecord<A>> temp = experienceRecordCollector.getAllExperienceRecords();
        List<ExperienceRecord<A>> newER = new ArrayList<>(temp.size());
        if (excludeSingleChoiceExperienceRecords) {
            for (ExperienceRecord<A> er : temp) {
                if (er.isInFinalState() || er.getPossibleActionsFromStartState().size() > 1)
                    newER.add(er);
            }
        } else {
            newER = temp;
        }
        if (newER.isEmpty()) return false;
        pastData.add(newER);
        if (pastData.size() > pastDataLimit + 1) {
            pastData.remove(0);
        }
        experienceRecordCollector.clearAllExperienceRecord();
        return true;
    }

    public void teach() {
        if (updateData()) {
            List<ExperienceRecord<A>> allDataForTraining = getLastNDataSets();
            Agent a = allDataForTraining.get(0).getAgent(); // just to get maxScore
            for (Decider<A> d : decidersToTeach) {
   //             System.out.println(this.toString() + " teaching " + d.toString() + " with " + allDataForTraining.size() + " records.");
                d.learnFromBatch(allDataForTraining, a.getMaxScore());
            }
        }
    }

    protected List<ExperienceRecord<A>> getLastNDataSets() {
        List<ExperienceRecord<A>> retValue = new ArrayList<ExperienceRecord<A>>();
        for (int i = 0; i <= pastDataLimit && i < pastData.size(); i++) {
            retValue.addAll(pastData.get(i));
        }
        return retValue;
    }

}
