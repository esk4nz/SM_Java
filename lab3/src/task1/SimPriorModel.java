package task1;

import java.util.ArrayList;

public class SimPriorModel {
    public static void main(String[] args) {
        double delayCreate = 2.0;

        double dP1 = 1.5;
        double dP2 = 1.0;
        double dP3 = 40.0; // можеш змінювати для експериментів

        Create c = new Create("CREATOR", delayCreate);

        Process p1 = new Process("P1_ROUTER", dP1);
        Process p2 = new Process("P2_SERVICE", dP2);
        Process p3 = new Process("P3_EXIT", dP3); // термінальний — без виходів

        // Канали
        p1.setServers(3);
        p2.setServers(2);
        p3.setServers(3);

        // Черги
        p1.setMaxqueue(3);
        p2.setMaxqueue(2);
        p3.setMaxqueue(1);

        // Розподіли
        c.setDistribution("exp");
        p1.setDistribution("exp");
        p2.setDistribution("exp");
        p3.setDistribution("exp");

        // Політики виходу
        p1.setBlockOnFull(false); // drop: при переповненні наступника p1 робить routeLoss і атрибутує відмову target'у
        p2.setBlockOnFull(false);  // blocking: p2 чекає на відкриття наступника
        // p3 — SINK, політика ігнорується, бо немає виходів

        // Схема:
        // Create -> p1
        // p1: priority (10)->p2, (5)->p3
        // p2: priority (7)->p3, (3)->p1
        c.setNextElement(p1);

//        p1.addNextElement(p2, 0.7);
//        p1.addNextElement(p3, 0.3);
//        p1.addPriorityRoute(p2, 10);
//        p1.addPriorityRoute(p3, 5);

        p1.addPriorityRoute(p3, 7);
        p1.addPriorityRoute(p2, 3);

//        p2.addPriorityRoute(p3, 7);
//        p2.addPriorityRoute(p1, 3);


        p2.addNextElement(p3, 0.7);
        p2.addNextElement(p1, 0.3);

//        p2.addPriorityRoute(p3, 7);
//        p2.addPriorityRoute(p1, 3);

        ArrayList<Element> list = new ArrayList<>();
        list.add(c);
        list.add(p1);
        list.add(p2);
        list.add(p3);

        Model model = new Model(list);
        model.simulate(1000.0);
    }
}