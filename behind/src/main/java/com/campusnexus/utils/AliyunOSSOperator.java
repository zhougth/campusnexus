package com.campusnexus.utils;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Component
@RequiredArgsConstructor
public class AliyunOSSOperator {

    private final AliyunOSSProperties aliyunOSSProperties;

    public String upload(byte[] data, String filename) {

        // 设置OSS地域和Endpoint
        String region = aliyunOSSProperties.getRegion();
        String endpoint = aliyunOSSProperties.getEndpoint();
        String bucketName = aliyunOSSProperties.getBucketname();
        String accessKeyId = aliyunOSSProperties.getAccessKeyId();
        String accessKeySecret = aliyunOSSProperties.getAccessKeySecret();

        // 创建凭证提供者
        DefaultCredentialProvider provider = new DefaultCredentialProvider(accessKeyId, accessKeySecret);

        // 配置客户端参数
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        // 显式声明使用V4签名算法
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);

        // 初始化OSS客户端
        OSS ossClient = OSSClientBuilder.create()
                .credentialsProvider(provider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .endpoint(endpoint)
                .build();

        try {
            // 1. 将 byte[] 包装为 ByteArrayInputStream
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);

            // 2. 执行上传
            // filename 即为 Object 的完整路径（例如 "images/photo.jpg"）
            // 注意：Object 路径中不能包含 Bucket 名称
            ossClient.putObject(bucketName, filename, inputStream);

            System.out.println("文件上传成功: " + filename);

        } catch (OSSException oe) {
            // 处理 OSS 服务端返回的错误（如权限不足、Bucket 不存在等）
            System.out.println("OSS 服务端异常: " + oe.getErrorMessage());
            oe.printStackTrace();
        } catch (ClientException ce) {
            // 处理客户端异常（如网络不通、Endpoint 错误等）
            System.out.println("OSS 客户端异常: " + ce.getMessage());
            ce.printStackTrace();
        } finally {
            // 3. 务必关闭客户端，释放连接资源
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        // 确保 endpoint 不以 "https://" 或 "http://" 开头
        String cleanEndpoint = endpoint.replace("https://", "").replace("http://", "");
        String fileUrl = String.format("https://%s.%s/%s", bucketName, cleanEndpoint, filename);
        // 例如：https://my-bucket.oss-cn-hangzhou.aliyuncs.com/images/photo.jpg
        return fileUrl;
    }

}
