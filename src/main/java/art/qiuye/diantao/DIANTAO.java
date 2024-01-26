package art.qiuye.diantao;

import art.qiuye.diantao.hire.Hire;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.HashUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.log.StaticLog;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class DIANTAO {
    public static final Log log= LogFactory.get();
    private double[] DIAMOND=new double[]{1,1,1,2,5,20,20,60};
    private double[] ENERGY=new double[]{0.5,1,1.5,3,5,10,30,60,180};
    private double[] WALK=new double[]{0.5,1,2,3,4,5,10,15,20,30,60,120,240};//m
    private double[] EXPRESS=new double[]{0.75,1,3,5,20,20,20,60};//m
    public static void main(String[] args) throws IOException {
        /*JFrame frame = new JFrame("title");
        frame.setSize(400,600);
        JDesktopPane desktopPane=new JDesktopPane();
        JInternalFrame internalFrame = new JInternalFrame("internal",true,true);
        desktopPane.add(internalFrame);
        frame.setContentPane(desktopPane);
//        frame.add(internalFrame);
        internalFrame.setSize(100,100);
        frame.setVisible(true);
        internalFrame.setVisible(true);
        internalFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);*/
        List<Hire> hires = Hire.generateList(FileUtil.readUtf8String(Config.log_path+"3.json"));
        System.out.println(hires);

    }


    @Test
    public void img() throws IOException {
        BufferedImage bufferedImage = ImageIO.read(new File(Config.screen_path+"4.png"));
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        int capacity = width * height * 4;
        ByteBuffer buffer=ByteBuffer.allocate(capacity);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                buffer.putInt(bufferedImage.getRGB(i,j));
            }
        }
        StaticLog.info("##buffer hash1{} ", DigestUtil.md5Hex(buffer.array()));
        boolean contain_red=false;
        boolean contain_green=false;
        for (int i = 0; i < capacity; i+=4) {
            if(buffer.get(i+1)==(byte) 0xFF && (buffer.get(i+2) & 0xFF) < 0X10){
                contain_red=true;
                System.out.println("red:"+HexUtil.toHex(buffer.getInt(i)));
                break;
            }
            if(buffer.get(i+1)==(byte) 0x00 && buffer.get(i+1)==(byte)0x00 && (buffer.get(i+2) & 0xFF) > 0XD0){
                contain_green=true;
                System.out.println("green:"+HexUtil.toHex(buffer.getInt(i)));
                break;
            }
        }
        StaticLog.info("##buffer hash2{} ", DigestUtil.md5Hex(buffer.array()));
        StaticLog.info("contain_red {}",contain_red);
        StaticLog.info("contain_green {}",contain_green);
        new Color(0, 255, 0, 255);
    }

}

