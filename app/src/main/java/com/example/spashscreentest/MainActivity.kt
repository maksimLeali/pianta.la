package com.example.spashscreentest

import android.animation.Animator
import android.animation.AnimatorListenerAdapter

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.Menu
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.opengl.Visibility

import android.util.Log

import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import com.google.ar.core.Config


import io.github.sceneview.math.Position
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.getDescription
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode

import io.github.sceneview.light.clone
import io.github.sceneview.light.intensity

import io.github.sceneview.node.LightNode


import io.github.sceneview.utils.setFullScreen



class MainActivity : AppCompatActivity(R.layout.activity_main) {

    lateinit var sceneView: ArSceneView
    lateinit var loadingView: View
    lateinit var statusText: TextView
    lateinit var plantImagesLayout: LinearLayout

    data class Model(
        val fileLocation: String,
        val scaleUnits: Float? = null,
        val placementMode: PlacementMode = PlacementMode.PLANE_HORIZONTAL_AND_VERTICAL,
        val applyPoseRotation: Boolean = true
    )

    data class Plant(
        val imageUrl: String,
        val glbModel: Model,
        val name: String,
        val priceEuro: Double
    )

    val plants = listOf(
        Plant(imageUrl = "plant_0" , glbModel= Model( "models/plant-01.glb",scaleUnits = .5F),name ="Lumibella" , priceEuro = 12.27),
        Plant(imageUrl = "plant_1" , glbModel= Model( "models/plant-02.glb",scaleUnits = .5F),name ="Vortexia" , priceEuro = 10.10),
        Plant(imageUrl = "plant_2" , glbModel= Model( "models/plant-03.glb",scaleUnits = .5F),name ="Zephyranthus" , priceEuro = 8.99),
        Plant(imageUrl = "plant_3" , glbModel= Model( "models/plant-04.glb",scaleUnits = .5F ),name ="Phantasmafolia" , priceEuro = 15.43),
        Plant(imageUrl = "plant_4" , glbModel= Model( "models/plant-05.glb", scaleUnits = .5F),name ="Glitterpetal" , priceEuro = 18.50),
    )

    var modelIndex = 0
    var modelNode: ArModelNode? = null
    var originalDistance = 0F
    var selected = false ;
    var isLoading = false
        set(value) {
            field = value
            loadingView.isGone = !value
        }

    private lateinit var backButton: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backButton = findViewById(R.id.showButton)
        backButton.visibility = View.GONE
        setFullScreen(
            findViewById(R.id.rootView),
            fullScreen = true,
            hideSystemBars = false,
            fitsSystemWindows = false
        )

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val arSceneView: View = findViewById(R.id.sceneView)
        val arlayoutParams = arSceneView.layoutParams
        arlayoutParams.width = screenWidth -1
        arlayoutParams.height = screenHeight - 480
        arSceneView.layoutParams = arlayoutParams

        statusText = findViewById(R.id.statusText)
        sceneView = findViewById<ArSceneView?>(R.id.sceneView).apply {
            lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            depthEnabled = false
            instantPlacementEnabled = true
            onArTrackingFailureChanged = { reason ->
                statusText.text = reason?.getDescription(context)
                statusText.isGone = reason == null
            }
        }

        originalDistance = sceneView.cameraDistance
        loadingView = findViewById(R.id.loadingView)
        plantImagesLayout = findViewById(R.id.plantImagesLayout)

        backButton.setOnClickListener {
            plantImagesLayout.removeAllViews()
            removeModel(true)
            populateImagesLayout()
        }

        populateImagesLayout()

    }

    private fun plantSelected(index: Int){
        selected = true;
        animateHeightChange(480, { newModelNode();})

        plantImagesLayout.removeAllViews()
        val displayMetrics = resources.displayMetrics
        val deviceWidth = displayMetrics.widthPixels
        val desiredWidth: Int =  ( deviceWidth / 3.5).toInt()

        val plantSelectedWrapper = LinearLayout(this).apply{
            orientation = LinearLayout.HORIZONTAL
            layoutParams= LinearLayout.LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT, 350)
            gravity = Gravity.CENTER_VERTICAL
        }

        val frameLayout = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                desiredWidth, LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.image_margin)
                marginEnd = resources.getDimensionPixelSize(R.dimen.image_margin)
                setPadding(40,40,40,40)
            }
            val cornerRadius = resources.getDimension(R.dimen.corner_radius)
            val shapeDrawable = ShapeDrawable(
                RoundRectShape(
                    floatArrayOf(
                        cornerRadius,
                        cornerRadius,
                        cornerRadius,
                        cornerRadius,
                        cornerRadius,
                        cornerRadius,
                        cornerRadius,
                        cornerRadius),
                    null, null))
            shapeDrawable.paint.color = Color.WHITE
            background = shapeDrawable
        }

        val imageName = plants[index].imageUrl
        val resourceId = resources.getIdentifier(imageName, "drawable", packageName)
        val imageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            setImageResource(resourceId)

        }

        val labelLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                (deviceWidth / 2.5) .toInt()  ,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val margin = resources.getDimensionPixelSize(R.dimen.label_margin_top)
                setMargins(margin, 0, 0, 0)
            }

            val nameLabel = TextView(this.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                textSize= 22F
                text = plants[index].name
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.BLACK)
            }
            addView(nameLabel)

            val priceLabel = TextView(this.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                textSize= 16F
                text = String.format("%.2f €", plants[index].priceEuro)
                setTextColor(Color.BLACK)
            }
            addView(priceLabel)
        }

        val svgImageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                180,180
            ).apply {
                gravity = Gravity.END
                setPadding(40,40,40,40)
            }

            setImageResource(R.drawable.shopping_cart)
            val circleDrawable = GradientDrawable()
            circleDrawable.shape = GradientDrawable.OVAL
            circleDrawable.setColor(ContextCompat.getColor(context, R.color.blue))
            background = circleDrawable
            setOnClickListener {

                plantImagesLayout.removeAllViews()
                removeModel()
                populateImagesLayout()
            }
        }

        frameLayout.addView(imageView)
        plantSelectedWrapper.addView(frameLayout)
        plantSelectedWrapper.addView(labelLayout)
        plantSelectedWrapper.addView(svgImageView)
        plantImagesLayout.addView(plantSelectedWrapper)
    }

    private fun populateImagesLayout() {
        animateHeightChange(760, null)
        selected = false
        backButton.visibility = View.GONE
        for ((index, model) in plants.withIndex()) {

            val displayMetrics = resources.displayMetrics
            val deviceWidth = displayMetrics.widthPixels
            val desiredWidth = (deviceWidth / 3) + 30

            val wrapperLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                     desiredWidth, LinearLayout.LayoutParams.MATCH_PARENT
                )

            }
            val frameLayout = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 480
                ).apply {
                    marginStart = resources.getDimensionPixelSize(R.dimen.image_margin)
                    marginEnd = resources.getDimensionPixelSize(R.dimen.image_margin)
                }
                val cornerRadius = resources.getDimension(R.dimen.corner_radius)
                val shapeDrawable = ShapeDrawable(RoundRectShape(floatArrayOf(cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius), null, null))
                shapeDrawable.paint.color = Color.WHITE
                background = shapeDrawable
            }

            val imageName = plants[index].imageUrl
            val resourceId = resources.getIdentifier(imageName, "drawable", packageName)

            val imageView = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                val roundedCorner = 10.dpToPx()
                val maskDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = roundedCorner.toFloat()
                    setBackgroundColor(Color.WHITE)
                }

                background = maskDrawable
                setImageResource(resourceId)
                setOnClickListener {
                    plantSelected(index)
                    modelIndex = index

                }
            }

            val labelLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    val margin = resources.getDimensionPixelSize(R.dimen.label_margin_top)
                    setMargins(margin, margin, 0, 0)
                }

                val nameLabel = TextView(this.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    text = plants[index].name
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.BLACK)
                }
                addView(nameLabel)

                val priceLabel = TextView(this.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    text = String.format("%.2f €", plants[index].priceEuro)
                    setTextColor(Color.BLACK)
                }
                addView(priceLabel)
            }

            wrapperLayout.addView(frameLayout)
            frameLayout.addView(imageView)
            wrapperLayout.addView(labelLayout)

            plantImagesLayout.addView(wrapperLayout)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        item.isChecked = !item.isChecked
        modelNode?.detachAnchor()
        modelNode?.placementMode =PlacementMode.PLANE_HORIZONTAL
        return super.onOptionsItemSelected(item)
    }


    fun placeModelNode() {
        modelNode?.anchor()

        sceneView.planeRenderer.isVisible = false
    }
    fun newModelNode() {
        isLoading = true
        val plant = plants[modelIndex]
        modelIndex = (modelIndex + 1) % plants.size
        modelNode = ArModelNode(sceneView.engine, plant.glbModel.placementMode).apply {
            isSmoothPoseEnable = true
            applyPoseRotation = plant.glbModel.applyPoseRotation
            loadModelGlbAsync(
                glbFileLocation = plant.glbModel.fileLocation,
                autoAnimate = true,
                scaleToUnits = plant.glbModel.scaleUnits,
                centerOrigin = Position(y =-.1F)
            ) {
                sceneView.planeRenderer.isVisible = true
                sceneView.planeRenderer.isShadowReceiver= true
                setCastShadows(true)
                isLoading = false
                if(selected){
                    backButton.visibility = View.VISIBLE
                }



            }
            onTap= { motionEvent, renderable ->
                    placeModelNode()

            }
        }

        sceneView.addChild(modelNode!!)
        sceneView.selectedNode = modelNode
    }
    fun removeModel(force: Boolean = false){
        modelNode?.takeIf { !it.isAnchored || force }?.let {
            sceneView.removeChild(it)
            it.destroy()
        }
    }

    private fun animateHeightChange(targetHeight: Int, onAnimationEnd: (() -> Unit)? ) {
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val arWrapper: View = findViewById(R.id.arWrapper)
        val initialHeight = screenHeight - arWrapper.height

        val valueAnimator = ValueAnimator.ofInt(initialHeight, targetHeight)
        valueAnimator.addUpdateListener { animator ->
            val animatedValue = animator.animatedValue as Int
            val layoutParams = arWrapper.layoutParams
            layoutParams.height = screenHeight - animatedValue
            arWrapper.layoutParams = layoutParams
        }
        valueAnimator.duration = 550
        if(onAnimationEnd != null) {
            valueAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onAnimationEnd.invoke()
                }
            })
        }
        valueAnimator.start()
    }

    fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }
}