/*
 * @Copyright: 2005-2018 www.hyjf.com. All rights reserved.
 */
package com.sandman.download.service.admin;

import com.sandman.download.base.BaseService;
import com.sandman.download.bean.admin.OverviewResultBean;

import java.util.Date;

/**
 * @author sunpeikai
 * @version AdminService, v0.1 2019/1/10 11:10
 */
public interface AdminService extends BaseService {

    /**
     * 根据日期获取上传资源数量
     * @auth sunpeikai
     * @param
     * @return
     */
    int getUploadCount(Date start,Date end);

    /**
     * 根据日期获取下载资源数量
     * @auth sunpeikai
     * @param
     * @return
     */
    int getDownloadCount(Date start,Date end);

    /**
     * 根据日期获取活跃用户数量
     * @auth sunpeikai
     * @param
     * @return
     */
    int getActiveCount(Date start,Date end);

    /**
     * 根据日期获取注册用户数量
     * @auth sunpeikai
     * @param
     * @return
     */
    int getRegisterCount(Date start,Date end);

    /**
     * 获取周上传量，封装起来比较好缓存
     * @auth sunpeikai
     * @param
     * @return
     */
    int getUploadCountWeek(Date start,Date end);

    /**
     * 获取月上传量，封装起来比较好缓存
     * @auth sunpeikai
     * @param
     * @return
     */
    int getUploadCountMonth(Date start,Date end);

    /**
     * 获取周下载量，封装起来比较好缓存
     * @auth sunpeikai
     * @param
     * @return
     */
    int getDownloadCountWeek(Date start,Date end);

    /**
     * 获取月下载量，封装起来比较好缓存
     * @auth sunpeikai
     * @param
     * @return
     */
    int getDownloadCountMonth(Date start,Date end);

    /**
     * 获取周活跃用户数量
     * @auth sunpeikai
     * @param
     * @return
     */
    int getActiveCountWeek(Date start,Date end);

    /**
     * 获取月活跃用户数量
     * @auth sunpeikai
     * @param
     * @return
     */
    int getActiveCountMonth(Date start,Date end);

    /**
     * 获取周注册用户数量
     * @auth sunpeikai
     * @param
     * @return
     */
    int getRegisterCountWeek(Date start,Date end);

    /**
     * 获取月注册用户数量
     * @auth sunpeikai
     * @param
     * @return
     */
    int getRegisterCountMonth(Date start,Date end);

    /**
     * 获取折线图数据
     * @auth sunpeikai
     * @param
     * @return
     */
    OverviewResultBean getOverview();
}
