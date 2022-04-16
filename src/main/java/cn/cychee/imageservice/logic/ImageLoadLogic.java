package cn.cychee.imageservice.logic;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 图片加载
 */
@Slf4j
@Service
public class ImageLoadLogic {

    private static final List<String> imageExtension = ImmutableList.<String>builder()
            .add("jpg", "png")
            .build();
    private List<String> imagePaths = new ArrayList<>();

    @Value("${folder.path}")
    private String filePath;

    @Value("${redirect}")
    private String redirectUrl;

    @Value("${useUrl}")
    private Boolean useUrl;

    @Value("${scale:1}")
    private Float scale;

    @Resource
    private Executor saveFileThreadPool;

    private static final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build();

    private List<String> getImagePathList() {
        List<String> imageList = new ArrayList<>();
        if (filePath == null || filePath.isEmpty()) {
            return imageList;
        }
        try {
            File file = new File(filePath);
            File[] files = file.listFiles(x -> x.getName().charAt(0) != '.' && imageExtension.contains(FilenameUtils.getExtension(x.getName())));
            if (files != null) {
                imageList = Arrays.stream(files).map(File::getPath).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("获取目录下图片列表失败", e);
        }
        return imageList;
    }

    /**
     * 定时获取图片路径
     */
    @Scheduled(cron = "*/5 * * * * ?")
    public void scheduleGetImageList() {
        if (!useUrl) {
            imagePaths = getImagePathList();
        }
    }

    /**
     * 获取随机图片
     *
     * @return 图片字节
     */
    public byte[] getRandomImage() {
        //如果本地图片为空，则走url
        if (useUrl) {
            Request request = new Request.Builder().url(redirectUrl).get().build();
            try {
                Response response = okHttpClient.newCall(request).execute();
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    byte[] bytes = responseBody.bytes();

                    saveFileThreadPool.execute(() -> {
                        String result = ImageBase64Utils.bytesToBase64(bytes);
                        //输出图片的路径
                        String fileName = DigestUtils.md5Hex(result);
                        if (filePath.charAt(filePath.length() - 1) != '/') {
                            filePath += '/';
                        }
                        String fileOutPath = filePath + fileName + ".jpg";
                        File file = new File(fileOutPath);
                        if (!file.exists()) {
                            try {
                                ImageBase64Utils.base64ToImageFile(result, fileOutPath);
                            } catch (Exception e) {
                                log.error("图片保存失败", e);
                            }
                        }
                    });

                    byte[] resByte = new byte[bytes.length];
                    BeanUtils.copyProperties(bytes, resByte);
                    return compression(bytes);
                }
            } catch (Exception e) {
                log.error("图片请求失败", e);
            }
        } else {
            try {
                if (imagePaths.isEmpty()) {
                    imagePaths = getImagePathList();
                }
                Random random = new Random();
                int index = random.nextInt(imagePaths.size() - 1);
                File file = new File(imagePaths.get(index));
                log.info("图片名:{}", file.getName());

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Thumbnails.of(file).scale(scale).outputFormat("jpg").toOutputStream(out);

                /*FileInputStream inputStream;
                inputStream = new FileInputStream(file);
                byte[] bytes = new byte[inputStream.available()];
                inputStream.read(bytes, 0, inputStream.available());
                inputStream.close();*/

                return out.toByteArray();
            } catch (IOException e) {
                log.warn("获取图片失败", e);
            }
        }

        return new byte[0];
    }

    public byte[] compression(byte[] imageByte) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(imageByte);
        BufferedImage image = ImageIO.read(in);

        /*BufferedImage res = Thumbnails.of(image).scale(0.4f).asBufferedImage();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(res, "jpg", out);*/

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(image).scale(scale).outputFormat("jpg").toOutputStream(out);
        return out.toByteArray();
    }
}
