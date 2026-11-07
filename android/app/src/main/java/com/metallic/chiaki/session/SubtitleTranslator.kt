// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL

package com.metallic.chiaki.session

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Blocking, call from a background thread. */
interface SubtitleTranslator
{
	fun translate(text: String): String
	fun close() {}
}

// shortcut: English to Turkish only, make the languages settings when other games/models are needed
class MlKitTranslator: SubtitleTranslator
{
	private val client = Translation.getClient(TranslatorOptions.Builder()
		.setSourceLanguage(TranslateLanguage.ENGLISH)
		.setTargetLanguage(TranslateLanguage.TURKISH)
		.build())

	init
	{
		// ~30MB language pack, only downloaded the first time
		Tasks.await(client.downloadModelIfNeeded(DownloadConditions.Builder().build()))
	}

	override fun translate(text: String): String = Tasks.await(client.translate(text))
	override fun close() = client.close()
}

class DeepLTranslator(private val authKey: String): SubtitleTranslator
{
	companion object
	{
		/** Free plan keys end with ":fx" and must use the free endpoint. */
		fun endpoint(authKey: String) =
			if(authKey.trim().endsWith(":fx")) "https://api-free.deepl.com/v2/translate"
			else "https://api.deepl.com/v2/translate"
	}

	override fun translate(text: String): String
	{
		val conn = URL(endpoint(authKey)).openConnection() as HttpURLConnection
		try
		{
			conn.requestMethod = "POST"
			conn.connectTimeout = 3000
			conn.readTimeout = 3000
			conn.doOutput = true
			conn.setRequestProperty("Authorization", "DeepL-Auth-Key ${authKey.trim()}")
			conn.setRequestProperty("Content-Type", "application/json")
			val body = JSONObject()
				.put("text", JSONArray().put(text))
				.put("target_lang", "TR")
			conn.outputStream.use { it.write(body.toString().toByteArray()) }
			if(conn.responseCode != 200)
				throw IllegalStateException("DeepL HTTP ${conn.responseCode}")
			val response = conn.inputStream.bufferedReader().use { it.readText() }
			return JSONObject(response).getJSONArray("translations").getJSONObject(0).getString("text")
		}
		finally
		{
			conn.disconnect()
		}
	}
}
