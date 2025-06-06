package com.stripe.android.connect.example.ui.embeddedcomponentmanagerloader

import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.example.core.Async
import com.stripe.android.connect.example.core.Uninitialized

data class EmbeddedComponentManagerLoaderState(
    val embeddedComponentManagerAsync: Async<EmbeddedComponentManager> = Uninitialized,
)
