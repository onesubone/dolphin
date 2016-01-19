package org.dolphin.secret.core;

import org.dolphin.lib.IOUtil;
import org.dolphin.secret.FileConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * Created by hanyanan on 2016/1/15.
 */
public class ReadableFileInputStream extends InputStream {
    private final File file;
    private FileInfo fileInfo = null;
    private long nextReadIndex = 0;
    private FileInfoContentCache contentCache;
    private RandomAccessFile randomAccessFile;
    private Integer markLimit = new Integer(0);

    public ReadableFileInputStream(File file) throws FileNotFoundException {
        this.file = file;
        randomAccessFile = new RandomAccessFile(file, "r");
    }

    @Override
    public int read() throws IOException {
        try {
            FileInfo fileInfo = getFileInfo();
            if (nextReadIndex >= fileInfo.originalFileLength) {
                return -1;
            }

            int transferSize = fileInfo.transferSize;
            if (nextReadIndex < transferSize) {
                // 需要从加密过的头部读取信息
                FileInfoContentCache cache = getContentCache();
                int index = (int) (nextReadIndex++);
                return 255 & cache.headBodyContent[index];
            }

            if (nextReadIndex >= fileInfo.originalFileLength - transferSize) {
                // 需要从加密过的尾部读取信息
                FileInfoContentCache cache = getContentCache();
                int index = (int) (nextReadIndex++ - (fileInfo.originalFileLength - transferSize));
                return 255 & cache.footBodyContent[index];
            }
            // 从中间读取
            if (nextReadIndex == transferSize) { // 正在起始的边界位置
                randomAccessFile.seek(nextReadIndex);
            }
            int res = randomAccessFile.read();
            nextReadIndex++;
            return res;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new IOException(throwable);
        } finally {

        }
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        int result = 0;
        try {
            FileInfo fileInfo = getFileInfo();
            if (nextReadIndex >= fileInfo.originalFileLength) {
                return -1;
            }
            int arrayLength = buffer.length;
            if ((byteOffset | byteCount) < 0 || byteOffset > arrayLength || arrayLength - byteOffset < byteCount) {
                throw new ArrayIndexOutOfBoundsException();
            }

            long left = nextReadIndex;
            long right = nextReadIndex + byteCount - 1;
            right = right >= fileInfo.originalFileLength ? fileInfo.originalFileLength - 1 : right;
            // 读取的区域包括 [left. right]
            int copyCount = (int) (right - left + 1);
            result = copyCount;
            if (copyCount <= 0) return copyCount;

            if (left < fileInfo.transferSize) { // 头部[0,transferSize)
                FileInfoContentCache cache = getContentCache();
                int maxCopyCount = (int) (fileInfo.transferSize - left);
                maxCopyCount = maxCopyCount > copyCount ? copyCount : maxCopyCount;
                System.arraycopy(cache.headBodyContent, (int) left, buffer, byteOffset, maxCopyCount);
                copyCount -= maxCopyCount;
                byteOffset += maxCopyCount;
                left += maxCopyCount;
            }
            if (copyCount <= 0) return result;

            if (left < fileInfo.originalFileLength - fileInfo.transferSize) { // 读取文件中间的内容
                randomAccessFile.seek(left);
                int maxCopyCount = (int) (fileInfo.originalFileLength - fileInfo.transferSize - left);
                maxCopyCount = maxCopyCount > copyCount ? copyCount : maxCopyCount;
                randomAccessFile.read(buffer, byteOffset, maxCopyCount);
                copyCount -= maxCopyCount;
                byteOffset += maxCopyCount;
                left += maxCopyCount;
            }
            if (copyCount <= 0) return result;

            if (left >= fileInfo.originalFileLength - fileInfo.transferSize) {
                FileInfoContentCache cache = getContentCache();
                System.arraycopy(cache.footBodyContent, (int) (left - (fileInfo.originalFileLength - fileInfo.transferSize)),
                        buffer, byteOffset, copyCount);
            }
            return result;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new IOException(throwable);
        } finally {
            nextReadIndex += result;
        }
    }

    @Override
    public int available() throws IOException {
        return super.available();
    }

    @Override
    public void mark(int readlimit) {
        markLimit = Integer.valueOf(readlimit);
    }

    @Override
    public boolean markSupported() {
        return true;
    }


    @Override
    public synchronized void reset() throws IOException {
        nextReadIndex = markLimit;
        randomAccessFile.seek(nextReadIndex);
    }

    @Override
    public long skip(long byteCount) throws IOException {
        FileInfo fileInfo = getFileInfo();
        long max = fileInfo.originalFileLength - nextReadIndex;
        byteCount = max > byteCount ? byteCount : max;
        nextReadIndex += byteCount;
        randomAccessFile.seek(nextReadIndex);
        return byteCount;
    }


    @Override
    public void close() throws IOException {
        super.close();
    }

    private FileInfoContentCache getContentCache() {
        if (null == contentCache) {
            contentCache = createContentCache(file, getFileInfo());
        }

        return contentCache;
    }

    private FileInfo getFileInfo() throws Throwable {
        if (null == fileInfo) {
            FileInfoReaderOperator operator = new FileInfoReaderOperator();
            fileInfo = operator.operate(file);
        }
        return fileInfo;
    }

    private static FileInfoContentCache createContentCache(File file, FileInfo fileInfo) throws FileNotFoundException, IOException {
        FileInfoContentCache cache = new FileInfoContentCache();
        FileInfo.Range headRange = fileInfo.originalFileHeaderRange;
        FileInfo.Range footRange = fileInfo.originalFileFooterRange;
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
            randomAccessFile.seek(footRange.offset);
            byte[] readedContent = new byte[footRange.count];
            randomAccessFile.readFully(readedContent);
            cache.footBodyContent = FileConstants.decode(readedContent);

            randomAccessFile.seek(headRange.offset);
            byte[] readedHeadContent = new byte[headRange.count];
            randomAccessFile.readFully(readedHeadContent);
            cache.headBodyContent = FileConstants.decode(readedHeadContent);
        } finally {
            IOUtil.safeClose(randomAccessFile);
        }

        return cache;
    }
}
