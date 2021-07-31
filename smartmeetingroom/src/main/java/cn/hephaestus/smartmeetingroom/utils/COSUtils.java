package cn.hephaestus.smartmeetingroom.utils;

import cn.hutool.core.io.FileUtil;
import com.aliyun.oss.*;
import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.model.*;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

public class COSUtils {

    /**常量:地域节点*/
    private static final String ENDPOINT = "https://oss-cn-shanghai.aliyuncs.com";
    /**常量:访问阿里云API的秘钥*/
    private static final String ACCESS_KEY_ID = "LTAI5tRYp1LuZspXzD7bjxfH";
    private static final String ACCESS_KEY_SECRET = "jJPZIAGZQLdBuA0GJG1QZAJRqLCtMa";
    /**常量:bucket名*/
    private static final String BUCKET_NAME = "fengye-school";
    /**常量:文件存储域名*/
    private static final String BUCKET_ENDPOINT_URL = "https://fengye-school.oss-cn-shanghai.aliyuncs.com";
    /**常量:url有效日期 100年*/
    private static final Date URL_EXPIRATION = new Date(System.currentTimeMillis() + 1000L * 3600 * 24 * 365 * 100);
    /**常量:用户默认头像*/
    public static final String USER_DEFAULT_AVATAR = "https://fengye-school.oss-cn-shanghai.aliyuncs.com/default/2021-04-171abf2720-db94-49a9-8838-f89fc51f318f.jpg?Expires=4772236665&OSSAccessKeyId=LTAI5tRYp1LuZspXzD7bjxfH&Signature=0Rv1SEAEEt5g8JOVWxANImtQ3DQ%3D";


    /**
     * 生成随机文件名字
     * @return 随机文件名字
     */
    private static String generateFileName(String fileName){
        return DateUtil.getNowDate() + UUID.randomUUID().toString() + fileName.substring(fileName.lastIndexOf("."));
    }


    /**
     * 以字节流的形式上传一个文件
     * @param   bytes     文件内容的字节数组形式
     * @param   fileName  文件名
     * @param   path      存储路径
     * @return  若成功     访问文件的url
     *          若出错     该文件存储路径
     */
    public static String uploadFile(byte[] bytes, String fileName, String path){
        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(ENDPOINT, ACCESS_KEY_ID, ACCESS_KEY_SECRET);

        // 设置bucket和文件存储路径
        String filePath = path + generateFileName(fileName);
        PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_NAME, filePath , new ByteArrayInputStream(bytes));

        // 设置存储类型和读写权限
        ObjectMetadata metadata = new ObjectMetadata();
        // 存储类型为标准存储
        metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Standard.toString());
        // 读写权限为私有
        metadata.setObjectAcl(CannedAccessControlList.Private);
        putObjectRequest.setMetadata(metadata);

        // 上传文件
        try{
            ossClient.putObject(putObjectRequest);
        } catch (OSSException | ClientException e) {
            e.printStackTrace();
            // 报错则返回存储路径，用于后续删除
            return filePath;
        }
        if (ossClient.doesObjectExist(BUCKET_NAME, filePath)){
            // 确认文件上传成功，返回访问该文件的url
            return ossClient.generatePresignedUrl(BUCKET_NAME, filePath, URL_EXPIRATION).toString();
        } else {
            throw new RuntimeException("文件上传失败");
        }
    }


    /**
     * 以文件流的形式上传一个文件
     * @param   file      文件
     * @param   filePath  存储路径
     * @return  若成功 访问文件的url
     *          若出错 该文件存储路径
     */
    public static String uploadFile(File file, String filePath) throws FileNotFoundException {
        return uploadFile(FileUtil.readBytes(file), file.getName(), filePath);
    }


    /**
     * 按文件路径删除目标文件
     * @param filePath 文件路径
     * @return 是否成功删除
     */
    public static boolean deleteFileByPath(String filePath) {
        OSS ossClient = new OSSClientBuilder().build(ENDPOINT, ACCESS_KEY_ID, ACCESS_KEY_SECRET);
        // 确认文件是否存在
        if (!ossClient.doesObjectExist(BUCKET_NAME, filePath)) {
            return false;
        }
        try{
            ossClient.deleteObject(BUCKET_NAME, filePath);
        } catch (OSSException e) {
            e.printStackTrace();
        }
        if (ossClient.doesObjectExist(BUCKET_NAME, filePath)) {
            // 执行删除操作后，文件依然存在，则报错。
            throw new RuntimeException("oss执行删除命令失败，目标文件路径:" + filePath);
        } else{
            ossClient.shutdown();
            return true;
        }
    }


    /**
     * 按url删除目标文件
     * @param url 访问该文件的url地址
     * @return 是否成功删除
     */
    public static boolean deleteFileByUrl(String url) {
        String filePath;
        if(url.startsWith(BUCKET_ENDPOINT_URL)){
            filePath = url.replace(BUCKET_ENDPOINT_URL + "/","");
            if(filePath.contains("?")){
                filePath = filePath.substring(0,filePath.indexOf('?'));
            }
        } else{
            return false;
        }
        return deleteFileByPath(filePath);
    }


    //上传文件
    public static String addFile(String key, InputStream inputStream){

        COSCredentials cred = new BasicCOSCredentials(secretId,secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region_name));
        COSClient cosClient = new COSClient(cred, clientConfig);
        PutObjectRequest objectRequest=new PutObjectRequest(bucketName,key,inputStream,new ObjectMetadata());
        try {
           PutObjectResult objectResult=cosClient.putObject(objectRequest);
        }catch (Exception e){
            return null;
        }finally {
            try {
                inputStream.close();
            }catch (Exception e){
                LogUtils.getExceptionLogger().error(e.toString());
            }
        }
        return "https://smartmeetingroom-1257009269.cos.ap-guangzhou.myqcloud.com/"+key;
    }
}
