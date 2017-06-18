package cn.edu.nju.cs.screencamera;

import android.util.SparseIntArray;

import java.util.BitSet;
import java.util.Map;

import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

/**
 * Created by zhantong on 2017/5/24.
 */

public class BlackWhiteCodeWithBar {
    public static final int OVERLAP_CLEAR_WHITE=0;
    public static final int OVERLAP_CLEAR_BLACK=1;
    public static final int OVERLAP_BLACK_TO_WHITE=2;
    public static final int OVERLAP_WHITE_TO_BLACK=3;

    MediateBarcode mediateBarcode;
    int refWhite;
    int refBlack;
    int threshold;
    int binaryThreshold;
    int overlapSituation;
    Map<DecodeHintType,?> hints;

    public BlackWhiteCodeWithBar(MediateBarcode mediateBarcode, Map<DecodeHintType,?> hints){
        this.mediateBarcode=mediateBarcode;
        this.hints=hints;
        if(mediateBarcode.rawImage==null){
            return;
        }
        processBorderRight();
        processBorderLeft();
        System.out.println("refWhite: "+refWhite+" refBlack: "+refBlack+" threshold: "+threshold);
        System.out.println("overlap: "+overlapSituation);
        //getClearRawContent();
        //getMixedContent();
    }
    public int getOverlapSituation(){
        return overlapSituation;
    }
    public int getTransmitFileLengthInBytes() throws CRCCheckException{
        return getTransmitFileLengthInBytes(RawImage.CHANNLE_Y);
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
        BitSet data=new BitSet();
        for(int i=0;i<content.length;i++){
            if(content[i]>binaryThreshold){
                data.set(i);
            }
        }
        int transmitFileLengthInBytes=Utils.bitsToInt(data,32,0);
        int crc=Utils.bitsToInt(data,8,32);
        Utils.crc8Check(transmitFileLengthInBytes,crc);
        return transmitFileLengthInBytes;
    }
    public void processBorderRight(){
        processBorderRight(RawImage.CHANNLE_Y);
    }
    public void processBorderRight(int channel){
        int[] content=mediateBarcode.getContent(mediateBarcode.districts.get(Districts.BORDER).get(District.RIGHT),channel);
        int sumWhite=0,sumBlack=0;
        int maxWhite=0,minWhite=255;
        int maxBlack=0,minBlack=255;
        for(int i=0;i<content.length;i+=2){
            int currentWhite=content[i];
            int currentBlack=content[i+1];
            sumWhite+=currentWhite;
            sumBlack+=currentBlack;
            if(maxWhite<currentWhite){
                maxWhite=currentWhite;
            }
            if(minWhite>currentWhite){
                minWhite=currentWhite;
            }
            if(maxBlack<currentBlack){
                maxBlack=currentBlack;
            }
            if(minBlack>currentBlack){
                minBlack=currentBlack;
            }
        }
        int numHalfPoints=content.length/2;
        refWhite=sumWhite/numHalfPoints;
        refBlack=sumBlack/numHalfPoints;
        binaryThreshold=(refWhite+refBlack)/2;
        threshold=(maxWhite+maxBlack-minWhite-minBlack)/2;
    }
    public void processBorderLeft(){
        processBorderLeft(RawImage.CHANNLE_Y);
    }
    public void processBorderLeft(int channel){
        int[] content=mediateBarcode.getContent(mediateBarcode.districts.get(Districts.BORDER).get(District.LEFT),channel);
        int mixIndicatorUp=content[0];
        int mixIndicatorDown=content[content.length-1];
        int refBlackExpand=refBlack+threshold;
        int refWhiteExpand=refWhite-threshold;
        if((mixIndicatorUp<=refBlackExpand)&&(mixIndicatorDown<=refBlackExpand)){
            overlapSituation=OVERLAP_CLEAR_BLACK;
        }else if((mixIndicatorUp>=refWhiteExpand)&&(mixIndicatorDown>=refWhiteExpand)){
            overlapSituation=OVERLAP_CLEAR_WHITE;
        }else if(mixIndicatorUp>mixIndicatorDown){
            overlapSituation=OVERLAP_WHITE_TO_BLACK;
        }else{
            overlapSituation=OVERLAP_BLACK_TO_WHITE;
        }
        System.out.println("mix indicator up: "+mixIndicatorUp+" mix indicator down: "+mixIndicatorDown+" refWhiteEx: "+refWhiteExpand+" refBlackEx: "+refBlackExpand);
    }
    public int[][] getRawContents(){
        if(overlapSituation==OVERLAP_CLEAR_WHITE||overlapSituation==OVERLAP_CLEAR_BLACK){
            return new int[][]{getClearRawContent()};
        }else{
            return getMixedRawContent();
        }
    }
    public int[] getClearRawContent(){
        return getClearRawContent(RawImage.CHANNLE_Y);
    }
    public int[] getClearRawContent(int channel){
        Zone zone=mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN);
        ShiftBlock block=(ShiftBlock) zone.getBlock();
        int[] content=mediateBarcode.getContent(zone,channel);
        int[] rawData=new int[zone.widthInBlock*zone.heightInBlock];
        int step=block.getNumSamplePoints();
        int offset=0;
        int rawDataPos=0;
        for(int y=0;y<zone.heightInBlock;y++) {
            for (int x = 0; x < zone.widthInBlock; x++) {
                boolean isWhite=(overlapSituation==OVERLAP_CLEAR_WHITE);
                int value=block.getClear(isWhite,x,y,content,offset);
                offset+=step;
                rawData[rawDataPos]=value;
                rawDataPos++;
            }
        }
        return rawData;
    }
    public int[][] getMixedRawContent(){
        return getMixedRawContent(RawImage.CHANNLE_Y);
    }
    public int[][] getMixedRawContent(int channel){
        Zone zone=mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN);
        BlackWhiteBlock block=(BlackWhiteBlock) zone.getBlock();
        int[] content=mediateBarcode.getContent(zone,channel);
        int[] realSamplePoints=mediateBarcode.getRealSamplePoints(zone);
        SparseIntArray[] varyBars=getVaryBar();
        int[] rawDataPrev=new int[zone.widthInBlock*zone.heightInBlock];
        int[] rawDataNext=new int[zone.widthInBlock*zone.heightInBlock];
        int step=block.getNumSamplePoints();
        int offset=0;
        int offsetCoord=0;
        int rawDataPos=0;
        for(int y=0;y<zone.heightInBlock;y++) {
            for (int x = 0; x < zone.widthInBlock; x++) {
                //boolean isFormerWhite=(overlapSituation==OVERLAP_WHITE_TO_BLACK);
                int[] values=block.getMixed(content[offset],refBlack,refWhite,varyBars[0].get(realSamplePoints[offsetCoord+1]),varyBars[1].get(realSamplePoints[offsetCoord+1]));
                offset+=step;
                offsetCoord+=2;
                rawDataPrev[rawDataPos]=values[0];
                rawDataNext[rawDataPos]=values[1];
                rawDataPos++;
            }
        }
        return new int[][]{rawDataPrev,rawDataNext};
    }
    public int[] rSDecode(int[] rawContent,Zone zone) throws ReedSolomonException {
        int ecSize=-1;
        float ecLevel=0.1f;
        int rawBitsPerUnit=zone.getBlock().getBitsPerUnit();
        if(hints!=null){
            if(hints.containsKey(DecodeHintType.RS_ERROR_CORRECTION_SIZE)){
                ecSize=Integer.parseInt(hints.get(DecodeHintType.RS_ERROR_CORRECTION_SIZE).toString());
            }else {
                throw new IllegalArgumentException();
            }
            if(hints.containsKey(DecodeHintType.RS_ERROR_CORRECTION_LEVEL)){
                ecLevel=Float.parseFloat(hints.get(DecodeHintType.RS_ERROR_CORRECTION_LEVEL).toString());
            }else{
                throw new IllegalArgumentException();
            }
        }
        int numRS=calcNumRS(zone,ecSize);
        int numRSEc=calcNumRSEc(numRS,ecLevel);
        int numRSData=calcNumRSData(numRS,numRSEc);
        int rSDataLengthInUnit=numRSData*ecSize/rawBitsPerUnit;
        int rSRepairLengthInUnit=numRSEc*ecSize/rawBitsPerUnit;
        int[] rsEncodedDataPart=Utils.changeNumBitsPerInt(rawContent,0,rSDataLengthInUnit,rawBitsPerUnit,ecSize);
        int[] rsEncodeRepairPart=Utils.changeNumBitsPerInt(rawContent,rSDataLengthInUnit,rSRepairLengthInUnit,rawBitsPerUnit,ecSize);
        int[] rSEncodedData=Utils.concatIntArray(rsEncodedDataPart,rsEncodeRepairPart);
        Utils.rSDecode(rSEncodedData,numRSEc,ecSize);
        return rSEncodedData;
    }
    protected static int calcNumRSEc(int numRS,float rSEcLevel){
        return (int)Math.floor(numRS*rSEcLevel);
    }
    protected static int calcNumRS(Zone zone,int rSEcSize){
        return zone.getBlock().getBitsPerUnit()*zone.widthInBlock*zone.heightInBlock/rSEcSize;
    }
    protected static int calcNumRSData(int numRS,int numRSEc){
        return numRS-numRSEc;
    }
    protected static int calcNumDataBytes(int numRSData,int rSEcSize){
        return numRSData*rSEcSize/8-8;
    }

    public int calcRaptorQPacketSize(){
        int ecSize=0;
        float ecLevel=0f;
        if(hints!=null){
            if(hints.containsKey(DecodeHintType.RS_ERROR_CORRECTION_SIZE)){
                ecSize=Integer.parseInt(hints.get(DecodeHintType.RS_ERROR_CORRECTION_SIZE).toString());
            }else {
                throw new IllegalArgumentException();
            }
            if(hints.containsKey(DecodeHintType.RS_ERROR_CORRECTION_LEVEL)){
                ecLevel=Float.parseFloat(hints.get(DecodeHintType.RS_ERROR_CORRECTION_LEVEL).toString());
            }else{
                throw new IllegalArgumentException();
            }
        }
        int numRS=calcNumRS(mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN),ecSize);
        int numRSEc=calcNumRSEc(numRS,ecLevel);
        int numRSData=calcNumRSData(numRS,numRSEc);
        return numRSData*ecSize/8;
    }
    public int calcRaptorQSymbolSize(int raptorQPacketSize){
        return raptorQPacketSize-8;
    }
}
