package tasks1_2;

import java.util.ArrayList;

public class SimModel {
    public static void main(String[] args) {
        Create c = new Create("CREATOR", 2.0);
        Process p = new Process("PROCESSOR", 1.0);

        c.setNextElement(p);

        p.setMaxqueue(1);

        c.setDistribution("exp");
        p.setDistribution("exp");

        ArrayList<Element> list = new ArrayList<>();
        list.add(c);
        list.add(p);

        Model model = new Model(list);
        model.simulate(1000.0);
    }
}