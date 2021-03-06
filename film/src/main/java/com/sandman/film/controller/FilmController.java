/*
 * @Copyright: 2005-2018 www.hyjf.com. All rights reserved.
 */
package com.sandman.film.controller;

import com.alibaba.fastjson.JSON;
import com.sandman.film.base.BaseController;
import com.sandman.film.base.BaseResult;
import com.sandman.film.bean.film.FilmBean;
import com.sandman.film.constant.ReturnMessage;
import com.sandman.film.dao.mysql.film.model.auto.Film;
import com.sandman.film.dao.mysql.film.model.auto.FilmLog;
import com.sandman.film.dao.mysql.film.model.auto.FilmType;
import com.sandman.film.dao.mysql.system.model.auto.User;
import com.sandman.film.service.film.FilmService;
import com.sandman.film.utils.SessionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/**
 * @author sunpeikai
 * @version FilmController, v0.1 2019/1/21 16:15
 */
@Controller
@RequestMapping(value = "/film")
public class FilmController extends BaseController {

    @Autowired
    private FilmService filmService;

    @GetMapping(value = "/get_info")
    public ModelAndView getInfo(Integer id){
        Film film = filmService.getFilmById(id);
        return new ModelAndView("film_info").addObject("film",film);
    }

    @ResponseBody
    @GetMapping(value = "/buy_film")
    public BaseResult buyFilm(Integer id){
        logger.info("购买影片,id:[{}]",id);
        User user = filmService.getUserByUserId(SessionUtils.getUserId());
        if(user!=null){
            FilmLog filmLog = filmService.getFilmLogByUserIdAndFilmId(user.getUserId(),id);
            // 判断是否重复购买
            if(filmLog == null){
                // 未购买
                Film film = filmService.getFilmById(id);
                // 判断是否存在
                if(film!=null){
                    // 判断用户积分是否足以购买该影片
                    if(user.getGold()>film.getFilmGold()){
                        // 积分足够可以购买影片
                        int result = filmService.buyFilm(user,film);
                        if(result>0){
                            return new BaseResult();
                        }else{
                            return new BaseResult(ReturnMessage.ERR_USER_BUY_FILM);
                        }
                    }
                    // 积分不足
                    return new BaseResult(ReturnMessage.ERR_USER_GOLD_NOT_ENOUGH);
                }
                // 资源不存在
                return new BaseResult(ReturnMessage.ERR_RESOURCE_NOT_EXIST);
            }
            // 已购买
            return new BaseResult(ReturnMessage.ERR_USER_ALREADY_BUY);
        }
        return new BaseResult(ReturnMessage.ERR_USER_LOGIN_INVALID);
    }

    @GetMapping(value = "/search")
    public ModelAndView search(FilmBean filmBean){
        logger.info("搜索资源 -> search:[{}]", JSON.toJSONString(filmBean));
        if(StringUtils.isBlank(filmBean.getSearch()) || "undefined".equals(filmBean.getSearch())){
            filmBean.setSearch(null);
        }
        if(null != filmBean.getCurrPage()){
            filmBean.setCurrPage((filmBean.getCurrPage()<1)?1:filmBean.getCurrPage());
            filmBean.computeLimit();
        }else{
            filmBean.setCurrPage(1);
            filmBean.computeLimit();
        }

        // 按照创建时间排序
        filmBean.setType(0);
        int count = filmService.getFilmCountByType(filmBean);
        List<Film> filmList = filmService.searchFilm(filmBean);
        List<FilmType> filmTypeList = filmService.getAllType();
        return new ModelAndView("search")
                .addObject("filmList",filmList)
                .addObject("search",filmBean.getSearch())
                .addObject("count",count)
                .addObject("currPage",filmBean.getCurrPage())
                .addObject("filmTypeList",filmTypeList);
    }

    @ResponseBody
    @GetMapping(value = "/get_download_url")
    public BaseResult getDownloadUrl(Integer id,Integer type){
        // type -> 1:磁力链接,2:迅雷链接
        logger.info("get film download url -> id:[{}],type:[{}]",id,type);
        if(type != null){
            Film film = filmService.getFilmById(id);
            film.setBuyCount(film.getBuyCount() + 1);
            filmService.updateFilm(film);
            if(type == 1){
                return new BaseResult(film.getMagnetUrl());
            }else if(type == 2){
                return new BaseResult(film.getThunderUrl());
            }else{
                return new BaseResult(ReturnMessage.ERR_OBJECT_VALUE,"传入类型");
            }
        }
        return new BaseResult(ReturnMessage.ERR_OBJECT_VALUE,"传入类型");
    }

    @ResponseBody
    @GetMapping(value = "/hot_film")
    public BaseResult hotResources(){
        List<Film> hotFilm = filmService.getHotFilm();
        return new BaseResult(hotFilm);
    }
}
