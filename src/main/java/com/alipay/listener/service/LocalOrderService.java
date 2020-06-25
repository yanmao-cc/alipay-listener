package com.alipay.listener.service;

import com.alipay.listener.model.OrderModel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class LocalOrderService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String dirPath = "./data";

    public void append(OrderModel model){
        append(new LinkedList<OrderModel>() {{add(model);}});
    }

    public void append(List<OrderModel> orderModels){
        try {
            File dirFile = new File(dirPath);
            if (!dirFile.exists()) {
                dirFile.mkdir();
            }
            LinkedHashMap<String,OrderModel> data = getList();
            for (OrderModel model : orderModels) {
                String todayPath = dirPath + "/" + LocalDate.parse(model.getCreatedTime(),DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".txt";
                File todayFile = new File(todayPath);
                if (!todayFile.exists()) {
                    todayFile.createNewFile();
                }
                if(data.containsKey(model.getId())) return;
                append(todayPath, MessageFormat.format("{0}|{1}|{2}|{3}|{4}",model.getId(),model.getAmount(),model.getAccount(),model.getName(),model.getCreatedTime()));
            }
        }catch (Exception e){
            logger.error(e.getLocalizedMessage());
        }
    }

    public LinkedHashMap<String,OrderModel> getList(){
        // 获取最多最近7个文件
        List<String> paths = getFiles(7);
        LinkedHashMap<String,OrderModel> linkedHashMap = new LinkedHashMap<>();
        StringBuilder data = new StringBuilder();
        for (String path : paths ) {
            File file = new File(path);
            if(file.exists()){
                data.append(readFileToString(path));
            }
        }
        String lineSeparator = System.getProperty("line.separator", "\n");
        String[] datas = data.toString().split(lineSeparator);
        for (String str : datas){
            if(StringUtils.isEmpty(str.trim())) continue;
            String[] info = str.split("\\|");
            OrderModel orderModel = new OrderModel(info[0],Double.valueOf(info[1]),info[2],info[3],info[4]);
            linkedHashMap.put(orderModel.getId(),orderModel);
        }
        return linkedHashMap;
    }

    /**
     * 追加文件：使用FileWriter
     *
     * @param fileName
     * @param content
     */
    public void append(String fileName, String content) {
        RandomAccessFile randomAccessFile = null;
        FileLock lock = null;
        try {
            randomAccessFile = new RandomAccessFile(fileName, "rw");
            FileChannel fileChannel = randomAccessFile.getChannel();
            while(true){
                try{
                    lock = fileChannel.tryLock();
                    if(lock != null ){
                        break;
                    }
                }catch(Exception e){
                    Thread.sleep(200);
                }
            }
            randomAccessFile.seek(randomAccessFile.length());
            String lineSeparator = System.getProperty("line.separator", "\n");
            randomAccessFile.write((content + lineSeparator).getBytes("UTF-8"));
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
        } finally {
            try {
                lock.release();
                randomAccessFile.close();
            } catch (IOException e) {
               logger.error(e.getLocalizedMessage());
            }
        }
    }

    public String readFileToString(String fileName) {
        String encoding = "UTF-8";
        File file = new File(fileName);
        Long fileLength = file.length();
        byte[] fileArray = new byte[fileLength.intValue()];
        RandomAccessFile randomAccessFile = null;
        FileLock lock = null;
        try {
            randomAccessFile = new RandomAccessFile(file,"rw");
            FileChannel fileChannel = randomAccessFile.getChannel();

            while(true){
                try{
                    lock = fileChannel.tryLock();
                    if(lock != null ){
                        break;
                    }
                }catch(Exception e){
                    Thread.sleep(200);
                }
            }
            randomAccessFile.read(fileArray);
            return new String(fileArray, encoding);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            return null;
        } finally {
            try {
                lock.release();
                randomAccessFile.close();
            } catch (IOException e) {
               logger.error(e.getLocalizedMessage());
            }
        }
    }

    public List<String> getFiles(int count) {
        List<String> list = new LinkedList<>();
        File root = new File(dirPath);
        if (root.exists()) {
            File[] files = root.listFiles();
            Arrays.sort(files, new ComparatorByLastModified());
            for (File file : files) {
                if (!file.isDirectory() && file.getName().indexOf(".txt") > 0) {
                    list.add(file.getPath());
                    if(count > 0 && list.size() >= count) break;
                }
            }
        }
        return list;
    }

    //根据文件修改时间进行比较的内部类
    static class ComparatorByLastModified implements Comparator<File> {

        public int compare(File f1, File f2) {
            long diff = f1.lastModified() - f2.lastModified();
            if (diff > 0) {
                return 1;
            } else if (diff == 0) {
                return 0;
            } else {
                return -1;
            }
        }
    }

}
