package dev.kiwiyou.jda;

public class UdpLoop {
    private final UdpSender sender;
    private final UdpStopper stopper;
    private final long loopHandle;
    private boolean disposed = false;

    public UdpLoop(int capacity) {
        this.loopHandle = UdpLoopLibrary.createLoop(capacity);
        this.stopper = new UdpStopper(UdpLoopLibrary.createStopper(this.loopHandle));
        this.sender = new UdpSender(capacity, UdpLoopLibrary.getSender(this.loopHandle));
    }

    public UdpSender newSender() {
        return sender.derive();
    }

    public void run() {
        this.disposed = true;
        UdpLoopLibrary.runLoop(this.loopHandle);
    }

    public void stop() {
        this.stopper.stop();
    }

    public void dispose() {
        if (!disposed) {
            UdpLoopLibrary.disposeLoop(this.loopHandle);
            this.disposed = true;
        }
    }
}
