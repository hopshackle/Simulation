package hopshackle.simulation;

import java.util.*;

import hopshackle.utilities.*;

public class LogisticDecider<A extends Agent> extends BaseStateDecider<A> {
	
	Map<String, LogisticRegression> regressors = new HashMap<String, LogisticRegression>();

	public LogisticDecider(StateFactory<A> stateFactory) {
		super(stateFactory);
	}

	public void addRegressor(String key, LogisticRegression regressor) {
		regressors.put(key, regressor);
	}

	public LogisticRegression getRegressor(String action) {
		return regressors.getOrDefault(action, null);
	}

	@Override
	public void learnFrom(ExperienceRecord<A> exp, double maxResult) {
		throw new AssertionError("Not yet implemented for LogisticDecider");
	}

	@Override
	public double valueOption(ActionEnum<A> option, State<A> state) {
		double[] stateArray = state.getAsArray();
		LogisticRegression reg  = regressors.get(option.toString());
		if (reg == null) return 0.0;
		return reg.classify(stateArray);
	}

	@Override
	public List<Double> valueOptions(List<ActionEnum<A>> options, State<A> state) {
		List<Double> retValue = new ArrayList<Double>(options.size());
		double[] stateArray = state.getAsArray();
		for (int i = 0; i < options.size(); i++) retValue.add(0.0); 
		for (int i = 0; i < options.size(); i++) {
			LogisticRegression reg  = regressors.get(options.get(i).toString());
			if (reg == null) continue;
			retValue.set(i, reg.classify(stateArray));
		}
		return retValue;
	}


}
