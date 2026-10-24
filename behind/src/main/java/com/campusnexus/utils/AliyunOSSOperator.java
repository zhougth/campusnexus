package com.campusnexus.utils;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.OSSObject;
import com.campusnexus.common.exception.BizException;
import com.campusnexus.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AliyunOSSOperator {

    private final AliyunOSSProperties aliyunOSSProperties;

    public String upload(byte[] data, String filename) {
        OSS ossClient = createClient();
        try {
            ossClient.putObject(bucketName(), filename, new ByteArrayInputStream(data));
        } catch (OSSException | ClientException e) {
            log.error("OSS 上传失败: {}", filename, e);
            throw new BizException(ErrorCode.OSS_UPLOAD_FAILED);
        } finally {
            ossClient.shutdown();
        }
        return buildFileUrl(filename);
    }

    public byte[] download(String fileUrl) {
        String objectKey = extractObjectKey(fileUrl);
        OSS ossClient = createClient();
        try (OSSObject object = ossClient.getObject(bucketName(), objectKey)) {
            return object.getObjectContent().readAllBytes();
        } catch (IOException e) {
            log.error("读取 OSS 文件失败: {}", fileUrl, e);
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "读取原始文件失败");
        } catch (OSSException | ClientException e) {
            log.error("下载 OSS 文件失败: {}", fileUrl, e);
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "读取原始文件失败");
        } finally {
            ossClient.shutdown();
        }
    }

    private OSS createClient() {
        String endpoint = aliyunOSSProperties.getEndpoint();
        String accessKeyId = aliyunOSSProperties.getAccessKeyId();
        String accessKeySecret = aliyunOSSProperties.getAccessKeySecret();
        if (!StringUtils.hasText(endpoint)
                || !StringUtils.hasText(accessKeyId)
                || !StringUtils.hasText(accessKeySecret)) {
            throw new BizException(ErrorCode.OSS_UPLOAD_FAILED, "OSS 未配置");
        }

        ClientBuilderConfiguration configuration = new ClientBuilderConfiguration();
        configuration.setSignatureVersion(SignVersion.V4);

        return OSSClientBuilder.create()
                .credentialsProvider(new DefaultCredentialProvider(accessKeyId, accessKeySecret))
                .clientConfiguration(configuration)
                .region(aliyunOSSProperties.getRegion())
                .endpoint(endpoint)
                .build();
    }

    private String bucketName() {
        String bucketName = aliyunOSSProperties.getBucketname();
        if (!StringUtils.hasText(bucketName)) {
            throw new BizException(ErrorCode.OSS_UPLOAD_FAILED, "OSS Bucket 未配置");
        }
        return bucketName;
    }

    private String buildFileUrl(String objectKey) {
        String endpoint = aliyunOSSProperties.getEndpoint();
        String cleanEndpoint = endpoint.replace("https://", "").replace("http://", "");
        return String.format("https://%s.%s/%s", bucketName(), cleanEndpoint, objectKey);
    }

    private String extractObjectKey(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "文件地址为空");
        }
        int schemeIndex = fileUrl.indexOf("://");
        int pathIndex = fileUrl.indexOf('/', schemeIndex < 0 ? 0 : schemeIndex + 3);
        if (pathIndex < 0 || pathIndex == fileUrl.length() - 1) {
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "无法解析 OSS 文件地址");
        }
        return fileUrl.substring(pathIndex + 1);
    }
}
