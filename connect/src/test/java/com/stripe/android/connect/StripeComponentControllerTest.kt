package com.stripe.android.connect

import android.view.ViewGroup
import androidx.core.view.children
import com.google.common.truth.Truth.assertThat
import com.stripe.android.connect.test.TestActivity
import com.stripe.android.connect.test.TestComponentController
import com.stripe.android.connect.test.TestComponentView
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class StripeComponentControllerTest {

    private val activityController = Robolectric.buildActivity(TestActivity::class.java)

    @Test
    fun `init checks context`() {
        val activity = activityController.create().get()
        verify(activity.embeddedComponentCoordinator).checkContextDuringCreate(activity)
    }

    @Test
    fun `init initializes the dialog fragment with listeners`() {
        val activity = activityController.create().get()
        val controller = activity.controller
        val df = controller.dialogFragment
        assertThat(df.listener).isEqualTo(controller.listener)
        assertThat(df.onDismissListener).isEqualTo(controller.onDismissListener)
    }

    @Test
    fun `show and dismiss shows and dismisses the dialog fragment respectively`() {
        val controller = activityController.create().start().resume().get().controller
        assertThat(controller.dialogFragment.isAdded).isFalse()

        controller.show()
        awaitMainLooper()
        assertThat(controller.dialogFragment.isAdded).isTrue()

        controller.dismiss()
        awaitMainLooper()
        assertThat(controller.dialogFragment.isAdded).isFalse()
    }

    @Test
    fun `existing dialog fragment is reused`() {
        val activity = activityController.create().start().resume().get()

        // Use first controller to show the DF.
        val controller = TestComponentController(activity, activity.embeddedComponentManager)
        val dialogFragment = controller.dialogFragment
        controller.show()
        awaitMainLooper()

        // Second controller should obtain the DF that was added.
        val newController = TestComponentController(activity, activity.embeddedComponentManager)
        assertThat(newController.dialogFragment).isSameInstanceAs(dialogFragment)
    }

    @Test
    fun `dialog fragment is able to create component view on recreate`() {
        // Show the DF
        val firstActivity = activityController.create().start().resume().get()
        firstActivity.controller.show()
        awaitMainLooper()
        val firstDialogFragment = firstActivity.controller.dialogFragment

        // Recreate activity to simulate a config change.
        val secondActivity = activityController.recreate().get()
        val secondDialogFragment = secondActivity.controller.dialogFragment

        // New DF should have been recreated by the OS and still added.
        assertThat(secondDialogFragment).isNotSameInstanceAs(firstDialogFragment)
        assertThat(secondDialogFragment.isAdded).isTrue()

        // The component view should be able to be recreated from the ECM cached in the VM.
        val componentView =
            (secondDialogFragment.view as ViewGroup)
                .children.filterIsInstance<TestComponentView>()
                .firstOrNull()
        assertThat(componentView).isNotNull()
        assertThat(componentView?.listener).isEqualTo(secondActivity.listener)
    }

    private fun awaitMainLooper() {
        ShadowLooper.shadowMainLooper().idle()
    }
}
