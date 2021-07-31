package cn.hephaestus.smartmeetingroom.utils;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import com.alibaba.fastjson.JSONObject;

import java.util.*;

public class MoblieMessageUtil {

    /**
     * 发送短信工具类
     * @param mobile 发送目标电话号码
     * @param code 验证码内容
     * @return 是否成功
     */
    public static boolean sendIdentifyingCode(String mobile, String code) {


        String host = "https://jmsms.market.alicloudapi.com";
        String path = "/sms/send";
        String method = "POST";
        String appcode = "8c9484bb8a924da087924c5fa11a20a9";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("mobile", mobile);
        querys.put("templateId", "M72CB42894");
        querys.put("value", code);
        Map<String, String> bodys = new HashMap<String, String>();

        try {
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            System.out.println(response.toString());
            System.out.println("短信接口返回："+response.toString());
            //获取response的body
            JSONObject jsonObject = JSONObject.parseObject(EntityUtils.toString(response.getEntity()));
            System.out.println(jsonObject);
            return jsonObject.getObject("success", boolean.class);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
