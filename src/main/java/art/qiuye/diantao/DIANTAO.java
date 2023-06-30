package art.qiuye.diantao;

import cn.hutool.log.LogFactory;
import cn.hutool.log.dialect.slf4j.Slf4jLog;

import java.io.IOException;

public class DIANTAO {
    public static final Slf4jLog log= (Slf4jLog) LogFactory.get();
    private double[] DIAMOND=new double[]{1,1,1,2,5,20,20,60};
    private double[] ENERGY=new double[]{0.5,1,1.5,3,5,10,30,60,180};
    private double[] WALK=new double[]{0.5,1,2,3,4,5,10,15,20,30,60,120,240};//m
    private double[] EXPRESS=new double[]{0.75,1,3,5,20,20,20,60};//m
    public static void main(String[] args) throws IOException, InterruptedException {
        log.info("log");

    }



}

