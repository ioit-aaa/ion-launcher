package one.zagura.IonLauncher.util.drawable

import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.shapes.Shape
import android.os.Build

class UniformSquircleRectShape(val r: Float) : Shape() {
    private var path = Path()

    override fun onResize(w: Float, h: Float) {
        path = Path().apply {
            moveTo(0f, r)
            quadTo(0f, 0f, r, 0f)
            lineTo(w - r, 0f)
            quadTo(w, 0f, w, r)
            lineTo(w, h - r)
            quadTo(w, h, w - r, h)
            lineTo(r, h)
            quadTo(0f, h, 0f, h - r)
            close()
        }
    }

    override fun getOutline(outline: Outline) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            outline.setPath(path)
        else outline.setConvexPath(path)
    }
    
    override fun draw(canvas: Canvas, paint: Paint) {
        canvas.drawPath(path, paint)
    }
}