package com.wj.utils;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * 绘图工具类
 * Created by jia.wei on 15/7/29.
 */
public class DrawUtils {

    /**
     * 测量文本内容宽度
     *
     * @param text  文本内容
     * @param paint 画笔
     * @return 内容宽度
     */
    public static float measureTextWidth(String text, Paint paint) {
        float width = paint.measureText(text);

        return width;
    }

    /**
     * 测量文本内容高度
     *
     * @param paint 画笔
     * @return 内容高度
     */
    public static float measureTextHeight(Paint paint) {
        Paint.FontMetrics fm = paint.getFontMetrics();
        float height = fm.bottom - fm.top;

        return height;
    }

    /**
     * 文本内容相对于centerX点居中
     *
     * @param canvas  画布
     * @param paint   画笔
     * @param text    文本内容
     * @param centerX x轴方向中心点
     * @param y       文本内容y轴方向偏移量
     */
    public static void drawTextByCenterX(Canvas canvas, Paint paint, String text, float centerX, float y) {
        canvas.drawText(text, (float) (centerX - measureTextWidth(text, paint) * 0.5), y, paint);
    }

    /**
     * 文本内容相对于centerY点居中
     *
     * @param canvas  画布
     * @param paint   画笔
     * @param text    文本内容
     * @param x       文本内容x轴方向偏移量
     * @param centerY y轴方向中心点
     */
    public static void drawTextByCenterY(Canvas canvas, Paint paint, String text, float x, float centerY) {
        float bottom = paint.getFontMetrics().bottom;
        canvas.drawText(text, x, (float) (0.5 * measureTextHeight(paint) - bottom + centerY), paint);
    }

    /**
     * 文本内容相对于某一点居中
     *
     * @param canvas  画布
     * @param paint   画笔
     * @param text    文本内容
     * @param centerX x轴方向中心点
     * @param centerY y轴方向中心点
     */
    public static void drawTextInCenter(Canvas canvas, Paint paint, String text, float centerX, float centerY) {
        float bottom = paint.getFontMetrics().bottom;
        canvas.drawText(text, (float) (centerX - measureTextWidth(text, paint) * 0.5),
                (float) (0.5 * measureTextHeight(paint) - bottom + centerY), paint);
    }

}
