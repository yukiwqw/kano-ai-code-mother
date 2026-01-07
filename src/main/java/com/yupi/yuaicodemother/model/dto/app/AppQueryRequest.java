package com.yupi.yuaicodemother.model.dto.app;

import com.mybatisflex.annotation.Column;
import com.yupi.yuaicodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 管理员查询应用请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AppQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 部署标识
     */
    private String deployKey;
    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 代码生成类型
     */
    private String codeGenType;

    private static final long serialVersionUID = 1L;
}

