package art.qiuye.diantao;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

/**
 * @author hy
 * @create 2023/4/24 0024
 */

public class Config {
    public String adb_path;

    public Vector<String> devices;

    public String current_device;
//    public Vector<Integer> items_time;
//    public Vector<Integer> items_interval;
//    public Vector<Integer> items_count;
    public Integer walk_base;
    public Integer walk_step;
    public Integer work_base;
    public Integer work_step;


    @JSONField(serialize = false)
    public TreeMap<String,String> quicks;

    public Boolean random_point;

    public String config_name;
    public String quicks_path;
    public String config_path;

    public String version_path;


    public String from;

    public String to;

    public int version;

    public Map<String,String> variables;
    @Override
    public String toString() {
        return JSON.toJSONString(this, SerializerFeature.PrettyFormat);
    }
}
