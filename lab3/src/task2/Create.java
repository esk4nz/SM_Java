package task2;

import task1.Element;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class Create extends Element {

    public interface RouteRule {
        boolean isBlocked(Create from, Element to);
    }

    private final List<Element> prioElements = new ArrayList<>();
    private final List<Integer> prioValues   = new ArrayList<>();
    private final List<Boolean> prioBlockOnFull = new ArrayList<>();
    private final List<RouteRule> prioRules  = new ArrayList<>();
    private boolean usePriorityRouting = false;

    private int routeLoss = 0;

    public Create(double delay) {
        super(delay);
        setTnext(0.0);
    }
    public Create(String nameOfElement, double delay) {
        super(nameOfElement, delay);
        setTnext(0.0);
    }

    public void addPriorityRoute(Element next, int priority, Boolean blockOnFullPerRoute, RouteRule rule) {
        if (next == null) throw new IllegalArgumentException("next element cannot be null");
        prioElements.add(next);
        prioValues.add(priority);
        prioBlockOnFull.add(blockOnFullPerRoute != null ? blockOnFullPerRoute : true);
        prioRules.add(rule);
        usePriorityRouting = true;
    }
    public void addPriorityRoute(Element next, int priority) {
        addPriorityRoute(next, priority, true, null);
    }

    public int getRouteLoss() { return routeLoss; }

    @Override
    public void outAct() {
        super.outAct();
        setTnext(getTcurr() + getDelay());

        if (!usePriorityRouting && getNextElement() != null) {
            if (getNextElement().canAccept()) {
                getNextElement().inAct();
            } else {
                routeLoss++;
                getNextElement().onDropFrom(this);
            }
            return;
        }

        Element target = null;
        boolean policyBlock = true;
        boolean routeAvailable = true;

        if (usePriorityRouting) {
            TreeSet<Integer> levelsDesc = new TreeSet<>(Comparator.reverseOrder());
            levelsDesc.addAll(prioValues);

            Element available = null;
            Element dropTarget = null;
            Element lastBlockCandidate = null;

            outer:
            for (Integer level : levelsDesc) {
                for (int i = 0; i < prioElements.size(); i++) {
                    if (!prioValues.get(i).equals(level)) continue;
                    Element e = prioElements.get(i);
                    if (e == null) continue;

                    boolean isBlockFlag = prioBlockOnFull.get(i);
                    RouteRule rule = prioRules.get(i);
                    boolean customBlocked = (rule != null) && rule.isBlocked(this, e);

                    boolean can = e.canAccept();

                    if (can && !customBlocked) {
                        available = e;
                        policyBlock = true;
                        break outer;
                    } else {
                        if (!isBlockFlag && !customBlocked) {
                            dropTarget = e;
                            policyBlock = false;
                            break outer;
                        } else {
                            lastBlockCandidate = e;
                            policyBlock = true;
                        }
                    }
                }
            }

            if (available != null) {
                target = available;
                routeAvailable = true;
            } else if (dropTarget != null) {
                target = dropTarget;
                routeAvailable = false;
                policyBlock = false;
            } else {
                target = lastBlockCandidate;
                routeAvailable = false;
                policyBlock = true;
            }
        }

        if (routeAvailable && target != null) {
            target.inAct();
        } else {
            if (policyBlock) {
                routeLoss++;
                if (target != null) target.onDropFrom(this);
            } else {
                routeLoss++;
                if (target != null) target.onDropFrom(this);
            }
        }
    }
}