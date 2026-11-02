package com.example.ui.keyboard

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.ui.theme.MyApplicationTheme

class CustomComposeImeService : InputMethodService(),
    LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        val sharedKeyboardStateHolder = KeyboardStateHolder()
    }

    private val keyboardStateHolder = sharedKeyboardStateHolder
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        // Provide fallback connection provider to avoid connection loss during rapid typing
        keyboardStateHolder.inputConnectionProvider = { currentInputConnection }
        keyboardStateHolder.initializeAudio(this)

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            val listener = ClipboardManager.OnPrimaryClipChangedListener {
                try {
                    val clipData = clipboard.primaryClip
                    if (clipData != null && clipData.itemCount > 0) {
                        val text = clipData.getItemAt(0).text?.toString()
                        if (!text.isNullOrBlank()) {
                            keyboardStateHolder.addClipboardItem(text)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            clipboard.addPrimaryClipChangedListener(listener)
            clipboardListener = listener
        }
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        window?.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(this)
            decor.setViewTreeViewModelStoreOwner(this)
            decor.setViewTreeSavedStateRegistryOwner(this)
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@CustomComposeImeService)
            setViewTreeViewModelStoreOwner(this@CustomComposeImeService)
            setViewTreeSavedStateRegistryOwner(this@CustomComposeImeService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MyApplicationTheme {
                    KeyboardView(stateHolder = keyboardStateHolder)
                }
            }
        }
        return composeView
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        keyboardStateHolder.activeInputConnection = currentInputConnection
        keyboardStateHolder.updateEditorInfo(attribute)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        keyboardStateHolder.activeInputConnection = currentInputConnection
        keyboardStateHolder.updateEditorInfo(info)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        // Keep activeInputConnection if auto-typing is active so it never stops until user stops it
        if (keyboardStateHolder.autoTypeState == AutoTypeState.RUNNING) {
            keyboardStateHolder.activeInputConnection = currentInputConnection
        } else {
            keyboardStateHolder.activeInputConnection = null
        }
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        // Do NOT cancel auto typing when keyboard window is hidden
        if (keyboardStateHolder.autoTypeState == AutoTypeState.RUNNING) {
            keyboardStateHolder.activeInputConnection = currentInputConnection
        }
    }

    override fun onFinishInput() {
        super.onFinishInput()
        if (keyboardStateHolder.autoTypeState == AutoTypeState.RUNNING) {
            keyboardStateHolder.activeInputConnection = currentInputConnection
        } else {
            keyboardStateHolder.activeInputConnection = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboardListener?.let { listener ->
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.removePrimaryClipChangedListener(listener)
        }
        if (keyboardStateHolder.autoTypeState != AutoTypeState.RUNNING) {
            keyboardStateHolder.activeInputConnection = null
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
