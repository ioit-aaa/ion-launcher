package one.zagura.IonLauncher.util.drawable

import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.shapes.Shape
import android.os.Build

class SquircleRectShape(val radii: FloatArray) : Shape() {
    private var path = Path()

    override fun onResize(w: Float, h: Float) {
        path = Path().apply {
            moveTo(0f, radii[0])
            quadTo(0f, 0f, radii[0], 0f)
            lineTo(w - radii[1], 0f)
            quadTo(w, 0f, w, radii[1])
            lineTo(w, h - radii[2])
            quadTo(w, h, w - radii[2], h)
            lineTo(radii[3], h)
            quadTo(0f, h, 0f, h - radii[3])
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