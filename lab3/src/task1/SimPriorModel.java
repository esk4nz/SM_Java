package task1;

import java.util.ArrayList;

public class SimPriorModel {
    public static void main(String[] args) {
        Create c = new Create("CREATOR", 2.0);

        Process p1 = new Process("P1_ROUTER", 3.0);
        Process p2 = new Process("P2_SERVICE", 4.0);
        Process p3 = new Process("P3_EXIT", 5.0);

        // Канали та черги
        p1.setServers(3); p1.setMaxqueue(3);
        p2.setServers(2); p2.setMaxqueue(2);
        p3.setServers(2); p3.setMaxqueue(1);

        // Розподіли
        c.setDistribution("exp");
        p1.setDistribution("exp");
        p2.setDistribution("exp");
        p3.setDistribution("exp");

        // Схема:
        // Create -> p1
        c.setNextElement(p1);

        // p1: до p2 БЛОКУВАННЯ, до p3 ДРОП
        // false = drop: true = block
        p1.addPriorityRoute(p2, 10, false);   // BLOCK
        p1.addPriorityRoute(p3, 5, false);   // DROP

        // p2: ймовірності, різні політики
        p2.addNextElement(p3, 0.7, false);   // DROP
        p2.addNextElement(p1, 0.3, true);    // BLOCK

        // p3 — SINK

        ArrayList<Element> list = new ArrayList<>();
        list.add(c);
        list.add(p1);
        list.add(p2);
        list.add(p3);

        Model model = new Model(list);
        model.simulate(1000.0);
    }
}