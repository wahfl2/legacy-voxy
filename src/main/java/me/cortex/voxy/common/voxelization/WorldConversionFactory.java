package me.cortex.voxy.common.voxelization;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import me.cortex.voxy.client.importers.util.ChunkBiomes;
import me.cortex.voxy.client.importers.util.SectionBlockData;
import me.cortex.voxy.client.mixin.interfaces.IEbsExtension;
import me.cortex.voxy.common.world.other.Mapper;
import me.cortex.voxy.common.world.other.Mipper;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class WorldConversionFactory {
    private static final ThreadLocal<Reference2IntOpenHashMap<Block>> BLOCK_CACHE = ThreadLocal.withInitial(Reference2IntOpenHashMap::new);

    //TODO: add a local mapper cache since it should be smaller and faster
    public static VoxelizedSection convert(Mapper stateMapper,
                                           SectionBlockData blockData,
                                           ChunkBiomes biomes,
                                           ILightingSupplier lightSupplier,
                                           int sx,
                                           int sy,
                                           int sz) {
        var blockCache = BLOCK_CACHE.get();

        var section = VoxelizedSection.createEmpty(sx, sy, sz);
        var data = section.section;

        int blockId = -1;
        Block block = null;

        for (int oy = 0; oy < 4; oy++) {
            for (int oz = 0; oz < 4; oz++) {
                for (int ox = 0; ox < 4; ox++) {

                    int biomeId = biomes.getBiomeId(ox * 4, oz * 4);

                    for (int iy = 0; iy < 4; iy++) {
                        for (int iz = 0; iz < 4; iz++) {
                            for (int ix = 0; ix < 4; ix++) {
                                int x = (ox<<2)|ix;
                                int y = (oy<<2)|iy;
                                int z = (oz<<2)|iz;
                                short blockIdd = blockData.getBlockId(x, y, z);
                                short meta = blockData.getMeta(x, y, z);
                                byte light = lightSupplier.supply(x,y,z, state);

                                boolean stateIsAir = Block.getBlockById(blockIdd).getMaterial() == Material.air;

                                if (!(stateIsAir && (light==0))) {
                                    if (block != state) {
                                        if (stateIsAir) {
                                            block = state;
                                            blockId = 0;
                                        } else {
                                            blockId = blockCache.computeIfAbsent(state, stateMapper::getIdForBlockState);
                                            block = state;
                                        }
                                    }
                                    data[G(x, y, z)] = Mapper.composeMappingId(light, blockId, biomeId);
                                }
                            }
                        }
                    }
                }
            }
        }
        return section;
    }

    public static VoxelizedSection convert(Mapper stateMapper,
                                           ExtendedBlockStorage blockData,
                                           ChunkBiomes biomes,
                                           ILightingSupplier lightSupplier,
                                           int sx,
                                           int sy,
                                           int sz) {
        var blockCache = BLOCK_CACHE.get();

        var section = VoxelizedSection.createEmpty(sx, sy, sz);
        var data = section.section;

        IEbsExtension sectionData = (IEbsExtension) blockData;

        int blockId = -1;
        Block block = null;

        for (int oy = 0; oy < 4; oy++) {
            for (int oz = 0; oz < 4; oz++) {
                for (int ox = 0; ox < 4; ox++) {

                    int biomeId = biomes.getBiomeId(ox * 4, oz * 4);

                    for (int iy = 0; iy < 4; iy++) {
                        for (int iz = 0; iz < 4; iz++) {
                            for (int ix = 0; ix < 4; ix++) {
                                int x = (ox<<2)|ix;
                                int y = (oy<<2)|iy;
                                int z = (oz<<2)|iz;
                                short blockIdd = (short) sectionData.getBlockId(x, y, z);
                                short meta = (short) blockData.getExtBlockMetadata(x, y, z);
                                byte light = lightSupplier.supply(x,y,z, state);

                                boolean stateIsAir = Block.getBlockById(blockIdd).getMaterial() == Material.air;

                                if (!(stateIsAir && (light==0))) {
                                    if (block != state) {
                                        if (stateIsAir) {
                                            block = state;
                                            blockId = 0;
                                        } else {
                                            blockId = blockCache.computeIfAbsent(state, stateMapper::getIdForBlockState);
                                            block = state;
                                        }
                                    }
                                    data[G(x, y, z)] = Mapper.composeMappingId(light, blockId, biomeId);
                                }
                            }
                        }
                    }
                }
            }
        }
        return section;
    }

    private static int G(int x, int y, int z) {
        return ((y<<8)|(z<<4)|x);
    }

    private static int H(int x, int y, int z) {
        return ((y<<6)|(z<<3)|x) + 16*16*16;
    }

    private static int I(int x, int y, int z) {
        return ((y<<4)|(z<<2)|x) + 8*8*8 + 16*16*16;
    }

    private static int J(int x, int y, int z) {
        return ((y<<2)|(z<<1)|x) + 4*4*4 + 8*8*8 + 16*16*16;
    }

    //TODO: Instead of this mip section as we are updating the data in the world
    public static void mipSection(VoxelizedSection section, Mapper mapper) {
        var data = section.section;

        //Mip L1
        int i = 0;
        for (int y = 0; y < 16; y+=2) {
            for (int z = 0; z < 16; z += 2) {
                for (int x = 0; x < 16; x += 2) {
                    data[16*16*16 + i++] =
                            Mipper.mip(
                                    data[G(x, y, z)],       data[G(x+1, y, z)],       data[G(x, y, z+1)],      data[G(x+1, y, z+1)],
                                    data[G(x, y+1, z)],   data[G(x+1, y+1, z)],  data[G(x, y+1, z+1)], data[G(x+1, y+1, z+1)],
                                    mapper);
                }
            }
        }

        //Mip L2
        i = 0;
        for (int y = 0; y < 8; y+=2) {
            for (int z = 0; z < 8; z += 2) {
                for (int x = 0; x < 8; x += 2) {
                    data[16*16*16 + 8*8*8 + i++] =
                            Mipper.mip(
                                    data[H(x, y, z)],       data[H(x+1, y, z)],       data[H(x, y, z+1)],      data[H(x+1, y, z+1)],
                                    data[H(x, y+1, z)],  data[H(x+1, y+1, z)],  data[H(x, y+1, z+1)], data[H(x+1, y+1, z+1)],
                                    mapper);
                }
            }
        }

        //Mip L3
        i = 0;
        for (int y = 0; y < 4; y+=2) {
            for (int z = 0; z < 4; z += 2) {
                for (int x = 0; x < 4; x += 2) {
                    data[16*16*16 + 8*8*8 + 4*4*4 + i++] =
                            Mipper.mip(
                                    data[I(x, y, z)],       data[I(x+1, y, z)],       data[I(x, y, z+1)],      data[I(x+1, y, z+1)],
                                    data[I(x, y+1, z)],   data[I(x+1, y+1, z)],  data[I(x, y+1, z+1)], data[I(x+1, y+1, z+1)],
                                    mapper);
                }
            }
        }

        //Mip L4
        data[16*16*16 + 8*8*8 + 4*4*4 + 2*2*2] =
                Mipper.mip(
                        data[J(0, 0, 0)], data[J(1, 0, 0)], data[J(0, 0, 1)], data[J(1, 0, 1)],
                        data[J(0, 1, 0)], data[J(1, 1, 0)], data[J(0, 1, 1)], data[J(1, 1, 1)],
                        mapper);
    }
}
