/*
 * @Copyright: 2005-2018 www.hyjf.com. All rights reserved.
 */
package com.sandman.download.service.system;

import com.sandman.download.base.BaseService;
import com.sandman.download.dao.mysql.system.model.auto.User;

/**
 * @author sunpeikai
 * @version LoginService, v0.1 2018/12/5 14:54
 */
public interface LoginService extends BaseService {
    /**
     * 收集登录日志
     * @auth sunpeikai
     * @param
     * @return
     */
    int updateLoginLog(int userId);
}
