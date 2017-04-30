package cn.edu.nju.cs.screencamera;

import android.util.SparseIntArray;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * Created by zhantong on 2016/11/28.
 */

public class ShiftCodeML extends ShiftCode{
    public static int numRandomBarcode=200;
    private boolean isRandom=false;
    public ShiftCodeML(MediateBarcode mediateBarcode, Map<DecodeHintType, ?> hints) {
        super(mediateBarcode, hints);
        if(mediateBarcode.rawImage==null){
            return;
        }
        processBorderDown();
    }
    public boolean getIsRandom(){
        return isRandom;
    }
    private void processBorderDown(){
        processBorderDown(RawImage.CHANNLE_Y);
    }
    private void processBorderDown(int channel){
        int[] content=mediateBarcode.getContent(mediateBarcode.districts.get(Districts.BORDER).get(District.DOWN),channel);
        if(content[2]>binaryThreshold){
            isRandom=true;
        }
    }
    public SparseIntArray[] getVaryBar(){
        return getVaryBar(RawImage.CHANNLE_Y);
    }
    public SparseIntArray[] getVaryBar(int channel){
        Zone leftPadding=mediateBarcode.districts.get(Districts.PADDING).get(District.LEFT);
        Zone rightPadding=mediateBarcode.districts.get(Districts.PADDING).get(District.RIGHT);
        SparseIntArray varyOne=new SparseIntArray();
        leftPadding.scanColumn(0,varyOne,mediateBarcode.transform,mediateBarcode.rawImage,channel);
        rightPadding.scanColumn(0,varyOne,mediateBarcode.transform,mediateBarcode.rawImage,channel);

        SparseIntArray varyTwo=new SparseIntArray();
        leftPadding.scanColumn(1,varyTwo,mediateBarcode.transform,mediateBarcode.rawImage,channel);
        rightPadding.scanColumn(1,varyTwo,mediateBarcode.transform,mediateBarcode.rawImage,channel);
        return new SparseIntArray[]{varyOne,varyTwo};
    }
    public int getTransmitFileLengthInBytes(int channel) throws CRCCheckException{
        int[] content=mediateBarcode.getContent(mediateBarcode.districts.get(Districts.BORDER).get(District.UP),channel);
        System.out.println(mediateBarcode.districts.get(Districts.BORDER).get(District.UP).toJson());
        System.out.println(Arrays.toString(content));
        BitSet data=new BitSet();
        for(int i=0;i<content.length;i++){
            if(content[i]>binaryThreshold){
                data.set(i);
            }
        }
        if(isRandom) {
            int transmitFileLengthInBytes = Utils.bitsToInt(Utils.reverse(data, 32), 32, 0);
            return Utils.grayCodeToInt(transmitFileLengthInBytes);
        }
        int transmitFileLengthInBytes=Utils.bitsToInt(data,32,0);
        int crc=Utils.bitsToInt(data,8,32);
        Utils.crc8Check(transmitFileLengthInBytes,crc);
        return transmitFileLengthInBytes;
    }
    public JsonObject getVaryBarToJson(){
        Gson gson=new Gson();
        JsonObject root=new JsonObject();
        SparseIntArray[] varyBars=getVaryBar();
        root.add("vary bar",gson.toJsonTree(varyBars));
        return root;
    }
    public static List<int[]> randomBarcodeValue(ShiftCodeMLConfig config){
        int bitsPerUnit=config.mainBlock.get(District.MAIN).getBitsPerUnit();
        int bitSetLength=config.mainWidth*config.mainHeight*bitsPerUnit;
        List<BitSet> randomBitSetList=Utils.randomBitSetList(bitSetLength,numRandomBarcode,0);
        List<int[]> randomIntArrayList=new ArrayList<>(randomBitSetList.size());
        for(BitSet randomBitSet:randomBitSetList){
            int[] randomIntArray=new int[bitSetLength/bitsPerUnit];
            for(int i=0,intArrayPos=0;i<bitSetLength;i+=bitsPerUnit,intArrayPos++){
                randomIntArray[intArrayPos]=Utils.bitsToInt(randomBitSet,bitsPerUnit,i);
            }
            randomIntArrayList.add(randomIntArray);
        }
        return randomIntArrayList;
    }
}
