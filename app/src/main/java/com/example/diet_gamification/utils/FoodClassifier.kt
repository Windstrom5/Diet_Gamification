package com.example.diet_gamification.utils

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.IOException

class FoodClassifier(private val context: Context) {

    private var classifier: ImageClassifier? = null

    init {
        try {
            val options = ImageClassifier.ImageClassifierOptions.builder()
                .setMaxResults(3)  // You can set top 3 predictions
                .build()
            classifier = ImageClassifier.createFromFileAndOptions(
                context,
                "dataset/food101.tflite", // path to your model in assets folder
                options
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun classify(bitmap: Bitmap): List<Classifications>? {
        // Resize image manually
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

        // Convert the resized image to TensorImage
        val image = TensorImage.fromBitmap(resizedBitmap)

        // Get the raw ByteBuffer from TensorImage
        val byteBuffer = image.buffer

        // Normalize the image: Scale pixels to [0, 1]
        val floatBuffer = ByteArray(byteBuffer.remaining())
        byteBuffer.get(floatBuffer)

        // Now convert and normalize the byte data
        for (i in floatBuffer.indices) {
            floatBuffer[i] = ((floatBuffer[i].toInt() and 0xFF) / 255f).toInt().toByte()
        }

        // Copy back the normalized values to the original ByteBuffer
        byteBuffer.clear()
        byteBuffer.put(floatBuffer)

        return classifier?.classify(image)
    }
}