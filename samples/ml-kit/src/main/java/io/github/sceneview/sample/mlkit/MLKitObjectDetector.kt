package io.github.sceneview.sample.mlkit

import android.media.Image
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.tasks.asDeferred

/**
 * Object detector using the base model provided by ML Kit
 */
class MLKitObjectDetector {

    private val builder = ObjectDetectorOptions.Builder()
    private val options = builder
        .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
        .enableClassification()
        .enableMultipleObjects()
        .build()

    private val detector = ObjectDetection.getClient(options)

    /**
     * Suspended function that provide a list of detected object for the given image
     *
     * @param image The image that will be analysed
     * @param imageRotation The rotation of the given image, used to compute the coordinates of detected objects
     * @return A list of [DetectedObjectResult]
     */
    suspend fun analyze(image: Image, imageRotation: Int): List<DetectedObjectResult> {

        val inputImage = InputImage.fromMediaImage(image, imageRotation)

        // TODO remove await
        val detectedObjects = detector.process(inputImage).asDeferred().await()

        return detectedObjects.mapNotNull { obj ->
            val bestLabel =
                obj.labels.maxByOrNull { label -> label.confidence } ?: return@mapNotNull null
            val coords =
                obj.boundingBox.exactCenterX().toInt() to obj.boundingBox.exactCenterY().toInt()
            val rotatedCoordinates =
                coords.rotateCoordinates(image.width, image.height, imageRotation)
            DetectedObjectResult(bestLabel.confidence, bestLabel.text, rotatedCoordinates)
        }
    }
}

/**
 * Value returned by the classifier
 */
data class DetectedObjectResult(
    val confidence: Float,
    val label: String,
    val centerCoordinate: Pair<Int, Int>
)

/**
 * Rotates a coordinate pair according to [imageRotation].
 */
fun Pair<Int, Int>.rotateCoordinates(
    imageWidth: Int,
    imageHeight: Int,
    imageRotation: Int,
): Pair<Int, Int> {
    val (x, y) = this
    return when (imageRotation) {
        0 -> x to y
        180 -> imageWidth - x to imageHeight - y
        90 -> y to imageWidth - x
        270 -> imageHeight - y to x
        else -> error("Invalid imageRotation $imageRotation")
    }
}