package art.qiuye.diantao;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import lombok.SneakyThrows;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

/**
 * @author hy
 * @create 2023/6/29 0029
 */
public class MiniCap {
    private static Log StaticLog = LogFactory.get();


    private static final String push_dir="/data/local/tmp";
    private static final String s_dir=System.getProperty("user.dir") + "/exclude/";
    private int forwardPort;
    private Banner banner = new Banner();

    private  String adb_path;

    private String device;

    public static final String NO_FILE = "No such file";
    public static final String NO_FOUND = "not found";

//    private int read=0;

    public MiniCap(Config config) {
        device = config.devices.get(0);
        adb_path = config.adb_path;
        String[] split = device.split("\\.");
        String[] split1 = split[3].split(":");
        String s = split[0] + split[1] + split[2] + split1[0];
        this.forwardPort= Integer.parseInt(s.substring(s.length() - 4));
    }
    private boolean initial;

    private Process process;
    private boolean init(){
        String s=null;
        s=RuntimeUtil.execForStr(StrUtil.format("{}{}shell ls {}/minicap",adb_path,device,push_dir));
        if(s.contains(NO_FILE) || s.contains(NO_FOUND)){
            s=RuntimeUtil.execForStr(StrUtil.format(adb_path+"{}push {} {}", device, s_dir + "minicap", push_dir));
            StaticLog.info("##minicap push1 {} ", s);
            if(s.contains("error"))return false;
            s=RuntimeUtil.execForStr(StrUtil.format(adb_path+"{}push {} {}", device, s_dir + "minicap.so", push_dir));
            StaticLog.info("##minicap push2 {} ", s);

            s=RuntimeUtil.execForStr(StrUtil.format(adb_path+"{}shell chmod 755 {}/minicap", device, push_dir));
            StaticLog.info("##minicap chmod {} ", s);
        }

        String exist = StrUtil.format(adb_path + "{}shell ps -u shell", device);
        s=RuntimeUtil.execForStr(exist);
        StaticLog.info("##exist {} \n {}", exist,s);
       /* if(!s.contains("minicap")){
            ThreadUtil.execute(()->{
                String ld = StrUtil.format(adb_path + "{}shell LD_LIBRARY_PATH={} {}/minicap -P 1080x1920@1080x1920/0", device, push_dir, push_dir);
                StaticLog.info("##ld {}",ld);
                process=RuntimeUtil.exec(ld);
                StaticLog.info("##ld {} \n {} \n {}", ld,IoUtil.read(process.getInputStream()),"minicap process exited");
                initial=false;
            });
        }
        ThreadUtil.sleep(3 * 1000);
        String forward = StrUtil.format(adb_path + "{}forward tcp:{} localabstract:minicap", device, forwardPort);
        s=RuntimeUtil.execForStr(forward);
        StaticLog.info("##minicap forward {}\n {}", s,forward);*/
        return true;
    }
    public void stop(){
        if(process!=null)process.destroy();
    }

    public byte[] frame(){
        if(!initial){initial=init();}
        String caps = StrUtil.format(adb_path + "{}exec-out LD_LIBRARY_PATH=/data/local/tmp /data/local/tmp/minicap -P 1080x1920@1080x1920/0 -s", device);
//        StaticLog.info("##caps {}",caps);
        Process exec = RuntimeUtil.exec(caps);
        byte[] bytes = IoUtil.readBytes(exec.getInputStream(), true);
        int from=0;
        for (int i = 0; i < bytes.length; i++) {
            if(bytes[i] == (byte) 0xFF && bytes[i+1] == (byte) 0xD8){
                from=i;break;
            }
        }
        return Arrays.copyOfRange(bytes,from,bytes.length);
    }

    @SneakyThrows
    public byte[] stream()  {
        if(!initial){initial=init();}
        ByteBuffer chunk = ByteBuffer.allocate(65536).order(ByteOrder.LITTLE_ENDIAN);
        int readBannerBytes = 0;
        int bannerLength = 2;
        int readFrameBytes = 0;
        int frameBodyLength = 0;
        byte[] frameBody = new byte[0];

        SocketChannel stream = SocketChannel.open(new InetSocketAddress("localhost", forwardPort));


//        MyWebsocketServer server=new MyWebsocketServer(new InetSocketAddress("127.0.0.1",9002));
//        server.start();
//        System.out.println("server listen 9002");
        byte[] frame=null;
        for (int read=0; frame==null ;read=stream.read(chunk)) {
//          log.info("chunk(length={})", chunk.limit());
//            StaticLog.info("##read,stream {} {}", read,stream);
//            if(read<=0) StaticLog.info("##read,stream {} {}", read,stream);
            if(read<=0)continue;
            chunk.flip();
            for (int cursor = 0, len = read; cursor < len; ) {
                if (readBannerBytes < bannerLength) {
                    switch (readBannerBytes) {
                        case 0:
                            // version
                            banner.version = chunk.get();
                            break;
                        case 1:
                            // length
                            banner.length = bannerLength = chunk.get();
                            break;
//                                                case 2:
//                                                case 3:
//                                                case 4:
                        case 5:
                            // pid
                            banner.pid = chunk.getInt();
//                                                                +=
//                                                                (chunk.get() << ((readBannerBytes - 2) * 8)) >>> 0;
                            break;
//                                                case 6:
//                                                case 7:
//                                                case 8:
                        case 9:
                            // real width
                            banner.realWidth = chunk.getInt();
//                                                                +=
//                                                                (chunk.get() << ((readBannerBytes - 6) * 8)) >>> 0;
                            break;
//                                                case 10:
//                                                case 11:
//                                                case 12:
                        case 13:
                            // real height
                            banner.realHeight = chunk.getInt();//+=(chunk.get() << ((readBannerBytes - 10) * 8)) >>> 0;
                            break;
//                                                case 14:
//                                                case 15:
//                                                case 16:
                        case 17:
                            // virtual width
                            banner.virtualWidth = chunk.getInt();
//                                                                +=
//                                                                (chunk.get() << ((readBannerBytes - 14) * 8)) >>> 0;
                            break;
//                                                case 18:
//                                                case 19:
//                                                case 20:
                        case 21:
                            // virtual height
                            banner.virtualHeight = chunk.getInt();
//                                                                +=
//                                                                (chunk.get() << ((readBannerBytes - 18) * 8)) >>> 0;
                            break;
                        case 22:
                            // orientation
                            banner.orientation += chunk.get() * 90;
                            break;
                        case 23:
                            // quirks
                            banner.quirks = chunk.get();
                            break;
                    }

                    cursor += 1;
                    readBannerBytes += 1;

                    if (readBannerBytes == bannerLength) {
//                        log.info("banner {}", JSON.toJSONString(banner));
                    }
                } else if (readFrameBytes < 4) {

                    cursor += 1;
                    readFrameBytes += 1;
                    if (readFrameBytes == 3) {
                        frameBodyLength = chunk.getInt();
//                        log.info("headerbyte{}(val={})", readFrameBytes, frameBodyLength);
                    }
                } else {
                    Assert.isTrue(frameBodyLength < 100 * 10000);
                    if (len - cursor >= frameBodyLength) {
//                                                log.info("bodyfin(len={},cursor={})", frameBodyLength, cursor);
                        byte[] part = new byte[frameBodyLength];
                        chunk.get(part);
                        frameBody = ArrayUtil.addAll(frameBody, part);
                                               /* frameBody = Buffer.concat([
                                                        frameBody
                                                        , chunk.slice(cursor, cursor + frameBodyLength)*/
//            ])

                        // Sanity check for JPG header, only here for debugging purposes.
                        if (frameBody[0] != (byte) 0xFF || frameBody[1] != (byte) 0xD8) {
                            StaticLog.error(
                                    "Frame body does not start with JPG header", frameBody);
                            System.exit(1);
                        }
//                        FileUtil.writeBytes(frameBody, new File("C:/frame.jpg"));

//                        server.broadcast(frameBody);
                                                /*ws.send(frameBody, {
                                                        binary: true
            })*/

                        cursor += frameBodyLength;
                        frameBodyLength = readFrameBytes = 0;
                        frame = frameBody;
                        frameBody = new byte[0];
//                        new Scanner(System.in).nextLine();
                    } else {
//                                                log.info("body(len={})", len - cursor);
                        byte[] part = new byte[chunk.limit() - chunk.position()];
                        chunk.get(part);
                        frameBody = ArrayUtil.addAll(frameBody, part);

                                                /*frameBody = Buffer.concat([
                                                        frameBody
                                                        , chunk.slice(cursor, len)*/
//            ])

                        frameBodyLength -= (len - cursor);
                        readFrameBytes += (len - cursor);
                        cursor = len;//break
                    }
                }
            }
            chunk.compact();
        }
        stream.close();
        return frame;
        }
//    }

}
