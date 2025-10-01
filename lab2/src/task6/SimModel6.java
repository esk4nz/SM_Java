package task6;

import tasks1_2.*;

import java.util.ArrayList;

public class SimModel6 {
    public static void main(String[] args) {
        double delayCreate = 2.0;

        double dP1 = 3.0;
        double dP2 = 2.2;
        double dP3 = 4.0;

        Create c = new Create("CREATOR", delayCreate);

        Process p1 = new Process("P1_ROUTER", dP1);
        Process p2 = new Process("P2_SERVICE", dP2);
        Process p3 = new Process("P3_EXIT", dP3);

        p1.setServers(3);
        p2.setServers(2);
        p3.setServers(3);

        p1.setMaxqueue(3);
        p2.setMaxqueue(2);
        p3.setMaxqueue(5);

        c.setDistribution("exp");
        p1.setDistribution("exp");
        p2.setDistribution("exp");
        p3.setDistribution("exp");

        c.setNextElement(p1);

        p1.addNextElement(p2, 0.7);
        p1.addNextElement(p3, 0.3);

        p2.addNextElement(p1, 0.2);
        p2.addNextElement(p3, 0.8);

        ArrayList<Element> list = new ArrayList<>();
        list.add(c);
        list.add(p1);
        list.add(p2);
        list.add(p3);

        Model model = new Model(list);
        model.simulate(1000.0);
    }
}