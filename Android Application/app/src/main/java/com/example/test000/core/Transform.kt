package com.example.test000.core

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.example.test000.R


val pageCompositePageTransformer = CompositePageTransformer().apply {
    addTransformer(MarginPageTransformer(40))
    addTransformer { page, position ->
        val r = 1 - kotlin.math.abs(position)
        page.scaleY = 0.85f + r * 0.15f
    }
}


fun setParallaxTransformation(page: View, position: Float) {
    page.apply {
        val parallaxView = this.findViewById<ImageView>(R.id.img)
        when {
            position < -1 -> // [-Infinity,-1)
                // This page is way off-screen to the left.
                alpha = 1f

            position <= 1 -> { // [-1,1]
                parallaxView.translationX = -position * (width / 2) //Half the normal speed
            }

            else -> // (1,+Infinity]
                // This page is way off-screen to the right.
                alpha = 1f
        }
    }

}