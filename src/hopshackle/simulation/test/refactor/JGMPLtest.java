package hopshackle.simulation.test.refactor;


import static org.junit.Assert.*;

import Jama.*;
import hopshackle.simulation.*;
import jgpml.*;
import jgpml.covariancefunctions.*;
import org.junit.*;

import java.util.*;

/**
 * Created by james on 02/08/2017.
 */
public class JGMPLtest {

    /**
     * Created by james on 02/08/2017.
     */

 //   @Test
    public void simpleKernelProcess() {
        // Firstly we construct a Matrix
        double[][] X = new double[10][3];
        double[][] xstar = new double[10][3];
        double[][] Y = new double[10][1];

        for (int i = 0; i < 10; i++) {
            Y[i][0] = Math.random();
            for (int j = 0; j < 3; j++) {
                X[i][j] = Math.random();
                xstar[i][j] = Math.random();
            }
        }

        Matrix Xm = new Matrix(X);
        Matrix ym = new Matrix(Y);

        playWithKernel(new CovNNoneNoise(), Xm, ym, new Matrix(xstar));
    }

   @Test
    public void withKnownPattern() {
        // Firstly we construct a Matrix
        double[][] X = new double[20][1];
        double[][] xstar = new double[20][1];
        double[][] Y = new double[20][1];

        Random r = new Random();
        for (int i = 0; i < 20; i++) {
            Y[i][0] = i/4.0 * i  + r.nextGaussian();
            X[i][0] = i/2.0   + r.nextGaussian();
            xstar[i][0] = i/2.0 + 0.05;
        }

        Matrix Xm = new Matrix(X);
        Matrix ym = new Matrix(Y);

        GaussianProcess gp = playWithKernel(new CovSum(1, new CovSEiso(), new CovNoise()), Xm, ym, new Matrix(xstar));
        // and with this quadratic fit, the level of noise should match that added
        // So...the one additional thinig I nee

       // Now look at the initial points
       Matrix[] ystar = gp.predict(Xm);
       for (int i = 0; i < 20; i++) {
           double x = X[i][0];
           double y = Y[i][0];
           double yprime = ystar[0].get(i, 0);
           double yvar = ystar[1].get(i, 0);
           System.out.println(String.format("X: %.2f, Y: %.2f, Y*: %.2f, sd: %.2f", x, y, yprime, Math.sqrt(yvar)));
       }
    }

    private GaussianProcess playWithKernel(CovarianceFunction kernel, Matrix X, Matrix Y, Matrix xstar) {
        // Now we throw this into a kernel
        GaussianProcess gp = new GaussianProcess(kernel);

        int n = kernel.numParameters();
        double[][] temp = new double[n][1];
        Matrix hyper = new Matrix(temp);
        for (int loop = 0; loop < 10; loop++) {
            gp.train(X, Y, hyper, 1);
            Matrix[] ystar = gp.predict(xstar);
            System.out.println(HopshackleUtilities.formatArray(ystar[0].transpose().getArray()[0], ", ", "%.3f"));
            System.out.println(HopshackleUtilities.formatArray(ystar[1].transpose().getArray()[0], ", ", "%.3f"));
            hyper = gp.logtheta;
            double[] expTheta = new double[n];
            for (int i = 0; i < n; i++)
                expTheta[i] = Math.exp(hyper.get(i, 0));
            System.out.println(HopshackleUtilities.formatArray(hyper.transpose().getArray()[0], ", ", "%.3f"));
            System.out.println(HopshackleUtilities.formatArray(expTheta, ", ", "%.3f"));
        }
        return gp;
    }
}

