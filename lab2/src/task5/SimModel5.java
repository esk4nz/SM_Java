package task5;

import tasks1_2.*;

import java.util.ArrayList;

public class SimModel5 {
    public static void main(String[] args) {
        double delayCreate = 2.0;
        double delayService = 4.0;

        Create c = new Create("CREATOR", delayCreate);
        Process p = new Process("PROCESS_MULTI", delayService);

        p.setServers(3);
        p.setMaxqueue(1);

        c.setNextElement(p);

        c.setDistribution("exp");
        p.setDistribution("exp");

        ArrayList<Element> list = new ArrayList<>();
        list.add(c);
        list.add(p);

        Model model = new Model(list);
        model.simulate(1000.0);
    }
}