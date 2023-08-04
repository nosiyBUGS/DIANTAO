package art.qiuye.diantao.db;

import java.io.Serializable;
import java.sql.Timestamp;

import lombok.Data;

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
    public String config;

    /**
     * 快捷配置
     */
    public String quick;

    private static final long serialVersionUID = 1L;

    public Timestamp createTime;
    public Timestamp updateTime;
}
