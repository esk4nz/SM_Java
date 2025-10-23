import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Create extends Element {
    private final List<Element> nextElements = new ArrayList<>();
    private final List<Double>  nextProbs    = new ArrayList<>();
    private final List<Integer> nextPriors   = new ArrayList<>();
    private boolean modeProb  = false;
    private boolean modePrior = false;

    public Create(double delay) {
        super(delay);
        setTnext(0.0);
    }
    public Create(String nameOfElement, double delay) {
        super(nameOfElement, delay);
        setTnext(0.0);
    }

    public void addNextElement(Element next, double probability) {
        if (next == null) throw new IllegalArgumentException("next element cannot be null");
        if (probability < 0) throw new IllegalArgumentException("probability must be >= 0");
        if (modePrior) throw new IllegalStateException("Cannot mix probability with priority mode in Create");
        modeProb = true;
        nextElements.add(next);
        nextProbs.add(probability);
        nextPriors.add(null);
    }

    public void addPriorityRoute(Element next, int priority) {
        if (next == null) throw new IllegalArgumentException("next element cannot be null");
        if (modeProb) throw new IllegalStateException("Cannot mix priority with probability mode in Create");
        modePrior = true;
        nextElements.add(next);
        nextProbs.add(null);
        nextPriors.add(priority);
    }

    public void addRoute(Element next) {
        if (next == null) throw new IllegalArgumentException("next element cannot be null");
        nextElements.add(next);
        nextProbs.add(null);
        nextPriors.add(null);
    }

    @Override
    public void outAct() {
        super.outAct();
        setTnext(getTcurr() + getDelay());

        Element target = chooseTarget();
        if (target == null) target = getNextElement();
        if (target != null) target.inAct();
    }

    private Element chooseTarget() {
        int n = nextElements.size();
        if (n == 0) return null;
        if (modePrior && modeProb) throw new IllegalStateException("Create cannot have both priorities and probabilities");

        if (modePrior) {
            Integer bestLevel = null;
            Element bestEl = null;
            for (int i = 0; i < n; i++) {
                Integer pr = nextPriors.get(i);
                if (pr == null) continue;
                Element t = nextElements.get(i);
                if (t != null && t.canAccept()) {
                    if (bestLevel == null || pr > bestLevel) {
                        bestLevel = pr; bestEl = t;
                    }
                }
            }
            if (bestEl != null) return bestEl;
            Integer maxLevel = nextPriors.stream().filter(p -> p != null).max(Comparator.naturalOrder()).orElse(null);
            if (maxLevel != null) {
                for (int i = 0; i < n; i++) if (maxLevel.equals(nextPriors.get(i))) return nextElements.get(i);
            }
            return null;
        }

        if (modeProb) {
            double sum = 0.0;
            for (Double p : nextProbs) {
                if (p == null) throw new IllegalStateException("Probability mode requires probabilities for all routes");
                sum += p;
            }
            if (Math.abs(sum - 1.0) > 1e-9) throw new IllegalStateException("Sum of routing probabilities must be 1.0");
            double r = Math.random(), acc = 0.0;
            for (int i = 0; i < n - 1; i++) {
                acc += nextProbs.get(i);
                if (r < acc) return nextElements.get(i);
            }
            return nextElements.get(n - 1);
        }

        return nextElements.get((int)(Math.random() * n));
    }
}