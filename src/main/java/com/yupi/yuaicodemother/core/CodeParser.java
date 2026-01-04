package com.yupi.yuaicodemother.core;

import com.yupi.yuaicodemother.ai.model.HtmlCodeResult;
import com.yupi.yuaicodemother.ai.model.MultiFileCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码解析器
 * 提供静态方法解析不同类型的代码内容
 */
public class CodeParser {
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_CODE_PATTERN = Pattern.compile("```css\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_CODE_PATTERN = Pattern.compile("```(?:js|javascript)\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    /**
     * 解析 HTML 单文件代码
     */
    public static HtmlCodeResult parseHtmlCode(String content) {
        HtmlCodeResult htmlCodeResult = new HtmlCodeResult();
        // 提取 HTML 代码
        String htmlCode = extractHtmlCode(content);
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            htmlCodeResult.setHtmlCode(htmlCode);
        } else {
            htmlCodeResult.setHtmlCode(content);
        }
        return htmlCodeResult;
    }

    public static MultiFileCodeResult parseMultiFileCode(String content) {
        MultiFileCodeResult result = new MultiFileCodeResult();
        // 提取各类代码
        String htmlCode = extractCodeByPattern(content, HTML_CODE_PATTERN);
        String cssCode = extractCodeByPattern(content, CSS_CODE_PATTERN);
        String jsCode = extractCodeByPattern(content, JS_CODE_PATTERN);

        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode);
        }
        if (cssCode != null && !cssCode.trim().isEmpty()) {
            result.setCssCode(cssCode);
        }
        if (jsCode != null && !jsCode.trim().isEmpty()) {
            result.setJsCode(jsCode);
        }
        return result;
    }

    /**
     * 提取 HTML 代码
     * @param content
     * @return
     */
    private static String extractHtmlCode(String content){
        Matcher matcher = HTML_CODE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 根据正则模式提取代码
     *
     * @param content 原始内容
     * @param pattern 正则模式
     * @return 提取的代码
     */
    private static String extractCodeByPattern(String content,Pattern pattern){
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
