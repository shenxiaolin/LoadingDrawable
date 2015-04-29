package com.alexjing.loadingdrawable;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.animation.OvershootInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alex on 15/4/29.
 */
public class LoadingDrawable extends Drawable implements Animatable {

    private static final int DEFAULT_DURATION = 800;
    private static final int DEFAULT_BORDER_DIP = 4;
    private static final int DEBAULT_LOADING_COLOR = 0XFF5677FC;
    private static final int DEBAULT_ERROR_COLOR = 0XFFE51C23;
    private static final int DEBAULT_SUCCESS_COLOR = 0XFF259B24;
    private static final int DEFAULT_MAX_SWEEP_ANGLE = 180;
    private static final int DEFAULT_MIN_SWEEP_ANGLE = 10;

    private static ArgbEvaluator sArgbEvaluator = new ArgbEvaluator();

    private Context mContext;

    private RectF mRectF = new RectF();

    private int mCurrentColor;

    private int mHookMaxLength;
    private int mSuccessMaxLength;


    private float mBorder;
    private float mCurrentRotationAngle;
    private float mMaxSweepAngle;
    private float mMinSweepAngle;
    private float mCurrentSweepAngle;
    private float mCenterX;
    private float mCenterY;
    private float mOffset;

    private Point mInflectionPoint = new Point();
    private Point mStartPoint = new Point();
    private Point mStopPoint = new Point();

    private boolean mFirstSweep = true;
    private boolean isRunning = false;
    private boolean isComplete = false;

    private List<Path> mErrorPathList = new ArrayList<>();
    private Path mSuccessPath = new Path();

    private ValueAnimator mRotationAnimator;
    private ValueAnimator mSweepAppearingAnimator;
    private ValueAnimator mSweepDisappearingAnimator;
    private ValueAnimator mSweepCompleteAnimator;
    private ValueAnimator mErrorAnimator;
    private ValueAnimator mSuccessAnimator;

    private Paint mPaint;

    private LoadingState mState = LoadingState.LOADING;

    public enum LoadingState {
        LOADING, ERROR, SUCCESS
    }

    public LoadingDrawable(Context context) {
        mContext = context;
        init();
        setupAnimations();
    }

    private void init() {
        mBorder = dip2px(mContext, DEFAULT_BORDER_DIP);
        mCurrentColor = DEBAULT_LOADING_COLOR;
        mMaxSweepAngle = DEFAULT_MAX_SWEEP_ANGLE;
        mMinSweepAngle = DEFAULT_MIN_SWEEP_ANGLE;

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(mCurrentColor);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(mBorder);
    }

    private void setupAnimations() {
        mRotationAnimator = ValueAnimator.ofFloat(0f, 1f);
        mRotationAnimator.setDuration(DEFAULT_DURATION);
        mRotationAnimator.setRepeatMode(ValueAnimator.RESTART);
        mRotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mRotationAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float fraction = getAnimatedFraction(animation);
                final float angle = fraction * 360f;
                setCurrentRotationAngle(angle);
            }
        });

        mSweepAppearingAnimator = ValueAnimator.ofFloat(0, 1f);
        mSweepAppearingAnimator.setDuration(DEFAULT_DURATION);
        mSweepAppearingAnimator.addListener(new Animator.AnimatorListener() {
            boolean cancelled;

            @Override
            public void onAnimationStart(Animator animation) {
                cancelled = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancelled) {
                    mFirstSweep = false;
                    switch (mState) {
                        case LOADING: {
                            mSweepDisappearingAnimator.start();
                        }
                        break;
                        case ERROR: {
                            mSweepCompleteAnimator.start();
                        }
                        break;
                        case SUCCESS: {
                            mSweepCompleteAnimator.start();
                        }
                        break;
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mSweepAppearingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float fraction = getAnimatedFraction(animation);
                final float angle;
                if (mFirstSweep) {
                    angle = fraction * mMaxSweepAngle;
                } else {
                    angle = fraction * (mMaxSweepAngle - mMinSweepAngle) + mMinSweepAngle;
                }
                setCurrentSweepAngle(angle);
            }
        });

        mSweepDisappearingAnimator = ValueAnimator.ofFloat(0, 1f);
        mSweepDisappearingAnimator.setDuration(DEFAULT_DURATION);
        mSweepDisappearingAnimator.addListener(new Animator.AnimatorListener() {
            boolean cancelled = false;

            @Override
            public void onAnimationStart(Animator animation) {
                cancelled = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancelled) {
                    mSweepAppearingAnimator.start();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mSweepDisappearingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float fraction = getAnimatedFraction(animation);
                final float angle = mMaxSweepAngle - fraction * (mMaxSweepAngle - mMinSweepAngle);
                setCurrentSweepAngle(angle);
            }
        });

        mSweepCompleteAnimator = ValueAnimator.ofFloat(0, 1f);
        mSweepCompleteAnimator.setDuration(DEFAULT_DURATION);
        mSweepCompleteAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float fraction = getAnimatedFraction(animation);
                final float angle = mMaxSweepAngle + fraction * (360 - mMaxSweepAngle);
                final int color;
                switch (mState) {
                    case SUCCESS: {
                        color = (int) sArgbEvaluator.evaluate(fraction, mCurrentColor, DEBAULT_SUCCESS_COLOR);
                    }
                    break;
                    case ERROR: {
                        color = (int) sArgbEvaluator.evaluate(fraction, mCurrentColor, DEBAULT_ERROR_COLOR);
                    }
                    break;
                    default:
                        color = DEBAULT_LOADING_COLOR;
                }
                mPaint.setColor(color);
                setCurrentSweepAngle(angle);

            }
        });
        mSweepCompleteAnimator.addListener(new Animator.AnimatorListener() {
            boolean cancelled = false;

            @Override
            public void onAnimationStart(Animator animation) {
                isComplete = false;
                cancelled = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isComplete = true;
                switch (mState) {
                    case ERROR: {
                        mErrorAnimator.start();
                    }
                    break;
                    case SUCCESS: {
                        mSuccessAnimator.start();
                    }
                    break;
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mErrorAnimator = ValueAnimator.ofFloat(0, 1f);
        mErrorAnimator.setDuration(DEFAULT_DURATION / 2);
        mErrorAnimator.setInterpolator(new OvershootInterpolator());
        mErrorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float fraction;
                if (Build.VERSION.SDK_INT > 11) {
                     fraction= animation.getAnimatedFraction();
                }else {
                    fraction = getAnimatedFraction(animation);
                }
                final float offset = mOffset * fraction;
                lineTo(offset);
            }
        });
        mErrorAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                stop();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        mSuccessAnimator = ValueAnimator.ofFloat(0, 1f);
        mSuccessAnimator.setDuration(DEFAULT_DURATION/2);
        mSuccessAnimator.setInterpolator(new OvershootInterpolator());
        mSuccessAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float fraction;
                if (Build.VERSION.SDK_INT > 11) {
                    fraction= animation.getAnimatedFraction();
                }else {
                    fraction = getAnimatedFraction(animation);
                }

                final int xLength = (int) (fraction * mSuccessMaxLength);
                final int inflaction = (int) (mHookMaxLength * 0.5);
                if (xLength < mHookMaxLength * 0.8) {
                    Point lineTo = new Point(mStartPoint.x + xLength, mStartPoint.y + xLength);
                    successLineTo(mStartPoint, lineTo);
                } else if (xLength == inflaction) {
                    mInflectionPoint.set(mStartPoint.x + xLength, mStartPoint.y + xLength);
                    successLineTo(mStartPoint, mInflectionPoint);
                } else{
                    if (xLength < mHookMaxLength) {
                        int x = xLength - inflaction;
                        Point lineTo = new Point(mInflectionPoint.x+x,mInflectionPoint.y-x);
                        successLineTo(mStartPoint,mInflectionPoint,lineTo);
                    }else {
                        int x = xLength - mHookMaxLength;
                        Point start = new Point(mStartPoint.x+x,mStartPoint.y+x);
                        int xx =(int) ( xLength -mHookMaxLength*.8f);
                        Point lineTo = new Point(mInflectionPoint.x+xx,mInflectionPoint.y-xx);
                        successLineTo(start,mInflectionPoint,lineTo);
                    }
                }
            }
        });
        mSuccessAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                stop();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

    }

    private void successLineTo(Point start, Point... lineTo) {
        mSuccessPath.reset();
        mSuccessPath.moveTo(start.x, start.y);
        for (Point p : lineTo) {
            mSuccessPath.lineTo(p.x, p.y);
        }
        invalidateSelf();
    }

    private void setCurrentRotationAngle(float angle) {
        mCurrentRotationAngle = angle;
        invalidateSelf();
    }

    private void setCurrentSweepAngle(float angle) {
        mCurrentSweepAngle = angle;
        invalidateSelf();
    }

    private void initPathList(RectF rect) {
        mCenterX = rect.width() / 2f + rect.left;
        mCenterY = rect.height() / 2f + rect.top;
        final float pathLenth = Math.min(rect.width() / 2f, rect.height() / 2f);
        mOffset = pathLenth * 5f/12;

        mErrorPathList.clear();

        for (int i = 0; i < 4; i++) {
            Path path = new Path();
            path.moveTo(mCenterX, mCenterY);
            mErrorPathList.add(path);
        }
    }

    private void lineTo(float offset) {
        for (int i = 0; i < mErrorPathList.size(); i++) {
            int xSign = i < 2 ? 1 : -1;
            int ySign = i > 0 && i < 3 ? -1 : 1;
            Path path = mErrorPathList.get(i);
            path.reset();
            path.moveTo(mCenterX, mCenterY);
            path.lineTo(xSign * offset + mCenterX, ySign * offset + mCenterY);
        }
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mRectF.left = bounds.left + mBorder / 2f + 0.5f;
        mRectF.right = bounds.right - mBorder / 2f;
        mRectF.top = bounds.top + mBorder / 2f + 0.5f;
        mRectF.bottom = bounds.bottom - mBorder / 2f;

        initPathList(mRectF);
        initPoints(mRectF);
    }

    private void initPoints(RectF mRectF) {
        final int inflectX = (int) (mCenterX - .12f * mRectF.width() / 2f + .5);
        final int inflectY = (int) (mCenterY + .375f * mRectF.height() / 2f + .5);
        mInflectionPoint.set(inflectX, inflectY);

        final int startX = (int) (mCenterX - .8f * mRectF.width() / 2f);
        final int startY = (int) (mCenterY - .39f * mRectF.height() / 2f);
        mStartPoint.set(startX, startY);

        final int stopX = (int) (mCenterX + .48f * mRectF.width() / 2f);
        final int stopY = (int) (mCenterY - .29 * mRectF.height() / 2f);
        mStopPoint.set(stopX, stopY);

        mHookMaxLength = (int) (mRectF.width()* 7/12f);
        mSuccessMaxLength = (int) (mRectF.width() * .8f);
    }

    @Override
    public void start() {
        if (isRunning()) {
            return;
        }
        mPaint.setColor(mCurrentColor);
        isRunning = true;
        isComplete = false;
        mState = LoadingState.LOADING;
        mRotationAnimator.start();
        mSweepAppearingAnimator.start();
    }

    @Override
    public void stop() {
        if (!isRunning()) {
            return;
        }
        mRotationAnimator.cancel();
        mSweepAppearingAnimator.cancel();
        mSweepDisappearingAnimator.cancel();
        isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void draw(Canvas canvas) {
        float startAngle = mCurrentRotationAngle;
        float sweepAngle = mCurrentSweepAngle;
        startAngle %= 360;
        canvas.drawArc(mRectF, startAngle, sweepAngle, false, mPaint);
        switch (mState) {
            case ERROR: {
                if (isComplete) {
                    drawErrorPaths(canvas);
                }
            }
            break;
            case SUCCESS:{
                if (isComplete){
                    drawSuccessPath(canvas);
                }
            }
        }

    }

    private void drawErrorPaths(Canvas canvas) {
        for (Path p : mErrorPathList) {
            canvas.drawPath(p, mPaint);
        }
    }

    private void drawSuccessPath(Canvas canvas){
        canvas.drawPath(mSuccessPath,mPaint);
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public int getOpacity() {
        return 0;
    }

    public float getBorder() {
        return mBorder;
    }

    public void setBorder(float border) {
        this.mBorder = border;
    }

    public LoadingState getLoadingState(){
        return mState;
    }
    public void setLoadingState(LoadingState state) {
        mState = state;
    }

    public float getMaxSweepAngle() {
        return mMaxSweepAngle;
    }

    public void setmMaxSweepAngle(float maxSweepAngle) {
        this.mMaxSweepAngle = maxSweepAngle;
    }

    public float getMinSweepAngle() {
        return mMinSweepAngle;
    }

    public void setMinSweepAngle(float minSweepAngle) {
        this.mMinSweepAngle = minSweepAngle;
    }

    public float dip2px(Context context,float dpValue){
        float density = context.getResources().getDisplayMetrics().density;
        return dpValue * density + 0.5f;
    }

    public float getAnimatedFraction(ValueAnimator animator) {
        float fraction = animator.getDuration() > 0 ? (float) animator.getCurrentPlayTime() / animator.getDuration() : 0f;
        fraction = Math.min(fraction, 1f);
        fraction = animator.getInterpolator().getInterpolation(fraction);
        return fraction;
    }
}
