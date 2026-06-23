package com.amr3d.preview

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color

class MainActivity : AppCompatActivity() {

    private lateinit var glViewerView: GLViewerView
    private lateinit var emptyStateText: TextView
    private lateinit var emptyStateLayout: View
    private lateinit var btnOpenFile: ImageButton
    private lateinit var btnOpenFileCenter: Button
    private lateinit var btnMeasureTool: ToggleButton
    private lateinit var btnInspect: Button
    private lateinit var btnWhatsapp: ImageButton
    private lateinit var btnResetView: Button
    private lateinit var btnWireframe: ToggleButton
    private lateinit var btnColor: Button
    private lateinit var btnUnit: Button
    private lateinit var btnExport: Button
    private lateinit var viewCube: ViewCubeView
    private lateinit var viewCubeContainer: View
    private lateinit var directionButtons: LinearLayout
    private lateinit var btnViewBack: Button
    private lateinit var btnViewLeft: Button
    private lateinit var btnViewBottom: Button
    private lateinit var measurementCard: CardView
    private lateinit var measurementText: TextView
    private lateinit var inspectionCard: CardView
    private lateinit var inspectionText: TextView

    private var currentModel: STLModel? = null
    private var measureModeOn = false
    private var currentUnit = MeasurementUnit.MM
    private var directionButtonsVisible = false
    private var lastMeasurementPoints: List<FloatArray>? = null

    private val openDocumentLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) loadFile(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        wireUpListeners()
        intent?.data?.let { uri -> loadFile(uri) }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let { uri -> loadFile(uri) }
    }

    private fun bindViews() {
        glViewerView = findViewById(R.id.glViewerView)
        emptyStateText = findViewById(R.id.emptyStateText)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        btnOpenFile = findViewById(R.id.btnOpenFile)
        btnOpenFileCenter = findViewById(R.id.btnOpenFileCenter)
        btnMeasureTool = findViewById(R.id.btnMeasureTool)
        btnInspect = findViewById(R.id.btnInspect)
        btnResetView = findViewById(R.id.btnResetView)
        btnWhatsapp = findViewById(R.id.btnWhatsapp)
        btnWireframe = findViewById(R.id.btnWireframe)
        btnColor = findViewById(R.id.btnColor)
        btnUnit = findViewById(R.id.btnUnit)
        btnExport = findViewById(R.id.btnExport)
        viewCube = findViewById(R.id.viewCube)
        viewCubeContainer = findViewById(R.id.viewCubeContainer)
        directionButtons = findViewById(R.id.directionButtons)
        btnViewBack = findViewById(R.id.btnViewBack)
        btnViewLeft = findViewById(R.id.btnViewLeft)
        btnViewBottom = findViewById(R.id.btnViewBottom)
        measurementCard = findViewById(R.id.measurementCard)
        measurementText = findViewById(R.id.measurementText)
        inspectionCard = findViewById(R.id.inspectionCard)
        inspectionText = findViewById(R.id.inspectionText)
    }

    private fun wireUpListeners() {
        val openAction = { openDocumentLauncher.launch(arrayOf("*/*")) }
        btnOpenFile.setOnClickListener { openAction() }
        btnOpenFileCenter.setOnClickListener { openAction() }

        btnMeasureTool.setOnCheckedChangeListener { _, isChecked ->
            measureModeOn = isChecked
            if (!isChecked) {
                glViewerView.stlRenderer.clearMeasurementPoints()
                measurementCard.visibility = View.GONE
                lastMeasurementPoints = null
            } else {
                inspectionCard.visibility = View.GONE
                Toast.makeText(this, "اضغط على نقطتين على سطح الموديل", Toast.LENGTH_LONG).show()
            }
        }

        btnInspect.setOnClickListener {
            currentModel?.let { showInspectionReport(it) }
                ?: Toast.makeText(this, "افتح ملفاً أولاً", Toast.LENGTH_SHORT).show()
        }

        btnResetView.setOnClickListener { resetCamera() }
        btnWhatsapp.setOnClickListener { openWhatsapp() }

        btnWireframe.setOnCheckedChangeListener { _, isChecked ->
            glViewerView.stlRenderer.wireframeMode = isChecked
        }

        btnColor.setOnClickListener { showColorPicker() }
        btnUnit.setOnClickListener { cycleUnit() }

        btnExport.setOnClickListener {
            if (currentModel == null) {
                Toast.makeText(this, "افتح ملفاً أولاً", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // سؤال: تصدير عادي أم بالقياسات؟
            AlertDialog.Builder(this)
                .setTitle("تصدير الصورة")
                .setItems(arrayOf("صورة عادية", "صورة بالقياسات")) { _, which ->
                    when (which) {
                        0 -> exportCurrentView(withMeasurements = false)
                        1 -> exportCurrentView(withMeasurements = true)
                    }
                }.show()
        }

        // ViewCube - عند الضغط تظهر/تختفي أزرار الاتجاهات
        viewCubeContainer.setOnClickListener {
            toggleDirectionButtons()
        }

        viewCube.onFaceSelected = { face ->
            jumpToView(face.rotX, face.rotY)
            hideDirectionButtons()
        }

        btnViewBack.setOnClickListener { jumpToView(-10f, 180f); hideDirectionButtons() }
        btnViewLeft.setOnClickListener { jumpToView(-10f, -90f); hideDirectionButtons() }
        btnViewBottom.setOnClickListener { jumpToView(89f, 0f); hideDirectionButtons() }

        glViewerView.onSingleTap = { x, y ->
            if (measureModeOn) handleMeasurementTap(x, y)
        }

        inspectionCard.setOnClickListener { inspectionCard.visibility = View.GONE }
        measurementCard.setOnClickListener {
            measurementCard.visibility = View.GONE
            glViewerView.stlRenderer.clearMeasurementPoints()
            lastMeasurementPoints = null
        }
    }

    private fun toggleDirectionButtons() {
        if (directionButtonsVisible) hideDirectionButtons() else showDirectionButtons()
    }

    private fun showDirectionButtons() {
        directionButtonsVisible = true
        directionButtons.visibility = View.VISIBLE
        directionButtons.alpha = 0f
        directionButtons.animate().alpha(1f).setDuration(200).start()
    }

    private fun hideDirectionButtons() {
        directionButtonsVisible = false
        directionButtons.animate().alpha(0f).setDuration(150).withEndAction {
            directionButtons.visibility = View.GONE
        }.start()
    }

    private fun jumpToView(targetRotX: Float, targetRotY: Float) {
        val renderer = glViewerView.stlRenderer
        val startX = renderer.rotationX
        val startY = renderer.rotationY
        android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 350
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener { anim ->
                val t = anim.animatedValue as Float
                renderer.rotationX = startX + (targetRotX - startX) * t
                renderer.rotationY = startY + (targetRotY - startY) * t
            }
        }.start()
    }

    private fun resetCamera() {
        val renderer = glViewerView.stlRenderer
        renderer.rotationX = -25f
        renderer.rotationY = 35f
        renderer.scaleFactor = 1f
        renderer.panX = 0f
        renderer.panY = 0f
        glViewerView.queueEvent { renderer.updateProjection() }
    }

    private fun showColorPicker() {
        val items = arrayOf(
            "── لون الموديل ──",
            "أزرق (افتراضي)", "نحاسي", "رمادي", "أخضر", "أحمر", "ذهبي",
            "── لون الخلفية ──",
            "داكن (افتراضي)", "أسود", "رمادي داكن", "أبيض", "كحلي"
        )
        AlertDialog.Builder(this)
            .setTitle("اختر اللون")
            .setItems(items) { _, which ->
                when (which) {
                    1 -> glViewerView.stlRenderer.setModelColor(0.45f, 0.75f, 0.95f)
                    2 -> glViewerView.stlRenderer.setModelColor(0.80f, 0.50f, 0.25f)
                    3 -> glViewerView.stlRenderer.setModelColor(0.65f, 0.65f, 0.68f)
                    4 -> glViewerView.stlRenderer.setModelColor(0.40f, 0.75f, 0.45f)
                    5 -> glViewerView.stlRenderer.setModelColor(0.85f, 0.35f, 0.30f)
                    6 -> glViewerView.stlRenderer.setModelColor(0.90f, 0.75f, 0.30f)
                    8 -> glViewerView.stlRenderer.setBackgroundColor(0.10f, 0.11f, 0.13f)
                    9 -> glViewerView.stlRenderer.setBackgroundColor(0.02f, 0.02f, 0.02f)
                    10 -> glViewerView.stlRenderer.setBackgroundColor(0.22f, 0.24f, 0.27f)
                    11 -> glViewerView.stlRenderer.setBackgroundColor(0.92f, 0.92f, 0.92f)
                    12 -> glViewerView.stlRenderer.setBackgroundColor(0.05f, 0.08f, 0.18f)
                }
            }.show()
    }

    private fun cycleUnit() {
        currentUnit = when (currentUnit) {
            MeasurementUnit.MM -> MeasurementUnit.CM
            MeasurementUnit.CM -> MeasurementUnit.INCH
            MeasurementUnit.INCH -> MeasurementUnit.MM
        }
        btnUnit.text = currentUnit.label
        currentModel?.let { if (inspectionCard.visibility == View.VISIBLE) showInspectionReport(it) }
        val points = glViewerView.stlRenderer.getMeasurementPoints()
        if (points.size == 2) updateMeasurementText(points[0], points[1])
    }

    private fun exportCurrentView(withMeasurements: Boolean) {
        val renderer = glViewerView.stlRenderer
        val width = renderer.getSurfaceWidth()
        val height = renderer.getSurfaceHeight()
        if (width <= 0 || height <= 0) return

        glViewerView.queueEvent {
            val bitmap = renderer.captureFrame(width, height)
            val finalBitmap = if (withMeasurements) {
                addMeasurementsOverlay(bitmap)
            } else {
                bitmap
            }
            runOnUiThread { saveAndShareBitmap(finalBitmap) }
        }
    }

    private fun addMeasurementsOverlay(src: Bitmap): Bitmap {
        val result = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 40f
            color = Color.WHITE
            setShadowLayer(3f, 2f, 2f, Color.BLACK)
        }
        val linePaint = Paint().apply {
            color = Color.argb(200, 255, 165, 30)
            strokeWidth = 3f
        }

        val points = lastMeasurementPoints
        if (points != null && points.size == 2) {
            val distance = MeasurementTools.distanceBetween(points[0], points[1], currentUnit)
            val label = String.format(Locale.US, "%.3f %s", distance, currentUnit.label)

            // رسم صندوق نص
            val boxPaint = Paint().apply {
                color = Color.argb(180, 0, 0, 0)
            }
            canvas.drawRoundRect(20f, 20f, 400f, 90f, 12f, 12f, boxPaint)
            canvas.drawText("📐 $label", 35f, 70f, paint)
        }

        // اسم التطبيق في الأسفل
        paint.textSize = 28f
        paint.color = Color.argb(180, 255, 138, 30)
        canvas.drawText("Amr3D Preview", 20f, result.height - 20f, paint)

        return result
    }

    private fun saveAndShareBitmap(bitmap: Bitmap) {
        try {
            val picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File(picturesDir, "Amr3D_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            Toast.makeText(this, getString(R.string.export_success), Toast.LENGTH_SHORT).show()
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.export_image)))
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر حفظ الصورة: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openWhatsapp() {
        val phone = getString(R.string.whatsapp_number)
        val message = Uri.encode(getString(R.string.whatsapp_message))
        try {
            val nativeUri = Uri.parse("whatsapp://send?phone=$phone&text=$message")
            startActivity(Intent(Intent.ACTION_VIEW, nativeUri).setPackage("com.whatsapp"))
            return
        } catch (e: Exception) { }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phone?text=$message")))
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر فتح واتساب", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadFile(uri: Uri) {
        Toast.makeText(this, "جارٍ تحميل الملف...", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val fileName = getFileName(uri).lowercase()
                val model = withContext(Dispatchers.Default) {
                    when {
                        fileName.endsWith(".dxf") -> DXFParser.parse(this@MainActivity, uri)
                        else -> STLParser.parse(this@MainActivity, uri)
                    }
                }
                currentModel = model
                glViewerView.stlRenderer.setModel(model)
                emptyStateLayout.visibility = View.GONE
                inspectionCard.visibility = View.GONE
                measurementCard.visibility = View.GONE
                btnMeasureTool.isChecked = false
                btnWireframe.isChecked = false
                lastMeasurementPoints = null
                Toast.makeText(this@MainActivity, "تم التحميل: ${model.triangleCount} مثلث", Toast.LENGTH_SHORT).show()
            } catch (e: STLParseException) {
                Toast.makeText(this@MainActivity, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "تعذر قراءة الملف", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = uri.lastPathSegment ?: ""
        try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && it.moveToFirst()) name = it.getString(idx) ?: name
            }
        } catch (_: Exception) {}
        return name
    }

    private fun handleMeasurementTap(screenX: Float, screenY: Float) {
        val model = currentModel ?: return
        val renderer = glViewerView.stlRenderer
        val ray = RayPicker.screenPointToRay(
            screenX, screenY,
            renderer.getSurfaceWidth(), renderer.getSurfaceHeight(),
            renderer.getCurrentModelMatrix(),
            renderer.getCurrentViewMatrix(),
            renderer.getCurrentProjectionMatrix()
        )
        val hitPoint = RayPicker.findClosestIntersection(ray, model)
        if (hitPoint == null) {
            Toast.makeText(this, "لم يتم تحديد نقطة على سطح الموديل", Toast.LENGTH_SHORT).show()
            return
        }
        renderer.addMeasurementPoint(hitPoint)
        val points = renderer.getMeasurementPoints()
        if (points.size == 2) {
            lastMeasurementPoints = points.toList()
            updateMeasurementText(points[0], points[1])
        } else {
            measurementText.text = "نقطة أولى محددة — اضغط على نقطة ثانية"
            measurementCard.visibility = View.VISIBLE
        }
    }

    private fun updateMeasurementText(p1: FloatArray, p2: FloatArray) {
        val distance = MeasurementTools.distanceBetween(p1, p2, currentUnit)
        measurementText.text = String.format(
            Locale.US,
            "المسافة: %.3f %s\n(X:%.2f Y:%.2f Z:%.2f) → (X:%.2f Y:%.2f Z:%.2f)",
            distance, currentUnit.label,
            p1[0], p1[1], p1[2], p2[0], p2[1], p2[2]
        )
        measurementCard.visibility = View.VISIBLE
    }

    private fun showInspectionReport(model: STLModel) {
        if (inspectionCard.visibility == View.VISIBLE) {
            inspectionCard.visibility = View.GONE
            return
        }
        val report = MeasurementTools.inspect(model, currentUnit)
        val u = report.unit.label
        val sb = StringBuilder()
        sb.append("📐 أبعاد الموديل\n─────────────────\n")
        sb.append(String.format(Locale.US, "الطول (X):    %.2f %s\n", report.width, u))
        sb.append(String.format(Locale.US, "العرض (Y):   %.2f %s\n", report.depth, u))
        sb.append(String.format(Locale.US, "الارتفاع (Z): %.2f %s", report.height, u))
        inspectionText.text = sb.toString()
        inspectionCard.visibility = View.VISIBLE
        measurementCard.visibility = View.GONE
    }
}
