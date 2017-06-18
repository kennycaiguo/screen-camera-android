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
 * Created by zhantong on 2017/5/11.
 */

public class BlackWhiteCodeML extends BlackWhiteCodeWithBar{
    private boolean isRandom=false;
    public BlackWhiteCodeML(MediateBarcode mediateBarcode, Map<DecodeHintType, ?> hints) {
        super(mediateBarcode,hints);
        if(mediateBarcode.rawImage==null){
            return;
        }
        processBorderDown();
    }
    static List<int[]> randomBarcodeValue(BarcodeConfig config,int numRandomBarcode){
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
    private void processBorderDown(){
        processBorderDown(RawImage.CHANNLE_Y);
    }
    private void processBorderDown(int channel){
        int[] content=mediateBarcode.getContent(mediateBarcode.districts.get(Districts.BORDER).get(District.DOWN),channel);
        if(content[2]>binaryThreshold){
            isRandom=true;
        }
    }
    public JsonObject getVaryBarToJson(){
        Gson gson=new Gson();
        JsonObject root=new JsonObject();
        SparseIntArray[] varyBars=getVaryBar();
        root.add("vary bar",gson.toJsonTree(varyBars));
        return root;
    }
    public boolean getIsRandom(){
        return isRandom;
    }
    public int getTransmitFileLengthInBytes(int channel) throws CRCCheckException{
        int[] content=mediateBarcode.getContent(mediateBarcode.districts.get(Districts.BORDER).get(District.UP),channel);
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
}
