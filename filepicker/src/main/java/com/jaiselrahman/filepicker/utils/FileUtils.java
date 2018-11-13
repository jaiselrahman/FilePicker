package com.jaiselrahman.filepicker.utils;

import android.webkit.MimeTypeMap;

/**
 * <b>功能描述：</b>文件帮助类<br />
 * <b>创建作者：</b>Hsiang Leekwok <br />
 * <b>创建时间：</b>2018/11/13 08:15 <br />
 * <b>作者邮箱：</b>xiang.l.g@gmail.com <br />
 * <b>最新版本：</b>Version: 1.0.0 <br />
 * <b>修改时间：</b>2018/11/13 08:15  <br />
 * <b>修改人员：</b><br />
 * <b>修改备注：</b><br />
 */
public class FileUtils {

    public static String getMimeTypeByFullPath(String filePath) {
        String ext = MimeTypeMap.getFileExtensionFromUrl(filePath);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
    }
}
