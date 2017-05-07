package hopshackle.simulation.test;

import static org.junit.Assert.*;

import hopshackle.simulation.*;
import hopshackle.simulation.basic.BasicAgent;
import org.encog.neural.networks.BasicNetwork;
import org.junit.*;

import java.util.*;
import java.io.*;

public class NeuralDeciderTest {

    DeciderProperties localProp;
    List<ActionEnum<Agent>> actions;
    List<GeneticVariable<Agent>> variables;
    NeuralDecider<Agent> decider;
    World w;
    BasicAgent testAgent;

    @Before
    public void setup() {
        localProp = SimProperties.getDeciderProperties("GLOBAL");
        localProp.setProperty("Gamma", "0.90");
        localProp.setProperty("NeuralLearningIterations", "100");
        localProp.setProperty("Lambda", "0.00");
        localProp.setProperty("NeuralDeciderArchitecture", "30");
        localProp.setProperty("NeuralControlSignal", "false");
        localProp.setProperty("TimePeriodForGamma", "1000");
        localProp.setProperty("NeuralMaxOutput", "100");
        localProp.setProperty("NeuralNeuronType", "Linear");
        actions = new ArrayList<ActionEnum<Agent>>();
        actions.add(RightLeft.RIGHT);
        actions.add(RightLeft.LEFT);
        actions.add(RightLeft.DITHER);
        variables = new ArrayList<GeneticVariable<Agent>>();
        variables.add(GeneralQDeciderTest.constantTerm);
        variables.add(GeneralQDeciderTest.gold);

        decider = new NeuralDecider<Agent>(new LinearStateFactory<>(variables), 1.0);
        decider.injectProperties(localProp);

        w = new World();
        w.setCalendar(new FastCalendar(0l));
        testAgent = new BasicAgent(w);
    }

    @Test
    public void checkSetup() {
        BasicNetwork brain = decider.getBrain();
        assertEquals(brain.getInputCount(), 2);
        assertEquals(brain.getOutputCount(), 100);
        assertEquals(brain.getLayerCount(), 3);
        assertEquals(brain.getLayerNeuronCount(1), 30);
    }

    @Test
    public void saveAndReload() {
        testAgent.addGold(0.5);
        List<Double> valuesBefore = decider.valueOptions(actions, decider.getCurrentState(testAgent));
        assertEquals(valuesBefore.size(), 3);
  //      System.out.println(String.format("%.4f, %.4f, %.4f", valuesBefore.get(0), valuesBefore.get(1), valuesBefore.get(2)));

        decider.saveBrain("test", "C://Simulation");
        NeuralDecider<Agent> newDecider = NeuralDecider.createFromFile(new LinearStateFactory<Agent>(variables),
                new File("C://Simulation//test_TEST.brain"), false);

        List<Double> valuesAfter = decider.valueOptions(actions, newDecider.getCurrentState(testAgent));
        assertEquals(valuesAfter.size(), 3);
 //       System.out.println(String.format("%.4f, %.4f, %.4f", valuesBefore.get(0), valuesBefore.get(1), valuesBefore.get(2)));

        for (int i = 0; i < 3; i++) {
            assertEquals(valuesBefore.get(i), valuesAfter.get(i), 0.0001);
        }
    }
}
