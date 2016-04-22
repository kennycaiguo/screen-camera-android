package cn.edu.nju.cs.screencamera;

import android.util.Log;

import java.util.BitSet;
import java.util.HashMap;

/**
 * 保存YUV格式图像的相关信息,如原始像素信息
 * 以及一些对原始图像操作的方法
 */
public class Matrix extends FileToImg{
    protected static final boolean VERBOSE = false;//是否记录详细log
    protected static final String TAG = "Matrix";//log tag
    protected final int imgWidth;//图像宽度
    protected final int imgHeight;//图像高度
    protected final byte[] pixels;//图像每个像素点原始值
    private int threshold = 0;//二值化阈值
    private int[] borders;//图像中二维码的四个顶点坐标值
    protected PerspectiveTransform transform;//透视变换参数
    public int frameIndex;//此图像中二维码的帧编号
    public GrayMatrixNormal grayMatrix;
    public boolean reverse=false;
    protected boolean ordered=true;
    public HashMap<Integer,Integer>[] bars;
    int barCodeWidth;
    int[] border;
    boolean isMixed=true;

    /**
     * 基本构造函数,作为正方形,且无原始像素数据,生成默认值
     *
     * @param dimension 图像边长
     */
    public Matrix(int dimension) {
        this(dimension, dimension);
    }

    /**
     * 构造函数,无原始像素数据,生成默认值
     *
     * @param imgWidth  图像宽度
     * @param imgHeight 图像高度
     */
    public Matrix(int imgWidth, int imgHeight) {
        this.imgWidth = imgWidth;
        this.imgHeight = imgHeight;
        this.pixels = new byte[imgWidth * imgHeight];
    }

    /**
     * 构造函数,有原始数据
     *
     * @param pixels 原始像素数据
     * @param imgWidth  图像宽度
     * @param imgHeight 图像高度
     * @throws NotFoundException 未找到二维码异常
     */
    public Matrix(byte[] pixels, int imgWidth, int imgHeight,int[] initBorder) throws NotFoundException {
        this.pixels = pixels;
        this.imgWidth = imgWidth;
        this.imgHeight = imgHeight;
        this.threshold = getThreshold();
        if(initBorder==null){
            this.borders = findBorder(genInitBorder());
        }
        else {
            try {
                this.borders = findBorder(initBorder);
            } catch (NotFoundException e) {
                this.borders = findBorder(genInitBorder());
            }
        }
    }
    public int[] genInitBorder(){
        int init = 20;
        int left = imgWidth / 2 - init;
        int right = imgWidth / 2 + init;
        int up = imgHeight / 2 - init;
        int down = imgHeight / 2 + init;
        return new int[] {left,up,right,down};
    }
    /**
     * 透视变换
     * 指定透视变换后二维码的四个顶点坐标,结合找到的图像中的二维码顶点坐标进行透视变换
     * 透视变换相当于只是算出一些矩阵参数,不进行具体的像素数据操作
     *
     * @param p1ToX 左上角顶点x值
     * @param p1ToY 左上角顶点y值
     * @param p2ToX 右上角顶点x值
     * @param p2ToY 右上角顶点y值
     * @param p3ToX 右下角顶点x值
     * @param p3ToY 右下角顶点y值
     * @param p4ToX 左下角顶点x值
     * @param p4ToY 左下角顶点y值
     */
    public void perspectiveTransform(float p1ToX, float p1ToY,
                                     float p2ToX, float p2ToY,
                                     float p3ToX, float p3ToY,
                                     float p4ToX, float p4ToY) {
        transform = PerspectiveTransform.quadrilateralToQuadrilateral(p1ToX, p1ToY,
                p2ToX, p2ToY,
                p3ToX, p3ToY,
                p4ToX, p4ToY,
                borders[0], borders[1],
                borders[2], borders[3],
                borders[4], borders[5],
                borders[6], borders[7]);
    }

    /**
     * 获取指定坐标点灰度值
     *
     * @param x x轴,即列
     * @param y y轴,即行
     * @return 返回灰度值
     */
    public int getGray(int x, int y) {
        return -1;
    }

    /**
     * 获取指定坐标点二值化值,即0或1
     * 根据阈值和灰度值判断二值化结果
     *
     * @param x x轴,即列
     * @param y y轴,即行
     * @return 返回二值化值, 即0或1
     */
    public int getBinary(int x, int y) {
        if (getGray(x, y) <= threshold) {
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * 判断指定坐标点二值化值是否与指定值相同
     *
     * @param x     x轴,即列
     * @param y     y轴,即行
     * @param pixel 指定值
     * @return 与指定值相同返回true, 否则返回false
     */
    public boolean pixelEquals(int x, int y, int pixel) {
        return getBinary(x, y) == pixel;
    }
    public boolean pixelIsBlack(int x, int y){
        return pixelEquals(x,y,0);
    }

    /**
     * 获取图像宽度
     *
     * @return 图像宽度
     */
    public int width() {
        return imgWidth;
    }

    /**
     * 获取图像高度
     *
     * @return 图像高度
     */
    public int height() {
        return imgHeight;
    }
    public BitSet getRawHead(){
        int black=grayMatrix.get(0,0);
        grayMatrix.get(0,0);
        int white=grayMatrix.get(0,1);
        grayMatrix.get(0,1);
        int threshold=(black+white)/2;
        System.out.println("black:"+black+"\twhite:"+white+"\tthreshold:"+threshold);
        int length=(frameBlackLength+frameVaryLength+frameVaryTwoLength)*2+contentLength;
        BitSet bitSet=new BitSet();
        for(int i=0;i<length;i++){
            if(grayMatrix.get(i,0)>threshold){
                bitSet.set(i);
            }
        }
        return bitSet;
    }
    public BitSet getHead(int dimensionX, int dimensionY){
        barCodeWidth=dimensionX;
        if(grayMatrix==null){
            initGrayMatrix(dimensionX,dimensionY);
        }
        return getRawHead();
    }
    public void initGrayMatrix(int dimensionX, int dimensionY){
    }
    public boolean isMixed(int dimensionX,int dimensionY,int[] posX,int topY,int bottomY){
        return false;
    }

    public HashMap<Integer,Integer>[] sampleVary(int[] firstColorX,int[] secondColorX,int topY,int bottomY){
        HashMap<Integer,Integer> firstColorMap=new HashMap<>();
        for(int x:firstColorX){
            getVary(firstColorMap,x,topY,bottomY);
        }
        HashMap<Integer,Integer> secondColorMap=new HashMap<>();
        for(int x:secondColorX){
            getVary(secondColorMap,x,topY,bottomY);
        }
        HashMap<Integer,Integer>[] colorBars=new HashMap[2];
        colorBars[0]=firstColorMap;
        colorBars[1]=secondColorMap;
        return colorBars;
    }
    public void getVary(HashMap<Integer,Integer> map,int posX,int topY,int bottomY){
        int length=bottomY-topY;
        Point[] points=new Point[length];

        int index=0;
        for(int y=topY;y<bottomY;y++){
            points[index]=grayMatrix.getPoint(posX,y);
            index++;
        }
        for(int y=points[0].y;y<=points[length-1].y;y++){
            if(!map.containsKey(y)) {
                int x = getX(points, y);
                map.put(y, getGray(x, y));
            }
        }
    }
    public int getX(Point[] points,int y){
        int i;
        for(i=0;i<points.length-1;i++){
            if(y<points[i].y){
                break;
            }
        }
        Point before=points[i-1];
        Point after=points[i];
        if(before.x==after.x){
            return before.x;
        }
        float res=(float)(y-before.y)/(after.y-before.y)*(after.x-before.x)+before.x;
        return Math.round(res);
    }
    public int toBinary(int x,int y,int blackValue,int whiteValue){
        Point orig=grayMatrix.getPoint(x,y);
        int value=orig.value;
        int origY=orig.y;
        int left=bars[0].get(origY);
        int right=bars[1].get(origY);
        int minDistance=10000;
        int index=-1;
        int distance=Math.abs(value-blackValue);
        if(distance<minDistance){
            minDistance=distance;
            index=0;
        }
        distance=Math.abs(value-whiteValue);
        if(distance<minDistance){
            minDistance=distance;
            index=1;
        }
        distance=Math.abs(value-left);
        if(distance<minDistance){
            minDistance=distance;
            if((ordered&&!reverse)||(!ordered&&reverse)){
                index=0;
            }else {
                index=1;
            }
        }
        distance=Math.abs(value-right);
        if(distance<minDistance){
            minDistance=distance;
            if((ordered&&!reverse)||(!ordered&&reverse)){
                index=1;
            }else {
                index=0;
            }
        }
        return index;
    }

    public BitSet getContent(int dimensionX, int dimensionY,int[] firstColorX,int[] secondColorX,int topY,int bottomY){
        return null;
    }
    /**
     * 获取图像的阈值
     * 阈值由灰度值确定,获得阈值的方法为双峰法
     * 对整个图像均匀采样5行,每行采样中间的3/5
     * 得到灰度值的分布,得到两个峰值,再取双峰之间最优值
     *
     * @return 灰度阈值
     * @throws NotFoundException 不能确定阈值则抛出未找到二维码异常
     */
    private int getThreshold() throws NotFoundException {
        int[] buckets = new int[256];

        for (int y = 1; y < 5; y++) {
            int row = imgHeight * y / 5;
            int right = (imgWidth * 4) / 5;
            for (int column = imgWidth / 5; column < right; column++) {
                int gray = getGray(column, row);
                buckets[gray]++;
            }
        }
        int numBuckets = buckets.length;
        int firstPeak = 0;
        int firstPeakSize = 0;
        for (int x = 0; x < numBuckets; x++) {
            if (buckets[x] > firstPeakSize) {
                firstPeak = x;
                firstPeakSize = buckets[x];
            }
        }
        int secondPeak = 0;
        int secondPeakScore = 0;
        for (int x = 0; x < numBuckets; x++) {
            int distanceToFirstPeak = x - firstPeak;
            int score = buckets[x] * distanceToFirstPeak * distanceToFirstPeak;
            if (score > secondPeakScore) {
                secondPeak = x;
                secondPeakScore = score;
            }
        }
        if (firstPeak > secondPeak) {
            int temp = firstPeak;
            firstPeak = secondPeak;
            secondPeak = temp;
        }
        if (secondPeak - firstPeak <= numBuckets / 16) {
            throw new NotFoundException("can't get proper binary threshold");
        }
        int bestValley = 0;
        int bestValleyScore = -1;
        for (int x = firstPeak + 1; x < secondPeak; x++) {
            int fromSecond = secondPeak - x;
            int score = (x - firstPeak) * fromSecond * fromSecond * (firstPeakSize - buckets[x]);
            //int score=fromSecond*fromSecond*(firstPeakSize-buckets[x]);
            if (score > bestValleyScore) {
                bestValley = x;
                bestValleyScore = score;
            }
        }
        if (VERBOSE) {
            Log.d(TAG, "threshold:" + bestValley);
        }
        return bestValley;
    }

    /**
     * 确定边界时需要
     * 判断指定线段中像素是否全部为白色
     * 以下参数可以唯一确定一条线段
     *
     * @param start      线段起点
     * @param end        线段终点
     * @param fixed      线段保持不变的坐标值
     * @param horizontal 线段是否为水平
     * @return 若线段包含有黑点, 则返回true, 否则返回false
     */
    public boolean containsBlack(int start, int end, int fixed, boolean horizontal) {
        if (horizontal) {
            for (int x = start; x <= end; x++) {
                if (pixelIsBlack(x, fixed)) {
                    return true;
                }

            }
        } else {
            for (int y = start; y <= end; y++) {
                if (pixelIsBlack(fixed, y)) {
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * 寻找图像中二维码的四个顶点坐标值
     * 首先采用矩形放大,找到第一个4条边都是全是白色的矩形,这样认为二维码在矩形内
     * 然后对矩形的每条边,逐步向内收缩,同时检查边上的黑点,是否为噪点,若是噪点则忽略,继续收缩
     * 直到碰到第一个属于二维码的黑点,此时认为此黑点是二维码的一个顶点
     * 通过判断此顶点与矩形边界的关系,可以确定此顶点是二维码的具体哪个顶点
     * 这样即可确定出二维码的四个顶点
     *
     * @return 长度为8的数组, 分别存储每个顶点的x和y坐标
     * @throws NotFoundException 能够确定不可能发现二维码时,则抛出未找到二维码异常
     */
    public int[] findBorder(int[] initBorder) throws NotFoundException {
        int left=initBorder[0];
        int up=initBorder[1];
        int right=initBorder[2];
        int down=initBorder[3];
        int leftOrig = left;
        int rightOrig = right;
        int upOrig = up;
        int downOrig = down;
        if (VERBOSE) {
            Log.d(TAG, "border init: up:" + up + "\t" + "right:" + right + "\t" + "down:" + down + "\t" + "left:" + left);
        }
        if (left < 0 || right >= imgWidth || up < 0 || down >= imgHeight) {
            throw new NotFoundException("frame size too small");
        }
        boolean flag;
        while (true) {
            flag = false;
            while (right < imgWidth && containsBlack(up, down, right, false)) {
                right++;
                flag = true;

            }
            while (down < imgHeight && containsBlack(left, right, down, true)) {
                down++;
                flag = true;
            }
            while (left > 0 && containsBlack(up, down, left, false)) {
                left--;
                flag = true;
            }
            while (up > 0 && containsBlack(left, right, up, true)) {
                up--;
                flag = true;
            }
            if (!flag) {
                break;
            }
        }
        if (VERBOSE) {
            Log.d(TAG, "find border: up:" + up + "\t" + "right:" + right + "\t" + "down:" + down + "\t" + "left:" + left);
        }
        if ((left == 0 || up == 0 || right == imgWidth || down == imgHeight) || (left == leftOrig && right == rightOrig && up == upOrig && down == downOrig)) {
            throw new NotFoundException("didn't find any possible bar code");
        }
        int[] vertexs = new int[8];
        left = findVertex(up, down, left, vertexs, 0, 3, false, false);
        if (VERBOSE) {
            Log.d(TAG, "found 1 vertex,left border now is:" + left);
            Log.d(TAG, "vertexes: (" + vertexs[0] + "," + vertexs[1] + ")\t(" + vertexs[2] + "," + vertexs[3] + ")\t(" + vertexs[4] + "," + vertexs[5] + ")\t(" + vertexs[6] + "," + vertexs[7] + ")");
        }
        up = findVertex(left, right, up, vertexs, 0, 1, true, false);

        if (VERBOSE) {
            Log.d(TAG, "found 2 vertex,up border now is:" + up);
            Log.d(TAG, "vertexes: (" + vertexs[0] + "," + vertexs[1] + ")\t(" + vertexs[2] + "," + vertexs[3] + ")\t(" + vertexs[4] + "," + vertexs[5] + ")\t(" + vertexs[6] + "," + vertexs[7] + ")");
        }
        right = findVertex(up, down, right, vertexs, 1, 2, false, true);
        if (VERBOSE) {
            Log.d(TAG, "found 3 vertex,right border now is:" + right);
            Log.d(TAG, "vertexes: (" + vertexs[0] + "," + vertexs[1] + ")\t(" + vertexs[2] + "," + vertexs[3] + ")\t(" + vertexs[4] + "," + vertexs[5] + ")\t(" + vertexs[6] + "," + vertexs[7] + ")");
        }
        down = findVertex(left, right, down, vertexs, 3, 2, true, true);
        if (VERBOSE) {
            Log.d(TAG, "found 4 vertex,down border now is:" + down);
        }
        if (VERBOSE) {
            Log.d(TAG, "vertexes: (" + vertexs[0] + "," + vertexs[1] + ")\t(" + vertexs[2] + "," + vertexs[3] + ")\t(" + vertexs[4] + "," + vertexs[5] + ")\t(" + vertexs[6] + "," + vertexs[7] + ")");
        }
        if (vertexs[0] == 0 || vertexs[2] == 0 || vertexs[4] == 0 || vertexs[6] == 0) {
            throw new NotFoundException("vertexs error");
        }
        border=new int[]{left,up,right,down};
        return vertexs;
    }

    /**
     * 寻找矩形内二维码顶点坐标
     * 方法在findBorder()中已经描述
     *
     * @param b1         较小边界坐标值
     * @param b2         较大边界坐标值
     * @param fixed      需要移动边界,在边界线段上不变的坐标值
     * @param vertexs    存储寻找到的顶点坐标
     * @param p1         可能的顶点编号
     * @param p2         可能的顶点编号
     * @param horizontal 此边是否为水平
     * @param sub        此边的移动方向,即对fix加还是减
     * @return 返回收缩后的矩形边界fixed值
     * @throws NotFoundException 能够确定不可能发现二维码时,则抛出未找到二维码异常
     */
    public int findVertex(int b1, int b2, int fixed, int[] vertexs, int p1, int p2, boolean horizontal, boolean sub) throws NotFoundException {
        int mid = (b2 - b1) / 2;
        boolean checkP1 = vertexs[p1 * 2] == 0;
        boolean checkP2 = vertexs[p2 * 2] == 0;

        if (horizontal) {
            while (true) {
                if (checkP1) {
                    for (int i = 1; i <= mid; i++) {
                        if (pixelIsBlack(b1 + i, fixed) && !isSinglePoint(b1 + i, fixed)) {
                            vertexs[p1 * 2] = b1 + i;
                            vertexs[p1 * 2 + 1] = fixed;
                            return fixed;
                        }
                    }
                }
                if (checkP2) {
                    for (int i = 1; i <= mid; i++) {
                        if (pixelIsBlack(b2 - i, fixed) && !isSinglePoint(b2 - i, fixed)) {
                            vertexs[p2 * 2] = b2 - i;
                            vertexs[p2 * 2 + 1] = fixed;
                            return fixed;
                        }
                    }
                }
                if (sub) {
                    fixed--;
                    if (fixed <= 0) {
                        throw new NotFoundException("didn't find any possible bar code");
                    }
                } else {
                    fixed++;
                    if (fixed >= imgHeight) {
                        throw new NotFoundException("didn't find any possible bar code");
                    }
                }
            }
        } else {
            while (true) {
                if (checkP1) {
                    for (int i = 1; i <= mid; i++) {
                        if (pixelIsBlack(fixed, b1 + i) && !isSinglePoint(fixed, b1 + i)) {
                            vertexs[p1 * 2] = fixed;
                            vertexs[p1 * 2 + 1] = b1 + i;
                            return fixed;
                        }
                    }
                }
                if (checkP2) {
                    for (int i = 1; i <= mid; i++) {
                        if (pixelIsBlack(fixed, b2 - i) && !isSinglePoint(fixed, b2 - i)) {
                            vertexs[p2 * 2] = fixed;
                            vertexs[p2 * 2 + 1] = b2 - i;
                            return fixed;
                        }
                    }
                }
                if (sub) {
                    fixed--;
                    if (fixed <= 0) {
                        throw new NotFoundException("didn't find any possible bar code");
                    }
                } else {
                    fixed++;
                    if (fixed >= imgWidth) {
                        throw new NotFoundException("didn't find any possible bar code");
                    }
                }
            }
        }
    }

    /**
     * 判断此点是否为噪点
     * 此点一定是黑点,通过判断周围8个像素点,是否有超过5个像素点为白色,来确定此点是否为噪点
     * 即中值滤波
     *
     * @param x x轴,即列
     * @param y y轴,即行
     * @return 为噪点则返回true, 否则返回false
     */
    public boolean isSinglePoint(int x, int y) {
        int sum = getBinary(x - 1, y - 1) + getBinary(x, y - 1) + getBinary(x + 1, y - 1) + getBinary(x - 1, y) + getBinary(x + 1, y) + getBinary(x - 1, y + 1) + getBinary(x, y + 1) + getBinary(x + 1, y + 1);
        return sum >= 6;
    }
}