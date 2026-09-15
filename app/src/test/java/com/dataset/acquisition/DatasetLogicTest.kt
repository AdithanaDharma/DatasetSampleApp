package com.dataset.acquisition

import com.dataset.acquisition.data.model.QualityCategory
import com.dataset.acquisition.data.model.TargetRoi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DatasetLogicTest {

    @Test
    fun testFileNameGeneration() {
        val template = "[kategoriKualitas]_[tanggalAmbil]_[sampleid]_[imageid].jpg"
        val category = QualityCategory.BAIK
        val sampleId = "S001"
        val imageIndex = 1

        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 24)
        }
        val date = calendar.time
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dateStr = dateFormat.format(date)

        val imageIdStr = String.format(Locale.US, "IMG%03d", imageIndex)

        val result = template
            .replace("[kategoriKualitas]", category.displayName, ignoreCase = true)
            .replace("[tanggalAmbil]", dateStr, ignoreCase = true)
            .replace("[sampleid]", sampleId, ignoreCase = true)
            .replace("[imageid]", imageIdStr, ignoreCase = true)

        assertEquals("Baik_20261024_S001_IMG001.jpg", result)
    }

    @Test
    fun testTargetRoiAlignmentLogic() {
        val targetRoi = TargetRoi(
            centerX = 0.5f,
            centerY = 0.5f,
            width = 0.5f,
            height = 0.6f,
            tolerance = 0.10f
        )

        // Objek tepat di tengah
        val isExactCenterAligned = targetRoi.isAligned(
            objCenterX = 0.5f,
            objCenterY = 0.5f,
            objWidth = 0.5f,
            objHeight = 0.6f
        )
        assertTrue("Objek tepat di tengah harus ALIGNED", isExactCenterAligned)

        // Objek bergeser sedikit masih dalam batas toleransi 0.10f
        val isWithinTolerance = targetRoi.isAligned(
            objCenterX = 0.55f,
            objCenterY = 0.55f,
            objWidth = 0.5f,
            objHeight = 0.6f
        )
        assertTrue("Objek dalam batas deviasi 0.05f harus tetap ALIGNED", isWithinTolerance)

        // Objek meleset jauh dari titik tengah
        val isFarOff = targetRoi.isAligned(
            objCenterX = 0.80f,
            objCenterY = 0.20f,
            objWidth = 0.5f,
            objHeight = 0.6f
        )
        assertFalse("Objek yang meleset jauh harus MISALIGNED", isFarOff)
    }
}
