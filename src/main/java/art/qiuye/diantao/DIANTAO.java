package art.qiuye.diantao;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

public class DIANTAO {
    public static final Log log= LogFactory.get();
    private double[] DIAMOND=new double[]{1,1,1,2,5,20,20,60};
    private double[] ENERGY=new double[]{0.5,1,1.5,3,5,10,30,60,180};
    private double[] WALK=new double[]{0.5,1,2,3,4,5,10,15,20,30,60,120,240};//m
    private double[] EXPRESS=new double[]{0.75,1,3,5,20,20,20,60};//m
    public static void main(String[] args) throws InterruptedException {
        /*Thread t=new Thread(()->{
            while (true){
                System.out.println("1");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        });
        t.start();
        Thread.sleep(3000);
        t.suspend();
        t.suspend();
        System.out.println("t suspend");
        Thread.sleep(3000);
        t.resume();
        System.out.println("t resume");
        Thread.currentThread().join();*/
        String[] ss = new String[]{"1", "2", "3", "4"};
        for (String s : ss) {
            if(s.equals("2")) ss = new String[]{"a", "b", "c"};
            System.out.println(s);
        }

    }



}

