package one.zagura.IonLauncher.util.drawable

import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.shapes.Shape
import android.os.Build

class SquircleRectShape(
    val tl: Float,
    val tr: Float,
    val br: Float,
    val bl: Float,
) : Shape() {

    constructor(radii: FloatArray) : this(radii[0], radii[1], radii[2], radii[3])

    private var path = Path()

    override fun onResize(w: Float, h: Float) {
        path = createPath(w, h, tl, tr, br, bl)
    }

    override fun getOutline(outline: Outline) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            outline.setPath(path)
        else outline.setConvexPath(path)
    }

    override fun draw(canvas: Canvas, paint: Paint) {
        canvas.drawPath(path, paint)
    }

    companion object {
        fun createPath(
            width: Float,
            height: Float,
            tl: Float,
            tr: Float,
            br: Float,
            bl: Float
        ): Path = Path().apply {
            moveTo(0f, tl)
            quadTo(0f, 0f, tl, 0f)
            lineTo(width - tr, 0f)
            quadTo(width, 0f, width, tr)
            lineTo(width, height - br)
            quadTo(width, height, width - br, height)
            lineTo(bl, height)
            quadTo(0f, height, 0f, height - bl)
            close()
        }
    }
}