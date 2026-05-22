package id.ac.ui.cs.advprog.donatjs.monitoring;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/**
 * Custom JFR event emitted for every subscription debit attempt in the nightly scheduler.
 *
 * Each event captures the subscription identity, amount, outcome, and (on failure)
 * the reason. When a JFR recording is active, these events appear in the event browser
 * of JDK Mission Control and can be used to:
 *   - measure per-debit latency as a flame graph / duration histogram
 *   - identify which users / campaigns produce the most failures
 *   - correlate debit timing with JVM GC pauses or lock contention
 *
 * Enable a recording locally:
 *   ./gradlew bootRun -Pprofile
 * This writes build/donatjs-profile.jfr; open with JDK Mission Control.
 */
@Name("donatjs.SubscriptionDebit")
@Label("Subscription Debit")
@Description("Records a single subscription debit attempt by the nightly scheduler")
@Category("DonatJS")
@StackTrace(false)
public class SubscriptionDebitJfrEvent extends Event {

    @Label("Subscription ID")
    public long subscriptionId;

    @Label("User ID")
    public String userId;

    @Label("Campaign ID")
    public long campaignId;

    @Label("Amount (IDR)")
    public long amount;

    @Label("Success")
    public boolean success;

    @Label("Failure Reason")
    public String failureReason;
}
