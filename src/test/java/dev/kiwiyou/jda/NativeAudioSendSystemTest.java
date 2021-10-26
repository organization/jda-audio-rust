package dev.kiwiyou.jda;

import net.dv8tion.jda.api.audio.factory.IPacketProvider;
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.*;
import java.nio.ByteBuffer;
import java.time.Duration;

public class NativeAudioSendSystemTest {
    private NativeAudioSendFactory factory;

    @BeforeEach
    void init() {
        this.factory = new NativeAudioSendFactory();
    }

    @Test
    void testFactorySendPacket() {
        try (var listener = new DatagramSocket()) {
            var target = listener.getLocalSocketAddress();
            var system = this.factory.createSendSystem(new SinglePacketProvider((InetSocketAddress) target));
            system.start();
            var packet = new DatagramPacket(new byte[8], 8);
            Assertions.assertTimeout(Duration.ofMillis(500), () -> listener.receive(packet));
            Assertions.assertArrayEquals(new byte[]{ 1, 2, 3, 4, 5, 6, 7, 8 }, packet.getData());
            system.shutdown();
        } catch(Exception e) {
            Assertions.fail(e);
        }
    }

    static class SinglePacketProvider implements IPacketProvider {
        private final InetSocketAddress target;
        private boolean sent = false;

        public SinglePacketProvider(InetSocketAddress target) {
            this.target = target;
        }

        @NotNull
        @Override
        public String getIdentifier() {
            return null;
        }

        @NotNull
        @Override
        public VoiceChannel getConnectedChannel() {
            return null;
        }

        @NotNull
        @Override
        public DatagramSocket getUdpSocket() {
            return null;
        }

        @NotNull
        @Override
        public InetSocketAddress getSocketAddress() {
            return this.target;
        }

        @Nullable
        @Override
        public ByteBuffer getNextPacketRaw(boolean changeTalking) {
            if (!this.sent) {
                this.sent = true;
                return ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
            } else return null;
        }

        @Nullable
        @Override
        public DatagramPacket getNextPacket(boolean changeTalking) {
            return null;
        }

        @Override
        public void onConnectionError(@NotNull ConnectionStatus status) {

        }

        @Override
        public void onConnectionLost() {

        }
    }
}
