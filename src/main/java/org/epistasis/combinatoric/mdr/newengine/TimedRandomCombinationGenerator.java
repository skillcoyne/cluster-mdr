package org.epistasis.combinatoric.mdr.newengine;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TimedRandomCombinationGenerator extends RandomCombinationGenerator
    {
    private Timer t;
    private final long millis;
    private boolean hasNext = true;

    public TimedRandomCombinationGenerator(final List<String> labels,
                                           final int attrCount, final long seed, final long millis)
        {
        super(labels, attrCount, seed);
        this.millis = millis;
        }

    public boolean hasNext()
        {
        return hasNext;
        }

    @Override
    public AttributeCombination next() {
    if (t == null)
        {
        t = new Timer(true);
        t.schedule(new SetDoneTask(), millis);
        }
    return super.next();
    }

    private synchronized void setDone()
        {
        hasNext = false;
        }

    private class SetDoneTask extends TimerTask
        {
        @Override
        public void run() {
        setDone();
        }
        }
    }
