package art.qiuye.diantao;


import java.util.TreeMap;
import java.util.Vector;

/**
 * @author hy
 * @create 2023/4/24 0024
 */

public class Config {
    public String adb_path;
    public Vector<String> devices;
    public Vector<Integer> items_time;
    public Vector<Integer> items_interval;
    public Vector<Integer> items_count;
    public Integer walk_base;
    public Integer walk_step;
    public Integer work_base;
    public Integer work_step;

    public TreeMap<String,String> quicks;

    public Boolean random_point;

    public String config_name;
    public String quicks_path;

    public String from;

    public String to;
}
