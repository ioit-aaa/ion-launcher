package one.zagura.IonLauncher.ui

import android.app.Activity
import android.app.Dialog
import android.app.WallpaperManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.ui.view.settings.WallpaperDragView
import one.zagura.IonLauncher.util.TaskRunner
import one.zagura.IonLauncher.util.Utils
import one.zagura.IonLauncher.util.drawable.FillDrawable
import one.zagura.IonLauncher.util.drawable.UniformSquircleRectShape
import kotlin.math.max

class WallpaperApplicationActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent.data ?: return
        var wallpaper = Drawable.createFromStream(contentResolver.openInputStream(data), null)
            ?: return finish()
        if (wallpaper is BitmapDrawable) {
            val b = wallpaper.bitmap
            val w = resources.displayMetrics.widthPixels
            val h = resources.displayMetrics.heightPixels
            if (b.width > w && b.height > h) {
                val s = max(w.toFloat() / b.width, h.toFloat() / b.height)
                val bb = Bitmap.createScaledBitmap(b, (b.width * s).toInt(), (b.height * s).toInt(), true)
                wallpaper = BitmapDrawable(resources, bb)
            }
        }
        setContentView(createView(wallpaper))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            window.setDecorFitsSystemWindows(false)
        else window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        )
    }

    private fun createView(wallpaper: Drawable): View {
        return FrameLayout(this).apply {
            val wallView = WallpaperDragView(context, wallpaper)
            val dp = resources.displayMetrics.density
            addView(wallView, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(View(context).apply {
                background = LayerDrawable(arrayOf(
                    GradientDrawable().apply {
                        setStroke(dp.toInt(), 0x44000000, dp * 4, dp * 4)
                    },
                    GradientDrawable().apply {
                        setStroke(dp.toInt(), 0x55ffffff, dp * 4, dp * 4)
                    },
                )).apply {
                    setLayerInset(1, -dp.toInt(), 4 * dp.toInt(), 0, 0)
                    setLayerInset(0, -dp.toInt(), 0, 0, 0)
                }
            }, FrameLayout.LayoutParams(dp.toInt(), MATCH_PARENT, Gravity.CENTER))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                val h = (18 * dp).toInt()
                val v = (8 * dp).toInt()
                setPadding(h, v, h, v + Utils.getNavigationBarHeight(context))
                addView(TextView(context).apply {
                    setText(R.string.apply)
                    textSize = 16f
                    typeface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                        Typeface.create(null, 900, false)
                    else Typeface.DEFAULT_BOLD
                    setTextColor(resources.getColor(R.color.color_button_text))
                    val bg = ShapeDrawable(UniformSquircleRectShape(18 * dp)).apply {
                        paint.color = resources.getColor(R.color.color_button)
                    }
                    background = RippleDrawable(
                        ColorStateList.valueOf(resources.getColor(R.color.color_hint)), bg, null)
                    Palette.from(wallpaper.toBitmap(64, 64))
                        .setRegion(0, 36, 64, 64)
                        .generate {
                            it ?: return@generate
                            val swatch = it.vibrantSwatch ?: it.dominantSwatch ?: return@generate
                            bg.paint.color = swatch.rgb
                            setTextColor(swatch.bodyTextColor)
                        }
                    val h = (32 * dp).toInt()
                    val v = (15 * dp).toInt()
                    setPadding(h, v, h, v)
                    gravity = Gravity.CENTER_HORIZONTAL
                    setSingleLine()
                    isAllCaps = true
                    setOnClickListener {
                        val c = currentTextColor
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            Dialog(context).apply {
                                window!!.setBackgroundDrawable(bg.constantState?.newDrawable())
                                setContentView(LinearLayout(context).apply {
                                    orientation = LinearLayout.VERTICAL
                                    val p = (14 * dp).toInt()
                                    val tp = (14 * dp).toInt()
                                    setPadding(p, p, p, p)
                                    addView(TextView(context).apply {
                                        setText(R.string.home_screen)
                                        setTextColor(c)
                                        setPadding(tp, tp, tp, tp)
                                        textSize = 16f
                                        typeface = Typeface.DEFAULT_BOLD
                                        setOnClickListener {
                                            TaskRunner.submit {
                                                wallView.applyWallpaper(WallpaperManager.FLAG_SYSTEM)
                                            }
                                            finish()
                                        }
                                        background = RippleDrawable(
                                            ColorStateList.valueOf(resources.getColor(R.color.color_disabled)), FillDrawable(bg.paint.color), null)
                                    }, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                                    addView(TextView(context).apply {
                                        setText(R.string.lock_screen)
                                        setTextColor(c)
                                        setPadding(tp, tp, tp, tp)
                                        textSize = 16f
                                        typeface = Typeface.DEFAULT_BOLD
                                        setOnClickListener {
                                            TaskRunner.submit {
                                                wallView.applyWallpaper(WallpaperManager.FLAG_LOCK)
                                            }
                                            finish()
                                        }
                                        background = RippleDrawable(
                                            ColorStateList.valueOf(resources.getColor(R.color.color_disabled)), FillDrawable(bg.paint.color), null)
                                    }, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                                    addView(TextView(context).apply {
                                        setText(R.string.both)
                                        setTextColor(c)
                                        setPadding(tp, tp, tp, tp)
                                        textSize = 16f
                                        typeface = Typeface.DEFAULT_BOLD
                                        setOnClickListener {
                                            TaskRunner.submit {
                                                wallView.applyWallpaper(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                                            }
                                            finish()
                                        }
                                        background = RippleDrawable(
                                            ColorStateList.valueOf(resources.getColor(R.color.color_disabled)), FillDrawable(bg.paint.color), null)
                                    }, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                                }, ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
                            }.show()
                        } else wallView.applyWallpaper()
                    }
                }, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            }, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM))
        }
    }
}