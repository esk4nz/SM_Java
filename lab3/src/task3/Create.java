package task3;

import task1.Element;

public class Create extends Element {
    private final double meanInterarrival; // 15 хв
    private final double pT1, pT2, pT3;   // 0.5, 0.1, 0.4
    private long nextId = 1L;

    private final Process reception;

    public Create(double meanInterarrival, double pT1, double pT2, double pT3,
                  Process reception) {
        super("ARRIVALS", 0.0);
        this.meanInterarrival = meanInterarrival;
        this.pT1 = pT1; this.pT2 = pT2; this.pT3 = pT3;
        this.reception = reception;
        setTnext(0.0); // перший прихід у t=0
    }

    private Patient.Type sampleType() {
        double u = Math.random();
        if (u < pT1) return Patient.Type.T1;
        if (u < pT1 + pT2) return Patient.Type.T2;
        return Patient.Type.T3;
    }

    @Override
    public void outAct() {
        super.outAct();
        setTnext(getTcurr() + RandUtil.exp(meanInterarrival));
        Patient p = new Patient(nextId++, sampleType(), getTcurr());
        reception.inAct(p);
    }
}