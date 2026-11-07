// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL

package com.metallic.chiaki.stream

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.metallic.chiaki.common.LogManager
import com.metallic.chiaki.session.StreamSession
import com.metallic.chiaki.common.Preferences
import com.metallic.chiaki.lib.*
import com.metallic.chiaki.session.StreamInput
import com.metallic.chiaki.session.DeepLTranslator
import com.metallic.chiaki.session.MlKitTranslator
import com.metallic.chiaki.session.SubtitleTranslator
import java.io.File

class StreamViewModel(val application: Application, val connectInfo: ConnectInfo): ViewModel()
{
	val preferences = Preferences(application)
	val logManager = LogManager(application)

	private var _session: StreamSession? = null
	val input = StreamInput(application, preferences)
	// shortcut: model is pushed by hand (adb), add an in-app download for real users
	val session = StreamSession(connectInfo, logManager, preferences.logVerbose, input,
		application.getExternalFilesDir(null)?.let { File(it, "vosk-model") },
		subtitleTranslatorFactory())

	private fun subtitleTranslatorFactory(): (() -> SubtitleTranslator)?
	{
		val deepLKey = preferences.deepLKey
		return when(preferences.subtitleTranslation)
		{
			Preferences.SubtitleTranslation.OFF -> null
			Preferences.SubtitleTranslation.MLKIT -> ::MlKitTranslator
			Preferences.SubtitleTranslation.DEEPL -> if(deepLKey.isBlank()) null else { { DeepLTranslator(deepLKey) } }
		}
	}

	private var _onScreenControlsEnabled = MutableLiveData<Boolean>(preferences.onScreenControlsEnabled)
	val onScreenControlsEnabled: LiveData<Boolean> get() = _onScreenControlsEnabled

	private var _touchpadOnlyEnabled = MutableLiveData<Boolean>(preferences.touchpadOnlyEnabled)
	val touchpadOnlyEnabled: LiveData<Boolean> get() = _touchpadOnlyEnabled

	override fun onCleared()
	{
		super.onCleared()
		_session?.shutdown()
	}

	fun setOnScreenControlsEnabled(enabled: Boolean)
	{
		preferences.onScreenControlsEnabled = enabled
		_onScreenControlsEnabled.value = enabled
	}

	fun setTouchpadOnlyEnabled(enabled: Boolean)
	{
		preferences.touchpadOnlyEnabled = enabled
		_touchpadOnlyEnabled.value = enabled
	}
}