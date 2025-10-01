package tasks3_4;
import tasks1_2.*;

import java.util.ArrayList;

public class SimModel3_4 {
    public static void main(String[] args) {
        double delayCreate = 2.0;
        double delayP1 = 3.0;
        double delayP2 = 1.5;
        double delayP3 = 2.0;

        int maxQ1 = 5, maxQ2 = 5, maxQ3 = 5;

        Create c = new Create("CREATOR", delayCreate);
        Process p1 = new Process("PROCESSOR", delayP1);
        Process p2 = new Process("PROCESSOR", delayP2);
        Process p3 = new Process("PROCESSOR", delayP3);

        c.setNextElement(p1);
        p1.setNextElement(p2);
        p2.setNextElement(p3);

        c.setDistribution("exp");
        p1.setDistribution("exp");
        p2.setDistribution("exp");
        p3.setDistribution("exp");

        p1.setMaxqueue(maxQ1);
        p2.setMaxqueue(maxQ2);
        p3.setMaxqueue(maxQ3);

        ArrayList<Element> list = new ArrayList<>();
        list.add(c);
        list.add(p1);
        list.add(p2);
        list.add(p3);

        Model model = new Model(list);
        model.simulate(1000.0);
    }
}