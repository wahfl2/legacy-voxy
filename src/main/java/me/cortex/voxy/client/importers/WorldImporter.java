package me.cortex.voxy.client.importers;

import me.cortex.voxy.client.core.util.ByteBufferBackedInputStream;
import me.cortex.voxy.client.importers.util.ChunkBiomes;
import me.cortex.voxy.client.importers.util.ChunkStreamVersion;
import me.cortex.voxy.client.importers.util.SectionBlockData;
import me.cortex.voxy.common.voxelization.VoxelizedSection;
import me.cortex.voxy.common.voxelization.WorldConversionFactory;
import me.cortex.voxy.common.world.WorldEngine;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import org.lwjgl.system.MemoryUtil;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class WorldImporter {
    public interface UpdateCallback {
        void update(int finished, int outof);
    }

    private final WorldEngine world;
    private final AtomicInteger totalRegions = new AtomicInteger();
    private final AtomicInteger regionsProcessed = new AtomicInteger();

    private volatile boolean isRunning;
    public WorldImporter(WorldEngine worldEngine, World mcWorld) {
        this.world = worldEngine;
    }


    public void shutdown() {
        this.isRunning = false;
        try {this.worker.join();} catch (InterruptedException e) {throw new RuntimeException(e);}
    }

    private Thread worker;
    public void importWorldAsyncStart(File directory, int threads, UpdateCallback updateCallback, Runnable onCompletion) {
        this.worker = new Thread(() -> {
            this.isRunning = true;
            var workers = new ForkJoinPool(threads);
            var files = directory.listFiles();
            for (var file : files) {
                if (!file.isFile()) {
                    continue;
                }
                var name = file.getName();
                var sections = name.split("\\.");
                if (sections.length != 4 || (!sections[0].equals("r")) || (!sections[3].equals("mca"))) {
                    System.err.println("Unknown file: " + name);
                    continue;
                }
                int rx = Integer.parseInt(sections[1]);
                int rz = Integer.parseInt(sections[2]);
                this.totalRegions.addAndGet(1);
                workers.submit(() -> {
                    try {
                        if (!isRunning) {
                            return;
                        }
                        this.importRegionFile(file.toPath(), rx, rz);
                        int regionsProcessedCount = this.regionsProcessed.addAndGet(1);
                        updateCallback.update(regionsProcessedCount, this.totalRegions.get());
                    } catch (
                            Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            workers.shutdown();
            try {
                workers.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {}
            onCompletion.run();
        });
        this.worker.setName("World importer");
        this.worker.start();
    }

    private void importRegionFile(Path file, int x, int z) throws IOException {
        //if (true) return;
        try (var fileStream = FileChannel.open(file, StandardOpenOption.READ)) {
            var sectorsSavesBB = MemoryUtil.memAlloc(8192);
            if (fileStream.read(sectorsSavesBB, 0) != 8192) {
                System.err.println("Header of region file invalid");
                return;
            }
            sectorsSavesBB.rewind();
            var sectorsSaves = sectorsSavesBB.order(ByteOrder.BIG_ENDIAN).asIntBuffer();

            //Find and load all saved chunks
            for (int idx = 0; idx < 1024; idx++) {
                int sectorMeta = sectorsSaves.get(idx);
                if (sectorMeta == 0) {
                    //Empty chunk
                    continue;
                }
                int sectorStart = sectorMeta>>>8;
                int sectorCount = sectorMeta&((1<<8)-1);
                var data = MemoryUtil.memAlloc(sectorCount*4096).order(ByteOrder.BIG_ENDIAN);
                fileStream.read(data, sectorStart*4096L);
                data.flip();
                {
                    int m = data.getInt();
                    byte b = data.get();
                    if (m == 0) {
                        System.err.println("Chunk is allocated, but stream is missing");
                    } else {
                        int n = m - 1;
                        if ((b & 128) != 0) {
                            if (n != 0) {
                                System.err.println("Chunk has both internal and external streams");
                            }
                            System.err.println("Chunk has external stream which is not supported");
                        } else if (n > data.remaining()) {
                            System.err.println("Chunk stream is truncated: expected "+n+" but read " + data.remaining());
                        } else if (n < 0) {
                            System.err.println("Declared size of chunk is negative");
                        } else {
                            try (var decompressedData = this.decompress(b, new ByteBufferBackedInputStream(data))) {
                                if (decompressedData == null) {
                                    System.err.println("Error decompressing chunk data");
                                } else {
                                    var nbt = CompressedStreamTools.read(decompressedData);
                                    this.importChunkNBT(nbt);
                                }
                            }
                        }
                    }
                }

                MemoryUtil.memFree(data);
            }

            MemoryUtil.memFree(sectorsSavesBB);
        }
    }

    private DataInputStream decompress(byte flags, InputStream stream) throws IOException {
        ChunkStreamVersion chunkStreamVersion = ChunkStreamVersion.get(flags);
        if (chunkStreamVersion == null) {
            System.err.println("Chunk has invalid chunk stream version");
            return null;
        } else {
            return new DataInputStream(chunkStreamVersion.wrap(stream));
        }
    }

    private void importChunkNBT(NBTTagCompound chunk) {
        try {
            int x = chunk.getInteger("xPos");
            int z = chunk.getInteger("zPos");
            ChunkBiomes biomes = new ChunkBiomes(chunk.getByteArray("Biomes"));

            NBTTagList tagList = chunk.getTagList("sections", Constants.NBT.TAG_COMPOUND);

            for (int i = 0; i < tagList.tagCount(); i++) {
                NBTTagCompound section = tagList.getCompoundTagAt(i);
                int y = section.getInteger("Y");
                this.importSectionNBT(x, y, z, section, biomes);
            }
        } catch (Exception e) {
            System.err.println("Exception importing world chunk:");
            e.printStackTrace();
        }
    }

    private void importSectionNBT(int x, int y, int z, NBTTagCompound section, ChunkBiomes biomes) {
//        if (section.getCompoundTag("block_states").isEmpty()) {
//            return;
//        }

        VoxelizedSection csec = WorldConversionFactory.convert(
                this.world.getMapper(),
                new SectionBlockData(section),
                biomes,
                x, y, z
        );

        WorldConversionFactory.mipSection(csec, this.world.getMapper());

        this.world.insertUpdate(csec);
        while (this.world.savingService.getTaskCount() > 4000) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
