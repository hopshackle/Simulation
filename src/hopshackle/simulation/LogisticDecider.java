package hopshackle.simulation;

import java.util.*;

import hopshackle.utilities.*;

public class LogisticDecider<A extends Agent> extends BaseDecider<A> {
	
	Map<ActionEnum<A>, LogisticRegression> regressors = new HashMap<ActionEnum<A>, LogisticRegression>();

	public LogisticDecider(StateFactory<A> stateFactory) {
		super(stateFactory);
	}

	public void addRegressor(ActionEnum<A> key, LogisticRegression regressor) {
		regressors.put(key, regressor);
	}
	public LogisticRegression getRegressor(ActionEnum<A> action) {
		return regressors.getOrDefault(action, null);
	}
	
	@Override
	public List<Double> valueOptions(List<ActionEnum<A>> optionList, A decidingAgent){
		List<Double> retValue = new ArrayList<Double>(optionList.size());
		double[] stateArray = stateFactory.getCurrentState(decidingAgent).getAsArray();
		for (int i = 0; i < optionList.size(); i++) retValue.add(0.0); 
		for (int i = 0; i < optionList.size(); i++) {
			LogisticRegression reg  = regressors.get(optionList.get(i));
			if (reg == null) continue;
			retValue.set(i, reg.classify(stateArray));
		}
		return retValue;
	}

	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		double[] stateArray = stateFactory.getCurrentState(decidingAgent).getAsArray();
		LogisticRegression reg  = regressors.get(option);
		if (reg == null) return 0.0;
		return reg.classify(stateArray);
	}
	

	@Override
	public void learnFrom(ExperienceRecord<A> exp, double maxResult) {
		throw new AssertionError("Not yet implemented for LogisticDecider");
	}


}
