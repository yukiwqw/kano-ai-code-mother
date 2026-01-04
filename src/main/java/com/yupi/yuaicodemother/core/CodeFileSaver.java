package com.yupi.yuaicodemother.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.yupi.yuaicodemother.ai.model.HtmlCodeResult;
import com.yupi.yuaicodemother.ai.model.MultiFileCodeResult;
import com.yupi.yuaicodemother.constant.CodeGenFileNameConstant;
import com.yupi.yuaicodemother.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class CodeFileSaver {

    // 保存文件根目录
    private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/temp/code_output";

    /**
     * 保存 HTML 网页代码
     * @param htmlCodeResult
     * @return
     */
    public static File saveHtmlCodeResult(HtmlCodeResult htmlCodeResult) {
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.HTML.getValue());
        writeToFile(baseDirPath, CodeGenFileNameConstant.INDEX_HTML, htmlCodeResult.getHtmlCode());
        return new File(baseDirPath);
    }

    /**
     * 保存多文件网页代码
     *
     * @param result
     * @return
     */
    public static File saveMultiFileCodeResult(MultiFileCodeResult result) {
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.MULTI_FILE.getValue());
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        writeToFile(baseDirPath, "script.js", result.getJsCode());
        return new File(baseDirPath);
    }

    /**
     * 保存文件
     * @param baseDirPath
     * @param indexHtml
     * @param content
     */
    public static void writeToFile(String baseDirPath, String indexHtml, String content) {
        String filePath = baseDirPath + File.separator + indexHtml;
        FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
    }

    /**
     * 根据业务类型构建唯一目录
     * @param bizType
     * @return
     */
    public static String buildUniqueDir(String bizType) {
        String uniqueDirName = StrUtil.format("{}-{}", bizType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

}
