package com.patbaumgartner.android.spacedrepeater.service

import android.content.pm.PackageManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewReminderPermissionTest {
    @Test
    fun `permission is not required before API 33`() {
        assertTrue(
            notificationPermissionSatisfied(
                sdkInt = 32,
                permissionResult = PackageManager.PERMISSION_DENIED,
            ),
        )
    }

    @Test
    fun `API 33 requires granted notification permission`() {
        assertFalse(
            notificationPermissionSatisfied(
                sdkInt = 33,
                permissionResult = PackageManager.PERMISSION_DENIED,
            ),
        )
        assertTrue(
            notificationPermissionSatisfied(
                sdkInt = 33,
                permissionResult = PackageManager.PERMISSION_GRANTED,
            ),
        )
    }
}
