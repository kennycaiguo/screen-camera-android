package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 2017/3/20.
 */

public class ShiftCodeColorMLConfig extends ShiftCodeMLConfig {
    public ShiftCodeColorMLConfig() {
        borderLength = new DistrictConfig<>(1);
        paddingLength = new DistrictConfig<>(2, 0, 2, 0);
        metaLength = new DistrictConfig<>(1, 0, 1, 0);

        mainWidth = 60;
        mainHeight = 60;

        blockLengthInPixel = 10;

        borderBlock = new DistrictConfig<Block>(new BlackWhiteBlock());
        metaBlock = new DistrictConfig<Block>(new BlackWhiteBlock());
        mainBlock = new DistrictConfig<Block>(new ColorShiftBlock(new int[]{1,2}));

        hints.put(ShiftCodeColorML.KEY_SIZE_RS_ERROR_CORRECTION,12);
        hints.put(ShiftCodeColorML.KEY_LEVEL_RS_ERROR_CORRECTION,0.1);
        hints.put(ShiftCodeColorML.KEY_NUMBER_RAPTORQ_SOURCE_BLOCKS,1);
        hints.put(ShiftCodeColorML.KEY_NUMBER_RANDOM_BARCODES,100);
    }
}
