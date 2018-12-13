package com.uni.cloud.chat.misc;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Spring on 2015/11/7.
 */
public class FileUtil {
    private String SDPATH;

    public FileUtil() {

    }

    public String getSDPATH() {
        return SDPATH;
    }

    public FileUtil(String SDPATH){
        //得到外部存储设备的目录（/SDCARD）
        SDPATH = Environment.getExternalStorageDirectory() + "/" ;
        Log.d("test", "FileUtil SDPATH="+SDPATH);
    }

    /**
     * 在SD卡上创建文件
     * @param fileName
     * @return
     * @throws java.io.IOException
     */
    public File createSDFile(String fileName) throws IOException {
        Log.d("test", "createSDFile fileName="+fileName);
        Log.d("test", "createSDFile SDPATH="+SDPATH);
        File file = new File(SDPATH + fileName);
        file.createNewFile();
        return file;
    }

    /**
     * 在SD卡上创建目录
     * @param dirName 目录名字
     * @return 文件目录
     */
    public File createDir(String dirName){
        Log.d("test", "createDir dirName="+dirName);
        Log.d("test", "createDir SDPATH="+SDPATH);
        File dir = new File(SDPATH + dirName);
        dir.mkdir();
        return dir;
    }

    /**
     * 判断文件是否存在
     * @param fileName
     * @return
     */
    public boolean isFileExist(String fileName){
        File file = new File(SDPATH + fileName);
        return file.exists();
    }

    public File write2SDFromInput(String path,String fileName,InputStream input){
        File file = null;
        OutputStream output = null;

        try {
            Log.d("test", "write2SDFromInput createDir!");
            createDir(path);
            file =createSDFile(path + fileName);
            Log.d("test", "write2SDFromInput file="+file);
            output = new FileOutputStream(file);
            byte [] buffer = new byte[4 * 1024];
            while(input.read(buffer) != -1){
                Log.d("test", "write2SDFromInput  buffer.length="+buffer.length);
                output.write(buffer);
                output.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }
}