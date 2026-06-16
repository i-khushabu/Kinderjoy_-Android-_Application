
package com.yourname.kidslearning


import android.content.Context
import android.util.Log
import com.google.mlkit.vision.digitalink.*
import com.google.mlkit.common.model.DownloadConditions

import com.google.mlkit.common.model.RemoteModelManager


class HandwritingRecognizer(context: Context) {

    private val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")
    private val model = DigitalInkRecognitionModel.builder(modelIdentifier!!).build()


    private val modelManager = RemoteModelManager.getInstance()

    private val conditions = DownloadConditions.Builder().requireWifi().build()
    init {
        modelManager.download(model, conditions)
            .addOnSuccessListener {
                Log.d("HandwritingRecognizer", "Model downloaded successfully")
            }
            .addOnFailureListener { e ->
                Log.e("HandwritingRecognizer", "Model download failed", e)
            }

    }

    private val recognizer = DigitalInkRecognition.getClient( // ✅ Fixed
        DigitalInkRecognizerOptions.builder(model).build()
    )

    fun recognizeInk(ink: Ink, onResult: (String) -> Unit) {
        if (ink.strokes.isEmpty()) {
            Log.e("HandwritingRecognizer", "Ink data is empty!")
            onResult("Error: No input")
            return
        }

        if (recognizer == null) {
            Log.e("HandwritingRecognizer", "Recognizer not initialized!")
            onResult("Error: Recognizer not ready")
            return
        }

        recognizer.recognize(ink)
            .addOnSuccessListener { result: RecognitionResult ->
                val recognizedText = result.candidates.firstOrNull()?.text ?: "?"
                onResult(recognizedText)
            }
            .addOnFailureListener { e ->
                Log.e("HandwritingRecognizer", "Recognition failed", e)
                onResult("Error")
            }
    }

}