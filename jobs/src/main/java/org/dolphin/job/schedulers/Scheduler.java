package org.dolphin.job.schedulers;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by hanyanan on 2015/9/25.
 *
 * 可以运行task的调度器
 */
public interface Scheduler {

    public void pause();


    public void resume();

    /**
     * Schedules an Action for execution.
     *
     * @param runnable
     *            the Runnable to schedule
     * @return a subscription to be able to unsubscribe the action (unschedule it if not executed)
     */
    Future schedule(final Runnable runnable);

    /**
     * Schedules an Action for execution at some point in the future.
     * <p>
     * Note to implementors: non-positive {@code delayTime} should be regarded as undelayed schedule, i.e.,
     * as if the {@link #schedule(Runnable)} was called.
     *
     * @param runnable
     *            the Runnable to schedule
     * @param delayTime
     *            time to wait before executing the action; non-positive values indicate an undelayed
     *            schedule
     * @param unit
     *            the time unit of {@code delayTime}
     * @return a subscription to be able to unsubscribe the action (unschedule it if not executed)
     */
    Future schedule(final Runnable runnable, final long delayTime, final TimeUnit unit);

    /**
     * Schedules a cancelable action to be executed periodically. This default implementation schedules
     * recursively and waits for actions to complete (instead of potentially executing long-running actions
     * concurrently). Each scheduler that can do periodic scheduling in a better way should override this.
     * <p>
     * Note to implementors: non-positive {@code initialTime} and {@code period} should be regarded as
     * undelayed scheduling of the first and any subsequent executions.
     *
     * @param runnable
     *            the Runnable to execute periodically
     * @param initialDelay
     *            time to wait before executing the action for the first time; non-positive values indicate
     *            an undelayed schedule
     * @param period
     *            the time interval to wait each time in between executing the action; non-positive values
     *            indicate no delay between repeated schedules
     * @param unit
     *            the time unit of {@code period}
     * @return a subscription to be able to unsubscribe the action (unschedule it if not executed)
     */
    Future schedulePeriodically(final Runnable runnable, long initialDelay, long period, TimeUnit unit);
}
