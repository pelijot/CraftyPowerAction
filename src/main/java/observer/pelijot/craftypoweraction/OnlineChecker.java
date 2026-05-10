package observer.pelijot.craftypoweraction;

import java.util.concurrent.CompletableFuture;

public interface OnlineChecker {
    CompletableFuture<Void> waitForRunning();

    boolean isRunningNow();
}
