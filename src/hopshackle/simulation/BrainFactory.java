package hopshackle.simulation;

import org.encog.engine.network.activation.*;
import org.encog.neural.data.basic.BasicNeuralDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
import java.util.Random;

public class BrainFactory {

	/**
	 * This static function returns a new Feedforward Network. 
	 * The parameter taken is a integer array indicating the number of neurons to
	 * have in each layer. The first entry in the array is the input layer; the last layer is the output layer.
	 * 
	 * @author $James Goodman$
	 * @version $1$
	 */
	public static BasicNetwork newFFNetwork(int[] layers, DeciderProperties properties) {

		boolean initialOptimismTraining = properties.getProperty("NeuralDeciderInitialOptimismTraining", "true").equals("true");
		boolean zeroGaussianRandomiser = properties.getProperty("NeuralDeciderZeroGaussianWeights", "false").equals("true");

		BasicNetwork network = new BasicNetwork();
		int maxLoop = layers.length;
		for (int loop = 0; loop < maxLoop; loop++) {
			ActivationFunction af = new ActivationTANH();
			switch (properties.getProperty("NeuralNeuronType", "TANH")) {
			case "TANH":
				break;
			case "Sigmoid": 
				af = new ActivationSigmoid();
				break;
			case "Linear":
				af = new ActivationLinear();
				break;
			case "ClippedLinear":
				af = new ActivationClippedLinear();
				break;
			default:
				throw new AssertionError("Unknown NeuronType "  + properties.getProperty("NeuralNeuronType", "TANH"));
			}
			network.addLayer(new BasicLayer(af, loop < maxLoop-1, layers[loop]));
		}
		// No bias neuron for output layer

		network.getStructure().finalizeStructure();
		network.reset();
		if (zeroGaussianRandomiser) {
			// we cannot inject one into network.reset(), which always uses Nguyen-Widrow
			Random GR = new Random();
			double[] weights = network.getFlat().getWeights();
			for (int i = 0; i < weights.length; i++) {
				weights[i] = GR.nextGaussian() / 3.0;
			}
			network.getFlat().setWeights(weights);
		}
		if (initialOptimismTraining)
			initialOptimismTraining(network, 1.0);

		return network;
	}

	private static void initialOptimismTraining(BasicNetwork network, double maxValue) {
		// Optimistically train this array
		int inputNeurons = network.getInputCount();
		double[][] trainingInputData = new double[100][inputNeurons];
		double[][] trainingOutputData = new double[100][1];
		for (int n=0; n<100; n++) {
			for (int m=0; m<inputNeurons; m++)
				trainingInputData[n][m] = Math.random()*2.0 - 1.0;
			trainingOutputData[n][0] = (Math.random()/4.0)+ maxValue - 0.25;
		}

		BasicNeuralDataSet trainingSet = new BasicNeuralDataSet(trainingInputData, trainingOutputData);
		Backpropagation trainer = new Backpropagation(network, trainingSet, 0.2, 0.00);
		trainer.iteration();
	}

	public static <A extends Agent> BasicNetwork initialiseBrain(int inputNeurons, int outputNeurons, DeciderProperties properties) {
		String networkArchitecture = properties.getProperty("NeuralDeciderArchitecture", "10");
		// NeuralDeciderArchitecture defines the hidden layers, with colon delimiters.
		String[] layerArchitecture = networkArchitecture.split(":");
		int neuronLayers = layerArchitecture.length;
		int[] layers = new int[neuronLayers+2];

		layers[0] = inputNeurons;
		for (int hiddenLayer = 0; hiddenLayer < neuronLayers; hiddenLayer++) {
			layers[hiddenLayer+1] = Integer.valueOf(layerArchitecture[hiddenLayer]);
		}
		layers[neuronLayers+1] = outputNeurons;	
		return newFFNetwork(layers, properties);
	}

}
