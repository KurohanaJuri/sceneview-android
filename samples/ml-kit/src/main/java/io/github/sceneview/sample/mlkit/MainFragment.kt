package io.github.sceneview.sample.mlkit

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Coordinates2d
import com.google.ar.sceneform.rendering.ViewRenderable
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import kotlinx.coroutines.*


class MainFragment : Fragment(R.layout.fragment_main) {

    companion object {
        private const val TAG = "MainFragment"
    }

    private lateinit var sceneView: ArSceneView
    private lateinit var loadingView: View
    private lateinit var floatingButton: ExtendedFloatingActionButton

    var scanButtonIsPressed = false

    private val objectDetector = MLKitObjectDetector()
    private var objectResults: List<DetectedObjectResult>? = null

    private val modelScope = CoroutineScope(Dispatchers.IO)

    private val rotationHelper = DeviceRotationHelper()

    private val convertFloats = FloatArray(4)
    private val convertFloatsOut = FloatArray(4)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        floatingButton = view.findViewById(R.id.floatingButton)
        loadingView = view.findViewById(R.id.loadingView)
        sceneView = view.findViewById(R.id.sceneView)

        floatingButton.setOnClickListener {
            scanButtonIsPressed = true
            isScanButtonActive(true)
        }

        sceneView.apply {

            onArFrame = {

                // If the button is pressed, start the image scan
                if (scanButtonIsPressed) {
                    scanButtonIsPressed = false

                    val cameraImage = it.frame.acquireCameraImage()

                    if (cameraImage != null) {
                        // start the model analysis
                        modelScope.launch {
                            val cameraId =
                                sceneView.arSession?.cameraConfig?.cameraId ?: return@launch

                            val imageRotation = rotationHelper.getRotationCompensation(
                                cameraId,
                                this@MainFragment.activity as Activity,
                                false
                            )
                            objectResults = objectDetector.analyze(cameraImage, imageRotation)
                            cameraImage.close()
                        }
                    }

                    // Display the labels
                    val objects = objectResults
                    if (objects != null && objects.isNotEmpty()) {
                        objectResults = null
                        Log.i(TAG, "Got objects : $objects")

                        // TODO : remove duplicate if scan multiple time
                        // For each label get the position and display the label
                        for (obj in objects) {
                            convertFloats[0] = obj.centerCoordinate.first.toFloat()
                            convertFloats[1] = obj.centerCoordinate.second.toFloat()
                            it.frame.transformCoordinates2d(
                                Coordinates2d.IMAGE_PIXELS,
                                convertFloats,
                                Coordinates2d.VIEW,
                                convertFloatsOut
                            )

                            // Create a anchor at the given position
                            val hits = it.frame.hitTest(convertFloatsOut[0], convertFloatsOut[1])
                            val result = hits.getOrNull(0) ?: continue
                            val anchor = result.trackable.createAnchor(result.hitPose)


                            val node = ArModelNode().apply {
                                this.anchor = anchor
                            }

                            // Create the label that will be displayed
                            ViewRenderable.builder()
                                .setView(this@MainFragment.context, R.layout.label).build(lifecycle)
                                .thenAccept { renderable ->
                                    (renderable.view as TextView).text = obj.label
                                    node.setModel(renderable)

                                    // Remove shadow
                                    renderable.isShadowCaster = false
                                    renderable.isShadowReceiver = false
                                }

                            sceneView.addChild(node)
                        }

                    } else {
                        Toast.makeText(
                            this@MainFragment.activity,
                            "No object detected",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    isScanButtonActive(false)
                }
            }
        }
    }

    private fun isScanButtonActive(active: Boolean) = when (active) {
        true -> {
            floatingButton.text = getString(R.string.Scanning)
            floatingButton.isEnabled = false
        }
        false -> {
            floatingButton.text = getString(R.string.scan)
            floatingButton.isEnabled = true
        }
    }

}