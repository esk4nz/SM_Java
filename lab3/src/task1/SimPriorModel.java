package task1;

import java.util.ArrayList;

public class SimPriorModel {
    public static void main(String[] args) {
        Create c = new Create("CREATOR", 2.0);

        Process p1 = new Process("P1_ROUTER", 3.0);
        Process p2 = new Process("P2_SERVICE", 4.0);
        Process p3 = new Process("P3_EXIT", 5.0);

        p1.setServers(3); p1.setMaxqueue(3);
        p2.setServers(2); p2.setMaxqueue(2);
        p3.setServers(2); p3.setMaxqueue(1);

        c.setDistribution("exp");
        p1.setDistribution("exp");
        p2.setDistribution("exp");
        p3.setDistribution("exp");

        c.setNextElement(p1);

        // false = drop: true = block
        p1.addPriorityRoute(p2, 10, true);   // BLOCK
        p1.addPriorityRoute(p3, 5, false);   // DROP

        p2.addNextElement(p3, 0.7, false);   // DROP
        p2.addNextElement(p1, 0.3, true);    // BLOCK


        ArrayList<Element> list = new ArrayList<>();
        list.add(c);
        list.add(p1);
        list.add(p2);
        list.add(p3);

        Model model = new Model(list);
        model.simulate(1000.0);
    }







//    public static void main(String[] args) {
//        // Створення процесів
//        Create c = new Create("CREATOR", 2.0);
//
//        Process p1 = new Process("P1", 3.0);
//        Process p2 = new Process("P2", 6.0);
//        Process p3 = new Process("P3", 4.0);
//        Process p4 = new Process("P4", 2.0);
//
//        // Налаштування серверів і черг
//        p1.setServers(2); p1.setMaxqueue(2);
//        p2.setServers(1); p2.setMaxqueue(1);
//        p3.setServers(2); p3.setMaxqueue(2);
//        p4.setServers(1); p4.setMaxqueue(1);
//
//        // Розподіл затримки
//        c.setDistribution("exp");
//        p1.setDistribution("exp");
//        p2.setDistribution("exp");
//        p3.setDistribution("exp");
//        p4.setDistribution("exp");
//
//        // Create обирає куди піти: (наприклад, ймовірності)
//        c.addNextElement(p1, 0.3);
//        c.addNextElement(p2, 0.3);
//        c.addNextElement(p3, 0.4);
//
//        // p3 має лише один вихід у p4, без блокування (DROP)
//        p3.setNextElement(p4); // DROP
//        p3.setBlockOnFull(false);
//        // p4 — SINK
//
//        ArrayList<Element> list = new ArrayList<>();
//        list.add(c);
//        list.add(p1);
//        list.add(p2);
//        list.add(p3);
//        list.add(p4);
//
//        Model model = new Model(list);
//        model.simulate(1000.0);
//    }


}