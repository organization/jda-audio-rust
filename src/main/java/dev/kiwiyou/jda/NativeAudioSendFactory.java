package dev.kiwiyou.jda;

import net.dv8tion.jda.api.audio.factory.IAudioSendFactory;
import net.dv8tion.jda.api.audio.factory.IAudioSendSystem;
import net.dv8tion.jda.api.audio.factory.IPacketProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.*;

public class NativeAudioSendFactory implements IAudioSendFactory {
    private final Map<NativeAudioSendSystem, Boolean> systems;
    private UdpLoop loop;
    private ScheduledExecutorService packetScheduler;

    public NativeAudioSendFactory() {
        this.systems = new ConcurrentHashMap<>();
    }

    @NotNull
    @Override
    public IAudioSendSystem createSendSystem(@NotNull IPacketProvider packetProvider) {
        return new NativeAudioSendSystem(this, packetProvider);
    }

    private void setupLoop() {
        this.packetScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
        this.packetScheduler.scheduleWithFixedDelay(this::flushAll, 0, 40, TimeUnit.MILLISECONDS);
        this.loop = new UdpLoop(32);
        Thread udpLoop = new Thread(this.loop::run);
        udpLoop.setDaemon(true);
        udpLoop.start();
    }

    public void flushAll() {
        for (NativeAudioSendSystem system : this.systems.keySet()) {
            system.flush();
        }
    }

    public UdpSender addSystem(NativeAudioSendSystem system) {
        if (this.systems.put(system, true) == null) {
            if (this.loop == null) {
                this.setupLoop();
            }
            return this.loop.newSender();
        } else {
            return null;
        }
    }

    public void removeSystem(NativeAudioSendSystem system) {
        this.systems.remove(system);
        if (this.loop != null && this.systems.isEmpty()) {
            this.packetScheduler.shutdown();
            this.loop.stop();
            this.loop = null;
        }
    }

    public void initDebugLogger() {
        UdpLoopLibrary.initDebugLogger();
    }
}
