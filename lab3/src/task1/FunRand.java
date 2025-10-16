package task1;

import java.util.Random;

public class FunRand {
    public static double Exp(double timeMean) {
        double a = 0;
        while (a == 0) {
            a = Math.random();
        }
        a = -timeMean * Math.log(a);
        return a;
    }

    public static double Unif(double timeMin, double timeMax) {
        double a = 0;
        while (a == 0) {
            a = Math.random();
        }
        a = timeMin + a * (timeMax - timeMin);
        return a;
    }

    public static double Norm(double timeMean, double timeDeviation) {
        Random r = new Random();
        double a;
        do {
            a = timeMean + timeDeviation * r.nextGaussian();
        } while (a <= 0.0);
        return a;
    }
}