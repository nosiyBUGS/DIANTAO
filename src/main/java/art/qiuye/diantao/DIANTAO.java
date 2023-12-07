package art.qiuye.diantao;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

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
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        BufferedImage image = ImageIO.read(new File(Config.screen_path + "TTT-2023-10-20-12-38-28.png"));
        System.out.println(image);

        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());

    }


    @Test
    public void imgCut() throws IOException {
        BufferedImage bufferedImage = ImageIO.read(new File("G:\\idea_projects\\DIANTAO\\exclude\\screen\\330822-106.jpg"));
        Graphics2D graphics2D = bufferedImage.createGraphics();
        int x1=200,y1=200,x2=400,y2=200,x3=400,y3=400,x4=200,y4=200;
        int w=bufferedImage.getWidth();
        int h=bufferedImage.getHeight();
        graphics2D.setColor(Color.RED);
        graphics2D.fillRect(0,0,w,y1);
        graphics2D.fillRect(0,y3,w,h-y3);
        graphics2D.fillRect(0,0,x1,h);
        graphics2D.fillRect(x2,0,w-x2,h);
//            graphics2D.fillRect(x1,y1,x2-x1,y3-y2);

//        bufferedImage=bufferedImage.getSubimage(200,200,200,200);
        ImageIO.write(bufferedImage, ImgUtil.IMAGE_TYPE_JPG, new File("G:\\idea_projects\\DIANTAO\\exclude\\screen\\out.jpg"));
    }

}

