package cn.cychee.imageservice.service;

import cn.cychee.imageservice.logic.ImageLoadLogic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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

    /**
     * 重定向的形式，直接返回图片url
     */
    @ResponseBody
    @GetMapping("getImageRedirect")
    public void getImageRedirect(HttpServletResponse response) throws IOException {
        response.sendRedirect("https://img.cychee.cn/Sakura.jpg");
    }

    @GetMapping("getMessage")
    public void sendMsg(HttpServletResponse response) throws IOException, InterruptedException {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("utf-8");
        for (int i = 0; i <= 100; i += 10) {
            write(response, "当前进度:" + i);
            Thread.sleep(1000);
        }
    }

    private void write(HttpServletResponse response, String content) throws IOException {
        response.getWriter().write(content + "");
        response.flushBuffer();
        response.getWriter().flush();
    }

    @ResponseBody
    @GetMapping(path = "subscribe", produces = {MediaType.TEXT_EVENT_STREAM_VALUE})
    public SseEmitter push(HttpServletResponse response) {
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setCharacterEncoding("utf-8");
        // 超时时间设置为1小时
        SseEmitter sseEmitter = new SseEmitter(3600_000L);

        sseEmitter.complete();
        sseEmitter.onCompletion(() -> System.out.println("完成！！！"));
        return sseEmitter;
    }

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> sseServer() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setCacheControl(CacheControl.noCache());
        SseEmitter emitter = new SseEmitter(60L * 1000L);
        emitter.onCompletion(() -> System.out.println("SseEmitter is completed"));
        emitter.onTimeout(() -> System.out.println("SseEmitter is timed out"));
        emitter.onError((e) -> System.out.println("SseEmitter is error"));

        new Thread(() -> {
            try {
                for (int i = 0; i <= 50; i += 10) {
                    emitter.send("当前进度:" + i);
                    Thread.sleep(1000);
                }
                emitter.complete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).headers(httpHeaders).body(emitter);
    }
}
