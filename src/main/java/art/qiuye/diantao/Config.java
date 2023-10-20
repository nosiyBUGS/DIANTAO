package art.qiuye.diantao;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.util.TreeMap;
import java.util.Vector;

/**
 * @author hy
 * @create 2023/4/24 0024
 */

public class Config {
    public String adb_path;

    public Vector<String> devices;


    @JSONField(serialize = false)
    public String current_device;
    public Integer walk_base;
    public Integer walk_step;
    public Integer work_base;
    public Integer work_step;


    @JSONField(serialize = false)
    public TreeMap<String,String> quicks;

    @JSONField(serialize = false)
    public TreeMap<String,String> commons;

    public Boolean random_point;

    public String config_name;
    public String quicks_path;
    public String config_path;

    public String version_path;

    public static final String commons_path=System.getProperty("user.dir") + "/exclude/commons.txt";
    public static final String exclude_path=System.getProperty("user.dir") + "/exclude/";
    public static final String screen_path=exclude_path + "screen/";


    public String from;

    public String to;

    public int version;

    public TreeMap<String,String> variables;
    @Override
    public String toString() {
        return JSON.toJSONString(this, SerializerFeature.PrettyFormat);
    }
}
