package cn.itailan.imageservice.service;

import cn.itailan.imageservice.logic.ImageLoadLogic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 获取随机图片
 */
@Slf4j
@RestController
@RequestMapping("RandomImgService")
public class RandomImgService {
    @Resource
    private ImageLoadLogic imageLoadLogic;

    @GetMapping(value = "getImage", produces = MediaType.IMAGE_JPEG_VALUE)
    @ResponseBody
    public byte[] getImage() {
      return imageLoadLogic.getRandomImage();
    }
}
