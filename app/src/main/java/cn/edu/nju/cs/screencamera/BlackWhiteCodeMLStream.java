package cn.edu.nju.cs.screencamera;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.parameters.FECParameters;
import net.fec.openrq.parameters.SerializableParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import cn.edu.nju.cs.screencamera.Logback.CustomMarker;

/**
 * Created by zhantong on 2017/5/11.
 */

public class BlackWhiteCodeMLStream extends StreamDecode {
    private static final String TAG="BlackWhiteCodeMLStream";
    private static final boolean DUMP=true;
    ArrayDataDecoder dataDecoder=null;
    int raptorQSymbolSize =-1;
    int numRandomBarcode;
    BarcodeConfig barcodeConfig;

    static Logger LOG= LoggerFactory.getLogger(MainActivity.class);

    List<int[]> randomIntArrayList;

    public BlackWhiteCodeMLStream(){
        barcodeConfig=getBarcodeConfigInstance();
        numRandomBarcode=Integer.parseInt(barcodeConfig.hints.get(BlackWhiteCodeML.KEY_NUMBER_RANDOM_BARCODES).toString());
        randomIntArrayList=getBarcodeInstance(new MediateBarcode(getBarcodeConfigInstance())).randomBarcodeValue(getBarcodeConfigInstance(),numRandomBarcode);
    }
    BlackWhiteCodeML getBarcodeInstance(MediateBarcode mediateBarcode){
        return new BlackWhiteCodeML(mediateBarcode);
    }
    BarcodeConfig getBarcodeConfigInstance(){
        return new BlackWhiteCodeMLConfig();
    }
    void sampleContent(BlackWhiteCodeML blackWhiteCodeML){
        blackWhiteCodeML.mediateBarcode.getContent(blackWhiteCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN),RawImage.CHANNLE_Y);
    }
    @Override
    protected void processFrame(RawImage frame) {
        JsonObject barcodeJson=new JsonObject();
        if(frame.getPixels()==null){
            return;
        }
        Log.i(TAG,frame.toString());

        MediateBarcode mediateBarcode;
        try {
            mediateBarcode = new MediateBarcode(frame,getBarcodeConfigInstance(),null,RawImage.CHANNLE_Y);
        } catch (NotFoundException e) {
            Log.i(TAG,"barcode not found");
            return;
        }
        BlackWhiteCodeML blackWhiteCodeML=getBarcodeInstance(mediateBarcode);
        sampleContent(blackWhiteCodeML);
        int overlapSituation=blackWhiteCodeML.getOverlapSituation();
        if(DUMP) {
            JsonObject mainJson=blackWhiteCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN).toJson();
            barcodeJson.add("barcode",mainJson);
            barcodeJson.addProperty("index",blackWhiteCodeML.mediateBarcode.rawImage.getIndex());
            JsonObject varyBarJson=blackWhiteCodeML.getVaryBarToJson();
            barcodeJson.add("varyBar",varyBarJson);
            barcodeJson.addProperty("overlapSituation",overlapSituation);
            barcodeJson.addProperty("isRandom",blackWhiteCodeML.getIsRandom());
            //String jsonString=new Gson().toJson(root);
            //LOG.info(CustomMarker.source,jsonString);
        }
        if(blackWhiteCodeML.getIsRandom()){
            try {
                int index = blackWhiteCodeML.getTransmitFileLengthInBytes();
                System.out.println("random index: " + index);
                barcodeJson.addProperty("randomIndex",index);
                if (index >= numRandomBarcode) {
                    return;
                }
                int[] value = randomIntArrayList.get(index);
                if (DUMP) {
                    barcodeJson.add("value", new Gson().toJsonTree(value));
                    //root.addProperty("index",shiftCodeML.mediateBarcode.rawImage.getIndex());
                    //LOG.info(CustomMarker.processed,new Gson().toJson(barcodeJson));
                }
            } catch (NumberFormatException e) {

            }catch (CRCCheckException e){
                e.printStackTrace();
            }
        }else{
            if(raptorQSymbolSize ==-1){
                raptorQSymbolSize =blackWhiteCodeML.calcRaptorQSymbolSize(blackWhiteCodeML.calcRaptorQPacketSize());
            }
            if(dataDecoder==null){
                try {
                    int head = blackWhiteCodeML.getTransmitFileLengthInBytes();
                    int numSourceBlock=Integer.parseInt(barcodeConfig.hints.get(BlackWhiteCodeML.KEY_NUMBER_RAPTORQ_SOURCE_BLOCKS).toString());
                    FECParameters parameters=FECParameters.newParameters(head, raptorQSymbolSize,numSourceBlock);
                    if(DUMP){
                        JsonObject paramsJson=new JsonObject();
                        SerializableParameters serializableParameters= parameters.asSerializable();
                        paramsJson.addProperty("commonOTI",serializableParameters.commonOTI());
                        paramsJson.addProperty("schemeSpecificOTI",serializableParameters.schemeSpecificOTI());
                        LOG.info(CustomMarker.raptorQMeta,new Gson().toJson(paramsJson));
                    }
                    System.out.println("FECParameters: "+parameters.toString());
                    Log.i(TAG,"data length: "+parameters.dataLengthAsInt()+" symbol length: "+parameters.symbolSize());
                    dataDecoder= OpenRQ.newDecoder(parameters,0);
                } catch (CRCCheckException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
        if(DUMP){
            LOG.info(CustomMarker.processed,new Gson().toJson(barcodeJson));
        }
    }
}
