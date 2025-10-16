package task3;

import java.util.Random;

public final class RandUtil {
    private static final Random R = new Random();

    private RandUtil() {}

    public static double exp(double mean) {
        double u = 1.0 - R.nextDouble();
        return -mean * Math.log(u);
    }

    public static double unif(double a, double b) {
        return a + (b - a) * R.nextDouble();
    }

    public static double erlang(double mean, int k) {
        double theta = mean / k;
        double sum = 0.0;
        for (int i = 0; i < k; i++) sum += exp(theta);
        return sum;
    }
}