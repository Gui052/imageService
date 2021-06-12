package cn.itailan.imageservice.logic;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
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
    @PostConstruct
    public void init() {
        imagePaths = getImagePathList();
    }

    private List<String> getImagePathList() {
        List<String> imageList = new ArrayList<>();
        try {
            File file = new File(filePath);
            File[] files = file.listFiles(x -> imageExtension.contains(FilenameUtils.getExtension(x.getName())));
            if (files != null) {
                imageList = Arrays.stream(files).map(File::getPath).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("获取目录下图片列表失败", e);
        }
        return imageList;
    }

    /**
     * 获取随机图片
     *
     * @return 图片字节
     */
    public byte[] getRandomImage() {
        try {
            Random random = new Random();
            int index = random.nextInt(imagePaths.size() - 1);
            File file = new File(imagePaths.get(index));
            FileInputStream inputStream;
            inputStream = new FileInputStream(file);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes, 0, inputStream.available());
            return bytes;
        } catch (IOException e) {
            log.warn("获取图片失败", e);
        }
        return new byte[0];
    }
}
