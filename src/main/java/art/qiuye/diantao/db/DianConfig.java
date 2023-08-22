package art.qiuye.diantao.db;

import art.qiuye.diantao.Config;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * config
 * @author
 */
public class DianConfig implements Serializable {
    /**
     * 品牌
     */
    public String vendor;

    /**
     * 主要配置
     */
    public Config config;

    /**
     * 快捷配置
     */
    public String quick;

    private static final long serialVersionUID = 1L;

    public Timestamp createTime;
    public Timestamp updateTime;
}
