package org.currency.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.EnumMap;
import java.util.Map;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRUtils {

    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    //Codes that identify the parameters of a qr code
    public static final String DEVICE_ID_KEY           = "did";
    public static final String ITEM_ID_KEY             = "iid";
    public static final String OPERATION_KEY           = "op";
    public static final String OPERATION_CODE_KEY      = "opc";
    public static final String PUBLIC_KEY_KEY          = "pk";
    public static final String SYSTEM_ENTITY_KEY       = "eid";
    public static final String MSG_KEY                 = "msg";
    public static final String URL_KEY                 = "url";
    public static final String UUID_KEY                = "uid";


    public static final String GET_BROWSER_CERTIFICATE = "0";
    public static final String MESSAGE_INFO            = "1";
    public static final String CURRENCY_SEND           = "2";



    public static Bitmap encodeAsBitmap(String contentsToEncode, Context context) throws WriterException {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point displaySize = new Point();
        display.getSize(displaySize);
        int width = displaySize.x;
        int height = displaySize.y;
        int smallerDimension = width < height ? width : height;
        smallerDimension = smallerDimension * 7 / 8;

        Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix result = null;
        try {
            result = new MultiFormatWriter().encode(contentsToEncode, BarcodeFormat.QR_CODE,
                    smallerDimension, smallerDimension, hints);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            return null;
        }
        width = result.getWidth();
        height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }
}
