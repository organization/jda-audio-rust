package dev.kiwiyou.jda;

public class UdpLoop {
    private UdpSender sender;
    private long stopperHandle;
    private long loopHandle;

    public UdpLoop(int capacity) {
        this.loopHandle = UdpLoopLibrary.createLoop(capacity);
        this.stopperHandle = UdpLoopLibrary.createStopper(this.loopHandle);
        this.sender = new UdpSender(capacity, UdpLoopLibrary.getSender(this.loopHandle));
    }

    public UdpSender createSender() {
        return sender.derive();
    }

    public void run() {
        UdpLoopLibrary.runLoop(this.loopHandle);
    }

    public void stop() {
        UdpLoopLibrary.signalStop(this.stopperHandle);
    }
}
