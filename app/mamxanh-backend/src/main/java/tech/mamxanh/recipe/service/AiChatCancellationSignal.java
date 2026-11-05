package tech.mamxanh.recipe.service;

/** Coordinates cancellation with the start of each Agent tool step. */
public final class AiChatCancellationSignal {
    private boolean cancelled;

    public synchronized boolean tryBeginToolStep() {
        return !cancelled;
    }

    public synchronized void cancel() {
        cancelled = true;
    }

    public synchronized boolean isCancelled() {
        return cancelled;
    }
}
