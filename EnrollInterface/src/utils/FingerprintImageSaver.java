package utils;

import java.io.IOException;

public class FingerprintImageSaver {
    private String fingerprintStorage;

    public FingerprintImageSaver(String fingerprintStoragePath){
        this.fingerprintStorage = fingerprintStoragePath;
    }

    public String saveTemplateAsImage(byte[] imgBuf, int imageWidth, int imageHeight, String name){
        String path = this.fingerprintStorage + "/" + name;

        try {
            this.writeBitmapToFile(imgBuf, imageWidth, imageHeight, path);
            return path;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    private void writeBitmapToFile(byte[] imageBuf, int nWidth, int nHeight, String path) throws IOException {
        java.io.FileOutputStream fos = new java.io.FileOutputStream(path);
        java.io.DataOutputStream dos = new java.io.DataOutputStream(fos);

        int w = (((nWidth+3)/4)*4);
        int bfType = 0x424d;
        int bfSize = 54 + 1024 + w * nHeight;
        int bfReserved1 = 0;
        int bfReserved2 = 0;
        int bfOffBits = 54 + 1024;

        dos.writeShort(bfType);
        dos.write(ByteArrayUtils.intToByteArray(bfSize), 0, 4);
        dos.write(ByteArrayUtils.intToByteArray(bfReserved1), 0, 2);
        dos.write(ByteArrayUtils.intToByteArray(bfReserved2), 0, 2);
        dos.write(ByteArrayUtils.intToByteArray(bfOffBits), 0, 4);

        int biSize = 40;
        int biWidth = nWidth;
        int biHeight = nHeight;
        int biPlanes = 1;
        int biBitcount = 8;
        int biCompression = 0;
        int biSizeImage = w * nHeight;
        int biXPelsPerMeter = 0;
        int biYPelsPerMeter = 0;
        int biClrUsed = 0;
        int biClrImportant = 0;

        dos.write(ByteArrayUtils.intToByteArray(biSize), 0, 4);
        dos.write(ByteArrayUtils.intToByteArray(biWidth), 0, 4);
        dos.write(ByteArrayUtils.intToByteArray(biHeight), 0, 4);
        dos.write(ByteArrayUtils.intToByteArray(biPlanes), 0, 2);
        dos.write(ByteArrayUtils.intToByteArray(biBitcount), 0, 2);
        dos.write(ByteArrayUtils.intToByteArray(biCompression), 0, 4);
        dos.write(ByteArrayUtils.intToByteArray(biSizeImage), 0, 4);
        dos.write(ByteArrayUtils.intToByteArray(biXPelsPerMeter), 0, 4);
        dos.write(ByteArrayUtils.intToByteArray(biYPelsPerMeter), 0, 4);
        dos.write(ByteArrayUtils.intToByteArray(biClrUsed), 0, 4);
        dos.write(ByteArrayUtils.intToByteArray(biClrImportant), 0, 4);

        for (int i = 0; i < 256; i++) {
            dos.writeByte(i);
            dos.writeByte(i);
            dos.writeByte(i);
            dos.writeByte(0);
        }

        byte[] filter = null;
        if (w > nWidth)
        {
            filter = new byte[w-nWidth];
        }

        for(int i=0;i<nHeight;i++)
        {
            dos.write(imageBuf, (nHeight-1-i)*nWidth, nWidth);
            if (w > nWidth)
                dos.write(filter, 0, w-nWidth);
        }
        dos.flush();
        dos.close();
        fos.close();
    }

    public static String leftPad(String string, int length){
        StringBuilder sb = new StringBuilder();

        for (int toPrepend=length-string.length(); toPrepend>0; toPrepend--) {
            sb.append('0');
        }

        sb.append(string);
        return sb.toString();
    }


}
