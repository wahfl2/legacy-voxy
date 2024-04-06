package me.cortex.voxy.common.world.service;

import com.gtnewhorizons.angelica.compat.mojang.ChunkSectionPos;
import it.unimi.dsi.fastutil.Pair;
import me.cortex.voxy.client.importers.util.ChunkBiomes;
import me.cortex.voxy.common.voxelization.ISectionDataProvider;
import me.cortex.voxy.common.voxelization.VoxelizedSection;
import me.cortex.voxy.common.voxelization.WorldConversionFactory;
import me.cortex.voxy.common.world.WorldEngine;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;

public class VoxelIngestService {
    private volatile boolean running = true;
    private final Thread[] workers;

    private final ConcurrentLinkedDeque<Chunk> ingestQueue = new ConcurrentLinkedDeque<>();
    private final Semaphore ingestCounter = new Semaphore(0);

    private final ConcurrentHashMap<Long, Pair<NibbleArray, NibbleArray>> captureLightMap = new ConcurrentHashMap<>(1000,0.75f, 7);

    private final WorldEngine world;
    public VoxelIngestService(WorldEngine world, int workers) {
        this.world = world;

        this.workers = new Thread[workers];
        for (int i = 0; i < workers; i++) {
            var worker = new Thread(this::ingestWorker);
            worker.setDaemon(false);
            worker.setName("Ingest service #" + i);
            worker.start();
            this.workers[i]  = worker;
        }
    }

    private void ingestWorker() {
        while (this.running) {
            this.ingestCounter.acquireUninterruptibly();
            if (!this.running) break;

            Chunk chunk = this.ingestQueue.pop();
            ChunkBiomes biomes = new ChunkBiomes(chunk);

            int i = -1;
            for (var section : chunk.getBlockStorageArray()) {
                i++;

                if (section.isEmpty()) {
                    this.world.insertUpdate(VoxelizedSection.createEmpty(chunk.xPosition, i, chunk.zPosition));
                } else {
                    VoxelizedSection csec = WorldConversionFactory.convert(
                        this.world.getMapper(),
                        (ISectionDataProvider) section,
                        biomes,
                        chunk.xPosition,
                        i,
                        chunk.zPosition
                    );

                    WorldConversionFactory.mipSection(csec, this.world.getMapper());
                    this.world.insertUpdate(csec);
                }
            }
        }
    }

    private static void fetchLightingData(Map<Long, Pair<NibbleArray, NibbleArray>> out, Chunk chunk) {
//        var lightingProvider = chunk.getWorld().getLightingProvider();
//        var blp = lightingProvider.get(LightType.BLOCK);
//        var slp = lightingProvider.get(LightType.SKY);

        int i = -1;
        for (var section : chunk.getBlockStorageArray()) {
            i++;
            if (section == null) continue;
            if (section.isEmpty()) continue;

            var pos = ChunkSectionPos.from(chunk.xPosition, i, chunk.zPosition);
            var bl = section.getBlocklightArray();
            if (bl != null) {
                bl = copyNibbleArray(bl);
            }

            var sl = section.getSkylightArray();
            if (sl != null) {
                sl = copyNibbleArray(sl);
            }

            if (bl == null && sl == null) {
                continue;
            }
            out.put(pos.asLong(), Pair.of(bl, sl));
        }
    }

    private static NibbleArray copyNibbleArray(NibbleArray nibbleArray) {
        return new NibbleArray(nibbleArray.data.clone(), 4);
    }

    public void enqueueIngest(Chunk chunk) {
        fetchLightingData(this.captureLightMap, chunk);
        this.ingestQueue.add(chunk);
        this.ingestCounter.release();
    }

    public int getTaskCount() {
        return this.ingestCounter.availablePermits();
    }

    public void shutdown() {
        boolean anyAlive = false;
        boolean allAlive = true;
        for (var worker : this.workers) {
            anyAlive |= worker.isAlive();
            allAlive &= worker.isAlive();
        }

        if (!anyAlive) {
            System.err.println("Ingest workers already dead on shutdown! this is very very bad, check log for errors from this thread");
            return;
        }
        if (!allAlive) {
            System.err.println("Some ingest workers already dead on shutdown! this is very very bad, check log for errors from this thread");
        }

        //Wait for the ingest to finish
        while (this.ingestCounter.availablePermits() != 0) {
            Thread.onSpinWait();
        }
        //Shutdown
        this.running = false;
        this.ingestCounter.release(1000);
        //Wait for thread to join
        try {
            for (var worker : this.workers) {
                worker.join();
            }
        } catch (InterruptedException e) {throw new RuntimeException(e);}
    }
}
