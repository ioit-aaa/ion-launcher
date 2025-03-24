package one.zagura.IonLauncher.ui.smartspacer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.appwidget.AppWidgetHostView
import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.makeMeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.allViews
import androidx.viewpager.widget.ViewPager
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants.SMARTSPACER_PACKAGE_NAME
import com.kieronquinn.app.smartspacer.sdk.client.BuildConfig
import com.kieronquinn.app.smartspacer.sdk.client.SmartspacerClient
import com.kieronquinn.app.smartspacer.sdk.client.helper.SmartspacerHelper
import com.kieronquinn.app.smartspacer.sdk.client.utils.repeatOnAttached
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView.SmartspaceTargetInteractionListener
import com.kieronquinn.app.smartspacer.sdk.client.views.popup.Popup
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat
import com.kieronquinn.app.smartspacer.sdk.utils.shouldExcludeFromSmartspacer
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import one.zagura.IonLauncher.provider.ColorThemer
import one.zagura.IonLauncher.ui.view.LongPressMenu
import one.zagura.IonLauncher.ui.view.DotIndicator
import one.zagura.IonLauncher.util.Settings
import one.zagura.IonLauncher.util.Utils
import kotlin.math.roundToInt

open class CustomSmartspaceView(context: Context) : FrameLayout(context), SmartspaceTargetInteractionListener {

    private val viewPager = ViewPager(context).apply {
        isSaveEnabled = false
        addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                indicator.setPageOffset(position, positionOffset)
                popup?.dismiss()
                popup = null
            }
            override fun onPageSelected(position: Int) {}
            override fun onPageScrollStateChanged(state: Int) {
                scrollState = state
                if (state == 0) pendingTargets?.let {
                    pendingTargets = null
                    onSmartspaceTargetsUpdate(it)
                }
            }
        })
    }
    private val indicator = DotIndicator(context).apply {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private val provider = SmartspacerHelper(SmartspacerClient.getInstance(context), SmartspaceConfig(
        5, UiSurface.HOMESCREEN, context.packageName, sdkVersion = BuildConfig.SDK_VERSION
    ))
    private val adapter = CardPagerAdapter(this)
    private var scrollState = ViewPager.SCROLL_STATE_IDLE
    private var pendingTargets: List<SmartspaceTarget>? = null
    private var runningAnimation: Animator? = null
    private var isResumed = false
    private var popup: Popup? = null

    init {
        addView(viewPager, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(indicator, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))
        onResume()
        val targets = provider.targets
        repeatOnAttached {
            viewPager.adapter = adapter
            targets.onEach(::onSmartspaceTargetsUpdate)
                .launchIn(this)
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) onResume()
        else onPause()
    }
    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) onResume()
        else onPause()
    }
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) onResume()
        else onPause()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        provider.onCreate()
    }
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        provider.onDestroy()
    }

    private fun onPause() {
        if (!isResumed) return
        isResumed = false
        provider.onPause()
    }
    private fun onResume() {
        if (isResumed) return
        isResumed = true
        provider.onResume()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val smartspaceHeight = 0
//            context.resources.getDimensionPixelSize(R.dimen.smartspace_height)
        if (height <= 0 || height >= smartspaceHeight) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            scaleX = 1f
            scaleY = 1f
            return
        }

        val scale = height.toFloat() / smartspaceHeight.toFloat()
        val width = (MeasureSpec.getSize(widthMeasureSpec).toFloat() / scale).roundToInt()
        super.onMeasure(
            makeMeasureSpec(width, EXACTLY),
            makeMeasureSpec(smartspaceHeight, EXACTLY)
        )
        scaleX = scale
        scaleY = scale
        pivotX = 0f
        pivotY = smartspaceHeight.toFloat() / 2f
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        viewPager.setOnLongClickListener(l)
    }

    open fun onSmartspaceTargetsUpdate(targets: List<SmartspaceTarget>) {
        if (adapter.count > 1 && scrollState != ViewPager.SCROLL_STATE_IDLE) {
            pendingTargets = targets
            return
        }

        val sortedTargets = targets.sortedByDescending { it.score }.toMutableList()
        val isRtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val currentItem = viewPager.currentItem
        val index = if (isRtl) adapter.count - currentItem else currentItem
        if (isRtl) {
            sortedTargets.reverse()
        }

        val oldCard = adapter.getCardAtPosition(currentItem)
        adapter.setTargets(sortedTargets)
        val count = adapter.count
        if (isRtl) {
            viewPager.setCurrentItem((count - index).coerceIn(0 until count), false)
        }
        indicator.setDotCount(targets.size)
        oldCard?.let { animateSmartspaceUpdate(it) }
        adapter.notifyDataSetChanged()
    }

    private fun animateSmartspaceUpdate(oldCard: View) {
        if (runningAnimation != null || oldCard.parent != null ||
            //Don't animate widget updates
            oldCard.allViews.any { it is AppWidgetHostView }) return

        val animParent = viewPager.parent as ViewGroup
        oldCard.measure(makeMeasureSpec(viewPager.width, EXACTLY), makeMeasureSpec(viewPager.height, EXACTLY))
        oldCard.layout(viewPager.left, viewPager.top, viewPager.right, viewPager.bottom)
        val shift = 0//resources.getDimension(R.dimen.smartspace_dismiss_margin)
        val animator = AnimatorSet()
        animator.play(
            ObjectAnimator.ofFloat(
                oldCard,
                View.TRANSLATION_Y,
                0f,
                (-height).toFloat() - shift
            )
        )
        animator.play(ObjectAnimator.ofFloat(oldCard, View.ALPHA, 1f, 0f))
        animator.play(
            ObjectAnimator.ofFloat(
                viewPager,
                View.TRANSLATION_Y,
                height.toFloat() + shift,
                0f
            )
        )
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                animParent.overlay.add(oldCard)
            }

            override fun onAnimationEnd(animation: Animator) {
                animParent.overlay.remove(oldCard)
                runningAnimation = null
            }
        })
        runningAnimation = animator
        animator.start()
    }

    override fun onInteraction(target: SmartspaceTarget, actionId: String?) {
        provider.onTargetInteraction(target, actionId)
    }

    @SuppressLint("RestrictedApi")
    override fun onLongPress(target: SmartspaceTarget): Boolean {
        val current = adapter.getTargetAtPosition(viewPager.currentItem) ?: return false
        if(current != target) return false //Page has changed
        val launchIntent = context.packageManager.getLaunchIntentForPackage(SMARTSPACER_PACKAGE_NAME)
        val feedbackIntent = target.baseAction?.extras
            ?.getParcelableCompat(SmartspaceAction.KEY_EXTRA_FEEDBACK_INTENT, Intent::class.java)
            ?.takeIf { !it.shouldExcludeFromSmartspacer() }
        val aboutIntent = target.baseAction?.extras
            ?.getParcelableCompat(SmartspaceAction.KEY_EXTRA_ABOUT_INTENT, Intent::class.java)
            ?.takeIf { !it.shouldExcludeFromSmartspacer() }
        val shouldShowSettings = launchIntent != null
        val shouldShowDismiss = target.featureType != SmartspaceTarget.FEATURE_WEATHER
                && target.canBeDismissed
        if (!shouldShowDismiss && !shouldShowSettings && feedbackIntent == null
            && aboutIntent == null) return false
        val dismissAction = if(shouldShowDismiss) {
            ::dismissAction
        } else null
        this.popup = LongPressMenu.popupSmartspacer(context, this, target, ::launchIntent, dismissAction, aboutIntent, feedbackIntent, launchIntent)
        return true
    }

    private fun dismissAction(smartspaceTarget: SmartspaceTarget) {
        provider.onTargetDismiss(smartspaceTarget)
    }

    private fun launchIntent(intent: Intent?){
        if (intent == null) return
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
//            Toast.makeText(context, R.string.smartspace_long_press_popup_failed_to_launch, Toast.LENGTH_LONG).show()
        }
    }

    fun applyCustomizations(settings: Settings, sideMargin: Int) {
        setPadding(0, sideMargin.coerceAtLeast(Utils.getStatusBarHeight(context) + sideMargin / 4), 0, 0)
        indicator.setPadding(sideMargin, 0, sideMargin, 0)
        val fg = ColorThemer.wallForeground(context)
        adapter.applyCustomizations(fg, sideMargin)
        indicator.color = fg
//        adapter.setApplyShadowIfRequired(applyShadowIfRequired)
    }
}
