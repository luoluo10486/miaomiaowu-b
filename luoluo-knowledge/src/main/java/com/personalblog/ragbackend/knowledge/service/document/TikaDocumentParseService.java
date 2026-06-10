package com.personalblog.ragbackend.knowledge.service.document;

import com.personalblog.ragbackend.core.parser.DocumentParser;
import com.personalblog.ragbackend.knowledge.config.RagDocumentUploadProperties;
import com.personalblog.ragbackend.knowledge.dto.document.ParseResult;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Tika 文档解析服务
 */
@Service
public class TikaDocumentParseService implements DocumentParser {
    private static final Logger log = LoggerFactory.getLogger(TikaDocumentParseService.class);
    public static final String PARSER_TYPE = "tika";
    private static final Tika TIKA = new Tika();
    private static final Parser PARSER = new AutoDetectParser();

    private final RagDocumentUploadProperties uploadProperties;

    public TikaDocumentParseService(RagDocumentUploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @Override
    public String getParserType() {
        return PARSER_TYPE;
    }

    public ParseResult parseFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ParseResult.failure("文件为空");
        }

        String originalFilename = file.getOriginalFilename();
        log.info("开始解析知识库文档: {}, size={} bytes", originalFilename, file.getSize());

        try (InputStream parseStream = file.getInputStream()) {
            return parse(parseStream, originalFilename, file.getContentType());
        } catch (IOException ex) {
            log.error("读取知识库文档失败: {}", originalFilename, ex);
            return ParseResult.failure("读取文件失败: " + ex.getMessage());
        }
    }

    @Override
    public ParseResult parse(InputStream stream, String fileName, String declaredMimeType) {
        if (stream == null) {
            return ParseResult.failure("解析输入流为空");
        }

        String mimeType = resolveMimeType(declaredMimeType, fileName);
        try {
            if (isTextLike(mimeType, fileName)) {
                return parseTextStream(stream, fileName, mimeType);
            }
            return parseWithTika(stream, fileName, mimeType);
        } catch (IllegalArgumentException ex) {
            return ParseResult.failure(ex.getMessage());
        } catch (TikaException ex) {
            log.error("Tika 解析知识库文档失败: {}", fileName, ex);
            return ParseResult.failure("文档解析失败: " + ex.getMessage());
        } catch (SAXException ex) {
            log.error("XML 结构解析失败: {}", fileName, ex);
            return ParseResult.failure("文档结构解析失败: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("解析知识库文档时出现未知错误: {}", fileName, ex);
            return ParseResult.failure("解析过程中出现未知错误: " + ex.getMessage());
        }
    }

    public String detectMimeType(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            return TIKA.detect(inputStream, file.getOriginalFilename());
        }
    }

    @Override
    public boolean supports(String mimeType, String fileName) {
        return true;
    }

    private ParseResult parseTextStream(InputStream stream, String fileName, String mimeType) throws IOException {
        int maxChars = Math.max(1, uploadProperties.getMaxParseTextChars());
        StringBuilder builder = new StringBuilder(Math.min(maxChars, 64 * 1024));
        Map<String, String> metadata = new HashMap<>();
        metadata.put("resourceName", fileName == null ? "" : fileName);
        metadata.put("parserMode", "stream-text");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, resolveCharset(mimeType)))) {
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                if (read == 0) {
                    continue;
                }
                if (builder.length() + read > maxChars) {
                    throw new IllegalArgumentException("文档内容过大，请拆分后再上传");
                }
                builder.append(buffer, 0, read);
            }
        }

        String content = cleanText(builder.toString());
        if (!StringUtils.hasText(content)) {
            return ParseResult.failure("解析结果为空，可能是扫描件、加密文件或纯图片文档");
        }
        return ParseResult.success(mimeType, content, metadata);
    }

    private ParseResult parseWithTika(InputStream stream, String fileName, String mimeType)
            throws IOException, SAXException, TikaException {
        int maxChars = Math.max(1, uploadProperties.getMaxParseTextChars());
        BodyContentHandler handler = new BodyContentHandler(maxChars);
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
        PARSER.parse(stream, handler, metadata, new ParseContext());

        String content = cleanText(handler.toString());
        Map<String, String> metadataMap = extractMetadata(metadata);
        if (!StringUtils.hasText(content)) {
            return ParseResult.failure("解析结果为空，可能是扫描件、加密文件或纯图片文档");
        }
        return ParseResult.success(mimeType, content, metadataMap);
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("(?m)^[ \\t]+|[ \\t]+$", "")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll("[ \\t]+", " ")
                .trim();
    }

    private Map<String, String> extractMetadata(Metadata metadata) {
        Map<String, String> result = new HashMap<>();
        for (String name : metadata.names()) {
            String value = metadata.get(name);
            if (value != null && !value.isEmpty()) {
                result.put(name, value);
            }
        }
        return result;
    }

    private boolean isTextLike(String mimeType, String fileName) {
        String lowerMimeType = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        String lowerName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        return lowerMimeType.startsWith("text/")
                || lowerMimeType.contains("json")
                || lowerMimeType.contains("xml")
                || lowerMimeType.contains("markdown")
                || lowerName.endsWith(".txt")
                || lowerName.endsWith(".md")
                || lowerName.endsWith(".csv")
                || lowerName.endsWith(".log")
                || lowerName.endsWith(".json")
                || lowerName.endsWith(".xml")
                || lowerName.endsWith(".html")
                || lowerName.endsWith(".htm");
    }

    private String resolveMimeType(String declaredMimeType, String fileName) {
        if (StringUtils.hasText(declaredMimeType)) {
            return declaredMimeType.trim();
        }
        return TIKA.detect(fileName == null ? "" : fileName);
    }

    private Charset resolveCharset(String mimeType) {
        if (mimeType != null && mimeType.toLowerCase(Locale.ROOT).contains("gb")) {
            return Charset.forName("GB18030");
        }
        return StandardCharsets.UTF_8;
    }
}
