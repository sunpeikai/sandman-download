/*
 * @Copyright: 2005-2018 www.hyjf.com. All rights reserved.
 */
package com.sandman.film.crawler.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sandman.film.base.BaseServiceImpl;
import com.sandman.film.config.SystemConfig;
import com.sandman.film.crawler.bean.Link;
import com.sandman.film.crawler.service.DyttCrawlerService;
import com.sandman.film.crawler.thread.DyttLinkThread;
import com.sandman.film.crawler.thread.DyttRootThread;
import com.sandman.film.dao.mysql.film.model.auto.Film;
import com.sandman.film.dao.mysql.film.model.auto.FilmExample;
import com.sandman.film.dao.mysql.film.model.auto.FilmTypeExample;
import com.sandman.film.utils.DateUtils;
import com.sandman.film.utils.FileUtils;
import com.sandman.film.utils.HttpUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author sunpeikai
 * @version DyttService, v0.1 2019/1/29 9:17
 */
@Service
public class DyttCrawlerServiceImpl extends BaseServiceImpl implements DyttCrawlerService {

    private static String startWith = "http://117.48.197.114";

    /**
     * 从电影天堂爬取数据
     * @auth sunpeikai
     * @param
     * @return
     */
    @Override
    public void crawFilm(){
        // 查全部类型
        Link.putFilmTypes(filmTypeMapper.selectByExample(new FilmTypeExample()));
        // 获取电影天堂最近一次的更新时间
        Link.deadLine = getDeadLine();
        Link.root.add("https://www.dytt8.net/html/gndy/rihan/list_6_{index}.html");
        Link.root.add("https://www.dytt8.net/html/gndy/dyzz/list_23_{index}.html");
        Link.root.add("https://www.dytt8.net/html/gndy/oumei/list_7_{index}.html");
        Link.root.add("https://www.dytt8.net/html/gndy/china/list_4_{index}.html");
        Link.root.add("https://www.dytt8.net/html/gndy/jddy/list_63_{index}.html");
        for(int i=0;i<5;i++){
            Thread root = new Thread(new DyttRootThread());
            root.setName("ROOT" + i);
            root.start();
            Link.addThreadCount();
        }
        // 阻塞掉主进程，等待ROOT线程结束再运行Link线程
        canRun();
        // 去重
        removeObj();
        for(int i=0;i<5;i++){
            Thread thread = new Thread(new DyttLinkThread());
            thread.setName("URL" + i);
            thread.start();
            Link.addThreadCount();
        }
        // 阻塞掉主进程，等待Link线程结束再将剩余的数据插入数据库
        canRun();
        // flush缓存
        Link.finishCrawler();
        //FileUtils.writeInfoFile("URL线程共收录[" + Link.filmMap.size() + "]");

    }

    /**
     * 处理图片
     * @auth sunpeikai
     * @param
     * @return
     */
    @Override
    public void imageHandle(){
        List<Film> films = getNeedUpdateImage();
        // 先将图片更新到数据库，然后下载图片
        if(films.size()>0){
            updateImage(films);
            downloadImage(films);
        }
    }

    /**
     * 从数据库中获取最后一次发布时间
     * @auth sunpeikai
     * @param
     * @return
     */
    private Date getDeadLine(){
        FilmExample filmExample = new FilmExample();
        filmExample.setLimitStart(0);
        filmExample.setLimitEnd(1);
        filmExample.setOrderByClause("create_time desc");
        filmExample.createCriteria().andDelFlagEqualTo(0);
        List<Film> filmList = filmMapper.selectByExample(filmExample);
        if(!CollectionUtils.isEmpty(filmList)){
            return filmList.get(0).getCreateTime();
        }
        return null;
    }

    /**
     * 查询数据库中需要更新图片的数据
     * @auth sunpeikai
     * @param
     * @return
     */
    private List<Film> getNeedUpdateImage(){
        FilmExample filmExample = new FilmExample();
        filmExample.createCriteria().andDelFlagEqualTo(0).andStatusEqualTo(1).andFilmImageIsNull();
        filmExample.or().andDelFlagEqualTo(0).andStatusEqualTo(1).andFilmImageNotLike(startWith + "%");
        List<Film> films = filmMapper.selectByExample(filmExample);
        logger.info("一共需要更新 -> [" + films.size() + "]条数据");
        return films;
    }

    /**
     * 更新图片到内存中
     * @auth sunpeikai
     * @param
     * @return
     */
    private void updateImage(List<Film> filmList){
        for(Film film:filmList){
            try {
                if(StringUtils.isBlank(film.getFilmImage()) || !film.getFilmImage().startsWith(startWith)){
                    //如果数据库中图片是空的就开始更新
                    String url = "https://movie.douban.com/j/subject_suggest?q=";
                    String name = null;
                    if(film.getFilmName().contains("/")){
                        name = URLEncode(film.getFilmName().split("/")[0],"UTF-8");
                    }else{
                        name = URLEncode(film.getFilmName(),"UTF-8");
                    }
                    if(StringUtils.isNotBlank(name)){
                        url += name;
                        byte[] resultByte = HttpUtils.doGet(url);
                        String resultStr = new String(resultByte);
                        if(resultStr.length()>2){
                            JSONArray resultArr = JSONObject.parseArray(resultStr);
                            JSONObject result = resultArr.getJSONObject(0);
                            if(result != null && result.containsKey("img")){
                                String img = result.getString("img");
                                film.setFilmImage(img);
                            }
                        }else{
                            logger.error("line 168 -> " + JSON.toJSONString(film));
                        }
                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
                logger.error("line 176 -> " + JSON.toJSONString(film));
            }
        }
    }


    /**
     * 下载图片
     * @auth sunpeikai
     * @param
     * @return
     */
    private void downloadImage(List<Film> filmList){
        int count = 0;
        for(Film film:filmList){
            try {
                if(StringUtils.isNotBlank(film.getFilmImage())){
                    // 图片地址不为空
                    String newImage = "";
                    if(film.getFilmName().contains("/")){
                        newImage = downloadPic(film.getFilmImage(),film.getFilmName().split("/")[0]);
                    }else{
                        newImage = downloadPic(film.getFilmImage(),film.getFilmName());
                    }
                    film.setFilmImage(newImage);

                }else if(StringUtils.isBlank(film.getFilmImage()) && DateUtils.beforeSixHours(film.getUpdateTime())){
                    // 如果爬取完了图片还是空且更新日期是6小时之前
                    film.setStatus(0);
                }

                // 更新数据库
                filmMapper.updateByPrimaryKeySelective(film);
                count ++;
                System.out.println("第[" + count + "]条数据更新image成功 -> id:[" + film.getId() + "],name:[" + film.getFilmName() + "]");

            } catch (Exception e) {
                e.printStackTrace();
                logger.error(JSON.toJSONString(film));
            }
        }
    }

    private static void canRun(){
        while (Link.threadCount != 0){
            try{
                // 线程休眠3秒
                Thread.sleep(3000);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * 爬取结果去重
     * @auth sunpeikai
     * @param
     * @return
     */
    private static void removeObj(){
        long start = System.currentTimeMillis();
        List<Film> list = Link.films;
        for  ( int  i  =   0 ; i  <  list.size()  -   1 ; i ++ )  {
            for  ( int  j  =  list.size()  -   1 ; j  >  i; j -- )  {
                if  (list.get(j).getFilmName().equals(list.get(i).getFilmName()))  {
                    list.remove(j);
                }
            }
        }
        System.out.println("去重所耗时间:[" + (System.currentTimeMillis() - start) + "ms]");
    }

    /**
     * 获取当前时间的String类型
     * @auth sunpeikai
     * @param
     * @return
     */
    private static String getNowStryyyyMMddHHmmss(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        return simpleDateFormat.format(new Date());
    }

    /**
     * url编码
     * @auth sunpeikai
     * @param
     * @return
     */
    private static String URLEncode(String str,String code){
        try {
            return URLEncoder.encode(str,code);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 下载图片
     * @auth sunpeikai
     * @param
     * @return
     */
    private static String downloadPic(String url,String name){
        logger.info("图片下载地址" + url);
        String path = "http://117.48.197.114/film/";
        name = getNowStryyyyMMddHHmmss() + name + ".jpg";
        try {
            byte[] resultByte = HttpUtils.doGet(url);
            File file = new File(SystemConfig.getTempFilePath() + "/" + name);
            OutputStream outputStream = new FileOutputStream(file);
            outputStream.write(resultByte);
            outputStream.close();
            boolean success = FileUtils.upload(SystemConfig.getFilmImagePath(),name,file);//上传服务器
            if(success){
                file.delete();
                return path + name;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return url;
    }

}
