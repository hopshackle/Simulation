package hopshackle.simulation;

import java.util.List;

/**
 * Created by james on 28/05/2017.
 */
public interface RawDecider<A extends Agent> {

    double[] valueOptions(double[] stateAsArray);

}
