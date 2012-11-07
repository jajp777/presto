/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.presto.serde;

import com.facebook.presto.block.Block;
import com.facebook.presto.block.BlockIterable;
import com.facebook.presto.slice.SizeOf;
import com.facebook.presto.slice.Slice;
import com.facebook.presto.slice.SliceInput;
import com.google.common.base.Preconditions;

import java.util.Iterator;

public class BlocksFileReader
        implements BlockIterable
{
    public static BlocksFileReader readBlocks(Slice slice)
    {
        return readBlocks(slice, 0);
    }

    public static BlocksFileReader readBlocks(Slice slice, long startPosition)
    {
        return new BlocksFileReader(slice, startPosition);
    }

    private final BlockEncoding blockEncoding;
    private final Slice blocksSlice;
    private final BlockIterable blockIterable;
    private final BlocksFileStats stats;

    public BlocksFileReader(Slice slice, long positionOffset)
    {
        Preconditions.checkNotNull(slice, "slice is null");
        Preconditions.checkArgument(positionOffset >= 0, "positionOffset is negative");

        // read file footer
        int footerLength = slice.getInt(slice.length() - SizeOf.SIZE_OF_INT);
        int footerOffset = slice.length() - footerLength - SizeOf.SIZE_OF_INT;
        Slice footerSlice = slice.slice(footerOffset, footerLength);
        SliceInput input = footerSlice.getInput();

        // read file encoding
        blockEncoding = BlockEncodings.readBlockEncoding(input);

        // read stats
        stats = BlocksFileStats.deserialize(input);

        blocksSlice = slice.slice(0, footerOffset);
        blockIterable = new EncodedBlockIterable(blockEncoding, blocksSlice, positionOffset);
    }

    public BlockEncoding getEncoding()
    {
        return blockEncoding;
    }

    public BlocksFileStats getStats()
    {
        return stats;
    }

    @Override
    public Iterator<Block> iterator()
    {
        return blockIterable.iterator();
    }
}