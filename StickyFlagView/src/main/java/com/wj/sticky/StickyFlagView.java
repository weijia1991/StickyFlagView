package com.wj.sticky;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.BounceInterpolator;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ValueAnimator;
import com.wj.utils.DrawUtils;
import com.wj.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 有粘性的标记view
 * Created by jia.wei on 16/1/22.
 */
public class StickyFlagView extends View {

    private Context context;

    private ViewGroup parent;
    private ViewGroup.LayoutParams originalLp; // view原始layout
    private int[] originalLocation; // view原始的location
    private int originalWidth; // view原始的宽度
    private int originalHeight; // view原始的高度

    private float stickRadius; // 黏贴半径

    private int flagColor; // 标记颜色
    private int flagTextColor; // 标记文本颜色
    private float maxDragDistance; // 最大拖拽距离
    private String flagText; // 标记文本
    private float flagTextSize; // 标记文本大小
    private float flagRadius; // 标记半径
    private Bitmap flagBitmap; // 标记图片
    private float maxStickRadius; // 最大黏贴半径
    private float minStickRadius; // 最小黏贴半径
    private float rate = 0.8f;

    private boolean isFirstSizeChange = true;
    private boolean isTouched;
    private boolean isReachLimit; // 是否达到最大拖拽距离
    private boolean isRollBackAnimating; // 回滚动画是否在执行
    private boolean isDisappearAnimating; // 消失动画是否在执行
    private boolean isFlagDisappear; // 标记是否消失
    private boolean isViewLoadFinish; // view是否加载完毕
    private boolean isViewInWindow; // view是否在window中

    private int which;
    private List<Integer> disappearRes;

    private PointF stickPoint; // 黏贴点
    private PointF dragFlagPoint; // 拖拽标记点
    private Paint flagPaint; // 标记画笔
    private Paint flagTextPaint; // 标记文本画笔
    private Path flagPath;

    private OnFlagDisappearListener listener;
    private WindowManager windowManager;

    public StickyFlagView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public StickyFlagView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        initViewProperty(context, attrs);
        init();
    }

    public StickyFlagView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;

        initViewProperty(context, attrs);
        init();
    }

    /**
     * 初始化view属性
     */
    private void initViewProperty(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.StickyFlagView);

        flagColor = typedArray.getColor(R.styleable.StickyFlagView_flagColor, Color.RED);
        flagTextColor = typedArray.getColor(R.styleable.StickyFlagView_flagTextColor, Color.WHITE);
        flagTextSize = typedArray.getDimension(R.styleable.StickyFlagView_flagTextSize, ScreenUtils.spTopx(context, 12));
        maxDragDistance = typedArray.getDimension(R.styleable.StickyFlagView_maxDistance, ScreenUtils.getScreenHeight(context) / 6);
        minStickRadius = typedArray.getDimension(R.styleable.StickyFlagView_minStickRadius, ScreenUtils.dpToPx(context, 2));
        flagRadius = typedArray.getDimension(R.styleable.StickyFlagView_flagRadius, ScreenUtils.dpToPx(context, 10));
        maxStickRadius = typedArray.getDimension(R.styleable.StickyFlagView_maxStickRadius, flagRadius * rate);

        Drawable flagDrawable = typedArray.getDrawable(R.styleable.StickyFlagView_flagDrawable);
        if (flagDrawable != null) {
            flagBitmap = ((BitmapDrawable) flagDrawable).getBitmap();
        }

        typedArray.recycle();
    }

    private void init() {
        // 处理onDraw方法不执行的问题
        setWillNotDraw(false);

        // 这些默认值是为第一个构造函数准备的
        if (flagColor == 0) {
            flagColor = Color.RED;
        }
        if (flagTextColor == 0) {
            flagTextColor = Color.WHITE;
        }
        if (flagTextSize == 0) {
            flagTextSize = ScreenUtils.spTopx(context, 12);
        }
        if (flagRadius == 0) {
            flagRadius = ScreenUtils.dpToPx(context, 10);
        }
        if (maxDragDistance == 0) {
            maxDragDistance = ScreenUtils.getScreenHeight(context) / 6;
        }
        if (minStickRadius == 0) {
            minStickRadius = ScreenUtils.dpToPx(context, 2);
        }
        if (maxStickRadius == 0) {
            maxStickRadius = flagRadius * rate;
        }

        originalLocation = new int[2];
        stickPoint = new PointF();
        dragFlagPoint = new PointF();
        flagPath = new Path();

        flagPaint = new Paint();
        flagPaint.setAntiAlias(true);
        flagPaint.setColor(flagColor);

        flagTextPaint = new Paint();
        flagTextPaint.setAntiAlias(true);
        flagTextPaint.setColor(flagTextColor);
        flagTextPaint.setTextSize(flagTextSize);

        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);

        isViewLoadFinish = false;
        this.post(new Runnable() {
            @Override
            public void run() {
                isViewLoadFinish = true;
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 0;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode == MeasureSpec.UNSPECIFIED) {
            if (flagBitmap == null) {
                width = (int) ScreenUtils.dpToPx(context, 20);
            } else {
                width = flagBitmap.getWidth();
            }
        } else if (widthMode == MeasureSpec.EXACTLY){
            width = MeasureSpec.getSize(widthMeasureSpec);
        } else if (widthMode == MeasureSpec.AT_MOST) {
            if (flagBitmap == null) {
                width = (int) ScreenUtils.dpToPx(context, 20);
            } else {
                width = flagBitmap.getWidth();
            }
        }

        int height = 0;
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.UNSPECIFIED) {
            if (flagBitmap == null) {
                height = (int) ScreenUtils.dpToPx(context, 20);
            } else {
                height = flagBitmap.getHeight();
            }
        } else if (heightMode == MeasureSpec.EXACTLY){
            height = MeasureSpec.getSize(heightMeasureSpec);
        } else if (heightMode == MeasureSpec.AT_MOST) {
            if (flagBitmap == null) {
                height = (int) ScreenUtils.dpToPx(context, 20);
            } else {
                height = flagBitmap.getHeight();
            }
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (isFirstSizeChange) {
            parent = (ViewGroup) getParent();
            // StickyFlagView的父控件只能是RelativeLayout或FrameLayout
            if (!(parent instanceof RelativeLayout || parent instanceof FrameLayout)) {
                throw new RuntimeException("StickyFlagView can only be placed on the RelativeLayout or FrameLayout.");
            }

            // 记录view原始layout参数
            originalLp = getLayoutParams();
            originalWidth = width;
            originalHeight = height;

            getLocationOnScreen(originalLocation);
            originalLocation[1] = originalLocation[1] - ScreenUtils.getStatusHeight(context);

            if (flagBitmap == null) {
                float radius = Math.min(originalWidth, originalHeight) * 0.5f;
                flagRadius = flagRadius > radius ? radius : flagRadius;
                stickRadius = flagRadius > maxStickRadius ? maxStickRadius : flagRadius * rate;
            } else {
                // 黏贴半径不能超过图片宽和高的最小值的一半
                flagRadius = Math.min(flagBitmap.getWidth(), flagBitmap.getHeight()) * 0.5f;
                stickRadius = maxStickRadius > flagRadius ? flagRadius * rate : maxStickRadius;
            }

            // 黏贴点在原始view的中心点
            stickPoint.set((float) (originalWidth * 0.5), (float) (originalHeight * 0.5));
            isFirstSizeChange = false;
        } else {
            // view的size改变之后，修正黏贴点坐标
            if (originalWidth == width && originalHeight == height) {
                stickPoint.set((float) (originalWidth * 0.5), (float) (originalHeight * 0.5));
            } else {
                stickPoint.x += originalLocation[0];
                stickPoint.y += originalLocation[1];
            }
        }
        dragFlagPoint.x = stickPoint.x;
        dragFlagPoint.y = stickPoint.y;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.TRANSPARENT);

        if (!isFlagDisappear) {
            if (isTouched || isRollBackAnimating) {
                if (!isReachLimit) {
                    drawStickCircle(canvas);
                    drawStickCurve(canvas);
                }
                drawDragFlag(canvas);
            } else {
                if (!isDisappearAnimating) {
                    drawDragFlag(canvas);
                }
            }

            if (isDisappearAnimating) {
                drawDisappearFlagBitmap(canvas);
            }

            if (!isReachLimit) {
                drawFlagText(canvas);
            } else {
                if (!isDisappearAnimating) {
                    drawFlagText(canvas);
                }
            }
        }
    }

    /**
     * 绘制黏贴圆点
     */
    private void drawStickCircle(Canvas canvas) {
        canvas.drawCircle(stickPoint.x, stickPoint.y, stickRadius, flagPaint);
    }

    /**
     * 绘制拖拽标记
     */
    private void drawDragFlag(Canvas canvas) {
        if (flagBitmap == null) {
            canvas.drawCircle(dragFlagPoint.x, dragFlagPoint.y, flagRadius, flagPaint);
        } else {
            canvas.drawBitmap(flagBitmap, dragFlagPoint.x - flagBitmap.getWidth() * 0.5f,
                    dragFlagPoint.y - flagBitmap.getHeight() * 0.5f, flagPaint);
        }
    }

    /**
     * 绘制黏贴曲线
     */
    private void drawStickCurve(Canvas canvas) {
        float stickOffsetX = (float) (stickRadius * Math.sin(Math.atan((dragFlagPoint.y - stickPoint.y) / (dragFlagPoint.x - stickPoint.x))));
        float stickOffsetY = (float) (stickRadius * Math.cos(Math.atan((dragFlagPoint.y - stickPoint.y) / (dragFlagPoint.x - stickPoint.x))));
        float flagOffsetX = (float) (flagRadius * Math.sin(Math.atan((dragFlagPoint.y - stickPoint.y) / (dragFlagPoint.x - stickPoint.x))));
        float flagOffsetY = (float) (flagRadius * Math.cos(Math.atan((dragFlagPoint.y - stickPoint.y) / (dragFlagPoint.x - stickPoint.x))));

        float x1 = stickPoint.x - stickOffsetX;
        float y1 = stickPoint.y + stickOffsetY;

        float x2 = dragFlagPoint.x - flagOffsetX;
        float y2 = dragFlagPoint.y + flagOffsetY;

        float x3 = dragFlagPoint.x + flagOffsetX;
        float y3 = dragFlagPoint.y - flagOffsetY;

        float x4 = stickPoint.x + stickOffsetX;
        float y4 = stickPoint.y - stickOffsetY;

        // 曲线控制点
        float controlPointX = (float) ((stickPoint.x + dragFlagPoint.x) * 0.5);
        float controlPointY = (float) ((stickPoint.y + dragFlagPoint.y) * 0.5);

        flagPath.reset();
        flagPath.moveTo(x1, y1);
        flagPath.quadTo(controlPointX, controlPointY, x2, y2);
        flagPath.lineTo(x3, y3);
        flagPath.quadTo(controlPointX, controlPointY, x4, y4);
        flagPath.lineTo(x1, y1);

        canvas.drawPath(flagPath, flagPaint);
    }

    /**
     * 绘制标记文本
     */
    private void drawFlagText(Canvas canvas) {
        if (!TextUtils.isEmpty(flagText)) {
            DrawUtils.drawTextInCenter(canvas, flagTextPaint, flagText, dragFlagPoint.x, dragFlagPoint.y);
        }
    }

    /**
     * 绘制标记消失的Bitmap
     */
    private void drawDisappearFlagBitmap(Canvas canvas) {
        if (disappearRes != null) {
            Drawable drawable = context.getResources().getDrawable(disappearRes.get(which));
            if (drawable != null) {
                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                canvas.drawBitmap(bitmap, (float) (dragFlagPoint.x - bitmap.getWidth() * 0.5),
                        (float) (dragFlagPoint.y - bitmap.getHeight() * 0.5), flagPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isViewLoadFinish || isRollBackAnimating || isDisappearAnimating || isFlagDisappear) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isTouched = true;
                addViewInWindow();
                break;
            case MotionEvent.ACTION_MOVE:
                dragFlagPoint.x = event.getRawX();
                dragFlagPoint.y = event.getRawY() - ScreenUtils.getStatusHeight(context);

                double distance = Math.sqrt(Math.pow(dragFlagPoint.y - stickPoint.y, 2) + Math.pow(dragFlagPoint.x - stickPoint.x, 2));
                if (distance > maxDragDistance) {
                    isReachLimit = true;
                } else {
                    isReachLimit = false;
                    stickRadius = (float) (maxStickRadius * (1 - distance / maxDragDistance));
                    stickRadius = stickRadius < minStickRadius ? minStickRadius : stickRadius;
                }

                postInvalidate();
                break;
            case MotionEvent.ACTION_UP:
                isTouched = false;
                if (isReachLimit) {
                    launchDisappearAnimation(1000);
                    if (listener != null) {
                        listener.onFlagDisappear(this);
                    }
                } else {
                    launchRollBackAnimation(300);
                }
                break;
        }
        return true;
    }

    /**
     * 把view加入应用窗口
     */
    private void addViewInWindow() {
        if (isViewLoadFinish && !isViewInWindow) {
            if (parent != null) {
                // 将view从它的父控件中移除
                parent.removeView(this);
            }

            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
            layoutParams.format = PixelFormat.TRANSPARENT;
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            layoutParams.gravity = Gravity.START | Gravity.TOP;
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.x = 0;
            layoutParams.y = 0;

            if (windowManager != null) {
                // 将view加入window
                windowManager.addView(this, layoutParams);
                post(new Runnable() {
                    @Override
                    public void run() {
                        isViewInWindow = true;
                    }
                });
            }
        }
    }

    /**
     * 还原view
     */
    private void restoreView() {
        if (isViewLoadFinish) {
            // 还原黏贴半径
            stickRadius = flagRadius > maxStickRadius ? maxStickRadius : flagRadius * rate;
            isReachLimit = false;

            if (windowManager != null && isViewInWindow) {
                // 把view从window中移除
                windowManager.removeView(this);
                isViewInWindow = false;

                if (parent != null) {
                    parent.addView(this, originalLp);
                    // 在高版本的SDK上，没有这段代码，view可能不会刷新
                    post(new Runnable() {
                        @Override
                        public void run() {
                            parent.invalidate();
                        }
                    });
                }
            }
        } else {
            post(new Runnable() {
                @Override
                public void run() {
                    restoreView();
                }
            });
        }
    }

    /**
     * 启动标记的消失动画
     */
    private void launchDisappearAnimation(long duration) {
        disappearRes = new ArrayList<>();
        disappearRes.add(R.drawable.disappear0);
        disappearRes.add(R.drawable.disappear1);
        disappearRes.add(R.drawable.disappear2);
        disappearRes.add(R.drawable.disappear3);
        disappearRes.add(R.drawable.disappear4);

        isDisappearAnimating = true;
        ValueAnimator animator = ValueAnimator.ofInt(0, 4);
        animator.setDuration(duration);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                which = (int) animation.getAnimatedValue();
                postInvalidate();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                isFlagDisappear = true;
                isDisappearAnimating = false;
                restoreView();
            }
        });
        animator.start();
    }

    /**
     * 启动标记的回滚动画
     */
    private void launchRollBackAnimation(long duration) {
        isRollBackAnimating = true;
        // 过黏贴点和拖拽点的直线的斜率
        final float slope = (dragFlagPoint.y - stickPoint.y) / (dragFlagPoint.x - stickPoint.x);

        // 有拖拽点向黏贴点靠近
        ValueAnimator rollBackAnim = ValueAnimator.ofFloat(dragFlagPoint.x, stickPoint.x);
        rollBackAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float curX = (float) animation.getAnimatedValue();
                // 由过黏贴点和拖拽点的直线的方程求出y坐标
                float curY = slope * (curX - stickPoint.x) + stickPoint.y;

                dragFlagPoint.x = curX;
                dragFlagPoint.y = curY;
                double distance = Math.sqrt(Math.pow(dragFlagPoint.y - stickPoint.y, 2) + Math.pow(dragFlagPoint.x - stickPoint.x, 2));
                stickRadius = (float) (maxStickRadius * (1 - distance / maxDragDistance));

                postInvalidate();
            }
        });
        rollBackAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                isRollBackAnimating = false;
                restoreView();
            }
        });
        rollBackAnim.setInterpolator(new BounceInterpolator()); // 弹跳插值器
        rollBackAnim.setDuration(duration);
        rollBackAnim.start();
    }

    public String getFlagText() {
        return flagText;
    }

    public void setFlagText(String flagText) {
        this.flagText = flagText;
        isFlagDisappear = false;

        requestParentInvalidate();
    }

    public void setFlagColor(@ColorInt int flagColor) {
        if (flagPaint != null) {
            flagPaint.setColor(flagColor);
        }
    }

    public void setFlagTextColor(@ColorInt int flagTextColor) {
        if (flagTextPaint != null) {
            flagTextPaint.setColor(flagTextColor);
        }
    }

    public void setMaxDistance(float maxDistance) {
        this.maxDragDistance = maxDistance;
    }

    public void setFlagTextSize(float flagTextSize) {
        if (flagTextPaint != null) {
            flagTextPaint.setTextSize(flagTextSize);
        }
    }

    public void setFlagRadius(float flagRadius) {
        this.flagRadius = flagRadius;
    }

    public void setFlagDrawable(@DrawableRes int drawableRes) {
        Drawable drawable = context.getResources().getDrawable(drawableRes);
        if (drawable != null) {
            this.flagBitmap = ((BitmapDrawable) drawable).getBitmap();
        }
    }

    public void setMaxStickRadius(float maxStickRadius) {
        this.maxStickRadius = maxStickRadius;
    }

    public void setMinStickRadius(float minStickRadius) {
        this.minStickRadius = minStickRadius;
    }

    private void requestParentInvalidate() {
        if (parent != null) {
            // 此处尝试过使用this.invalidate(), 但不起作用, view不会刷新
            // 可能与布局的优化机制有关，它认为并不需要重绘
            parent.invalidate();
        }
    }

    public void setOnFlagDisappearListener(OnFlagDisappearListener listener) {
        this.listener = listener;
    }

    public interface OnFlagDisappearListener {
        void onFlagDisappear(StickyFlagView view);
    }

}
