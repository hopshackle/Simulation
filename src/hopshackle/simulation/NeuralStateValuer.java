package hopshackle.simulation;

import org.encog.ml.data.MLDataPair;
import org.encog.neural.data.basic.BasicNeuralData;
import org.encog.neural.data.basic.BasicNeuralDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.structure.NeuralStructure;
import org.encog.neural.networks.training.propagation.Propagation;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
import org.encog.neural.networks.training.propagation.quick.QuickPropagation;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;

import java.io.*;
import java.util.*;

public class NeuralStateValuer<A extends Agent> extends NeuralDecider<A> {

    public NeuralStateValuer(StateFactory<A> stateFactory, double scaleFactor) {
        super(stateFactory, scaleFactor);
    }

    @Override
    public void injectProperties(DeciderProperties dp) {
        super.injectProperties(dp);
        brain = BrainFactory.initialiseBrain(stateFactory.getVariables().size(), 1, decProp);
    }

    protected double[] getTarget(ExperienceRecord<A> exp) {
        /*
        NeuralDecider would then use ControlSignal or multiple output neurons to answer this question (value of best action)
        In the case of a state valuer we can just value states. So, we know the end state from the ER, but we cannot
        calculate a best action from it - the point is that could change as we learn.
        So, the best target we can use is the actual end state itself.

        We value endStateAsArray, discount appropriately, and use this plus the reward as our target.
         */
        double discountPeriod = exp.getDiscountPeriod();
        double target = exp.getMonteCarloReward()[exp.getAgentNumber()] / scaleFactor;
        if (!monteCarlo) {
            target = exp.getReward()[exp.getAgentNumber()] / scaleFactor;
            BasicNeuralData inputDataForEnd = new BasicNeuralData(exp.getEndStateAsArray());
            double[] prediction = brain.compute(inputDataForEnd).getData();
            double endStateValue = prediction[0];
            target = target / scaleFactor + Math.pow(gamma, discountPeriod) * endStateValue;
        }
        if (target > 1.0) target = 1.0;
        if (target < -1.0) target = -1.0;

        double[] retValue = new double[1];
        retValue[0] = target;
        return retValue;
    }

}
