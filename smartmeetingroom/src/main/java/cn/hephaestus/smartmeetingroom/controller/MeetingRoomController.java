package cn.hephaestus.smartmeetingroom.controller;

import cn.hephaestus.smartmeetingroom.common.RetJson;
import cn.hephaestus.smartmeetingroom.model.MeetingRoom;
import cn.hephaestus.smartmeetingroom.model.ReserveInfo;
import cn.hephaestus.smartmeetingroom.model.User;
import cn.hephaestus.smartmeetingroom.model.UserInfo;
import cn.hephaestus.smartmeetingroom.service.*;
import cn.hephaestus.smartmeetingroom.utils.ValidatedUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Future;
import java.util.*;

/**
 * 会议室的管理
 */
@RestController
@Validated
public class MeetingRoomController {
    @Autowired
    MeetingRoomService meetingRoomService;
    @Autowired
    UserService userService;
    @Autowired
    ReserveInfoService reserveInfoService;
    @Autowired
    FaceInfoService faceInfoService;
    @Autowired
    FaceEngineService faceEngineService;
    @Autowired
    MeetingParticipantService meetingParticipantService;
    @Autowired
    NewsService newsService;
    @Autowired
    RedisService redisService;
    @Autowired
    DepartmentService departmentService;
    User user=null;
    UserInfo userInfo=null;


    @ModelAttribute
    public void comment(HttpServletRequest request,ReserveInfo reserveInfo){
        //前端直接传数组遇到了麻烦

        if (reserveInfo.getRid()!=null){
            String[] strings=reserveInfo.getParticipantStr().split("\\ ");
            List<Integer> list=new LinkedList<>();
            for (String s:strings){
                list.add(Integer.parseInt(s));
            }
            Integer[] integers=new Integer[list.size()];
            list.toArray(integers);
            reserveInfo.setParticipants(integers);
        }
        if (user!=null&&userInfo!=null){
            return;
        }
        user=(User)request.getAttribute("user");
        userInfo = (UserInfo)request.getAttribute("userInfo");
    }
    //1.添加会议室
    @RequestMapping("/addMeetingRoom")
    public RetJson addMeetingRoom(MeetingRoom meetingRoom){
        //判断当前用户权限
        if (user.getRole()==0){
            return RetJson.fail(-1,"无权限的操作");
        }

        if (!ValidatedUtil.validate(meetingRoom)){
            return RetJson.fail(-1,"请检查参数");
        }
        //设置oid
        meetingRoom.setOid(meetingRoom.getOid());
        if(meetingRoomService.addMeetingRoom(meetingRoom)){
            return RetJson.success(null);
        }
        return RetJson.fail(-1,"当前用户没有权限！");
    }

    //2.修改会议室信息
    @RequestMapping("alterMeetingRoom")
    public RetJson alterMeetingRoom(MeetingRoom meetingRoom){
        if (user.getRole()==0){
            return RetJson.fail(-1,"当前用户没有权限");
        }
        if (!ValidatedUtil.validate(meetingRoom)){
            return RetJson.fail(-1,"请检查参数");
        }
        meetingRoom.setOid(meetingRoom.getOid());
        if(meetingRoomService.alterMeetingRoom(meetingRoom)){
            return RetJson.success(null);
        }
        return RetJson.fail(-1,"当前用户没有权限！");
    }
    //3.删除会议室
    @RequestMapping("/deleteMeetingRoom")
    public RetJson deleteMeetingRoom(Integer oid,Integer roomId){
        if (user.getRole()==0){
            return RetJson.fail(-1,"当前用户没有权限！");
        }
        if(meetingRoomService.delteteMeetingRoom(oid,roomId)){
            return RetJson.success(null);
        }
        return RetJson.fail(-1,"当前用户没有权限！");
    }
    //4.获取会议室
    @RequestMapping("/getMeetingRoom")
    public RetJson getMeetingRoomWithRoomId(Integer roomId){
        MeetingRoom meetingRoom = meetingRoomService.getMeetingRoomWithRoomId(roomId);
        if (meetingRoom==null){
            return RetJson.fail(-1,"找不到该会议室");
        }
        return RetJson.success("meetingRoom",meetingRoom);
    }

    //5.获取所有会议室
    @RequestMapping("/getMeetingRoomList")
    public RetJson getMeetingRoomList() {
        Integer oid=userInfo.getOid();
        MeetingRoom[] meetingRooms=meetingRoomService.getMeetingRoomList(oid);
        return RetJson.success("meetingRooms",meetingRooms);
    }

    //6.获取所有可用的会议室
    @RequestMapping("/getAllAvailableMeetingRoom")
    public RetJson getAllAvailableMeetingRoom(@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")Date startTime,@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")Date endTime,Integer num){
        Integer oid=userInfo.getOid();
        MeetingRoom[] meetingRooms=meetingRoomService.getAllUsableMeetingRooms(oid,startTime,endTime,num);
        return RetJson.success("meetingRooms",meetingRooms);
    }

    @RequestMapping("/reserveRoom")
    public RetJson reserveMeetingRoom(@Valid ReserveInfo reserveInfo){
        Integer oid=userInfo.getOid();
        reserveInfo.setReserveUid(user.getId());
        reserveInfo.setReserveOid(oid);
        reserveInfo.setReserveDid(userInfo.getDid());

        System.out.println(reserveInfo);

        //判断该用户是否拥有预定会议室的权限
        if (user.getRole()==0&&user.getReserveJurisdiction()==0){
            return RetJson.fail(-1,"你没有预定会议室的权限");
        }
        //判断参会人员是否合法,存在且有空闲时间
        Set<Integer> set=meetingRoomService.getAllConficUser(oid,reserveInfo.getStartTime(),reserveInfo.getEndTime());

        for(Integer participant:reserveInfo.getParticipants()){
            if(userService.getUserByUserId(participant) == null){
                return RetJson.fail(-1,"参与者暂未注册！");
            }
            if (set.contains(participant)){
                return RetJson.fail(-1,"用户"+participant+"忙碌");
            }
        }

        //看该会议室是否存在
        MeetingRoom room=meetingRoomService.getMeetingRoomWithRoomId(reserveInfo.getRid());
        Integer reserveInfoId=0;
        //预定会议室
        if(room!=null){
            //与该时间段有交集的reserveInfo
            ReserveInfo[] reserveInfos = reserveInfoService.queryIsAvailable(reserveInfo.getRid(),reserveInfo.getStartTime(),reserveInfo.getEndTime());
            if(reserveInfos.length == 0){
                //会议室有效
                reserveInfoService.addReserveInfo(reserveInfo);
                //插入后直接映射到实体类了!!!
                reserveInfoId = reserveInfo.getReserveId();
                meetingParticipantService.addParticipants(user.getId(),oid,reserveInfoId,reserveInfo.getParticipants());
                //预定成功，发送消息给所有参会用户
                newsService.sendNews("您有一个会议，请注意查看会议具体情况。",reserveInfo.getParticipants(),"other");
                return RetJson.success("meetingId",reserveInfoId);
            }
            return RetJson.fail(-1,"会议室已被占用！");
        }
        return RetJson.fail(-1,"会议室不存在！");
    }

    //7.根据条件获取用户列表
    @RequestMapping("/getIdleUser")
    public RetJson getIdleUser(Integer did,@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")Date startTime,@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")Date endTime){
        Integer oid=userInfo.getOid();
        List<UserInfo> list=null;
        //按照部门查出所有用户
        if (did==0){
            list=userService.getUserinfoListByOid(oid);
        }else{
            list=userService.getUserInfoListByDid(oid,did);
        }
        //查出当前不可用的用户
        Set<Integer> set=meetingRoomService.getAllConficUser(oid,startTime,endTime);
        for (UserInfo userInfo:list){
            if (!set.contains(userInfo.getId())){
                userInfo.setIdle(true);
            }else {
                userInfo.setIdle(false);
            }
        }
        return RetJson.success("userList",list);
    }

    @RequestMapping("/roomIsAvailable")
    public RetJson checkRoomIsAvailable(Integer roomId, Integer oid, @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") @Future(message = "时间必须在当前时间之前")
            Date startTime,@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") @Future(message = "时间必须在当前时间之前") Date endTime, HttpServletRequest request){
        if(!meetingRoomService.getMeetingRoomWithRoomId(roomId).isAvailable()){
            return RetJson.fail(-1,"会议室暂时不能使用，请询问管理员！");
        }
        User user = (User)request.getAttribute("user");
        UserInfo userInfo = userService.getUserInfo(user.getId());
        if(userInfo.getOid() == oid){
            ReserveInfo[] reserveInfos = reserveInfoService.queryIsAvailable(roomId,startTime,endTime);
            if(reserveInfos.length == 0){
                return RetJson.success(null);
            }else {
                return RetJson.fail(-1,"已被预订！");
            }
        }
        return RetJson.fail(-1,"只能查询自己公司的会议室");
    }

    @RequestMapping("/checkAllRooms")
    public RetJson checkAllOrganizationMeetingRooms(Integer oid,@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") @Future(message = "时间必须在当前时间之前")
            Date startTime,@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") @Future(message = "时间必须在当前时间之前") Date endTime){
        if(oid != userInfo.getOid()){
            return RetJson.fail(-1,"非法操作！");
        }
        Set<String> keys = redisService.sGetByPattern(oid + "cm");
        Set<Integer> roomSet = new LinkedHashSet<>();

        for(String key:keys){
            key = key.substring(key.indexOf('m') + 1);
            Integer reserveId = Integer.parseInt(key);
            Integer occupiedRoomId = reserveInfoService.queryIsAvailableByReserveId(reserveId,startTime,endTime);
            if(occupiedRoomId != null){
                roomSet.add(occupiedRoomId);
            }
        }
        return RetJson.success("occupiedRoomId",roomSet);
    }

    //取消预定会议
    @RequestMapping("/cancelReservation")
    public RetJson cancelReservationByMid(Integer mid){
        if(user.getRole() == 0){
            RetJson.fail(-1,"您的权限不够，取消失败！");
        }
        if(reserveInfoService.deleteReserveInfo(userInfo.getOid(),mid)){
            return RetJson.success(null);
        }
        return RetJson.fail(-1,"取消失败！");
    }


    @RequestMapping("/updateReservation")
    public RetJson updateReservation(@Valid ReserveInfo reserveInfo){
        if(user.getId() != reserveInfo.getReserveUid()){
            return RetJson.fail(-1,"操作非法！");
        }
        if(user.getRole() == 0){
            RetJson.fail(-1,"您的权限不够，预定失败！");
        }
        if(reserveInfoService.queryIsAvailable(reserveInfo.getRid(),reserveInfo.getStartTime(),reserveInfo.getEndTime()).length == 0){
            reserveInfoService.updateReserveInfo(reserveInfo);
            return RetJson.success(null);
        }
        return RetJson.fail(-1,"操作失败!");
    }

    @RequestMapping("/getProperTime")
    public RetJson getProperMeetingTime(Integer[] participants,@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")Date[] dates,double duration){
        Set<Integer> set = new HashSet<>();
        for(int participant:participants){
            if(userService.getUserByUserId(participant) == null){
                return RetJson.fail(-1,"参会人员尚未注册！");
            }
            set.add(participant);
        }
        if((int)(duration * 10) % 5 != 0){
            return RetJson.fail(-1,"参数错误！");
        }
        ReserveInfo reserveInfo = meetingRoomService.getProperMeetingTime(set,dates,duration,userInfo.getOid());
        return RetJson.success("reserveInfo",reserveInfo)                         ;
    }

    //========================授权===========================
    //赋予用户权限
    @RequestMapping("/givePower")
    public RetJson giveReservePower(Integer uid){
        if(user.getRole() == 0){
            return RetJson.fail(-1,"权限不够！");
        }
        User user = userService.getUserByUserId(uid);
        if(user == null){
            return RetJson.fail(-2,"该用户不存在");
        }
        //Jurisdiction 0表示没有权限，1代表有权限
        userService.alterReserveJurisdiction(1,uid);
        return RetJson.success(null);
    }

    //取消用户权限
    @RequestMapping("/cancelPower")
    public RetJson cancelReservePower(Integer uid){
        User user = userService.getUserByUserId(uid);
        if(user == null){
            return RetJson.fail(-2,"该用户不存在");
        }
        if(user.getRole() == 0){
            return RetJson.fail(-1,"权限不够！");
        }
        //Jurisdiction 0表示没有权限，1代表有权限
        userService.alterReserveJurisdiction(0,uid);
        return RetJson.success(null);
    }

    //申请权限
    @RequestMapping("/applyPower")
    public RetJson applyReservePower(){
        if(user.getRole() != 0){
            return RetJson.fail(-1,"您已有权限！");
        }
        Set<Integer> adminSet = departmentService.getAdmin(userInfo.getOid(),userInfo.getDid());
        newsService.sendNews(user.getId() + "号用户想拥有预定会议的权限，请您尽快处理！",adminSet.toArray(new Integer[adminSet.size()]),"other");
        return RetJson.success(null);
    }
}
