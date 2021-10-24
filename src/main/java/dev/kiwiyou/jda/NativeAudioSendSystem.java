package dev.kiwiyou.jda;

import net.dv8tion.jda.api.audio.factory.IAudioSendSystem;
import net.dv8tion.jda.api.audio.factory.IPacketProvider;

import java.nio.ByteBuffer;

public class NativeAudioSendSystem implements IAudioSendSystem {
    private final NativeAudioSendFactory factory;
    private final IPacketProvider packetProvider;
    private UdpSender sender;

    public NativeAudioSendSystem(NativeAudioSendFactory factory, IPacketProvider provider) {
        this.factory = factory;
        this.packetProvider = provider;
    }

    public void flush() {
        if (sender == null) {
            return;
        }
        boolean isEmpty = this.sender.isEmpty();
        while (true) {
            ByteBuffer packet = packetProvider.getNextPacketRaw(isEmpty);
            if (packet == null || sender.queuePacket(packet, packetProvider.getSocketAddress())) {
                break;
            }
        }
    }

    @Override
    public void start() {
        this.sender = this.factory.addSystem(this);
    }

    @Override
    public void shutdown() {
        if (sender != null) {
            this.factory.removeSystem(this);
            this.sender.dispose();
            this.sender = null;
        }
    }
}
