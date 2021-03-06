package cn.hephaestus.smartmeetingroom.controller;

import cn.hephaestus.smartmeetingroom.common.RetJson;
import cn.hephaestus.smartmeetingroom.model.OrganizationInfo;
import cn.hephaestus.smartmeetingroom.model.User;
import cn.hephaestus.smartmeetingroom.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 用来管理一个企业或者学校机构账号，通过该控制器注册的账号权限均为root
 */
@Validated
@RestController
public class OrganizationController {
    @Autowired
    OrganizationService organizationService;
    //企业修改信息
    @RequestMapping("/alterorganization")
    public RetJson alterOrganization(OrganizationInfo organizationInfo, HttpServletRequest request){
        User user = (User)request.getAttribute("user");
        if(user.getRole() == 0){
            return RetJson.fail(-1,"您的权限不够！");
        }
        Boolean b=organizationService.alterOne(organizationInfo);
        if (b){
            return RetJson.success(null);
        }
        return RetJson.fail(-1,"修改信息失败");
    }

    @RequestMapping("/getOrganizations")
    public RetJson getOrganizations(){
        return RetJson.success("organizations",organizationService.getOrganizationInfos());
    }

    @RequestMapping("/getOrganization")
    public RetJson getOrganizationByOid(Integer oid){
        return RetJson.success("organization",organizationService.getOne(oid));
    }
}
