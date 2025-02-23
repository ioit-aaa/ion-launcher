package one.zagura.IonLauncher.util.drawable

import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.shapes.Shape
import android.os.Build
import one.zagura.IonLauncher.util.drawable.SquircleRectShape.Companion.createPath

class UniformSquircleRectShape(val r: Float) : Shape() {
    private var path = Path()

    override fun onResize(w: Float, h: Float) {
        path = createPath(w, h, r, r, r, r)
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