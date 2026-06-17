package com.tuhoang.pocketmind.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

private const val DURATION = 280

fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInFromEnd() =
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(DURATION)) +
        fadeIn(tween(DURATION))

fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToStart() =
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(DURATION)) +
        fadeOut(tween(DURATION / 2))

fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInFromStart() =
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(DURATION)) +
        fadeIn(tween(DURATION))

fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToEnd() =
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(DURATION)) +
        fadeOut(tween(DURATION / 2))
